import React from 'react'
import { FndtTableWrapper } from '@components/shared/table/TableWrapper'
import { PersonListItemResponseDTO } from '@api/generated'
import { PageableResponse, PagingRequest } from '@common/api/types'
import { personControllerApi } from '@api/api'
import { FndtTable } from '@components/shared/table/Table'
import { FndtTableHeadCell } from '@components/shared/table/TableHeadCell'
import {
    transformRequest,
    transformArrayOfT1ToArrayOfT2
} from '@common/api/transformers'
import { UiPersonListItemResponseDTO } from '@api/types'
import { useTranslation } from 'react-i18next'
import { PencilSquareIcon } from '@heroicons/react/24/outline'
import FndtIcon from '@components/shared/icons/FndtIcon'
import routes from '@components/router/routes'
import { Link } from 'react-router-dom'

export default function PersonsTable() {
    const { t } = useTranslation()
    return (
        <FndtTableWrapper<UiPersonListItemResponseDTO>
            initialSortBy={'NAME'}
            loadDataFn={(queryParams: PagingRequest) => {
                return new Promise<PageableResponse<UiPersonListItemResponseDTO>>(
                    (resolve) => {
                        const transformedParams: Record<string, unknown> = {
                            ...queryParams
                        }
                        personControllerApi.listUsers(transformRequest(transformedParams))
                            .then((response) => {
                                return resolve({
                                    hasNextPage: true,
                                    totalElements: response.data.totalElements,
                                    elements: transformArrayOfT1ToArrayOfT2<PersonListItemResponseDTO, UiPersonListItemResponseDTO>(response.data.elements ?? []),
                                })
                            })
                    }
                )
            }}>
            <FndtTable<UiPersonListItemResponseDTO>
                HeadRow={
                    <>
                        <FndtTableHeadCell className="text-end">
                            {t('form.input.id')}
                        </FndtTableHeadCell>
                        <FndtTableHeadCell>
                            {t('form.input.firstName')}
                        </FndtTableHeadCell>
                        <FndtTableHeadCell sort={'NAME'}>
                            {t('form.input.lastName')}
                        </FndtTableHeadCell>
                        <FndtTableHeadCell sort={'EMAIL'}>
                            {t('form.input.email')}
                        </FndtTableHeadCell>
                        <FndtTableHeadCell
                            className="text-end">
                            {t('common.helper.action')}
                        </FndtTableHeadCell>
                    </>
                }
                renderRow={(item) => {
                    return (
                        <>
                            <td className="text-end">{item.id}</td>
                            <td>{item.firstName}</td>
                            <td>{item.lastName}</td>
                            <td>{item.email}</td>
                            <td className="text-end">
                                <Link
                                    id={`person_edit_${item.id}`}
                                    className="fndt_row_action"
                                    to={routes.personsEdit.path.replace(':id', String(item.id))}>
                                    <FndtIcon
                                        icon={PencilSquareIcon}
                                        size="sm" />
                                    {t('common.btn.edit')}
                                </Link>
                            </td>
                        </>
                    )
                }}
            />
        </FndtTableWrapper>)
}
