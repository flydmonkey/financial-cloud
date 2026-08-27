import {expect, test} from '@playwright/test'
import {getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {exportStatementReport, fetchBalanceSheet, fetchCashFlowStatement, fetchIncomeStatement, fetchSubjectBalances} from './helpers/reports'
import {ensureCashFlowConfigInitialized} from './helpers/cash-flow'

/** TC-RPT-007：报表导出接口可下载 xlsx */
test.describe('report export', () => {
    test('TC-RPT-007: statement exports return downloadable files', async ({request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        const term = await getCurrentTerm(request, auth.headers, user.bookId)

        await fetchBalanceSheet(request, auth.headers, term)
        await fetchIncomeStatement(request, auth.headers, term)
        await ensureCashFlowConfigInitialized(request, auth.headers, user.bookId)
        await fetchCashFlowStatement(request, auth.headers, term)
        await fetchSubjectBalances(request, auth.headers, term)

        const exports: Array<{path: string; params?: Record<string, string>}> = [
            {path: '/api/statement/income/export'},
            {path: '/api/statement/balance-sheet/export'},
            {path: '/api/statement/subject-balance/export', params: {showAll: 'true'}},
            {path: '/api/statement/cash-flow/export'},
        ]

        for (const item of exports) {
            await exportStatementReport(request, auth.headers, item.path, term, 'month', item.params)
        }

        const income = await fetchIncomeStatement(request, auth.headers, term)
        const incomeBody = await exportStatementReport(
            request, auth.headers, '/api/statement/income/export', term,
        )
        test.info().annotations.push({
            type: 'note',
            description: `利润表导出行数=${income?.items?.length ?? 0}, 文件=${incomeBody.length}B`,
        })
        expect(incomeBody.length).toBeGreaterThan(1000)
        expect((income?.items || []).length).toBeGreaterThan(0)
    })
})
