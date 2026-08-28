import {expect, type APIRequestContext} from '@playwright/test'
import {getVoucherDetail, runVoucherToPosted} from './voucher'

export interface CarryTemplate {
    id: string
    code: string
    name: string
    voucherId?: string | null
}

export async function fetchCarryTemplates(
    request: APIRequestContext,
    headers: Record<string, string>,
): Promise<CarryTemplate[]> {
    const res = await request.get(
        '/api/settlementcarry/fetchcarry?pageNumber=1&pageSize=50',
        {headers},
    )
    expect(res.ok()).toBeTruthy()
    const body = await res.json()
    expect(body.code).toBe(0)
    return body.data?.records || []
}

export async function generateCarryVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    template: CarryTemplate,
) {
    const res = await request.post('/api/settlementcarry/generate-voucher', {
        headers,
        data: {id: template.id, templateId: template.id, voucherType: 1},
    })
    return res.json()
}

export async function deleteCarryVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.delete(`/api/settlementcarry/delete/${voucherId}`, {headers})
    const text = await res.text()
    if (!text) {
        return {code: res.ok() ? 0 : res.status()}
    }
    return JSON.parse(text)
}

export function findCarryTemplate(templates: CarryTemplate[], code: string) {
    return templates.find((item) => item.code === code)
}

/** 清理当期已生成的结转草稿，避免重复生成失败 */
export async function cleanupExistingCarryVouchers(
    request: APIRequestContext,
    headers: Record<string, string>,
) {
    const templates = await fetchCarryTemplates(request, headers)
    for (const template of templates) {
        if (template.voucherId) {
            await deleteCarryVoucher(request, headers, template.voucherId)
        }
    }
}

/** 生成结转凭证并过账（qm_jz_sr / qm_jz_cbfy 等） */
export async function generateAndPostCarryByCode(
    request: APIRequestContext,
    headers: Record<string, string>,
    templateCode: string,
) {
    const templates = await fetchCarryTemplates(request, headers)
    const template = findCarryTemplate(templates, templateCode)
    expect(template, `缺少结转模板 ${templateCode}`).toBeTruthy()
    const gen = await generateCarryVoucher(request, headers, template!)
    expect(gen.code, gen.message || `${templateCode} generate failed`).toBe(0)
    expect(gen.data).toBeTruthy()

    const detail = await getVoucherDetail(request, headers, gen.data)
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
        items: detail.items.map((item: {
            subjectId: string
            subjectName: string
            summary?: string
            debitAmount?: number | string
            creditAmount?: number | string
        }) => ({
            subjectId: item.subjectId,
            subjectName: item.subjectName,
            summary: item.summary,
            debitAmount: Number(item.debitAmount ?? 0),
            creditAmount: Number(item.creditAmount ?? 0),
        })),
    }, gen.data)
    return gen.data as string
}
