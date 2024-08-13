import React, { PropsWithChildren } from 'react'
import { Accordion } from 'react-bootstrap'
import './Accordion.scss'

export const FndtAccordionHeader = ({
    children,
    ...props
}: PropsWithChildren) => {
    return (
        <Accordion.Header {...props}>
            <div className="d-flex flex-fill align-self-stretch ps-4">
                <b className="text-primary">{children}</b>
            </div>
        </Accordion.Header>
    )
}
