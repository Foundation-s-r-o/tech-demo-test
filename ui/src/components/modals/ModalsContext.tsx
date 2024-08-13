import React from 'react'
import { ModalsContextType } from './types'

export const ModalsContext = React.createContext<ModalsContextType>(null)
export const ModalsContextProvider = ModalsContext.Provider
