import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import globals from 'globals'
import vueParser from 'vue-eslint-parser'
import autoImportGlobals from './.eslintrc-auto-import.json' with {type: 'json'}

export default tseslint.config(
    {
        ignores: [
            'dist/**',
            'node_modules/**',
            'src/assets/iconfont/**',
            '.playwright-browsers/**',
            'test-results/**',
            'playwright-report/**',
        ],
    },
    js.configs.recommended,
    ...pluginVue.configs['flat/recommended'],
    ...tseslint.configs.recommended,
    {
        files: ['**/*.vue'],
        languageOptions: {
            parser: vueParser,
            parserOptions: {
                parser: tseslint.parser,
                ecmaVersion: 'latest',
                sourceType: 'module',
                extraFileExtensions: ['.vue'],
            },
            globals: {
                ...globals.browser,
                ...globals.node,
                ...autoImportGlobals.globals,
                __APP_VERSION__: 'readonly',
            },
        },
    },
    {
        files: ['**/*.{js,mjs,cjs,ts}'],
        languageOptions: {
            ecmaVersion: 'latest',
            sourceType: 'module',
            globals: {
                ...globals.browser,
                ...globals.node,
                ...autoImportGlobals.globals,
                __APP_VERSION__: 'readonly',
            },
        },
    },
    {
        rules: {
            'no-console': ['warn', {allow: ['warn', 'error']}],
            '@typescript-eslint/no-explicit-any': 'off',
            '@typescript-eslint/no-unused-vars': ['warn', {argsIgnorePattern: '^_'}],
            'vue/multi-word-component-names': 'off',
            'vue/no-ref-as-operand': 'warn',
            'vue/no-deprecated-v-on-native-modifier': 'warn',
            'vue/no-deprecated-v-bind-sync': 'warn',
            'vue/no-deprecated-slot-attribute': 'warn',
            'vue/no-deprecated-filter': 'warn',
            'vue/no-side-effects-in-computed-properties': 'warn',
            'vue/valid-define-emits': 'warn',
            'vue/no-mutating-props': 'warn',
            'vue/no-reserved-component-names': 'warn',
            'vue/require-valid-default-prop': 'warn',
            'vue/valid-v-for': 'warn',
            'vue/require-v-for-key': 'warn',
            'vue/no-unused-vars': 'warn',
            'vue/no-use-v-if-with-v-for': 'warn',
            'vue/no-dupe-keys': 'warn',
            '@typescript-eslint/no-unused-expressions': 'warn',
            '@typescript-eslint/no-empty-object-type': 'warn',
            '@typescript-eslint/no-this-alias': 'warn',
            '@typescript-eslint/no-unsafe-function-type': 'warn',
            'no-prototype-builtins': 'warn',
            'prefer-const': 'warn',
            'no-empty': 'warn',
            'no-useless-escape': 'warn',
            'no-undef': 'warn',
            'no-unsafe-optional-chaining': 'warn',
        },
    }
)
