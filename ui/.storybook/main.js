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
        'storybook-addon-react-router-v6',
        'storybook-react-i18next',
        '@storybook/addon-docs'
    ],

    framework: {
        name: '@storybook/react-webpack5',
        options: {},
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
        // The root .babelrc only enables preset-react, so Storybook's default
        // babel-loader cannot parse TypeScript/JSX in stories or imported app
        // code. Add an explicit TS/TSX rule with the full preset set, plus an
        // SCSS rule (Storybook does not add one by default).
        config.module.rules.unshift({
            test: /\.(ts|tsx)$/,
            exclude: /node_modules/,
            use: {
                loader: require.resolve('babel-loader'),
                options: {
                    presets: [
                        ['@babel/preset-env', { shippedProposals: true, loose: true }],
                        '@babel/preset-typescript',
                        ['@babel/preset-react', { runtime: 'automatic' }],
                    ],
                },
            },
        })
        config.module.rules.push({
            test: /\.scss$/,
            use: ['style-loader', 'css-loader', 'sass-loader'],
        })
        if (!config.resolve.extensions.includes('.ts')) {
            config.resolve.extensions.push('.ts', '.tsx')
        }
        return config
    }
}
