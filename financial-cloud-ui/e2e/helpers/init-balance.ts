import {expect, type APIRequestContext} from '@playwright/test'

export interface InitBalanceRow {
    id?: string | null
    originId?: string
    bookId?: string
    category?: number
    code?: string
    name?: string
    direction?: string
    parentId?: string | null
    idPath?: string
    level?: number
    balance?: number
    openingYearBalanceDebit?: number
    openingYearBalanceCredit?: number
    debitAmount?: number
    creditAmount?: number
    hasVoucher?: boolean
}

const CAPITAL_CODES = ['3001', '4001'] as const

export function resolveCapitalSubjectCode(rows: InitBalanceRow[]): string | undefined {
    for (const code of CAPITAL_CODES) {
        const row = rows.find((item) => item.code === code && (item.name || '').includes('实收资本'))
        if (row) {
            return code
        }
    }
    return rows.find((item) => (item.name || '').includes('实收资本'))?.code
}

export async function fetchInitBalanceList(
    request: APIRequestContext,
    headers: Record<string, string>,
): Promise<InitBalanceRow[]> {
    const res = await request.get('/api/base/init-balance/list', {headers})
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data || []) as InitBalanceRow[]
}

function buildInitBalanceChange(
    row: InitBalanceRow,
    bookId: string,
    openingDebit: number,
    openingCredit: number,
): InitBalanceRow {
    const debitAmount = Number(row.debitAmount ?? 0)
    const creditAmount = Number(row.creditAmount ?? 0)
    return {
        ...row,
        bookId,
        openingYearBalanceDebit: openingDebit,
        openingYearBalanceCredit: openingCredit,
        debitAmount,
        creditAmount,
        balance: openingDebit + debitAmount - openingCredit - creditAmount,
    }
}

/** 保存账套 B 标准期初：1002 借 100,000 + 实收资本贷 100,000 */
export async function saveStandardOpeningBalances(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
    amount = 100_000,
) {
    const rows = await fetchInitBalanceList(request, headers)
    const bank = rows.find((item) => item.code === '1002')
    const capitalCode = resolveCapitalSubjectCode(rows)
    const capital = capitalCode ? rows.find((item) => item.code === capitalCode) : undefined

    expect(bank, '缺少 1002 银行存款科目').toBeTruthy()
    expect(capital, '缺少实收资本科目').toBeTruthy()
    expect(bank!.hasVoucher, '1002 已有凭证，无法再改期初').toBeFalsy()
    expect(capital!.hasVoucher, '实收资本已有凭证，无法再改期初').toBeFalsy()

    const payload = [
        buildInitBalanceChange(bank!, bookId, amount, 0),
        buildInitBalanceChange(capital!, bookId, 0, amount),
    ]

    const res = await request.post('/api/base/init-balance/save', {headers, data: payload})
    const body = await res.json()
    expect(body.code, body.message || 'save opening balances failed').toBe(0)
    return {bankCode: bank!.code!, capitalCode: capital!.code!}
}
