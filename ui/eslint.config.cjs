// For more info, see https://github.com/storybookjs/eslint-plugin-storybook#configuration-flat-config-format
const storybook = require('eslint-plugin-storybook')

const { FlatCompat } = require('@eslint/eslintrc')
const js = require('@eslint/js')
const rc = require('./.eslintrc.json')

const compat = new FlatCompat({
    baseDirectory: __dirname,
    recommendedConfig: js.configs.recommended,
})

module.exports = [...compat.config(rc), ...storybook.configs['flat/recommended']]