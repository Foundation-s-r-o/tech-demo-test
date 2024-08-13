import * as Yup from 'yup'

const personFormSchema = Yup.object()
    .shape({
        firstName: Yup.string().nullable().required(),
        lastName: Yup.string().nullable().required(),
        address: Yup.string().nullable().required(),
        email: Yup.string().email().nullable().required(),
        phoneNumber: Yup.string().nullable(),
        state: Yup.string().nullable(),
    })

export default personFormSchema
