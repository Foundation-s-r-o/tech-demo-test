import { expect, test, Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { BASE_URL } from './vars'
import { login } from './helpers/auth'

/**
 * Accessibility smoke over the app's primary pages using axe-core.
 * Gate: zero violations with impact `critical` or `serious` (WCAG 2.0/2.1 A+AA
 * rule sets). Lesser impacts are reported in the test output but do not fail.
 */

async function expectNoSeriousViolations(page: Page, pageName: string): Promise<void> {
    const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .analyze()

    const serious = results.violations.filter(
        (v) => v.impact === 'critical' || v.impact === 'serious'
    )
    const lesser = results.violations.filter(
        (v) => v.impact !== 'critical' && v.impact !== 'serious'
    )
    if (lesser.length) {
        console.log(
            `[a11y] ${pageName}: ${lesser.length} minor violation(s) (non-blocking): ` +
                lesser.map((v) => v.id).join(', ')
        )
    }
    expect(
        serious.map((v) => ({ id: v.id, impact: v.impact, help: v.help, nodes: v.nodes.length })),
        `Serious/critical a11y violations on ${pageName}`
    ).toEqual([])
}

test.describe('Accessibility smoke', () => {
    test('Login page has no serious a11y violations', async ({ page }) => {
        await page.goto(BASE_URL + '/login')
        await expect(page.locator('#login_username')).toBeVisible()
        await expectNoSeriousViolations(page, 'login')
    })

    test('Home page has no serious a11y violations', async ({ page }) => {
        await login(page)
        await expect(page.locator('#app-content-title')).toContainText('Homepage')
        await expectNoSeriousViolations(page, 'home')
    })

    test('Persons list has no serious a11y violations', async ({ page }) => {
        await login(page)
        await page.goto(BASE_URL + '/persons')
        await expect(page.locator('table.fndt_table')).toBeVisible()
        await expectNoSeriousViolations(page, 'persons list')
    })

    test('Persons add form has no serious a11y violations', async ({ page }) => {
        await login(page)
        await page.goto(BASE_URL + '/persons/add')
        await expect(page.locator('#PersonEditForm_firstName')).toBeVisible()
        await expectNoSeriousViolations(page, 'persons add')
    })
})
