import { PropsWithChildren } from 'react'

export type FndtButtonType = 'button' | 'reset' | 'submit'
export type FndtButtonVariant = 'primary' | 'secondary'
export enum FndtButtonSize {
    'sm' = 'sm',
    'lg' = 'lg',
}
export type FndtButtonProps = {
    type?: FndtButtonType | undefined
    role?: 'button'
    to?: string
    tabIndex?: number | undefined
    href?: string | undefined
    id?: string | undefined
    className?: string | undefined
    target?: string | undefined
    rel?: string | undefined
    'aria-disabled'?: true | undefined
    onClick?: (event: React.MouseEvent | React.KeyboardEvent) => void
    variant?: FndtButtonVariant
    size?: FndtButtonSize | undefined
    disabled?: boolean | undefined
    active?: boolean | undefined
} & PropsWithChildren
