import React, {
    createContext,
    PropsWithChildren,
    useContext,
    useMemo,
    useState,
} from 'react'
import persistService from '../../common/persistService'

const SIDEBAR_COLLAPSED_KEY = 'fndt-sidebar-collapsed'
const DESKTOP_MEDIA_QUERY = '(min-width: 992px)'

type SidebarContextType = {
    // Desktop rail state (persisted); irrelevant on mobile.
    collapsed: boolean
    // Mobile overlay state (ephemeral).
    mobileOpen: boolean
    toggleSidebar: () => void
    closeMobile: () => void
    expand: () => void
}

const SidebarContext = createContext<SidebarContextType | undefined>(undefined)

export const SidebarProvider = ({ children }: PropsWithChildren) => {
    const [collapsed, setCollapsed] = useState<boolean>(
        () => persistService.get(SIDEBAR_COLLAPSED_KEY) === true
    )
    const [mobileOpen, setMobileOpen] = useState(false)

    const setAndPersistCollapsed = (value: boolean) => {
        setCollapsed(value)
        persistService.set(SIDEBAR_COLLAPSED_KEY, value)
    }

    const value = useMemo(() => {
        const toggleSidebar = () => {
            if (globalThis.matchMedia(DESKTOP_MEDIA_QUERY).matches) {
                setAndPersistCollapsed(!collapsed)
            } else {
                setMobileOpen((prev) => !prev)
            }
        }

        const closeMobile = () => setMobileOpen(false)

        const expand = () => setAndPersistCollapsed(false)

        return { collapsed, mobileOpen, toggleSidebar, closeMobile, expand }
    }, [collapsed, mobileOpen])

    return (
        <SidebarContext.Provider value={value}>
            {children}
        </SidebarContext.Provider>
    )
}

export const useSidebar = () => {
    const context = useContext(SidebarContext)
    if (!context) {
        throw new Error('useSidebar must be used within SidebarProvider')
    }
    return context
}
