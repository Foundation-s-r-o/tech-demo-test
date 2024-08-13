import Yup from '@common/yup'

export const LoginFormSchema = Yup.object().shape({
    username: Yup.string().required(),
    password: Yup.string().required(),
})

export interface LoginFormValues {
    username: string
    password: string
}

export type LoginLocationState = {
    from: {
        pathname: string
    }
}

export type LoginFormProps = {
    onLoginSuccess: () => void
}
