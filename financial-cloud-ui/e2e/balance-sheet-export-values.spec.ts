import {expect, test} from '@playwright/test'
import fs from 'node:fs'
import path from 'node:path'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'
import {fetchBalanceSheet} from './helpers/reports'
import {createAndPostVoucher, pickStandardBusinessSubjects} from './helpers/voucher'

test('balance sheet exports preserve positive and negative actual differences', async ({request}) => {
    const {headers} = await loginViaApi(request)
    const user = await getCurrentUser(request, headers)
    const term = await getCurrentTerm(request, headers, user.bookId)
    const name = `导出金额回归-${Date.now()}`
    const created = await (await request.post('/api/book/save', {headers, data: {
        name, companyName: name, standardId: '1', enableDate: term,
        vatType: 1, voucherReviewed: 1, status: 1,
    }})).json()
    expect(created.code, created.message).toBe(0)
    const books = await (await request.get('/api/book/fetchAll', {headers})).json()
    const bookId = books.data.find((book: any) => book.name === name)?.id
    expect(bookId).toBeTruthy()
    try {
        expect((await (await request.get(`/api/users/switchBook/${bookId}`, {headers})).json()).code).toBe(0)
        const {bank, revenue, expense} = pickStandardBusinessSubjects(await fetchBookSubjects(request, headers, bookId))
        expect(bank && revenue && expense).toBeTruthy()
        const dir = path.resolve('../.e2e-run/export-verification')
        fs.mkdirSync(dir, {recursive: true})
        for (const [name, amount, debit, credit, difference] of [
            ['positive', 1000, bank!, revenue!, 1000],
            ['negative', 2500, expense!, bank!, -1500],
        ] as const) {
            await createAndPostVoucher(request, headers, bookId, `导出核对-${name}`, amount, {debit, credit})
            const sheet = await fetchBalanceSheet(request, headers, term)
            expect(Number(sheet.items.balanceDifference)).toBe(difference)
            fs.writeFileSync(path.join(dir, `${name}.json`), JSON.stringify(sheet, null, 2))
            for (const [suffix, extension] of [['export', 'xlsx'], ['export-pdf', 'pdf']]) {
                const response = await request.get(`/api/statement/balance-sheet/${suffix}`, {
                    headers, params: {periodType: 'month', reportDate: term},
                })
                expect(response.ok()).toBeTruthy()
                const body = await response.body()
                expect(body.subarray(0, extension === 'pdf' ? 4 : 2).toString()).toBe(extension === 'pdf' ? '%PDF' : 'PK')
                fs.writeFileSync(path.join(dir, `${name}.${extension}`), body)
            }
        }
    } finally {
        expect((await (await request.get(`/api/users/switchBook/${user.bookId}`, {headers})).json()).code).toBe(0)
    }
})
