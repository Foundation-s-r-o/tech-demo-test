import { PersonListItemResponseDTO } from '@api/generated'
import { IdentifiableItemResponse } from '@common/api/types'

export type UiPersonListItemResponseDTO = PersonListItemResponseDTO & IdentifiableItemResponse
