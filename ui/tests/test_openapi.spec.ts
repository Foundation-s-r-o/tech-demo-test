import { expect, test } from '@playwright/test'
import { readFileSync } from 'fs'
import { resolve } from 'path'

/**
 * OpenAPI contract-drift gate. The committed `ui/api-docs.json` is the input
 * for the generated TS client (`npm run generate-openapi-services`); if the
 * backend's live spec no longer matches it, the client is stale. Refresh flow:
 * start the API, save `/v3/api-docs` into `ui/api-docs.json`, regenerate the
 * client, and commit both. The spec ↔ generated-client half of the gate runs
 * in `./doctor`.
 */
test('Live OpenAPI spec matches the committed api-docs.json', async ({ request }) => {
    const live = await (await request.get('http://localhost:8082/v3/api-docs')).json()
    const committed = JSON.parse(readFileSync(resolve(__dirname, '../api-docs.json'), 'utf-8'))

    expect(live, 'Backend OpenAPI spec drifted from ui/api-docs.json — refresh the spec and regenerate the client').toEqual(committed)
})
