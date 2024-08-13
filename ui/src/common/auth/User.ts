import { Role } from './Role'

export type User = {
    id: number
    email: string
    firstName: string
    lastName: string
    username: string
    role?: Role
}
