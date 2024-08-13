import React from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import ReactLogo from '../../assets/images/login_avatar.svg'
import { useAuth } from '../../common/auth/useAuth'
import AppMenu from '../menu/AppMenu'
import routes from '../router/routes'
import './AppHeader.scss'

const AppHeader = () => {
    const { t } = useTranslation()
    const auth = useAuth()
    const navigate = useNavigate()

    const onLogoutSuccess = () => {
        auth.logout(() => {
            navigate(routes.home.path, { replace: true })
        })
    }

    return (
        <>
            <div className={'header'}>
                <div className="container">
                    <div className="d-flex header-content-wrapper">
                        {auth.isAuthenticated && <AppMenu />}
                        <div className="d-flex align-items-center flex-grow-1">
                            <h1>{t('common.layout.headerTitle')}</h1>
                        </div>

                        <div
                            className="d-flex align-items-center pe-2"
                            id="auth-info">
                            <img src={ReactLogo} />
                            {auth.isAuthenticated ? (
                                <>
                                    <div id="app_header_user_fullname">
                                        {auth.user.firstName +
                                            ' ' +
                                            auth.user.lastName}
                                    </div>
                                    <span>{' | '}</span>
                                    <button
                                        onClick={onLogoutSuccess}
                                        className="ms-2">
                                        {t('common.btn.logout')}
                                    </button>
                                </>
                            ) : (
                                <>
                                    <div id="app_header_user_fullname">
                                        {t('common.layout.notLoggedInUser')}
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </>
    )
}

export default AppHeader
