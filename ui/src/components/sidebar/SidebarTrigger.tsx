import { Bars3Icon } from '@heroicons/react/24/outline'
import React from 'react'
import { useTranslation } from 'react-i18next'
import { useSidebar } from './SidebarContext'

const SidebarTrigger = () => {
    const { t } = useTranslation()
    const { collapsed, mobileOpen, toggleSidebar } = useSidebar()

    const label = collapsed
        ? t('common.layout.sidebarExpand')
        : t('common.layout.sidebarCollapse')

    return (
        <button
            className="menu-trigger"
            aria-label={label}
            title={label}
            aria-expanded={mobileOpen || !collapsed}
            onClick={toggleSidebar}>
            <Bars3Icon aria-hidden="true" />
        </button>
    )
}

export default SidebarTrigger
