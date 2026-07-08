export interface IdentifiableItemResponse {
    // Registry rows are keyed by an opaque string UID; demo rows used a numeric id.
    id: string | number
}

export interface PageableResponse<T, > {
    elements: T[]
    hasNextPage: boolean
    totalElements?: number
}

export interface PagingRequest {
    sortBy: string
    sortDesc: boolean
    pageStart: number
    pageSize: number
    filter?: unknown
}
