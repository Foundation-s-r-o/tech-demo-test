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
            className={classNames('form-label', className)}
            {...props}>
            {children}
        </label>
    )
}

export default FndtLabel
