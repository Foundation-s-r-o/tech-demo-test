import { useState } from 'react'
import { applyTheme, getInitialTheme, Theme } from './theme'

export const useTheme = () => {
    const [theme, setTheme] = useState<Theme>(getInitialTheme)

    const toggleTheme = () => {
        const next: Theme = theme === 'dark' ? 'light' : 'dark'
        applyTheme(next)
        setTheme(next)
    }

    return { theme, toggleTheme }
}
