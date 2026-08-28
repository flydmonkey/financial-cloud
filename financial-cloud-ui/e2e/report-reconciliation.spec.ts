import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    assertReportsBalanced,
    assertThreeReportsConsistent,
    assertIncomeFormulaChain,
    captureReportSnapshot,
    fetchIncomeStatement,
    type ReportSnapshot,
} from './helpers/reports'
import {
    auditVoucher,
    buildBalancedVoucherPayload,
    createDraftVoucher,
    ensureVoucherReviewEnabled,
    pickReconciliationSubjects,
    postVoucher,
    submitVoucher,
    tryPostVoucher,
} from './helpers/voucher'

/**
 * TC-RPT-003 / TC-E2E-005：三报表口径统一（仅已过账）
 * - 已审核未过账：三报表均无变化
 * - 过账后：三报表同步更新，恒等式成立
 * - 重复过账：余额不翻倍
 */
test.describe.serial('report reconciliation', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        voucherId: string
        baseline: ReportSnapshot | null
        afterAudit: ReportSnapshot | null
        afterPost: ReportSnapshot | null
        voucherAmount: number
    } = {
        headers: {},
        bookId: '',
        term: '',
        voucherId: '',
        baseline: null,
        afterAudit: null,
        afterPost: null,
        voucherAmount: 123.45,
    }

    test('login and capture baseline reports', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套，请先完成 onboarding 或登录有效账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
        await ensureVoucherReviewEnabled(request, auth.headers, user.bookId)

        ctx.baseline = await captureReportSnapshot(request, ctx.headers, ctx.term)
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('audited-not-posted: all three reports stay unchanged', async ({request}) => {
        test.skip(!ctx.baseline, '缺少基线报表快照')

        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        test.skip(subjects.length < 2, '账套科目不足')
        const pair = pickReconciliationSubjects(subjects)

        const payload = await buildBalancedVoucherPayload(
            request,
            ctx.headers,
            ctx.bookId,
            'E2E报表勾稽-已审未过账',
            ctx.voucherAmount,
            pair,
        )
        ctx.voucherId = await createDraftVoucher(request, ctx.headers, payload)
        await submitVoucher(request, ctx.headers, payload, ctx.voucherId)
        await auditVoucher(request, ctx.headers, ctx.voucherId)

        ctx.afterAudit = await captureReportSnapshot(request, ctx.headers, ctx.term)
        await assertReportsBalanced(request, ctx.headers, ctx.term)

        const incomeUnchanged =
            Math.abs((ctx.afterAudit.incomeNetProfit ?? 0) - (ctx.baseline!.incomeNetProfit ?? 0)) <= 0.01
        const balanceSheetUnchanged =
            ctx.baseline!.assetTotal != null &&
            ctx.afterAudit.assetTotal != null &&
            Math.abs(ctx.afterAudit.assetTotal - ctx.baseline!.assetTotal) <= 0.01
        const cashUnchanged =
            ctx.baseline!.cashEnding == null ||
            ctx.afterAudit.cashEnding == null ||
            Math.abs(ctx.afterAudit.cashEnding - ctx.baseline!.cashEnding) <= 0.01

        test.info().annotations.push({
            type: 'note',
            description: `已审未过账: 利润表不变=${incomeUnchanged}, 资产负债表不变=${balanceSheetUnchanged}, 现金表不变=${cashUnchanged}`,
        })

        expect(incomeUnchanged, '未过账凭证不应改变利润表').toBeTruthy()
        if (ctx.baseline!.assetTotal != null && ctx.afterAudit.assetTotal != null) {
            expect(balanceSheetUnchanged, '未过账凭证不应改变资产负债表总计').toBeTruthy()
        }
        if (ctx.baseline!.cashEnding != null && ctx.afterAudit.cashEnding != null) {
            expect(cashUnchanged, '未过账凭证不应改变现金流量表').toBeTruthy()
        }
    })

    test('posted: all three reports update together', async ({request}) => {
        test.skip(!ctx.voucherId || !ctx.afterAudit || !ctx.baseline, '前置凭证或快照缺失')

        await postVoucher(request, ctx.headers, ctx.voucherId)
        ctx.afterPost = await captureReportSnapshot(request, ctx.headers, ctx.term)
        await assertReportsBalanced(request, ctx.headers, ctx.term)

        // 审核未过账时利润表应与基线一致（仅已过账才计入）
        if (ctx.baseline!.incomeNetProfit != null && ctx.afterAudit!.incomeNetProfit != null) {
            expect(Math.abs(ctx.afterAudit!.incomeNetProfit - ctx.baseline!.incomeNetProfit))
                .toBeLessThanOrEqual(0.01)
        }

        const incomeMoved =
            ctx.baseline!.incomeNetProfit != null &&
            ctx.afterPost!.incomeNetProfit != null &&
            Math.abs(ctx.afterPost!.incomeNetProfit - ctx.baseline!.incomeNetProfit) > 0.01
        const assetMoved =
            ctx.baseline!.assetTotal != null &&
            ctx.afterPost!.assetTotal != null &&
            Math.abs(ctx.afterPost!.assetTotal - ctx.baseline!.assetTotal) > 0.01
        const cashMoved =
            ctx.baseline!.cashEnding != null &&
            ctx.afterPost!.cashEnding != null &&
            Math.abs(ctx.afterPost!.cashEnding - ctx.baseline!.cashEnding) > 0.01

        test.info().annotations.push({
            type: 'note',
            description: `过账后: 利润表变化=${incomeMoved}, 资产总计变化=${assetMoved}, 现金表变化=${cashMoved}`,
        })

        // 过账后至少一张报表相对基线应有变化（凭证涉及报表映射科目时）
        expect(incomeMoved || assetMoved || cashMoved, '过账后三报表应至少一处相对基线更新').toBeTruthy()

        const income = await fetchIncomeStatement(request, ctx.headers, ctx.term)
        assertIncomeFormulaChain(income?.items || [])
    })

    test('duplicate post does not double-report balances', async ({request}) => {
        test.skip(!ctx.voucherId || !ctx.afterPost, '前置凭证或过账快照缺失')

        const before = await captureReportSnapshot(request, ctx.headers, ctx.term)
        const duplicateResult = await tryPostVoucher(request, ctx.headers, ctx.voucherId)
        const after = await captureReportSnapshot(request, ctx.headers, ctx.term)

        test.info().annotations.push({
            type: 'note',
            description: `重复过账响应: code=${duplicateResult.code}, message=${duplicateResult.message || ''}`,
        })

        if (before.assetTotal != null && after.assetTotal != null) {
            expect(Math.abs(after.assetTotal - before.assetTotal)).toBeLessThanOrEqual(0.01)
        }
        if (before.incomeNetProfit != null && after.incomeNetProfit != null) {
            expect(Math.abs(after.incomeNetProfit - before.incomeNetProfit)).toBeLessThanOrEqual(0.01)
        }
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })

    test('TC-E2E-005: three reports stay consistent after duplicate post guard', async ({request}) => {
        test.skip(!ctx.afterPost, '缺少过账快照')
        await assertThreeReportsConsistent(request, ctx.headers, ctx.term)
    })
})
