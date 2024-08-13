import React from 'react'
import { useTranslation } from 'react-i18next'
import './AppFooter.scss'

const AppFooter = () => {
    const { t } = useTranslation()
    const version = '1.0'
    const date = new Date().toLocaleString('sk', {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
    })
    const time = new Date().toLocaleString('sk', {
        hour: 'numeric',
        minute: 'numeric',
        second: 'numeric',
    })

    return (
        <footer className={'footer mt-auto'}>
            <p className={'footer-text'}>
                {t('common.layout.footerVersion', { version })} | {date} | {time}
            </p>
        </footer>
    )
}

export default AppFooter
