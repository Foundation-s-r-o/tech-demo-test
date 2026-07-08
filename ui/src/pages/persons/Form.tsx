import React from 'react'
import { Formik, Form } from 'formik'
import { PersonCreateRequestDTO } from '@api/generated'
import InputSection from '@components/shared/forms/InputSection'
import FndtInputSegment from '@components/form/InputSegment'
import { FndtInputType } from '@components/form/types'
import { FndtButton } from '@components/shared/buttons/FndtButton'
import { useTranslation } from 'react-i18next'
import personFormSchema from '@pages/persons/validation'
import { personControllerApi } from '@api/api'
import { useNavigate } from 'react-router-dom'
import routes from '@components/router/routes'
import { useModals } from '@components/modals/useModals'
import ErrorModal from '@components/shared/alert-modals/ErrorModal'

const idPrefix = 'PersonCreateForm'

const initialValues: PersonCreateRequestDTO = {
    givenNames: '',
    familyName: '',
    birthSurname: '',
    sex: '',
    dateOfBirth: '',
    citizenship: '',
    nationalId: '',
}

export default function PersonForm() {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const { showModal } = useModals()

    const onSubmit = async (values: PersonCreateRequestDTO) => {
        try {
            const resp = await personControllerApi.create({ personCreateRequestDTO: values })
            navigate(routes.personsDetail.path.replace(':uid', String(resp.data.uid ?? '')))
        } catch {
            // Server rejected the write (e.g. 403 for non-clerk roles, validation).
            showModal(<ErrorModal text="common.error.generic" />)
        }
    }

    return (
        <Formik
            validationSchema={personFormSchema}
            initialValues={initialValues}
            onSubmit={onSubmit}>
            {({ resetForm }) => (
                <Form>
                    <InputSection title={t('form.section.personalData')}>
                        <FndtInputSegment
                            type={FndtInputType.Input}
                            idPrefix={idPrefix}
                            label="form.input.givenNames"
                            name="givenNames"
                        />
                        <FndtInputSegment
                            type={FndtInputType.Input}
                            idPrefix={idPrefix}
                            label="form.input.familyName"
                            name="familyName"
                        />
                        <FndtInputSegment
                            type={FndtInputType.Input}
                            idPrefix={idPrefix}
                            label="form.input.birthSurname"
                            name="birthSurname"
                        />
                        <FndtInputSegment
                            type={FndtInputType.Input}
                            idPrefix={idPrefix}
                            label="form.input.sex"
                            name="sex"
                        />
                        <FndtInputSegment
                            type={FndtInputType.Input}
                            idPrefix={idPrefix}
                            label="form.input.dateOfBirth"
                            name="dateOfBirth"
                        />
                        <FndtInputSegment
                            type={FndtInputType.Input}
                            idPrefix={idPrefix}
                            label="form.input.citizenship"
                            name="citizenship"
                        />
                        <FndtInputSegment
                            type={FndtInputType.Input}
                            idPrefix={idPrefix}
                            label="form.input.nationalId"
                            name="nationalId"
                        />
                    </InputSection>
                    <div className="mt-3">
                        <FndtButton
                            variant="primary"
                            className="me-2"
                            type="submit">
                            {t('common.btn.save')}
                        </FndtButton>
                        <FndtButton
                            variant="secondary"
                            onClick={() => resetForm()}>
                            {t('common.btn.resetToDefaultValues')}
                        </FndtButton>
                    </div>
                </Form>
            )}
        </Formik>
    )
}
