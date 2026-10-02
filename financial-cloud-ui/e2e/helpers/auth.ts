import type {APIRequestContext, Page} from '@playwright/test'
import {expect} from '@playwright/test'

export const username = process.env.E2E_USERNAME || 'admin'
export const password = process.env.E2E_PASSWORD || 'changeme'

/** 制单/审核职责分离：e2e 专用审核员（与 admin 不同 userId） */
export const reviewerUsername = process.env.E2E_REVIEWER_USERNAME || 'e2e_reviewer'
export const reviewerPassword = process.env.E2E_REVIEWER_PASSWORD || 'Review@2026'

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

const reviewerSessionByBook = new Map<string, AuthSession>()

export async function loginViaApiAs(
    request: APIRequestContext,
    loginUsername: string,
    loginPassword: string,
): Promise<AuthSession> {
    const init = await request.get('/api/login/get?_allow_anonymous=true')
    if (!init.ok()) {
        throw new Error(`login/get failed: HTTP ${init.status()}`)
    }
    const initBody = await init.json()
    if (initBody.code !== 0 && initBody.code !== undefined) {
        throw new Error(`login/get business error: ${initBody.message || JSON.stringify(initBody)}`)
    }
    const signin = await request.post('/api/login/signin?_allow_anonymous=true', {
        data: {
            username: loginUsername,
            password: loginPassword,
            captcha: '',
            state: initBody.data.state,
            authType: 'normal',
        },
    })
    if (!signin.ok()) {
        throw new Error(`login/signin failed: HTTP ${signin.status()}`)
    }
    const signinBody = await signin.json()
    if (signinBody.code !== 0 && signinBody.code !== undefined) {
        throw new Error(`login/signin business error: ${signinBody.message || JSON.stringify(signinBody)}`)
    }
    const token = signinBody.data?.token as string
    if (!token) {
        throw new Error(`login/signin missing token: ${JSON.stringify(signinBody)}`)
    }
    return {
        token,
        state: initBody.data.state,
        headers: {Authorization: `Bearer ${token}`},
    }
}

export async function loginViaApi(request: APIRequestContext): Promise<AuthSession> {
    return loginViaApiAs(request, username, password)
}

/**
 * 确保存在独立审核员并切入目标账套。
 * 会计规范：制单人与审核人不得为同一人；admin 制单后必须由此账号审核。
 */
export async function ensureReviewerSession(
    request: APIRequestContext,
    adminHeaders: Record<string, string>,
    bookId: string,
): Promise<AuthSession> {
    const cached = reviewerSessionByBook.get(bookId)
    if (cached) {
        return cached
    }

    let userId: string | undefined
    const existing = await request.get(`/api/book/members/search?bookId=${bookId}&q=${encodeURIComponent(reviewerUsername)}`, {
        headers: adminHeaders,
    })
    if (existing.ok()) {
        try {
            const body = await existing.json()
            const reviewer = body?.data?.find((item: any) => item.username === reviewerUsername)
            if (body?.code === 0 && reviewer?.userId) {
                userId = String(reviewer.userId)
            }
        } catch {
            // getByUsername 在用户不存在时可能返回非 JSON / 500，忽略并创建
        }
    }

    if (!userId) {
        const create = await request.post('/api/users/add', {
            headers: adminHeaders,
            data: {
                username: reviewerUsername,
                password: reviewerPassword,
                displayName: 'E2E审核员',
                userType: 'EMPLOYEE',
                userState: 'RESIDENT',
                status: 1,
                sortIndex: 99,
            },
        })
        const createBody = await create.json()
        expect(createBody.code, createBody.message || 'create reviewer failed').toBe(0)

        const created = await request.get(`/api/users/getByUsername/${reviewerUsername}`, {
            headers: adminHeaders,
        })
        const createdBody = await created.json()
        expect(createdBody.code, createdBody.message || 'fetch reviewer failed').toBe(0)
        userId = String(createdBody.data.id)
    }

    // 授予目标账套审核角色，同时建立账套访问资格。
    const role = await request.post('/api/book/members/grant', {
        headers: adminHeaders,
        data: {
            bookId,
            roleId: 'ROLE_REVIEWER',
            userId,
        },
    })
    const roleBody = await role.json()
    // 已是成员时也可能成功；仅在明确失败时抛错
    if (roleBody.code !== 0 && roleBody.code !== undefined) {
        // 忽略重复加入；若完全失败仍尝试登录（admin seed 场景）
        const msg = String(roleBody.message || '')
        if (!/已|存在|duplicate|Duplicate/i.test(msg)) {
            expect(roleBody.code, roleBody.message || 'add reviewer role failed').toBe(0)
        }
    }

    // switchBook 现要求 permission_book 显式授权；仅加全局角色不够
    const grant = await request.post('/api/book/members/grant', {
        headers: adminHeaders,
        data: {
            bookId,
            userId,
            roleId: 'ROLE_ADMINISTRATORS',
        },
    })
    const grantBody = await grant.json()
    expect(grantBody.code, grantBody.message || 'grant reviewer book access failed').toBe(0)

    const session = await loginViaApiAs(request, reviewerUsername, reviewerPassword)
    const switched = await request.get(`/api/users/switchBook/${bookId}`, {headers: session.headers})
    const switchBody = await switched.json()
    expect(switchBody.code, switchBody.message || 'reviewer switchBook failed').toBe(0)

    reviewerSessionByBook.set(bookId, session)
    return session
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

export function formatVoucherWord(head: string, _term: string, wordNum: number): string {
    return formatVoucherWordListLabel(head, wordNum)
}

/** 凭证字号统一对外格式：记-9 */
export function formatVoucherWordListLabel(head: string, wordNum: number | string): string {
    return `${head}-${wordNum}`
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
