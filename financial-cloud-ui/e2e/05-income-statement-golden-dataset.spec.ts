import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {fetchInitBalanceList, saveStandardOpeningBalances} from './helpers/init-balance'
import {
    assertIncomeCarryReconciliation,
    assertIncomeFormulaChain,
    assertIncomeGoldenLines,
    assertReportsBalanced,
    fetchIncomeStatement,
    fetchIncomeStatementResult,
    fetchSubjectBalances,
    getIncomeNetProfit,
    getSubjectBalance,
} from './helpers/reports'
import {generateAndPostCarryByCode} from './helpers/settlement-carry'
import {createAndPostVoucher, pickStandardBusinessSubjects} from './helpers/voucher'

/**
 * IS-G01~G02 / IS-R01：利润表 Golden Dataset
 * 独立套件：空白账套 + 标准期初 100,000，录入多笔损益凭证后逐行勾稽 + 结转勾稽
 * 运行：E2E_RESET_BOOK=1 后 `npm run test:e2e:income-golden`
 */
test.describe.serial('income statement golden dataset', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        profit3103BeforeCarry: number
        netProfitBeforeCarry: number
        pAndLCodes: string[]
    } = {
        headers: {},
        bookId: '',
        term: '',
        profit3103BeforeCarry: 0,
        netProfitBeforeCarry: 0,
        pAndLCodes: ['5001', '5401', '5601', '5602', '5111', '5301', '5711', '5801'],
    }

    test('login and verify blank book', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)

        const rows = await fetchInitBalanceList(request, auth.headers)
        const hasVoucherSubject = rows.some((row) => row.hasVoucher)
        test.skip(hasVoucherSubject, '账套已有凭证，Golden Dataset 需 E2E_RESET_BOOK=1 后单独运行')
    })

    test('IS-G00: save standard opening balances', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        await saveStandardOpeningBalances(request, ctx.headers, ctx.bookId, 100_000)
    })

    test('IS-G00: post golden P&L vouchers', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const s = pickStandardBusinessSubjects(subjects)
        test.skip(!s.revenue || !s.receivable, '缺少 5001/1122')
        test.skip(!s.cost || !s.rawMaterial, '缺少 5401/1403')
        test.skip(!s.salesExpense || !s.expense || !s.bank, '缺少 5601/5602/1002')

        // 营业收入 100,000
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'Golden-确认收入', 100_000,
            {debit: s.receivable, credit: s.revenue},
        )
        // 营业成本 60,000
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'Golden-结转成本', 60_000,
            {debit: s.cost, credit: s.rawMaterial},
        )
        // 销售费用 5,000
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'Golden-销售费用', 5_000,
            {debit: s.salesExpense, credit: s.bank},
        )
        // 管理费用 10,000
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'Golden-管理费用', 10_000,
            {debit: s.expense, credit: s.bank},
        )

        if (s.investmentIncome) {
            await createAndPostVoucher(
                request, ctx.headers, ctx.bookId, 'Golden-投资收益', 2_000,
                {debit: s.bank, credit: s.investmentIncome},
            )
        }
        if (s.nonOpIncome) {
            await createAndPostVoucher(
                request, ctx.headers, ctx.bookId, 'Golden-营业外收入', 1_000,
                {debit: s.bank, credit: s.nonOpIncome},
            )
        }
        if (s.nonOpExpense) {
            await createAndPostVoucher(
                request, ctx.headers, ctx.bookId, 'Golden-营业外支出', 500,
                {debit: s.nonOpExpense, credit: s.bank},
            )
        }
        if (s.incomeTax) {
            await createAndPostVoucher(
                request, ctx.headers, ctx.bookId, 'Golden-所得税', 6_500,
                {debit: s.incomeTax, credit: s.bank},
            )
        }

        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('IS-G01: formula chain and config rules reconciliation', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const income = await fetchIncomeStatement(request, ctx.headers, ctx.term)
        const items = income?.items || []
        const formula = assertIncomeFormulaChain(items)
        ctx.netProfitBeforeCarry = formula.netProfit

        test.info().annotations.push({
            type: 'note',
            description: `Golden 公式链: 收入=${formula.revenue}, 营业利润=${formula.operatingProfit}, 利润总额=${formula.totalProfit}, 净利润=${formula.netProfit}`,
        })

        expect(formula.revenue).toBeCloseTo(100_000, 0)
        expect(Math.abs(formula.netProfit)).toBeGreaterThan(0)

        await assertIncomeGoldenLines(request, ctx.headers, ctx.term, [
            '1', '101', '104', '105', '301',
        ])

        const body = await fetchIncomeStatementResult(request, ctx.headers, ctx.term)
        expect(body.code, body.message || 'Golden 利润表应通过 strict 公式校验').toBe(0)
    })

    test('IS-G02: current period equals year-to-date on first month', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const income = await fetchIncomeStatement(request, ctx.headers, ctx.term)
        const items = income?.items || []
        for (const item of items) {
            const current = Math.abs(Number(item.currentBalance ?? 0))
            const cumulative = Math.abs(Number(item.cumulativeBalance ?? 0))
            if (current < 0.01 && cumulative < 0.01) {
                continue
            }
            expect(cumulative, `itemCode=${item.itemCode} 首月累计应等于本期`).toBeCloseTo(current, 1)
        }
    })

    test('IS-R01: carry-forward reconciles 3103 with net profit', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        test.skip(Math.abs(ctx.netProfitBeforeCarry) < 0.01, '未记录结转前净利润')

        const beforeBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        ctx.profit3103BeforeCarry = getSubjectBalance(beforeBalances, '3103')

        await generateAndPostCarryByCode(request, ctx.headers, 'qm_jz_sr')
        await generateAndPostCarryByCode(request, ctx.headers, 'qm_jz_cbfy')

        await assertIncomeCarryReconciliation(request, ctx.headers, ctx.term, {
            netProfitBeforeCarry: ctx.netProfitBeforeCarry,
            profit3103BeforeCarry: ctx.profit3103BeforeCarry,
            pAndLSubjectCodes: ctx.pAndLCodes,
        })

        const income = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        test.info().annotations.push({
            type: 'note',
            description: `IS-R01 完成: 结转后净利润=${income.current}, 3103=${getSubjectBalance(await fetchSubjectBalances(request, ctx.headers, ctx.term), '3103')}`,
        })
    })
})
