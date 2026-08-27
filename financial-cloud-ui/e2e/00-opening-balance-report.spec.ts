import {expect, test} from '@playwright/test'
import {getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {fetchInitBalanceList, saveStandardOpeningBalances} from './helpers/init-balance'
import {
    assertReportsBalanced,
    fetchBalanceSheet,
    fetchSubjectBalances,
    findBalanceSheetItemByName,
    getSubjectBalance,
    num,
} from './helpers/reports'

/**
 * TC-RPT-002：账套 B 期初无业务时报表 = 期初余额
 * 文件名 00- 前缀确保在 accounting 套件中最先执行
 */
test.describe.serial('opening balance reports', () => {
    const ctx: {
        headers: Record<string, string>
        bookId: string
        term: string
        capitalCode: string
    } = {
        headers: {},
        bookId: '',
        term: '',
        capitalCode: '3001',
    }

    test('login and save standard opening balances', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
        ctx.term = await getCurrentTerm(request, auth.headers, user.bookId)

        const rows = await fetchInitBalanceList(request, auth.headers)
        const bank = rows.find((item) => item.code === '1002')
        if (bank?.hasVoucher) {
            test.skip(true, '账套已有凭证，TC-RPT-002 需在 accounting 套件最前执行')
        }

        const {capitalCode} = await saveStandardOpeningBalances(
            request, ctx.headers, ctx.bookId, 100_000,
        )
        ctx.capitalCode = capitalCode
    })

    test('TC-RPT-002: reports reflect opening balances before any voucher', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')

        const subjectBalances = await fetchSubjectBalances(request, ctx.headers, ctx.term)
        expect(getSubjectBalance(subjectBalances, '1002')).toBeCloseTo(100_000, 0)
        expect(Math.abs(getSubjectBalance(subjectBalances, ctx.capitalCode))).toBeCloseTo(100_000, 0)
        for (const code of ['1002', ctx.capitalCode]) {
            const row = subjectBalances.find((item) => item.subjectCode === code)
            expect(row?.sourceId, `${code} 期初余额必须绑定 book_subject.id`).toBeTruthy()
        }

        const balanceSheet = await fetchBalanceSheet(request, ctx.headers, ctx.term)
        const assets = balanceSheet?.items?.assets || []
        const liability = balanceSheet?.items?.liability || []
        const monetary = findBalanceSheetItemByName(assets, '货币资金')
        const capital = findBalanceSheetItemByName(liability, '实收资本')

        test.info().annotations.push({
            type: 'note',
            description: `货币资金=${num(monetary?.currentBalance)}, 实收资本=${num(capital?.currentBalance)}`,
        })
        expect(num(monetary?.currentBalance)).toBeCloseTo(100_000, 0)
        expect(capital, '资产负债表应映射实收资本行').toBeTruthy()
        expect(num(capital?.currentBalance)).toBeCloseTo(100_000, 0)
        await assertReportsBalanced(request, ctx.headers, ctx.term)
    })
})
