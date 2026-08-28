import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    CashFlowItems,
    ensureCashFlowConfigInitialized,
    fetchCashFlowConfigItems,
    getCashFlowTotals,
    getPendingCashFlowItems,
    saveCashFlowConfigItemBalance,
    specifyCashFlowForItem,
} from './helpers/cash-flow'
import {
    assertReportsBalanced,
    captureReportSnapshot,
    fetchBalanceSheet,
    fetchCashFlowStatement,
    fetchSubjectBalances,
    findBalanceSheetItemByName,
    findCashFlowItem,
    getSubjectBalance,
    num,
} from './helpers/reports'
import {
    auditVoucher,
    buildBalancedVoucherPayload,
    createAndPostVoucher,
    createDraftVoucher,
    ensureVoucherReviewEnabled,
    postVoucher,
    submitVoucher,
    pickStandardBusinessSubjects,
} from './helpers/voucher'

/**
 * TC-RPT-020~024：现金流量表勾稽
 */
test.describe.serial('cash flow reconciliation', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        baseline: Awaited<ReturnType<typeof captureReportSnapshot>> | null
        cashVoucherId: string
    } = {
        headers: {},
        bookId: '',
        term: '',
        baseline: null,
        cashVoucherId: '',
    }

    test('login and capture baseline', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)
        await ensureCashFlowConfigInitialized(request, auth.headers, user.bookId)
        ctx.baseline = await captureReportSnapshot(request, ctx.headers, ctx.term)
    })

    test('TC-RPT-025: cash flow config balance affects statement', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const itemCode = '4-jy-sdqt'
        const delta = 6666

        await ensureCashFlowConfigInitialized(request, ctx.headers, ctx.bookId)

        const beforeItems = await fetchCashFlowStatement(request, ctx.headers, ctx.term)
        const beforeAmount = num(findCashFlowItem(beforeItems, itemCode)?.currentAmount)

        const currentConfig = await fetchCashFlowConfigItems(request, ctx.headers)
        const row = currentConfig.find((item) => item.itemCode === itemCode)
        const originalBalance = num(row?.balance)
        const nextBalance = originalBalance + delta
        await saveCashFlowConfigItemBalance(request, ctx.headers, itemCode, nextBalance)

        const afterItems = await fetchCashFlowStatement(request, ctx.headers, ctx.term)
        const afterAmount = num(findCashFlowItem(afterItems, itemCode)?.currentAmount)
        test.info().annotations.push({
            type: 'note',
            description: `${itemCode} currentAmount ${beforeAmount} → ${afterAmount}`,
        })
        expect(afterAmount - beforeAmount).toBeCloseTo(delta, 0)

        // 恢复配置，避免污染后续 TC-RPT-021 勾稽
        await saveCashFlowConfigItemBalance(request, ctx.headers, itemCode, originalBalance)
    })

    test('TC-RPT-023: non-cash transfer does not affect cash flow totals', async ({request}) => {
        test.skip(!ctx.baseline, '无基线')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {receivable, prepaid} = pickStandardBusinessSubjects(subjects)
        test.skip(!receivable || !prepaid, '缺少应收/预付科目')

        const before = await getCashFlowTotals(request, ctx.headers, ctx.term)
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'E2E非现金转账', 50,
            {debit: receivable, credit: prepaid},
        )
        const after = await getCashFlowTotals(request, ctx.headers, ctx.term)
        expect(after.netIncrease).toBeCloseTo(before.netIncrease, 2)
        expect(after.endingCash).toBeCloseTo(before.endingCash, 2)
    })

    test('TC-RPT-022/024: bank receipt with specified cash flow item', async ({request}) => {
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, revenue} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !revenue, '缺少银行/收入科目')

        const payload = await buildBalancedVoucherPayload(
            request, ctx.headers, ctx.bookId, 'E2E现金收款', 777,
            {debit: bank, credit: revenue},
        )
        ctx.cashVoucherId = await createDraftVoucher(request, ctx.headers, payload)
        await submitVoucher(request, ctx.headers, payload, ctx.cashVoucherId)
        await auditVoucher(request, ctx.headers, ctx.cashVoucherId)

        const pending = await getPendingCashFlowItems(
            request, ctx.headers, ctx.term, {voucherId: ctx.cashVoucherId, cashFlowItemType: 0},
        )
        const flowLine = pending.find(
            (item) =>
                item.voucherId === ctx.cashVoucherId &&
                !/^(1001|1002|1003)/.test(item.subjectCode || '') &&
                (num(item.creditAmount) > 0 || num(item.debitAmount) > 0),
        )
        test.skip(!flowLine?.voucherItemId, '未找到非现金科目现金流量待指定项')

        const specifyResult = await specifyCashFlowForItem(
            request, ctx.headers, ctx.bookId, ctx.term, flowLine,
            CashFlowItems.SALES_RECEIPT,
            0,
        )
        expect(specifyResult.code, specifyResult.message || 'specify cash flow failed').toBe(0)

        const beforeItems = await fetchCashFlowStatement(request, ctx.headers, ctx.term)
        const beforeSales = num(findCashFlowItem(beforeItems, CashFlowItems.SALES_RECEIPT)?.monthlyAmount)

        await postVoucher(request, ctx.headers, ctx.cashVoucherId)

        const afterItems = await fetchCashFlowStatement(request, ctx.headers, ctx.term)
        const afterSales = num(findCashFlowItem(afterItems, CashFlowItems.SALES_RECEIPT)?.monthlyAmount)
        expect(afterSales - beforeSales).toBeCloseTo(777, 0)
    })

    test('TC-RPT-020: operating + investing + financing = net increase', async ({request}) => {
        const totals = await getCashFlowTotals(request, ctx.headers, ctx.term)
        const sum = totals.operatingNet + totals.investingNet + totals.financingNet
        test.info().annotations.push({
            type: 'note',
            description: `经营${totals.operatingNet}+投资${totals.investingNet}+筹资${totals.financingNet}=${sum}, 净增${totals.netIncrease}`,
        })
        if (Math.abs(totals.netIncrease) > 0.01 || Math.abs(sum) > 0.01) {
            expect(Math.abs(sum - totals.netIncrease)).toBeLessThanOrEqual(0.02)
        }
    })

    test('TC-RPT-021: ending cash aligns with balance sheet monetary funds', async ({request}) => {
        await assertReportsBalanced(request, ctx.headers, ctx.term)
        const totals = await getCashFlowTotals(request, ctx.headers, ctx.term)
        const items = await fetchCashFlowStatement(request, ctx.headers, ctx.term)
        expect(findCashFlowItem(items, CashFlowItems.ENDING_CASH), '缺少期末现金行').toBeTruthy()

        const balanceSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const monetary = findBalanceSheetItemByName(balanceSheet?.items?.assets || [], '货币资金')
        const monetaryFunds = num(monetary?.currentBalance)
        const monetaryOpening = num(monetary?.initialBalance)
        const subjectBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        const cashSubjects = getSubjectBalance(subjectBalances, '1001') + getSubjectBalance(subjectBalances, '1002')
        // 期初行未必写入 monthlyAmount；用 期末-净增加 反推报表所用期初
        const impliedBeginning = totals.endingCash - totals.netIncrease
        test.info().annotations.push({
            type: 'note',
            description: `现金流 隐含期初=${impliedBeginning}+净增=${totals.netIncrease}=期末${totals.endingCash}; 货币资金=${monetaryFunds}(年初${monetaryOpening}), 现金科目=${cashSubjects}`,
        })
        expect(monetary, '资产负债表缺少货币资金行').toBeTruthy()
        expect(monetaryFunds).toBeCloseTo(cashSubjects, 2)
        // 有现金业务后，货币资金应随过账变动；期初勾稽仅在期初行有值时检查
        if (Math.abs(monetaryOpening) > 0.01 && Math.abs(impliedBeginning) > 0.01) {
            expect(impliedBeginning).toBeCloseTo(monetaryOpening, 2)
        }
        // 本文件置于 opening 之后、其它污染性业务之前，跨表应严格一致
        expect(totals.endingCash).toBeCloseTo(monetaryFunds, 2)
    })
})
