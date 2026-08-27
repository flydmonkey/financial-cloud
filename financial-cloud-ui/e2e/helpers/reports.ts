import {expect, type APIRequestContext} from '@playwright/test'

export interface ReportLineItem {
    itemCode?: string
    itemName?: string
    currentBalance?: number | string
    initialBalance?: number | string
    cumulativeBalance?: number | string
    monthlyAmount?: number | string
    yearToDateAmount?: number | string
}

export interface ReportSnapshot {
    assetTotal: number | null
    liabilityTotal: number | null
    incomeNetProfit: number | null
    cashEnding: number | null
}

export function sheetGrandTotal(items: Array<{itemCode?: string; currentBalance?: number | string}>) {
    const totals = items.filter((item) => (item.itemCode || '').endsWith('99'))
    if (totals.length === 0) {
        return null
    }
    const grand = totals.reduce((max, item) =>
        ((item.itemCode || '') > (max.itemCode || '') ? item : max),
    )
    return Number(grand.currentBalance ?? 0)
}

export function assertBalanceSheetTrial(
    assets: Array<{itemCode?: string; currentBalance?: number | string; itemName?: string}>,
    liability: Array<{itemCode?: string; currentBalance?: number | string; itemName?: string}>,
) {
    expect(assets.length + liability.length).toBeGreaterThan(0)
    expect(assets.some((item) => (item.itemName || '').includes('总计'))).toBeTruthy()
    expect(liability.some((item) => (item.itemName || '').includes('总计'))).toBeTruthy()

    const assetTotal = sheetGrandTotal(assets)
    const liabilityTotal = sheetGrandTotal(liability)
    expect(assetTotal, '资产负债表缺少资产总计行').not.toBeNull()
    expect(liabilityTotal, '资产负债表缺少负债及权益总计行').not.toBeNull()
    expect(Math.abs(assetTotal! - liabilityTotal!)).toBeLessThanOrEqual(0.01)
}

export function num(value: number | string | null | undefined): number {
    if (value == null || value === '') {
        return 0
    }
    return Number(value)
}

export function findIncomeItem(items: ReportLineItem[], itemCode: string) {
    return items.find((item) => String(item.itemCode) === itemCode)
}

export function findIncomeItemByName(items: ReportLineItem[], namePart: string) {
    return items.find((item) => (item.itemName || '').includes(namePart))
}

export function findBalanceSheetItemByName(
    items: ReportLineItem[],
    namePart: string,
): ReportLineItem | undefined {
    return items.find((item) => (item.itemName || '').includes(namePart))
}

export function findCashFlowItem(items: ReportLineItem[], itemCode: string) {
    return items.find((item) => item.itemCode === itemCode)
}

