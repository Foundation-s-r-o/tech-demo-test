import React from 'react'
import { FndtTableContextType } from './types'

export const FndtTableContext = React.createContext<FndtTableContextType>(null)
export const FndtTableProvider = FndtTableContext.Provider
