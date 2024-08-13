import { AxiosPromise } from 'axios'
import { User } from './User'

export enum AuthStateReducerActionType {
    INIT,
    LOGIN,
    LOGOUT,
}

export type AuthStateReducerAction = {
    type: AuthStateReducerActionType
    payload?: {
        isAuthenticated?: boolean,
        user?: User | null,
    }
}

export interface AuthStateType {
    user: User | null
    isInitialised: boolean
    isAuthenticated: boolean
}

export interface AuthContextType extends AuthStateType {
    login: (LoginRequest: LoginRequest) => void
    logout: (callback: VoidFunction) => void
}

export type LoginRequest = {
    username: string
    password: string
}

export type LogoutRequest = {
    username: string
}

export type AuthApi = {
    getCurrentUser: () => AxiosPromise<User>
    login: (loginRequest: LoginRequest) => AxiosPromise<User>
    logout: (logoutRequest: LogoutRequest) => AxiosPromise
}
