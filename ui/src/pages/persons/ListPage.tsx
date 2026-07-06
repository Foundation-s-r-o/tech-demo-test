import React from 'react'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import PersonsTable from '@pages/persons/Table'
import { FndtButton } from '@components/shared/buttons/FndtButton'
import routes from '@components/router/routes'

export default function PersonsListPage() {
    const { t } = useTranslation()

    return (
        <AppContent
            title={t('component.persons.title')}
            actions={
                <FndtButton
                    id="persons_add_action"
                    to={routes.personsAdd.path}>
                    {t('component.persons.add')}
                </FndtButton>
            }>
            <PersonsTable />
        </AppContent>
    )
}
