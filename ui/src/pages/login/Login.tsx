import React from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'
import AppContent from '../../components/layout/AppContent'
import './Login.scss'
import LoginForm from './LoginForm'
import { LoginLocationState } from './types'

export function LoginPage() {
    const { state } = useLocation()
    const { t } = useTranslation()
    const { from } = (state as LoginLocationState) || {
        from: { pathname: '/' },
    }
    const navigate = useNavigate()

    const onLoginSuccess = () => {
        navigate(from, { replace: true })
    }

    return (
        <AppContent
            title={t('component.title.prihlasenie')}
            contentClassName="d-flex">
            <div className="form-signin-wrapper w-100 d-flex align-items-center">
                <div className="form-signin m-auto">
                    <LoginForm onLoginSuccess={onLoginSuccess} />
                </div>
            </div>
        </AppContent>
    )
}
