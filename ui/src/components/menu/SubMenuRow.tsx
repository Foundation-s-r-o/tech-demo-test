import React from 'react'
import { useNavigate } from 'react-router-dom'
import { ListItemType } from './types'
import './Menu.scss'
import { Accordion } from 'react-bootstrap'

const SubMenuRow = (props: ListItemType) => {
    const navigate = useNavigate()
    return (
        <Accordion>
            <div
                onClick={() => {
                    navigate(props.link, {
                        replace: true,
                    })
                }}
                className="w-100 menu-row-item">
                {props.name}
            </div>
        </Accordion>
    )
}

export default SubMenuRow
