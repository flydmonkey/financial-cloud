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

/**
 * TEST-BS-DEEP Golden Dataset 期初（借贷平衡 340,000 = 20,000 + 320,000）
 *
 * | 科目 | 借方 | 贷方 | 报表归属 |
 * | 1001 | 10,000 | | 货币资金 |
 * | 1002 | 90,000 | | 货币资金 |
 * | 1122 | | 20,000 | 预收款项（重分类） |
 * | 1123 | 25,000 | | 预付款项 |
 * | 2202 | 15,000 | | 预付款项（重分类） |
 * | 1403 | 30,000 | | 存货（合并） |
 * | 1405 | 20,000 | | 存货（合并） |
 * | 1601 | 200,000 | | 固定资产 |
 * | 1602 | | 50,000 | 固定资产（净值扣备抵） |
 * | 3001 | | 320,000 | 实收资本 |
 */
export async function saveGoldenBalanceSheetOpeningBalances(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
) {
    const rows = await fetchInitBalanceList(request, headers)
    const mustFind = (codes: string[], label: string) => {
        for (const code of codes) {
            const row = rows.find((item) => item.code === code)
            if (row) {
                return row
            }
        }
        expect(undefined, `缺少科目 ${label}（${codes.join('/')}）`).toBeTruthy()
        throw new Error(`missing ${label}`)
    }

    const touched = [
        mustFind(['1001'], '1001 库存现金'),
        mustFind(['1002'], '1002 银行存款'),
        mustFind(['1122'], '1122 应收账款'),
        mustFind(['1123', '1151'], '1123 预付账款'),
        mustFind(['2202', '2121'], '2202 应付账款'),
        mustFind(['1403'], '1403 原材料'),
        mustFind(['1405'], '1405 库存商品'),
        mustFind(['1601'], '1601 固定资产'),
        mustFind(['1602'], '1602 累计折旧'),
        mustFind([...CAPITAL_CODES], '实收资本'),
    ]
    for (const row of touched) {
        expect(row.hasVoucher, `${row.code} 已有凭证，Golden Dataset 需空白账套`).toBeFalsy()
    }

    const [
        cash, bank, receivable, prepaid, payable,
        rawMaterial, finishedGoods, fixedAsset, accumulatedDepreciation, capital,
    ] = touched
    const payload = [
        buildInitBalanceChange(cash, bookId, 10_000, 0),
        buildInitBalanceChange(bank, bookId, 90_000, 0),
        buildInitBalanceChange(receivable, bookId, 0, 20_000),
        buildInitBalanceChange(prepaid, bookId, 25_000, 0),
        buildInitBalanceChange(payable, bookId, 15_000, 0),
        buildInitBalanceChange(rawMaterial, bookId, 30_000, 0),
        buildInitBalanceChange(finishedGoods, bookId, 20_000, 0),
        buildInitBalanceChange(fixedAsset, bookId, 200_000, 0),
        buildInitBalanceChange(accumulatedDepreciation, bookId, 0, 50_000),
        buildInitBalanceChange(capital, bookId, 0, 320_000),
    ]

    const res = await request.post('/api/base/init-balance/save', {headers, data: payload})
    const body = await res.json()
    expect(body.code, body.message || 'save golden opening balances failed').toBe(0)
    return {
        capitalCode: capital.code!,
        expected: {
            monetary: 100_000,
            advanceReceipt: 20_000,
            prepaid: 40_000,
            receivable: 0,
            payable: 0,
            inventory: 50_000,
            fixedAssetNet: 150_000,
            capital: 320_000,
            assetTotal: 340_000,
        },
    }
}
