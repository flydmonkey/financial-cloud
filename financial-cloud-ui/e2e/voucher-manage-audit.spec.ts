import {expect, test} from '@playwright/test'
import {getCurrentUser, loginViaApi} from './helpers/auth'
import {
    auditVoucher,
    buildBalancedVoucherPayload,
    createDraftVoucher,
    ensureVoucherReviewEnabled,
    getVoucherDetail,
    submitVoucher,
    tryManageAuditVoucher,
} from './helpers/voucher'

/** TC-VCH-037：主管复核 */
test.describe.serial('voucher manage audit', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        completedId: string
        draftId: string
    } = {
        headers: {},
        bookId: '',
        completedId: '',
        draftId: '',
    }

    test('login and prepare completed voucher', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)

        const completedPayload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '主管复核-completed', 21,
        )
        ctx.completedId = await createDraftVoucher(request, ctx.headers, completedPayload)
        await submitVoucher(request, ctx.headers, completedPayload, ctx.completedId)
        await auditVoucher(request, ctx.headers, ctx.completedId)

        const draftPayload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '主管复核-draft', 22,
        )
        ctx.draftId = await createDraftVoucher(request, ctx.headers, draftPayload)
    })

    test('TC-VCH-037: manage-audit writes manager fields on completed voucher', async ({request}) => {
        test.skip(!ctx.completedId, '无 completed 凭证')
        const before = await getVoucherDetail(request, ctx.headers, ctx.completedId)
        expect(before.status).toBe('completed')
        expect(before.managerName).toBeFalsy()

        const result = await tryManageAuditVoucher(request, ctx.headers, ctx.completedId)
        expect(result.code, result.message || 'manage-audit failed').toBe(0)
        expect(result.message || '').toMatch(/成功：1/)

        const after = await getVoucherDetail(request, ctx.headers, ctx.completedId)
        expect(after.status).toBe('completed')
        expect(after.managerName).toBeTruthy()
        expect(after.managerId).toBeTruthy()
        expect(after.managerDate).toBeTruthy()
    })

    test('TC-VCH-037: manage-audit rejects draft voucher', async ({request}) => {
        test.skip(!ctx.draftId, '无 draft 凭证')
        const result = await tryManageAuditVoucher(request, ctx.headers, ctx.draftId)
        expect(result.code).not.toBe(0)
        expect(result.message || '').toMatch(/没有可以主管复核/)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.draftId)
        expect(detail.managerName).toBeFalsy()
    })
})
