import React, { useState, useReducer, useEffect } from 'react'
import { isBoolean } from 'lodash'
import { FndtTableProvider } from './TableContext'
import FndtTablePagination from './TablePagination'
import {
    FndtTableReducerActionType,
    FndtTableWrapperProps,
    SortDir,
} from './types'
import fndtTableReducer, { initialFndtTableState } from './reducer'
import {
    IdentifiableItemResponse,
    PagingRequest,
} from '@common/api/types'

/*
* EXAMPLE:
*
* type TestTableItem = { id: number; title: string }
*
<FndtTableWrapper<TestTableItem>
    loadDataFn={(queryParams: PagingRequest) => {
        const pageNumber = queryParams.pageStart || 0
        const pageSize = queryParams.pageSize || 15
        return new Promise<PageableResponse<TestTableItem>>(
            (resolve) => {
                axios
                    .get(
                        'https://jsonplaceholder.typicode.com/posts'
                    )
                    .then((response: any) => {
                        const totalElements =
                            response.data.length
                        response.data.sort(
                            dynamicSort(
                                (queryParams.sortDesc
                                    ? '-'
                                    : '') + queryParams.sortBy
                            )
                        )
                        const paginatedData =
                            response.data.slice(
                                pageNumber * pageSize,
                                pageNumber * pageSize + pageSize
                            )
                        return resolve({
                            hasNextPage: true,
                            totalElements: totalElements,
                            elements: paginatedData,
                        })
                    })
            }
        )
    }}>
        <FndtTable<TestTableItem>
            HeadRow={
                <>
                    <FndtTableHeadCell sort={'id'}>
                        ID#
                    </FndtTableHeadCell>
                    <FndtTableHeadCell
                        sort={'title'}
                        className="text-end">
                        Title
                    </FndtTableHeadCell>
                </>
            }
            renderRow={(item) => {
                return (
                    <>
                        <td>{item.id}</td>
                        <td className="text-end">{item.title}</td>
                    </>
                )
            }}
            />
</FndtTableWrapper>
* */
export const FndtTableWrapper = <T extends IdentifiableItemResponse>({
    initialPage = 1,
    initialPageSize = 15,
    initialSortBy = 'id',
    initialSortDir = SortDir.Asc,
    loadDataFn,
    children,
}: FndtTableWrapperProps<T>) => {
    const [filter, setFilter] = useState({})
    const [state, dispatch] = useReducer(fndtTableReducer, initialFndtTableState)
    const [totalPages, setTotalPages] = useState(0)
    const [currentPage, setCurrentPage] = useState(initialPage)
    const [currentPageSize, setCurrentPageSize] = useState(initialPageSize)
    const [currentSortBy, setCurrentSortBy] = useState(initialSortBy)
    const [currentSortDir, setCurrentSortDir] = useState(initialSortDir)

    useEffect(() => {
        load()
    }, [filter, currentPage, currentPageSize, currentSortDir, currentSortBy])

    function onSort(by: string, dir: SortDir) {
        setCurrentSortBy(by)
        setCurrentSortDir(dir)
    }

    function load(
        pagingParamsOverrides: PagingRequest = null,
        isSilent = false
    ) {
        const pagingParams = {
            sortBy: currentSortBy,
            sortDesc: currentSortDir === SortDir.Desc,
            pageStart: currentPage - 1,
            pageSize: currentPageSize,
            ...pagingParamsOverrides,
        }

        if (!isSilent) {
            dispatch({
                type: FndtTableReducerActionType.LOAD_INIT,
            })
        }

        try {
            loadDataFn(pagingParams)
                .then((result) => {
                    if (
                        isSilent &&
                        JSON.stringify(result.elements) ===
                            JSON.stringify(state.entries)
                    ) {
                        // when it is silent table refresh (eg. websocket/polling) and the result is the same as it is currently rendered,
                        // don't update UI
                        return
                    }
                    setTotalPages(
                        Math.ceil(result.totalElements / pagingParams.pageSize)
                    )
                    dispatch({
                        type: FndtTableReducerActionType.LOAD_SUCCESS,
                        payload: {
                            elements: result.elements,
                            totalElements: result.totalElements,
                            ...(isBoolean(result.hasNextPage)
                                ? { hasNextPage: result.hasNextPage }
                                : null),
                        },
                    })
                })
                .catch((e) => {
                    setTotalPages(0)
                    dispatch({
                        type: FndtTableReducerActionType.LOAD_ERROR,
                    })
                    console.error(
                        'Error occurred when calling Rcm Table Load Data function',
                        e
                    )
                })
        } catch (e) {
            dispatch({
                type: FndtTableReducerActionType.LOAD_ERROR,
            })
            console.error(
                'Error in Fndt Table Load Data function. It must return a promise with object of {elements: Array<any>,  totalElements: Number}'
            )
        }
    }

    const fndtTableApi = {
        load: load,
        total: state.total,
        hasNextPage: state.hasNextPage,
        page: currentPage,
        pageSize: currentPageSize,
        filter,
        setFilter,
        isLoading: state.isLoading,
        entries: state.entries,
        error: state.error,
        currentSortBy,
        currentSortDir,
        onSort,
    }
    return (
        <FndtTableProvider value={fndtTableApi}>
            {children}
            <FndtTablePagination
                page={currentPage}
                totalPages={totalPages}
                pageSize={currentPageSize}
                onPageChange={(newPage) => {
                    setCurrentPage(newPage)
                }}
                onPageSizeChange={(newPageSize) => {
                    setCurrentPage(1)
                    setCurrentPageSize(newPageSize)
                }}
            />
        </FndtTableProvider>
    )
}
