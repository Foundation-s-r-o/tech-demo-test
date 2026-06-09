import { config } from 'dotenv'
config({ path: __dirname + '/../../.env' })

// Fall back to the Flyway-seeded bootstrap user (admin/admin) so these stay `string`
// (not string | undefined) under TS6 strict mode. Env vars still override when set.
export const USERNAME = process.env.ADMIN_USERNAME?.trim() || 'admin'
export const PASSWORD = process.env.ADMIN_PASS?.trim() || 'admin'
export const BASE_URL = 'http://localhost:8080'
export const INCORRECT_PASSWORD = 'IncorrectPassw0rd!'
export const USER_FULLNAME = 'Admin Admin'
export const LOGOUT_USER = 'Neprihlásený používateľ'
