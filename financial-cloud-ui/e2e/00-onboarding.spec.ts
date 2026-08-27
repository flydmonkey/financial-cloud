import {expect, test, type Page} from '@playwright/test'
import {
    clearBooksViaScript,
    getOnboardingStatus,
    listAccessibleBooks,
} from './helpers/books'
import {loginViaApi, password, username} from './helpers/auth'

function onboardingField(page: Page, label: string) {
    return page.locator('.onboarding-form .el-form-item').filter({
        has: page.locator('.el-form-item__label', {hasText: label}),
    })
}

async function selectFirstOption(page: Page, label: string) {
    await onboardingField(page, label).locator('.el-select').click()
    const item = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first()
    await expect(item).toBeVisible({timeout: 15_000})
    await item.click()
}

test.describe.serial('onboarding', () => {
    test('API reports needsSetup after clearing books', async ({request}) => {
        clearBooksViaScript()
        const auth = await loginViaApi(request)
        const status = await getOnboardingStatus(request, auth.headers)
        expect(status.needsSetup).toBe(true)
        expect(await listAccessibleBooks(request, auth.headers)).toHaveLength(0)
    })

    test('UI wizard creates book and enters dashboard', async ({page}) => {
        test.setTimeout(120_000)
        await page.goto('/login')
        await page.locator('input[type="text"]').first().fill(username)
        await page.locator('input[type="password"]').fill(password)
        await page.locator('.login-btn').click()

        await expect(page).toHaveURL(/\/onboarding/, {timeout: 30_000})
        await expect(page.getByText('欢迎使用，请先创建账套')).toBeVisible()

        await page.getByRole('textbox', {name: '账套名称'}).fill('E2E账套')
        await page.getByRole('textbox', {name: '单位名称'}).fill('E2E测试公司')

        await selectFirstOption(page, '会计准则')

        await onboardingField(page, '建账期间').locator('.el-date-editor').click()
        await page.getByRole('gridcell', {name: '1 月', exact: true}).click()

        await selectFirstOption(page, '纳税性质')

        const setupResponse = page.waitForResponse(
            (resp) => resp.url().includes('/book/setup') && resp.request().method() === 'POST',
            {timeout: 60_000},
        )
        await page.getByRole('button', {name: '创建并进入系统'}).click()
        const setupRes = await setupResponse
        expect(setupRes.ok()).toBeTruthy()
        const setupBody = await setupRes.json()
        expect(setupBody.code, setupBody.message || 'setup failed').toBe(0)

        await page.waitForURL((url) => !url.pathname.includes('/onboarding'), {timeout: 60_000})
        await expect(page.locator('.sidebar-container').first()).toBeVisible({timeout: 30_000})
    })

    test('API reports onboarding complete after setup', async ({request}) => {
        const auth = await loginViaApi(request)
        const status = await getOnboardingStatus(request, auth.headers)
        expect(status.needsSetup).toBe(false)
        expect(await listAccessibleBooks(request, auth.headers)).not.toHaveLength(0)
    })
})
