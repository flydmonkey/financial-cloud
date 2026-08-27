import {expect, test} from '@playwright/test'
import {assertBalanceSheetTrial} from './helpers/reports'
import {getCurrentTerm, getCurrentUser, loginViaApi, loginViaUi} from './helpers/auth'
import {
    auditVoucher,
    buildBalancedVoucherPayload,
    createDraftVoucher,
    ensureVoucherReviewEnabled,
    fixVoucherNumbering,
    getVoucherDetail,
    postVoucher,
    submitVoucher,
    type VoucherPayload,
} from './helpers/voucher'

test.describe.serial('accounting lifecycle', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        voucherId: string
        payload: VoucherPayload | null
        closedTerm: string
    } = {
        headers: {},
        bookId: '',
        term: '',
        voucherId: '',
        payload: null,
        closedTerm: '',
    }

    test('login and prepare book with voucher review enabled', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套，请先完成 onboarding 或登录有效账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)
    })

    test('draft balanced voucher entry', async ({request}) => {
        ctx.payload = await buildBalancedVoucherPayload(
            request,
            ctx.headers,
            ctx.bookId,
            'E2E全流程凭证',
            100,
        )
        ctx.voucherId = await createDraftVoucher(request, ctx.headers, ctx.payload)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.status).toBe('draft')
    })

    test('submit voucher for audit', async ({request}) => {
        test.skip(!ctx.payload || !ctx.voucherId, '前置凭证未创建')
        await submitVoucher(request, ctx.headers, ctx.payload!, ctx.voucherId)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.status).toBe('reviewing')
    })

    test('audit voucher', async ({request}) => {
        test.skip(!ctx.voucherId, '前置凭证未创建')
        await auditVoucher(request, ctx.headers, ctx.voucherId)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.status).toBe('completed')
        expect(detail.auditMemberName).toBeTruthy()
    })

    test('post voucher (过账)', async ({request}) => {
        test.skip(!ctx.voucherId, '前置凭证未创建')
        await postVoucher(request, ctx.headers, ctx.voucherId)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.voucherId)
        expect(detail.senderName).toBeTruthy()
    })

    test('settlement verify (结账试算) passes', async ({request}) => {
        await fixVoucherNumbering(request, ctx.headers)
        const res = await request.get('/api/settlement/verify', {headers: ctx.headers})
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code, body.message || 'verify failed').toBe(0)
        const checks = body.data || []
        expect(checks.length).toBeGreaterThan(0)
        for (const item of checks) {
            expect(item.result, `结账检查未通过: ${item.item}`).toBeTruthy()
        }
    })

    test('balance sheet trial before checkout', async ({request}) => {
        const res = await request.get(
            `/api/statement/balance-sheet?periodType=month&reportDate=${ctx.term}`,
            {headers: ctx.headers},
        )
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code).toBe(0)
        assertBalanceSheetTrial(
            body.data?.items?.assets || [],
            body.data?.items?.liability || [],
        )
    })

    test('period checkout (期末结账)', async ({request}) => {
        ctx.closedTerm = ctx.term
        const year = ctx.term.slice(0, 4)
        const res = await request.get(`/api/settlement/checkout?year=${year}`, {headers: ctx.headers})
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code, body.message || 'checkout failed').toBe(0)

        const nextTerm = await getCurrentTerm(request, ctx.headers, ctx.bookId)
        expect(nextTerm).not.toBe(ctx.closedTerm)
    })

    test('balance sheet for closed period', async ({request}) => {
        const res = await request.get(
            `/api/statement/balance-sheet?periodType=month&reportDate=${ctx.closedTerm}`,
            {headers: ctx.headers},
        )
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code).toBe(0)
        assertBalanceSheetTrial(
            body.data?.items?.assets || [],
            body.data?.items?.liability || [],
        )
    })

    test('income statement for closed period', async ({request}) => {
        const res = await request.get(
            `/api/statement/income?periodType=month&reportDate=${ctx.closedTerm}`,
            {headers: ctx.headers},
        )
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code).toBe(0)
        const items = body.data?.items || []
        expect(items.length).toBeGreaterThan(0)
    })

    test('carry-forward list loads after checkout', async ({request}) => {
        const res = await request.get(
            '/api/settlementcarry/fetchcarry?pageNumber=1&pageSize=10',
            {headers: ctx.headers},
        )
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code).toBe(0)
    })

    test('UI: voucher and settlement pages render', async ({page}) => {
        await loginViaUi(page)
        for (const path of [
            '/voucher/voucher-index',
            '/voucher/voucher-edit',
            '/settlement/settle-period',
            '/statement/balance-sheet',
        ]) {
            await page.goto(path)
            await expect(page.locator('.app-container').first()).toBeVisible({timeout: 15_000})
        }
    })
})
