import React from 'react'
import { RouteObject } from 'react-router-dom'
import { withRequireAuth } from '../../common/auth/RequireAuth'
import { HomePage } from '../../pages/home/Home'
import { LoginPage } from '../../pages/login/Login'
import PersonsListPage from '@pages/persons/ListPage'
import PersonsAddPage from '@pages/persons/AddPage'
import PersonDetailPage from '@pages/persons/DetailPage'
import BirthPage from '@pages/records/BirthPage'
import MarriagePage from '@pages/records/MarriagePage'
import DeathPage from '@pages/records/DeathPage'
import GdprPage from '@pages/gdpr/GdprPage'
import AuditPage from '@pages/audit/AuditPage'
import MePage from '@pages/me/MePage'


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
    personsDetail: {
        path: '/persons/:uid',
        element: withRequireAuth(<PersonDetailPage />),
    },
    recordsBirth: {
        path: '/records/births',
        element: withRequireAuth(<BirthPage />),
    },
    recordsMarriage: {
        path: '/records/marriages',
        element: withRequireAuth(<MarriagePage />),
    },
    recordsDeath: {
        path: '/records/deaths',
        element: withRequireAuth(<DeathPage />),
    },
    gdpr: {
        path: '/gdpr',
        element: withRequireAuth(<GdprPage />),
    },
    audit: {
        path: '/audit',
        element: withRequireAuth(<AuditPage />),
    },
    me: {
        path: '/me',
        element: withRequireAuth(<MePage />),
    }
}

export const routesArray: RouteObject[] = Object.values(routes)

export default routes
