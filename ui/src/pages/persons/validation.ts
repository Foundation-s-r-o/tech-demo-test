import * as Yup from 'yup'

// Registry person creation: identity core is required; national id and titles optional.
const personFormSchema = Yup.object()
    .shape({
        givenNames: Yup.string().nullable().required(),
        familyName: Yup.string().nullable().required(),
        sex: Yup.string().nullable().required(),
        dateOfBirth: Yup.string().nullable().required(),
        citizenship: Yup.string().nullable().required(),
        birthSurname: Yup.string().nullable(),
        nationalId: Yup.string().nullable(),
    })

export default personFormSchema
