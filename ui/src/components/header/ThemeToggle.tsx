import { MoonIcon, SunIcon } from '@heroicons/react/24/outline'
import React from 'react'
import { useTranslation } from 'react-i18next'
import { useTheme } from '../../common/theme/useTheme'

const ThemeToggle = () => {
    const { t } = useTranslation()
    const { theme, toggleTheme } = useTheme()

    const label =
        theme === 'dark'
            ? t('common.layout.themeLight')
            : t('common.layout.themeDark')

    return (
        <button
            id="theme_toggle"
            type="button"
            onClick={toggleTheme}
            aria-label={label}
            title={label}
            className="theme-toggle ms-2">
            {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
        </button>
    )
}

export default ThemeToggle
