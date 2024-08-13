import React from 'react'
import { MenuRowPropsType } from './types'
import { Accordion } from 'react-bootstrap'
import SubMenuRow from './SubMenuRow'
import './Menu.scss'
import MenuCollapse from './MenuCollapse'

const MenuRow = (props: MenuRowPropsType) => {
    const child = props.child
    const i = props.iter
    if (child.listItem) {
        return (
            <Accordion>
                <div key={`c${i}`}>
                    <div>
                        <MenuCollapse eventKey={`c${i}`}>
                            {child.name}
                        </MenuCollapse>
                        <Accordion.Collapse eventKey={`c${i}`}>
                            <div>
                                {child.items.map((row, iter) => (
                                    <SubMenuRow
                                        key={`b${iter}`}
                                        name={row.name}
                                        link={row.link}
                                    />
                                ))}
                            </div>
                        </Accordion.Collapse>
                    </div>
                </div>
            </Accordion>
        )
    } else {
        return (
            <SubMenuRow
                name={child.name}
                link={child.link}
            />
        )
    }
}

export default MenuRow
