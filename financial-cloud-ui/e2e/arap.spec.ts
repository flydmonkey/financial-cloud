import {expect, test} from '@playwright/test'
import {getCurrentTerm, loginViaApi} from './helpers/auth'

/**
 * AR/AP L1+L2 API smoke. Requires running backend + credentials.
 * Skip gracefully when login env unavailable (CI without stack).
 */
test.describe('arap L1+L2', () => {
    test('balance / aging / verify arap item', async ({request}) => {
        let auth: Awaited<ReturnType<typeof loginViaApi>>
        try {
            auth = await loginViaApi(request)
        } catch {
            test.skip(true, 'login unavailable — skip arap API smoke')
            return
        }
        const term = await getCurrentTerm(request, auth.headers, '')

        const balance = await request.get('/api/arap/balance', {
            headers: auth.headers,
            params: {side: 'AR', periodStart: term, periodEnd: term, includeZero: true},
        })
        expect(balance.ok()).toBeTruthy()
        const balBody = await balance.json()
        expect(balBody.code).toBe(0)
        expect(Array.isArray(balBody.data)).toBeTruthy()

        const aging = await request.get('/api/arap/aging', {
            headers: auth.headers,
            params: {side: 'AR', asOfDate: `${term}-28`},
        })
        expect(aging.ok()).toBeTruthy()
        const ageBody = await aging.json()
        expect(ageBody.code).toBe(0)
        expect(Array.isArray(ageBody.data)).toBeTruthy()

        const verify = await request.get('/api/settlement/verify', {headers: auth.headers})
        expect(verify.ok()).toBeTruthy()
        const vBody = await verify.json()
        expect(vBody.code === 0 || vBody.code === 1).toBeTruthy()
        const arap = (vBody.data || []).find((x: any) => String(x.item || '').includes('往来'))
        expect(arap, 'verify should include 往来款项 item').toBeTruthy()
        if (arap.warning) {
            expect(arap.result).toBeTruthy()
        }

        const counterpartId = balBody.data?.[0]?.counterpartId
        if (counterpartId) {
            const detail = await request.get('/api/arap/detail', {
                headers: auth.headers,
                params: {
                    side: 'AR',
                    counterpartId,
                    periodStart: term,
                    periodEnd: term,
                },
            })
            expect(detail.ok()).toBeTruthy()
            const dBody = await detail.json()
            expect(dBody.code).toBe(0)

            const exportRes = await request.get('/api/arap/statement/export', {
                headers: auth.headers,
                params: {
                    side: 'AR',
                    counterpartId,
                    periodStart: term,
                    periodEnd: term,
                },
            })
            expect(exportRes.ok()).toBeTruthy()
            const buf = await exportRes.body()
            expect(buf.byteLength).toBeGreaterThan(100)
        }
    })

    const uiTest = process.env.E2E_ENABLE_UI === '1' ? test : test.skip
    uiTest('arap pages open', async ({page}) => {
        await page.goto('/login')
        await page.locator('input[type="text"]').first().fill(process.env.E2E_USERNAME || 'admin')
        await page.locator('input[type="password"]').fill(process.env.E2E_PASSWORD || 'changeme')
        await page.locator('.login-btn').click()
        await expect(page).not.toHaveURL(/\/login/, {timeout: 30_000})
        for (const path of ['/arap/balance', '/arap/detail', '/arap/aging']) {
            await page.goto(path)
            await expect(page.locator('.app-container').first()).toBeVisible({timeout: 15_000})
        }
    })
})
