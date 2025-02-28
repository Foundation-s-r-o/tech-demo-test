import React from 'react'
import { useFormikContext } from 'formik'
import FndtDatePicker from './DatePicker'

// Define a simple interface with just what we need
interface CustomDatePickerProps {
    name?: string;
    value?: Date | string;
    onChange?: (date: Date | null) => void;
    className?: string;
}

const DatePickerField = (props: CustomDatePickerProps) => {
    const { setFieldValue } = useFormikContext()

    return (
        <FndtDatePicker
            className={props.className}
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
