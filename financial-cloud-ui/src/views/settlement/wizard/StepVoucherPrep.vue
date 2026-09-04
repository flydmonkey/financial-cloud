<!-- 月结向导 · 步骤1 凭证整理 -->
<template>
  <div class="step-body">
    <el-alert
      v-if="!canLeaveStep1"
      type="warning"
      :closable="false"
      show-icon
      :title="gateAlertTitle"
      style="margin-bottom: 12px"
    />
    <el-alert
      v-else
      type="success"
      :closable="false"
      show-icon
      title="本期未过账凭证与断号均已清理，可进入下一步"
      style="margin-bottom: 12px"
    />

    <div class="toolbar">
      <el-button
        :loading="loadingStep1"
        @click="emit('refresh')"
      >
        刷新
      </el-button>
      <el-button
        type="primary"
        :loading="loadingAction"
        :disabled="unposted.length === 0"
        @click="emit('submit-audit-post')"
      >
        批量提交审核过账
      </el-button>
      <el-button
        :loading="loadingAction"
        :disabled="successiveGaps.length === 0"
        @click="emit('fix-successive')"
      >
        整理断号
      </el-button>
    </div>

    <el-table
      v-loading="loadingStep1"
      :data="unposted"
      border
      class="voucher-prep-table"
      empty-text="无未过账凭证"
    >
      <el-table-column
        prop="word"
        label="凭证字号"
        min-width="120"
      />
      <el-table-column
        prop="voucherDate"
        label="日期"
        min-width="110"
      />
      <el-table-column
        label="摘要"
        min-width="220"
        show-overflow-tooltip
      >
        <template #default="scope">
          {{ scope.row.remark || '—' }}
        </template>
      </el-table-column>
      <el-table-column
        label="借方金额"
        min-width="120"
        align="right"
        header-align="right"
      >
        <template #default="scope">
          {{ formatAmount(scope.row.debitAmount) }}
        </template>
      </el-table-column>
      <el-table-column
        label="贷方金额"
        min-width="120"
        align="right"
        header-align="right"
      >
        <template #default="scope">
          {{ formatAmount(scope.row.creditAmount) }}
        </template>
      </el-table-column>
      <el-table-column
        label="附件"
        min-width="72"
        align="center"
      >
        <template #default="scope">
          {{ scope.row.receiptNum != null && scope.row.receiptNum > 0 ? scope.row.receiptNum : '—' }}
        </template>
      </el-table-column>
      <el-table-column
        label="结转"
        min-width="72"
        align="center"
      >
        <template #default="scope">
          {{ isCarryForward(scope.row.carryForward) ? '是' : '—' }}
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        min-width="130"
      >
        <template #default="scope">
          {{ statusLabel(scope.row.status) }}
        </template>
      </el-table-column>
    </el-table>

    <p
      v-if="successiveGaps.length"
      class="hint"
    >
      断号待整理：{{ successiveGaps.length }} 处
    </p>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { UnpostedVoucherRow } from './useMonthEndWizard'

const props = defineProps<{
  unposted: UnpostedVoucherRow[]
  successiveGaps: any[]
  canLeaveStep1: boolean
  loadingStep1: boolean
  loadingAction: boolean
}>()

const emit = defineEmits<{
  refresh: []
  'submit-audit-post': []
  'fix-successive': []
}>()

const gateAlertTitle = computed(() => {
  const parts: string[] = []
  if (props.unposted.length) {
    parts.push(`未过账凭证 ${props.unposted.length} 张`)
  }
  if (props.successiveGaps.length) {
    parts.push(`断号 ${props.successiveGaps.length} 处`)
  }
  return parts.length
    ? `尚不能进入下一步：${parts.join('，')}`
    : '尚不能进入下一步'
})

function statusLabel(status: string): string {
  switch (status) {
    case 'draft':
      return '草稿'
    case 'reviewing':
      return '审核中'
    case 'completed':
      return '已审核（待过账）'
    case 'rejected':
      return '被拒绝'
    default:
      return status || '—'
  }
}

function isCarryForward(flag: string | null | undefined): boolean {
  return flag === 'y' || flag === 'Y' || flag === '1' || flag === 'true'
}

function formatAmount(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  const n = Number(value)
  if (!Number.isFinite(n)) {
    return '—'
  }
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.step-body {
  margin-top: 16px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.voucher-prep-table {
  width: 100%;
  margin-top: 12px;
}
.hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 12px 0 0;
}
</style>
