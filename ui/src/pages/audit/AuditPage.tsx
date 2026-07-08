import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import { FndtButton } from '@components/shared/buttons/FndtButton'
import { auditControllerApi } from '@api/api'
import { AccessLogEntryDTO, AuditEventDTO, ChainVerificationResult } from '@api/generated'

export default function AuditPage() {
    const { t } = useTranslation()
    const [events, setEvents] = useState<AuditEventDTO[]>([])
    const [chain, setChain] = useState<ChainVerificationResult | null>(null)
    const [personUid, setPersonUid] = useState('')
    const [accessLog, setAccessLog] = useState<AccessLogEntryDTO[]>([])

    useEffect(() => {
        auditControllerApi.events().then((r) => setEvents(r.data))
    }, [])

    const verifyChain = () => {
        auditControllerApi.chainVerification().then((r) => setChain(r.data))
    }

    const loadAccessLog = () => {
        if (!personUid) {
            return
        }
        auditControllerApi.auditAccessLog({ personUid }).then((r) => setAccessLog(r.data))
    }

    return (
        <AppContent title={t('component.audit.title')}>
            <h3>{t('component.audit.chain')}</h3>
            <FndtButton id="audit_verify_chain" variant="primary" onClick={verifyChain}>
                {t('component.audit.verify')}
            </FndtButton>
            {chain && (
                <p id="audit_chain_result" className="mt-2">
                    {chain.intact ? t('component.audit.chainIntact') : t('component.audit.chainBroken')}
                    {' '}({chain.entriesChecked} {t('component.audit.entries')})
                </p>
            )}

            <h3 className="mt-4">{t('component.audit.events')}</h3>
            <table id="audit_events" className="table table-sm">
                <thead>
                    <tr>
                        <th>{t('component.audit.time')}</th>
                        <th>{t('component.audit.actor')}</th>
                        <th>{t('component.audit.action')}</th>
                        <th>{t('component.audit.table')}</th>
                    </tr>
                </thead>
                <tbody>
                    {events.length === 0 ? (
                        <tr><td colSpan={4}>{t('common.table.noResults')}</td></tr>
                    ) : events.map((event) => (
                        <tr key={event.auditId}>
                            <td>{event.eventTime}</td>
                            <td>{event.actor}</td>
                            <td>{event.action}</td>
                            <td>{event.schemaName}.{event.tableName}</td>
                        </tr>
                    ))}
                </tbody>
            </table>

            <h3 className="mt-4">{t('component.audit.accessLog')}</h3>
            <div className="d-flex gap-2 mb-2">
                <input
                    id="audit_access_uid"
                    className="form-control"
                    placeholder={t('form.input.uid')}
                    value={personUid}
                    onChange={(e) => setPersonUid(e.target.value)} />
                <FndtButton id="audit_access_load" variant="secondary" onClick={loadAccessLog}>
                    {t('common.btn.search')}
                </FndtButton>
            </div>
            <table id="audit_access_log" className="table table-sm">
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
