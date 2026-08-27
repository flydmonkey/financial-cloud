import {defineConfig, devices} from '@playwright/test'
import path from 'path'
import {fileURLToPath} from 'url'

const rootDir = path.dirname(fileURLToPath(import.meta.url))
// Local installs pin browsers under the repo; CI uses the default Playwright cache.
if (!process.env.CI && !process.env.PLAYWRIGHT_BROWSERS_PATH) {
    process.env.PLAYWRIGHT_BROWSERS_PATH = path.join(rootDir, '.playwright-browsers')
}
if (process.env.npm_lifecycle_event === 'test:e2e:ui-pages') {
    process.env.E2E_ENABLE_UI = '1'
}

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:3154'

export default defineConfig({
    testDir: './e2e',
    globalSetup: path.join(rootDir, 'e2e/global-setup.ts'),
    fullyParallel: false,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    workers: 1,
    reporter: [['list']],
    use: {
        baseURL,
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
    },
    projects: [
        {
            name: 'chromium',
            use: {...devices['Desktop Chrome']},
        },
    ],
})
