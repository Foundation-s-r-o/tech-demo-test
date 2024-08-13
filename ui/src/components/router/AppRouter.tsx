import { useRoutes } from 'react-router-dom'
import { routesArray } from './routes'

export function AppRouter() {
    return useRoutes(routesArray)
}
