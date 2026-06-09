import React from 'react'
import { RouteObject } from 'react-router-dom'
import { withRequireAuth } from '../../common/auth/RequireAuth'
import { HomePage } from '../../pages/home/Home'
import { LoginPage } from '../../pages/login/Login'
import PersonsListPage from '@pages/persons/ListPage'
import PersonsAddPage from '@pages/persons/AddPage'
import PersonsEditPage from '@pages/persons/EditPage'


// Every app route is statically defined with a concrete path, so narrow RouteObject's
// optional `path?: string` to a required string — lets callers use routes.x.path.replace(...)
// without undefined guards under TS6 strictNullChecks.
export const routes: {
    [routeName: string]: RouteObject & { path: string }
} = {
    login: {
        path: '/login',
        element: <LoginPage />,
    },
    root: {
        path: '/',
        element: withRequireAuth(<HomePage />),
    },
    home: {
        path: '/home',
        element: withRequireAuth(<HomePage />),
    },
    personsList: {
        path: '/persons',
        element: withRequireAuth(<PersonsListPage />),
    },
    personsAdd: {
        path: '/persons/add',
        element: withRequireAuth(<PersonsAddPage />),
    },
    personsEdit: {
        path: '/persons/:id',
        element: withRequireAuth(<PersonsEditPage />),
    }
}

export const routesArray: RouteObject[] = Object.values(routes)

export default routes
