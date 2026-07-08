import React, { useState } from 'react'
import { Formik, Form } from 'formik'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import InputSection from '@components/shared/forms/InputSection'
import FndtInputSegment from '@components/form/InputSegment'
import { FndtInputType } from '@components/form/types'
import { FndtButton } from '@components/shared/buttons/FndtButton'
import { marriageControllerApi } from '@api/api'
import { MarriageRequestDTO } from '@api/generated'
import { useModals } from '@components/modals/useModals'
import ErrorModal from '@components/shared/alert-modals/ErrorModal'

const idPrefix = 'MarriageForm'

const initialValues: MarriageRequestDTO = {
    officeCode: '',
    bookYear: new Date().getFullYear(),
    entryNo: 1,
    spouse1Uid: '',
    spouse2Uid: '',
    marriageDate: '',
    marriageForm: 'civil',
}

export default function MarriagePage() {
    const { t } = useTranslation()
    const { showModal } = useModals()
    const [recordId, setRecordId] = useState<string | null>(null)
    const [dissolveId, setDissolveId] = useState('')

    const onSubmit = async (values: MarriageRequestDTO) => {
        try {
            const resp = await marriageControllerApi.registerMarriage({ marriageRequestDTO: values })
            setRecordId(resp.data.recordId ?? null)
        } catch {
            showModal(<ErrorModal text="common.error.generic" />)
        }
    }

    const dissolve = () => {
        marriageControllerApi.dissolve({
            id: dissolveId,
            dissolutionRequestDTO: { dissolutionType: 'divorce', effectiveDate: new Date().toISOString().slice(0, 10) },
        }).catch(() => showModal(<ErrorModal text="common.error.generic" />))
    }

    return (
        <AppContent title={t('component.records.marriage')}>
            {recordId && (
                <div id="marriage_created" className="alert alert-success">
                    {t('component.records.created')}: {recordId}
                </div>
            )}
            <Formik initialValues={initialValues} onSubmit={onSubmit}>
                <Form>
                    <InputSection title={t('component.records.marriage')}>
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="component.records.officeCode" name="officeCode" />
                        <FndtInputSegment type={FndtInputType.InputNumber} idPrefix={idPrefix}
                            label="component.records.bookYear" name="bookYear" />
                        <FndtInputSegment type={FndtInputType.InputNumber} idPrefix={idPrefix}
                            label="component.records.entryNo" name="entryNo" />
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="component.records.spouse1" name="spouse1Uid" />
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="component.records.spouse2" name="spouse2Uid" />
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="component.records.marriageDate" name="marriageDate" />
                    </InputSection>
                    <FndtButton className="mt-3" variant="primary" type="submit">
                        {t('common.btn.save')}
                    </FndtButton>
                </Form>
            </Formik>

            <h3 className="mt-4">{t('component.records.dissolution')}</h3>
            <div className="d-flex gap-2">
                <input
                    id="marriage_dissolve_id"
                    className="form-control"
                    placeholder={t('component.records.recordId')}
                    value={dissolveId}
                    onChange={(e) => setDissolveId(e.target.value)} />
                <FndtButton id="marriage_dissolve" variant="secondary" onClick={dissolve}>
                    {t('common.btn.confirm')}
                </FndtButton>
            </div>
        </AppContent>
    )
}
