import i18n from '@common/i18n'

export const oneOfMultipleRequiredErrorMsg = (fieldsNameKeys: string[]) =>
    i18n.t('common.error.oneOfRequired', {
        requiredFields: fieldsNameKeys
            .map((fieldName) => i18n.t(fieldName))
            .join(', '),
    })
export const oneOfMultipleRequiredTestFn = function (fieldsToCheck: string[]) {
    return function (item: unknown) {
        if (item) {
            let isAnyValid = false
            fieldsToCheck.forEach((fieldKey) => {
                if (this.parent[fieldKey]) {
                    isAnyValid = true
                }
            })
            if (!isAnyValid) {
                return false
            }
        }
        return true
    }
}
