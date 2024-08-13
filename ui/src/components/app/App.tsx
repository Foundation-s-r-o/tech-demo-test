import React from 'react'
import { AppRouter } from '../router/AppRouter'
import './App.scss'
import AppLayout from '../layout/AppLayout'

const App = () => {
    return (
        <AppLayout>
            <AppRouter />
        </AppLayout>
    )
}

export default App

