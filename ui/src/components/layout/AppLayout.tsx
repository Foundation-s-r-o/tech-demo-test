import React, { PropsWithChildren } from 'react'
import AppFooter from '../footer/AppFooter'
import AppHeader from '../header/AppHeader'
import './AppLayout.scss'

const AppLayout = ({ children }: PropsWithChildren) => {
    return (
        <>
            <AppHeader />
            {children}
            <AppFooter />
        </>
    )
}

export default AppLayout
