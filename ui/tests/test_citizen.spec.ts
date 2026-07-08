import { expect, test } from '@playwright/test'
import { BASE_URL } from './vars'
import { CITIZEN_CREDENTIALS, login } from './helpers/auth'

/**
 * Citizen portal E2E: the seeded citizen sees their own composite data and access log via /me.
 */
test('citizen sees own data through the portal', async ({ page }) => {
    await login(page, CITIZEN_CREDENTIALS)
    await page.goto(BASE_URL + '/me')

    await expect(page.locator('#me_detail')).toContainText('Portalovy')
    await expect(page.locator('#me_access_log')).toBeVisible()
})
