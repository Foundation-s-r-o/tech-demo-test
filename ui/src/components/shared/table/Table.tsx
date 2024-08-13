import React from 'react'
import { IdentifiableItemResponse } from '../../../common/api/types'
import './Table.scss'
import FndtTableBodyRow from './TableBodyRow'
import { FndtTableProps } from './types'
import { useFndtTable } from './useFndtTable'
import { useTranslation } from 'react-i18next'

export const FndtTable = <T extends IdentifiableItemResponse>({
  renderRow,
  keyExtractor = (item: T) => {
      return item.id
  },
  HeadRow,
}: FndtTableProps<T>) => {
    const tableData = useFndtTable()
    const { t } = useTranslation()

    return (
        <table className="table fndt_table">
            <thead>
            <tr>{HeadRow}</tr>
            </thead>
            <tbody>
            {tableData.entries.length ? tableData.entries.map((item: T) => (
                <FndtTableBodyRow key={keyExtractor(item)}>
                    {renderRow(item)}
                </FndtTableBodyRow>
            )) : <tr><td>{t('common.table.noResults')}</td></tr>}
            </tbody>
        </table>
    )
}
