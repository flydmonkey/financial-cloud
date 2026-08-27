import {request, type FullConfig} from '@playwright/test'
import {loginViaApi} from './helpers/auth'
import {clearBooksViaScript, getOnboardingStatus, setupE2eBookViaApi} from './helpers/books'

/**
 * 设置 E2E_RESET_BOOK=1 时在全部 spec 运行前清库并 API 建账。
 * 例：E2E_RESET_BOOK=1 npx playwright test e2e/
 */
export default async function globalSetup(config: FullConfig) {
    if (process.env.E2E_RESET_BOOK !== '1') {
        return
    }

    const baseURL = config.projects[0]?.use?.baseURL as string | undefined
    if (!baseURL) {
        throw new Error('globalSetup: missing baseURL in playwright config')
    }

    clearBooksViaScript()

    const api = await request.newContext({baseURL})
    try {
        const auth = await loginViaApi(api)
        const status = await getOnboardingStatus(api, auth.headers)
        if (!status.needsSetup) {
            console.warn('[globalSetup] expected needsSetup after clear_books, continuing setup')
        }
        await setupE2eBookViaApi(api, auth.headers)
        console.log('[globalSetup] E2E book ready')
    } finally {
        await api.dispose()
    }
}
