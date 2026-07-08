import React, { useState } from 'react'
import { Formik, Form } from 'formik'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import InputSection from '@components/shared/forms/InputSection'
import FndtInputSegment from '@components/form/InputSegment'
import { FndtInputType } from '@components/form/types'
import { FndtButton } from '@components/shared/buttons/FndtButton'
import { deathControllerApi } from '@api/api'
import { DeathRequestDTO } from '@api/generated'
import { useModals } from '@components/modals/useModals'
import ErrorModal from '@components/shared/alert-modals/ErrorModal'

const idPrefix = 'DeathForm'

const initialValues: DeathRequestDTO = {
    officeCode: '',
    bookYear: new Date().getFullYear(),
    entryNo: 1,
    personUid: '',
    dateOfDeath: '',
    declaredByCourt: false,
}

export default function DeathPage() {
    const { t } = useTranslation()
    const { showModal } = useModals()
    const [recordId, setRecordId] = useState<string | null>(null)
    const [medicalId, setMedicalId] = useState('')
    const [cause, setCause] = useState('')

    const onSubmit = async (values: DeathRequestDTO) => {
        try {
            const resp = await deathControllerApi.registerDeath({ deathRequestDTO: values })
            setRecordId(resp.data.recordId ?? null)
        } catch {
            showModal(<ErrorModal text="common.error.generic" />)
        }
    }

    const saveMedical = () => {
        deathControllerApi.setMedical({
            id: medicalId,
            medicalRequestDTO: { causeOfDeathIcd10: cause },
        }).catch(() => showModal(<ErrorModal text="common.error.generic" />))
    }

    return (
        <AppContent title={t('component.records.death')}>
            {recordId && (
                <div id="death_created" className="alert alert-success">
                    {t('component.records.created')}: {recordId}
                </div>
            )}
            <Formik initialValues={initialValues} onSubmit={onSubmit}>
                <Form>
                    <InputSection title={t('component.records.death')}>
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="component.records.officeCode" name="officeCode" />
                        <FndtInputSegment type={FndtInputType.InputNumber} idPrefix={idPrefix}
                            label="component.records.bookYear" name="bookYear" />
                        <FndtInputSegment type={FndtInputType.InputNumber} idPrefix={idPrefix}
                            label="component.records.entryNo" name="entryNo" />
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="form.input.uid" name="personUid" />
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="component.records.dateOfDeath" name="dateOfDeath" />
                    </InputSection>
                    <FndtButton className="mt-3" variant="primary" type="submit">
                        {t('common.btn.save')}
                    </FndtButton>
                </Form>
            </Formik>

            <h3 className="mt-4">{t('component.records.medical')}</h3>
            <div className="d-flex gap-2">
                <input
                    id="death_medical_id"
                    className="form-control"
                    placeholder={t('component.records.recordId')}
                    value={medicalId}
                    onChange={(e) => setMedicalId(e.target.value)} />
                <input
                    id="death_medical_cause"
                    className="form-control"
                    placeholder={t('component.records.cause')}
                    value={cause}
                    onChange={(e) => setCause(e.target.value)} />
                <FndtButton id="death_medical_save" variant="secondary" onClick={saveMedical}>
                    {t('common.btn.confirm')}
                </FndtButton>
            </div>
        </AppContent>
    )
}
