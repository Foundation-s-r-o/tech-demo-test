import React from 'react'
import { useFormikContext } from 'formik'
import { ReactDatePickerProps } from 'react-datepicker'
import FndtDatePicker from './DatePicker'

// Define a simple interface with just what we need
interface CustomDatePickerProps {
    name?: string;
    value?: Date | string;
    onChange?: (date: Date | null) => void;
    [key: string]: any; // Allow any other props to pass through
}

const DatePickerField = (props: CustomDatePickerProps) => {
    const { setFieldValue } = useFormikContext()

    return (
        <FndtDatePicker
            {...props as any}
            selected={(props.value && new Date(props.value)) || null}
            onChange={(val: Date) => {
                if (props.name) {
                    setFieldValue(props.name, val)
                }
            }}
        />
    )
}

export default DatePickerField
