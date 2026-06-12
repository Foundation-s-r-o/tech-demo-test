import type { PlaywrightTestConfig } from '@playwright/test'
import { devices } from '@playwright/test'
import 'dotenv/config'

const config: PlaywrightTestConfig = {
    testDir: './tests',
    testMatch: /test_.*\.spec\.ts/,
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
    use: {
        baseURL: 'http://localhost:8080',
        actionTimeout: 0,
        trace: 'on-first-retry',
        launchOptions: {
            slowMo: process.env.PLAYWRIGHT_SLOW_MO
                ? parseInt(process.env.PLAYWRIGHT_SLOW_MO)
                : 0,
        },
    },
    webServer: [
        {
            command: './mvnw spring-boot:run -Dspring-boot.run.profiles=local,e2e -Dspring-boot.run.arguments=--server.port=8082',
            cwd: '../api',
            url: 'http://localhost:8082/actuator/health',
            reuseExistingServer: false,
            timeout: 180 * 1000,
        },
        {
            command: 'npx webpack serve --mode development --port 8080 --no-client-overlay',
            url: 'http://localhost:8080/bundle.js',
            reuseExistingServer: false,
            timeout: 180 * 1000,
        },
    ],
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
