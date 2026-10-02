<template>
  <el-card
    class="accounting-guide"
    shadow="never"
  >
    <template #header>
      <div class="guide-header">
        <strong>做账指引</strong>
        <el-button
          link
          type="primary"
          :aria-expanded="expanded"
          @click="expanded = !expanded"
        >
          {{ expanded ? '收起指引' : '展开指引' }}
        </el-button>
      </div>
    </template>
    <p class="guide-summary">
      按以下顺序完成建账到月末交付。指引不代表各项业务已经完成。
    </p>
    <ol
      v-if="expanded"
      class="guide-stages"
    >
      <li
        v-for="stage in accountingGuide"
        :key="stage.title"
      >
        <strong>{{ stage.title }}</strong>
        <p>{{ stage.description }}</p>
        <div class="guide-actions">
          <el-button
            v-for="link in availableLinks(stage.links)"
            :key="link.path"
            link
            type="primary"
            @click="router.push(link.path)"
          >
            {{ link.label }}
          </el-button>
          <span v-if="!availableLinks(stage.links).length">需要相应页面权限，请联系账套管理员。</span>
        </div>
      </li>
    </ol>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { accountingGuide } from '@/utils/accountingGuide'

const router = useRouter()
const expanded = ref(true)
function availableLinks(links: Array<{ label: string; path: string }>) {
  return links.filter(link => router.getRoutes().some(route => route.path === link.path))
}
</script>

<style scoped>
.accounting-guide { margin-bottom: 20px; }
.guide-header { display: flex; align-items: center; justify-content: space-between; }
.guide-summary, .guide-stages p { color: var(--el-text-color-regular); line-height: 1.7; }
.guide-summary { margin: 0 0 12px; }
.guide-stages { padding: 0; margin: 0; list-style: none; display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
.guide-stages li { padding: 12px; background: var(--el-fill-color-light); border-radius: 6px; }
.guide-stages p { font-size: 13px; }
.guide-actions { display: flex; flex-wrap: wrap; gap: 8px; font-size: 13px; color: var(--el-text-color-secondary); }
</style>
