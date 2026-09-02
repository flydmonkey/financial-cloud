<template>
  <div>
    <template v-for="(item, index) in typedOptions" :key="item.value">
      <template v-if="values.includes(String(item.value))">
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
    <template v-if="unmatch && showValue">
      {{ handleArray(unmatchArray) }}
    </template>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from "vue";

interface DictOption {
  value: string | number
  label: string
  elTagType?: string
  elTagClass?: string | null
}

const props = defineProps<{
  options?: DictOption[] | null
  value?: number | string | Array<string | number> | null
  showValue?: boolean
  separator?: string
}>()

const showValue = computed(() => props.showValue !== false)
const separator = computed(() => props.separator ?? ',')

const unmatchArray = ref<Array<string | number>>([])

const typedOptions = computed<DictOption[]>(() => props.options ?? [])

const values = computed(() => {
  if (props.value === null || typeof props.value === 'undefined' || props.value === '') return [] as string[]
  return Array.isArray(props.value)
    ? props.value.map((item) => '' + item)
    : String(props.value).split(separator.value)
})

const unmatch = computed(() => {
  unmatchArray.value = []
  if (props.value === null || typeof props.value === 'undefined' || props.value === '' || typedOptions.value.length === 0) {
    return false
  }
  let hasUnmatch = false
  values.value.forEach((item) => {
    if (!typedOptions.value.some((v) => String(v.value) === item)) {
      unmatchArray.value.push(item)
      hasUnmatch = true
    }
  })
  return hasUnmatch
})

function handleArray(array: Array<string | number>): string {
  if (array.length === 0) return ""
  return array.reduce((pre: string, cur) => pre + " " + cur, "").trim()
}
</script>

<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
