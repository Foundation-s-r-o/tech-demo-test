import { PropsWithChildren } from 'react'
import { IdentifiableItemResponse, PageableResponse, PagingRequest } from '../../../common/api/types'

export enum SortDir {
    Desc = 'desc',
    Asc = 'asc',
}

// export type LoadItemsFunction<T> = (queryParams: PagingRequest) => Promise<T>
export type LoadItemsFunction<T extends IdentifiableItemResponse> = (
    queryParams: PagingRequest
) => Promise<PageableResponse<T>>

export type FndtTableWrapperProps<T extends IdentifiableItemResponse> = {
    initialPage?: number
    initialPageSize?: number
    initialSortBy?: string
    initialSortDir?: SortDir
    loadDataFn: LoadItemsFunction<T>
} & PropsWithChildren

export interface FndtTableWrapperStateType {
    total: number
    entries: IdentifiableItemResponse[]
    hasNextPage: boolean
    isLoading: boolean
    error: boolean
}

export enum FndtTableReducerActionType {
    LOAD_INIT,
    LOAD_SUCCESS,
    LOAD_ERROR,
}

export type FndtTableReducerAction = {
    type: FndtTableReducerActionType
    payload?: PageableResponse<IdentifiableItemResponse>
}

export interface FndtTableContextType extends FndtTableWrapperStateType {
    load: (queryParams?: PagingRequest, isSilent?: boolean) => void
    filter: unknown
    setFilter: (filter: unknown) => void
    currentSortBy: string
    currentSortDir: SortDir
    onSort: (by: string, dir: SortDir) => void
}

export type FndtTableProps<T extends IdentifiableItemResponse> = {
    keyExtractor?: (item: T) => string | number
    renderRow: (item: T) => React.ReactNode
    HeadRow: React.ReactNode
}

export type FndtTableBodyRowProps = {
    className?: string
    onClick?: () => void
} & PropsWithChildren

export type FndtTableHeadCellProps = {
    className?: string
    children: React.ReactNode
    sortDir?: SortDir
    sort?: string
    onSort?: (sortDir: SortDir) => void
} & PropsWithChildren

export type FndtTablePaginationProps = {
    page: number
    pageSize: number
    totalPages: number
    onPageChange?: (page: number) => void
    onPageSizeChange?: (pageSize: number) => void
}
