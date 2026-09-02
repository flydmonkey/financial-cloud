import {expect, test} from '@playwright/test'
import {getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    addMonthsToTerm,
    checkoutCurrentPeriod,
    countClosedSettlements,
    fetchSettlementRecords,
    uncheckoutPeriod,
    verifySettlement,
} from './helpers/settlement'
import {
    createAndPostVoucher,
    fixVoucherNumbering,
    unpostVoucher,
} from './helpers/voucher'
import {
    assertBalanceSheetTrial,
    assertIncomeFormulaChain,
    fetchBalanceSheet,
    fetchIncomeStatement,
    fetchSubjectBalances,
    getSubjectBalanceByCodes,
    YEAR_PROFIT_SUBJECT_CODES,
    UNDISTRIBUTED_PROFIT_SUBJECT_CODES,
} from './helpers/reports'

/**
 * P0 反结账：打开最近已结月 → 改账 → 重结 → 勾稽；拒绝路径保持数据不变。
 */
test.describe.serial('settlement uncheckout', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        closedTerm: string
        nextTerm: string
        voucherId: string
    } = {
        headers: {},
        bookId: '',
        closedTerm: '',
        nextTerm: '',
        voucherId: '',
    }

    test('4.1 close then uncheckout restores term', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId

        const created = await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '反结账-业务凭证', 80,
        )
        ctx.voucherId = created.voucherId
        await fixVoucherNumbering(request, ctx.headers)
        await verifySettlement(request, ctx.headers)

        const {closedTerm, nextTerm} = await checkoutCurrentPeriod(
            request, ctx.headers, ctx.bookId,
        )
        ctx.closedTerm = closedTerm
        ctx.nextTerm = nextTerm

        const balancesBefore = await fetchSubjectBalances(request, ctx.headers, nextTerm)
        expect(balancesBefore.length).toBeGreaterThan(0)

        const result = await uncheckoutPeriod(request, ctx.headers, closedTerm)
        expect(result.code, result.message || 'uncheckout failed').toBe(0)

        const termAfter = await getCurrentTerm(request, ctx.headers, ctx.bookId)
        expect(termAfter).toBe(closedTerm)

        const balancesAfter = await fetchSubjectBalances(request, ctx.headers, nextTerm)
        expect(balancesAfter.length).toBe(0)

        const records = await fetchSettlementRecords(
            request, ctx.headers, closedTerm.slice(0, 4),
        )
        expect(countClosedSettlements(records, closedTerm)).toBe(0)

        // 凭证保持过账状态（未级联反过账）
        const voucherRes = await request.get(`/api/voucher/get/${ctx.voucherId}`, {headers: ctx.headers})
        const voucherBody = await voucherRes.json()
        expect(voucherBody.code).toBe(0)
        expect(voucherBody.data?.senderId || voucherBody.data?.senderName).toBeTruthy()
    })

    test('4.3 reject non-adjacent and when next period has voucher', async ({request}) => {
        test.skip(!ctx.closedTerm, '无已反结账上下文')

        // 当前已回到 closedTerm；先再结一次得到 nextTerm，再在 next 录凭证
        await fixVoucherNumbering(request, ctx.headers)
        await verifySettlement(request, ctx.headers)
        const {closedTerm, nextTerm} = await checkoutCurrentPeriod(
            request, ctx.headers, ctx.bookId,
        )
        ctx.closedTerm = closedTerm
        ctx.nextTerm = nextTerm

        const older = addMonthsToTerm(closedTerm, -1)
        const badPeriod = await uncheckoutPeriod(request, ctx.headers, older)
        expect(badPeriod.code).not.toBe(0)
        expect(badPeriod.message || '').toMatch(/只能反结账最近已结期间/)
        expect(await getCurrentTerm(request, ctx.headers, ctx.bookId)).toBe(nextTerm)
        expect(countClosedSettlements(
            await fetchSettlementRecords(request, ctx.headers, closedTerm.slice(0, 4)),
            closedTerm,
        )).toBe(1)

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '反结账-下期阻挡', 20,
        )
        const blocked = await uncheckoutPeriod(request, ctx.headers, closedTerm)
        expect(blocked.code).not.toBe(0)
        expect(blocked.message || '').toMatch(/已有凭证/)
        expect(await getCurrentTerm(request, ctx.headers, ctx.bookId)).toBe(nextTerm)
    })

    test('4.2 / 4.4 re-close after adjust reconciles equity aliases', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')

        // 清掉阻挡凭证：反过账后删除，再反结账
        const listRes = await request.get(
            `/api/voucher/fetch?pageNumber=1&pageSize=50&year=${ctx.nextTerm.slice(0, 4)}&month=${Number(ctx.nextTerm.slice(5, 7))}`,
            {headers: ctx.headers},
        )
        const listBody = await listRes.json()
        const nextVouchers = (listBody.data?.records || []).filter(
            (v: {voucherDate?: string}) => String(v.voucherDate || '').startsWith(ctx.nextTerm),
        )
        for (const v of nextVouchers) {
            if (v.senderId || v.senderName) {
                await unpostVoucher(request, ctx.headers, v.id)
            }
            await request.delete(`/api/voucher/delete/${v.id}`, {headers: ctx.headers})
        }

        const opened = await uncheckoutPeriod(request, ctx.headers, ctx.closedTerm)
        expect(opened.code, opened.message || 'uncheckout for re-close').toBe(0)
        expect(await getCurrentTerm(request, ctx.headers, ctx.bookId)).toBe(ctx.closedTerm)

        await fixVoucherNumbering(request, ctx.headers)
        await verifySettlement(request, ctx.headers)
        await checkoutCurrentPeriod(request, ctx.headers, ctx.bookId)

        const term = ctx.closedTerm
        const bs = await fetchBalanceSheet(request, ctx.headers, term)
        assertBalanceSheetTrial(bs?.items?.assets || [], bs?.items?.liability || [])

        const income = await fetchIncomeStatement(request, ctx.headers, term)
        assertIncomeFormulaChain(income?.items || [])

        const balances = await fetchSubjectBalances(request, ctx.headers, term)
        const yearProfit = getSubjectBalanceByCodes(balances, YEAR_PROFIT_SUBJECT_CODES)
        const undistributed = getSubjectBalanceByCodes(balances, UNDISTRIBUTED_PROFIT_SUBJECT_CODES)
        // 小企业 3103/3104 与企业别名 4103/4104 任一路径有余额即可核对存在性
        expect(Number.isFinite(yearProfit)).toBeTruthy()
        expect(Number.isFinite(undistributed)).toBeTruthy()
    })
})
