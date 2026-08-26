import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import globals from 'globals'
import vueParser from 'vue-eslint-parser'
import autoImportGlobals from './.eslintrc-auto-import.json' with {type: 'json'}

export default tseslint.config(
    {ignores: ['dist/**', 'node_modules/**', 'src/assets/iconfont/**']},
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
            'no-unsafe-optional-chaining': 'warn',
        },
    }
)
