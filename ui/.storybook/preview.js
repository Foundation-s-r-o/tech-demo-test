import { withRouter } from 'storybook-addon-react-router-v6'
import 'bootstrap/dist/js/bootstrap.bundle.min.js'
import '../src/assets/sass/main.scss'
import ContextProviders from '../src/components/shared/ContextProviders'
import i18n from '../src/common/i18n'

export const parameters = {
    actions: { argTypesRegex: '^on[A-Z].*' },
    controls: {
        matchers: {
            color: /(background|color)$/i,
            date: /Date$/,
        },
    },

    // Required by the storybook-react-i18next addon (the app's i18next instance).
    i18n,

    paddings: {
        values: [
            { name: 'None', value: '0' },
            { name: 'Small', value: '16px' },
            { name: 'Medium', value: '32px' },
            { name: 'Large', value: '64px' },
        ],
        default: 'None',
    },
}

export const initialGlobals = {
    locale: 'sk',
    locales: { sk: 'Slovensky' },
}

export const decorators = [
    withRouter,
    // (Story) => (
    //     <ContextProviders>
    //         <Story />
    //     </ContextProviders>
    // ),
]
