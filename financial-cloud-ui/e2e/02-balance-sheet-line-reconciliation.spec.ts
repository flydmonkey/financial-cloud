import {test} from '@playwright/test'
import {getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    assertBalanceSheetLineMatchesConfig,
    assertBalanceSheetLineMatchesRules,
    assertReportsBalanced,
    fetchBalanceSheet,
    fetchBalanceSheetRules,
    findBalanceSheetItemByName,
} from './helpers/reports'

/**
 * BS-D/M：报表行 ↔ 科目余额逐行勾稽（依赖 00-opening-balance-report 期初数据）
 */
test.describe.serial('balance sheet line reconciliation', () => {
    const ctx: {
        headers: Record<string, string>
        term: string
        capitalItemCode: string
    } = {
        headers: {},
        term: '',
        capitalItemCode: '',
    }

    test('login and resolve current term', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)
    })

    test('BS-M01: monetary funds equals cash subject balances', async ({request}) => {
        test.skip(!ctx.term, '无账期')
        await assertBalanceSheetLineMatchesRules(
            request,
            ctx.headers,
            ctx.term,
            '货币资金',
            [
                {subjectCode: '1001', symbol: '+'},
                {subjectCode: '1002', symbol: '+'},
            ],
        )
    })

    test('BS-D02: paid-in capital equals configured subject rules', async ({request}) => {
        test.skip(!ctx.term, '无账期')
        const balanceSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const liability = balanceSheet?.items?.liability || []
        const capitalLine = findBalanceSheetItemByName(liability, '实收资本')
        test.skip(!capitalLine?.itemCode, '资产负债表无实收资本行')
        ctx.capitalItemCode = String(capitalLine!.itemCode)

        const rules = await fetchBalanceSheetRules(request, ctx.headers, ctx.capitalItemCode)
        test.skip(rules.length === 0, '实收资本未配置科目规则')
        await assertBalanceSheetLineMatchesRules(
            request,
            ctx.headers,
            ctx.term,
            '实收资本',
            rules.map((rule) => ({
                subjectCode: String(rule.subjectCode),
                symbol: rule.symbol === '-' ? '-' : '+',
            })),
        )
    })

    test('BS-CFG: monetary funds line matches config rules closing balances', async ({request}) => {
        test.skip(!ctx.term, '无账期')
        await assertBalanceSheetLineMatchesConfig(request, ctx.headers, ctx.term, '1101')
    })

    test('BS-B01: trial balance holds after line reconciliation', async ({request}) => {
        test.skip(!ctx.term, '无账期')
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })
})
