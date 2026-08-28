import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {CashFlowItems, createAndPostVoucherWithMainCashFlow, ensureCashFlowConfigInitialized} from './helpers/cash-flow'
import {fetchInitBalanceList, saveGoldenBalanceSheetOpeningBalances} from './helpers/init-balance'
import {
    assertBalanceSheetLineMatchesSubjectRulesByCode,
    assertGoldenBalanceSheetLines,
    assertGoldenCashFlowLines,
    fetchBalanceSheet,
    fetchBalanceSheetResult,
    fetchCashFlowStatement,
    findBalanceSheetItemByName,
    findCashFlowItem,
    getIncomeNetProfit,
    num,
} from './helpers/reports'
import {generateAndPostCarryByCode} from './helpers/settlement-carry'
import {pickStandardBusinessSubjects} from './helpers/voucher'

/**
 * TEST-BS-DEEP Golden Dataset：复杂期初 + 逐行勾稽 + 重分类 + 损益结转 + 三表现金流
 * 独立套件：需在空白账套上运行（E2E_RESET_BOOK=1 或手工 clear_books）
 */
test.describe.serial('three-statement golden dataset', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        expected: {
            monetary: number
            advanceReceipt: number
            prepaid: number
            receivable: number
            payable: number
            inventory: number
            fixedAssetNet: number
            capital: number
            undistributedProfit: number
            assetTotal: number
            cashFlow: {
                beginningCash: number
                endingCash: number
                operatingNet: number
                netIncrease: number
                purchasePayment: number
                depreciation: number
                inventoryChange: number
                receivableChange: number
                payableChange: number
                netProfit: number
                other: number
            }
        } | null
    } = {
        headers: {},
        bookId: '',
        term: '',
        expected: null,
    }

    test('login and verify blank book', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureCashFlowConfigInitialized(request, auth.headers, user.bookId)

        const rows = await fetchInitBalanceList(request, ctx.headers)
        const hasVoucherSubject = rows.some((row) => row.hasVoucher)
        test.skip(hasVoucherSubject, '账套已有凭证，Golden Dataset 需 E2E_RESET_BOOK=1 后单独运行')
    })

    test('BS-G01: save golden opening balances', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const result = await saveGoldenBalanceSheetOpeningBalances(
            request, ctx.headers, ctx.bookId,
        )
        ctx.expected = {
            ...result.expected,
            undistributedProfit: 0,
            cashFlow: {
                beginningCash: 100_000,
                endingCash: 100_000,
                operatingNet: 0,
                netIncrease: 0,
                purchasePayment: 0,
                depreciation: 0,
                inventoryChange: 0,
                receivableChange: 0,
                payableChange: 0,
                netProfit: 0,
                other: 0,
            },
        }
    })

    test('BS-G02: golden lines match manual calculation and config rules', async ({request}) => {
        test.skip(!ctx.expected, '未写入 Golden 期初')
        await assertGoldenBalanceSheetLines(request, ctx.headers, ctx.term, [
            {itemName: '货币资金', expected: ctx.expected!.monetary},
            {itemName: '应收账款', expected: ctx.expected!.receivable},
            {itemName: '预收款项', expected: ctx.expected!.advanceReceipt},
            {itemName: '预付款项', expected: ctx.expected!.prepaid},
            {itemName: '应付账款', expected: ctx.expected!.payable},
            {itemName: '存货', expected: ctx.expected!.inventory},
            {itemName: '固定资产', expected: ctx.expected!.fixedAssetNet},
            {itemName: '实收资本', expected: ctx.expected!.capital},
        ])
    })

    test('CF-G01: opening cash flow equals monetary funds', async ({request}) => {
        test.skip(!ctx.expected, '未写入 Golden 期初')
        await assertGoldenCashFlowLines(request, ctx.headers, ctx.term, [
            {itemCode: CashFlowItems.BEGINNING_CASH, label: '期初现金', expected: ctx.expected!.cashFlow.beginningCash},
            {itemCode: CashFlowItems.ENDING_CASH, label: '期末现金', expected: ctx.expected!.cashFlow.endingCash},
            {itemCode: CashFlowItems.OPERATING_NET, label: '经营净额', expected: 0},
            {itemCode: CashFlowItems.INVENTORY_CHANGE, label: '存货减少', expected: 0},
            {itemCode: CashFlowItems.RECEIVABLE_CHANGE, label: '应收减少', expected: 0},
            {itemCode: CashFlowItems.PAYABLE_CHANGE, label: '应付增加', expected: 0},
        ])

        const balanceSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const monetary = findBalanceSheetItemByName(balanceSheet?.items?.assets || [], '货币资金')
        expect(num(monetary?.currentBalance)).toBeCloseTo(ctx.expected!.cashFlow.endingCash, 0)
    })

    test('BS-G04: posted inventory purchase updates 存货 and 货币资金', async ({request}) => {
        test.skip(!ctx.expected, '未写入 Golden 期初')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, rawMaterial} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !rawMaterial, '缺少 1002/1403 科目')

        const amount = 10_000
        await createAndPostVoucherWithMainCashFlow(
            request, ctx.headers, ctx.bookId, ctx.term, 'Golden-采购原材料', amount,
            {debit: rawMaterial, credit: bank},
            CashFlowItems.PURCHASE_PAYMENT,
        )
        ctx.expected = {
            ...ctx.expected!,
            monetary: ctx.expected!.monetary - amount,
            inventory: ctx.expected!.inventory + amount,
            cashFlow: {
                ...ctx.expected!.cashFlow,
                endingCash: ctx.expected!.cashFlow.beginningCash - amount,
                operatingNet: -amount,
                netIncrease: -amount,
                purchasePayment: amount,
                inventoryChange: -amount,
            },
        }
        await assertGoldenBalanceSheetLines(request, ctx.headers, ctx.term, [
            {itemName: '货币资金', expected: ctx.expected.monetary},
            {itemName: '存货', expected: ctx.expected.inventory},
            {itemName: '固定资产', expected: ctx.expected.fixedAssetNet},
            {itemName: '实收资本', expected: ctx.expected.capital},
        ])
    })

    test('CF-G03: purchase payment and inventory indirect adjustment', async ({request}) => {
        test.skip(!ctx.expected, '未执行 BS-G04')
        await assertGoldenCashFlowLines(request, ctx.headers, ctx.term, [
            {itemCode: CashFlowItems.PURCHASE_PAYMENT, label: '购买商品支付', expected: ctx.expected!.cashFlow.purchasePayment},
            {itemCode: CashFlowItems.OPERATING_NET, label: '经营净额', expected: ctx.expected!.cashFlow.operatingNet},
            {itemCode: CashFlowItems.NET_INCREASE, label: '净增加', expected: ctx.expected!.cashFlow.netIncrease},
            {itemCode: CashFlowItems.ENDING_CASH, label: '期末现金', expected: ctx.expected!.cashFlow.endingCash},
            {itemCode: CashFlowItems.INVENTORY_CHANGE, label: '存货减少', expected: ctx.expected!.cashFlow.inventoryChange},
            {itemCode: CashFlowItems.RECEIVABLE_CHANGE, label: '应收减少', expected: 0},
            {itemCode: CashFlowItems.PAYABLE_CHANGE, label: '应付增加', expected: 0},
        ])
    })

    test('BS-G05: posted depreciation updates 固定资产净值', async ({request}) => {
        test.skip(!ctx.expected, '未执行 BS-G04')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {expense, accumulatedDepreciation} = pickStandardBusinessSubjects(subjects)
        test.skip(!expense || !accumulatedDepreciation, '缺少 5602/1602 科目')

        const amount = 5_000
        const {createAndPostVoucher} = await import('./helpers/voucher')
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'Golden-计提折旧', amount,
            {debit: expense, credit: accumulatedDepreciation},
        )
        ctx.expected = {
            ...ctx.expected!,
            fixedAssetNet: ctx.expected!.fixedAssetNet - amount,
            assetTotal: ctx.expected!.assetTotal - amount,
            cashFlow: {
                ...ctx.expected!.cashFlow,
                depreciation: amount,
            },
        }
        await assertBalanceSheetLineMatchesSubjectRulesByCode(
            request, ctx.headers, ctx.term, '1206', ctx.expected.fixedAssetNet,
        )
        const body = await fetchBalanceSheetResult(request, ctx.headers, ctx.term)
        if (body.code === 0) {
            await assertGoldenBalanceSheetLines(
                request, ctx.headers, ctx.term,
                [
                    {itemName: '固定资产', expected: ctx.expected.fixedAssetNet},
                    {itemName: '存货', expected: ctx.expected.inventory},
                    {itemName: '货币资金', expected: ctx.expected.monetary},
                ],
                {skipTrialBalance: true},
            )
        }
    })

    test('CF-G04: depreciation auto-fills indirect line without moving main table', async ({request}) => {
        test.skip(!ctx.expected, '未执行 BS-G05')
        const items = await fetchCashFlowStatement(request, ctx.headers, ctx.term)
        expect(num(findCashFlowItem(items, CashFlowItems.DEPRECIATION)?.monthlyAmount))
            .toBeCloseTo(ctx.expected!.cashFlow.depreciation, 0)
        expect(num(findCashFlowItem(items, CashFlowItems.OPERATING_NET)?.monthlyAmount))
            .toBeCloseTo(ctx.expected!.cashFlow.operatingNet, 0)
        expect(num(findCashFlowItem(items, CashFlowItems.INVENTORY_CHANGE)?.monthlyAmount))
            .toBeCloseTo(ctx.expected!.cashFlow.inventoryChange, 0)
    })

    test('BS-B05: strict mode blocks imbalanced balance sheet', async ({request}) => {
        test.skip(!ctx.expected, '未执行 BS-G05')
        const body = await fetchBalanceSheetResult(request, ctx.headers, ctx.term)
        if (body.code === 513013) {
            expect(body.code).toBe(513013)
            return
        }
        expect(body.code).toBe(0)
        test.info().annotations.push({
            type: 'note',
            description: '非 strict 模式：费用过账后总计行静默调平，跳过 513013 断言',
        })
    })

    test('BS-G06: carry-forward expense restores trial balance via 未分配利润', async ({request}) => {
        test.skip(!ctx.expected, '未执行 BS-G05')
        await generateAndPostCarryByCode(request, ctx.headers, 'qm_jz_cbfy')
        ctx.expected = {
            ...ctx.expected!,
            undistributedProfit: -5_000,
            cashFlow: {
                ...ctx.expected!.cashFlow,
                netProfit: -5_000,
                other: 0,
            },
        }
        await assertGoldenBalanceSheetLines(request, ctx.headers, ctx.term, [
            {itemName: '固定资产', expected: ctx.expected.fixedAssetNet},
            {itemName: '存货', expected: ctx.expected.inventory},
            {itemName: '货币资金', expected: ctx.expected.monetary},
            {itemName: '实收资本', expected: ctx.expected.capital},
            {itemName: '未分配利润', expected: ctx.expected.undistributedProfit},
        ])
    })

    test('CF-G05: indirect operating net reconciles with direct after carry-forward', async ({request}) => {
        test.skip(!ctx.expected, '未执行 BS-G06')
        const incomeNet = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        expect(incomeNet).toBeCloseTo(ctx.expected!.cashFlow.netProfit, 0)

        await assertGoldenCashFlowLines(request, ctx.headers, ctx.term, [
            {itemCode: CashFlowItems.NET_PROFIT, label: '净利润', expected: ctx.expected!.cashFlow.netProfit},
            {itemCode: CashFlowItems.DEPRECIATION, label: '折旧', expected: ctx.expected!.cashFlow.depreciation},
            {itemCode: CashFlowItems.INVENTORY_CHANGE, label: '存货减少', expected: ctx.expected!.cashFlow.inventoryChange},
            {itemCode: CashFlowItems.OTHER, label: '其他', expected: ctx.expected!.cashFlow.other},
            {itemCode: CashFlowItems.OPERATING_NET, label: '主表经营净额', expected: ctx.expected!.cashFlow.operatingNet},
            {itemCode: CashFlowItems.OPERATING_NET_INDIRECT, label: '附表经营净额', expected: ctx.expected!.cashFlow.operatingNet},
            {itemCode: CashFlowItems.ENDING_CASH, label: '期末现金', expected: ctx.expected!.cashFlow.endingCash},
        ], {requireMainEqualsIndirect: true})
    })
})
