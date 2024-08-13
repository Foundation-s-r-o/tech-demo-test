import { ErrorMessage, useFormikContext } from 'formik'
import React from 'react'
import { useTranslation } from 'react-i18next'

const FndtFormErrorMessage = ({ name }: { name: string }) => {
    const formikContext = useFormikContext()
    const { t } = useTranslation()

    return (
        formikContext.submitCount > 0 && (
            <ErrorMessage name={name}>
                {(msg) => <div className="text-danger">{t(msg)}</div>}
            </ErrorMessage>
        )
    )
}

export default FndtFormErrorMessage
