import React from 'react'
import { AuthContextType } from './types'

export const AuthContext = React.createContext<AuthContextType | null>(null)
export const AuthContextProvider = AuthContext.Provider
