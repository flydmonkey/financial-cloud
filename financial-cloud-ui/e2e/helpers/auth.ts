import type {APIRequestContext, Page} from '@playwright/test'
import {expect} from '@playwright/test'

export const username = process.env.E2E_USERNAME || 'admin'
export const password = process.env.E2E_PASSWORD || 'maxkey'

export interface AuthSession {
    token: string
    state: string
    headers: Record<string, string>
}

export interface BookSubjectRef {
    id: string
    name: string
    code?: string
}

export async function loginViaApi(request: APIRequestContext): Promise<AuthSession> {
    const init = await request.get('/api/login/get?_allow_anonymous=true')
    const initBody = await init.json()
    const signin = await request.post('/api/login/signin?_allow_anonymous=true', {
        data: {
            username,
            password,
            captcha: '',
            state: initBody.data.state,
            authType: 'normal',
        },
    })
    const signinBody = await signin.json()
    const token = signinBody.data.token as string
    return {
        token,
        state: initBody.data.state,
        headers: {Authorization: `Bearer ${token}`},
    }
}

export async function getCurrentUser(request: APIRequestContext, headers: Record<string, string>) {
    const res = await request.get('/api/users/currentUser', {headers})
    const body = await res.json()
    return body.data
}

function flattenTree(nodes: any[], output: BookSubjectRef[] = []): BookSubjectRef[] {
    for (const node of nodes || []) {
        if (node.children?.length) {
            flattenTree(node.children, output)
        } else if (node.id) {
            output.push({
                id: String(node.id),
                name: node.name || node.label || String(node.id),
                code: node.code,
            })
        }
    }
    return output
}

export async function fetchBookSubjects(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
): Promise<BookSubjectRef[]> {
    const pageRes = await request.get(
        `/api/booksubject/fetch?bookId=${bookId}&pageNum=1&pageSize=500&status=1`,
        {headers},
    )
    const pageBody = await pageRes.json()
    const records = (pageBody.data?.records || []).filter((item: any) => item.status === 1)
    if (records.length >= 2) {
        return records.map((item: any) => ({
            id: item.id,
            name: item.displayName || item.name || item.code,
            code: item.code,
        }))
    }

    const treeRes = await request.get(`/api/booksubject/tree/${bookId}`, {headers})
    const treeBody = await treeRes.json()
    return flattenTree(treeBody.data || [])
}

export function formatVoucherWord(head: string, term: string, wordNum: number): string {
    const year = term.slice(0, 4)
    const month = String(Number(term.slice(5, 7))).padStart(2, '0')
    return `${head}${year}${month}第${String(wordNum).padStart(4, '0')}号`
}

export async function getCurrentTerm(
    request: APIRequestContext,
    headers: Record<string, string>,
    _bookId: string,
): Promise<string> {
    const res = await request.get('/api/config/sys/books', {headers})
    const body = await res.json()
    const configs = body.data || []
    const current = configs.find((item: any) => item.configKey === 'sys.payment.term.current')
    return current?.configValue || '2025-01'
}

export async function loginViaUi(page: Page) {
    await page.goto('/login')
    await page.locator('input[type="text"]').first().fill(username)
    await page.locator('input[type="password"]').fill(password)
    const routesReady = page.waitForResponse(
        (resp) => resp.url().includes('/api/open/func/list') && resp.ok(),
        {timeout: 30_000},
    )
    await page.locator('.login-btn').click()
    await expect(page).not.toHaveURL(/\/login/, {timeout: 30_000})
    await routesReady
    await expect(page.locator('.sidebar-container').first()).toBeVisible({timeout: 15_000})
}

export async function expectPagesOpen(page: Page, paths: string[]) {
    for (const path of paths) {
        await page.goto(path, {waitUntil: 'networkidle'})
        await expect(page.getByText('404错误')).toHaveCount(0)
        await expect(page.locator('.app-container').first()).toBeVisible({timeout: 15_000})
    }
}
