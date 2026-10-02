import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {assertReportsBalanced, assertReportsReconciled, fetchBalanceSheet, findBalanceSheetItemByName, num} from './helpers/reports'
import {prepareRequiredCarryForClose} from './helpers/settlement'
import {createAndPostVoucher} from './helpers/voucher'

test('BS-R04: enterprise allowance reduces receivables before and after carry', async ({request}) => {
    const {headers} = await loginViaApi(request)
    const originalUser = await getCurrentUser(request, headers)
    const term = await getCurrentTerm(request, headers, originalUser.bookId)
    const name = `坏账准备回归-${Date.now()}`
    const created = await (await request.post('/api/book/save', {
        headers,
        data: {name, companyName: '坏账准备回归公司', standardId: '2',
            enableDate: term, vatType: 1, voucherReviewed: 1, status: 1},
    })).json()
    expect(created.code, created.message).toBe(0)
    const books = await (await request.get('/api/book/fetchAll', {headers})).json()
    const createdBook = books.data.find((book: any) => book.name === name)
    expect(createdBook).toBeTruthy()
    const bookId = String(createdBook.id)
    try {
        const switched = await (await request.get(`/api/users/switchBook/${bookId}`, {headers})).json()
        expect(switched.code, switched.message).toBe(0)
        const rules = await (await request.get('/api/statement/config/rules?itemCode=1105', {headers})).json()
        expect(rules.code).toBe(0)
        expect(rules.data.length).toBeGreaterThan(0)
        expect(rules.data.every((rule: any) => String(rule.bookId) === bookId)).toBe(true)
        const subjects = await fetchBookSubjects(request, headers, bookId)
        const subject = (code: string) => {
            const result = subjects.find((row) => row.code === code)
            expect(result, `企业会计制度缺少 ${code}`).toBeTruthy()
            return result!
        }
        await createAndPostVoucher(request, headers, bookId, '坏账回归-赊销', 20_000,
            {debit: subject('1131'), credit: subject('5101')})
        await createAndPostVoucher(request, headers, bookId, '坏账回归-计提准备', 3_000,
            {debit: subject('5502'), credit: subject('1141')})
        const assertNetReceivables = async () => {
            const sheet = await fetchBalanceSheet(request, headers, term)
            const row = findBalanceSheetItemByName(sheet.items.assets, '应收账款')
            expect(row).toBeTruthy()
            expect(num(row!.currentBalance)).toBe(17_000)
        }
        await assertNetReceivables()
        await assertReportsReconciled(request, headers, term)
        await prepareRequiredCarryForClose(request, headers, bookId)
        await assertNetReceivables()
        await assertReportsBalanced(request, headers, term)
    } finally {
        const restored = await (await request.get(`/api/users/switchBook/${originalUser.bookId}`, {headers})).json()
        expect(restored.code, restored.message).toBe(0)
        const rules = await (await request.get('/api/statement/config/rules?itemCode=1105', {headers})).json()
        expect(rules.data.length).toBeGreaterThan(0)
        expect(rules.data.every((rule: any) => String(rule.bookId) === String(originalUser.bookId))).toBe(true)
    }
})
