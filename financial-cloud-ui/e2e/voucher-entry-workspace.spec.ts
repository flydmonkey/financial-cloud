import { expect, test } from '@playwright/test'
import { getCurrentUser, loginViaApi, loginViaUi } from './helpers/auth'
import {
  buildBalancedVoucherPayload,
  createDraftVoucher,
  getNextWordNum,
} from './helpers/voucher'

/**
 * voucher-entry-workspace：留页、邻证、整页录入工作台
 */
test.describe('voucher entry workspace', () => {
  test('draft stays addressable by id for workspace reload', async ({ request }) => {
    const auth = await loginViaApi(request)
    const user = await getCurrentUser(request, auth.headers)
    test.skip(!user?.bookId, '无账套')

    const payload = await buildBalancedVoucherPayload(
      request,
      auth.headers,
      user.bookId,
      'workspace-stay',
    )
    const id = await createDraftVoucher(request, auth.headers, payload)
    const detail = await request.get(`/api/voucher/get/${id}`, { headers: auth.headers })
    const body = await detail.json()
    expect(body.code).toBe(0)
    expect(String(body.data.id)).toBe(String(id))
    expect(body.data.status).toBe('draft')
  })

  test('two drafts ordered by date+word in fetch list', async ({ request }) => {
    const auth = await loginViaApi(request)
    const user = await getCurrentUser(request, auth.headers)
    test.skip(!user?.bookId, '无账套')

    const firstPayload = await buildBalancedVoucherPayload(
      request,
      auth.headers,
      user.bookId,
      'workspace-nav-1',
    )
    const ym = `${firstPayload.voucherYear}-${String(firstPayload.voucherMonth).padStart(2, '0')}`
    firstPayload.voucherDate = `${ym}-10`
    const id1 = await createDraftVoucher(request, auth.headers, firstPayload)

    const wordNum2 = await getNextWordNum(request, auth.headers, ym)
    const secondPayload = {
      ...firstPayload,
      wordNum: wordNum2,
      voucherDate: `${ym}-11`,
      items: firstPayload.items.map((item) => ({ ...item, summary: 'workspace-nav-2' })),
    }
    const id2 = await createDraftVoucher(request, auth.headers, secondPayload)

    const listRes = await request.get('/api/voucher/fetch', {
      headers: auth.headers,
      params: {
        pageNumber: 1,
        pageSize: 500,
        bookId: user.bookId,
        includeItems: false,
        orderByColumn: 'voucherDate,wordHead,wordNum',
        isAsc: 'asc,asc,asc',
      },
    })
    const listBody = await listRes.json()
    expect(listBody.code).toBe(0)
    const records = listBody.data?.records || []
    const i1 = records.findIndex((row: any) => String(row.id) === String(id1))
    const i2 = records.findIndex((row: any) => String(row.id) === String(id2))
    expect(i1).toBeGreaterThanOrEqual(0)
    expect(i2).toBeGreaterThan(i1)
  })

  const uiTest = process.env.E2E_ENABLE_UI === '1' ? test : test.skip

  uiTest('list open and new go to full-page workspace', async ({ page, request }) => {
    const auth = await loginViaApi(request)
    const user = await getCurrentUser(request, auth.headers)
    test.skip(!user?.bookId, '无账套')

    const payload = await buildBalancedVoucherPayload(
      request,
      auth.headers,
      user.bookId,
      'workspace-ui',
    )
    const id = await createDraftVoucher(request, auth.headers, payload)

    await loginViaUi(page)
    await page.goto(`/voucher/voucher-edit?id=${id}`)
    await expect(page.getByRole('button', { name: '返回列表' })).toBeVisible({ timeout: 15_000 })
    await expect(page.getByRole('button', { name: '上一张' })).toBeVisible()
    await expect(page.getByRole('button', { name: '下一张' })).toBeVisible()
    await expect(page.getByRole('button', { name: '新建凭证' })).toBeVisible()
    await expect(page.getByText(/借贷平衡/)).toBeVisible()
    await expect(page.locator('.el-drawer')).toHaveCount(0)

    await page.getByRole('button', { name: '添加分录' }).click()
    await expect(page.getByRole('button', { name: '打印' })).toBeVisible()

    await page.getByRole('button', { name: '新建凭证' }).click()
    await expect(page).toHaveURL(/\/voucher\/voucher-edit(?:\?|$)/)
    await expect(page).not.toHaveURL(/[?&]id=/)
  })
})
