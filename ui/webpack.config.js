const path = require('path')
const HTMLWebpackPlugin = require('html-webpack-plugin')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const webpack = require('webpack')
const Dotenv = require('dotenv-webpack')
const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin')

module.exports = (env, argv) => {
    const envFileVars = require('dotenv').config({ path: __dirname + '/../.env' }).parsed;
    const devMode = argv.mode === 'development'

    return {
        entry: './src/index.tsx',
        output: {
            path: path.join(__dirname, '/dist'),
            filename: 'bundle.js',
        },
        plugins: [
            // new Dotenv(),
            new webpack.DefinePlugin({
                'process.env.APP_API_SERVER_URL': '\'' + (process?.env?.APP_API_SERVER_URL || envFileVars?.APP_API_SERVER_URL || '/') + '\'',
            }),
            new HTMLWebpackPlugin({
                template: './src/index.html',
            }),
            new MiniCssExtractPlugin({
                filename: devMode ? '[name].css' : '[name].[contenthash].css',
                chunkFilename: devMode ? '[id].css' : '[id].[contenthash].css',
            }),
        ],
        devtool: 'eval-source-map',
        // watchOptions: {
        //     poll: 1000, // Check for changes every second
        // },
        devServer: {
            port: 9002,
            historyApiFallback: { index: '/', disableDotRule: true },
        },
        resolve: {
            extensions: ['.tsx', '.ts', '.js'],
            plugins: [new TsconfigPathsPlugin({})],
        },
        module: {
            rules: [
                {
                    test: /\.ts(x?)$/,
                    use: 'ts-loader',
                    exclude: /node_modules/,
                },
                {
                    test: /\.js$/,
                    exclude: /node_modules/,
                    use: {
                        loader: 'babel-loader',
                        options: {
                            presets: [
                                '@babel/preset-env',
                                '@babel/preset-react',
                            ],
                        },
                    },
                },
                {
                    test: /\.(sa|sc|c)ss$/,
                    use: [
                        MiniCssExtractPlugin.loader,
                        'css-loader',
                        'postcss-loader',
                        'sass-loader',
                    ],
                },
                {
                    test: /\.(png|jp(e*)g|svg|gif)$/,
                    use: [
                        {
                            loader: 'file-loader',
                            options: {
                                name: 'images/[hash]-[name].[ext]',
                            },
                        },
                    ],
                },
            ],
        },
    }
}
