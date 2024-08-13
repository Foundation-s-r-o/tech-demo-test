import React, { PropsWithChildren } from 'react'
import { Form } from 'react-bootstrap'

const FndtCheckbox = (props: PropsWithChildren) => {
    return <Form.Check {...props} />
}

export default FndtCheckbox
