import classNames from 'classnames'
import React from 'react'
import { NavLink } from 'react-router-dom'
import { MenuItemType } from '../menu/types'
import FndtIcon from '../shared/icons/FndtIcon'

type SidebarItemProps = {
    item: MenuItemType
}

const SidebarItem = ({ item }: SidebarItemProps) => {
    return (
        <li>
            <NavLink
                to={item.link}
                end
                title={item.name}
                className={({ isActive }) =>
                    classNames('sidebar-item', {
                        'sidebar-item--active': isActive,
                    })
                }>
                {item.icon && <FndtIcon icon={item.icon} />}
                <span className="sidebar-label">{item.name}</span>
            </NavLink>
        </li>
    )
}

export default SidebarItem