export async function fetchBalanceSheet(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const res = await request.get(
        `/api/statement/balance-sheet?periodType=${periodType}&reportDate=${term}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return body.data
}

export async function fetchIncomeStatement(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const res = await request.get(
        `/api/statement/income?periodType=${periodType}&reportDate=${term}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return body.data
}

export async function fetchCashFlowStatement(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const res = await request.get(
        `/api/statement/cash-flow?periodType=${periodType}&reportDate=${term}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return body.data || []
}

export async function fetchVoucherSummary(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const res = await request.get(
        `/api/statement/voucher-summary?periodType=${periodType}&reportDate=${term}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data || []) as Array<{
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>
}

export async function fetchSubjectBalances(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    periodType = 'month',
) {
    const res = await request.get(
        `/api/statement/subject-balance?periodType=${periodType}&reportDate=${term}&showAll=true`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data || []) as Array<{
        sourceId?: string
        parentId?: string
        subjectCode?: string
        isVoucher?: string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
        balance?: number | string
    }>
}

/** 试算平衡：仅汇总末级科目，避免父级行重复计入 */
export function leafSubjectBalanceRows<T extends {sourceId?: string; parentId?: string}>(rows: T[]): T[] {
    const parentIds = new Set(rows.map((row) => row.parentId).filter((id): id is string => Boolean(id)))
    return rows.filter((row) => row.sourceId && !parentIds.has(row.sourceId))
}

export function subjectBalanceTrialTotals(
    rows: Array<{
        sourceId?: string
        parentId?: string
        isVoucher?: string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>,
) {
    const voucherLeafRows = leafSubjectBalanceRows(rows).filter((row) => row.isVoucher === 'y')
    const debitTotal = voucherLeafRows.reduce((sum, row) => sum + Math.abs(num(row.currentPeriodDebit)), 0)
    const creditTotal = voucherLeafRows.reduce((sum, row) => sum + Math.abs(num(row.currentPeriodCredit)), 0)
    return {debitTotal, creditTotal, leafCount: voucherLeafRows.length}
}

export function getSubjectBalance(
    records: Array<{subjectCode?: string; balance?: number | string}>,
    subjectCode: string,
): number {
    const row = records.find((item) => item.subjectCode === subjectCode)
    return num(row?.balance)
}

export function subjectPeriodAmount(
    records: Array<{
        subjectCode?: string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>,
    subjectCode: string,
): number {
    const row = records.find((item) => item.subjectCode === subjectCode)
    if (!row) {
        return 0
    }
    return num(row.currentPeriodDebit) + num(row.currentPeriodCredit)
}

export function subjectPeriodNet(
    records: Array<{
        subjectCode?: string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>,
    subjectCode: string,
): number {
    const row = records.find((item) => item.subjectCode === subjectCode)
    if (!row) {
        return 0
    }
    return num(row.currentPeriodDebit) - num(row.currentPeriodCredit)
}

export async function captureReportSnapshot(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
): Promise<ReportSnapshot> {
    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    const income = await fetchIncomeStatement(request, headers, term)
    const cashFlow = await fetchCashFlowStatement(request, headers, term)

    const assets = balanceSheet?.items?.assets || []
    const liability = balanceSheet?.items?.liability || []
    const incomeItems = income?.items || []
    const cashEndingItem = findCashFlowItem(cashFlow, '62-xj-xjqk')
    const monetaryItem = findBalanceSheetItemByName(assets, '货币资金')

    return {
        assetTotal: sheetGrandTotal(assets),
        liabilityTotal: sheetGrandTotal(liability),
        incomeNetProfit: num(findIncomeItem(incomeItems, '4')?.currentBalance),
        cashEnding: cashEndingItem
            ? num(cashEndingItem.monthlyAmount ?? cashEndingItem.yearToDateAmount)
            : num(monetaryItem?.currentBalance),
    }
}

export async function assertReportsBalanced(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    assertBalanceSheetTrial(
        balanceSheet?.items?.assets || [],
        balanceSheet?.items?.liability || [],
    )
}

export async function getIncomeNetProfit(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    const income = await fetchIncomeStatement(request, headers, term)
    const item = findIncomeItem(income?.items || [], '4')
    return {
        current: num(item?.currentBalance),
        cumulative: num(item?.cumulativeBalance),
    }
}

export async function getBalanceSheetTotals(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    const balanceSheet = await fetchBalanceSheet(request, headers, term)
    const assets = balanceSheet?.items?.assets || []
    const liability = balanceSheet?.items?.liability || []
    return {
        assetTotal: sheetGrandTotal(assets),
        liabilityTotal: sheetGrandTotal(liability),
    }
}

export function assertSubjectBalanceTrial(
    records: Array<{
        closingBalanceDebit?: number | string
        closingBalanceCredit?: number | string
        yearToDateDebit?: number | string
        yearToDateCredit?: number | string
        currentPeriodDebit?: number | string
        currentPeriodCredit?: number | string
    }>,
    mode: 'closing' | 'yearToDate' | 'currentPeriod' = 'yearToDate',
) {
    const fieldMap = {
        closing: ['closingBalanceDebit', 'closingBalanceCredit'],
        yearToDate: ['yearToDateDebit', 'yearToDateCredit'],
        currentPeriod: ['currentPeriodDebit', 'currentPeriodCredit'],
    } as const
    const [debitField, creditField] = fieldMap[mode]
    const debitTotal = records.reduce((sum, row) => sum + num(row[debitField]), 0)
    const creditTotal = records.reduce((sum, row) => sum + num(row[creditField]), 0)
    expect(Math.abs(debitTotal - creditTotal)).toBeLessThanOrEqual(0.01)
    return {debitTotal, creditTotal}
}

export async function exportStatementReport(
    request: APIRequestContext,
    headers: Record<string, string>,
    path: string,
    term: string,
    periodType = 'month',
    extraParams: Record<string, string> = {},
) {
    const params = new URLSearchParams({periodType, reportDate: term, ...extraParams})
    const res = await request.get(`${path}?${params.toString()}`, {headers})
    const contentType = res.headers()['content-type'] || ''
    const body = await res.body()
    if (contentType.includes('json')) {
        const text = body.toString('utf-8')
        throw new Error(`expected spreadsheet export from ${path}, got JSON: ${text.slice(0, 300)}`)
    }
    expect(res.ok(), `export ${path} HTTP ${res.status()}`).toBeTruthy()
    expect(contentType).toMatch(/spreadsheet|octet-stream|excel|zip|openxmlformats/i)
    expect(body.length).toBeGreaterThan(500)
    // xlsx is a zip package
    expect(body.subarray(0, 2).toString('latin1'), `export ${path} is not xlsx/zip`).toBe('PK')
    return body
}
