import React from 'react'
import { FndtTableContextType } from './types'

export const FndtTableContext = React.createContext<FndtTableContextType | null>(null)
export const FndtTableProvider = FndtTableContext.Provider
