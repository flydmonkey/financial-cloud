import {expect, test} from '@playwright/test'
import {loginViaApi} from './helpers/auth'

test.describe('books board workspace', () => {
    test('books-board API returns focusPeriod and rows array', async ({request}) => {
        const auth = await loginViaApi(request)
        const res = await request.get('/api/workspace/books-board?focusPeriod=2026-08', {
            headers: auth.headers,
        })
        expect(res.ok()).toBeTruthy()
        const body = await res.json()
        expect(body.code).toBe(0)
        expect(body.data.focusPeriod).toBe('2026-08')
        expect(Array.isArray(body.data.rows)).toBeTruthy()
        expect(typeof body.data.totalGranted).toBe('number')
    })

    const uiTest = process.env.E2E_ENABLE_UI === '1' ? test : test.skip
    uiTest('books-board page renders table', async ({page}) => {
        await page.goto('/login')
        await page.locator('input[type="text"]').first().fill(process.env.E2E_USERNAME || 'admin')
        await page.locator('input[type="password"]').fill(process.env.E2E_PASSWORD || 'changeme')
        await page.locator('.login-btn').click()
        await expect(page).not.toHaveURL(/\/login/, {timeout: 30_000})
        await page.goto('/workspace/books-board')
        await expect(page.locator('.books-board')).toBeVisible({timeout: 30_000})
        await expect(page.getByText('关注月')).toBeVisible()
    })
})
