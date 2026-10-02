import fs from 'node:fs'
import path from 'node:path'
import os from 'node:os'
import { expect, test, type Page } from '@playwright/test'
import { loginViaApi, loginViaUi, getCurrentTerm } from './helpers/auth'
import { clearBooksViaScript, setupE2eBookViaApi } from './helpers/books'

const evidenceDir = process.env.QA_ARTIFACT_DIR || path.join(os.tmpdir(), 'jinbooks-guided-delivery')
const componentBase = process.env.E2E_COMPONENT_BASE_URL || process.env.E2E_BASE_URL || 'http://localhost:3154'

function captureErrors(page: Page) {
  const errors: string[] = []
  page.on('pageerror', error => { errors.push(error.message); console.error(error.message) })
  page.on('console', message => {
    if (message.type() !== 'error') return
    const text = message.text()
    if (text.includes('ERR_BLOCKED_BY_LOCAL_NETWORK_ACCESS_CHECKS') || text.includes('[vite] failed to connect to websocket')) {
      test.info().annotations.push({ type: 'environment', description: 'Chromium blocks Vite HMR websocket in fulfilled component harness; HTTP component interactions remain tested.' })
      return
    }
    errors.push(text)
    console.error(text)
  })
  return errors
}

test('homepage guide works at desktop and mobile sizes', async ({ page }) => {
  const errors = captureErrors(page)
  await loginViaUi(page)
  await page.goto('/index')
  const guide = page.locator('.accounting-guide')
  await expect(guide).toContainText('做账指引')
  await expect(guide.locator('li')).toHaveCount(5)
  fs.mkdirSync(evidenceDir, { recursive: true })
  await page.screenshot({ path: path.join(evidenceDir, 'guide-desktop.png') })
  await page.setViewportSize({ width: 390, height: 844 })
  await expect(guide.getByRole('button', { name: '进入月结向导' })).toBeVisible()
  expect(await guide.evaluate(element => element.scrollWidth <= element.clientWidth)).toBe(true)
  await page.screenshot({ path: path.join(evidenceDir, 'guide-mobile.png') })
  await guide.getByRole('button', { name: '收起指引' }).click()
  await expect(guide.locator('li')).toHaveCount(0)
  await guide.getByRole('button', { name: '展开指引' }).click()
  await guide.getByRole('button', { name: '进入月结向导' }).click()
  await expect(page).toHaveURL(/\/settlement\/settle-period/)
  await expect(page.getByText('人工确认', { exact: true }).first()).toBeVisible()
  expect(errors).toEqual([])
})

