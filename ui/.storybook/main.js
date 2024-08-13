const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin')
const webpack = require('webpack')
const dotenv = require('dotenv')

module.exports = {
    typescript: {
        reactDocgen: 'any-string',
    },
    stories: [
        '../src/**/*.stories.mdx',
        '../src/**/*.stories.@(js|jsx|ts|tsx)',
    ],
    addons: [
        '@storybook/addon-links',
        '@storybook/addon-essentials',
        '@storybook/addon-interactions',
        '@storybook/preset-scss',
        '@storybook-addon-react-router-v6',
        '@storybook-react-i18next',
        '@storybook-addon-paddings',
    ],
    framework: '@storybook/react',
    core: {
        builder: '@storybook/builder-webpack5',
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
