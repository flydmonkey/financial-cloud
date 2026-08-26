import {expect, test} from '@playwright/test'
import {expectPagesOpen, loginViaApi, loginViaUi} from './helpers/auth'

test.describe('config module', () => {
    test('init balance and assist APIs respond', async ({request}) => {
        const auth = await loginViaApi(request)

        const endpoints = [
            `/api/base/init-balance/list`,
            `/api/base/assist-acc/fetch?pageNumber=1&pageSize=10`,
        ]

        for (const url of endpoints) {
            const res = await request.get(url, {headers: auth.headers})
            expect(res.ok()).toBeTruthy()
            const body = await res.json()
            expect(body.code).toBe(0)
        }
    })

    test('config pages open', async ({page}) => {
        await loginViaUi(page)
        await expectPagesOpen(page, [
            '/config/initBalance/index',
            '/config/assistAcc/index',
        ])
    })
})
