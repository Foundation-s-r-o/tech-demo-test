import { formatDate } from '@common/util'
import React, { PropsWithChildren } from 'react'

const FndtTextDate = ({
    value,
    id,
    ...props
}: PropsWithChildren & {
    value?: string | Date
    id?: string
}) => {
    const textValue = value ? formatDate(value instanceof Date ? value : new Date(value)) : ''
    return (
        <div
            {...props}
            id={id}
            className="form-value">
            {textValue}
        </div>
    )
}

export default FndtTextDate
