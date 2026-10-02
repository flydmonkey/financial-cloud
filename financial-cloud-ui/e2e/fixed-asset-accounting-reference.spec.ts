import { expect, test } from '@playwright/test'
import { fetchBookSubjects, getCurrentUser, getCurrentTerm, loginViaApi } from './helpers/auth'
import { getVoucherDetail, runVoucherToPosted, voucherDetailToPayload } from './helpers/voucher'
import { fetchSubjectBalances } from './helpers/reports'

test('asset card generates purchase and exactly 950 depreciation without duplicate accrual', async ({ request }) => {
  test.setTimeout(120_000)
  const { headers } = await loginViaApi(request)
  const user = await getCurrentUser(request, headers)
  expect(user?.bookId).toBeTruthy()
  const bookId = user.bookId
  const term = await getCurrentTerm(request, headers, bookId)
  const subjects = await fetchBookSubjects(request, headers, bookId)
  function leaf(code: string) {
    const subject = subjects.find(s => (s.code === code || s.code?.startsWith(`${code}.`)) && !subjects.some(other => other.code?.startsWith(`${s.code}.`)))
    expect(subject, `缺少 ${code} 末级科目`).toBeTruthy()
    return subject!
  }
  const asset = leaf('1601')
  const depreciation = leaf('1602')
  const expense = leaf('5602')
  const bank = leaf('1002')
  const category = await (await request.post('/api/fixed-asset/category/save', { headers, data: { code: 'REF-FA', name: '验收电子设备', depreciationMethod: 'STRAIGHT_LINE', usefulLifeMonths: 60, residualRate: 5, fixedAssetSubjectId: asset.id, accumDeprSubjectId: depreciation.id } })).json()
  expect(category.code, category.message).toBe(0)
  expect(category.data).toBeTruthy()
  const previous = new Date(`${term}-01T00:00:00Z`)
  previous.setUTCMonth(previous.getUTCMonth() - 1)
  const startUseDate = `${previous.toISOString().slice(0, 7)}-10`
  const saved = await (await request.post('/api/fixed-asset/card/save', { headers, data: { code: 'REF-FA-001', name: '直线折旧验收设备', categoryId: category.data, startUseDate, entryPeriod: term, quantity: 1, originalValue: 60000, purchaseCounterpartSubjectId: bank.id, expenseSubjectId: expense.id } })).json()
  expect(saved.code, saved.message).toBe(0)
  expect(saved.data?.purchaseVoucherId, '购入凭证必须生成').toBeTruthy()
  const purchase = await getVoucherDetail(request, headers, saved.data.purchaseVoucherId)
  expect(Number(purchase.items.find((item: { subjectId: string }) => item.subjectId === asset.id)?.debitAmount)).toBe(60000)
  expect(Number(purchase.items.find((item: { subjectId: string }) => item.subjectId === bank.id)?.creditAmount)).toBe(60000)
  await runVoucherToPosted(request, headers, voucherDetailToPayload(purchase), purchase.id)
  const accrued = await (await request.post('/api/fixed-asset/depreciation/accrue', { headers, data: { yearPeriod: term, voucherDate: `${term}-28 00:00:00`, summary: '固定手算折旧：60000 × 95% ÷ 60 = 950' } })).json()
  expect(accrued.code, accrued.message).toBe(0)
  expect(Number(accrued.data?.totalAmount)).toBe(950)
  expect(accrued.data?.voucherId).toBeTruthy()
  const voucher = await getVoucherDetail(request, headers, accrued.data.voucherId)
  expect(Number(voucher.items.find((item: { subjectId: string }) => item.subjectId === expense.id)?.debitAmount)).toBe(950)
  expect(Number(voucher.items.find((item: { subjectId: string }) => item.subjectId === depreciation.id)?.creditAmount)).toBe(950)
  await runVoucherToPosted(request, headers, voucherDetailToPayload(voucher), voucher.id)
  const balance = async () => (await fetchSubjectBalances(request, headers, term)).find(row => row.subjectCode === depreciation.code)
  expect(Number((await balance())?.balance)).toBe(-950)
  await request.post('/api/fixed-asset/depreciation/accrue', { headers, data: { yearPeriod: term } })
  expect(Number((await balance())?.balance), '重复计提不能重复影响账簿').toBe(-950)
  const detail = await (await request.get(`/api/fixed-asset/card/get/${saved.data.id || saved.data.assetId}`, { headers })).json()
  expect(detail.code, detail.message).toBe(0)
  expect(Number(detail.data?.accumDepr)).toBe(950)
})
