import { expect, test } from '@playwright/test'
import { BASE_URL } from './vars'
import { CLERK_CREDENTIALS, login } from './helpers/auth'
import { createPersonViaUi } from './helpers/registry'

const entryNo = () => String(Math.floor(Math.random() * 1_000_000))

/**
 * Vital-records E2E: a clerk registers a birth for their own office (2957) and is blocked
 * from registering for another office (server-side office scoping, T8).
 */
test.describe('Vital records', () => {
    test('clerk registers a birth for their own office', async ({ page }) => {
        await login(page, CLERK_CREDENTIALS)
        const child = await createPersonViaUi(page, 'Dieta')

        await page.goto(BASE_URL + '/records/births')
        await page.locator('#BirthForm_officeCode').fill('2957')
        await page.locator('#BirthForm_entryNo').fill(entryNo())
        await page.locator('#BirthForm_childUid').fill(child)
        await page.locator('#BirthForm_dateOfBirth').fill('2024-05-01')
        await page.getByRole('button', { name: 'Uložiť' }).click()

        await expect(page.locator('#birth_created')).toBeVisible()
    })

    test('clerk cannot register a birth for another office', async ({ page }) => {
        await login(page, CLERK_CREDENTIALS)
        const child = await createPersonViaUi(page, 'Dieta')

        await page.goto(BASE_URL + '/records/births')
        await page.locator('#BirthForm_officeCode').fill('2958')
        await page.locator('#BirthForm_entryNo').fill(entryNo())
        await page.locator('#BirthForm_childUid').fill(child)
        await page.locator('#BirthForm_dateOfBirth').fill('2024-05-01')
        await page.getByRole('button', { name: 'Uložiť' }).click()

        await expect(page.locator('#error_dialog_msg')).toBeVisible()
    })
})
