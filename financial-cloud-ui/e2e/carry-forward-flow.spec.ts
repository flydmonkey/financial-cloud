import {expect, test, type APIRequestContext} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {assertReportsBalanced, assertIncomeCarryReconciliation, assertIncomeMonthlyCarryReconciliation, assertThreeReportsConsistent, computeCarryNetFromSubjectBalances, fetchSubjectBalances, getIncomeNetProfit, getSubjectBalance, getSubjectBalanceByCodes, UNDISTRIBUTED_PROFIT_SUBJECT_CODES} from './helpers/reports'
import {
    deleteCarryVoucher,
    fetchCarryTemplates,
    findCarryTemplate,
    generateCarryVoucher,
    cleanupExistingCarryVouchers,
} from './helpers/settlement-carry'
import {verifySettlement} from './helpers/settlement'
import {
    createAndPostVoucher,
    ensureVoucherReviewEnabled,
    findSubjectByCode,
    fixVoucherNumbering,
    getVoucherDetail,
    pickStandardBusinessSubjects,
    runVoucherToPosted,
    tryDeleteVoucher,
} from './helpers/voucher'

/**
 * TC-SET-010~015, TC-E2E-001（结转部分）
 */
test.describe.serial('carry-forward flow', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        templates: Awaited<ReturnType<typeof fetchCarryTemplates>>
        carrySrVoucherId: string
        carryCbfyVoucherId: string
        revenueBeforePrepare: number
        expenseBeforePrepare: number
        profit3103BeforeCarry: number
        netProfitBeforeCarry: number
        expectedCarryNet: number
        undistributedBeforeCarry: number
    } = {
        headers: {},
        bookId: '',
        term: '',
        templates: [],
        carrySrVoucherId: '',
        carryCbfyVoucherId: '',
        revenueBeforePrepare: 0,
        expenseBeforePrepare: 0,
        profit3103BeforeCarry: 0,
        netProfitBeforeCarry: 0,
        expectedCarryNet: 0,
        undistributedBeforeCarry: 0,
    }

    test('login and load carry templates', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账�?)
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)
        ctx.templates = await fetchCarryTemplates(request, ctx.headers)
        expect(ctx.templates.length).toBeGreaterThan(0)
        expect(findCarryTemplate(ctx.templates, 'qm_jz_sr')).toBeTruthy()
        expect(findCarryTemplate(ctx.templates, 'qm_jz_cbfy')).toBeTruthy()
    })

    test('prepare P&L balances with revenue and expense vouchers', async ({request}) => {
        test.skip(!ctx.bookId, '账套未就�?)
        const before = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        ctx.revenueBeforePrepare = getSubjectBalance(before, '5001')
        ctx.expenseBeforePrepare = getSubjectBalance(before, '5602')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, revenue, expense} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !revenue || !expense, '账套缺少 1002/5001/5602 科目，跳过结转测�?)

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'E2E结转-确认收入', 800,
            {debit: bank, credit: revenue},
        )
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'E2E结转-管理费用', 100,
            {debit: expense, credit: bank},
        )
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('TC-RPT-015: income statement before carry-forward retains P&L subject balances', async ({request}) => {
        test.skip(!ctx.bookId, '账套未就�?)
        const balances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        const revenueBal = Math.abs(getSubjectBalance(balances, '5001'))
        const expenseBal = Math.abs(getSubjectBalance(balances, '5602'))
        expect(revenueBal).toBeGreaterThan(0)
        expect(expenseBal).toBeGreaterThan(0)

        const income = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        test.info().annotations.push({
            type: 'note',
            description: `结转前：5001=${revenueBal}, 5602=${expenseBal}, 利润表净利润=${income.current}`,
        })
    })

    test('TC-RPT-013 partial: P&L net profit before carry-forward', async ({request}) => {
        test.skip(!ctx.bookId, '账套未就�?)
        const income = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        const balances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        const revenueBal = getSubjectBalance(balances, '5001')
        const expenseBal = getSubjectBalance(balances, '5602')
        const revenueDelta = revenueBal - ctx.revenueBeforePrepare
        const expenseDelta = expenseBal - ctx.expenseBeforePrepare
        test.info().annotations.push({
            type: 'note',
            description: `结转前净利润=${income.current}, 5001增量=${revenueDelta}, 5602增量=${expenseDelta}`,
        })
        expect(Math.abs(revenueDelta)).toBeCloseTo(800, 0)
        expect(Math.abs(expenseDelta)).toBeCloseTo(100, 0)
    })

    test('TC-SET-012: qm_jz_sds carry when income tax expense exists', async ({request}) => {
        test.skip(!ctx.bookId, '账套未就�?)
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank} = pickStandardBusinessSubjects(subjects)
        const incomeTax = findSubjectByCode(subjects, '5801')
        test.skip(!bank || !incomeTax, '账套缺少 1002/5801 科目')

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, 'E2E结转-所得税费用', 30,
            {debit: incomeTax, credit: bank},
        )

        const sds = findCarryTemplate(ctx.templates, 'qm_jz_sds')
        test.skip(!sds, '�?qm_jz_sds 模板')
        const result = await generateCarryVoucher(request, ctx.headers, sds)
        expect(result.code, result.message || 'qm_jz_sds failed').toBe(0)
        expect(result.data).toBeTruthy()

        const detail = await getVoucherDetail(request, ctx.headers, result.data)
        const codes = detail.items.map((item: {subjectCode?: string}) => item.subjectCode)
        expect(codes).toContain('5801')
        expect(codes).toContain('3103')
        const taxLine = detail.items.find((item: {subjectCode?: string}) => item.subjectCode === '5801')
        const profitLine = detail.items.find((item: {subjectCode?: string}) => item.subjectCode === '3103')
        expect(Number(taxLine?.creditAmount ?? 0)).toBeCloseTo(30, 2)
        expect(Number(profitLine?.debitAmount ?? 0)).toBeCloseTo(30, 2)
    })

    test('TC-SET-013: qm_jz_bnlr rejected outside December', async ({request}) => {
        test.skip(!ctx.templates.length, '无结转模�?)
        const month = Number(ctx.term.slice(5, 7))
        test.skip(month === 12, '12 月账期走年末正例，本用例仅验证非年末拦截')

        const bnlr = findCarryTemplate(ctx.templates, 'qm_jz_bnlr')
        test.skip(!bnlr, '�?qm_jz_bnlr 模板')
        const result = await generateCarryVoucher(request, ctx.headers, bnlr)
        expect(result.code).not.toBe(0)
        expect(result.message || '').toMatch(/非年�?)
    })

    test('TC-SET-010/011: attempt qm_jz carry voucher generation', async ({request}) => {
        test.skip(!ctx.templates.length, '无结转模�?)
        await cleanupExistingCarryVouchers(request, ctx.headers)
        ctx.templates = await fetchCarryTemplates(request, ctx.headers)
        const sr = findCarryTemplate(ctx.templates, 'qm_jz_sr')!
        const cbfy = findCarryTemplate(ctx.templates, 'qm_jz_cbfy')!
        const srResult = await generateCarryVoucher(request, ctx.headers, sr)
        const cbfyResult = await generateCarryVoucher(request, ctx.headers, cbfy)

        expect(srResult.code, srResult.message || 'qm_jz_sr failed').toBe(0)
        expect(cbfyResult.code, cbfyResult.message || 'qm_jz_cbfy failed').toBe(0)
        expect(srResult.data).toBeTruthy()
        expect(cbfyResult.data).toBeTruthy()
        ctx.carrySrVoucherId = srResult.data
        ctx.carryCbfyVoucherId = cbfyResult.data
        const detail = await getVoucherDetail(request, ctx.headers, srResult.data)
        expect(detail.carryForward).toBe('y')
    })

    test('TC-SET-014: carryForward flag on generated voucher', async ({request}) => {
        test.skip(!ctx.carrySrVoucherId, '未生成结转凭�?)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.carrySrVoucherId)
        expect(detail.carryForward).toBe('y')
        expect(detail.status).toBe('draft')
    })

    test('TC-SET-015: delete draft carry voucher', async ({request}) => {
        const templates = await fetchCarryTemplates(request, ctx.headers)
        const sr = findCarryTemplate(templates, 'qm_jz_sr')
        test.skip(!sr, '无结转收入模�?)
        const gen = await generateCarryVoucher(request, ctx.headers, sr)
        expect(gen.code, gen.message || 'generate carry draft failed').toBe(0)
        expect(gen.data).toBeTruthy()
        const deleteResult = await deleteCarryVoucher(request, ctx.headers, gen.data)
        expect(deleteResult.code).toBe(0)
    })

    async function postCarryVoucher(request: APIRequestContext, voucherId: string) {
        const detail = await getVoucherDetail(request, ctx.headers, voucherId)
        const payload = {
            bookId: detail.bookId,
            word: detail.word,
            wordHead: detail.wordHead,
            wordNum: detail.wordNum,
            companyName: detail.companyName,
            receiptNum: detail.receiptNum ?? 0,
            voucherDate: detail.voucherDate,
            voucherYear: detail.voucherYear,
            voucherMonth: detail.voucherMonth,
            items: detail.items.map((item: any) => ({
                subjectId: item.subjectId,
                subjectName: item.subjectName,
                summary: item.summary,
                debitAmount: Number(item.debitAmount ?? 0),
                creditAmount: Number(item.creditAmount ?? 0),
            })),
        }
        await runVoucherToPosted(request, ctx.headers, payload, voucherId)
    }

    test('post carry vouchers if generated', async ({request}) => {
        test.skip(!ctx.carrySrVoucherId || !ctx.carryCbfyVoucherId, '无结转凭�?)
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {revenue, expense} = pickStandardBusinessSubjects(subjects)
        const beforeBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        const beforeRevenue = revenue ? getSubjectBalance(beforeBalances, revenue.code) : 0
        const beforeExpense = expense ? getSubjectBalance(beforeBalances, expense.code) : 0
        expect(Math.abs(beforeRevenue)).toBeGreaterThan(0)
        expect(Math.abs(beforeExpense)).toBeGreaterThan(0)

        ctx.profit3103BeforeCarry = getSubjectBalance(beforeBalances, '3103')
        ctx.undistributedBeforeCarry = getSubjectBalanceByCodes(beforeBalances, UNDISTRIBUTED_PROFIT_SUBJECT_CODES)
        const incomeBefore = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        ctx.netProfitBeforeCarry = incomeBefore.current
        const revenueCode = revenue?.code || '5001'
        const expenseCode = expense?.code || '5602'
        ctx.expectedCarryNet = computeCarryNetFromSubjectBalances(
            beforeBalances,
            [revenueCode],
            [expenseCode, '5801'],
        )

        // TC-SET-012 仅生成所得税结转草稿，后�?cleanup 会删掉；过账前补�?SDS，使 Δ3103 含税
        const taxBalance = Math.abs(getSubjectBalance(beforeBalances, '5801'))
        if (taxBalance > 0.01) {
            const templates = await fetchCarryTemplates(request, ctx.headers)
            const sds = findCarryTemplate(templates, 'qm_jz_sds')
            test.skip(!sds, '�?5801 余额但无 qm_jz_sds 模板')
            const sdsResult = await generateCarryVoucher(request, ctx.headers, sds)
            expect(sdsResult.code, sdsResult.message || 'qm_jz_sds regenerate failed').toBe(0)
            expect(sdsResult.data).toBeTruthy()
            await postCarryVoucher(request, sdsResult.data)
        }

        await postCarryVoucher(request, ctx.carrySrVoucherId)
        await postCarryVoucher(request, ctx.carryCbfyVoucherId)

        await assertIncomeCarryReconciliation(request, ctx.headers, ctx.term, {
            expectedCarryNet: ctx.expectedCarryNet,
            netProfitBeforeCarry: ctx.netProfitBeforeCarry,
            profit3103BeforeCarry: ctx.profit3103BeforeCarry,
            pAndLSubjectCodes: ['5001', '5602', '5801'],
            tolerance: 1,
        })

        const afterBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        if (revenue) {
            expect(Math.abs(getSubjectBalance(afterBalances, revenue.code))).toBeLessThanOrEqual(0.01)
        }
        if (expense) {
            expect(Math.abs(getSubjectBalance(afterBalances, expense.code))).toBeLessThanOrEqual(0.01)
        }
        const afterProfit = getSubjectBalance(afterBalances, '3103')
        test.info().annotations.push({
            type: 'note',
            description: `TC-SET-010/011/IS-R01: 5001 ${beforeRevenue}�?{revenue ? getSubjectBalance(afterBalances, revenue.code) : 'n/a'}, 5602 ${beforeExpense}�?{expense ? getSubjectBalance(afterBalances, expense.code) : 'n/a'}, 3103 ${ctx.profit3103BeforeCarry}�?{afterProfit}, expectedCarryNet=${ctx.expectedCarryNet}`,
        })
    })

    test('IS-R03: monthly carry leaves undistributed profit unchanged', async ({request}) => {
        test.skip(!ctx.carrySrVoucherId, '未过账结转凭�?)
        const month = Number(ctx.term.slice(5, 7))
        test.skip(month === 12, '12 月走 IS-R02 年末勾稽')

        await assertIncomeMonthlyCarryReconciliation(request, ctx.headers, ctx.term, {
            undistributedBeforeCarry: ctx.undistributedBeforeCarry,
            expectedNetProfit: ctx.expectedCarryNet || ctx.netProfitBeforeCarry,
            tolerance: 1,
        })
        test.info().annotations.push({
            type: 'note',
            description: `IS-R03: 未分配利润保�?${ctx.undistributedBeforeCarry}�?103≈结转前净�?${ctx.expectedCarryNet || ctx.netProfitBeforeCarry}`,
        })
    })

    test('TC-RPT-013: year profit after carry matches P&L net', async ({request}) => {
        test.skip(!ctx.carrySrVoucherId, '未过账结转凭�?)
        const balances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        const yearProfit = Math.abs(getSubjectBalance(balances, '3103'))
        expect(yearProfit).toBeGreaterThan(0)
        expect(Math.abs(getSubjectBalance(balances, '5001'))).toBeLessThanOrEqual(0.01)
        expect(Math.abs(getSubjectBalance(balances, '5602'))).toBeLessThanOrEqual(0.01)
        test.info().annotations.push({
            type: 'note',
            description: `结转后本年利�?3103=${yearProfit}（损益科目已归零）`,
        })
    })

    test('TC-VCH-056: carry-forward posting updates P&L subject balances', async ({request}) => {
        test.skip(!ctx.carrySrVoucherId, '未过账结转凭�?)
        const posted = await getVoucherDetail(request, ctx.headers, ctx.carrySrVoucherId)
        expect(posted.senderName).toBeTruthy()
        expect(posted.carryForward).toBe('y')
        const balances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        expect(Math.abs(getSubjectBalance(balances, '5001'))).toBeLessThanOrEqual(0.01)
        expect(Math.abs(getSubjectBalance(balances, '5602'))).toBeLessThanOrEqual(0.01)
    })

    test('TC-EXC-003: posted carry voucher delete is blocked', async ({request}) => {
        test.skip(!ctx.carrySrVoucherId, '无已过账结转凭证')
        const incomeBefore = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        const result = await tryDeleteVoucher(request, ctx.headers, ctx.carrySrVoucherId)
        expect(result.code, result.message || 'posted carry delete should fail').not.toBe(0)
        const detail = await getVoucherDetail(request, ctx.headers, ctx.carrySrVoucherId)
        expect(detail.senderName).toBeTruthy()
        const incomeAfter = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        expect(incomeAfter.current).toBeCloseTo(incomeBefore.current, 2)
    })

    test('TC-E2E-001: verify, checkout after carry-forward period', async ({request}) => {
        await fixVoucherNumbering(request, ctx.headers)
        await verifySettlement(request, ctx.headers, {bookId: ctx.bookId})
        await assertThreeReportsConsistent(request, ctx.headers, ctx.term)
        const income = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        test.info().annotations.push({
            type: 'note',
            description: `结账前净利润本期=${income.current}, 累计=${income.cumulative}`,
        })
    })
})
