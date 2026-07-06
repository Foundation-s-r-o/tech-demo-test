import { Page, expect } from '@playwright/test'
import { BASE_URL, PASSWORD, USERNAME, USER_FULLNAME } from '../vars'

export type Credentials = {
    username: string
    password: string
    fullName: string
}

/** The ROLE_ADMIN demo account (admin/admin) — the default for most tests. */
export const ADMIN_CREDENTIALS: Credentials = {
    username: USERNAME,
    password: PASSWORD,
    fullName: USER_FULLNAME,
}

/**
 * Logs in via the login form and waits until the app confirms the session
 * (header shows the user's full name). Defaults to the admin demo account.
 */
export async function login(page: Page, credentials: Credentials = ADMIN_CREDENTIALS): Promise<void> {
    await page.goto(BASE_URL)
    await expect(page).toHaveURL(BASE_URL + '/login')

    await page.locator('#login_username').fill(credentials.username)
    await page.locator('#login_password').fill(credentials.password)
    await page.locator('#login_submit').click()

    await expect(page).toHaveURL(BASE_URL)
    await expect(page.locator('#app_header_user_fullname')).toContainText(credentials.fullName)
}
