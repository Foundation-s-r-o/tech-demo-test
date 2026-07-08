import React from 'react'
import { FndtTableWrapper } from '@components/shared/table/TableWrapper'
import { PageableResponse, PagingRequest } from '@common/api/types'
import { personControllerApi } from '@api/api'
import { FndtTable } from '@components/shared/table/Table'
import { FndtTableHeadCell } from '@components/shared/table/TableHeadCell'
import { UiPersonSummary } from '@api/types'
import { useTranslation } from 'react-i18next'
import { EyeIcon } from '@heroicons/react/24/outline'
import FndtIcon from '@components/shared/icons/FndtIcon'
import routes from '@components/router/routes'
import { Link } from 'react-router-dom'

export default function PersonsTable() {
    const { t } = useTranslation()
    return (
        <FndtTableWrapper<UiPersonSummary>
            initialSortBy={'familyName'}
            loadDataFn={(queryParams: PagingRequest): Promise<PageableResponse<UiPersonSummary>> =>
                personControllerApi
                    .search({
                        xAccessPurpose: 'registry-list',
                        pageStart: queryParams.pageStart,
                        pageSize: queryParams.pageSize,
                    })
                    .then((response) => {
                        const elements = (response.data.elements ?? []).map((person) => ({
                            ...person,
                            id: person.uid ?? '',
                        }))
                        return {
                            hasNextPage: elements.length >= queryParams.pageSize,
                            totalElements: response.data.totalElements,
                            elements,
                        }
                    })
            }>
            <FndtTable<UiPersonSummary>
                HeadRow={
                    <>
                        <FndtTableHeadCell>{t('form.input.uid')}</FndtTableHeadCell>
                        <FndtTableHeadCell>{t('form.input.givenNames')}</FndtTableHeadCell>
                        <FndtTableHeadCell>{t('form.input.familyName')}</FndtTableHeadCell>
                        <FndtTableHeadCell>{t('form.input.dateOfBirth')}</FndtTableHeadCell>
                        <FndtTableHeadCell className="text-end">
                            {t('common.helper.action')}
                        </FndtTableHeadCell>
                    </>
                }
                renderRow={(item) => (
                    <>
                        <td>{item.uid}</td>
                        <td>{item.givenNames}</td>
                        <td>{item.familyName}</td>
                        <td>{item.dateOfBirth}</td>
                        <td className="text-end">
                            <Link
                                id={`person_detail_${item.uid}`}
                                className="fndt_row_action"
                                to={routes.personsDetail.path.replace(':uid', String(item.uid ?? ''))}>
                                <FndtIcon icon={EyeIcon} size="sm" />
                                {t('component.persons.view')}
                            </Link>
                        </td>
                    </>
                )}
            />
        </FndtTableWrapper>)
}
