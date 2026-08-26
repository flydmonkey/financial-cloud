import {expect, test} from '@playwright/test'
import {loginViaApi} from './helpers/auth'

test.describe('hr module', () => {
    test('employee and salary APIs respond', async ({request}) => {
        const auth = await loginViaApi(request)

        const endpoints = [
            `/api/salary/employee/fetch?pageNumber=1&pageSize=10`,
            `/api/employee/salary-summary/fetch?pageNumber=1&pageSize=10`,
        ]

        for (const url of endpoints) {
            const res = await request.get(url, {headers: auth.headers})
            expect(res.ok()).toBeTruthy()
            const body = await res.json()
            expect(body.code).toBe(0)
        }
    })
})
