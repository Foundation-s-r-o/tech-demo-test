import React from 'react'
import {useTranslation} from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import PersonsTable from '@pages/persons/Table'

export default function PersonsListPage() {
    const { t } = useTranslation()

    return (
        <AppContent title={t('component.persons.title')}>
            <PersonsTable />
        </AppContent>
    )
}
