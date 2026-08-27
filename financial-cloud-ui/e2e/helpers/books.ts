import {execSync} from 'child_process'
import path from 'path'
import {fileURLToPath} from 'url'
import type {APIRequestContext} from '@playwright/test'

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
