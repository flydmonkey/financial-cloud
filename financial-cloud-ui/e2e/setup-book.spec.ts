import {expect, test} from '@playwright/test'
import {clearBooksViaScript, getOnboardingStatus, listAccessibleBooks, setupE2eBookViaApi} from './helpers/books'
import {loginViaApi} from './helpers/auth'

/** 重置并创建 E2E 账套（API，不依赖浏览器） */
test('reset and setup E2E book via API', async ({request}) => {
    clearBooksViaScript()
    const auth = await loginViaApi(request)
    const status = await getOnboardingStatus(request, auth.headers)
    expect(status.needsSetup).toBe(true)

    await setupE2eBookViaApi(request, auth.headers)

    const after = await getOnboardingStatus(request, auth.headers)
    expect(after.needsSetup).toBe(false)
    expect(await listAccessibleBooks(request, auth.headers)).not.toHaveLength(0)
})
