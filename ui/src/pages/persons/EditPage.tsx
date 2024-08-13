import React from 'react'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import PersonForm from '@pages/persons/Form'
import { useParams } from 'react-router-dom'

export default function PersonsEditPage() {
    const { id } = useParams<'id'>()
    const { t } = useTranslation()

    return (
        <AppContent title={t('component.persons.edit')}>
            <PersonForm id={Number(id)}/>
        </AppContent>
    )
}
