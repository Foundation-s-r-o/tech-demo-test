const { FlatCompat } = require('@eslint/eslintrc')
const js = require('@eslint/js')
const rc = require('./.eslintrc.json')

const compat = new FlatCompat({
    baseDirectory: __dirname,
    recommendedConfig: js.configs.recommended,
})

module.exports = [...compat.config(rc)]