import { expect, test } from '@playwright/test'
import { INCORRECT_PASSWORD, BASE_URL, USER_FULLNAME, LOGOUT_USER, USERNAME, PASSWORD } from './vars'

const error_message =
    'Neboli zadané korektné prístupové údaje.Zatvorte oznam a zadajte korektné meno a heslo.'

test.describe('On login page', () => {
    test('Failed Login', async ({ page }) => {
        await page.goto(BASE_URL)

        // check
        await expect(page).toHaveURL(BASE_URL + '/login')

        // find elements
        const inputUsername = page.locator('#login_username')
        const inputPassword = page.locator('#login_password')
        const btnSubmit = page.locator('#login_submit')
        const btnZatvorit = page.locator('[type="button"]')

        // interact
        await inputUsername.click()
        await inputUsername.fill(USERNAME)
        await inputPassword.click()
        await inputPassword.fill(INCORRECT_PASSWORD)
        await btnSubmit.click()

        // check
        await expect(page).toHaveURL(BASE_URL + '/login')

        // find elements
        const header_error_message = page.locator('.modal-body')

        // check
        await expect(header_error_message).toBeVisible()
        await expect(header_error_message).toContainText(error_message)
        // close error message
        await btnZatvorit.click()
    })

    test('Successful Login', async ({ page }) => {
        // Runs before each test and signs in each page.
        await page.goto(BASE_URL)

        // check
        await expect(page).toHaveURL(BASE_URL + '/login')

        // find elements
        const inputUsername = page.locator('#login_username')
        const inputPassword = page.locator('#login_password')
        const btnSubmit = page.locator('#login_submit')

        // interact
        await inputUsername.click()
        await inputUsername.fill(USERNAME)
        await inputPassword.click()
        await inputPassword.fill(PASSWORD)
        await btnSubmit.click()

        // check
        await expect(page).toHaveURL(BASE_URL)

        // find elements
        const headerUsername = page.locator('#app_header_user_fullname')

        // check
        await expect(headerUsername).toBeVisible()
        await expect(headerUsername).toContainText(USER_FULLNAME)
    })

    // Each test runs in its own isolated browser context (no shared global session since
    // global_setup was removed), so logging out here no longer affects other tests.
    test('Successfully Logged out', async ({ page }) => {
        await page.goto(BASE_URL)

        //check
        await expect(page).toHaveURL(BASE_URL + '/login')

        // find elements
        const inputUsername = page.locator('#login_username')
        const inputPassword = page.locator('#login_password')
        const btnSubmit = page.locator('#login_submit')

        // interact
        await inputUsername.click()
        await inputUsername.fill(USERNAME)
        await inputPassword.click()
        await inputPassword.fill(PASSWORD)
        await btnSubmit.click()

        // logged in: land on home, header shows the user
        await expect(page).toHaveURL(BASE_URL)
        await expect(page.locator('#app_header_user_fullname')).toContainText(USER_FULLNAME)

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
