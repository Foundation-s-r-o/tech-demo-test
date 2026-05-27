import { test, expect } from '@playwright/test'
import { attachConsoleCapture, consoleErrors, writeConsoleLog } from './console-capture'

/**
 * App smoke test — backend-free.
 *
 * Loads the app on the dev server (auto-started by playwright.smoke.config.ts),
 * confirms the public login shell renders, exercises a couple of visual
 * elements, and fails on any browser console / page error. Run after major
 * (>50 net line) frontend changes via `npm run smoke`.
 */
test.describe('App smoke', () => {
    test('login shell loads and is interactive without console errors', async ({ page }) => {
        const messages = attachConsoleCapture(page)

        // Unauthenticated visit to root is redirected to /login by RequireAuth.
        await page.goto('/', { waitUntil: 'domcontentloaded' })

        const username = page.locator('#login_username')
        const password = page.locator('#login_password')
        const submit = page.locator('#login_submit')

        // Generous timeout: the dev server may still be compiling on cold start.
        await expect(username).toBeVisible({ timeout: 30_000 })
        await expect(page).toHaveURL(/\/login$/)
        await expect(password).toBeVisible()
        await expect(submit).toBeVisible()

        // Click a few visual elements and type — catches render/handler errors.
        await username.click()
        await username.fill('smoke-user')
        await password.click()
        await password.fill('smoke-pass')
        await expect(username).toHaveValue('smoke-user')

        // Give async errors a moment to surface, then dump + assert.
        await page.waitForTimeout(500)
        const log = writeConsoleLog('../docs/smoke-app.log', 'App smoke', messages)
        const errors = consoleErrors(messages)
        if (errors.length) {
            console.log('App smoke console errors:\n' + errors.map((e) => e.text).join('\n'))
        }
        expect(errors, `Browser console errors detected (see ${log})`).toEqual([])
    })
})
