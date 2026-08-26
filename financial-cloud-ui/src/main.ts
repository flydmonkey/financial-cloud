import {createApp} from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import i18n from '@/languages'
import '@/assets/styles/index.scss'

import App from './App.vue'
import store from './store'
import router from './router'
import directive from './directive'

import plugins from './plugins'
import {download} from '@/utils/Request'

import 'virtual:svg-icons-register'
import SvgIcon from '@/components/SvgIcon/index.vue'

import '@/assets/iconfont/iconfont.css'

import './permission'

import {useDict} from '@/utils/Dict'
import {parseTime, resetForm, addDateRange, handleTree, selectDictLabel, selectDictLabels} from '@/utils/Jinbooks'

import Pagination from '@/components/Pagination/index.vue'
import RightToolbar from '@/components/RightToolbar/index.vue'
import Editor from '@/components/Editor/index.vue'
import FileUpload from '@/components/FileUpload/index.vue'
import ImageUpload from '@/components/ImageUpload/index.vue'
import ImagePreview from '@/components/ImagePreview/index.vue'
import TreeSelect from '@/components/TreeSelect/index.vue'
import DictTag from '@/components/DictTag/index.vue'
import DictTagNumber from '@/components/DIctTagNumber/index.vue'

import './index.css'

import modal from './plugins/modal'
import registerElementPlusIcons from './plugins/element-icons'

const app = createApp(App)

app.use(ElementPlus)
registerElementPlusIcons(app)

app.config.globalProperties.useDict = useDict
app.config.globalProperties.download = download
app.config.globalProperties.parseTime = parseTime
app.config.globalProperties.resetForm = resetForm
app.config.globalProperties.handleTree = handleTree
app.config.globalProperties.addDateRange = addDateRange
app.config.globalProperties.selectDictLabel = selectDictLabel
app.config.globalProperties.selectDictLabels = selectDictLabels
app.config.globalProperties.$modal = modal

app.component('DictTag', DictTag)
app.component('DictTagNumber', DictTagNumber)
app.component('Pagination', Pagination)
app.component('TreeSelect', TreeSelect)
app.component('FileUpload', FileUpload)
app.component('ImageUpload', ImageUpload)
app.component('ImagePreview', ImagePreview)
app.component('RightToolbar', RightToolbar)
app.component('Editor', Editor)

app.use(router)
app.use(store)
app.use(plugins)
app.component('SvgIcon', SvgIcon)

directive(app)

app.use(i18n)
app.mount('#app')
