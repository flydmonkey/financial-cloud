import {expect, test} from '@playwright/test'
import {expectPagesOpen, loginViaApi, loginViaUi} from './helpers/auth'

test.describe('journal module', () => {
    test('journal APIs respond', async ({request}) => {
        const auth = await loginViaApi(request)

        const endpoints = [
            `/api/journal/account/fetch?pageNumber=1&pageSize=10`,
            `/api/journal/entry/fetch?pageNumber=1&pageSize=10`,
            `/api/journal/summary/fetch?pageNumber=1&pageSize=10`,
        ]

        for (const url of endpoints) {
            const res = await request.get(url, {headers: auth.headers})
            expect(res.ok()).toBeTruthy()
            const body = await res.json()
            expect(body.code).toBe(0)
        }
    })

    test('journal pages open', async ({page}) => {
        await loginViaUi(page)
        await expectPagesOpen(page, [
            '/journal/journalaccout',
            '/journal/journalentry',
            '/journal/journalsummary',
        ])
    })
})
