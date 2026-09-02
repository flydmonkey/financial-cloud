import type {ElMessageBoxOptions} from 'element-plus'
import modal from '@/plugins/modal'
import tab from '@/plugins/tab'
import auth from '@/plugins/auth'
import cache from '@/plugins/cache'
import downloadPlugin from '@/plugins/download'
import {useDict} from '@/utils/Dict'
import {
    addDateRange,
    handleTree,
    parseTime,
    resetForm,
    selectDictLabel,
    selectDictLabels,
} from '@/utils/financialCloud'
import {download} from '@/utils/Request'

declare module '@vue/runtime-core' {
    interface ComponentCustomProperties {
        $modal: typeof modal
        $tab: typeof tab
        $auth: typeof auth
        $cache: typeof cache
        $download: typeof downloadPlugin
        useDict: typeof useDict
        download: typeof download
        parseTime: typeof parseTime
        resetForm: typeof resetForm
        handleTree: typeof handleTree
        addDateRange: typeof addDateRange
        selectDictLabel: typeof selectDictLabel
        selectDictLabels: typeof selectDictLabels
        $t: (key: string, ...args: unknown[]) => string
        $confirm: (
            message: string,
            title: string,
            options?: ElMessageBoxOptions,
        ) => Promise<unknown>
    }
}

interface ImportMeta {
    glob: (pattern: string, options?: { eager?: boolean; import?: boolean }) => Record<string, () => Promise<unknown>>
    env: ImportMetaEnv
}

interface Navigator {
    browserLanguage?: string
}

declare module '*.vue' {
    import type {DefineComponent} from 'vue'
    const component: DefineComponent<object, object, unknown>
    export default component
}

declare module '*.png' {
    const value: string
    export default value
}

declare module '*.module.scss' {
    const content: Record<string, string>
    export default content
}

declare module '*.json' {
    const value: unknown
    export default value
}

export {}
