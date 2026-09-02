<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="72px"
      >
        <el-form-item label="期间">
          <el-date-picker
            v-model="dateRange"
            type="monthrange"
            value-format="YYYY-MM"
            format="YYYY年MM期"
            range-separator="至"
            :clearable="false"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="groupByDept">
            按部门汇总
          </el-checkbox>
          <el-checkbox v-model="includeDisposed">
            显示已清理资产
          </el-checkbox>
          <el-checkbox v-model="includeChangeInfo">
            显示变动信息
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="load"
          >
            刷新
          </el-button>
          <el-button @click="handleExport">
            导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="common-card">
      <el-table
        v-loading="loading"
        border
        :data="rows"
        :row-class-name="rowClass"
        show-summary
        :summary-method="() => []"
      >
        <el-table-column
          prop="categoryName"
          label="类别"
          min-width="140"
        />
        <el-table-column
          prop="assetCode"
          label="编码"
          width="100"
        >
          <template #default="{ row }">
            <el-link
              v-if="row.rowType === 'ASSET' && row.assetId"
              type="primary"
              @click="goCard(row)"
            >
              {{ row.assetCode }}
            </el-link>
            <span v-else>{{ row.assetCode }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="assetName"
          label="名称"
          min-width="120"
        >
          <template #default="{ row }">
            {{ displayName(row) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="deptName"
          label="部门"
          width="120"
        />
        <el-table-column
          prop="originalValue"
          label="原值"
          align="right"
          width="120"
        >
          <template #default="{ row }">
            {{ fmt(row.originalValue) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="openingAccumDepr"
          label="期初累计折旧"
          align="right"
          width="130"
        >
          <template #default="{ row }">
            {{ fmt(row.openingAccumDepr) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="periodDepr"
          :label="periodLabel"
          align="right"
          width="140"
        >
          <template #default="{ row }">
            {{ fmt(row.periodDepr) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="yearDepr"
          label="本年折旧额"
          align="right"
          width="120"
        >
          <template #default="{ row }">
            {{ fmt(row.yearDepr) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="endingAccumDepr"
          label="期末累计折旧"
          align="right"
          width="130"
        >
          <template #default="{ row }">
            {{ fmt(row.endingAccumDepr) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="endingImpairment"
          label="期末减值准备"
          align="right"
          width="120"
        >
          <template #default="{ row }">
            {{ fmt(row.endingImpairment) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="endingNetValue"
          label="期末净值"
          align="right"
          width="120"
        >
          <template #default="{ row }">
            {{ fmt(row.endingNetValue) }}
          </template>
        </el-table-column>
        <el-table-column
          v-if="includeChangeInfo"
          prop="changeInfo"
          label="期间变动"
          min-width="200"
          show-overflow-tooltip
        />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts" name="FixedAssetDepreciationDetail">
import { fetchDepreciationDetail, exportDepreciationDetail } from '@/api/fixed-asset/report'
import bookStore from '@/store/modules/bookStore'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import modal from '@/plugins/modal'

const route = useRoute()
const router = useRouter()
const curr = bookStore()
const loading = ref(false)
const rows = ref<any[]>([])
const periodLabel = ref('本期折旧')
const includeDisposed = ref(false)
const includeChangeInfo = ref(false)
const groupByDept = ref(false)
const dateRange = ref<[string, string]>([
  (route.query.startPeriod as string) || curr.termCurrent,
  (route.query.endPeriod as string) || curr.termCurrent
])

function displayName(row: any) {
  if (row.rowType === 'SUBTOTAL') return '小计'
  if (row.rowType === 'TOTAL') return '合计'
  return row.assetName
}

function rowClass({ row }: any) {
  if (row.rowType === 'SUBTOTAL') return 'subtotal-row'
  if (row.rowType === 'TOTAL') return 'total-row'
  return ''
}

function fmt(v: any) {
  if (v == null || v === '') return ''
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function queryParams() {
  return {
    startPeriod: dateRange.value?.[0],
    endPeriod: dateRange.value?.[1],
    includeDisposed: includeDisposed.value,
    groupByDept: groupByDept.value,
    includeChangeInfo: includeChangeInfo.value
  }
}

function load() {
  loading.value = true
  fetchDepreciationDetail(queryParams()).then((res: any) => {
    rows.value = res.data?.rows || []
    periodLabel.value = res.data?.periodDeprColumnLabel || '本期折旧'
  }).finally(() => {
    loading.value = false
  })
}

function goCard(row: any) {
  router.push({ path: '/fixed-asset/card', query: { id: row.assetId } })
}

function handleExport() {
  exportDepreciationDetail(queryParams()).then((blob: any) => {
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `折旧明细表.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    modal.msgSuccess('导出成功')
  })
}

onMounted(load)
</script>

<style scoped>
:deep(.subtotal-row) {
  background: #fff8e6;
  font-weight: 600;
}
:deep(.total-row) {
  background: #ffe8a3;
  font-weight: 700;
}
</style>
