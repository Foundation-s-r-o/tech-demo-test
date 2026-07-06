import React from 'react'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import PersonForm from '@pages/persons/Form'
import routes from '@components/router/routes'

export default function PersonsAddPage() {
    const { t } = useTranslation()

    return (
        <AppContent
            title={t('component.persons.add')}
            breadcrumbs={[
                {
                    label: t('component.persons.title'),
                    to: routes.personsList.path,
                },
            ]}>
            <PersonForm />
        </AppContent>
    )
}
