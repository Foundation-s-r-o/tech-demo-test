import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useParams } from 'react-router-dom'
import AppContent from '@components/layout/AppContent'
import routes from '@components/router/routes'
import { personControllerApi } from '@api/api'
import { PersonCompositeResponseDTO } from '@api/generated'
import { useModals } from '@components/modals/useModals'
import ErrorModal from '@components/shared/alert-modals/ErrorModal'

export default function PersonDetailPage() {
    const { uid } = useParams<'uid'>()
    const { t } = useTranslation()
    const { showModal } = useModals()
    const [person, setPerson] = useState<PersonCompositeResponseDTO | null>(null)

    useEffect(() => {
        if (!uid) {
            return
        }
        personControllerApi
            .getPerson({ uid, xAccessPurpose: 'registry-detail' })
            .then((response) => setPerson(response.data))
            .catch(() => showModal(<ErrorModal text="common.error.generic" />))
    }, [uid])

    const detail = person?.detail

    return (
        <AppContent
            title={t('component.persons.detail')}
            breadcrumbs={[{ label: t('component.persons.title'), to: routes.personsList.path }]}>
            {!person ? (
                <p>{t('common.table.noResults')}</p>
            ) : (
                <dl id="person_detail" className="row">
                    <dt className="col-sm-3">{t('form.input.uid')}</dt>
                    <dd className="col-sm-9">{person.uid}</dd>
                    <dt className="col-sm-3">{t('form.input.givenNames')}</dt>
                    <dd className="col-sm-9">{detail?.givenNames}</dd>
                    <dt className="col-sm-3">{t('form.input.familyName')}</dt>
                    <dd className="col-sm-9">{detail?.familyName}</dd>
                    <dt className="col-sm-3">{t('form.input.sex')}</dt>
                    <dd className="col-sm-9">{detail?.sex}</dd>
                    <dt className="col-sm-3">{t('form.input.dateOfBirth')}</dt>
                    <dd className="col-sm-9">{detail?.dateOfBirth}</dd>
                    <dt className="col-sm-3">{t('form.input.citizenship')}</dt>
                    <dd className="col-sm-9">{detail?.citizenship}</dd>
                    <dt className="col-sm-3">{t('component.persons.deceased')}</dt>
                    <dd className="col-sm-9">{person.deceased ? t('common.yes') : '—'}</dd>
                    <dt className="col-sm-3">{t('component.persons.addresses')}</dt>
                    <dd className="col-sm-9">
                        {(person.addresses ?? []).length === 0
                            ? '—'
                            : (person.addresses ?? []).map((address) => (
                                <div key={address.personAddressId}>
                                    {address.addressType}: {address.street} {address.houseNumber}
                                    {address.foreignAddress}
                                </div>
                            ))}
                    </dd>
                </dl>
            )}
        </AppContent>
    )
}
