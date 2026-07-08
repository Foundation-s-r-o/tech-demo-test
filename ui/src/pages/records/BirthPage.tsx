import React, { useState } from 'react'
import { Formik, Form } from 'formik'
import { useTranslation } from 'react-i18next'
import AppContent from '@components/layout/AppContent'
import InputSection from '@components/shared/forms/InputSection'
import FndtInputSegment from '@components/form/InputSegment'
import { FndtInputType } from '@components/form/types'
import { FndtButton } from '@components/shared/buttons/FndtButton'
import { birthControllerApi } from '@api/api'
import { BirthRequestDTO } from '@api/generated'
import { useModals } from '@components/modals/useModals'
import ErrorModal from '@components/shared/alert-modals/ErrorModal'

const idPrefix = 'BirthForm'

const initialValues: BirthRequestDTO = {
    officeCode: '',
    bookYear: new Date().getFullYear(),
    entryNo: 1,
    childUid: '',
    dateOfBirth: '',
}

export default function BirthPage() {
    const { t } = useTranslation()
    const { showModal } = useModals()
    const [recordId, setRecordId] = useState<string | null>(null)

    const onSubmit = async (values: BirthRequestDTO) => {
        try {
            const resp = await birthControllerApi.registerBirth({ birthRequestDTO: values })
            setRecordId(resp.data.recordId ?? null)
        } catch {
            showModal(<ErrorModal text="common.error.generic" />)
        }
    }

    return (
        <AppContent title={t('component.records.birth')}>
            {recordId && (
                <div id="birth_created" className="alert alert-success">
                    {t('component.records.created')}: {recordId}
                </div>
            )}
            <Formik initialValues={initialValues} onSubmit={onSubmit}>
                <Form>
                    <InputSection title={t('component.records.birth')}>
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="component.records.officeCode" name="officeCode" />
                        <FndtInputSegment type={FndtInputType.InputNumber} idPrefix={idPrefix}
                            label="component.records.bookYear" name="bookYear" />
                        <FndtInputSegment type={FndtInputType.InputNumber} idPrefix={idPrefix}
                            label="component.records.entryNo" name="entryNo" />
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="component.records.childUid" name="childUid" />
                        <FndtInputSegment type={FndtInputType.Input} idPrefix={idPrefix}
                            label="form.input.dateOfBirth" name="dateOfBirth" />
                    </InputSection>
                    <FndtButton className="mt-3" variant="primary" type="submit">
                        {t('common.btn.save')}
                    </FndtButton>
                </Form>
            </Formik>
        </AppContent>
    )
}
