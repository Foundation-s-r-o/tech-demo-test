import { Page } from '@playwright/test'
import { mkdirSync, writeFileSync } from 'fs'
import { dirname, resolve } from 'path'

/**
 * Helpers to capture browser console output / page errors during a Playwright
 * run so smoke tests can both (a) fail on real errors and (b) dump everything
 * to a log file under docs/ for inspection.
 */

export type CapturedMessage = {
    type: string
    text: string
    location?: string
}

/**
 * Build-tool / framework noise that is relayed into the browser console but is
 * not real application output (dev-server compile warnings, HMR chatter, the
 * React DevTools banner). Filtered out so logs stay focused on app errors.
 */
const isNoise = (text: string, location?: string): boolean =>
    /webpack-dev-server|webpack\/hot/.test(location ?? '') ||
    text.startsWith('[webpack-dev-server]') ||
    text.startsWith('[HMR]') ||
    text.includes('Download the React DevTools')

/** Attach listeners to a page and collect console, pageerror and failed-request events. */
export const attachConsoleCapture = (page: Page): CapturedMessage[] => {
    const messages: CapturedMessage[] = []
    page.on('console', (msg) => {
        const location = msg.location()?.url
        if (isNoise(msg.text(), location)) return
        messages.push({ type: msg.type(), text: msg.text(), location })
    })
    page.on('pageerror', (err) => {
        messages.push({ type: 'pageerror', text: err.message })
    })
    page.on('requestfailed', (req) => {
        messages.push({
            type: 'requestfailed',
            text: `${req.method()} ${req.url()} — ${req.failure()?.errorText ?? ''}`,
        })
    })
    return messages
}

/** Only the entries that should fail a smoke test. */
export const consoleErrors = (messages: CapturedMessage[]): CapturedMessage[] =>
    messages.filter((m) => m.type === 'error' || m.type === 'pageerror')

/**
 * Write all captured messages to a log file (path is relative to the ui/ dir,
 * e.g. '../docs/smoke-app.log'). Returns the absolute path written.
 */
export const writeConsoleLog = (
    logFile: string,
    title: string,
    messages: CapturedMessage[]
): string => {
    const abs = resolve(__dirname, '..', logFile)
    mkdirSync(dirname(abs), { recursive: true })
    const lines = messages.map(
        (m) => `[${m.type}] ${m.text}${m.location ? ` (${m.location})` : ''}`
    )
    const body =
        `=== ${title} @ ${new Date().toISOString()} ===\n` +
        (lines.length ? lines.join('\n') : '(no console output)') +
        '\n'
    writeFileSync(abs, body)
    return abs
}
