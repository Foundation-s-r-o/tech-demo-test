import React, { useState } from 'react'
import { useAccordionButton } from 'react-bootstrap'
import { MenuCollapsePropsType } from './types'

function MenuCollapse({ children, eventKey }: MenuCollapsePropsType) {
    const [toggle, setToggle] = useState(true)
    const decoratedOnClick = useAccordionButton(eventKey, () => {
        setToggle((prevValue) => !prevValue)
    })

    return (
        <div
            className="w-100 menu-row-item"
            onClick={decoratedOnClick}>
            <i
                className={`bi ${
                    toggle ? 'bi-chevron-right' : 'bi-chevron-down'
                }  text-end`}
            />
            {children}
        </div>
    )
}

export default MenuCollapse
