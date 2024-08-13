import React, { PropsWithChildren } from 'react'
import { Form } from 'react-bootstrap'

const FndtInput = ({
    id,
    name,
    type = 'text',
    value,
    ...props
}: PropsWithChildren & {
    id?: string
    name?: string
    type?: string
    value?: string
}) => {
    return (
        <Form.Control
            {...{ id, name, type, value }}
            {...props}
        />
    )
}

export default FndtInput
