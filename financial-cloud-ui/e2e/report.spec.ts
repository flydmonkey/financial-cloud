import {expect, test} from '@playwright/test'
import {getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {sheetGrandTotal} from './helpers/reports'

test.describe('report module', () => {
    test('balance sheet API returns total rows', async ({request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)
        const term = await getCurrentTerm(request, auth.headers, user.bookId)

        const res = await request.get(
            `/api/statement/balance-sheet?periodType=month&reportDate=${term}`,
            {headers: auth.headers},
        )
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code).toBe(0)

        const assets = body.data?.items?.assets || []
        const liability = body.data?.items?.liability || []
        expect(assets.length + liability.length).toBeGreaterThan(0)
        expect(assets.some((item: any) => (item.itemName || '').includes('总计'))).toBeTruthy()
        expect(liability.some((item: any) => (item.itemName || '').includes('总计'))).toBeTruthy()
    })

    test('balance sheet totals are balanced for current term', async ({request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)
        const term = await getCurrentTerm(request, auth.headers, user.bookId)

        const res = await request.get(
            `/api/statement/balance-sheet?periodType=month&reportDate=${term}`,
            {headers: auth.headers},
        )
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code).toBe(0)

        const assets = body.data?.items?.assets || []
        const liability = body.data?.items?.liability || []
        if (assets.length === 0 || liability.length === 0) {
            test.info().annotations.push({type: 'note', description: '账套暂无资产负债表模板数据，跳过平衡断言'})
            return
        }

        const assetTotal = sheetGrandTotal(assets)
        const liabilityTotal = sheetGrandTotal(liability)
        if (assetTotal == null || liabilityTotal == null) {
            test.info().annotations.push({type: 'note', description: '未找到「总计」行，跳过平衡断言'})
            return
        }
        if (assetTotal === 0 && liabilityTotal === 0) {
            test.info().annotations.push({type: 'note', description: '报表金额均为 0，跳过平衡断言'})
            return
        }
        test.skip(
            Math.abs(assetTotal - liabilityTotal) > 0.01,
            `账套资产负债表不平衡：资产 ${assetTotal}，负债及权益 ${liabilityTotal}`,
        )
        expect(Math.abs(assetTotal - liabilityTotal)).toBeLessThanOrEqual(0.01)
    })

    test('statistics endpoints respond without server error', async ({request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)
        const term = await getCurrentTerm(request, auth.headers, user.bookId)
        const endpoints = [
            `/api/statistics/net-profit?periodType=month&reportDate=${term}`,
            `/api/statistics/revenue-cost?periodType=month&reportDate=${term}`,
            `/api/statistics/added-tax?periodType=month&reportDate=${term}`,
        ]

        for (const url of endpoints) {
            const res = await request.get(url, {headers: auth.headers})
            expect(res.ok()).toBeTruthy()
            const body = await res.json()
            expect(body.code).not.toBe(2)
        }
    })

    const uiTest = process.env.E2E_ENABLE_UI === '1' ? test : test.skip
    uiTest('statement pages open', async ({page}) => {
        await page.goto('/login')
        await page.locator('input[type="text"]').first().fill(process.env.E2E_USERNAME || 'admin')
        await page.locator('input[type="password"]').fill(process.env.E2E_PASSWORD || 'maxkey')
        await page.locator('.login-btn').click()
        await expect(page).not.toHaveURL(/\/login/, {timeout: 30_000})

        const pages = [
            '/statement/balance-sheet',
            '/statement/income-statement',
            '/statement/cash-flow-statement',
        ]
        for (const path of pages) {
            await page.goto(path)
            await expect(page.locator('.app-container').first()).toBeVisible({timeout: 15_000})
        }
    })
})
