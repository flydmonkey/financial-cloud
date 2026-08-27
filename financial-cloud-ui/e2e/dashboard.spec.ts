import {expect, test} from '@playwright/test'
import {expectPagesOpen, getCurrentTerm, getCurrentUser, loginViaApi, loginViaUi} from './helpers/auth'

test.describe('dashboard module', () => {
    test('statistics APIs respond', async ({request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)
        const term = await getCurrentTerm(request, auth.headers, user.bookId)

        const endpoints = [
            `/api/statistics/fund-balance?periodType=month&reportDate=${term}`,
            `/api/statistics/net-profit?periodType=month&reportDate=${term}`,
            `/api/statistics/revenue-cost?periodType=month&reportDate=${term}`,
            `/api/statistics/able-cash?periodType=month&reportDate=${term}`,
        ]

        for (const url of endpoints) {
            const res = await request.get(url, {headers: auth.headers})
            expect(res.ok()).toBeTruthy()
            const body = await res.json()
            expect(body.code).toBe(0)
        }
    })

    test('dashboard home opens', async ({page}) => {
        await loginViaUi(page)
        await expectPagesOpen(page, ['/index'])
    })
})
