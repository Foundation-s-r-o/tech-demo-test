import { ChevronLeftIcon, ChevronRightIcon } from '@heroicons/react/24/outline'
import classNames from 'classnames'
import React from 'react'
import { useTranslation } from 'react-i18next'
import FndtIcon from '../icons/FndtIcon'
import { FndtTablePaginationProps } from './types'

// Windowed page numbers: 1 … page-1 page page+1 … last (null = ellipsis)
const pageWindow = (page: number, totalPages: number): (number | null)[] => {
    const wanted = [1, page - 1, page, page + 1, totalPages]
    const pages = [...new Set(wanted)]
        .filter((p) => p >= 1 && p <= totalPages)
        .sort((a, b) => a - b)
    const result: (number | null)[] = []
    pages.forEach((p, i) => {
        if (i > 0 && p - pages[i - 1] > 1) {
            result.push(null)
        }
        result.push(p)
    })
    return result
}

const FndtTablePagination = ({
    totalPages,
    page,
    pageSize,
    total,
    onPageChange,
    onPageSizeChange,
}: FndtTablePaginationProps) => {
    const { t } = useTranslation()
    const perPageSizes = [10, 15, 25, 50, 100, 200]

    if (!totalPages) {
        return null
    }

    const from = (page - 1) * pageSize + 1
    const to = Math.min(page * pageSize, total)

    return (
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 fndt_pagination">
            <div className="fndt_pagination_summary">
                {t('common.table.showingResults', { from, to, total })}
            </div>

            <div className="d-flex align-items-center gap-3">
                <div className="d-flex align-items-center gap-2">
                    <label
                        htmlFor="pageSizeSelect"
                        className="fndt_pagination_summary">
                        {t('common.table.pageSize')}
                    </label>
                    <select
                        id="pageSizeSelect"
                        name="itemsPerPage"
                        className="form-select form-select-sm w-auto"
                        value={pageSize}
                        onChange={(
                            event: React.ChangeEvent<HTMLSelectElement>
                        ) => {
                            onPageSizeChange(parseInt(event.target.value))
                        }}>
                        {perPageSizes.map((pageLength) => (
                            <option
                                key={pageLength}
                                value={pageLength}>
                                {pageLength}
                            </option>
                        ))}
                    </select>
                </div>

                <nav aria-label={t('common.table.pagination')}>
                    <ul className="pagination pagination-sm mb-0">
                        <li className="page-item">
                            <button
                                className="page-link"
                                disabled={page === 1}
                                onClick={() => onPageChange(page - 1)}
                                title={t('common.table.prevPage')}
                                aria-label={t('common.table.prevPage')}>
                                <FndtIcon
                                    icon={ChevronLeftIcon}
                                    size="sm" />
                            </button>
                        </li>
                        {pageWindow(page, totalPages).map((p, i) =>
                            p === null ? (
                                <li
                                    key={`gap${i}`}
                                    className="page-item disabled">
                                    <span className="page-link">&hellip;</span>
                                </li>
                            ) : (
                                <li
                                    key={p}
                                    className={classNames('page-item', {
                                        active: p === page,
                                    })}>
                                    <button
                                        className="page-link"
                                        aria-current={
                                            p === page ? 'page' : undefined
                                        }
                                        onClick={() => onPageChange(p)}>
                                        {p}
                                    </button>
                                </li>
                            )
                        )}
                        <li className="page-item">
                            <button
                                className="page-link"
                                disabled={page === totalPages}
                                onClick={() => onPageChange(page + 1)}
                                title={t('common.table.nextPage')}
                                aria-label={t('common.table.nextPage')}>
                                <FndtIcon
                                    icon={ChevronRightIcon}
                                    size="sm" />
                            </button>
                        </li>
                    </ul>
                </nav>
            </div>
        </div>
    )
}

export default FndtTablePagination
