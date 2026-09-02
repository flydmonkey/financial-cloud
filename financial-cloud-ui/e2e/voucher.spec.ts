import {expect, test} from '@playwright/test'
import {fetchBookSubjects, getCurrentTerm, getCurrentUser, loginViaApi} from './helpers/auth'

test.describe('voucher module', () => {
    test('validation rejects unbalanced draft via API', async ({request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)
        const subjects = await fetchBookSubjects(request, auth.headers, user.bookId)
        test.skip(subjects.length < 2, '账套科目不足，跳过凭证 API 测试')

        const term = await getCurrentTerm(request, auth.headers, user.bookId)
        const debitSubject = subjects[0]
        const creditSubject = subjects[1]
        const wordNumRes = await request.get(
            `/api/voucher/able-word-num?head=记&year=${term.slice(0, 4)}&month=${Number(term.slice(5, 7))}`,
            {headers: auth.headers},
        )
        const wordNum = (await wordNumRes.json()).data ?? 1

        const draft = await request.post('/api/voucher/draft', {
            headers: auth.headers,
            data: {
                bookId: user.bookId,
                wordHead: '记',
                wordNum,
                companyName: user.companyName || '测试公司',
                receiptNum: 0,
                voucherDate: `${term}-15`,
                voucherYear: Number(term.slice(0, 4)),
                voucherMonth: Number(term.slice(5, 7)),
                items: [
                    {
                        subjectId: debitSubject.id,
                        subjectName: debitSubject.name,
                        summary: 'E2E测试',
                        debitAmount: 100,
                        creditAmount: 0,
                    },
                    {
                        subjectId: creditSubject.id,
                        subjectName: creditSubject.name,
                        summary: 'E2E测试',
                        debitAmount: 0,
                        creditAmount: 90,
                    },
                ],
            },
        })
        const draftBody = await draft.json()
        expect(draftBody.code).not.toBe(0)
        expect(draftBody.message).toContain('借贷不平衡')
    })

    test('draft and submit balanced voucher via API', async ({request}) => {
        const auth = await loginViaApi(request)
        const user = await getCurrentUser(request, auth.headers)
        const subjects = await fetchBookSubjects(request, auth.headers, user.bookId)
        test.skip(subjects.length < 2, '账套科目不足，跳过凭证 API 测试')

        const term = await getCurrentTerm(request, auth.headers, user.bookId)
        const debitSubject = subjects[0]
        const creditSubject = subjects[1]
        const wordNumRes = await request.get(
            `/api/voucher/able-word-num?head=记&year=${term.slice(0, 4)}&month=${Number(term.slice(5, 7))}`,
            {headers: auth.headers},
        )
        const wordNum = (await wordNumRes.json()).data ?? 1

        const payload = {
            bookId: user.bookId,
            wordHead: '记',
            wordNum,
            companyName: user.companyName || '测试公司',
            receiptNum: 0,
            voucherDate: `${term}-15`,
            voucherYear: Number(term.slice(0, 4)),
            voucherMonth: Number(term.slice(5, 7)),
            items: [
                {
                    subjectId: debitSubject.id,
                    subjectName: debitSubject.name,
                    summary: 'E2E测试凭证',
                    debitAmount: 1,
                    creditAmount: 0,
                },
                {
                    subjectId: creditSubject.id,
                    subjectName: creditSubject.name,
                    summary: 'E2E测试凭证',
                    debitAmount: 0,
                    creditAmount: 1,
                },
            ],
        }

        const draft = await request.post('/api/voucher/draft', {headers: auth.headers, data: payload})
        const draftBody = await draft.json()
        expect(draftBody.code, draftBody.message || 'draft failed').toBe(0)
        const voucherId = draftBody.data

        const submit = await request.post('/api/voucher/submit', {
            headers: auth.headers,
            data: {...payload, id: voucherId},
        })
        const submitBody = await submit.json()
        expect(submitBody.code).toBe(0)

        const detail = await request.get(`/api/voucher/get/${voucherId}`, {headers: auth.headers})
        const detailBody = await detail.json()
        expect(detailBody.code).toBe(0)
        expect(['completed', 'reviewing']).toContain(detailBody.data.status)
    })

    const uiTest = process.env.E2E_ENABLE_UI === '1' ? test : test.skip
    uiTest('voucher list and edit pages render', async ({page}) => {
        await page.goto('/login')
        await page.locator('input[type="text"]').first().fill(process.env.E2E_USERNAME || 'admin')
        await page.locator('input[type="password"]').fill(process.env.E2E_PASSWORD || 'changeme')
        await page.locator('.login-btn').click()
        await expect(page).not.toHaveURL(/\/login/, {timeout: 30_000})

        await page.goto('/voucher/voucher-index')
        await expect(page.locator('.app-container').first()).toBeVisible({timeout: 15_000})
        await expect(page.getByRole('button', {name: /新增|add/i}).first()).toBeVisible()

        await page.goto('/voucher/voucher-edit')
        await expect(page.getByRole('button', {name: '暂存'})).toBeVisible({timeout: 15_000})
        // 编辑页以「保存」提交（开启审核时等同提交审核流）
        await expect(page.getByRole('button', {name: '保存'})).toBeVisible()
    })
})
