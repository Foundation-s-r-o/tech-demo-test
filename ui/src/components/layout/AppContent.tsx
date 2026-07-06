import classNames from 'classnames'
import React, { PropsWithChildren, ReactNode } from 'react'
import { Link } from 'react-router-dom'
import PageInfo from '../shared/PageInfo'
import './AppContent.scss'

export type Breadcrumb = {
    label: string
    to?: string
}

type Props = PropsWithChildren & {
    title: string
    contentClassName?: string
    breadcrumbs?: Breadcrumb[]
    actions?: ReactNode
}

const AppContent = ({
    title,
    children,
    contentClassName,
    breadcrumbs,
    actions,
}: Props) => {
    return (
        <div id="app-content">
            {title && (
                <>
                    <PageInfo title={title} />
                    <div
                        id={'app-content-title'}
                        className="container-fluid">
                        <div className="container">
                            {breadcrumbs && breadcrumbs.length > 0 && (
                                <nav
                                    aria-label="breadcrumb"
                                    className="app-breadcrumbs">
                                    <ol>
                                        {breadcrumbs.map((crumb) => (
                                            <li key={crumb.label}>
                                                {crumb.to ? (
                                                    <Link to={crumb.to}>
                                                        {crumb.label}
                                                    </Link>
                                                ) : (
                                                    crumb.label
                                                )}
                                            </li>
                                        ))}
                                        <li aria-current="page">{title}</li>
                                    </ol>
                                </nav>
                            )}
                            <div className="app-page-heading">
                                <h2 className="text-primary-emphasis">
                                    {title}
                                </h2>
                                {actions && (
                                    <div className="app-page-actions">
                                        {actions}
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </>
            )}
            <div
                id={'app-content-frame'}
                className={classNames('container p-3', contentClassName)}>
                {children}
            </div>
        </div>
    )
}

export default AppContent
