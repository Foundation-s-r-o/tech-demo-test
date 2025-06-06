import FndtTextArea from '@components/form/TextArea'
import { ReactElement } from 'react'
import { DatePickerProps } from 'react-datepicker'
import FndtCheckbox from './Checkbox'
import DatePickerField from './DatePickerField'
import FndtInput from './Input'
import FndtInputNumber from './InputNumber'
import FndtSelect from './Select'
import FndtText from './Text'
import FndtTextDate from './TextDate'

export type SelectOption = {
    value: string | boolean
    label: string
}

export enum FndtInputType {
    TextDate = 'TextDate',
    Text = 'Text',
    TextArea = 'TextArea',
    Input = 'Input',
    Select = 'Select',
    DatePicker = 'DatePicker',
    Checkbox = 'Checkbox',
    InputNumber = 'InputNumber',
    Custom = 'Custom',
    MultipleChecbkoxes = 'MultipleChecbkoxes',
}

interface SegmentPropsGeneric {
    namePrefix?: string | null
    idPrefix: string
    name: string
    label?: string | null
    onChange?: (value: unknown) => void
    options?: SelectOption[]
    isDisabled?: boolean
    isClearable?: boolean
    defaultValue?: SelectOption
    fieldElement?: ReactElement<any, any>
    labelElement?: ReactElement<any, any>
    inputWrapperClassName?: string
    datePickerProps?: Partial<DatePickerProps> | null
}

interface SegmentPropsForCustom extends SegmentPropsGeneric {
    type: FndtInputType.Custom
    fieldElement: ReactElement<any, any>
}

interface SegmentPropsForText extends SegmentPropsGeneric {
    type: FndtInputType.Text
}
interface SegmentPropsForTextDate extends SegmentPropsGeneric {
    type: FndtInputType.TextDate
}

interface SegmentPropsForTextArea extends SegmentPropsGeneric {
    type: FndtInputType.TextArea
}

interface SegmentPropsForInput extends SegmentPropsGeneric {
    type: FndtInputType.Input
}

interface SegmentPropsForInputNumber extends SegmentPropsGeneric {
    type: FndtInputType.InputNumber
}

interface SegmentPropsForSelect extends SegmentPropsGeneric {
    options: SelectOption[]
    type: FndtInputType.Select
}

interface SegmentPropsForDate extends SegmentPropsGeneric {
    type: FndtInputType.DatePicker
}

interface SegmentPropsForCheckbox extends SegmentPropsGeneric {
    type: FndtInputType.Checkbox
}

interface SegmentPropsForMultipleCheckboxes extends SegmentPropsGeneric {
    options: SelectOption[]
    type: FndtInputType.MultipleChecbkoxes
}

export interface GetFieldProps {
    id: string
    name: string
    fieldElement?: ReactElement<any, any>
    isDisabled?: boolean
    isClearable?: boolean
    defaultValue?: SelectOption
    onChange?: (value: unknown) => void
    options?: SelectOption[]
    type: FndtInputType
    datePickerProps?: Partial<DatePickerProps> | null
}

export type SegmentInputCombined =
    | SegmentPropsForText
    | SegmentPropsForTextDate
    | SegmentPropsForTextArea
    | SegmentPropsForInput
    | SegmentPropsForInputNumber
    | SegmentPropsForCheckbox
    | SegmentPropsForMultipleCheckboxes
    | SegmentPropsForDate
    | SegmentPropsForSelect
    | SegmentPropsForCustom

export const getInputType = (type: FndtInputType) => {
    switch (type) {
        case FndtInputType.Input:
            return FndtInput
        case FndtInputType.TextArea:
            return FndtTextArea
        case FndtInputType.InputNumber:
            return FndtInputNumber
        case FndtInputType.Select:
            return FndtSelect
        case FndtInputType.Checkbox:
            return FndtCheckbox
        case FndtInputType.DatePicker:
            return DatePickerField
        case FndtInputType.TextDate:
            return FndtTextDate
        default:
            return FndtText
    }
}
