import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {assertReportsBalanced} from './helpers/reports'
import {verifySettlement} from './helpers/settlement'
import {
    buildBalancedVoucherPayload,
    createDraftVoucher,
    ensureVoucherReviewDisabled,
    fixVoucherNumbering,
    getVoucherDetail,
    postVoucher,
    submitVoucher,
    tryUnauditVoucher,
} from './helpers/voucher'

/**
 * TC-E2E-004 / TC-VCH-021：免审核账套全流程
 */
test.describe.serial('no-review voucher flow', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        voucherId: string
        originalReviewSetting: number | null
    } = {
        headers: {},
        bookId: '',
        term: '',
        voucherId: '',
        originalReviewSetting: null,
    }

    test.afterAll(async ({request}) => {
        if (!ctx.bookId || ctx.originalReviewSetting == null) {
            return
        }
        const bookRes = await request.get(`/api/book/get/${ctx.bookId}`, {headers: ctx.headers})
        const book = (await bookRes.json()).data
        await request.put('/api/book/update', {
            headers: ctx.headers,
            data: {...book, voucherReviewed: ctx.originalReviewSetting},
        })
    })

    test('disable voucher review and create voucher', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)

        const bookRes = await request.get(`/api/book/get/${ctx.bookId}`, {headers: ctx.headers})
        ctx.originalReviewSetting = (await bookRes.json()).data?.voucherReviewed ?? 1
        await ensureVoucherReviewDisabled(request, ctx.headers, ctx.bookId)

        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        test.skip(subjects.length < 2, '科目不足')
        const payload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, 'E2E免审核凭证', 66,
        )
        ctx.voucherId = await createDraftVoucher(request, ctx.headers, payload)
    })

    test('submit skips reviewing and goes to completed', async ({request}) => {
        test.skip(!ctx.voucherId, '无凭证')
        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        const payload = {
            bookId: detail.bookId,
            word: detail.word,
            wordHead: detail.wordHead,
            wordNum: detail.wordNum,
            companyName: detail.companyName,
            receiptNum: detail.receiptNum ?? 0,
            voucherDate: detail.voucherDate,
            voucherYear: detail.voucherYear,
            voucherMonth: detail.voucherMonth,
            items: detail.items.map((item: any) => ({
                subjectId: item.subjectId,
                subjectName: item.subjectName,
                summary: item.summary,
                debitAmount: Number(item.debitAmount ?? 0),
                creditAmount: Number(item.creditAmount ?? 0),
            })),
        }
        await submitVoucher(request, ctx.headers, payload, ctx.voucherId)
        const after = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(after.status).toBe('completed')
        expect(after.status).not.toBe('reviewing')
    })

    test('TC-VCH-033: no-review unaudit returns to draft', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const payload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, 'E2E免审核-反审', 67,
        )
        const id = await createDraftVoucher(request, ctx.headers, payload)
        await submitVoucher(request, ctx.headers, payload, id)

        const completed = await getVoucherDetail(request, ctx.headers, id)
        expect(completed.status).toBe('completed')

        const result = await tryUnauditVoucher(request, ctx.headers, id)
        expect(result.code).toBe(0)
        expect(result.message || '').toMatch(/成功：1/)

        const after = await getVoucherDetail(request, ctx.headers, id)
        expect(after.status).toBe('draft')
        expect(after.senderId).toBeFalsy()
    })

    test('post and verify reports without audit step', async ({request}) => {
        test.skip(!ctx.voucherId, '无凭证')
        await postVoucher(request, ctx.headers, ctx.voucherId)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.senderName).toBeTruthy()
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('settlement verify passes with no-review voucher', async ({request}) => {
        await fixVoucherNumbering(request, ctx.headers)
        await verifySettlement(request, ctx.headers)
    })
})
