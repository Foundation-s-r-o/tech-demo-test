import persistService from '../persistService'

export type Theme = 'light' | 'dark'

export const THEME_STORAGE_KEY = 'fndt-theme'

export const getInitialTheme = (): Theme => {
    const stored = persistService.get(THEME_STORAGE_KEY)
    if (stored === 'light' || stored === 'dark') {
        return stored
    }
    return globalThis.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light'
}

export const applyTheme = (theme: Theme) => {
    document.documentElement.dataset.bsTheme = theme
    persistService.set(THEME_STORAGE_KEY, theme)
}
