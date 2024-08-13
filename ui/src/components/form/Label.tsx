import classNames from 'classnames'
import React, { PropsWithChildren } from 'react'

const FndtLabel = ({
    children,
    className,
    ...props
}: PropsWithChildren & {
    htmlFor?: string | undefined
    className?: string | undefined
}) => {
    return (
        <label
            className={classNames('col-form-label fw-bold', className)}
            {...props}>
            {children}
        </label>
    )
}

export default FndtLabel
