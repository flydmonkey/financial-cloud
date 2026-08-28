import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, loginViaApi} from './helpers/auth'
import {clearBooksViaScript, setupE2eBookViaApi} from './helpers/books'
import {
    assertIncomeYearEndReconciliation,
    fetchSubjectBalances,
    getIncomeNetProfit,
    getSubjectBalance,
    getSubjectBalanceByCodes,
    UNDISTRIBUTED_PROFIT_SUBJECT_CODES,
    YEAR_PROFIT_SUBJECT_CODES,
} from './helpers/reports'
import {
    fetchCarryTemplates,
    findCarryTemplate,
    generateCarryVoucher,
} from './helpers/settlement-carry'
import {
    createAndPostVoucher,
    ensureVoucherReviewEnabled,
    getVoucherDetail,
    pickStandardBusinessSubjects,
    runVoucherToPosted,
} from './helpers/voucher'

/**
 * TC-SET-013 + IS-R02：12 月账期年末结转 qm_jz_bnlr
 * 独立运行：E2E_RESET_BOOK=1 npm run test:e2e:year-end
 */
test.describe.serial('year-end carry forward', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        templates: Awaited<ReturnType<typeof fetchCarryTemplates>>
        netProfitCumulative: number
        undistributedBefore: number
        profit3103Before: number
    } = {
        headers: {},
        bookId: '',
        term: '',
        templates: [],
        netProfitCumulative: 0,
        undistributedBefore: 0,
        profit3103Before: 0,
    }

    test('setup December book and prepare year profit', async ({request}) => {
        clearBooksViaScript()
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        ctx.bookId = await setupE2eBookViaApi(request, auth.headers, {
            name: 'E2E年末账套',
            enableDate: '2025-12',
        })
        ctx.term = await getCurrentTerm(request, ctx.headers, ctx.bookId)
        expect(ctx.term).toBe('2025-12')
        await ensureVoucherReviewEnabled(request, ctx.headers, ctx.bookId)

        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, revenue, expense} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !revenue || !expense, '缺少 1002/5001/5602')

        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '年末-确认收入', 800,
            {debit: bank, credit: revenue},
        )
        await createAndPostVoucher(
            request, ctx.headers, ctx.bookId, '年末-管理费用', 100,
            {debit: expense, credit: bank},
        )

        ctx.templates = await fetchCarryTemplates(request, ctx.headers)
        const sr = findCarryTemplate(ctx.templates, 'qm_jz_sr')
        const cbfy = findCarryTemplate(ctx.templates, 'qm_jz_cbfy')
        test.skip(!sr || !cbfy, '缺少 qm_jz_sr/qm_jz_cbfy 模板')
        const srResult = await generateCarryVoucher(request, ctx.headers, sr)
        const cbfyResult = await generateCarryVoucher(request, ctx.headers, cbfy)
        expect(srResult.code, srResult.message || 'qm_jz_sr failed').toBe(0)
        expect(cbfyResult.code, cbfyResult.message || 'qm_jz_cbfy failed').toBe(0)

        for (const voucherId of [srResult.data, cbfyResult.data]) {
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
                items: detail.items.map((item: {subjectId?: string; subjectName?: string; summary?: string; debitAmount?: number | string; creditAmount?: number | string}) => ({
                    subjectId: item.subjectId,
                    subjectName: item.subjectName,
                    summary: item.summary,
                    debitAmount: Number(item.debitAmount ?? 0),
                    creditAmount: Number(item.creditAmount ?? 0),
                })),
            }
            await runVoucherToPosted(request, ctx.headers, payload, voucherId)
        }

        const balances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        ctx.profit3103Before = getSubjectBalanceByCodes(balances, YEAR_PROFIT_SUBJECT_CODES)
        ctx.undistributedBefore = getSubjectBalanceByCodes(balances, UNDISTRIBUTED_PROFIT_SUBJECT_CODES)
        const income = await getIncomeNetProfit(request, ctx.headers, ctx.term)
        ctx.netProfitCumulative = income.cumulative

        expect(Math.abs(ctx.profit3103Before)).toBeGreaterThan(0)
        expect(Math.abs(ctx.netProfitCumulative)).toBeCloseTo(700, 0)
    })

    test('TC-SET-013 / IS-R02: qm_jz_bnlr posts and reconciles undistributed profit', async ({request}) => {
        test.skip(!ctx.bookId || ctx.term !== '2025-12', '非 12 月账套')
        const bnlr = findCarryTemplate(ctx.templates, 'qm_jz_bnlr')
        test.skip(!bnlr, '无 qm_jz_bnlr 模板')

        const result = await generateCarryVoucher(request, ctx.headers, bnlr)
        expect(result.code, result.message || 'qm_jz_bnlr failed').toBe(0)
        expect(result.data).toBeTruthy()

        const detail = await getVoucherDetail(request, ctx.headers, result.data)
        const codes = detail.items.map((item: {subjectCode?: string}) => item.subjectCode)
        expect(codes).toContain('3103')
        expect(codes.some((code: string) => code === '3104.02' || code.startsWith('3104'))).toBeTruthy()

        const profitLine = detail.items.find((item: {subjectCode?: string}) => item.subjectCode === '3103')
        const undistributedLine = detail.items.find(
            (item: {subjectCode?: string}) => item.subjectCode === '3104.02' || item.subjectCode?.startsWith('3104'),
        )
        expect(Number(profitLine?.debitAmount ?? 0)).toBeGreaterThan(0)
        expect(Number(undistributedLine?.creditAmount ?? 0)).toBeGreaterThan(0)
        expect(Number(profitLine?.debitAmount ?? 0)).toBeCloseTo(
            Number(undistributedLine?.creditAmount ?? 0),
            2,
        )

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
            items: detail.items.map((item: {subjectId?: string; subjectName?: string; summary?: string; debitAmount?: number | string; creditAmount?: number | string}) => ({
                subjectId: item.subjectId,
                subjectName: item.subjectName,
                summary: item.summary,
                debitAmount: Number(item.debitAmount ?? 0),
                creditAmount: Number(item.creditAmount ?? 0),
            })),
        }
        await runVoucherToPosted(request, ctx.headers, payload, result.data)

        await assertIncomeYearEndReconciliation(request, ctx.headers, ctx.term, {
            netProfitCumulative: ctx.netProfitCumulative,
            undistributedBefore: ctx.undistributedBefore,
            profit3103Before: ctx.profit3103Before,
        })

        const afterBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        test.info().annotations.push({
            type: 'note',
            description: `IS-R02: 累计净利润=${ctx.netProfitCumulative}, 3103 ${ctx.profit3103Before}→${getSubjectBalance(afterBalances, '3103')}, 未分配利润 ${ctx.undistributedBefore}→${getSubjectBalanceByCodes(afterBalances, UNDISTRIBUTED_PROFIT_SUBJECT_CODES)}`,
        })
    })
})
