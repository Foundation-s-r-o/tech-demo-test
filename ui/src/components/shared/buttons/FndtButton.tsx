import React from 'react'
import { Button } from 'react-bootstrap'
import classNames from 'classnames'
import { Link } from 'react-router-dom'
import { FndtButtonProps, FndtButtonSize } from './types'
import './Button.scss'

export const FndtButton = ({
    children,
    className,
    size = FndtButtonSize.lg,
    variant = 'primary',
    to = null,
    ...props
}: FndtButtonProps) => {
    const prefix = 'btn'
    const cssClasses = classNames(
        'fndt_button',
        className,
        prefix,
        props.active && 'active',
        variant && `${prefix}-${variant}`,
        size && `${prefix}-${size}`,
        props.disabled && 'disabled'
    )

    return to ? (
        <Link
            to={to}
            className={cssClasses}>
            {children}
        </Link>
    ) : (
        <Button
            className={cssClasses}
            {...{ variant, size }}
            {...props}>
            {children}
        </Button>
    )
}
