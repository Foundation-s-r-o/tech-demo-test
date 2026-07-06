import { expect, test, Page } from '@playwright/test'
import { BASE_URL } from './vars'
import { login } from './helpers/auth'

/**
 * E2E coverage of the main page and the persons feature (list / create / edit /
 * client-side validation). Each test logs in and creates its own data through
 * the UI, so the suite is self-contained on the fresh in-memory `e2e` database.
 */

type PersonInput = {
    firstName: string
    lastName: string
    email: string
    address: string
}

// Unique-per-test-run values so parallel tests and repeated runs never collide.
function uniquePerson(prefix: string): PersonInput {
    const unique = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`
    return {
        firstName: `${prefix}Fn${unique}`,
        lastName: `${prefix}Ln${unique}`,
        email: `${prefix}.${unique}@example.com`,
        address: `Testovacia 1, Bratislava`,
    }
}

async function fillPersonForm(page: Page, person: PersonInput): Promise<void> {
    await page.locator('#PersonEditForm_firstName').fill(person.firstName)
    await page.locator('#PersonEditForm_lastName').fill(person.lastName)
    await page.locator('#PersonEditForm_email').fill(person.email)
    await page.locator('#PersonEditForm_address').fill(person.address)
}

// Creates a person through the UI and returns after the app navigated to its edit page.
async function createPerson(page: Page, person: PersonInput): Promise<void> {
    await page.goto(BASE_URL + '/persons/add')
    await expect(page.locator('#app-content-title')).toContainText('Pridať osobu')
    await fillPersonForm(page, person)
    await page.getByRole('button', { name: 'Uložiť' }).click()
    // successful create navigates to the edit page /persons/<id>
    await expect(page).toHaveURL(new RegExp(`^${BASE_URL}/persons/\\d+$`))
}

test.describe('Main page and persons', () => {
    test('Main page renders after login', async ({ page }) => {
        await login(page)

        // check — home page content and chrome are rendered
        await expect(page.locator('#app-content-title')).toContainText('Homepage')
        await expect(page.locator('.menu-trigger')).toBeVisible()
        await expect(page.locator('#app_header_logout')).toBeVisible()
    })

    test('Persons list page renders', async ({ page }) => {
        await login(page)

        await page.goto(BASE_URL + '/persons')

        // check — title, table with expected columns
        await expect(page.locator('#app-content-title')).toContainText('Osoby')
        const table = page.locator('table.fndt_table')
        await expect(table).toBeVisible()
        await expect(table.locator('thead')).toContainText('Priezvisko')
        await expect(table.locator('thead')).toContainText('E-mail')
    })

    test('Created person appears in list', async ({ page }) => {
        await login(page)
        const person = uniquePerson('create')

        await createPerson(page, person)

        // check — the new person shows up in the list
        await page.goto(BASE_URL + '/persons')
        const row = page.locator('table.fndt_table tbody tr', { hasText: person.lastName })
        await expect(row).toBeVisible()
        await expect(row).toContainText(person.email)
    })

    test('Edited person shows updated data in list', async ({ page }) => {
        await login(page)
        const person = uniquePerson('edit')
        await createPerson(page, person)

        // interact — change the last name on the edit page and save
        const updatedLastName = `${person.lastName}Upd`
        await page.locator('#PersonEditForm_lastName').fill(updatedLastName)
        await page.getByRole('button', { name: 'Uložiť' }).click()

        // check — list shows the updated value
        await page.goto(BASE_URL + '/persons')
        const row = page.locator('table.fndt_table tbody tr', { hasText: updatedLastName })
        await expect(row).toBeVisible()
        await expect(row).toContainText(person.email)
    })

    test('Invalid email is rejected by form validation', async ({ page }) => {
        await login(page)
        const person = uniquePerson('invalid')

        await page.goto(BASE_URL + '/persons/add')
        await fillPersonForm(page, { ...person, email: 'not-an-email' })
        await page.getByRole('button', { name: 'Uložiť' }).click()

        // check — validation error is shown and no navigation happened
        await expect(page.locator('.text-danger').first()).toBeVisible()
        await expect(page).toHaveURL(BASE_URL + '/persons/add')
    })
})
