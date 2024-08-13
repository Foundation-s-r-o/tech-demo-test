import React from 'react'
import { FndtTableBodyRowProps } from './types'

const FndtTableBodyRow = ({ className, onClick, children }: FndtTableBodyRowProps) => {
    const whiteListedProps = {
        className,
        onClick,
    }

    return <tr {...whiteListedProps}>{children}</tr>
}

export default FndtTableBodyRow
