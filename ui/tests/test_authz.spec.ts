import { expect, test } from '@playwright/test'
import { BASE_URL } from './vars'
import {
    AGENCY_CREDENTIALS,
    AUDITOR_CREDENTIALS,
    CITIZEN_CREDENTIALS,
    CLERK_CREDENTIALS,
    DPO_CREDENTIALS,
    login
} from './helpers/auth'

/**
 * Authorization matrix (see SECURITY.md). The role-gated sidebar mirrors the server rules for UX;
 * the server (@PreAuthorize + office scoping) is the enforcement point — verified by the agency
 * create-rejection below.
 */
test.describe('Role-gated menu', () => {
    test('clerk sees persons + records only', async ({ page }) => {
        await login(page, CLERK_CREDENTIALS)
        const sidebar = page.locator('#app_sidebar')
        await expect(sidebar).toContainText('Osoby')
        await expect(sidebar).toContainText('Matričné záznamy')
        await expect(sidebar).not.toContainText('Audit')
        await expect(sidebar).not.toContainText('Portál občana')
    })

    test('dpo sees persons, GDPR and audit', async ({ page }) => {
        await login(page, DPO_CREDENTIALS)
        const sidebar = page.locator('#app_sidebar')
        await expect(sidebar).toContainText('Osoby')
        await expect(sidebar).toContainText('GDPR')
        await expect(sidebar).toContainText('Audit')
    })

    test('auditor sees audit but not persons', async ({ page }) => {
        await login(page, AUDITOR_CREDENTIALS)
        const sidebar = page.locator('#app_sidebar')
        await expect(sidebar).toContainText('Audit')
        await expect(sidebar).not.toContainText('Osoby')
    })

    test('citizen sees only the portal', async ({ page }) => {
        await login(page, CITIZEN_CREDENTIALS)
        const sidebar = page.locator('#app_sidebar')
        await expect(sidebar).toContainText('Portál občana')
        await expect(sidebar).not.toContainText('Osoby')
    })
})

test.describe('Agency endpoint (T9)', () => {
    const AGENCY_API = 'http://localhost:8082/api/agency/persons'

    test('agency reader is authorized on the agency endpoint (404 for a missing uid, not 403)', async ({ page }) => {
        await login(page, AGENCY_CREDENTIALS)
        // Authorized + authenticated -> the request reaches the handler, which 404s on the unknown
        // person. A 401/403 here would mean the allow-rule regressed.
        const res = await page.request.get(`${AGENCY_API}/P-DOES-NOT-EXIST`, {
            headers: { 'X-Access-Purpose': 'e2e-agency-check' }
        })
        expect(res.status()).toBe(404)
    })

    test('clerk is forbidden on the agency endpoint (403)', async ({ page }) => {
        await login(page, CLERK_CREDENTIALS)
        const res = await page.request.get(`${AGENCY_API}/P-DOES-NOT-EXIST`, {
            headers: { 'X-Access-Purpose': 'e2e-agency-check' }
        })
        expect(res.status()).toBe(403)
    })
})

test.describe('Server-side enforcement', () => {
    test('agency cannot create a person (server rejects, UI shows error)', async ({ page }) => {
        await login(page, AGENCY_CREDENTIALS)
        await page.goto(BASE_URL + '/persons/add')
        await page.locator('#PersonCreateForm_givenNames').fill('X')
        await page.locator('#PersonCreateForm_familyName').fill('Y')
        await page.locator('#PersonCreateForm_sex').fill('M')
        await page.locator('#PersonCreateForm_dateOfBirth').fill('1990-01-01')
        await page.locator('#PersonCreateForm_citizenship').fill('703')
        await page.getByRole('button', { name: 'Uložiť' }).click()

        await expect(page.locator('#error_dialog_msg')).toBeVisible()
        await expect(page).toHaveURL(BASE_URL + '/persons/add')
    })
})
