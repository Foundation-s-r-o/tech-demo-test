import classNames from 'classnames'
import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation } from 'react-router-dom'
import menuTree from '../menu/AppMenuTree'
import { MenuGroupType } from '../menu/types'
import { useSidebar } from './SidebarContext'
import SidebarGroup from './SidebarGroup'
import './Sidebar.scss'

const groupContainsPath = (group: MenuGroupType, pathname: string) =>
    group.children.some((item) => item.link === pathname)

const AppSidebar = () => {
    const { t } = useTranslation()
    const location = useLocation()
    const { collapsed, mobileOpen, closeMobile, expand } = useSidebar()

    // Groups containing the active route start (and stay) open.
    const [openGroups, setOpenGroups] = useState<string[]>(() =>
        menuTree.items
            .filter((group) => groupContainsPath(group, location.pathname))
            .map((group) => group.key)
    )

    useEffect(() => {
        closeMobile()
        const active = menuTree.items.find((group) =>
            groupContainsPath(group, location.pathname)
        )
        if (active) {
            setOpenGroups((prev) =>
                prev.includes(active.key) ? prev : [...prev, active.key]
            )
        }
    }, [location])

    useEffect(() => {
        if (!mobileOpen) {
            return
        }
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                closeMobile()
            }
        }
        document.addEventListener('keydown', onKeyDown)
        return () => document.removeEventListener('keydown', onKeyDown)
    }, [mobileOpen])

    // In the desktop rail state groups cannot unfold — clicking one expands
    // the sidebar and opens the group. On mobile the rail state is ignored.
    const railMode = collapsed && !mobileOpen

    const toggleGroup = (key: string) => {
        if (railMode) {
            expand()
            setOpenGroups((prev) =>
                prev.includes(key) ? prev : [...prev, key]
            )
            return
        }
        setOpenGroups((prev) =>
            prev.includes(key)
                ? prev.filter((openKey) => openKey !== key)
                : [...prev, key]
        )
    }

    return (
        <>
            {mobileOpen && (
                <button
                    type="button"
                    className="sidebar-backdrop"
                    aria-label={t('common.btn.close')}
                    tabIndex={-1}
                    onClick={closeMobile} />
            )}
            <nav
                id="app_sidebar"
                aria-label={t('common.layout.menu')}
                className={classNames('app-sidebar', {
                    'app-sidebar--collapsed': collapsed,
                    'app-sidebar--mobile-open': mobileOpen,
                })}>
                <ul className="sidebar-nav">
                    {menuTree.items.map((group) => (
                        <SidebarGroup
                            key={group.key}
                            group={group}
                            open={openGroups.includes(group.key) && !railMode}
                            onToggle={() => toggleGroup(group.key)}
                        />
                    ))}
                </ul>
            </nav>
        </>
    )
}

export default AppSidebar
