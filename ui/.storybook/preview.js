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
}

export const initialGlobals = {
    locale: 'sk',
    locales: { sk: 'Slovensky' },
    theme: 'light',
}

export const globalTypes = {
    theme: {
        description: 'Color mode (Bootstrap data-bs-theme)',
        toolbar: {
            title: 'Theme',
            icon: 'mirror',
            items: ['light', 'dark'],
            dynamicTitle: true,
        },
    },
}

const withTheme = (Story, context) => {
    document.documentElement.dataset.bsTheme = context.globals.theme || 'light'
    return Story()
}

export const decorators = [
    withTheme,
    withRouter,
    // (Story) => (
    //     <ContextProviders>
    //         <Story />
    //     </ContextProviders>
    // ),
]
export const tags = ['autodocs'];
