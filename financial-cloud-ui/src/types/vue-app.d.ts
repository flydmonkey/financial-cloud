import type {ComponentPublicInstance} from 'vue'

declare module 'vue' {
    interface ComponentInternalInstance {
        proxy: ComponentPublicInstance & Record<string, unknown>
    }
}

export {}
