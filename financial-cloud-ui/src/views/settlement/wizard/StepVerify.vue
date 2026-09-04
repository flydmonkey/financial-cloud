<!-- 月结向导 · 步骤3 系统校验 -->
<template>
  <div class="step-body">
    <el-alert
      v-if="isVerify"
      type="success"
      :closable="false"
      show-icon
      title="硬检已通过，可进入结账"
      style="margin-bottom: 12px"
    />
    <el-alert
      v-else-if="verifyRows.length"
      type="warning"
      :closable="false"
      show-icon
      title="存在未通过项，请处理后重新检查；硬检失败不可进入结账"
      style="margin-bottom: 12px"
    />

    <el-table
      v-loading="loadingVerify"
      :data="verifyRows"
      border
      style="width: 100%"
      empty-text="暂无校验结果，请点击「重新检查」"
    >
      <el-table-column
        type="index"
        label="序号"
        width="70"
      />
      <el-table-column
        prop="item"
        label="检查项目"
        width="200"
      />
      <el-table-column
        label="类型"
        width="100"
      >
        <template #default="scope">
          <el-tag
            size="small"
            :type="scope.row.warning ? 'warning' : (scope.row.hard === false ? 'info' : 'danger')"
          >
            {{ scope.row.warning ? '警告' : (scope.row.hard === false ? '人工' : '硬检') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="结果"
        width="100"
      >
        <template #default="scope">
          <span v-if="scope.row.applicable === false">不适用</span>
          <span v-else-if="scope.row.warning === true && scope.row.result === true">
            <el-icon color="#E6A23C"><WarningFilled /></el-icon>
          </span>
          <span v-else-if="scope.row.result === true">
            <el-icon color="#67C23A"><Select /></el-icon>
          </span>
          <span v-else-if="scope.row.result === false">
            <el-icon color="#F56C6C"><CloseBold /></el-icon>
          </span>
        </template>
      </el-table-column>
      <el-table-column
        prop="reason"
        label="说明"
        min-width="180"
      />
      <el-table-column
        label="操作"
        width="100"
      >
        <template #default="scope">
          <el-button
            v-if="needsJump(scope.row)"
            type="primary"
            link
            @click="emit('jump', scope.row.item)"
          >
            去处理
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script lang="ts" setup>
import { Select, CloseBold, WarningFilled } from '@element-plus/icons-vue'

defineProps<{
  verifyRows: any[]
  isVerify: boolean
  loadingVerify: boolean
}>()

const emit = defineEmits<{
  jump: [item: string]
}>()

function needsJump(row: any): boolean {
  if (row?.applicable === false) return false
  if (row?.result === false) return true
  if (row?.warning === true && row?.result === true) return false
  return false
}
</script>

<style scoped>
.step-body {
  margin-top: 16px;
}
</style>
