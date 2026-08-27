import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {checkoutCurrentPeriod} from './helpers/settlement'
import {
    auditVoucher,
    buildBalancedVoucherPayload,
    createDraftVoucher,
    ensureVoucherReviewEnabled,
    getVoucherDetail,
    postVoucher,
    submitVoucher,
    tryAuditVoucher,
    tryCancelVoucher,
    tryDeleteVoucher,
    tryPostVoucher,
    trySubmitVoucher,
    tryUnauditVoucher,
    tryUpdateVoucher,
    updateVoucher,
    voucherDetailToPayload,
} from './helpers/voucher'

/**
 * TC-VCH-031~045, 051, 054, TC-EXC-002：凭证状态拦截
 */
test.describe.serial('voucher state guards', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        draftId: string
        reviewingId: string
        completedId: string
        postedId: string
        closedTerm: string
    } = {
        headers: {},
        bookId: '',
        term: '',
        draftId: '',
        reviewingId: '',
        completedId: '',
        postedId: '',
        closedTerm: '',
    }

    test('login and prepare vouchers in each state', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)

        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        test.skip(subjects.length < 2, '科目不足')

        const mk = async (summary: string, amount: number) => {
            const payload = await buildBalancedVoucherPayload(
                request, ctx.headers, ctx.bookId, summary, amount,
            )
            return {payload, id: await createDraftVoucher(request, ctx.headers, payload)}
        }

        const d1 = await mk('守卫-draft', 11)
        ctx.draftId = d1.id

        const d2 = await mk('守卫-reviewing', 12)
        await submitVoucher(request, ctx.headers, d2.payload, d2.id)
        ctx.reviewingId = d2.id

        const d3 = await mk('守卫-completed', 13)
        await submitVoucher(request, ctx.headers, d3.payload, d3.id)
        await auditVoucher(request, ctx.headers, d3.id)
        ctx.completedId = d3.id

        const d4 = await mk('守卫-posted', 14)
        await submitVoucher(request, ctx.headers, d4.payload, d4.id)
        await auditVoucher(request, ctx.headers, d4.id)
        await postVoucher(request, ctx.headers, d4.id)
        ctx.postedId = d4.id
    })

    test('TC-VCH-032: draft cannot be audited', async ({request}) => {
        test.skip(!ctx.draftId, '无 draft 凭证')
        const result = await tryAuditVoucher(request, ctx.headers, ctx.draftId)
        expect(result.message || '').toMatch(/失败|成功：0/)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.draftId)
        expect(detail.status).toBe('draft')
    })

    test('TC-VCH-031: duplicate audit ignored', async ({request}) => {
        test.skip(!ctx.completedId, '无 completed 凭证')
        const before = await getVoucherDetail(request, ctx.headers, ctx.completedId)
        const result = await tryAuditVoucher(request, ctx.headers, ctx.completedId)
        expect(result.code).toBe(0)
        expect(result.message || '').toMatch(/失败：1|失败：0/)
        const after = await getVoucherDetail(request, ctx.headers, ctx.completedId)
        expect(after.status).toBe(before.status)
    })

    test('TC-VCH-023: submitted voucher cannot be resubmitted', async ({request}) => {
        test.skip(!ctx.reviewingId, '无 reviewing 凭证')
        const detail = await getVoucherDetail(request, ctx.headers, ctx.reviewingId)
        const payload = voucherDetailToPayload(detail)
        const result = await trySubmitVoucher(request, ctx.headers, payload, ctx.reviewingId)
        expect(result.code).not.toBe(0)
        expect(result.message || '').toMatch(/已提交|不允许/)
    })

    test('TC-VCH-051: reviewing cannot be posted', async ({request}) => {
        test.skip(!ctx.reviewingId, '无 reviewing 凭证')
        const result = await tryPostVoucher(request, ctx.headers, ctx.reviewingId)
        expect(result.code).not.toBe(0)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.reviewingId)
        expect(detail.senderId).toBeFalsy()
    })

    test('TC-VCH-033: completed unaudited returns to reviewing', async ({request}) => {
        const payload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '守卫-unaudit', 16,
        )
        const id = await createDraftVoucher(request, ctx.headers, payload)
        await submitVoucher(request, ctx.headers, payload, id)
        await auditVoucher(request, ctx.headers, id)

        const result = await tryUnauditVoucher(request, ctx.headers, id)
        expect(result.code).toBe(0)
        expect(result.message || '').toMatch(/成功：1/)

        const detail = await getVoucherDetail(request, ctx.headers, id)
        expect(detail.status).toBe('reviewing')
        expect(detail.senderId).toBeFalsy()
        expect(detail.auditMemberName).toBeFalsy()
    })

    test('TC-VCH-034: posted cannot be unaudited', async ({request}) => {
        test.skip(!ctx.postedId, '无 posted 凭证')
        const result = await tryUnauditVoucher(request, ctx.headers, ctx.postedId)
        expect(result.code).not.toBe(0)
    })

    test('TC-VCH-043: completed cannot be deleted', async ({request}) => {
        test.skip(!ctx.completedId, '无 completed 凭证')
        const result = await tryDeleteVoucher(request, ctx.headers, ctx.completedId)
        expect(result.code).not.toBe(0)
    })

    test('TC-VCH-042: draft can be deleted', async ({request}) => {
        const payload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '守卫-待删', 9,
        )
        const id = await createDraftVoucher(request, ctx.headers, payload)
        const result = await tryDeleteVoucher(request, ctx.headers, id)
        expect(result.code).toBe(0)
    })

    test('TC-EXC-002: posted voucher cannot be updated via API', async ({request}) => {
        test.skip(!ctx.postedId, '无 posted 凭证')
        const result = await tryUpdateVoucher(request, ctx.headers, ctx.postedId, (p) => {
            p.items[0].debitAmount = 9999
            p.items[1].creditAmount = 9999
        })
        expect(result.code).not.toBe(0)
    })

    test('TC-VCH-035: cancel reviewing back to draft', async ({request}) => {
        const payload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '守卫-cancel', 15,
        )
        const id = await createDraftVoucher(request, ctx.headers, payload)
        await submitVoucher(request, ctx.headers, payload, id)
        const result = await tryCancelVoucher(request, ctx.headers, id)
        expect(result.code).toBe(0)
        const detail = await getVoucherDetail(request, ctx.headers, id)
        expect(detail.status).toBe('draft')
    })

    test('TC-VCH-040: draft voucher can be updated', async ({request}) => {
        test.skip(!ctx.draftId, '无 draft 凭证')
        await updateVoucher(request, ctx.headers, ctx.draftId, (p) => {
            p.items[0].debitAmount = 22
            p.items[1].creditAmount = 22
        })
        const detail = await getVoucherDetail(request, ctx.headers, ctx.draftId)
        expect(Number(detail.debitAmount)).toBe(22)
    })

    test('TC-VCH-041: reviewing voucher can be updated', async ({request}) => {
        test.skip(!ctx.reviewingId, '无 reviewing 凭证')
        await updateVoucher(request, ctx.headers, ctx.reviewingId, (p) => {
            p.items[0].summary = '守卫-reviewing-已改'
        })
        const detail = await getVoucherDetail(request, ctx.headers, ctx.reviewingId)
        expect(detail.items[0].summary).toBe('守卫-reviewing-已改')
    })

    test('TC-VCH-044: posted voucher cannot be updated', async ({request}) => {
        test.skip(!ctx.postedId, '无 posted 凭证')
        const result = await tryUpdateVoucher(request, ctx.headers, ctx.postedId, (p) => {
            p.items[0].debitAmount = 8888
            p.items[1].creditAmount = 8888
        })
        expect(result.code).not.toBe(0)
    })

    test('TC-VCH-054: cannot unpost in closed period', async ({request}) => {
        test.skip(!ctx.postedId, '无 posted 凭证')
        ctx.closedTerm = ctx.term
        await checkoutCurrentPeriod(request, ctx.headers, ctx.bookId)
        const unpost = await request.put(`/api/voucher/unsender/${ctx.postedId}`, {headers: ctx.headers})
        const body = await unpost.json()
        expect(body.code).not.toBe(0)
    })

    test('TC-VCH-045: closed period blocks voucher update', async ({request}) => {
        test.skip(!ctx.postedId || !ctx.closedTerm, '无 posted 凭证或未结账')
        const result = await tryUpdateVoucher(request, ctx.headers, ctx.postedId, (p) => {
            p.items[0].summary = '结账后修改'
        })
        expect(result.code).not.toBe(0)
    })
})
