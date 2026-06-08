import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './components/app/App'
import './common/i18n'
import ContextProviders from './components/shared/ContextProviders'
import '../node_modules/bootstrap/dist/js/bootstrap.bundle.min.js'
import './assets/sass/main.scss'

const container = document.getElementById('root')
if (!container) {
    throw new Error('Root element #root not found')
}
const root = ReactDOM.createRoot(container)

root.render(
    // <React.StrictMode>
    <BrowserRouter>
        <ContextProviders>
            <App />
        </ContextProviders>
    </BrowserRouter>
    // </React.StrictMode>
)
