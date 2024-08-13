import React from 'react'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import PersonForm from '@pages/persons/Form'

export default function PersonsAddPage() {
    const { t } = useTranslation()

    return (
        <AppContent title={t('component.persons.add')}>
            <PersonForm />
        </AppContent>
    )
}
