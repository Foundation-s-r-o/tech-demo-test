import { expect, test, Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { BASE_URL } from './vars'
import { login } from './helpers/auth'

/**
 * Accessibility smoke over the app's primary pages using axe-core.
 * Gate: zero violations with impact `critical` or `serious` (WCAG 2.0/2.1 A+AA
 * rule sets). Lesser impacts are reported in the test output but do not fail.
 *
 * Pages are checked in light mode only (dark-mode runs disabled per project
 * decision, 2026-07-06); the theme-toggle persistence test below still covers
 * the toggle control itself.
 */

type ColorMode = 'light' | 'dark'

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

// Seed the persisted theme choice before the app boots (same key the
// index.html inline script and persistService read; value is JSON-encoded).
async function useColorMode(page: Page, mode: ColorMode): Promise<void> {
    await page.addInitScript((theme) => {
        window.localStorage.setItem('fndt-theme', JSON.stringify(theme))
    }, mode)
}

for (const mode of ['light'] as ColorMode[]) {
    test.describe(`Accessibility smoke (${mode} mode)`, () => {
        test.beforeEach(async ({ page }) => {
            await useColorMode(page, mode)
        })

        test(`Login page has no serious a11y violations (${mode})`, async ({ page }) => {
            await page.goto(BASE_URL + '/login')
            await expect(page.locator('#login_username')).toBeVisible()
            await expect(page.locator('html')).toHaveAttribute('data-bs-theme', mode)
            await expectNoSeriousViolations(page, `login (${mode})`)
        })

        test(`Home page has no serious a11y violations (${mode})`, async ({ page }) => {
            await login(page)
            await expect(page.locator('#app-content-title')).toContainText('Homepage')
            await expectNoSeriousViolations(page, `home (${mode})`)
        })

        test(`Persons list has no serious a11y violations (${mode})`, async ({ page }) => {
            await login(page)
            await page.goto(BASE_URL + '/persons')
            await expect(page.locator('table.fndt_table')).toBeVisible()
            await expectNoSeriousViolations(page, `persons list (${mode})`)
        })

        test(`Persons add form has no serious a11y violations (${mode})`, async ({ page }) => {
            await login(page)
            await page.goto(BASE_URL + '/persons/add')
            await expect(page.locator('#PersonCreateForm_givenNames')).toBeVisible()
            await expectNoSeriousViolations(page, `persons add (${mode})`)
        })
    })
}

test.describe('Theme toggle', () => {
    test('Toggle switches to dark mode and persists across reload', async ({ page }) => {
        await page.goto(BASE_URL + '/login')
        await expect(page.locator('html')).toHaveAttribute('data-bs-theme', 'light')

        await page.locator('#theme_toggle').click()
        await expect(page.locator('html')).toHaveAttribute('data-bs-theme', 'dark')

        await page.reload()
        await expect(page.locator('#login_username')).toBeVisible()
        await expect(page.locator('html')).toHaveAttribute('data-bs-theme', 'dark')
    })
})
