import { test, expect } from '@playwright/test'
import { attachConsoleCapture, consoleErrors, writeConsoleLog } from './console-capture'

/**
 * Storybook smoke test.
 *
 * Renders a couple of stories in isolation (Storybook dev server auto-started
 * by playwright.storybook.config.ts) and fails on any console / page error.
 * Story ids derive from the story `title` ('Common') + export name.
 * Run via `npm run smoke:storybook`.
 */
const stories = [
    { id: 'common--button', name: 'Button' },
    { id: 'common--form', name: 'Form' },
]

test.describe('Storybook smoke', () => {
    for (const story of stories) {
        test(`story "${story.name}" renders without console errors`, async ({ page }) => {
            const messages = attachConsoleCapture(page)

            await page.goto(`/iframe.html?id=${story.id}&viewMode=story`)

            const root = page.locator('#storybook-root')
            await expect(root).toBeVisible()
            await expect(root).not.toBeEmpty()

            await page.waitForTimeout(500)
            const log = writeConsoleLog(
                `../docs/smoke-storybook-${story.id}.log`,
                `Storybook ${story.name}`,
                messages
            )
            const errors = consoleErrors(messages)
            if (errors.length) {
                console.log(
                    `Storybook "${story.name}" console errors:\n` +
                        errors.map((e) => e.text).join('\n')
                )
            }
            expect(errors, `Storybook console errors detected (see ${log})`).toEqual([])
        })
    }
})
