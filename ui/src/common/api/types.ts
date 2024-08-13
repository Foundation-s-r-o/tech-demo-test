export interface IdentifiableItemResponse {
    id: number
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
