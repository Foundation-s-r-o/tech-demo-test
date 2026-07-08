import classNames from 'classnames'
import React, { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation } from 'react-router-dom'
import menuTree from '../menu/AppMenuTree'
import { MenuGroupType } from '../menu/types'
import { Role } from '@common/auth/Role'
import { useAuth } from '@common/auth/useAuth'
import { useSidebar } from './SidebarContext'
import SidebarGroup from './SidebarGroup'
import './Sidebar.scss'

const groupContainsPath = (group: MenuGroupType, pathname: string) =>
    group.children.some((item) => item.link === pathname)

const visibleFor = (role: Role | undefined, roles?: Role[]) =>
    !roles || roles.length === 0 || (role !== undefined && roles.includes(role))

const AppSidebar = () => {
    const { t } = useTranslation()
    const location = useLocation()
    const { user } = useAuth()
    const { collapsed, mobileOpen, closeMobile, expand } = useSidebar()

    const role = user?.role
    // Show only the groups/items the current role may reach (enforcement stays server-side).
    const items = useMemo<MenuGroupType[]>(() =>
        menuTree.items
            .map((group) => ({
                ...group,
                children: group.children.filter((item) => visibleFor(role, item.roles)),
            }))
            .filter((group) => visibleFor(role, group.roles) && group.children.length > 0),
    [role])

    // Groups containing the active route start (and stay) open.
    const [openGroups, setOpenGroups] = useState<string[]>(() =>
        items
            .filter((group) => groupContainsPath(group, location.pathname))
            .map((group) => group.key)
    )

    useEffect(() => {
        closeMobile()
        const active = items.find((group) =>
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
                    {items.map((group) => (
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
