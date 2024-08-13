import React from 'react'
import { Helmet } from 'react-helmet'
import { useTranslation } from 'react-i18next'

const PageInfo = ({ title = null }: { title?: string }) => {
    const { t } = useTranslation()

    return (
        <Helmet>
            <title>
                {t('common.layout.headerTitle') +
                    (title
                        ? t('common.helper.pageTitleDelimiter') + title
                        : '')}
            </title>
        </Helmet>
    )
}

export default PageInfo
