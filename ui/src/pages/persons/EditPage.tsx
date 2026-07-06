import React from 'react'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import PersonForm from '@pages/persons/Form'
import routes from '@components/router/routes'
import { useParams } from 'react-router-dom'

export default function PersonsEditPage() {
    const { id } = useParams<'id'>()
    const { t } = useTranslation()

    return (
        <AppContent
            title={t('component.persons.edit')}
            breadcrumbs={[
                {
                    label: t('component.persons.title'),
                    to: routes.personsList.path,
                },
            ]}>
            <PersonForm id={Number(id)}/>
        </AppContent>
    )
}
