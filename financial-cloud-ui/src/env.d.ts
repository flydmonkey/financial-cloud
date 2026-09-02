/// <reference types="vite/client" />

declare const __APP_VERSION__: string

interface ImportMetaEnv {
    readonly VITE_APP_BASE_API: string
    readonly VITE_APP_ENV?: string
    readonly VITE_APP_CONTEXT_PATH?: string
}

interface ImportMeta {
    readonly env: ImportMetaEnv
}
