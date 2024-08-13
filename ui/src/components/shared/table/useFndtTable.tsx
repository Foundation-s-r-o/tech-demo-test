import React from 'react'
import { FndtTableContext } from './TableContext'

export const useFndtTable = () => {
    const context = React.useContext(FndtTableContext)
    if (context === undefined) {
        throw new Error('useFndtTable in not within FndtTableWrapper')
    }

    return context
}
