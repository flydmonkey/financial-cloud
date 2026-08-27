import {expect, test} from '@playwright/test'
import {getCurrentUser, loginViaApi} from './helpers/auth'
import {
    buildBalancedVoucherPayload,
    tryCreateDraftVoucher,
} from './helpers/voucher'

/**
 * TC-VCH-006 / TC-VCH-007：凭证录入校验
 */
test.describe.serial('voucher input validation', () => {
    const ctx: {headers: Record<string, string>; bookId: string} = {
        headers: {},
        bookId: '',
    }

    test('login and prepare', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
    })

    test('TC-VCH-006: missing subject is rejected', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const payload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '校验-缺科目', 10,
        )
        payload.items[0].subjectId = ''
        const result = await tryCreateDraftVoucher(request, ctx.headers, payload)
        expect(result.code).not.toBe(0)
        expect(result.message || '').toContain('存在未选择科目的分录')
    })

    test('TC-VCH-007: zero amount lines are rejected', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const payload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, '校验-零金额', 0,
        )
        payload.items[0].debitAmount = 0
        payload.items[0].creditAmount = 0
        payload.items[1].debitAmount = 0
        payload.items[1].creditAmount = 0
        const result = await tryCreateDraftVoucher(request, ctx.headers, payload)
        expect(result.code).not.toBe(0)
        expect(result.message || '').toMatch(/存在未填写金额的分录|借贷不平衡/)
    })
})
