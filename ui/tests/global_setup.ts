import { chromium, FullConfig, expect } from '@playwright/test'
import {PASSWORD,
        STORAGE_STATE_ADMIN,
        USERNAME,   BASE_URL,
        USER_FULLNAME
} from './vars'
import 'dotenv/config'

async function globalSetup(config: FullConfig) {
    const browser = await chromium.launch()
    const context = await browser.newContext()
    const page = await context.newPage()
    await page.goto(BASE_URL)
    await expect(page).toHaveURL(BASE_URL + '/login')
    const inputUsername = page.locator('#login_username')
    const inputPassword = page.locator('#login_password')
    const btnSubmit = page.locator('#login_submit')
    await inputUsername.click()
    await inputUsername.fill(USERNAME)
    await inputPassword.click()
    await inputPassword.fill(PASSWORD)
    await btnSubmit.click()
    await expect(page).toHaveURL(BASE_URL)

    const headerUsername = page.locator('#app_header_user_fullname')
    await expect(headerUsername).toBeVisible()
    await expect(headerUsername).toContainText(USER_FULLNAME)

    // Save signed-in state to 'storageState.json'.
    await page.context().storageState({ path: STORAGE_STATE_ADMIN })
    await browser.close()
}

export default globalSetup
