import {
    GetFieldProps,
    getInputType,
    SelectOption,
    FndtInputType
} from '@components/form/types'
import FndtSelect from '@components/form/Select'
import { OnChangeValue } from 'react-select'
import { isFunction } from 'lodash'
import React from 'react'
import FndtCheckboxWithLabel from '@components/form/CheckboxWithLabel'
import { Field, useField, useFormikContext } from 'formik'
import { useTranslation } from 'react-i18next'

export const FndtFormField = ({
    type,
    name,
    id,
    fieldElement = null,
    options,
    onChange,
    isClearable,
    isDisabled,
    defaultValue,
    datePickerProps = null,
}: GetFieldProps) => {
    const { t } = useTranslation()
    const { setFieldValue } = useFormikContext()
    const inputType = getInputType(type)
    const [field] = useField({ name: name })

    const getAdditionalFieldProps = () => {
        switch (type) {
            case FndtInputType.DatePicker:
                return datePickerProps ? datePickerProps : {}
            default:
                return {}
        }
    }
    switch (type) {
        case FndtInputType.Select:
            return (
                <FndtSelect
                    {...{ options, defaultValue, isClearable, isDisabled }}
                    value={options.find((element) => element.value === field.value) || null}
                    onChange={(value: OnChangeValue<SelectOption, false>) => {
                        const newVal = value ? value.value : null
                        setFieldValue(name, newVal)
                        if (isFunction(onChange)) {
                            onChange(newVal)
                        }
                    }}
                    id={id}
                    name={name}
                />
            )

        case FndtInputType.Custom:
            return React.cloneElement(fieldElement, {
                name: name,
                id: id,
                value: field.value,
                onChange: (value: unknown) => {
                    setFieldValue(name, value)
                    if (isFunction(fieldElement.props.onChange)) {
                        fieldElement.props.onChange(value)
                    }
                },
            })

        case FndtInputType.MultipleChecbkoxes:
            return (
                <>
                    {options.map((item) => {
                        const localId = id + '_' + item.value
                        return (
                            <FndtCheckboxWithLabel
                                key={localId}
                                value={field.value.indexOf(item.value) > -1}
                                onChange={(checked: boolean) => {
                                    const currentSelected = [...field.value]
                                    if (checked && field.value.indexOf(item.value) === -1) {
                                        currentSelected.push(item.value)
                                    } else if (field.value.indexOf(item.value) > -1) {
                                        currentSelected.splice(currentSelected.indexOf(item.value), 1)
                                    }
                                    setFieldValue(name, currentSelected)
                                }}
                                label={t(item.label)}
                                name={name}
                                id={localId}
                            />
                        )
                    })}
                </>
            )
        default:
            return (
                <Field
                    as={inputType}
                    disabled={isDisabled}
                    {...getAdditionalFieldProps()}
                    id={id}
                    name={name}
                />
            )
    }
}

export default FndtFormField
