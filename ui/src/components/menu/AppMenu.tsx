import React, { useEffect, useState } from 'react'
import { Accordion, Offcanvas } from 'react-bootstrap'
import { useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import menuTree from './AppMenuTree'
import MenuRow from './MenuRow'
import './Menu.scss'
import MenuCollapse from './MenuCollapse'

const AppMenu = () => {
    const [show, setShow] = useState(false)
    const location = useLocation()
    const { t } = useTranslation()

    useEffect(() => {
        window.scrollTo(0, 0)
        handleClose()
    }, [location])

    const handleClose = () => setShow(false)
    const handleShow = () => setShow(true)
    return (
        <div className="d-flex align-items-center">
            <button
                className="bi-list menu-trigger"
                onClick={handleShow}
            />

            <Offcanvas
                show={show}
                onHide={handleClose}
                className="menu-offcanvas">
                <Offcanvas.Header closeButton>
                    <Offcanvas.Title>{t('common.layout.menu')}</Offcanvas.Title>
                </Offcanvas.Header>
                <Offcanvas.Body>
                    <Accordion>
                        {menuTree.items.map((item, i) => (
                            <div key={item.name.toString()}>
                                <div className="menu-row-item">
                                    <MenuCollapse eventKey={`${i}`}>
                                        {item.name}{' '}
                                    </MenuCollapse>
                                </div>
                                <Accordion.Collapse eventKey={`${i}`}>
                                    <div>
                                        {item.children.map((row, iter) => (
                                            <MenuRow
                                                key={`a${iter}`}
                                                child={row}
                                                iter={iter}
                                            />
                                        ))}
                                    </div>
                                </Accordion.Collapse>
                            </div>
                        ))}
                    </Accordion>
                </Offcanvas.Body>
            </Offcanvas>
        </div>
    )
}

export default AppMenu
