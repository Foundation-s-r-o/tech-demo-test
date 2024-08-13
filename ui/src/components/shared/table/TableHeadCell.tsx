import classNames from 'classnames'
import React from 'react'
import { FndtTableHeadCellProps, SortDir } from './types'
import { useFndtTable } from './useFndtTable'

export const FndtTableHeadCell = (props: FndtTableHeadCellProps) => {
    const tableData = useFndtTable()

    const isCurrent = () => {
        return tableData.currentSortBy === props.sort
    }

    const sortLinkClassNames = () => {
        const classes = ['sort', 'fndt_sort_link']
        if (isCurrent()) {
            classes.push('ed')
        }

        return classes.join(' ')
    }

    const sortIcon = () => {
        return isCurrent() ? getCurrentSortClass() : 'bi-arrow-down-up'
    }

    const getCurrentSortClass = () => {
        return tableData.currentSortDir === SortDir.Asc
            ? 'bi-arrow-up'
            : 'bi-arrow-down'
    }

    const handleSort = (event: React.MouseEvent<HTMLElement>) => {
        event.preventDefault()

        if (!isCurrent()) {
            tableData.onSort(props.sort, SortDir.Desc)
        } else {
            tableData.onSort(props.sort, oppositeSort())
        }
    }

    const oppositeSort = () => {
        return tableData.currentSortDir === SortDir.Asc
            ? SortDir.Desc
            : SortDir.Asc
    }

    const renderUnsortable = () => {
        return <th className={props.className}>{props.children}</th>
    }

    const renderSortable = () => {
        return (
            <th className={props.className}>
                <a
                    href="#"
                    data-sort={props.sort}
                    data-sort-dir={isCurrent() ? props.sortDir : 'none'}
                    className={sortLinkClassNames()}
                    onClick={handleSort}>
                    {props.children}
                    <span> </span>
                    <i className={classNames('bi', sortIcon())}></i>
                </a>
            </th>
        )
    }
    return props.sort ? renderSortable() : renderUnsortable()
}
