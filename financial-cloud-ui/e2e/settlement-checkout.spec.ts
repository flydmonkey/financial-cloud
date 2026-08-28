import {expect, test} from '@playwright/test'
import {getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    checkoutCurrentPeriod,
    countClosedSettlements,
    fetchSettlementRecords,
    setCurrentTerm,
    tryCheckoutCurrentPeriod,
    verifySettlement,
} from './helpers/settlement'
import {
    createAndPostVoucher,
    fixVoucherNumbering,
} from './helpers/voucher'
import {assertIncomeFormulaChain, fetchIncomeStatement} from './helpers/reports'

/**
 * TC-SET-005：重复结账同一期间不产生重复结账记录
 */
test.describe.serial('settlement checkout guards', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        closedTerm: string
        year: string
    } = {
        headers: {},
        bookId: '',
        closedTerm: '',
        year: '',
    }

    test('login and close first period', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '结账守卫-凭证', 50,
        )
        await fixVoucherNumbering(request, ctx.headers)
        await verifySettlement(request, ctx.headers)

        const {closedTerm} = await checkoutCurrentPeriod(request, ctx.headers, ctx.bookId)
        ctx.closedTerm = closedTerm
        ctx.year = closedTerm.slice(0, 4)

        const records = await fetchSettlementRecords(request, ctx.headers, ctx.year)
        expect(countClosedSettlements(records, closedTerm)).toBe(1)

        const income = await fetchIncomeStatement(request, ctx.headers, closedTerm)
        assertIncomeFormulaChain(income?.items || [])
    })

    test('TC-SET-005: closed period rejects duplicate checkout', async ({request}) => {
        test.skip(!ctx.closedTerm, '无已结账期间')

        const nextOpenTerm = await getCurrentTerm(request, ctx.headers, ctx.bookId)
        expect(nextOpenTerm).not.toBe(ctx.closedTerm)

        // 回拨当前账期到已结账月，验证重复结账被拒绝
        await setCurrentTerm(request, ctx.headers, ctx.closedTerm)
        expect(await getCurrentTerm(request, ctx.headers, ctx.bookId)).toBe(ctx.closedTerm)

        const retryClosed = await tryCheckoutCurrentPeriod(request, ctx.headers, ctx.bookId)
        expect(retryClosed.code, retryClosed.message || 'duplicate checkout should fail').not.toBe(0)
        expect(retryClosed.message || '').toMatch(/已结账/)

        const recordsAfterReject = await fetchSettlementRecords(request, ctx.headers, ctx.year)
        expect(countClosedSettlements(recordsAfterReject, ctx.closedTerm)).toBe(1)

        // 恢复到下一开放账期，并完成一次正常结账（仍保持已结账月仅 1 条）
        await setCurrentTerm(request, ctx.headers, nextOpenTerm)
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '结账守卫-下期凭证', 30,
        )
        await fixVoucherNumbering(request, ctx.headers)
        await verifySettlement(request, ctx.headers)

        const retry = await tryCheckoutCurrentPeriod(request, ctx.headers, ctx.bookId)
        expect(retry.code).toBe(0)

        const records = await fetchSettlementRecords(request, ctx.headers, ctx.year)
        expect(countClosedSettlements(records, ctx.closedTerm)).toBe(1)
        expect(countClosedSettlements(records, nextOpenTerm)).toBe(1)
    })
})
