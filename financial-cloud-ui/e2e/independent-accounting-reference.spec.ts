import fs from 'node:fs'
import { expect, test, type APIRequestContext } from '@playwright/test'
import { fetchBookSubjects, getCurrentTerm, loginViaApi, type BookSubjectRef } from './helpers/auth'
import { clearBooksViaScript, setupE2eBookViaApi } from './helpers/books'
import { saveStandardOpeningBalances } from './helpers/init-balance'
import { createAndPostVoucher, getVoucherDetail, runVoucherToPosted, voucherDetailToPayload } from './helpers/voucher'
import { createAndPostVoucherWithMainCashFlow, ensureCashFlowConfigInitialized, CashFlowItems } from './helpers/cash-flow'
import { fetchBalanceSheet, fetchCashFlowStatement, fetchSubjectBalances, getIncomeNetProfit, findCashFlowItem, sheetGrandTotal } from './helpers/reports'
import { checkoutCurrentPeriod, uncheckoutPeriod } from './helpers/settlement'

// Fixed answers are reviewed in docs/testing/accounting-acceptance.md, not computed from report rules.
const reference = JSON.parse(fs.readFileSync(new URL('../../docs/testing/accounting-reference-case.json', import.meta.url), 'utf8'))
type Event = { id: string; summary: string; debit: string; credit: string; amount: number; cashFlow?: string }
const codes: Record<string, string> = { bank: '1002', receivable: '1122', inventory: '1403', asset: '1601', depreciation: '1602', payable: '2202', salaryPayable: '2211', expense: '5602', revenue: '5001', cost: '5401' }

