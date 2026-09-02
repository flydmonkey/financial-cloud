import {expect, type APIRequestContext} from '@playwright/test'
import {getCurrentTerm} from './auth'

export async function verifySettlement(
    request: APIRequestContext,
    headers: Record<string, string>,
) {
    const res = await request.get('/api/settlement/verify', {headers})
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code, body.message || 'verify failed').toBe(0)
    const checks = body.data || []
    expect(checks.length).toBeGreaterThan(0)
    for (const item of checks) {
        expect(item.result, `结账检查未通过: ${item.item}`).toBeTruthy()
    }
    return checks
}

export async function checkoutCurrentPeriod(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
) {
    const closedTerm = await getCurrentTerm(request, headers, bookId)
    const year = closedTerm.slice(0, 4)
    const res = await request.get(`/api/settlement/checkout?year=${year}`, {headers})
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code, body.message || 'checkout failed').toBe(0)

    const nextTerm = await getCurrentTerm(request, headers, bookId)
    expect(nextTerm).not.toBe(closedTerm)
    return {closedTerm, nextTerm}
}

export async function uncheckoutPeriod(
    request: APIRequestContext,
    headers: Record<string, string>,
    yearPeriod?: string,
) {
    const res = await request.post('/api/settlement/uncheckout', {
        headers,
        params: yearPeriod ? {yearPeriod} : undefined,
        data: yearPeriod ? {yearPeriod} : {},
    })
    const body = await res.json()
    return {
        code: body.code as number,
        message: body.message as string | undefined,
        ok: res.ok(),
    }
}

export async function tryCheckoutCurrentPeriod(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
) {
    const term = await getCurrentTerm(request, headers, bookId)
    const year = term.slice(0, 4)
    const res = await request.get(`/api/settlement/checkout?year=${year}`, {headers})
    const body = await res.json()
    return {
        code: body.code as number,
        message: body.message as string | undefined,
        closedTerm: term,
        nextTerm: await getCurrentTerm(request, headers, bookId),
    }
}

export async function setCurrentTerm(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
) {
    // updateByKey + bookId 过滤（ConfigSysService.update 已按账套限定）
    const res = await request.put('/api/config/sys/updateByKey', {
        headers,
        data: {
            configKey: 'sys.payment.term.current',
            configValue: term,
        },
    })
    const body = await res.json()
    expect(body.code, body.message || 'set current term failed').toBe(0)
}

export async function fetchSettlementRecords(
    request: APIRequestContext,
    headers: Record<string, string>,
    year: string,
) {
    const res = await request.get(
        `/api/settlement/fetch?pageNumber=1&pageSize=12&year=${year}`,
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data?.records || []) as Array<{yearPeriod?: string; status?: number}>
}

export function countClosedSettlements(
    records: Array<{yearPeriod?: string; status?: number}>,
    yearPeriod: string,
) {
    return records.filter((row) => row.yearPeriod === yearPeriod && row.status === 6).length
}

export function addMonthsToTerm(term: string, months: number): string {
    const [year, month] = term.split('-').map(Number)
    const date = new Date(year, month - 1 + months, 1)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}
