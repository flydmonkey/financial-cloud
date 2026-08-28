import {expect, type APIRequestContext} from '@playwright/test'
import {
    fetchBookSubjects,
    getCurrentTerm,
    type BookSubjectRef,
} from './auth'

export interface VoucherPayload {
    bookId: string
    word?: string
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
    subjects?: {debit: BookSubjectRef; credit: BookSubjectRef},
    termOverride?: string,
): Promise<VoucherPayload> {
    const allSubjects = subjects
        ? [subjects.debit, subjects.credit]
        : await fetchBookSubjects(request, headers, bookId)
    expect(allSubjects.length).toBeGreaterThanOrEqual(2)
    const debitSubject = subjects?.debit ?? allSubjects[0]
    const creditSubject = subjects?.credit ?? allSubjects[1]
    const term = termOverride ?? await getCurrentTerm(request, headers, bookId)
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
                subjectId: debitSubject.id,
                subjectName: debitSubject.name,
                summary,
                debitAmount: amount,
                creditAmount: 0,
            },
            {
                subjectId: creditSubject.id,
                subjectName: creditSubject.name,
                summary,
                debitAmount: 0,
                creditAmount: amount,
            },
        ],
    }
}

/** 优先选银行+收入科目，便于报表勾稽测试 */
export function pickReconciliationSubjects(subjects: BookSubjectRef[]): {
    debit: BookSubjectRef
    credit: BookSubjectRef
} {
    const bank = subjects.find((s) => /^100[12]/.test(s.code || ''))
    const revenue = subjects.find((s) =>
        /^(6001|6051|5001|5051)/.test(s.code || ''),
    )
    const expense = subjects.find((s) => /^(660[12]|560[123])/.test(s.code || ''))
    if (bank && revenue) {
        return {debit: bank, credit: revenue}
    }
    if (bank && expense) {
        return {debit: expense, credit: bank}
    }
    expect(subjects.length).toBeGreaterThanOrEqual(2)
    return {debit: subjects[0], credit: subjects[1]}
}

/** 小企业准则常用科目：1002 银行、5001 收入、5601/5602 费用、1122 应收、1123 预付、2202 应付 */
export function pickStandardBusinessSubjects(subjects: BookSubjectRef[]) {
    const bank = subjects.find((s) => s.code === '1002')
    const revenue = subjects.find((s) => s.code === '5001')
    const expense = subjects.find((s) => s.code === '5602')
    const salesExpense = subjects.find((s) => s.code === '5601')
    const receivable = subjects.find((s) => s.code === '1122')
    const prepaid = subjects.find((s) => s.code === '1123')
    const payable = subjects.find((s) => s.code === '2202')
    const rawMaterial = subjects.find((s) => s.code === '1403')
    const finishedGoods = subjects.find((s) => s.code === '1405')
    const fixedAsset = subjects.find((s) => s.code === '1601')
    const accumulatedDepreciation = subjects.find((s) => s.code === '1602')
    const badDebtAllowance = subjects.find((s) => s.code === '1141')
    const badDebtExpense = subjects.find((s) => s.code === '5711.03')
        ?? subjects.find((s) => s.code === '5711')
    const cost = subjects.find((s) => s.code === '5401')
    const investmentIncome = subjects.find((s) => s.code === '5111')
    const nonOpIncome = subjects.find((s) => s.code === '5301')
    const nonOpExpense = subjects.find((s) => s.code === '5711')
    const incomeTax = subjects.find((s) => s.code === '5801')
    return {
        bank, revenue, expense, salesExpense, receivable, prepaid, payable,
        rawMaterial, finishedGoods, fixedAsset, accumulatedDepreciation,
        badDebtAllowance, badDebtExpense, cost, investmentIncome, nonOpIncome,
        nonOpExpense, incomeTax,
    }
}

export function findSubjectByCode(subjects: BookSubjectRef[], code: string) {
    return subjects.find((s) => s.code === code)
}

