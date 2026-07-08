import { Page, expect } from '@playwright/test'
import { BASE_URL, PASSWORD, USERNAME, USER_FULLNAME } from '../vars'

export type Credentials = {
    username: string
    password: string
    fullName: string
}

/** The SUPERVISOR demo account (admin/admin) — the default for most tests. */
export const ADMIN_CREDENTIALS: Credentials = {
    username: USERNAME,
    password: PASSWORD,
    fullName: USER_FULLNAME,
}

// One demo account per registry role (seeded by LocalAdminInitializer, local | it profiles).
export const SUPERVISOR_CREDENTIALS: Credentials = ADMIN_CREDENTIALS
export const CLERK_CREDENTIALS: Credentials = { username: 'user', password: 'user', fullName: 'User User' }
export const DPO_CREDENTIALS: Credentials = { username: 'dpo', password: 'dpo', fullName: 'Dpo Officer' }
export const AUDITOR_CREDENTIALS: Credentials =
    { username: 'auditor', password: 'auditor', fullName: 'Security Auditor' }
export const AGENCY_CREDENTIALS: Credentials =
    { username: 'agency', password: 'agency', fullName: 'Agency Reader' }
export const CITIZEN_CREDENTIALS: Credentials =
    { username: 'citizen', password: 'citizen', fullName: 'Citizen Portal' }

/** Logs the current user out and waits for the login page. */
export async function logout(page: Page): Promise<void> {
    await page.locator('#app_header_logout').click()
    await expect(page).toHaveURL(BASE_URL + '/login')
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
