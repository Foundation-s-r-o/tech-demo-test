const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin')
const webpack = require('webpack')
const dotenv = require('dotenv')

module.exports = {
    typescript: {
        reactDocgen: 'react-docgen-typescript',
        reactDocgenTypescriptOptions: {
            shouldExtractLiteralValuesFromEnum: true,
            propFilter: (prop) => (prop.parent ? !/node_modules/.test(prop.parent.fileName) : true),
        },
    },
    stories: [
        '../src/**/*.stories.mdx',
        '../src/**/*.stories.@(js|jsx|ts|tsx)',
    ],
    addons: [
        '@storybook/addon-links',
        '@storybook/addon-essentials',
        '@storybook/addon-interactions',
        'storybook-addon-react-router-v6',
        'storybook-react-i18next',
        'storybook-addon-paddings',
    ],
    framework: {
        name: '@storybook/react-webpack5',
        options: {},
    },
    docs: {
        autodocs: true,
    },
    staticDirs: ['../src/assets'],
    webpackFinal: async (config) => {
        const envFileVars = dotenv.config({ path: __dirname + '/../../.env' }).parsed
        const apiServerUrl = process?.env?.APP_API_SERVER_URL || envFileVars?.APP_API_SERVER_URL || '/'
        // config.devServer = {
        //     ...config.devServer,
        //     port: 9002,
        // }
        config.plugins = [
            ...config.plugins,
            new webpack.DefinePlugin({
                'process.env.APP_API_SERVER_URL': JSON.stringify(apiServerUrl),
            })
        ]
        config.resolve.plugins = [
            ...(config.resolve.plugins || []),
            new TsconfigPathsPlugin({
                extensions: config.resolve.extensions,
            }),
        ]
        return config
    },
}