/** 选余额未接近 decimal(10,2) 上限的科目，避免 voucher_item.subject_balance 快照溢出 */
export async function pickSafeBalanceSubjects(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
    term: string,
    maxAbsBalance = 1_000_000,
): Promise<{debit: BookSubjectRef; credit: BookSubjectRef}> {
    const {fetchSubjectBalances, getSubjectBalance} = await import('./reports')
    const subjects = await fetchBookSubjects(request, headers, bookId)
    const balances = await fetchSubjectBalances(request, headers, term)
    const std = pickStandardBusinessSubjects(subjects)
    const isSafe = (s?: BookSubjectRef) =>
        !!s
        && balances.some((row) => row.subjectCode === s.code)
        && Math.abs(getSubjectBalance(balances, s.code!)) <= maxAbsBalance

    if (isSafe(std.prepaid) && isSafe(std.expense)) {
        return {debit: std.prepaid!, credit: std.expense!}
    }
    if (isSafe(std.expense) && isSafe(std.revenue)) {
        return {debit: std.expense!, credit: std.revenue!}
    }

    const safe = subjects.filter(
        (s) =>
            balances.some((row) => row.subjectCode === s.code)
            && Math.abs(getSubjectBalance(balances, s.code)) <= maxAbsBalance,
    )
    expect(safe.length, '缺少余额安全的科目，请重置测试账套').toBeGreaterThanOrEqual(2)
    return {debit: safe[0], credit: safe[1]}
}

export async function ensureVoucherReviewDisabled(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
) {
    const bookRes = await request.get(`/api/book/get/${bookId}`, {headers})
    const book = (await bookRes.json()).data
    if (book?.voucherReviewed === 0) {
        return
    }
    const update = await request.put('/api/book/update', {
        headers,
        data: {...book, voucherReviewed: 0},
    })
    const body = await update.json()
    expect(body.code, body.message || 'disable voucher review failed').toBe(0)
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
    const body = await tryCreateDraftVoucher(request, headers, payload)
    expect(body.code, body.message || 'draft failed').toBe(0)
    return body.data as string
}

export async function tryCreateDraftVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    payload: VoucherPayload,
) {
    const res = await request.post('/api/voucher/draft', {headers, data: payload})
    return res.json() as Promise<{code: number; message?: string; data?: string}>
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

export async function trySubmitVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    payload: VoucherPayload,
    voucherId: string,
) {
    const res = await request.post('/api/voucher/submit', {
        headers,
        data: {...payload, id: voucherId},
    })
    return res.json() as Promise<{code: number; message?: string}>
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

export async function unauditVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.put(`/api/voucher/unaudit/${voucherId}`, {headers})
    const body = await res.json()
    expect(body.code, body.message || 'unaudit failed').toBe(0)
}

export type VoucherPayloadWithId = VoucherPayload & {id: string; status?: string}

export function voucherDetailToPayload(detail: any): VoucherPayloadWithId {
    return {
        id: detail.id,
        bookId: detail.bookId,
        word: detail.word,
        wordHead: detail.wordHead,
        wordNum: detail.wordNum,
        companyName: detail.companyName,
        receiptNum: detail.receiptNum ?? 0,
        voucherDate: detail.voucherDate,
        voucherYear: detail.voucherYear,
        voucherMonth: detail.voucherMonth,
        status: detail.status,
        items: (detail.items || []).map((item: any) => ({
            subjectId: item.subjectId,
            subjectName: item.subjectName,
            summary: item.summary,
            debitAmount: Number(item.debitAmount ?? 0),
            creditAmount: Number(item.creditAmount ?? 0),
        })),
    }
}

export async function updateVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
    mutator: (payload: VoucherPayloadWithId) => void,
) {
    const detail = await getVoucherDetail(request, headers, voucherId)
    const payload = voucherDetailToPayload(detail)
    mutator(payload)
    const res = await request.put('/api/voucher/update', {headers, data: payload})
    const body = await res.json()
    expect(body.code, body.message || 'update failed').toBe(0)
}

