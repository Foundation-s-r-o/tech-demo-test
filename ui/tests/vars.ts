import { config } from 'dotenv'
config({ path: __dirname + '/../../.env' })

export const USERNAME = process.env.ADMIN_USERNAME
export const PASSWORD = process.env.ADMIN_PASS
export const BASE_URL = 'http://localhost:8082'
export const STORAGE_STATE_ADMIN = './tests/storageStateAdmin.json'
export const STORAGE_STATE_USER = './tests/storageStateUser.json'
export const INCORRECT_PASSWORD = 'IncorrectPassw0rd!'
export const USER_FULLNAME = 'Admin Admin'
export const LOGOUT_USER = 'Neprihlásený používateľ'
