import React, { PropsWithChildren } from 'react'
import AuthProvider from '../../common/auth/AuthProvider'
import ModalsProvider from '../modals/ModalsProvider'

const ContextProviders = ({ children }: PropsWithChildren) => {
    return (
        <>
            <AuthProvider>
                <ModalsProvider>
                    {children}
                </ModalsProvider>
            </AuthProvider>
        </>
    )
}

export default ContextProviders
