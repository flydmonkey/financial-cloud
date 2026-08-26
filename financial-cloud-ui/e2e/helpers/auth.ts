import type {APIRequestContext} from '@playwright/test'

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
        `/api/booksubject/fetch?bookId=${bookId}&pageNum=1&pageSize=200&status=1`,
        {headers},
    )
    const pageBody = await pageRes.json()
    const records = (pageBody.data?.records || []).filter((item: any) => item.status === 1)
    if (records.length >= 2) {
        return records.map((item: any) => ({
            id: item.id,
            name: item.displayName || item.name || item.code,
        }))
    }

    const treeRes = await request.get(`/api/booksubject/tree/${bookId}`, {headers})
    const treeBody = await treeRes.json()
    return flattenTree(treeBody.data || [])
}

export async function getCurrentTerm(
    request: APIRequestContext,
    headers: Record<string, string>,
    bookId: string,
): Promise<string> {
    const res = await request.get('/api/config/sys/books', {headers})
    const body = await res.json()
    const configs = body.data || []
    const current = configs.find((item: any) => item.configKey === 'sys.payment.term.current')
    return current?.configValue || '2025-01'
}
