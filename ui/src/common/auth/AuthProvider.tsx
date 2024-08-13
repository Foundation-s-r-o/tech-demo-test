import React, { PropsWithChildren, useEffect, useReducer } from 'react'
import persistService from '../persistService'
import { AuthContextProvider } from './AuthContext'
import authStateReducer from './reducer'
import {
    AuthStateReducerActionType,
    AuthStateType,
    LoginRequest,
} from './types'
import { User } from './User'
import { authControllerApi } from '../../api/api'

const initialState: AuthStateType = {
    isInitialised: false,
    isAuthenticated: false,
    user: null,
}

export default function AuthProvider({ children }: PropsWithChildren) {
    const [state, dispatch] = useReducer(authStateReducer, initialState)

    const setSession = (user: User) => {
        persistService.set('user', user)
    }

    const unsetSession = () => {
        persistService.remove('user')
    }

    const login = async (loginRequest: LoginRequest) => {
        const response = await authControllerApi.login({
            loginRequestDTO: loginRequest,
        })
        const user = response.data as User
        dispatch({ type: AuthStateReducerActionType.LOGIN, payload: { user } })
        setSession(user)
    }

    const logout = async (callback: VoidFunction) => {
        try {
            await authControllerApi.logout({ logoutRequestDTO: { username: state.user.username } })
            unsetSession()
            dispatch({ type: AuthStateReducerActionType.LOGOUT })
            callback()
        } catch (err) {
            console.log('error during logout', err)
        }
    }

    const value = { ...state, login, logout }

    useEffect(() => {
        const initAuth = async () => {
            try {
                const storedUser = persistService.get('user')

                if (storedUser) {
                    const response = await authControllerApi.currentUser()
                    const user = response.data as User

                    dispatch({
                        type: AuthStateReducerActionType.INIT,
                        payload: {
                            isAuthenticated: true,
                            user: { ...user },
                        },
                    })
                } else {
                    dispatch({
                        type: AuthStateReducerActionType.INIT,
                        payload: {
                            isAuthenticated: false,
                            user: null,
                        },
                    })
                }
            } catch (err) {
                dispatch({
                    type: AuthStateReducerActionType.INIT,
                    payload: {
                        isAuthenticated: false,
                        user: null,
                    },
                })
            }
        }

        initAuth()
    }, [])

    if (!state.isInitialised) {
        return <>Loading...</>
    }

    return <AuthContextProvider value={value}>{children}</AuthContextProvider>
}
