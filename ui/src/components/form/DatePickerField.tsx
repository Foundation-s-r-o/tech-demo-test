import React from 'react'
import { useFormikContext } from 'formik'
import { ReactDatePickerProps } from 'react-datepicker'
import FndtDatePicker from './DatePicker'

const DatePickerField = (props: ReactDatePickerProps) => {
    const { setFieldValue } = useFormikContext()

    return (
        <FndtDatePicker
            {...props}
            selected={(props.value && new Date(props.value)) || null}
            onChange={(val) => {
                setFieldValue(props.name, val)
            }}
        />
    )
}

export default DatePickerField
