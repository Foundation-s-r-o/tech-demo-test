import React, { PropsWithChildren } from 'react'
import FndtInput from './Input'

const FndtInputNumber = (props: PropsWithChildren) => {
    return (
        <FndtInput
            type="number"
            {...props}
        />
    )
}

export default FndtInputNumber
