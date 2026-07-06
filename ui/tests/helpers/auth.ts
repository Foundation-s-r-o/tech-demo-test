import { Page, expect } from '@playwright/test'
import { BASE_URL, PASSWORD, USERNAME, USER_FULLNAME } from '../vars'

/**
 * Logs in as the local-profile test user via the login form and waits until
 * the app confirms the session (header shows the user's full name).
 */
export async function login(page: Page): Promise<void> {
    await page.goto(BASE_URL)
    await expect(page).toHaveURL(BASE_URL + '/login')

    await page.locator('#login_username').fill(USERNAME)
    await page.locator('#login_password').fill(PASSWORD)
    await page.locator('#login_submit').click()

    await expect(page).toHaveURL(BASE_URL)
    await expect(page.locator('#app_header_user_fullname')).toContainText(USER_FULLNAME)
}
