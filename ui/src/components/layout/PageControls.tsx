import React, { PropsWithChildren } from 'react'

export default function PageControls({ children }: PropsWithChildren) {
    return (
        <div
            id={'app-content-controls'}
            className="container-fluid">
            <div className="container">{children}</div>
        </div>
    )
}
