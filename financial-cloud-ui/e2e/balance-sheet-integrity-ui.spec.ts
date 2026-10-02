import {expect, test} from '@playwright/test'

// This source-component harness requires Vite; deployed application tests use
// the production base URL independently.
if (process.env.E2E_COMPONENT_BASE_URL) {
    test.use({baseURL:process.env.E2E_COMPONENT_BASE_URL})
}

// Mount the actual report component against isolated responses; no financial data is written.
test('balance sheet shows actual difference, prints warning and clears stale warnings', async ({page}) => {
    const compiled = await page.request.get('/src/views/statement/balance-sheet.vue')
    expect(compiled.ok()).toBeTruthy()
    const routerModule = (await compiled.text()).match(/["']([^"']*vue-router\.js[^"']*)["']/)?.[1]
    expect(routerModule).toBeTruthy()
    const errors: string[] = []
    page.on('pageerror', (error) => errors.push(error.message))
    page.on('console', (message) => {
        if (message.type() === 'error') console.error(message.text())
    })
    page.on('response', (response) => {
        if (response.status() >= 400) console.error(`${response.status()} ${response.url()}`)
    })
    let difference: number | null = -10_000
    const items = () => ({
        assets: [{id: 'asset', itemCode: '1199', itemName: '资产总计', level: 1, sortIndex: 1, currentBalance: 90_000, initialBalance: 100_000}],
        liability: [{id: 'liability', itemCode: '2299', itemName: '负债和所有者权益总计', level: 1, sortIndex: 1, currentBalance: 90_000 - (difference ?? 0), initialBalance: 100_000}],
        balanced: difference === null ? null : Math.abs(difference) <= 0.01,
        assetTotal: 90_000,
        liabilityTotal: 90_000 - (difference ?? 0),
        balanceDifference: difference,
    })
    await page.route('**/api/**', async (route) => {
        const url = new URL(route.request().url())
        if (!url.pathname.startsWith('/api/')) {
            await route.continue()
            return
        }
        const data = url.pathname === '/api/statement/balance-sheet' ? {items: items()}
            : url.pathname.includes('balance-sheet') ? items() : []
        await route.fulfill({json: {code: 0, data}})
    })
    await page.route('**/__balance-sheet-qa', (route) => route.fulfill({
        contentType: 'text/html; charset=utf-8',
        body: `<!doctype html><html><head><meta charset="utf-8"><title>资产负债表回归</title></head><body><div id="app"></div>
        <script type="module">
        import {createApp} from '/node_modules/.vite/deps/vue.js';
        import ElementPlus from '/node_modules/.vite/deps/element-plus.js';
        import '/node_modules/element-plus/dist/index.css';
        import store from '/src/store/index.ts';
        import books from '/src/store/modules/bookStore.ts';
        import i18n from '/src/languages/index.ts';
        import {useDict} from '/src/utils/Dict.ts';
        import {createRouter, createMemoryHistory} from '${routerModule}';
        import Report from '/src/views/statement/balance-sheet.vue';
        const book = books(store);
        Object.assign(book, {bookId: 'qa', termCurrent: '2026-08', termStart: '2026-01', setList: [{id: 'qa', companyName: '验收账套'}]});
        const app = createApp(Report);
        app.config.globalProperties.useDict = useDict;
        app.use(store).use(i18n).use(ElementPlus).use(createRouter({history: createMemoryHistory(), routes: [{path: '/', component: {template: '<div />'}}]}));
        app.mount('#app');
        </script></body></html>`,
    }))
    await page.goto('/__balance-sheet-qa')
    await expect(page).toHaveTitle('资产负债表回归')
    expect(errors).toEqual([])
    const warning = page.getByTestId('balance-sheet-warning')
    await expect(warning).toContainText('资产负债表不平')
    await expect(warning).toContainText('-10,000.00')
    await expect(warning).toContainText('损益结转、期初余额及报表取数规则')
    await expect(page.locator('#export-table')).toContainText('90,000.00')
    await expect(page.locator('#export-table')).toContainText('100,000.00')
    const popupPromise = page.waitForEvent('popup')
    await page.getByRole('button', {name: '打印', exact: true}).click()
    const popup = await popupPromise
    await expect(popup.locator('.warning')).toContainText('-10,000.00')
    await expect(popup.locator('table')).toContainText('100,000.00')
    await popup.close()

    difference = 10_000
    await page.getByRole('button', {name: '刷新', exact: true}).click()
    await expect(warning).toContainText('总计 80,000.00')
    difference = 0.01
    await page.getByRole('button', {name: '刷新', exact: true}).click()
    await expect(warning).toHaveCount(0)
    difference = null
    await page.getByRole('button', {name: '刷新', exact: true}).click()
    await expect(warning).toHaveCount(0)
    difference = -10_000
    await page.getByRole('button', {name: '刷新', exact: true}).click()
    await expect(warning).toBeVisible()
    await page.getByRole('button', {name: '启用编辑', exact: true}).click()
    await expect(warning).toHaveCount(0)
    expect(errors).toEqual([])
})
