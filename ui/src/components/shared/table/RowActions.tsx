import { EllipsisHorizontalIcon } from '@heroicons/react/24/outline'
import React, { PropsWithChildren } from 'react'
import { Dropdown } from 'react-bootstrap'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

// Per-row "..." actions menu (Nord design system table pattern): a bordered
// icon button opening a dropdown of row actions.
export const FndtRowActions = ({
    id,
    children,
}: PropsWithChildren<{ id: string }>) => {
    const { t } = useTranslation()
    return (
        <Dropdown
            align="end"
            className="fndt_row_actions">
            <Dropdown.Toggle
                id={id}
                variant=""
                className="fndt_row_actions_toggle"
                aria-label={t('common.helper.action')}>
                <EllipsisHorizontalIcon aria-hidden="true" />
            </Dropdown.Toggle>
            <Dropdown.Menu>{children}</Dropdown.Menu>
        </Dropdown>
    )
}

export const FndtRowActionItem = ({
    to,
    onClick,
    children,
}: PropsWithChildren<{ to?: string; onClick?: () => void }>) => {
    return to ? (
        <Dropdown.Item
            as={Link}
            to={to}>
            {children}
        </Dropdown.Item>
    ) : (
        <Dropdown.Item onClick={onClick}>{children}</Dropdown.Item>
    )
}
