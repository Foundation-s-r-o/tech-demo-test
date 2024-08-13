import type { PlaywrightTestConfig } from '@playwright/test'
import { devices } from '@playwright/test'
import 'dotenv/config'

const config: PlaywrightTestConfig = {
    testDir: './tests',
    /* Maximum time one test can run for. */
    timeout: 30 * 1000,
    expect: {
        /**
         * Maximum time expect() should wait for the condition to be met.
         */
        timeout: 5000,
    },
    /* Run tests in files in parallel */
    fullyParallel: true,
    /* Fail the build on CI if you accidentally left test.only in the source code. */
    forbidOnly: !!process.env.CI,
    /* Retry on CI only */
    retries: process.env.CI ? 2 : 0,
    /* Opt out of parallel tests on CI. */
    workers: process.env.CI ? 1 : undefined,
    reporter: 'html',
    globalSetup: require.resolve('./tests/global_setup'),
    use: {
        actionTimeout: 0,
        trace: 'on-first-retry',
        launchOptions: {
            slowMo: process.env.PLAYWRIGHT_SLOW_MO
                ? parseInt(process.env.PLAYWRIGHT_SLOW_MO)
                : 0,
        },
        // storageState: 'tests/storageState.json',
    },
    projects: [
        {
            name: 'chromium',
            use: {
                ...devices['Desktop Chrome'],
            },
        },
    ],
}

export default config
