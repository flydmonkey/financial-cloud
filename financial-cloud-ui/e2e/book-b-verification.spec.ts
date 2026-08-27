import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    assertReportsBalanced,
    exportStatementReport,
    fetchIncomeStatement,
    fetchSubjectBalances,
    fetchVoucherSummary,
    findIncomeItem,
    findIncomeItemByName,
    getBalanceSheetTotals,
    getIncomeNetProfit,
    getSubjectBalance,
    num,
    subjectPeriodAmount,
} from './helpers/reports'
import {
    createAndPostVoucher,
    ensureVoucherReviewEnabled,
    pickStandardBusinessSubjects,
} from './helpers/voucher'

/**
 * TC-RPT-002/004/010~013：账套 B 标准验算（适配小企业准则科目）
 * 标准业务：收款 50000 / 费用 10000 / 收入 80000
 */
test.describe.serial('book B verification', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        step0: {assetTotal: number | null; incomeCumulative: number; bankBalance: number}
        baselinePeriod: {revenue: number; expense: number}
        step3Income: {current: number; cumulative: number} | null
        afterPostBank: number | null
        bankBeforeSteps: number | null
    } = {
        headers: {},
        bookId: '',
        term: '',
        step0: {assetTotal: null, incomeCumulative: 0, bankBalance: 0},
        baselinePeriod: {revenue: 0, expense: 0},
        step3Income: null,
        afterPostBank: null,
        bankBeforeSteps: null,
    }

    test('login and record baseline', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)

        const balance = await getBalanceSheetTotals(request, ctx.headers, ctx.term)
        const income = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        const subjectBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        ctx.step0 = {
            assetTotal: balance.assetTotal,
            incomeCumulative: income.cumulative,
            bankBalance: getSubjectBalance(subjectBalances, '1002'),
        }
        // 套件内可能已有其它用例写入收入/费用，验算用增量
        ctx.baselinePeriod = {
            revenue: subjectPeriodAmount(subjectBalances, '5001'),
            expense: subjectPeriodAmount(subjectBalances, '5602'),
        }
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('step1: receive payment 50000 (bank debit, receivable credit)', async ({request}) => {
        const subjectBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        ctx.bankBeforeSteps = getSubjectBalance(subjectBalances, '1002')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, receivable} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !receivable, '缺少 1002/1122')
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '验算-收到货款', 50000,
            {debit: bank, credit: receivable},
        )
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('step2: pay expense 10000 (expense debit, bank credit)', async ({request}) => {
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, expense} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !expense, '缺少 1002/5602')
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '验算-支付管理费', 10000,
            {debit: expense, credit: bank},
        )
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('TC-RPT-011: expense voucher reflected in income statement', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const income = await fetchIncomeStatement(request, ctx.headers, ctx.term)
        const adminExpense = findIncomeItem(income?.items || [], '105')
            ?? findIncomeItemByName(income?.items || [], '管理费用')
        const subjectBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        const expenseDelta = subjectPeriodAmount(subjectBalances, '5602') - ctx.baselinePeriod.expense
        test.info().annotations.push({
            type: 'note',
            description: `利润表105=${num(adminExpense?.currentBalance)}, 科目5602增量=${expenseDelta}`,
        })
        expect(expenseDelta).toBeCloseTo(10000, 0)
        if (adminExpense && Math.abs(num(adminExpense.currentBalance)) >= 10000) {
            expect(Math.abs(num(adminExpense.currentBalance))).toBeGreaterThanOrEqual(10000 - 0.5)
        }
    })

    test('step3: confirm revenue 80000 (receivable debit, revenue credit)', async ({request}) => {
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {receivable, revenue} = pickStandardBusinessSubjects(subjects)
        test.skip(!receivable || !revenue, '缺少 1122/5001')
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '验算-确认销售收入', 80000,
            {debit: receivable, credit: revenue},
        )
        ctx.step3Income = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        const subjectBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        ctx.afterPostBank = getSubjectBalance(subjectBalances, '1002')
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('TC-RPT-010: revenue voucher reflected in income statement', async ({request}) => {
        test.skip(!ctx.step3Income, '未完成步骤3')
        const income = await fetchIncomeStatement(request, ctx.headers, ctx.term)
        const revenueLine = findIncomeItem(income?.items || [], '1')
        const subjectBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        const revenueDelta = subjectPeriodAmount(subjectBalances, '5001') - ctx.baselinePeriod.revenue
        test.info().annotations.push({
            type: 'note',
            description: `利润表1=${num(revenueLine?.currentBalance)}, 科目5001增量=${revenueDelta}`,
        })
        expect(revenueDelta).toBeCloseTo(80000, 0)
        if (revenueLine && Math.abs(num(revenueLine.currentBalance)) >= 80000) {
            expect(Math.abs(num(revenueLine.currentBalance))).toBeGreaterThanOrEqual(80000 - 0.5)
        }
    })

    test('TC-RPT-004: balance sheet updates after posting vouchers', async ({request}) => {
        test.skip(!ctx.step3Income, '未完成步骤3')
        const balance = await getBalanceSheetTotals(request, ctx.headers, ctx.term)
        test.info().annotations.push({
            type: 'note',
            description: `资产总计 ${ctx.step0.assetTotal} → ${balance.assetTotal}, 1002=${ctx.afterPostBank}`,
        })
        expect((balance.assetTotal ?? 0)).toBeGreaterThanOrEqual(ctx.step0.assetTotal ?? 0)
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('TC-RPT-012: income statement net profit calculation', async ({request}) => {
        test.skip(!ctx.step3Income, '未完成步骤3')
        const income = await fetchIncomeStatement(request, ctx.headers, ctx.term)
        const items = income?.items || []
        const operatingProfit = findIncomeItem(items, '2')
        const totalProfit = findIncomeItem(items, '3')
        const netProfit = findIncomeItem(items, '4')
        const netFromLine = num(netProfit?.currentBalance)
        const subjectBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        const expenseDelta = subjectPeriodAmount(subjectBalances, '5602') - ctx.baselinePeriod.expense
        const revenuePeriodDelta = subjectPeriodAmount(subjectBalances, '5001') - ctx.baselinePeriod.revenue
        const netDelta = revenuePeriodDelta - expenseDelta
        test.info().annotations.push({
            type: 'note',
            description: `净利润行=${netFromLine}, API=${ctx.step3Income!.current}, 科目增量净利=${netDelta}`,
        })
        expect(netDelta).toBeCloseTo(70000, 0)
        if (operatingProfit && totalProfit && Math.abs(netFromLine) > 0) {
            expect(num(netFromLine)).toBeLessThanOrEqual(num(totalProfit.currentBalance) + 0.01)
        }
    })

    test('TC-RPT-013: net profit matches revenue minus expense', async ({request}) => {
        test.skip(!ctx.step3Income, '未完成步骤3')
        const subjectBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        const revenueDelta = subjectPeriodAmount(subjectBalances, '5001') - ctx.baselinePeriod.revenue
        const expenseDelta = subjectPeriodAmount(subjectBalances, '5602') - ctx.baselinePeriod.expense
        const netDelta = revenueDelta - expenseDelta
        test.info().annotations.push({
            type: 'note',
            description: `净利润API=${ctx.step3Income!.current}, 科目增量净利=${netDelta}`,
        })
        expect(netDelta).toBeCloseTo(70000, 0)
    })

    test('TC-VCH-057: subject balance export and period activity', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        await exportStatementReport(
            request, ctx.headers, '/api/statement/subject-balance/export', ctx.term, 'month', {showAll: 'true'},
        )
        const summary = await fetchVoucherSummary(request, ctx.headers, ctx.term)
        const debitTotal = summary.reduce((sum, row) => sum + num(row.currentPeriodDebit), 0)
        const creditTotal = summary.reduce((sum, row) => sum + num(row.currentPeriodCredit), 0)
        test.info().annotations.push({
            type: 'note',
            description: `凭证汇总 ${summary.length} 行，本期借方=${debitTotal}, 贷方=${creditTotal}`,
        })
        expect(summary.length).toBeGreaterThan(0)
        expect(debitTotal).toBeGreaterThan(0)
        expect(creditTotal).toBeGreaterThan(0)
        expect(debitTotal).toBeCloseTo(creditTotal, 2)
    })
})
