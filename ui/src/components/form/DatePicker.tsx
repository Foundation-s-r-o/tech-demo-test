import classNames from 'classnames'
import React from 'react'
import ReactDatePicker, {
    DatePickerProps,
    registerLocale,
} from 'react-datepicker'
import { sk } from 'date-fns/locale/sk'

registerLocale('sk', sk)

const FndtDatePicker = ({
    className,
    ...props
}: {
    className?: string | undefined
} & DatePickerProps) => {
    // TODO: decide whether the date format stays a fixed template default or comes from config.
    const dateFormat = 'd.M.yyyy'
    return (
        <ReactDatePicker
            locale="sk"
            dateFormat={dateFormat}
            className={classNames('form-control', className)}
            {...props}
        />
    )
}

export default FndtDatePicker
