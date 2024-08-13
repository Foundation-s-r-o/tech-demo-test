import React, { PropsWithChildren } from 'react'
import {getDateValue} from '@common/util'

const FndtText = ({
    value,
    children,
    id,
    ...props
}: PropsWithChildren & {
    value?: string | Date
    id?: string
}) => {
    const textValue = value ? getDateValue(value) : children

    return (
        <div
            {...props}
            id={id}
            className="form-value">
            {textValue}
        </div>
    )
}

export default FndtText
