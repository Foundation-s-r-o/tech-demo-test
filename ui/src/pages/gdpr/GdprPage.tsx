import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import { FndtButton } from '@components/shared/buttons/FndtButton'
import { gdprControllerApi } from '@api/api'
import {
    ConsentResponseDTO,
    DisclosureResponseDTO,
    DsrResponseDTO
} from '@api/generated'

export default function GdprPage() {
    const { t } = useTranslation()
    const [personUid, setPersonUid] = useState('')
    const [requests, setRequests] = useState<DsrResponseDTO[]>([])
    const [consents, setConsents] = useState<ConsentResponseDTO[]>([])
    const [disclosures, setDisclosures] = useState<DisclosureResponseDTO[]>([])
    const [requestType, setRequestType] = useState('access')
    const [requesterRef, setRequesterRef] = useState('')
    const [consentPurpose, setConsentPurpose] = useState('')

    const load = () => {
        if (!personUid) {
            return
        }
        gdprControllerApi.listRequests({ personUid }).then((r) => setRequests(r.data))
        gdprControllerApi.listConsents({ personUid }).then((r) => setConsents(r.data))
        gdprControllerApi.listDisclosures({ personUid }).then((r) => setDisclosures(r.data))
    }

    const fileRequest = () => {
        gdprControllerApi.createRequest({
            dsrCreateRequestDTO: { personUid, requesterRef, requestType },
        }).then(load)
    }

    const recordConsent = () => {
        gdprControllerApi.createConsent({
            consentCreateRequestDTO: { personUid, purpose: consentPurpose, givenVia: 'in_person' },
        }).then(load)
    }

    const withdraw = (id?: number) => {
        if (id === undefined) {
            return
        }
        gdprControllerApi.withdrawConsent({ id }).then(load)
    }

    const exportData = () => {
        gdprControllerApi._export({ uid: personUid }).then(load)
    }

    return (
        <AppContent title={t('component.gdpr.title')}>
            <div className="d-flex gap-2 mb-3">
                <input
                    id="gdpr_person_uid"
                    className="form-control"
                    placeholder={t('form.input.uid')}
                    value={personUid}
                    onChange={(e) => setPersonUid(e.target.value)} />
                <FndtButton id="gdpr_load" variant="primary" onClick={load}>
                    {t('common.btn.search')}
                </FndtButton>
                <FndtButton id="gdpr_export" variant="secondary" onClick={exportData}>
                    {t('component.gdpr.export')}
                </FndtButton>
            </div>

            <h3>{t('component.gdpr.requests')}</h3>
            <div className="d-flex gap-2 mb-2">
                <input
                    id="gdpr_dsr_ref"
                    className="form-control"
                    placeholder={t('component.gdpr.requesterRef')}
                    value={requesterRef}
                    onChange={(e) => setRequesterRef(e.target.value)} />
                <input
                    id="gdpr_dsr_type"
                    className="form-control"
                    value={requestType}
                    onChange={(e) => setRequestType(e.target.value)} />
                <FndtButton id="gdpr_dsr_create" variant="secondary" onClick={fileRequest}>
                    {t('common.btn.confirm')}
                </FndtButton>
            </div>
            <ul id="gdpr_requests">
                {requests.map((request) => (
                    <li key={request.requestId}>
                        {request.requestType} — {request.status} ({t('component.gdpr.due')}: {request.dueAt})
                    </li>
                ))}
            </ul>

            <h3 className="mt-3">{t('component.gdpr.consents')}</h3>
            <div className="d-flex gap-2 mb-2">
                <input
                    id="gdpr_consent_purpose"
                    className="form-control"
                    placeholder={t('component.gdpr.purpose')}
                    value={consentPurpose}
                    onChange={(e) => setConsentPurpose(e.target.value)} />
                <FndtButton id="gdpr_consent_create" variant="secondary" onClick={recordConsent}>
                    {t('common.btn.confirm')}
                </FndtButton>
            </div>
            <ul id="gdpr_consents">
                {consents.map((consent) => (
                    <li key={consent.consentId}>
                        {consent.purpose} {consent.withdrawnAt ? `(${t('component.gdpr.withdrawn')})` : (
                            <FndtButton
                                id={`gdpr_consent_withdraw_${consent.consentId}`}
                                variant="secondary"
                                onClick={() => withdraw(consent.consentId)}>
                                {t('component.gdpr.withdraw')}
                            </FndtButton>
                        )}
                    </li>
                ))}
            </ul>

            <h3 className="mt-3">{t('component.gdpr.disclosures')}</h3>
            <ul id="gdpr_disclosures">
                {disclosures.map((disclosure) => (
                    <li key={disclosure.disclosureId}>
                        {disclosure.recipient} — {disclosure.disclosedFields}
                    </li>
                ))}
            </ul>
        </AppContent>
    )
}
