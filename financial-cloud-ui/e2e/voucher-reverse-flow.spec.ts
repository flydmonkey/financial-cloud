import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {assertReportsBalanced, fetchSubjectBalances, subjectPeriodAmount} from './helpers/reports'
import {
    auditVoucher,
    buildBalancedVoucherPayload,
    createDraftVoucher,
    ensureVoucherReviewEnabled,
    getVoucherDetail,
    postVoucher,
    submitVoucher,
    unauditVoucher,
    unpostVoucher,
    updateVoucher,
    type VoucherPayload,
} from './helpers/voucher'

/**
 * TC-E2E-002：反向修改闭环
 * 反过账 → 反审核 → 修改金额 → 重新审核 → 重新过账 → 报表仍平衡
 */
test.describe.serial('voucher reverse flow', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        voucherId: string
        payload: VoucherPayload | null
        debitSubjectCode: string
        firstPostDebitAmount: number
    } = {
        headers: {},
        bookId: '',
        term: '',
        voucherId: '',
        payload: null,
        debitSubjectCode: '',
        firstPostDebitAmount: 0,
    }

    const initialAmount = 100
    const revisedAmount = 200

    test('login and enable voucher review', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套，请先完成 onboarding 或登录有效账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)
    })

    test('create draft voucher', async ({request}) => {
        ctx.payload = await buildBalancedVoucherPayload(
            request,
            ctx.headers,
            ctx.bookId,
            'E2E反向闭环凭证',
            initialAmount,
        )
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        ctx.debitSubjectCode = subjects.find((s) => s.id === ctx.payload!.items[0].subjectId)?.code || ''
        ctx.voucherId = await createDraftVoucher(request, ctx.headers, ctx.payload)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.status).toBe('draft')
    })

    test('submit, audit and post voucher', async ({request}) => {
        test.skip(!ctx.payload || !ctx.voucherId, '前置凭证未创建')
        await submitVoucher(request, ctx.headers, ctx.payload!, ctx.voucherId)
        await auditVoucher(request, ctx.headers, ctx.voucherId)
        await postVoucher(request, ctx.headers, ctx.voucherId)

        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.status).toBe('completed')
        expect(detail.senderName).toBeTruthy()

        if (ctx.debitSubjectCode) {
            const balances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
            ctx.firstPostDebitAmount = subjectPeriodAmount(balances, ctx.debitSubjectCode)
            expect(ctx.firstPostDebitAmount).toBeGreaterThanOrEqual(initialAmount)
        }
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('unpost and unaudit posted voucher', async ({request}) => {
        test.skip(!ctx.voucherId, '前置凭证未创建')
        await unpostVoucher(request, ctx.headers, ctx.voucherId)
        let detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.senderId).toBeFalsy()

        await unauditVoucher(request, ctx.headers, ctx.voucherId)
        detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.status).toBe('reviewing')
        expect(detail.auditMemberName).toBeFalsy()
    })

    test('update voucher amount after reverse', async ({request}) => {
        test.skip(!ctx.voucherId, '前置凭证未创建')
        await updateVoucher(request, ctx.headers, ctx.voucherId, (payload) => {
            payload.items[0].debitAmount = revisedAmount
            payload.items[1].creditAmount = revisedAmount
        })
        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(Number(detail.items[0].debitAmount)).toBe(revisedAmount)
        expect(Number(detail.items[1].creditAmount)).toBe(revisedAmount)
    })

    test('re-audit and repost with new amount', async ({request}) => {
        test.skip(!ctx.voucherId, '前置凭证未创建')
        await auditVoucher(request, ctx.headers, ctx.voucherId)
        await postVoucher(request, ctx.headers, ctx.voucherId)

        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.status).toBe('completed')
        expect(detail.senderName).toBeTruthy()

        if (ctx.debitSubjectCode) {
            const balances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
            const revisedDebitAmount = subjectPeriodAmount(balances, ctx.debitSubjectCode)
            expect(revisedDebitAmount).toBeGreaterThanOrEqual(revisedAmount)
            if (ctx.firstPostDebitAmount > 0) {
                expect(revisedDebitAmount).toBeGreaterThan(ctx.firstPostDebitAmount)
            }
        }
    })

    test('balance sheet remains balanced after reverse flow', async ({request}) => {
        test.skip(!ctx.voucherId, '前置凭证未创建')
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })
})
