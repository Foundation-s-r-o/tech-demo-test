import { expect, test } from '@playwright/test'
import { INCORRECT_PASSWORD, BASE_URL, USER_FULLNAME, LOGOUT_USER, USERNAME } from './vars'
import { login } from './helpers/auth'

const error_message =
    'Neboli zadané korektné prístupové údaje.Zatvorte oznam a zadajte korektné meno a heslo.'

test.describe('On login page', () => {
    test('Failed Login', async ({ page }) => {
        await page.goto(BASE_URL)

        // check
        await expect(page).toHaveURL(BASE_URL + '/login')

        // interact — submit wrong credentials
        await page.locator('#login_username').fill(USERNAME)
        await page.locator('#login_password').fill(INCORRECT_PASSWORD)
        await page.locator('#login_submit').click()

        // check — still on login, error modal shown
        await expect(page).toHaveURL(BASE_URL + '/login')
        const header_error_message = page.locator('.modal-body')
        await expect(header_error_message).toBeVisible()
        await expect(header_error_message).toContainText(error_message)

        // close error message
        await page.locator('#error_dialog_close').click()
        await expect(header_error_message).toBeHidden()
    })

    test('Successful Login', async ({ page }) => {
        await login(page)

        // check — header shows the logged-in user
        const headerUsername = page.locator('#app_header_user_fullname')
        await expect(headerUsername).toBeVisible()
        await expect(headerUsername).toContainText(USER_FULLNAME)
    })

    // Each test runs in its own isolated browser context (no shared global session since
    // global_setup was removed), so logging out here no longer affects other tests.
    test('Successfully Logged out', async ({ page }) => {
        await login(page)

        // interact — log out (stable id, not a bootstrap utility class)
        await page.locator('#app_header_logout').click()

        // check — logout redirects back to /login and the header reflects the signed-out state
        await expect(page).toHaveURL(BASE_URL + '/login')
        const headerUsername = page.locator('#app_header_user_fullname')

        // check
        await expect(headerUsername).toBeVisible()
        await expect(headerUsername).toContainText(LOGOUT_USER)
    })
})
