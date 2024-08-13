import { Field, Form, Formik } from 'formik'
import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../common/auth/useAuth'
import { useModals } from '../../components/modals/useModals'
import ErrorModal from '../../components/shared/alert-modals/ErrorModal'
import { FndtButton } from '../../components/shared/buttons/FndtButton'
import FndtFormErrorMessage from '../../components/shared/forms/ErrorMessage'
import { LoginFormProps, LoginFormSchema, LoginFormValues } from './types'

const LoginForm = ({ onLoginSuccess }: LoginFormProps) => {
    const { t } = useTranslation()
    const auth = useAuth()
    const [isLoading, setIsLoading] = useState(false)
    const { showModal } = useModals()

    const initialValues: LoginFormValues = {
        username: '',
        password: '',
    }

    const submit = async (values: LoginFormValues) => {
        setIsLoading(true)
        try {
            await auth.login({
                username: values.username,
                password: values.password,
            })
            onLoginSuccess()
        } catch (error: unknown) {
            showModal(<ErrorModal text="common.error.badCredentials" />)
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <Formik
            initialValues={initialValues}
            validationSchema={LoginFormSchema}
            onSubmit={submit}>
            {() => (
                <Form>
                    <div className="input-row py-2">
                        <label htmlFor="login_username">
                            {t('form.input.username')}
                        </label>
                        <div>
                            <Field
                                id="login_username"
                                name="username"
                            />
                            <FndtFormErrorMessage name="username" />
                        </div>
                    </div>
                    <div className="input-row py-2">
                        <label htmlFor="login_password">
                            {t('form.input.password')}
                        </label>
                        <div>
                            <Field
                                id="login_password"
                                type="password"
                                name="password"
                            />
                            <FndtFormErrorMessage name="password" />
                        </div>
                    </div>
                    <div className="button-row pt-2">
                        <FndtButton
                            disabled={isLoading}
                            id="login_submit"
                            type="submit">
                            {t('common.btn.confirm')}
                        </FndtButton>
                    </div>
                </Form>
            )}
        </Formik>
    )
}

export default LoginForm
