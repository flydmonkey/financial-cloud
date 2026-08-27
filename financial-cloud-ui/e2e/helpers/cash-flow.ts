import {expect, type APIRequestContext} from '@playwright/test'
import {fetchCashFlowStatement, findCashFlowItem, num} from './reports'

export const CashFlowItems = {
    SALES_RECEIPT: '2-jy-sqxj',
    OPERATING_NET: '11-jy-lljh',
    INVESTING_NET: '24-tz-llje',
    FINANCING_NET: '34-cz-hdje',
    NET_INCREASE: '36-xj-djje',
    BEGINNING_CASH: '37-xj-qcye',
    ENDING_CASH: '38-xj-qmye',
} as const

export async function ensureCashFlowConfigInitialized(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
) {
    const fetch = await request.get('/api/config/cash-flow-balance/fetch', {headers})
    const fetched = await fetch.json()
    if (fetched.code === 0 && fetched.data?.configCashFlowBalances?.length > 0) {
        return
    }
    const bookRes = await request.get(`/api/book/get/${bookId}`, {headers})
    const book = (await bookRes.json()).data
    const update = await request.put('/api/book/update', {headers, data: book})
    const body = await update.json()
    expect(body.code, body.message || 'init cash flow config failed').toBe(0)

    const verify = await request.get('/api/config/cash-flow-balance/fetch', {headers})
    const verified = await verify.json()
    expect(verified.code).toBe(0)
    expect(verified.data?.configCashFlowBalances?.length).toBeGreaterThan(0)
}

export async function getPendingCashFlowItems(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    options?: {voucherId?: string; cashFlowItemType?: number},
) {
    const year = term.slice(0, 4)
    const month = Number(term.slice(5, 7))
    const params = new URLSearchParams({
        pageNumber: '1',
        pageSize: '50',
        year,
        month: String(month),
        cashFlowItemType: String(options?.cashFlowItemType ?? 1),
    })
    if (options?.voucherId) {
        params.set('voucherId', options.voucherId)
    }
    const res = await request.get(
        `/api/statement/cash-flow/get?${params.toString()}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data || []) as Array<{
        voucherId?: string
        voucherItemId?: string
        subjectCode?: string
        subjectName?: string
        debitAmount?: number | string
        creditAmount?: number | string
        cashFlowItemCode?: string
        id?: string | null
    }>
}

export async function specifyCashFlowForItem(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
    term: string,
    item: {
        voucherId?: string
        voucherItemId?: string
        id?: string | null
        debitAmount?: number | string
        creditAmount?: number | string
    },
    cashFlowItemCode: string,
    cashFlowItemType = 1,
) {
    const amount = num(item.debitAmount) || num(item.creditAmount)
    const res = await request.post('/api/statement/cash-flow/specify', {
        headers,
        data: {
            bookId,
            voucherId: item.voucherId,
            voucherDate: term,
            cashFlowItemType,
            isEdit: Boolean(item.id),
            voucherItemCashFlowDtos: [
                {
                    id: item.id || null,
                    voucherItemId: item.voucherItemId,
                    voucherId: item.voucherId,
                    cashFlowItemCode,
                    cashFlowBalance: amount,
                    cashFlowItemType,
                    bookId,
                },
            ],
        },
    })
    return res.json()
}

export async function getCashFlowTotals(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    const items = await fetchCashFlowStatement(request, headers, term)
    return {
        operatingNet: num(findCashFlowItem(items, CashFlowItems.OPERATING_NET)?.monthlyAmount),
        investingNet: num(findCashFlowItem(items, CashFlowItems.INVESTING_NET)?.monthlyAmount),
        financingNet: num(findCashFlowItem(items, CashFlowItems.FINANCING_NET)?.monthlyAmount),
        netIncrease: num(findCashFlowItem(items, CashFlowItems.NET_INCREASE)?.monthlyAmount),
        beginningCash: num(findCashFlowItem(items, CashFlowItems.BEGINNING_CASH)?.monthlyAmount),
        endingCash: num(findCashFlowItem(items, CashFlowItems.ENDING_CASH)?.monthlyAmount),
    }
}

export async function fetchCashFlowConfigItems(
    request: APIRequestContext,
    headers: Record<string, string>,
) {
    const res = await request.get('/api/config/cash-flow-balance/fetch', {headers})
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data?.configCashFlowBalances || []) as Array<{
        id?: string
        itemCode?: string
        itemName?: string
        balance?: number | string
        isEdit?: number
        direction?: number
    }>
}

export async function saveCashFlowConfigItemBalance(
    request: APIRequestContext,
    headers: Record<string, string>,
    itemCode: string,
    balance: number,
) {
    const items = await fetchCashFlowConfigItems(request, headers)
    const target = items.find((item) => item.itemCode === itemCode)
    expect(target?.id, `缺少现金流量配置项 ${itemCode}`).toBeTruthy()

    const res = await request.post('/api/config/cash-flow-balance/save', {
        headers,
        data: {
            cashFlowItemDtos: [{...target, balance}],
        },
    })
    const body = await res.json()
    expect(body.code, body.message || 'save cash flow config failed').toBe(0)
}
