import React, { PropsWithChildren } from 'react'
import AuthProvider from '../../common/auth/AuthProvider'
import ModalsProvider from '../modals/ModalsProvider'
import { SidebarProvider } from '../sidebar/SidebarContext'

const ContextProviders = ({ children }: PropsWithChildren) => {
    return (
        <>
            <AuthProvider>
                <SidebarProvider>
                    <ModalsProvider>
                        {children}
                    </ModalsProvider>
                </SidebarProvider>
            </AuthProvider>
        </>
    )
}

export default ContextProviders
