import React from 'react'
import { useTranslation } from 'react-i18next'

const PageInfo = ({ title = null }: { title?: string }) => {
    const { t } = useTranslation()

    // React 19 hoists <title> rendered anywhere in the tree to <head> — no react-helmet needed.
    const pageTitle =
        t('common.layout.headerTitle') +
        (title ? t('common.helper.pageTitleDelimiter') + title : '')

    return <title>{pageTitle}</title>
}

export default PageInfo
