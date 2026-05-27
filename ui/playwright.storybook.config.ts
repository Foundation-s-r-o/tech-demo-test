import type { PlaywrightTestConfig } from '@playwright/test'
import { devices } from '@playwright/test'
import 'dotenv/config'

/**
 * Storybook smoke config. Auto-starts Storybook on a dedicated port (6007, not
 * the interactive dev port 6006) so it never clashes with other apps, and runs
 * tests/smoke.storybook.spec.ts. First boot compiles the preview, so the
 * webServer timeout is generous.
 */
const config: PlaywrightTestConfig = {
    testDir: './tests',
    testMatch: /smoke\.storybook\.spec\.ts/,
    timeout: 120 * 1000,
    expect: { timeout: 30 * 1000 },
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    reporter: [['list'], ['html', { outputFolder: 'playwright-report-storybook', open: 'never' }]],
    use: {
        baseURL: 'http://localhost:6007',
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
    },
    projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
    webServer: {
        command: 'npx storybook dev -p 6007 --no-open',
        url: 'http://localhost:6007',
        reuseExistingServer: false,
        timeout: 180 * 1000,
    },
}

export default config
