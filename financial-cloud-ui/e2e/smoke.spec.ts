import {expect, test} from '@playwright/test'

const username = process.env.E2E_USERNAME || 'admin'
const password = process.env.E2E_PASSWORD || 'changeme'

test.describe('smoke', () => {
    test('login api without captcha', async ({request}) => {
        const init = await request.get('/api/login/get?_allow_anonymous=true')
        expect(init.ok()).toBeTruthy()
        const initBody = await init.json()
        expect(initBody.data.captcha).toBe('NONE')

        const signin = await request.post('/api/login/signin?_allow_anonymous=true', {
            data: {
                username,
                password,
                captcha: '',
                state: initBody.data.state,
                authType: 'normal',
            },
        })
        expect(signin.ok()).toBeTruthy()
        const signinBody = await signin.json()
        expect(signinBody.code).toBe(0)
        expect(signinBody.data.token).toBeTruthy()
    })

    test('login page and dashboard', async ({page}) => {
        await page.goto('/login')
        await page.locator('input[type="text"]').first().fill(username)
        await page.locator('input[type="password"]').fill(password)
        await page.locator('.login-btn').click()
        await expect(page).not.toHaveURL(/\/login/, {timeout: 30_000})
        await expect(page.locator('.sidebar-container, .app-container').first()).toBeVisible({timeout: 15_000})
    })
})
