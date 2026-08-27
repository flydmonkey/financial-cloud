import {expect, test} from '@playwright/test'
import {getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {fetchSubjectBalances, getSubjectBalance} from './helpers/reports'
import {
    auditVoucher,
    buildBalancedVoucherPayload,
    createDraftVoucher,
    ensureVoucherReviewEnabled,
    getVoucherDetail,
    postVoucher,
    runVoucherToPosted,
    submitVoucher,
    tryBatchPostVoucher,
    tryBatchSubmitVoucher,
    tryBatchAuditVoucher,
    tryPostVoucher,
    pickStandardBusinessSubjects,
} from './helpers/voucher'
import {fetchBookSubjects} from './helpers/auth'

/**
 * TC-EXC-001 / TC-EXC-006 / TC-EXC-007、TC-VCH-024 / TC-VCH-055：批量提交与过账
 */
test.describe.serial('voucher batch post operations', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        postableId: string
        reviewingId: string
        postedId: string
        debitCode: string
    } = {
        headers: {},
        bookId: '',
        term: '',
        postableId: '',
        reviewingId: '',
        postedId: '',
        debitCode: '',
    }

    test('login and prepare vouchers', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)

        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {prepaid, expense} = pickStandardBusinessSubjects(subjects)
        test.skip(!prepaid || !expense, '缺少预付/费用科目')
        const pair = {debit: prepaid!, credit: expense!}
        ctx.debitCode = prepaid!.code!

        const mk = async (summary: string, amount: number) => {
            const payload = await buildBalancedVoucherPayload(
                request, ctx.headers, ctx.bookId, summary, amount, pair,
            )
            return {payload, id: await createDraftVoucher(request, ctx.headers, payload)}
        }

        const ready = await mk('批量-可过账', 21)
        await submitVoucher(request, ctx.headers, ready.payload, ready.id)
        await auditVoucher(request, ctx.headers, ready.id)
        ctx.postableId = ready.id

        const reviewing = await mk('批量-reviewing', 22)
        await submitVoucher(request, ctx.headers, reviewing.payload, reviewing.id)
        ctx.reviewingId = reviewing.id

        const posted = await mk('批量-已过账', 23)
        await runVoucherToPosted(request, ctx.headers, posted.payload, posted.id)
        ctx.postedId = posted.id
    })

    test('TC-VCH-036: batch audit completes all reviewing vouchers', async ({request}) => {
        test.skip(!ctx.bookId || !ctx.postableId, '前置凭证缺失')

        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {prepaid, expense} = pickStandardBusinessSubjects(subjects)
        test.skip(!prepaid || !expense, '缺少预付/费用科目')
        const pair = {debit: prepaid!, credit: expense!}

        const mkReviewing = async (summary: string, amount: number) => {
            const payload = await buildBalancedVoucherPayload(
                request, ctx.headers, ctx.bookId, summary, amount, pair,
            )
            const id = await createDraftVoucher(request, ctx.headers, payload)
            await submitVoucher(request, ctx.headers, payload, id)
            return id
        }

        const r1 = await mkReviewing('批量审核-1', 26)
        const r2 = await mkReviewing('批量审核-2', 27)

        const result = await tryBatchAuditVoucher(request, ctx.headers, [
            r1,
            r2,
            ctx.postableId,
        ])

        expect(result.code).toBe(0)
        expect(result.message || '').toMatch(/成功：2/)

        for (const id of [r1, r2]) {
            const detail = await getVoucherDetail(request, ctx.headers, id)
            expect(detail.status).toBe('completed')
            expect(detail.auditMemberName).toBeTruthy()
        }
    })

    test('TC-VCH-024: batch submit skips non-draft vouchers', async ({request}) => {
        test.skip(!ctx.bookId || !ctx.reviewingId, '前置凭证缺失')

        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {prepaid, expense} = pickStandardBusinessSubjects(subjects)
        test.skip(!prepaid || !expense, '缺少预付/费用科目')
        const pair = {debit: prepaid!, credit: expense!}

        const mkDraft = async (summary: string, amount: number) => {
            const payload = await buildBalancedVoucherPayload(
                request, ctx.headers, ctx.bookId, summary, amount, pair,
            )
            return {payload, id: await createDraftVoucher(request, ctx.headers, payload)}
        }

        const d1 = await mkDraft('批量提交-1', 24)
        const d2 = await mkDraft('批量提交-2', 25)
        const result = await tryBatchSubmitVoucher(request, ctx.headers, [
            d1.id,
            d2.id,
            ctx.reviewingId,
        ])

        expect(result.code).toBe(0)
        expect(result.message || '').toMatch(/成功提交2条/)
        expect(result.message || '').toMatch(/忽略1条/)

        for (const id of [d1.id, d2.id]) {
            const detail = await getVoucherDetail(request, ctx.headers, id)
            expect(detail.status).toBe('reviewing')
        }
    })

    test('TC-EXC-006: batch post skips invalid ids and posts valid only', async ({request}) => {
        test.skip(!ctx.postableId || !ctx.reviewingId || !ctx.postedId, '前置凭证缺失')

        const before = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            ctx.debitCode,
        )

        const fakeId = '9999999999999999999'
        const result = await tryBatchPostVoucher(request, ctx.headers, [
            ctx.postableId,
            ctx.reviewingId,
            ctx.postedId,
            fakeId,
        ])

        expect(result.code).toBe(0)
        expect(result.message || '').toMatch(/成功：1/)
        expect(result.message || '').toMatch(/失败：/)

        const postableDetail = await getVoucherDetail(request, ctx.headers, ctx.postableId)
        expect(postableDetail.senderId).toBeTruthy()

        const reviewingDetail = await getVoucherDetail(request, ctx.headers, ctx.reviewingId)
        expect(reviewingDetail.senderId).toBeFalsy()

        const postedDetail = await getVoucherDetail(request, ctx.headers, ctx.postedId)
        expect(postedDetail.senderId).toBeTruthy()

        const after = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            ctx.debitCode,
        )
        expect(after - before).toBeCloseTo(21, 2)
    })

    test('TC-EXC-001: duplicate post does not double subject balance', async ({request}) => {
        test.skip(!ctx.postedId, '无已过账凭证')

        const before = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            ctx.debitCode,
        )

        const duplicate = await tryPostVoucher(request, ctx.headers, ctx.postedId)
        expect(duplicate.code).not.toBe(0)

        const after = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            ctx.debitCode,
        )
        expect(after).toBeCloseTo(before, 2)
    })

    test('TC-EXC-007 partial: duplicate id in batch post is idempotent', async ({request}) => {
        test.skip(!ctx.postedId, '无已过账凭证')

        const before = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            ctx.debitCode,
        )

        const result = await tryBatchPostVoucher(request, ctx.headers, [
            ctx.postedId,
            ctx.postedId,
        ])
        expect(result.code).not.toBe(0)

        const after = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            ctx.debitCode,
        )
        expect(after).toBeCloseTo(before, 2)
    })

    test('TC-VCH-055: batch post multiple completed vouchers', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')

        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {prepaid, expense} = pickStandardBusinessSubjects(subjects)
        test.skip(!prepaid || !expense, '缺少预付/费用科目')
        const pair = {debit: prepaid!, credit: expense!}

        const mkCompleted = async (summary: string, amount: number) => {
            const payload = await buildBalancedVoucherPayload(
                request, ctx.headers, ctx.bookId, summary, amount, pair,
            )
            const id = await createDraftVoucher(request, ctx.headers, payload)
            await submitVoucher(request, ctx.headers, payload, id)
            await auditVoucher(request, ctx.headers, id)
            return {id, amount}
        }

        const before = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            ctx.debitCode,
        )

        const c1 = await mkCompleted('批量过账-A', 30)
        const c2 = await mkCompleted('批量过账-B', 31)
        const result = await tryBatchPostVoucher(request, ctx.headers, [c1.id, c2.id])

        expect(result.code).toBe(0)
        expect(result.message || '').toMatch(/成功：2/)

        const after = getSubjectBalance(
            await fetchSubjectBalances(request, ctx.headers, ctx.term),
            ctx.debitCode,
        )
        expect(after - before).toBeCloseTo(c1.amount + c2.amount, 2)
    })
})
