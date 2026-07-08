import { expect, test } from '@playwright/test'
import { BASE_URL } from './vars'
import { login } from './helpers/auth'
import { createPersonViaUi } from './helpers/registry'

/**
 * E2E coverage of the main page and the registry persons feature (list / create / detail /
 * client-side validation). Self-contained on the fresh in-memory `e2e` database.
 */
test.describe('Main page and persons', () => {
    test('Main page renders after login', async ({ page }) => {
        await login(page)
        await expect(page.locator('#app-content-title')).toContainText('Homepage')
        await expect(page.locator('.menu-trigger')).toBeVisible()
        await expect(page.locator('#app_header_logout')).toBeVisible()
    })

    test('Persons list page renders', async ({ page }) => {
        await login(page)
        await page.goto(BASE_URL + '/persons')
        await expect(page.locator('#app-content-title')).toContainText('Osoby')
        const table = page.locator('table.fndt_table')
        await expect(table).toBeVisible()
        await expect(table.locator('thead')).toContainText('Priezvisko')
    })

    test('Created person is shown on its detail page', async ({ page }) => {
        await login(page)
        const familyName = `Novak${Date.now().toString(36)}`
        const uid = await createPersonViaUi(page, familyName)

        // create navigated to /persons/<uid>; the composite detail is shown
        await expect(page).toHaveURL(BASE_URL + '/persons/' + uid)
        await expect(page.locator('#person_detail')).toBeVisible()
        await expect(page.locator('#person_detail')).toContainText(familyName)
        await expect(page.locator('#person_detail')).toContainText(uid)
    })

    test('Create form rejects missing required fields', async ({ page }) => {
        await login(page)
        await page.goto(BASE_URL + '/persons/add')
        await page.getByRole('button', { name: 'Uložiť' }).click()

        // client-side validation blocks submit; no navigation
        await expect(page.locator('.text-danger').first()).toBeVisible()
        await expect(page).toHaveURL(BASE_URL + '/persons/add')
    })
})
