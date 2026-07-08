import { expect, test } from '@playwright/test'
import { BASE_URL } from './vars'
import { AUDITOR_CREDENTIALS, login } from './helpers/auth'

/**
 * Audit E2E: a security auditor verifies the hash chain is intact and sees the events table.
 */
test('auditor verifies the audit hash chain', async ({ page }) => {
    await login(page, AUDITOR_CREDENTIALS)
    await page.goto(BASE_URL + '/audit')

    await page.locator('#audit_verify_chain').click()
    await expect(page.locator('#audit_chain_result')).toContainText('neporušená')
    await expect(page.locator('#audit_events')).toBeVisible()
})
