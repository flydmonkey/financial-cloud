import {execSync} from 'child_process'
import path from 'path'
import {fileURLToPath} from 'url'
import type {APIRequestContext} from '@playwright/test'
import {expect} from '@playwright/test'

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..')

export function clearBooksViaScript() {
    execSync('python tools/clear_books.py', {
        cwd: repoRoot,
        stdio: 'inherit',
        encoding: 'utf-8',
    })
}

export async function getOnboardingStatus(request: APIRequestContext, headers: Record<string, string>) {
    const res = await request.get('/api/book/onboarding-status', {headers})
    const body = await res.json()
    return body.data as { needsSetup: boolean }
}

export async function listAccessibleBooks(request: APIRequestContext, headers: Record<string, string>) {
    const res = await request.get('/api/book/fetchAll', {headers})
    const body = await res.json()
    return body.data || []
}

/** 通过 API 创建 E2E 账套（无需浏览器 onboarding 向导） */
export async function setupE2eBookViaApi(
    request: APIRequestContext,
    headers: Record<string, string>,
    options?: {name?: string; companyName?: string; voucherReviewed?: number; enableDate?: string},
) {
    const standardsRes = await request.get('/api/standard/fetchAll?status=1', {headers})
    const standardsBody = await standardsRes.json()
    const standard = (standardsBody.data || []).find((s: {id?: string; name?: string}) =>
        String(s.id) === '1' || s.name?.includes('小企业'),
    ) || standardsBody.data?.[0]
    expect(standard?.id, '缺少可用会计准则').toBeTruthy()

    const now = new Date()
    const enableDate = options?.enableDate
        ?? `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`

    const setupRes = await request.post('/api/book/setup', {
        headers,
        data: {
            name: options?.name ?? 'E2E账套',
            companyName: options?.companyName ?? 'E2E测试公司',
            standardId: String(standard.id),
            enableDate,
            vatType: 1,
            voucherReviewed: options?.voucherReviewed ?? 1,
            status: 1,
        },
    })
    const setupBody = await setupRes.json()
    expect(setupBody.code, setupBody.message || 'book setup failed').toBe(0)

    const bookId = setupBody.data?.bookId as string
    const switchRes = await request.get(`/api/users/switchBook/${bookId}`, {headers})
    const switchBody = await switchRes.json()
    expect(switchBody.code, switchBody.message || 'switch book failed').toBe(0)
    return bookId
}
