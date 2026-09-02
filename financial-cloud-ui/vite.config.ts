import {defineConfig, loadEnv} from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'
import {version} from "./package.json"

// https://vitejs.dev/config/
export default defineConfig(({mode, command}) => {
    const env = loadEnv(mode, process.cwd())
    const {VITE_APP_ENV, VITE_APP_CONTEXT_PATH} = env
    return {
        base: VITE_APP_CONTEXT_PATH || '/',
        define: {
            __APP_VERSION__: JSON.stringify(version), // 必须转为字符串
        },
        plugins: createVitePlugins(env, command === 'build'),
        resolve: {
            // https://cn.vitejs.dev/config/#resolve-alias
            alias: {
                // 设置路径
                '~': path.resolve(__dirname, './'),
                // 设置别名
                '@': path.resolve(__dirname, './src')
            },
            // https://cn.vitejs.dev/config/#resolve-extensions
            extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
        },
        // vite 相关配置
        server: {
            port: 3154,
            host: true,
            open: false,
            proxy: {
                '/api': {
                    target: 'http://localhost:2154',
                    changeOrigin: true,
                    rewrite: (p) => p
                }
            }
        },
        //fix:error:stdin>:7356:1: warning: "@charset" must be the first rule in the file
        css: {
            preprocessorOptions: {
                scss: {
                    api: 'modern-compiler',
                    silenceDeprecations: ['legacy-js-api']
                },
            },
            postcss: {
                plugins: [
                    {
                        postcssPlugin: 'internal:charset-removal',
                        AtRule: {
                            charset: (atRule) => {
                                if (atRule.name === 'charset') {
                                    atRule.remove();
                                }
                            }
                        }
                    }
                ]
            }
        },
        build: {
            rollupOptions: {
                output: {
                    manualChunks(id) {
                        if (id.includes('node_modules')) {
                            if (id.includes('element-plus')) return 'element-plus'
                            if (id.includes('echarts')) return 'echarts'
                            if (id.includes('vue') || id.includes('pinia') || id.includes('@vue')) return 'vue-vendor'
                            return 'vendor'
                        }

                        const moduleNames = [
                            'voucher', 'hr', 'statement', 'standard', 'book',
                            'journal', 'settlement', 'config', 'idm', 'audit',
                            'dashboard', 'permissions', 'security'
                        ]
                        for (const name of moduleNames) {
                            if (id.includes(`/src/views/${name}/`) || id.includes(`/src/api/${name}/`)) {
                                return `module-${name}`
                            }
                        }
                    }
                }
            }
        }
    }
})
