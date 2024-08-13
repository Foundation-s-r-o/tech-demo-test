import classNames from 'classnames'
import React, { PropsWithChildren } from 'react'
import PageInfo from '../shared/PageInfo'
import './AppContent.scss'

type Props = PropsWithChildren & {
    title: string
    contentClassName?: string
}

const AppContent = ({ title, children, contentClassName = null }: Props) => {
    return (
        <div id="app-content">
            {title && (
                <>
                    <PageInfo title={title} />
                    <div
                        id={'app-content-title'}
                        className="container-fluid border-bottom border-secondary">
                        <div className="container">
                            <h2 className="text-primary-light fw-normal">
                                {title}
                            </h2>
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
