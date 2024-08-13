import React, { useEffect, useState } from 'react'
import { Formik, Form } from 'formik'
import { PersonModifyRequestDTO } from '@api/generated'
import { Container, Row } from 'react-bootstrap'
import InputSection from '@components/shared/forms/InputSection'
import FndtInputSegment from '@components/form/InputSegment'
import { FndtInputType } from '@components/form/types'
import { FndtButton } from '@components/shared/buttons/FndtButton'
import { useTranslation } from 'react-i18next'
import personFormSchema from '@pages/persons/validation'
import { personControllerApi } from '@api/api'
import { useNavigate } from 'react-router-dom'
import routes from '@components/router/routes'

export default function PersonForm({ id = null }: { id?: number }) {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const idPrefix = 'PersonEditForm'
    const [defaultValues, setDefaultValues] = useState<PersonModifyRequestDTO>({
        address: '',
        email: '',
        firstName: '',
        lastName: '',
        phoneNumber: '',
        state: ''
    })
    const onSubmit = async (values: PersonModifyRequestDTO) => {
        if (id) {
            await personControllerApi.update({
                id: id,
                personModifyRequestDTO: values
            })
        } else {
            const resp = await personControllerApi.create({ personModifyRequestDTO: values })
            navigate(routes.personsEdit.path.replace(':id', String(resp.data.id)))
        }
    }

    useEffect(() => {

        if (id) {
            const load = async () => {
                const response = await personControllerApi.get({ id: id })
                const person = response.data
                setDefaultValues({
                    firstName: person.firstName,
                    lastName: person.lastName,
                    address: person.address,
                    email: person.email,
                    phoneNumber: person.phoneNumber || '',
                    state: person.state || ''
                })
            }

            load()
        }
    }, [id])

    return (
        <Formik
            validationSchema={personFormSchema}
            initialValues={defaultValues}
            enableReinitialize={true}
            onSubmit={onSubmit}>
            {({ resetForm }) => (
                <Form>
                    <Container>
                        <Row>
                            <InputSection>
                                <FndtInputSegment
                                    type={FndtInputType.Input}
                                    idPrefix={idPrefix}
                                    label="form.input.firstName"
                                    name="firstName"
                                />
                                <FndtInputSegment
                                    type={FndtInputType.Input}
                                    idPrefix={idPrefix}
                                    label="form.input.lastName"
                                    name="lastName"
                                />
                                <FndtInputSegment
                                    type={FndtInputType.Input}
                                    idPrefix={idPrefix}
                                    label="form.input.email"
                                    name="email"
                                />
                                <FndtInputSegment
                                    type={FndtInputType.Input}
                                    idPrefix={idPrefix}
                                    label="form.input.address"
                                    name="address"
                                />
                                <FndtInputSegment
                                    type={FndtInputType.Input}
                                    idPrefix={idPrefix}
                                    label="form.input.phone"
                                    name="phoneNumber"
                                />
                                <FndtInputSegment
                                    type={FndtInputType.Input}
                                    idPrefix={idPrefix}
                                    label="form.input.state"
                                    name="state"
                                />
                            </InputSection>
                        </Row>
                        <Row>
                            <div className="mt-3">
                                <FndtButton
                                    variant="primary"
                                    className="me-2"
                                    type="submit">
                                    {t('common.btn.save')}
                                </FndtButton>
                                <FndtButton
                                    variant="secondary"
                                    onClick={() => {
                                        resetForm()
                                    }}>
                                    {t('common.btn.resetToDefaultValues')}
                                </FndtButton>
                            </div>
                        </Row>
                    </Container>
                </Form>
            )}
        </Formik>
    )
}
