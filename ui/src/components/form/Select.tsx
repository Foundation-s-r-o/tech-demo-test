import React from 'react'
import { useTranslation } from 'react-i18next'
import Select, { GroupBase, Props } from 'react-select'

const FndtSelect = <
    Option,
    IsMulti extends boolean = false,
    Group extends GroupBase<Option> = GroupBase<Option>
>(
    props: Props<Option, IsMulti, Group>
) => {
    const { t } = useTranslation()
    return (
        <Select
            placeholder={props.placeholder || t('form.option.defaultSelectPlaceholder')}
            {...props}
        />
    )
}

export default FndtSelect
