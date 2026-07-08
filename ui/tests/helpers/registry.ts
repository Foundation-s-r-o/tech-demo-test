import { Page, expect } from '@playwright/test'
import { BASE_URL } from '../vars'

/**
 * Creates a registry person through the UI (caller must be logged in as clerk/supervisor)
 * and returns the generated person UID from the detail URL.
 */
export async function createPersonViaUi(page: Page, familyName = 'Testovaci'): Promise<string> {
    await page.goto(BASE_URL + '/persons/add')
    await page.locator('#PersonCreateForm_givenNames').fill('Test')
    await page.locator('#PersonCreateForm_familyName').fill(familyName)
    await page.locator('#PersonCreateForm_sex').fill('M')
    await page.locator('#PersonCreateForm_dateOfBirth').fill('1990-01-01')
    await page.locator('#PersonCreateForm_citizenship').fill('703')
    await page.getByRole('button', { name: 'Uložiť' }).click()
    await expect(page).toHaveURL(new RegExp(`^${BASE_URL}/persons/P-`))
    const url = page.url()
    return url.substring(url.lastIndexOf('/') + 1)
}
