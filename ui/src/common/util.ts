import i18n from '@common/i18n'

export function enumToSelectOptions(
    enumType: any, // eslint-disable-line
    translate: (param: string) => string
): Array<{ value: string; label: string }> {
    return Object.keys(enumType as object).map((val) => {
        return {
            value: val,
            label: translate(`form.option.${val}`),
        }
    })
}

export function arrayToSelectOptions(
    arrayToTransfer: string[],
    translationPrefix: string = null
): Array<{ value: string; label: string }> {
    return arrayToTransfer.map((val) => {
        return {
            value: val,
            label: i18n.t(`${translationPrefix ? translationPrefix : 'form.option'}.${val}`),
        }
    })
}

export function formatPrice(price: number) {
    const fraction = price.toFixed(2).split('.')[1]
    const priceFormatted = parseInt(price.toString()).toLocaleString().replace(',', ' ')
    return priceFormatted + `,${('00' + fraction).slice(-2)}`
}

export function formatDate(date: Date | string) {
    let resultDate = ''
    if (date) {
        if (date instanceof Date) {
            const day = date.getDate()
            const month = date.getMonth() + 1
            const year = date.getFullYear()

            resultDate = `${day}.${month}.${year}`
        } else if (isDate(date)) {
            const createdDate = new Date(date)
            const day = createdDate.getDate()
            const month = createdDate.getMonth() + 1
            const year = createdDate.getFullYear()

            resultDate = `${day}.${month}.${year}`
        }
    }

    return resultDate
}

export const isDate = function (date: string) {
    return new Date(date).toString() !== 'Invalid Date'
}

export function formatDateForRodneCislo(date: Date) {
    const day = ('0' + date.getDate()).slice(-2)
    const month = ('0' + (date.getMonth() + 1)).slice(-2)
    const year = date.getFullYear()

    return `${year}${month}${day}`
}

export const tabIsOpen = (activeTabs: string[], tabId: string) => {
    return activeTabs.find((ev) => {
        return ev == tabId
    })
}

export const getDateValue = (value?: string | Date) => {
    return value instanceof Date
        ? value.toLocaleDateString()
        : value
}