/** 尝试过账，不断言成功（用于重复过账测试） */
export async function tryPostVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.put(`/api/voucher/sender/${voucherId}`, {headers})
    return res.json()
}

/** 批量过账，返回原始响应（合法/非法 ID 混合场景） */
export async function tryBatchPostVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherIds: string[],
) {
    const res = await request.put(`/api/voucher/sender/${voucherIds.join(',')}`, {headers})
    return res.json() as Promise<{code: number; message?: string}>
}

/** 批量审核 reviewing 凭证 */
export async function tryBatchAuditVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherIds: string[],
) {
    const res = await request.put(`/api/voucher/audit/${voucherIds.join(',')}`, {headers})
    return res.json() as Promise<{code: number; message?: string}>
}

/** 批量提交 draft 凭证 */
export async function tryBatchSubmitVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherIds: string[],
) {
    const res = await request.post(`/api/voucher/submit/${voucherIds.join(',')}`, {headers})
    return res.json() as Promise<{code: number; message?: string}>
}

export async function tryAuditVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.put(`/api/voucher/audit/${voucherId}`, {headers})
    return res.json()
}

/** 主管复核 completed 凭证 */
export async function tryManageAuditVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.put(`/api/voucher/manage-audit/${voucherId}`, {headers})
    return res.json() as Promise<{code: number; message?: string}>
}

export async function tryUnauditVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.put(`/api/voucher/unaudit/${voucherId}`, {headers})
    return res.json()
}

export async function tryDeleteVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.delete(`/api/voucher/delete/${voucherId}`, {headers})
    return res.json()
}

export async function tryUpdateVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
    mutator: (payload: VoucherPayloadWithId) => void,
) {
    const detail = await getVoucherDetail(request, headers, voucherId)
    const payload = voucherDetailToPayload(detail)
    mutator(payload)
    const res = await request.put('/api/voucher/update', {headers, data: payload})
    return res.json()
}

export async function tryCancelVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    voucherId: string,
) {
    const res = await request.put(`/api/voucher/cancel/${voucherId}`, {headers})
    return res.json()
}

/** 完整正向流程：暂存 → 提交 → 审核 → 过账 */
export async function runVoucherToPosted(
    request: APIRequestContext,
    headers: Record<string, string>,
    payload: VoucherPayload,
    voucherId: string,
    options?: {skipAudit?: boolean},
) {
    await submitVoucher(request, headers, payload, voucherId)
    const afterSubmit = await getVoucherDetail(request, headers, voucherId)
    if (afterSubmit.status === 'reviewing') {
        await auditVoucher(request, headers, voucherId)
    } else if (options?.skipAudit) {
        expect(afterSubmit.status).toBe('completed')
    }
    await postVoucher(request, headers, voucherId)
}

/** 创建凭证并完成 暂存→提交→审核→过账 */
export async function createAndPostVoucher(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
    summary: string,
    amount: number,
    subjects?: {debit: BookSubjectRef; credit: BookSubjectRef},
    termOverride?: string,
) {
    const payload = await buildBalancedVoucherPayload(
        request,
        headers,
        bookId,
        summary,
        amount,
        subjects,
        termOverride,
    )
    const voucherId = await createDraftVoucher(request, headers, payload)
    await runVoucherToPosted(request, headers, payload, voucherId)
    return {payload, voucherId}
}

/** 整理凭证号，满足结账连续性检查 */
export async function fetchSuccessiveGaps(
    request: APIRequestContext,
    headers: Record<string, string>,
) {
    const res = await request.get('/api/voucher/successive', {headers})
    const body = await res.json()
    expect(body.code).toBe(0)
    return (body.data || []) as Array<{sourceWord?: string; targetWord?: string; wordNum?: number}>
}

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
