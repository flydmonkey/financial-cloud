import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {
    createDraftVoucher,
    getVoucherDetail,
    pickStandardBusinessSubjects,
    getNextWordNum,
} from './helpers/voucher'

async function buildMultiLinePayload(
    request: Parameters<typeof buildBalancedVoucherPayload>[0],
    headers: Record<string, string>,
    bookId: string,
    summary: string,
    lines: Array<{
        subject: {id: string; name: string}
        debitAmount: number
        creditAmount: number
    }>,
) {
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
        items: lines.map((line) => ({
            subjectId: line.subject.id,
            subjectName: line.subject.name,
            summary,
            debitAmount: line.debitAmount,
            creditAmount: line.creditAmount,
        })),
    }
}

/**
 * TC-VCH-008 / TC-VCH-009：多行分录凭证
 */
test.describe.serial('voucher multi-line entries', () => {
    const ctx: {headers: Record<string, string>; bookId: string} = {
        headers: {},
        bookId: '',
    }

    test('login and prepare', async ({request}) => {
        const auth = await loginViaApi(request)
        ctx.headers = auth.headers
        const user = await getCurrentUser(request, auth.headers)
        test.skip(!user?.bookId, '无账套')
        ctx.bookId = user.bookId
    })

    test('TC-VCH-008: one debit and multiple credits', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, payable, revenue} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !payable || !revenue, '缺少 1002/2202/5001 科目')

        const payload = await buildMultiLinePayload(
            request, ctx.headers, ctx.bookId, '一借多贷', [
                {subject: bank, debitAmount: 50_000, creditAmount: 0},
                {subject: payable, debitAmount: 0, creditAmount: 30_000},
                {subject: revenue, debitAmount: 0, creditAmount: 20_000},
            ],
        )

        const id = await createDraftVoucher(request, ctx.headers, payload)
        const detail = await getVoucherDetail(request, ctx.headers, id)
        expect(Number(detail.debitAmount)).toBe(50_000)
        expect(Number(detail.creditAmount)).toBe(50_000)
        expect(detail.items).toHaveLength(3)
    })

    test('TC-VCH-009: multiple debits and one credit', async ({request}) => {
        test.skip(!ctx.bookId, '无账套')
        const subjects = await fetchBookSubjects(request, ctx.headers, ctx.bookId)
        const {bank, salesExpense, expense} = pickStandardBusinessSubjects(subjects)
        test.skip(!bank || !salesExpense || !expense, '缺少 1002/5601/5602 科目')

        const payload = await buildMultiLinePayload(
            request, ctx.headers, ctx.bookId, '多借一贷', [
                {subject: expense, debitAmount: 5_000, creditAmount: 0},
                {subject: salesExpense, debitAmount: 5_000, creditAmount: 0},
                {subject: bank, debitAmount: 0, creditAmount: 10_000},
            ],
        )

        const id = await createDraftVoucher(request, ctx.headers, payload)
        const detail = await getVoucherDetail(request, ctx.headers, id)
        expect(Number(detail.debitAmount)).toBe(10_000)
        expect(Number(detail.creditAmount)).toBe(10_000)
        expect(detail.items).toHaveLength(3)
    })
})
