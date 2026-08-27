import {expect, test} from '@playwright/test'
import {
    fetchBookSubjects,
    formatVoucherWord,
    getCurrentTerm,
    getCurrentUser,
    loginViaApi,
    loginViaUi,
} from './helpers/auth'

async function createDraftVoucher(request: any, headers: Record<string, string>, bookId: string) {
    const subjects = await fetchBookSubjects(request, headers, bookId)
    if (subjects.length < 2) {
        return null
    }
    const term = await getCurrentTerm(request, headers, bookId)
    const wordNumRes = await request.get(
        `/api/voucher/able-word-num?head=记&year=${term.slice(0, 4)}&month=${Number(term.slice(5, 7))}`,
        {headers},
    )
    const wordNum = (await wordNumRes.json()).data ?? 1
    const payload = {
        bookId,
        wordHead: '记',
        wordNum,
        companyName: 'E2E测试公司',
        receiptNum: 0,
        voucherDate: `${term}-15`,
        voucherYear: Number(term.slice(0, 4)),
        voucherMonth: Number(term.slice(5, 7)),
        items: [
            {
                subjectId: subjects[0].id,
                subjectName: subjects[0].name,
                summary: 'E2E取消确认框',
                debitAmount: 1,
                creditAmount: 0,
            },
            {
                subjectId: subjects[1].id,
                subjectName: subjects[1].name,
                summary: 'E2E取消确认框',
                debitAmount: 0,
                creditAmount: 1,
            },
        ],
    }
    const draft = await request.post('/api/voucher/draft', {headers, data: payload})
    const draftBody = await draft.json()
    if (draftBody.code !== 0) {
        return null
    }
    return {
        id: draftBody.data as string,
        wordLabel: formatVoucherWord('记', term, wordNum),
    }
}

test.describe('confirm dialog cancel', () => {
    test('voucher delete confirm cancel does not toast operation failed', async ({page, request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)
        const voucher = await createDraftVoucher(request, auth.headers, user.bookId)
        test.skip(!voucher, '无法创建暂存凭证，跳过确认框测试')

        await loginViaUi(page)
        await page.goto('/voucher/voucher-index')
        await expect(page.locator('.app-container').first()).toBeVisible({timeout: 15_000})

        const targetRow = page.locator('.el-table__body tr').filter({hasText: voucher!.wordLabel}).first()
        await expect(targetRow).toBeVisible({timeout: 15_000})
        await targetRow.locator('.el-checkbox').click()

        await page.getByRole('button', {name: '批量删除'}).click()
        const dialog = page.locator('.el-message-box')
        await expect(dialog).toBeVisible({timeout: 5_000})
        await dialog.getByRole('button', {name: '取消'}).click()
        await expect(dialog).toBeHidden()

        await expect(page.locator('.el-message--error')).toHaveCount(0)
        await expect(page.getByText('操作失败')).toHaveCount(0)
    })

    test('voucher audit confirm cancel does not toast operation failed', async ({page, request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)
        const subjects = await fetchBookSubjects(request, auth.headers, user.bookId)
        test.skip(subjects.length < 2, '科目不足，跳过审核确认框测试')

        const term = await getCurrentTerm(request, auth.headers, user.bookId)
        const wordNumRes = await request.get(
            `/api/voucher/able-word-num?head=记&year=${term.slice(0, 4)}&month=${Number(term.slice(5, 7))}`,
            {headers: auth.headers},
        )
        const wordNum = (await wordNumRes.json()).data ?? 1
        const wordLabel = formatVoucherWord('记', term, wordNum)
        const bookRes = await request.get(`/api/book/get/${user.bookId}`, {headers: auth.headers})
        const book = (await bookRes.json()).data

        const payload = {
            bookId: user.bookId,
            wordHead: '记',
            wordNum,
            companyName: user.companyName || 'E2E测试公司',
            receiptNum: 0,
            voucherDate: `${term}-15`,
            voucherYear: Number(term.slice(0, 4)),
            voucherMonth: Number(term.slice(5, 7)),
            items: [
                {
                    subjectId: subjects[0].id,
                    subjectName: subjects[0].name,
                    summary: 'E2E审核取消',
                    debitAmount: 1,
                    creditAmount: 0,
                },
                {
                    subjectId: subjects[1].id,
                    subjectName: subjects[1].name,
                    summary: 'E2E审核取消',
                    debitAmount: 0,
                    creditAmount: 1,
                },
            ],
        }

        if (book?.voucherReviewed !== 1) {
            await request.put('/api/book/update', {
                headers: auth.headers,
                data: {
                    ...book,
                    voucherReviewed: 1,
                },
            })
        }

        const draft = await request.post('/api/voucher/draft', {headers: auth.headers, data: payload})
        const draftBody = await draft.json()
        expect(draftBody.code, draftBody.message || 'draft failed').toBe(0)
        const submit = await request.post('/api/voucher/submit', {
            headers: auth.headers,
            data: {...payload, id: draftBody.data},
        })
        const submitBody = await submit.json()
        expect(submitBody.code, submitBody.message || 'submit failed').toBe(0)

        await loginViaUi(page)
        await page.goto('/voucher/voucher-index')
        await expect(page.locator('.app-container').first()).toBeVisible({timeout: 15_000})

        const targetRow = page.locator('.el-table__body tr').filter({hasText: wordLabel}).first()
        await expect(targetRow).toBeVisible({timeout: 15_000})
        await targetRow.locator('.el-checkbox').click()

        await page.getByRole('button', {name: '审核'}).click()
        const dialog = page.locator('.el-message-box')
        await expect(dialog).toBeVisible({timeout: 5_000})
        await dialog.getByRole('button', {name: '取消'}).click()
        await expect(dialog).toBeHidden()

        await expect(page.locator('.el-message--error')).toHaveCount(0)
        await expect(page.getByText('操作失败')).toHaveCount(0)
    })
})
