import {expect, type APIRequestContext} from '@playwright/test'
import {
    fetchBookSubjects,
    getCurrentTerm,
    type BookSubjectRef,
} from './auth'

export interface VoucherPayload {
    bookId: string
    wordHead: string
    wordNum: number
    companyName: string
    receiptNum: number
    voucherDate: string
    voucherYear: number
    voucherMonth: number
    items: Array<{
        subjectId: string
        subjectName: string
        summary: string
        debitAmount: number
        creditAmount: number
    }>
}

export async function getNextWordNum(
    request: APIRequestContext,
    headers: Record<string, string>,
    term: string,
    head = '记',
) {
    const res = await request.get(
        `/api/voucher/able-word-num?head=${head}&year=${term.slice(0, 4)}&month=${Number(term.slice(5, 7))}`,
        {headers},
    )
    const body = await res.json()
    return body.data ?? 1
}

export async function buildBalancedVoucherPayload(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
    summary: string,
    amount = 100,
): Promise<VoucherPayload> {
    const subjects = await fetchBookSubjects(request, headers, bookId)
    expect(subjects.length).toBeGreaterThanOrEqual(2)
    const term = await getCurrentTerm(request, headers, bookId)
    const wordNum = await getNextWordNum(request, headers, term)
    const bookRes = await request.get(`/api/book/get/${bookId}`, {headers})
    const book = (await bookRes.json()).data

    return {
        bookId,
        wordHead: '记',
        wordNum,
        companyName: book?.companyName || 'E2E测试公司',
        receiptNum: 0,
        voucherDate: `${term}-15`,
        voucherYear: Number(term.slice(0, 4)),
        voucherMonth: Number(term.slice(5, 7)),
        items: [
            {
                subjectId: subjects[0].id,
                subjectName: subjects[0].name,
                summary,
                debitAmount: amount,
                creditAmount: 0,
            },
            {
                subjectId: subjects[1].id,
                subjectName: subjects[1].name,
                summary,
                debitAmount: 0,
                creditAmount: amount,
            },
        ],
    }
}

export async function ensureVoucherReviewEnabled(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
) {
    const bookRes = await request.get(`/api/book/get/${bookId}`, {headers})
    const book = (await bookRes.json()).data
    if (book?.voucherReviewed === 1) {
        return
    }
    const update = await request.put('/api/book/update', {
        headers,
        data: {...book, voucherReviewed: 1},
    })
    const body = await update.json()
    expect(body.code, body.message || 'enable voucher review failed').toBe(0)
}

export async function createDraftVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    payload: VoucherPayload,
) {
    const res = await request.post('/api/voucher/draft', {headers, data: payload})
    const body = await res.json()
    expect(body.code, body.message || 'draft failed').toBe(0)
    return body.data as string
}

export async function submitVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    payload: VoucherPayload,
    voucherId: string,
) {
    const res = await request.post('/api/voucher/submit', {
        headers,
        data: {...payload, id: voucherId},
    })
    const body = await res.json()
    expect(body.code, body.message || 'submit failed').toBe(0)
}

export async function getVoucherDetail(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.get(`/api/voucher/get/${voucherId}`, {headers})
    const body = await res.json()
    expect(body.code).toBe(0)
    return body.data
}

export async function auditVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.put(`/api/voucher/audit/${voucherId}`, {headers})
    const body = await res.json()
    expect(body.code, body.message || 'audit failed').toBe(0)
}

export async function postVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.put(`/api/voucher/sender/${voucherId}`, {headers})
    const body = await res.json()
    expect(body.code, body.message || 'sender/post failed').toBe(0)
}

export async function unpostVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.put(`/api/voucher/unsender/${voucherId}`, {headers})
    const body = await res.json()
    expect(body.code, body.message || 'unsender/unpost failed').toBe(0)
}

/** 整理凭证号，满足结账连续性检查 */
export async function fixVoucherNumbering(
    request: APIRequestContext,
    headers: Record<string, string>,
) {
    const res = await request.get('/api/voucher/successive', {headers})
    const body = await res.json()
    expect(body.code).toBe(0)
    if (!body.data?.length) {
        return
    }
    const fix = await request.put('/api/voucher/successive', {headers, data: body.data})
    const fixBody = await fix.json()
    expect(fixBody.code, fixBody.message || 'fix successive failed').toBe(0)
}

export async function pickLeafSubjects(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
): Promise<BookSubjectRef[]> {
    return fetchBookSubjects(request, headers, bookId)
}
