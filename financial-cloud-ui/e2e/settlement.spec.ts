import {expect, test} from '@playwright/test'
import {getCurrentTerm, loginViaApi} from './helpers/auth'

test.describe('settlement module', () => {
    test('settlement list API returns 12 months for current year', async ({request}) => {
        const auth = await loginViaApi(request)
        const term = await getCurrentTerm(request, auth.headers, '')
        const year = term.substring(0, 4)

        const res = await request.get(`/api/settlement/fetch?pageNumber=1&pageSize=10&year=${year}`, {
            headers: auth.headers,
        })
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code).toBe(0)
        expect(body.data.records.length).toBe(12)
    })

    test('settlement list defaults year when omitted', async ({request}) => {
        const auth = await loginViaApi(request)

        const res = await request.get('/api/settlement/fetch?pageNumber=1&pageSize=10', {
            headers: auth.headers,
        })
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code).toBe(0)
        expect(body.data.records.length).toBe(12)
    })

    const uiTest = process.env.E2E_ENABLE_UI === '1' ? test : test.skip
    uiTest('settlement pages open', async ({page}) => {
        await page.goto('/login')
        await page.locator('input[type="text"]').first().fill(process.env.E2E_USERNAME || 'admin')
        await page.locator('input[type="password"]').fill(process.env.E2E_PASSWORD || 'changeme')
        await page.locator('.login-btn').click()
        await expect(page).not.toHaveURL(/\/login/, {timeout: 30_000})

        for (const path of ['/settlement/settle-list', '/settlement/settle-period', '/settlement/carry-forward']) {
            await page.goto(path)
            await expect(page.locator('.app-container').first()).toBeVisible({timeout: 15_000})
        }
    })
})
