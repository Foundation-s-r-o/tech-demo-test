import React, { PropsWithChildren } from 'react'
import { useAuth } from '../../common/auth/useAuth'
import AppFooter from '../footer/AppFooter'
import AppHeader from '../header/AppHeader'
import AppSidebar from '../sidebar/AppSidebar'
import './AppLayout.scss'

const AppLayout = ({ children }: PropsWithChildren) => {
    const auth = useAuth()

    return (
        <>
            <AppHeader />
            <div className="app-shell">
                {auth.isAuthenticated && <AppSidebar />}
                <main className="app-main">{children}</main>
            </div>
            <AppFooter />
        </>
    )
}

export default AppLayout
