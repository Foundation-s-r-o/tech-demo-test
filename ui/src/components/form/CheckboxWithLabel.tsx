import React from 'react'
import classNames from 'classnames'
import { uniqueId } from 'lodash'
import { useTranslation } from 'react-i18next'

type FndtCheckboxWithLabelProps = {
    value: boolean,
    onChange: (newValue: boolean) => void,
    label: string,
    name: string,
    className?: string,
    id?: string,
}

const FndtCheckboxWithLabel = ({value, onChange, label, name, className = null, id = null}: FndtCheckboxWithLabelProps) => {
    const { t } = useTranslation()
    const idAttribute = id || uniqueId('fndtcheckbox')

    return (
        <div className="form-check">
            <input
                type="checkbox"
                checked={value}
                onChange={(e) => {
                    onChange(e.target.checked)
                }}
                className={classNames('form-check-input', className)}
                id={idAttribute}
                name={name}
            />
            <label
                className="form-check-label"
                htmlFor={idAttribute}>
                {t(label)}
            </label>
        </div>
    )
}

export default FndtCheckboxWithLabel
