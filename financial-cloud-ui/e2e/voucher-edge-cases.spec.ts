import {expect, test} from '@playwright/test'
import {getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    assertReportsBalanced,
    fetchSubjectBalances,
    getSubjectBalance,
} from './helpers/reports'
import {
    createAndPostVoucher,
    createDraftVoucher,
    ensureVoucherReviewEnabled,
    getVoucherDetail,
    pickSafeBalanceSubjects,
    runVoucherToPosted,
    buildBalancedVoucherPayload,
    tryCreateDraftVoucher,
    tryDeleteVoucher,
} from './helpers/voucher'

const MAX_SUPPORTED_AMOUNT = 99_999_999.99
const OVER_LIMIT_AMOUNT = 999_999_999.99
const RED_LETTER_AMOUNT = 88

/**
 * TC-EXC-004~005：边界金额与红字冲销（红字先跑，避免大金额污染银行余额）
 */
test.describe.serial('voucher edge cases', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
    } = {
        headers: {},
        bookId: '',
        term: '',
    }

    test('login and prepare', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)
    })

    test('TC-EXC-005: red-letter reversal with negative debit and credit', async ({request}) => {
        const {debit, credit} = await pickSafeBalanceSubjects(
            request, ctx.headers, ctx.bookId, ctx.term,
        )

        const beforePositive = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            debit.code,
        )

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'E2E红字-正向', RED_LETTER_AMOUNT,
            {debit, credit},
        )
        const afterPositive = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            debit.code,
        )
        expect(afterPositive - beforePositive).toBeCloseTo(RED_LETTER_AMOUNT, 2)

        const redPayload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, 'E2E红字-冲销', -RED_LETTER_AMOUNT,
            {debit, credit},
        )
        const redId = await createDraftVoucher(request, ctx.headers, redPayload)
        await runVoucherToPosted(request, ctx.headers, redPayload, redId)

        const redDetail = await getVoucherDetail(request, ctx.headers, redId)
        expect(Number(redDetail.debitAmount)).toBe(-RED_LETTER_AMOUNT)
        expect(Number(redDetail.creditAmount)).toBe(-RED_LETTER_AMOUNT)

        const afterRed = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            debit.code,
        )
        expect(afterRed).toBeCloseTo(beforePositive, 2)
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('TC-RPT-006: red-letter posting preserves negative sign in subject balance', async ({request}) => {
        const {debit, credit} = await pickSafeBalanceSubjects(
            request, ctx.headers, ctx.bookId, ctx.term,
        )
        const before = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            debit.code,
        )
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'E2E红字-报表', -RED_LETTER_AMOUNT,
            {debit, credit},
        )
        const after = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            debit.code,
        )
        expect(after - before).toBeCloseTo(-RED_LETTER_AMOUNT, 2)
        test.info().annotations.push({
            type: 'note',
            description: `${debit.code} 余额 ${before} → ${after}（红字 -${RED_LETTER_AMOUNT}）`,
        })
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('TC-EXC-004: amount precision at schema max 99,999,999.99', async ({request}) => {
        const {debit, credit} = await pickSafeBalanceSubjects(
            request, ctx.headers, ctx.bookId, ctx.term,
        )

        const maxPayload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, 'E2E最大精度金额', MAX_SUPPORTED_AMOUNT,
            {debit, credit},
        )
        const maxId = await createDraftVoucher(request, ctx.headers, maxPayload)

        const detail = await getVoucherDetail(request, ctx.headers, maxId)
        expect(Number(detail.debitAmount)).toBe(MAX_SUPPORTED_AMOUNT)
        expect(Number(detail.creditAmount)).toBe(MAX_SUPPORTED_AMOUNT)
        expect(Number(detail.items?.[0]?.debitAmount)).toBe(MAX_SUPPORTED_AMOUNT)
        expect(Number(detail.items?.[1]?.creditAmount)).toBe(MAX_SUPPORTED_AMOUNT)

        const del = await tryDeleteVoucher(request, ctx.headers, maxId)
        expect(del.code, del.message || 'cleanup draft failed').toBe(0)

        const overPayload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, 'E2E超限金额', OVER_LIMIT_AMOUNT,
            {debit, credit},
        )
        const overResult = await tryCreateDraftVoucher(request, ctx.headers, overPayload)
        expect(overResult.code, overResult.message || 'over limit should fail').not.toBe(0)
        test.info().annotations.push({
            type: 'note',
            description: `decimal(10,2) 上限 99,999,999.99；${OVER_LIMIT_AMOUNT} 暂存被拒`,
        })
    })
})
