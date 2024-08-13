import { useFormikContext } from 'formik'
import React, { PropsWithChildren, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import FndtFormErrorMessage from '@components/shared/forms/ErrorMessage'
import InputRow from '@components/shared/forms/InputRow'
import FndtLabel from './Label'
import { SegmentInputCombined, } from './types'
import FndtFormField from '@components/form/FormField'

const FndtInputSegment = ({
  idPrefix,
  name,
  type,
  options,
  namePrefix = null,
  label = null,
  labelElement = null,
  onChange = null,
  defaultValue = null,
  isDisabled = false,
  isClearable = false,
  fieldElement = null,
  inputWrapperClassName = null,
  datePickerProps = null,
  children,
}: PropsWithChildren<SegmentInputCombined>) => {
    const { t } = useTranslation()
    const { setFieldValue } = useFormikContext()
    const id = `${idPrefix}_${namePrefix ? namePrefix + '_' : ''}${name}`.replace(/\./g, '_')
    useEffect(() => {
        if (defaultValue) {
            setFieldValue(name, defaultValue.value)
        }
    }, [defaultValue])

    return (
        <InputRow>
            <FndtLabel htmlFor={id}>{labelElement ? labelElement : t(label)}</FndtLabel>
            <div className={inputWrapperClassName ? inputWrapperClassName : ''}>
                {children ? (
                    <div {...{ id, className: 'form-value' }}>{children}</div>
                ) : (
                    <>
                        <FndtFormField
                            {...{ type, id, options, onChange, defaultValue, isClearable, fieldElement, datePickerProps, isDisabled }}
                            name={`${namePrefix ? namePrefix + '.' : ''}${name}`}
                        />
                        <FndtFormErrorMessage name={`${namePrefix ? namePrefix + '.' : ''}${name}`} />
                    </>
                )}
            </div>
        </InputRow>
    )
}

export default FndtInputSegment
