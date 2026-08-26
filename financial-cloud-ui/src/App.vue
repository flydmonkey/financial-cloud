<template>
  <el-config-provider :locale="locale" :size="elementPlusSize">
    <RouterView/>
  </el-config-provider>
</template>

<script setup lang="ts">
import useSettingsStore from '@/store/modules/settings'
import {handleThemeStyle} from '@/utils/Theme'
import {ElConfigProvider} from 'element-plus'
import {onMounted, nextTick, ref, computed} from 'vue'
import elZhCN from 'element-plus/es/locale/lang/zh-cn'
import elZhTW from 'element-plus/es/locale/lang/zh-tw'
import elEnUS from 'element-plus/es/locale/lang/en'
import Cookies from 'js-cookie'
import {getLocale} from '@/languages'

const elementPlusSize = Cookies.get('size') || 'default'

const language = ref('zh-CN')
language.value = getLocale()
const locale = computed(() => (language.value === 'zh-CN' ? elZhCN : language.value === 'en-US' ? elEnUS : elZhTW))

onMounted(() => {
  nextTick(() => {
    handleThemeStyle(useSettingsStore().theme)
  })
})
</script>
