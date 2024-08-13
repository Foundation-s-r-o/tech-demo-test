import React, { PropsWithChildren } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import routes from '../../components/router/routes'
import { useAuth } from './useAuth'

export function RequireAuth({ children }: PropsWithChildren) {
    const auth = useAuth()
    const location = useLocation()

    if (!auth.isAuthenticated || !auth.user) {
        return (
            <Navigate
                to={routes.login.path}
                state={{ from: location }}
                replace
            />
        )
    }

    return <>{children}</>
}

export function withRequireAuth(
    WrappedComponent: React.ReactNode
): React.ReactNode {
    return <RequireAuth>{WrappedComponent}</RequireAuth>
}
