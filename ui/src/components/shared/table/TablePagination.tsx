import React, { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FndtTablePaginationProps } from './types'

const FndtTablePagination = ({
                                 totalPages,
                                 page,
                                 pageSize,
                                 onPageChange,
                                 onPageSizeChange,
                             }: FndtTablePaginationProps) => {
    const { t } = useTranslation()
    const [internalPageNumber, setInternalPageNumber] = useState(page)
    const [internalPageSize, setInternalPageSize] = useState(pageSize)
    const perPageSizes = [10, 15, 25, 50, 100, 200]

    useEffect(() => {
        setInternalPageNumber(page)
    }, [page])

    return (
        <div className="d-flex justify-content-between">
            {totalPages ? <>
                    <div>
                        <nav aria-label="Page navigation example">
                            <ul className="pagination">
                                <li className="page-item">
                                    <button
                                        className="page-link"
                                        disabled={page === 1}
                                        onClick={(
                                            event: React.MouseEvent<HTMLButtonElement>
                                        ) => {
                                            event.preventDefault()
                                            onPageChange(1)
                                        }}
                                        title={t('common.table.firstPage')}
                                        aria-label={t('common.table.firstPage')}>
                                    <span
                                        aria-hidden="true">&laquo;&laquo;</span>
                                    </button>
                                </li>

                                <li className="page-item">
                                    <button
                                        className="page-link"
                                        disabled={page === 1}
                                        onClick={(
                                            event: React.MouseEvent<HTMLButtonElement>
                                        ) => {
                                            event.preventDefault()
                                            onPageChange(page - 1)
                                        }}
                                        title={t('common.table.prevPage')}
                                        aria-label={t('common.table.prevPage')}>
                                        <span aria-hidden="true">&laquo;</span>
                                    </button>
                                </li>

                                <li className="page-item">
                                    <button
                                        className="page-link"
                                        disabled={page === totalPages}
                                        onClick={(
                                            event: React.MouseEvent<HTMLButtonElement>
                                        ) => {
                                            event.preventDefault()
                                            onPageChange(page + 1)
                                        }}
                                        title={t('common.table.nextPage')}
                                        aria-label={t('common.table.nextPage')}>
                                        <span aria-hidden="true">&raquo;</span>
                                    </button>
                                </li>
                                <li className="page-item">
                                    <button
                                        className="page-link"
                                        disabled={page === totalPages}
                                        onClick={(
                                            event: React.MouseEvent<HTMLButtonElement>
                                        ) => {
                                            event.preventDefault()
                                            onPageChange(totalPages)
                                        }}
                                        title={t('common.table.lastPage')}
                                        aria-label={t('common.table.lastPage')}>
                                    <span
                                        aria-hidden="true">&raquo;&raquo;</span>
                                    </button>
                                </li>
                            </ul>
                        </nav>
                    </div>
                    <div>
                        <div className="row g-3 align-items-center">
                            <div className="col-auto">
                                <label
                                    htmlFor="pageInput"
                                    className="col-form-label">
                                    {t('common.table.pageNo')}
                                </label>
                            </div>
                            <div className="col-auto">
                                <form
                                    onSubmit={(e: React.SyntheticEvent) => {
                                        e.preventDefault()
                                        onPageChange(internalPageNumber)
                                    }}>
                                    <input
                                        type="number"
                                        min={1}
                                        max={totalPages}
                                        onChange={(
                                            event: React.ChangeEvent<HTMLInputElement>
                                        ) => {
                                            setInternalPageNumber(
                                                parseInt(event.target.value)
                                            )
                                        }}
                                        value={internalPageNumber}
                                        id="pageInput"
                                        className="form-control pagination_input_page"
                                    />
                                </form>
                            </div>
                            <div className="col-auto">
                        <span>
                            {t('common.table.ofTotalPages', {
                                totalPages: totalPages,
                            })}
                        </span>
                            </div>
                        </div>
                    </div>

                    <div>
                        <div className="row g-3 align-items-center">
                            <div className="col-auto">
                                {t('common.table.pageSize')}
                            </div>
                            <div className="col-auto">
                                <select
                                    name="itemsPerPage"
                                    className="form-select"
                                    value={internalPageSize}
                                    onChange={(
                                        event: React.ChangeEvent<HTMLSelectElement>
                                    ) => {
                                        setInternalPageSize(
                                            parseInt(event.target.value)
                                        )
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
                        </div>
                    </div>
                </>
                : null}
        </div>
    )
}

export default FndtTablePagination