test('guide limits links to available authorized routes', async ({ page }) => {
  const errors = captureErrors(page)
  const compiledGuide = await page.request.get(`${componentBase}/src/views/dashboard/accounting/AccountingGuide.vue`)
  expect(compiledGuide.ok()).toBeTruthy()
  const routerModule = (await compiledGuide.text()).match(/["']([^"']*vue-router\.js[^"']*)["']/)?.[1]
  expect(routerModule, 'Vite component must expose its resolved router module').toBeTruthy()
  await page.route('**/__guide-permissions', route => route.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><meta charset="utf-8"></head><body><div id="app"></div><script type="module">
    import { createApp, h } from '/node_modules/.vite/deps/vue.js';
    import { createRouter, createMemoryHistory } from '${routerModule}';
    import ElementPlus from '/node_modules/.vite/deps/element-plus.js';
    import '/node_modules/element-plus/dist/index.css';
    import Guide from '/src/views/dashboard/accounting/AccountingGuide.vue';
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: { render: () => h(Guide) } }, { path: '/statement/balance-sheet', component: { render: () => h('p', 'report') } }] });
    createApp(Guide).use(router).use(ElementPlus).mount('#app');
  </script></body></html>` }))
  await page.goto(`${componentBase}/__guide-permissions`)
  await expect(page.getByRole('button', { name: '核对资产负债表' })).toBeVisible()
  await expect(page.getByRole('button', { name: '录凭证', exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: '账套管理' })).toHaveCount(0)
  await expect(page.getByText('需要相应页面权限，请联系账套管理员。')).toHaveCount(4)
  expect(errors).toEqual([])
})

test('closing checks explain failures and avoid unknown no-op links', async ({ page }) => {
  const errors = captureErrors(page)
  await page.route('**/__closing-checks', route => route.fulfill({ contentType: 'text/html', body: `<!doctype html><html><head><meta charset="utf-8"></head><body><div id="app"></div><script type="module">
    import { createApp, h, ref } from '/node_modules/.vite/deps/vue.js';
    import ElementPlus from '/node_modules/.vite/deps/element-plus.js';
    import '/node_modules/element-plus/dist/index.css';
    import Verify from '/src/views/settlement/wizard/StepVerify.vue';
    const rows = [{ item: '未完成凭证检查', result: false, hard: true, reason: '存在未过账凭证' }, { item: '往来款项（应收应付/账龄）', result: true, warning: true }, { item: '未知检查项', result: false, hard: true }];
    createApp({ setup() { const jumped = ref(''); return () => h('div', [h(Verify, { verifyRows: rows, isVerify: false, loadingVerify: false, verifyError: '检查请求失败，请重新检查', onJump: item => jumped.value = item }), h('p', { id: 'jumped' }, jumped.value)]); } }).use(ElementPlus).mount('#app');
  </script></body></html>` }))
  await page.goto(`${componentBase}/__closing-checks`)
  await expect(page.getByText('检查请求失败，请重新检查')).toBeVisible()
  const rows = page.locator('.el-table__body tr')
  await expect(rows.nth(0)).toContainText('由其他审核人审核后过账')
  await expect(rows.nth(1)).toContainText('核对账龄')
  await expect(rows.nth(2).getByRole('button', { name: '去处理' })).toHaveCount(0)
  await rows.nth(1).getByRole('button', { name: '去处理' }).click()
  await expect(page.locator('#jumped')).toHaveText('往来款项（应收应付/账龄）')
  expect(errors).toEqual([])
})

test('December wizard closes to January and delivers December', async ({ page, request }) => {
  test.setTimeout(120_000)
  const errors = captureErrors(page)
  expect(process.env.FC_DB_NAME).toMatch(/^financial_cloud_e2e_/)
  clearBooksViaScript()
  const auth = await loginViaApi(request)
  const bookId = await setupE2eBookViaApi(request, auth.headers, { name: '跨年交付验收', enableDate: '2025-12' })
  let verifyRequests = 0
  let checkoutRequests = 0
  await page.route('**/api/settlement/verify*', async route => {
    verifyRequests++
    if (verifyRequests === 1) return route.fulfill({ json: { code: 0, message: '成功', data: [] } })
    if (verifyRequests === 2) return route.fulfill({ json: { code: 500123, message: '验收模拟：检查请求中断', data: null } })
    await route.continue()
  })
  await page.route('**/api/settlement/checkout*', async route => {
    checkoutRequests++
    if (checkoutRequests === 1) return route.fulfill({ json: { code: 500124, message: '验收模拟：结账被阻止', data: null } })
    await route.continue()
  })
  await loginViaUi(page)
  await page.goto('/settlement/settle-period')
  await page.locator('.wizard-footer .el-checkbox').click()
  await page.getByRole('button', { name: '下一步', exact: true }).click()
  await page.getByRole('button', { name: '下一步：计提与结转' }).click()
  await expect(page.getByText('qm_jz_sr', { exact: true })).toBeVisible()
  // Empty book has no carry balances; explicit generation confirms N/A.
  for (let index = 0; index < 3; index++) {
    const generate = page.getByRole('button', { name: '生成并过账' })
    if (await generate.count()) {
      const count = await generate.count()
      await generate.first().click()
      await expect(generate).toHaveCount(count - 1)
    }
  }
  await expect(page.getByRole('button', { name: '下一步：系统校验' })).toBeEnabled()
  await page.getByRole('button', { name: '下一步：系统校验' }).click()
  await expect(page.getByText('未取得检查结果，请重新检查。')).toBeVisible()
  await expect(page.getByRole('button', { name: '下一步：结账' })).toBeDisabled()
  await page.getByRole('button', { name: '重新检查' }).click()
  await expect(page.getByText('验收模拟：检查请求中断')).toBeVisible()
  await expect(page.getByRole('button', { name: '下一步：结账' })).toBeDisabled()
  await page.getByRole('button', { name: '重新检查' }).click()
  await page.getByRole('button', { name: '下一步：结账' }).click()
  await page.getByRole('button', { name: '结账', exact: true }).click()
  await expect(page.locator('.checkout-result')).toContainText('验收模拟：结账被阻止')
  await page.getByRole('button', { name: '返回系统校验' }).click()
  await expect(page.getByRole('button', { name: '下一步：结账' })).toBeDisabled()
  await expect(page.getByText('暂无校验结果，请点击「重新检查」')).toBeVisible()
  await page.getByRole('button', { name: '重新检查' }).click()
  await page.getByRole('button', { name: '下一步：结账' }).click()
  await page.getByRole('button', { name: '结账', exact: true }).click()
  await expect(page.getByText('结账成功', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '交付本期账本包' })).toBeVisible()
  await expect(page.getByText('2025-12月结已完成', { exact: false })).toBeVisible()
  expect(await getCurrentTerm(request, auth.headers, bookId)).toBe('2026-01')
  await expect(page.locator('.el-message--error')).toHaveCount(0)
  fs.mkdirSync(evidenceDir, { recursive: true })
  await page.screenshot({ path: path.join(evidenceDir, 'closed-december.png') })
  await page.getByRole('button', { name: '核对本期报表' }).click()
  await expect(page).toHaveURL(/balance-sheet\?yearPeriod=2025-12/)
  await expect(page.locator('.queryForm .el-date-editor input').first()).toHaveValue('2025年12期')
  await page.getByRole('button', { name: '交付所选账期' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toBeVisible()
  await expect(dialog.locator('input').first()).toHaveValue('2025-12')
  const downloaded = page.waitForEvent('download')
  await dialog.getByRole('button', { name: '确认导出' }).click()
  expect((await downloaded).suggestedFilename()).toContain('2025-12')
  expect(errors).toEqual([])
})
