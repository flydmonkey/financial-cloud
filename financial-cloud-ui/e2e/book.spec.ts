import {expect, test} from '@playwright/test'
import {getCurrentUser, loginViaApi} from './helpers/auth'

test.describe('book module', () => {
    test('book and subject APIs respond', async ({request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)

        const endpoints = [
            `/api/book/fetch?pageNumber=1&pageSize=10`,
            `/api/booksubject/fetch?bookId=${user.bookId}&pageNumber=1&pageSize=10`,
            `/api/base/assist-acc/fetch?pageNumber=1&pageSize=10`,
            `/api/base/init-balance/list`,
        ]

        for (const url of endpoints) {
            const res = await request.get(url, {headers: auth.headers})
            expect(res.ok()).toBeTruthy()
            const body = await res.json()
            expect(body.code).toBe(0)
        }
    })
})
