import {expect, type APIRequestContext} from '@playwright/test'
import {getCurrentTerm} from './auth'
import {
    fetchCarryTemplates,
    findCarryTemplate,
    generateAndPostCarryByCode,
} from './settlement-carry'
import {
    getVoucherDetail,
    runVoucherToPosted,
    fixVoucherNumbering,
    tryCancelVoucher,
    tryDeleteVoucher,
    tryPostVoucher,
} from './voucher'

/**
 * 清理会阻断结账硬检的凭证：撤回待审、删除暂存、过账已审未过账。
 * accounting 套件共享账套，前期用例会留下 reviewing/draft。
 */
export async function cleanupBlockingVouchersForSettlement(
    request: APIRequestContext,
    headers: Record<string, string>,
) {
    const term = await getCurrentTerm(request, headers, '')
    const year = term.slice(0, 4)
    const month = Number(term.slice(5, 7))
    const res = await request.get(
        `/api/voucher/fetch?pageNumber=1&pageSize=200&voucherYear=${year}&voucherMonth=${month}`,
        {headers},
    )
    if (!res.ok()) {
        return
    }
    const body = await res.json()
    const records = (body.data?.records || body.data || []) as Array<{
        id: string
        status?: string
        senderId?: string | null
        senderName?: string | null
    }>
    for (const row of records) {
        if (!row?.id) continue
        if (row.status === 'reviewing') {
            await tryCancelVoucher(request, headers, row.id)
            await tryDeleteVoucher(request, headers, row.id)
            continue
        }
        if (row.status === 'draft' || row.status === 'rejected') {
            await tryDeleteVoucher(request, headers, row.id)
            continue
        }
        if (row.status === 'completed' && !row.senderId && !row.senderName) {
            const posted = await tryPostVoucher(request, headers, row.id)
            if (posted.code !== 0) {
                await tryDeleteVoucher(request, headers, row.id)
            }
        }
    }
}

/** Ensure required month-end hard gates can pass (损益结转 posted). */
export async function prepareRequiredCarryForClose(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
) {
    await cleanupBlockingVouchersForSettlement(request, headers)
    const term = await getCurrentTerm(request, headers, bookId)
    const codes = ['qm_jz_sr', 'qm_jz_cbfy']
    if (term.endsWith('-12')) {
        codes.push('qm_jz_bnlr')
    }
    const templates = await fetchCarryTemplates(request, headers)
    for (const code of codes) {
        const template = findCarryTemplate(templates, code)
        if (!template) {
            if (code === 'qm_jz_bnlr') continue
            throw new Error(`缺少必做结转模板 ${code}`)
        }
        if (template.voucherId) {
            const detail = await getVoucherDetail(request, headers, template.voucherId)
            if (!detail.senderId && !detail.senderName) {
                await runVoucherToPosted(request, headers, {
                    bookId: detail.bookId,
                    word: detail.word,
                    wordHead: detail.wordHead,
                    wordNum: detail.wordNum,
                    companyName: detail.companyName,
                    receiptNum: detail.receiptNum ?? 0,
                    voucherDate: detail.voucherDate,
                    voucherYear: detail.voucherYear,
                    voucherMonth: detail.voucherMonth,
                    items: detail.items.map((item: any) => ({
                        subjectId: item.subjectId,
                        subjectName: item.subjectName,
                        summary: item.summary,
                        debitAmount: Number(item.debitAmount ?? 0),
                        creditAmount: Number(item.creditAmount ?? 0),
                    })),
                }, template.voucherId)
            }
            // An earlier posted carry does not include later business vouchers.
            // Generate a supplemental carry from remaining source balances below.
        }
        try {
            await generateAndPostCarryByCode(request, headers, code)
        } catch (e) {
            if (code === 'qm_jz_bnlr') {
                continue
            }
            throw e
        }
    }
}

export async function verifySettlement(
    request: APIRequestContext,
    headers: Record<string, string>,
    options?: {prepareCarry?: boolean; bookId?: string},
) {
    await cleanupBlockingVouchersForSettlement(request, headers)
    if (options?.prepareCarry !== false && options?.bookId) {
        await prepareRequiredCarryForClose(request, headers, options.bookId)
    }
    // 清理/结转会删改凭证，必须在 verify 前整理号段
    await fixVoucherNumbering(request, headers)
    const res = await request.get('/api/settlement/verify', {headers})
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    const checks = body.data || []
    if (body.code !== 0) {
        const failed = checks
            .filter((item: any) => item.hard !== false && item.applicable !== false && !item.result)
            .map((item: any) => `${item.item}:${item.reason || 'fail'}`)
            .join(' | ')
        expect(body.code, `${body.message || 'verify failed'}; ${failed}`).toBe(0)
    }
    expect(checks.length).toBeGreaterThan(0)
    for (const item of checks) {
        if (item.hard === false) continue
        if (item.applicable === false) {
            expect(item.result, `N/A item should pass: ${item.item}`).toBeTruthy()
            continue
        }
        expect(item.result, `结账硬检未通过: ${item.item} ${item.reason || ''}`).toBeTruthy()
    }
    return checks
}

export async function checkoutCurrentPeriod(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
) {
    await prepareRequiredCarryForClose(request, headers, bookId)
    await fixVoucherNumbering(request, headers)
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
