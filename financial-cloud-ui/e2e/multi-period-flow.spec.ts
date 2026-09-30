import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    CashFlowItems,
    createAndPostVoucherWithMainCashFlow,
    ensureCashFlowConfigInitialized,
    getCashFlowTotals,
} from './helpers/cash-flow'
import {
    assertCashFlowReconciliation,
    assertReportsBalanced,
    fetchBalanceSheet,
    fetchCashFlowStatement,
    fetchSubjectBalances,
    findBalanceSheetItemByName,
    findCashFlowItem,
    getBalanceSheetTotals,
    getIncomeNetProfit,
    getSubjectBalance,
    num,
} from './helpers/reports'
import {checkoutCurrentPeriod, verifySettlement} from './helpers/settlement'
import {
    createAndPostVoucher,
    ensureVoucherReviewEnabled,
    fixVoucherNumbering,
    pickReconciliationSubjects,
    pickStandardBusinessSubjects,
} from './helpers/voucher'

/**
 * TC-E2E-003：多期连续做账
 * 第 1 期：录凭证 → 过账 → 结账
 * 第 2 期：录凭证 → 过账 → 验证利润表累计滚动 + 资产负债表平衡 + 现金流跨期衔接
 */
test.describe.serial('multi-period accounting flow', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        period1Term: string
        period2Term: string
        period1Amount: number
        period2Amount: number
        period1Income: {current: number; cumulative: number} | null
        period1Balance: {assetTotal: number | null; liabilityTotal: number | null} | null
        period1BankBalance: number | null
        period1CashEnding: number | null
        period1Inventory: number | null
        period2InventoryPurchase: number
    } = {
        headers: {},
        bookId: '',
        period1Term: '',
        period2Term: '',
        period1Amount: 150,
        period2Amount: 250,
        period1Income: null,
        period1Balance: null,
        period1BankBalance: null,
        period1CashEnding: null,
        period1Inventory: null,
        period2InventoryPurchase: 80,
    }

    test('login and prepare book', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套，请先完成 onboarding 或登录有效账套')
        ctx.bookId = user.bookId
        ctx.period1Term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)
        await ensureCashFlowConfigInitialized(request, auth.headers, user.bookId)
    })

    test('period 1: create voucher, post and checkout', async ({request}) => {
        test.skip(!ctx.bookId, '账套未就绪')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        test.skip(subjects.length < 2, '账套科目不足')
        const pair = pickReconciliationSubjects(subjects)

        await createAndPostVoucher(
            request,
            ctx.headers,
            ctx.bookId,
            'E2E多期-第1期凭证',
            ctx.period1Amount,
            pair,
        )

        const {bank, rawMaterial} = pickStandardBusinessSubjects(subjects)
        if (bank && rawMaterial) {
            await createAndPostVoucherWithMainCashFlow(
                request, ctx.headers, ctx.bookId, ctx.period1Term,
                'E2E多期-P1采购', 100,
                {debit: rawMaterial, credit: bank},
                CashFlowItems.PURCHASE_PAYMENT,
            )
        }

        await fixVoucherNumbering(request, ctx.headers)
        await verifySettlement(request, ctx.headers)
        await assertReportsBalanced(request, ctx.headers, ctx.period1Term)

        ctx.period1Income = await getIncomeNetProfit(request, ctx.headers, ctx.period1Term)
        ctx.period1Balance = await getBalanceSheetTotals(request, ctx.headers, ctx.period1Term)
        const p1Subjects = await fetchSubjectBalances(request, ctx.headers, ctx.period1Term)
        ctx.period1BankBalance = getSubjectBalance(p1Subjects, '1002')

        const p1Cash = await getCashFlowTotals(request, ctx.headers, ctx.period1Term)
        ctx.period1CashEnding = p1Cash.endingCash

        const p1BalanceSheet = await fetchBalanceSheet(request, ctx.headers, ctx.period1Term)
        const inventoryLine = findBalanceSheetItemByName(p1BalanceSheet?.items?.assets || [], '存货')
        ctx.period1Inventory = inventoryLine ? num(inventoryLine.currentBalance) : null

        const checkout = await checkoutCurrentPeriod(request, ctx.headers, ctx.bookId)
        expect(checkout.closedTerm).toBe(ctx.period1Term)
        ctx.period2Term = checkout.nextTerm
    })

    test('period 1: reports remain balanced after checkout', async ({request}) => {
        test.skip(!ctx.period1Term, '第 1 期未结账')
        await assertReportsBalanced(request, ctx.headers, ctx.period1Term)
    })

    test('TC-RPT-005: period 2 opening bank balance rolls from period 1 closing', async ({request}) => {
        test.skip(ctx.period1BankBalance == null || !ctx.period2Term, '缺少第 1 期银行快照')

        const p2SubjectsBeforeVoucher = await fetchSubjectBalances(request, ctx.headers, ctx.period2Term)
        const p2BankBefore = getSubjectBalance(p2SubjectsBeforeVoucher, '1002')

        test.info().annotations.push({
            type: 'note',
            description: `P1期末1002=${ctx.period1BankBalance}, P2期初1002=${p2BankBefore}`,
        })
        expect(p2BankBefore).toBeCloseTo(ctx.period1BankBalance!, 2)
    })

    test('CF-M01: period 2 cash flow beginning rolls from period 1 ending', async ({request}) => {
        test.skip(ctx.period1CashEnding == null || !ctx.period2Term, '缺少第 1 期现金流快照')

        const p2CashBefore = await getCashFlowTotals(request, ctx.headers, ctx.period2Term)
        test.info().annotations.push({
            type: 'note',
            description: `P1 CF期末=${ctx.period1CashEnding}, P2 CF期初=${p2CashBefore.beginningCash}`,
        })
        expect(p2CashBefore.beginningCash).toBeCloseTo(ctx.period1CashEnding!, 2)
    })

    test('period 2: create voucher in new term', async ({request}) => {
        test.skip(!ctx.period2Term, '第 2 期账期未推进')
        const currentTerm = await getCurrentTerm(request, ctx.headers, ctx.bookId)
        expect(currentTerm).toBe(ctx.period2Term)

        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const pair = pickReconciliationSubjects(subjects)

        await createAndPostVoucher(
            request,
            ctx.headers,
            ctx.bookId,
            'E2E多期-第2期凭证',
            ctx.period2Amount,
            pair,
        )

        const {bank, rawMaterial} = pickStandardBusinessSubjects(subjects)
        if (bank && rawMaterial && ctx.period1Inventory != null) {
            await createAndPostVoucherWithMainCashFlow(
                request, ctx.headers, ctx.bookId, ctx.period2Term,
                'E2E多期-P2采购', ctx.period2InventoryPurchase,
                {debit: rawMaterial, credit: bank},
                CashFlowItems.PURCHASE_PAYMENT,
            )
        }

        await assertReportsBalanced(request, ctx.headers, ctx.period2Term)
    })

    test('CF-M02: period 2 inventory indirect uses prior month closing as opening', async ({request}) => {
        test.skip(ctx.period1Inventory == null || !ctx.period2Term, '缺少存货基线或未进入第 2 期')

        const items = await fetchCashFlowStatement(request, ctx.headers, ctx.period2Term)
        const inventoryChange = num(findCashFlowItem(items, CashFlowItems.INVENTORY_CHANGE)?.monthlyAmount)
        const purchaseOutflow = num(findCashFlowItem(items, CashFlowItems.PURCHASE_PAYMENT)?.monthlyAmount)

        test.info().annotations.push({
            type: 'note',
            description: `P1存货=${ctx.period1Inventory}, P2 53=${inventoryChange}, P2 6=${purchaseOutflow}`,
        })

        if (purchaseOutflow > 0.01) {
            expect(inventoryChange).toBeCloseTo(-ctx.period2InventoryPurchase, 0)
            expect(purchaseOutflow).toBeGreaterThanOrEqual(ctx.period2InventoryPurchase - 0.01)
        }

        assertCashFlowReconciliation(items)
    })

    test('TC-RPT-014: period 2 income cumulative rolls from period 1', async ({request}) => {
        test.skip(!ctx.period1Income || !ctx.period2Term, '缺少第 1 期快照或第 2 期账期')

        const period2Income = await getIncomeNetProfit(request, ctx.headers, ctx.period2Term)
        const expectedCumulative = ctx.period1Income.cumulative + period2Income.current

        test.info().annotations.push({
            type: 'note',
            description: `P1累计=${ctx.period1Income.cumulative}, P2本期=${period2Income.current}, P2累计=${period2Income.cumulative}, 期望累计≈${expectedCumulative}`,
        })

        if (Math.abs(ctx.period1Income.cumulative) > 0.01 || Math.abs(period2Income.current) > 0.01) {
            expect(Math.abs(period2Income.cumulative - expectedCumulative)).toBeLessThanOrEqual(0.02)
        }
        expect(Math.abs(period2Income.cumulative)).toBeGreaterThanOrEqual(
            Math.abs(ctx.period1Income.cumulative),
        )
    })

    test('period 2: balance sheet totals roll forward consistently', async ({request}) => {
        test.skip(!ctx.period1Balance || !ctx.period2Term, '缺少第 1 期资产负债表快照')

        const period2Balance = await getBalanceSheetTotals(request, ctx.headers, ctx.period2Term)

        test.info().annotations.push({
            type: 'note',
            description: `P1资产=${ctx.period1Balance.assetTotal}, P2资产=${period2Balance.assetTotal}`,
        })

        await assertReportsBalanced(request, ctx.headers, ctx.period1Term)
        await assertReportsBalanced(request, ctx.headers, ctx.period2Term)

        if (ctx.period1Balance.assetTotal != null && period2Balance.assetTotal != null) {
            expect(Math.abs(period2Balance.assetTotal! - period2Balance.liabilityTotal!)).toBeLessThanOrEqual(0.01)
        }
    })
})
