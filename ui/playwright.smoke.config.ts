import type { PlaywrightTestConfig } from '@playwright/test'
import { devices } from '@playwright/test'
import 'dotenv/config'

/**
 * Backend-free app smoke config. Auto-starts the webpack dev server on a
 * dedicated port (8090) and runs tests/smoke.spec.ts. Deliberately omits the
 * global_setup used by the full E2E suite (that one logs into the backend on
 * :8082).
 */
const config: PlaywrightTestConfig = {
    testDir: './tests',
    testMatch: /smoke\.spec\.ts/,
    timeout: 90 * 1000,
    expect: { timeout: 15 * 1000 },
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 1 : 0,
    reporter: [['list'], ['html', { outputFolder: 'playwright-report-smoke', open: 'never' }]],
    use: {
        // Dedicated smoke port (not 8080) so it never clashes with a developer's
        // running `npm start` dev server.
        baseURL: 'http://localhost:8090',
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
    },
    projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
    webServer: {
        // --no-client-overlay: the dev-server overlay surfaces SCSS deprecation
        // warnings and would cover the page, intercepting clicks during the smoke.
        command: 'npx webpack serve --mode development --port 8090 --no-client-overlay',
        // Wait for the compiled bundle (not just the open port) so tests don't
        // start mid-compile on a cold dev server.
        url: 'http://localhost:8090/bundle.js',
        reuseExistingServer: false,
        timeout: 180 * 1000,
    },
}

export default config
