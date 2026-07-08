import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import { meControllerApi } from '@api/api'
import { AccessLogEntryDTO, PersonCompositeResponseDTO } from '@api/generated'

export default function MePage() {
    const { t } = useTranslation()
    const [person, setPerson] = useState<PersonCompositeResponseDTO | null>(null)
    const [accessLog, setAccessLog] = useState<AccessLogEntryDTO[]>([])

    useEffect(() => {
        // Fetch the access log only after the composite read resolves, so the citizen sees the
        // access-log entry that their own /me read just created.
        meControllerApi
            .me({ xAccessPurpose: 'self-service' })
            .then((r) => {
                setPerson(r.data)
                return meControllerApi.myAccessLog()
            })
            .then((r) => setAccessLog(r.data))
    }, [])

    const detail = person?.detail

    return (
        <AppContent title={t('component.me.title')}>
            <h3>{t('component.me.myData')}</h3>
            <dl id="me_detail" className="row">
                <dt className="col-sm-3">{t('form.input.givenNames')}</dt>
                <dd className="col-sm-9">{detail?.givenNames}</dd>
                <dt className="col-sm-3">{t('form.input.familyName')}</dt>
                <dd className="col-sm-9">{detail?.familyName}</dd>
                <dt className="col-sm-3">{t('form.input.dateOfBirth')}</dt>
                <dd className="col-sm-9">{detail?.dateOfBirth}</dd>
                <dt className="col-sm-3">{t('form.input.citizenship')}</dt>
                <dd className="col-sm-9">{detail?.citizenship}</dd>
            </dl>

            <h3 className="mt-4">{t('component.me.accessLog')}</h3>
            <table id="me_access_log" className="table table-sm">
                <thead>
                    <tr>
                        <th>{t('component.audit.accessedAt')}</th>
                        <th>{t('component.audit.actor')}</th>
                        <th>{t('component.audit.purpose')}</th>
                    </tr>
                </thead>
                <tbody>
                    {accessLog.length === 0 ? (
                        <tr><td colSpan={3}>{t('common.table.noResults')}</td></tr>
                    ) : accessLog.map((entry) => (
                        <tr key={entry.accessId}>
                            <td>{entry.accessedAt}</td>
                            <td>{entry.actor}</td>
                            <td>{entry.purpose}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </AppContent>
    )
}
