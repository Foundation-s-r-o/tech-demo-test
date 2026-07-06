import classNames from 'classnames'
import React, { ComponentType, SVGProps } from 'react'
import './FndtIcon.scss'

// Any @heroicons/react component (outline or solid) matches this shape.
export type HeroIcon = ComponentType<
    SVGProps<SVGSVGElement> & { title?: string; titleId?: string }
>

export type FndtIconSize = 'sm' | 'md' | 'lg'

type FndtIconProps = {
    icon: HeroIcon
    size?: FndtIconSize
    className?: string
    title?: string
}

const FndtIcon = ({
    icon: Icon,
    size = 'md',
    className,
    title,
}: FndtIconProps) => {
    return (
        <Icon
            aria-hidden={title ? undefined : true}
            focusable="false"
            title={title}
            className={classNames('fndt_icon', `fndt_icon--${size}`, className)}
        />
    )
}

export default FndtIcon
