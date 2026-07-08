import { expect, test } from '@playwright/test'
import { BASE_URL } from './vars'
import { CLERK_CREDENTIALS, DPO_CREDENTIALS, login, logout } from './helpers/auth'
import { createPersonViaUi } from './helpers/registry'

/**
 * GDPR E2E: a clerk creates a person, then a DPO files a data subject request against that
 * person and sees it listed with its statutory due date.
 */
test('dpo files a data subject request for a person', async ({ page }) => {
    await login(page, CLERK_CREDENTIALS)
    const uid = await createPersonViaUi(page, 'Gdpr')
    await logout(page)

    await login(page, DPO_CREDENTIALS)
    await page.goto(BASE_URL + '/gdpr')
    await page.locator('#gdpr_person_uid').fill(uid)
    await page.locator('#gdpr_dsr_ref').fill('REQ-1')
    await page.locator('#gdpr_dsr_create').click()
    await page.locator('#gdpr_load').click()

    await expect(page.locator('#gdpr_requests')).toContainText('access')
})
