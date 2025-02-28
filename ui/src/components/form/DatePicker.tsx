import classNames from 'classnames'
import React from 'react'
import ReactDatePicker, {
    ReactDatePickerProps,
    registerLocale,
} from 'react-datepicker'
import sk from 'date-fns/locale/sk'

registerLocale('sk', sk)

const FndtDatePicker = ({
    className,
    ...props
}: {
    className?: string | undefined
} & ReactDatePickerProps) => {
    const dateFormat = 'd.M.yyyy' // todo: get from env/db??
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
