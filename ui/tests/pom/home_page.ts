import { Page } from '@playwright/test'

class HomePage {
    page

    constructor(page: Page) {
        this.page = page
    }
}

export default HomePage
