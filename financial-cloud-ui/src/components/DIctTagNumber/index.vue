<template>
  <div>
    <template v-for="(item, index) in typedOptions" :key="item.value">
      <template v-if="values.includes(item.value)">
        <span
          v-if="(item.elTagType == 'default' || item.elTagType == '') && (item.elTagClass == '' || item.elTagClass == null)"
          :index="index"
          :class="item.elTagClass"
        >{{ item.label + " " }}</span>
        <el-tag
          v-else
          :disable-transitions="true"
          :index="index"
          :type="item.elTagType === 'primary' ? '' : item.elTagType"
          :class="item.elTagClass"
        >
          {{ item.label + " " }}
        </el-tag>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'

interface DictOption {
  value: string | number
  label: string
  elTagType?: string
  elTagClass?: string | null
}

const props = defineProps<{
  options?: DictOption[] | null
  value?: number | string | Array<string | number> | null
}>()

const typedOptions = computed<DictOption[]>(() => props.options ?? [])

const values = computed(() => {
  if (props.value !== null && typeof props.value !== 'undefined') {
    return Array.isArray(props.value) ? props.value : [props.value]
  }
  return [] as Array<string | number>
})
</script>
<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
