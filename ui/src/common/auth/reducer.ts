import { Reducer } from 'react'
import { AuthStateReducerActionType, AuthStateType, AuthStateReducerAction } from './types'

const authStateReducer: Reducer<AuthStateType, AuthStateReducerAction> = (
    state,
    action
): AuthStateType => {
    switch (action.type) {
        case AuthStateReducerActionType.INIT: {
            const { isAuthenticated = false, user = null } = action.payload ?? {}

            return {
                ...state,
                isAuthenticated,
                user,
                isInitialised: true,
            }
        }
        case AuthStateReducerActionType.LOGIN: {
            const { user = null } = action.payload ?? {}

            return {
                ...state,
                isAuthenticated: true,
                user,
            }
        }
        case AuthStateReducerActionType.LOGOUT: {
            return {
                ...state,
                isAuthenticated: false,
                user: null,
            }
        }
        default: {
            return { ...state }
        }
    }
}

export default authStateReducer
