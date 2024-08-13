import React from 'react'
import { ModalsContext } from './ModalsContext'

export const useModals = () => {
    const context = React.useContext(ModalsContext)
    if (context === undefined) {
        throw new Error('useModals in not within ModalsProvider')
    }
    return context
}
