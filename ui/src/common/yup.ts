import * as yup from 'yup'

yup.setLocale({
    mixed: {
        required: 'common.error.requiredField',
    },
})

export default yup
