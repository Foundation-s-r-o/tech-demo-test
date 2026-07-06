import { ChevronDownIcon, ChevronRightIcon } from '@heroicons/react/24/outline'
import React from 'react'
import { MenuGroupType } from '../menu/types'
import FndtIcon from '../shared/icons/FndtIcon'
import SidebarItem from './SidebarItem'

type SidebarGroupProps = {
    group: MenuGroupType
    open: boolean
    onToggle: () => void
}

const SidebarGroup = ({ group, open, onToggle }: SidebarGroupProps) => {
    return (
        <li className="sidebar-group">
            <button
                type="button"
                className="sidebar-group-toggle"
                aria-expanded={open}
                title={group.name}
                onClick={onToggle}>
                {group.icon && <FndtIcon icon={group.icon} />}
                <span className="sidebar-label">{group.name}</span>
                <FndtIcon
                    icon={open ? ChevronDownIcon : ChevronRightIcon}
                    size="sm"
                    className="sidebar-group-chevron" />
            </button>
            {open && (
                <ul className="sidebar-group-items">
                    {group.children.map((item) => (
                        <SidebarItem
                            key={item.link}
                            item={item} />
                    ))}
                </ul>
            )}
        </li>
    )
}

export default SidebarGroup
