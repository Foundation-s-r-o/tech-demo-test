import React from 'react'
import {useTranslation} from 'react-i18next'
import AppContent from '@components/layout/AppContent'

export function HomePage() {
    const { t } = useTranslation()

    return (
        <AppContent title={t('component.title.homepage')}>
            Homepage
        </AppContent>
    )
}
