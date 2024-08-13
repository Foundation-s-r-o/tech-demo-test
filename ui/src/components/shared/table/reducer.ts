import { isBoolean } from 'lodash'
import { Reducer } from 'react'
import { FndtTableReducerAction, FndtTableReducerActionType, FndtTableWrapperStateType } from './types'

export const initialFndtTableState: FndtTableWrapperStateType = {
    total: 0,
    entries: [],
    hasNextPage: false,
    isLoading: false,
    error: false,
}

const fndtTableReducer: Reducer<FndtTableWrapperStateType, FndtTableReducerAction> = (
    state,
    action
): FndtTableWrapperStateType => {
    switch (action.type) {
        case FndtTableReducerActionType.LOAD_INIT:
            return {
                ...state,
                error: false,
                hasNextPage: false,
                isLoading: true,
            }
        case FndtTableReducerActionType.LOAD_SUCCESS:
            return {
                isLoading: false,
                total: action?.payload?.totalElements || initialFndtTableState.total,
                entries: action?.payload?.elements || initialFndtTableState.entries,
                hasNextPage: isBoolean(action?.payload?.hasNextPage)
                    ? action?.payload?.hasNextPage
                    : false,
                error: false,
            }
        case FndtTableReducerActionType.LOAD_ERROR:
            return {
                ...initialFndtTableState,
                error: true,
            }
        default:
            throw new Error()
    }
}

export default fndtTableReducer
