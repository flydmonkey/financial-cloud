<template>
  <div>
    <el-cascader
        ref="cascaderRef"
        v-model="innerValue"
        :options="props.options"
        :props="props.props"
        :filter-method="props.filterMethod"
        :show-all-levels="props.props.showAllLevels"
        :clearable="props.props.clearable"
        filterable
        style="width: 100%"
        @change="(val) => emit('change', val)"
        @keydown="handleKeydown"
    />
  </div>
</template>

<script lang="ts" setup>
import {ref, defineExpose, watch, nextTick} from 'vue'
import type {CascaderProps, CascaderOption} from 'element-plus'

interface CustomCascaderProps {
  modelValue: string | number | (string | number)[]
  options: CascaderOption[]
  props?: CascaderProps
  filterMethod?: (item: any, keyword: any) => boolean
}

const props = withDefaults(defineProps<CustomCascaderProps>(), {
  modelValue: '',
  options: () => [],
  props: () => ({
    expandTrigger: 'click',
    label: 'name',
    value: 'code',
    children: 'children',
    checkStrictly: false,
    emitPath: false,
    defaultExpandAll: true,
    showAllLevels: false,
    clearable: false,
    filterable: true
  })
})

const emit = defineEmits(['update:modelValue', 'change'])

// 组件内部 ref
const cascaderRef = ref<any>(null)
const innerValue = ref(props.modelValue)
const visible = ref(false) // 控制下拉显示

// 同步外部 v-model
watch(
    () => props.modelValue,
    (val) => {
      innerValue.value = val
    }
)

watch(innerValue, (val) => {
  emit('update:modelValue', val)
})

// 对外暴露方法：显示/隐藏
const showPanel = () => {
  visible.value = true
  nextTick(() => {
    cascaderRef.value?.togglePopperVisible(true)
  })
}
const hidePanel = () => {
  visible.value = false
  nextTick(() => {
    cascaderRef.value?.togglePopperVisible(false)
  })
}

// 键盘事件处理：上下左右、回车
const handleKeydown = (event: KeyboardEvent) => {
  const key = event.key
  const panel = cascaderRef.value?.cascaderPanelRef

  if (!panel) return

  switch (key) {
    case 'ArrowDown':
      event.preventDefault()
      panel.focusNode('next')
      break
    case 'ArrowUp':
      event.preventDefault()
      panel.focusNode('prev')
      break
    case 'ArrowRight':
      event.preventDefault()
      panel.focusNode('child')
      break
    case 'ArrowLeft':
      event.preventDefault()
      panel.focusNode('parent')
      break
    case 'Enter':
      event.preventDefault()
      panel.handlePick(panel.checkedValue || panel.hoverValue)
      hidePanel()
      break
  }
}

// 暴露接口给父组件调用
defineExpose({
  showPanel,
  hidePanel,
  cascaderRef
})
</script>
