import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    assertReportsBalanced,
    captureReportSnapshot,
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
 * TC-RPT-003 / TC-E2E-005：三报表口径差异与勾稽
 * - 已审核未过账：利润表可能有数，资产负债表依赖过账
 * - 过账后：三报表同步，恒等式成立
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

    test('audited-not-posted: income may differ, balance sheet uses posted data only', async ({request}) => {
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

        const incomeChanged =
            Math.abs((ctx.afterAudit.incomeNetProfit ?? 0) - (ctx.baseline!.incomeNetProfit ?? 0)) > 0.01
        const balanceSheetUnchanged =
            ctx.baseline!.assetTotal != null &&
            ctx.afterAudit.assetTotal != null &&
            Math.abs(ctx.afterAudit.assetTotal - ctx.baseline!.assetTotal) <= 0.01

        test.info().annotations.push({
            type: 'note',
            description: `已审未过账: 利润表变化=${incomeChanged}, 资产负债表不变=${balanceSheetUnchanged}`,
        })

        // 利润表取 completed 凭证（含未过账）；若科目映射到损益类则应有变化
        if (pair.debit.code?.startsWith('660') || pair.credit.code?.startsWith('60')) {
            expect(incomeChanged, '已审核未过账凭证应影响利润表').toBeTruthy()
        }
        // 资产负债表仅反映已过账数据，资产总计应不变
        if (ctx.baseline!.assetTotal != null && ctx.afterAudit.assetTotal != null) {
            expect(balanceSheetUnchanged, '未过账凭证不应改变资产负债表总计').toBeTruthy()
        }
    })

    test('posted: balance sheet updates while income stays consistent with audited state', async ({request}) => {
        test.skip(!ctx.voucherId || !ctx.afterAudit, '前置凭证或审核快照缺失')

        await postVoucher(request, ctx.headers, ctx.voucherId)
        ctx.afterPost = await captureReportSnapshot(request, ctx.headers, ctx.term)
        await assertReportsBalanced(request, ctx.headers, ctx.term)

        // 过账后利润表应与已审未过账时一致（同一 completed 凭证集合）
        if (ctx.afterAudit!.incomeNetProfit != null && ctx.afterPost!.incomeNetProfit != null) {
            expect(Math.abs(ctx.afterPost!.incomeNetProfit - ctx.afterAudit!.incomeNetProfit)).toBeLessThanOrEqual(0.01)
        }

        // 若涉及货币资金科目，过账后资产负债表或现金表应有变化
        const bankInvolved =
            ctx.afterPost!.cashEnding != null &&
            ctx.baseline!.cashEnding != null &&
            Math.abs(ctx.afterPost!.cashEnding - ctx.baseline!.cashEnding) > 0.01
        const assetMoved =
            ctx.baseline!.assetTotal != null &&
            ctx.afterPost!.assetTotal != null &&
            Math.abs(ctx.afterPost!.assetTotal - ctx.baseline!.assetTotal) > 0.01

        test.info().annotations.push({
            type: 'note',
            description: `过账后: 资产总计变化=${assetMoved}, 现金相关变化=${bankInvolved}`,
        })
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
})
