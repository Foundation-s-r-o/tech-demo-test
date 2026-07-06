import { expect, test } from '@playwright/test'
import { BASE_URL, ROLE_USER_FULLNAME, ROLE_USER_PASSWORD, ROLE_USER_USERNAME } from './vars'
import { login, Credentials } from './helpers/auth'

/**
 * Authorization-matrix E2E (see SECURITY.md): ROLE_USER may read persons but all
 * writes are ADMIN-only — the server must reject them and the UI must surface
 * the rejection instead of pretending the write succeeded.
 */

const userCredentials: Credentials = {
    username: ROLE_USER_USERNAME,
    password: ROLE_USER_PASSWORD,
    fullName: ROLE_USER_FULLNAME,
}

test.describe('Authorization matrix for ROLE_USER', () => {
    test('USER can view the persons list', async ({ page }) => {
        await login(page, userCredentials)

        await page.goto(BASE_URL + '/persons')

        // check — reads are open to any authenticated role
        await expect(page.locator('#app-content-title')).toContainText('Osoby')
        await expect(page.locator('table.fndt_table')).toBeVisible()
    })

    test('USER cannot create a person (server rejects, UI shows error)', async ({ page }) => {
        await login(page, userCredentials)
        const lastName = `AuthzLn${Date.now().toString(36)}`

        await page.goto(BASE_URL + '/persons/add')
        await page.locator('#PersonEditForm_firstName').fill('AuthzFn')
        await page.locator('#PersonEditForm_lastName').fill(lastName)
        await page.locator('#PersonEditForm_email').fill(`authz.${lastName}@example.com`)
        await page.locator('#PersonEditForm_address').fill('Testovacia 1, Bratislava')
        await page.getByRole('button', { name: 'Uložiť' }).click()

        // check — error modal shown, no navigation to an edit page
        await expect(page.locator('#error_dialog_msg')).toBeVisible()
        await expect(page).toHaveURL(BASE_URL + '/persons/add')
        await page.locator('#error_dialog_close').click()

        // check — the person was NOT created
        await page.goto(BASE_URL + '/persons')
        await expect(page.locator('table.fndt_table')).toBeVisible()
        await expect(page.locator('table.fndt_table tbody')).not.toContainText(lastName)
    })
})