test.describe.serial('independent accountant reference', () => {
  test.setTimeout(120_000)
  let headers: Record<string, string>
  let bookId: string
  const subjects: Record<string, BookSubjectRef> = {}

  async function postEvents(request: APIRequestContext, term: string, events: Event[]) {
    for (const event of events) {
      const pair = { debit: subjects[event.debit], credit: subjects[event.credit] }
      if (event.cashFlow) {
        await createAndPostVoucherWithMainCashFlow(request, headers, bookId, term, `${event.id} ${event.summary}`, event.amount, pair, event.cashFlow)
      } else {
        await createAndPostVoucher(request, headers, bookId, `${event.id} ${event.summary}`, event.amount, pair)
      }
    }
  }

  async function assertReference(request: APIRequestContext, term: string, expected: Record<string, number>, profit = true) {
    const balances = await fetchSubjectBalances(request, headers, term)
    for (const key of ['bank', 'receivable', 'inventory', 'asset', 'depreciation', 'payable', 'salaryPayable']) {
      const row = balances.find(row => row.subjectCode === subjects[key].code)
      expect(row, `缺少 ${key} 科目余额行`).toBeTruthy()
      expect(Number(row!.balance), `${term} ${key} 独立预期`).toBeCloseTo(expected[key], 2)
    }
    if (profit) {
      const income = await getIncomeNetProfit(request, headers, term)
      expect(income.current, `${term} 当月利润`).toBeCloseTo(expected.profit, 2)
      expect(income.cumulative, `${term} 累计利润`).toBeCloseTo(expected.cumulativeProfit, 2)
    }
    const flow = await fetchCashFlowStatement(request, headers, term)
    for (const [key, code] of Object.entries({ cashBeginning: CashFlowItems.BEGINNING_CASH, cashEnding: CashFlowItems.ENDING_CASH, operatingNet: CashFlowItems.OPERATING_NET, investingNet: CashFlowItems.INVESTING_NET, netIncrease: CashFlowItems.NET_INCREASE })) {
      const row = findCashFlowItem(flow, code)
      expect(row, `缺少现金流行 ${code}`).toBeTruthy()
      expect(Number(row!.monthlyAmount), `${term} ${key}`).toBeCloseTo(expected[key], 2)
    }
  }

  async function assertClosedBalance(request: APIRequestContext, term: string, expected: number) {
    const sheet = await fetchBalanceSheet(request, headers, term)
    expect(sheetGrandTotal(sheet.items.assets)).toBeCloseTo(expected, 2)
    expect(sheetGrandTotal(sheet.items.liability)).toBeCloseTo(expected, 2)
  }

  test('reference setup: November opening bank and capital 100000', async ({ request }) => {
    expect(process.env.FC_DB_NAME, '仅允许隔离测试库').toMatch(/^financial_cloud_e2e_/)
    clearBooksViaScript()
    headers = (await loginViaApi(request)).headers
    bookId = await setupE2eBookViaApi(request, headers, { name: '独立会计验收', enableDate: reference.startPeriod })
    const all = await fetchBookSubjects(request, headers, bookId)
    for (const [key, code] of Object.entries(codes)) {
      // Pick a posting leaf where the root has detailed subjects.
      const candidates = all.filter(subject => subject.code === code || subject.code?.startsWith(`${code}.`))
      const leaf = candidates.find(subject => !all.some(other => other.code?.startsWith(`${subject.code}.`)))
      expect(leaf, `缺少必需科目 ${key}/${code}`).toBeTruthy()
      subjects[key] = leaf!
    }
    await saveStandardOpeningBalances(request, headers, bookId, reference.opening.bank)
    await ensureCashFlowConfigInitialized(request, headers, bookId)
    expect(await getCurrentTerm(request, headers, bookId)).toBe('2025-11')
  })

  test('November business entries match fixed balances, profit and cash flow', async ({ request }) => {
    await postEvents(request, '2025-11', reference.november.events)
    await assertReference(request, '2025-11', reference.november.expected)
  })

  test('full red reversal preserves original and has zero effect', async ({ request }) => {
    const correction = reference.correction
    const original = await createAndPostVoucher(request, headers, bookId, correction.summary, correction.amount, { debit: subjects.expense, credit: subjects.salaryPayable })
    const res = await request.post(`/api/voucher/reverse/${original.voucherId}`, { headers })
    const body = await res.json()
    expect(body.code, body.message).toBe(0)
    expect(body.data).toBeTruthy()
    const reversed = await getVoucherDetail(request, headers, body.data)
    expect(reversed.items.some((item: { debitAmount: number; creditAmount: number }) => Number(item.debitAmount) === -200 || Number(item.creditAmount) === -200)).toBeTruthy()
    await runVoucherToPosted(request, headers, voucherDetailToPayload(reversed), body.data)
    expect((await getVoucherDetail(request, headers, original.voucherId)).senderId).toBeTruthy()
    await assertReference(request, '2025-11', reference.november.expected)
  })

  test('November close, reopen and reclose preserve the independent totals', async ({ request }) => {
    expect((await checkoutCurrentPeriod(request, headers, bookId)).nextTerm).toBe('2025-12')
    await assertReference(request, '2025-11', reference.november.expected)
    await assertClosedBalance(request, '2025-11', 114000)
    const reopened = await uncheckoutPeriod(request, headers, '2025-11')
    expect(reopened.code, reopened.message).toBe(0)
    expect(await getCurrentTerm(request, headers, bookId)).toBe('2025-11')
    await assertReference(request, '2025-11', reference.november.expected)
    expect((await checkoutCurrentPeriod(request, headers, bookId)).nextTerm).toBe('2025-12')
    await assertClosedBalance(request, '2025-11', 114000)
  })

  test('December and year-end carry preserve fixed profit and January opening', async ({ request }) => {
    await postEvents(request, '2025-12', reference.december.events)
    await assertReference(request, '2025-12', reference.december.expected)
    expect((await checkoutCurrentPeriod(request, headers, bookId)).nextTerm).toBe('2026-01')
    await assertReference(request, '2025-12', reference.december.expected)
    await assertClosedBalance(request, '2025-12', 109000)
    const yearEnd = await fetchSubjectBalances(request, headers, '2025-12')
    expect(Number(yearEnd.find(row => row.subjectCode === '3103')?.balance)).toBeCloseTo(0, 2)
    const retained = yearEnd.find(row => row.subjectCode === '3104.02') ?? yearEnd.find(row => row.subjectCode === '3104')
    expect(retained, '缺少未分配利润').toBeTruthy()
    expect(Number(retained!.balance)).toBeCloseTo(-9000, 2)
    const january = await fetchSubjectBalances(request, headers, '2026-01')
    expect(Number(january.find(row => row.subjectCode === subjects.bank.code)?.balance)).toBeCloseTo(80000, 2)
    expect((await getIncomeNetProfit(request, headers, '2026-01')).cumulative).toBeCloseTo(0, 2)
  })
})
