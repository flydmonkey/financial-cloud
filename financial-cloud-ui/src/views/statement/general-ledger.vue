<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <div class="queryForm toolbar">
        <el-form
          :model="queryParams"
          :inline="true"
          label-width="52px"
        >
          <el-form-item
            label="期间"
            prop="dateRange"
          >
            <el-date-picker
              v-model="queryParams.dateRange"
              type="monthrange"
              unlink-panels
              range-separator="~"
              start-placeholder="起始"
              end-placeholder="结束"
              value-format="YYYY-MM"
              format="YYYY年MM期"
              :clearable="false"
              :disabled-date="disabledDate"
            />
          </el-form-item>
          <el-form-item>
            <el-popover
              :visible="filterVisible"
              placement="bottom-start"
              :width="420"
              trigger="click"
            >
              <template #reference>
                <el-button @click="filterVisible = !filterVisible">
                  过滤
                </el-button>
              </template>
              <el-form
                label-width="120px"
                class="filter-form"
              >
                <el-form-item label="起始科目">
                  <el-tree-select
                    v-model="filterForm.subjectCodeFrom"
                    class="subject-select"
                    :data="subjectTreeOptions"
                    :props="subjectTreeProps"
                    node-key="code"
                    filterable
                    clearable
                    check-strictly
                    :render-after-expand="false"
                    placeholder="科目编码/名称"
                  />
                </el-form-item>
                <el-form-item label="结束科目">
                  <el-tree-select
                    v-model="filterForm.subjectCodeTo"
                    class="subject-select"
                    :data="subjectTreeOptions"
                    :props="subjectTreeProps"
                    node-key="code"
                    filterable
                    clearable
                    check-strictly
                    :render-after-expand="false"
                    placeholder="科目编码/名称"
                  />
                </el-form-item>
                <el-form-item label="科目级次">
                  <el-select
                    v-model="filterForm.maxLevel"
                    style="width: 100%"
                  >
                    <el-option
                      label="仅显示一级"
                      :value="1"
                    />
                    <el-option
                      label="至二级"
                      :value="2"
                    />
                    <el-option
                      label="至三级"
                      :value="3"
                    />
                    <el-option
                      label="至末级"
                      :value="0"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="">
                  <el-checkbox v-model="filterForm.showAux">
                    显示辅助核算
                  </el-checkbox>
                </el-form-item>
                <el-form-item label="">
                  <el-checkbox v-model="filterForm.hideZeroBalance">
                    余额为0不显示
                  </el-checkbox>
                </el-form-item>
                <el-form-item label="">
                  <el-checkbox v-model="filterForm.hideNoActivityAndZeroBalance">
                    无发生额且余额为0不显示
                  </el-checkbox>
                </el-form-item>
                <el-form-item label="">
                  <el-checkbox v-model="filterForm.hidePeriodRowsWhenNoActivity">
                    无发生额不显示本期合计、本年累计
                  </el-checkbox>
                </el-form-item>
                <el-form-item>
                  <el-button @click="resetFilter">
                    重置
                  </el-button>
                  <el-button
                    type="primary"
                    @click="applyFilter"
                  >
                    查询
                  </el-button>
                </el-form-item>
              </el-form>
            </el-popover>
            <el-button @click="handleQuery">
              刷新
            </el-button>
            <el-button
              :loading="exporting"
              @click="handleExport"
            >
              导出
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="common-card">
      <div
        v-if="warnings.length"
        class="trial-box"
      >
        <el-alert
          v-for="(w, idx) in warnings"
          :key="idx"
          :title="w"
          :type="trialBalanced ? 'success' : 'warning'"
          show-icon
          :closable="false"
          class="trial-alert"
        />
        <div
          v-if="periodDebitTotal != null"
          class="trial-numbers"
        >
          本期借方合计 {{ formatAmount(periodDebitTotal, '') }}
          ／ 贷方合计 {{ formatAmount(periodCreditTotal, '') }}
          ；期末借方余额合计 {{ formatAmount(closingDebitTotal, '') }}
          ／ 贷方余额合计 {{ formatAmount(closingCreditTotal, '') }}
        </div>
      </div>
      <el-table
        v-loading="loading"
        :data="recordsList"
        border
        :span-method="spanMethod"
      >
        <el-table-column
          prop="subjectCode"
          label="科目编码"
          min-width="110"
        >
          <template #default="{ row }">
            <el-link
              type="primary"
              :underline="false"
              @click="goSubLedger(row)"
            >
              {{ row.subjectCode }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          prop="subjectName"
          label="科目名称"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column
          prop="period"
          label="期间"
          width="100"
        />
        <el-table-column
          prop="summary"
          label="摘要"
          width="110"
        />
        <el-table-column
          prop="debit"
          label="借方"
          align="right"
          min-width="120"
        >
          <template #default="{ row }">
            {{ formatAmount(row.debit, '') }}
          </template>
        </el-table-column>
        <el-table-column
          prop="credit"
          label="贷方"
          align="right"
          min-width="120"
        >
          <template #default="{ row }">
            {{ formatAmount(row.credit, '') }}
          </template>
        </el-table-column>
        <el-table-column
          prop="direction"
          label="方向"
          width="70"
          align="center"
        />
        <el-table-column
          prop="balance"
          label="余额"
          align="right"
          min-width="120"
        >
          <template #default="{ row }">
            {{ formatAmount(row.balance, '') }}
          </template>
        </el-table-column>
      </el-table>
      <div class="footer-count">
        共 {{ subjectCount }} 条
      </div>
    </el-card>
  </div>
</template>

<script setup name="StatementGeneralLedger" lang="ts">
import {onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {generalLedgerExport, getGeneralLedger} from '@/api/statement/statement-general-ledger'
import * as subjectApi from '@/api/standard/standard-subject'
import booksSetStore from '@/store/modules/bookStore'
import {downloadData, formatAmount} from '@/utils'
import {parseTime} from '@/utils/Jinbooks'

interface LedgerItem {
  subjectCode: string
  subjectName: string
  period: string
  summary: string
  debit?: number | string | null
  credit?: number | string | null
  direction?: string
  balance?: number | string | null
  groupKey?: string
  rowSpan?: number
}

interface SubjectTreeNode {
  id?: string
  code: string
  name: string
  label?: string
  children?: SubjectTreeNode[]
}

const router = useRouter()
const bookStore = booksSetStore()
const nowPeriod = parseTime(new Date(), '{y}-{m}')
const currentTerm = bookStore.termCurrent || nowPeriod

const queryParams = reactive({
  periodType: 'between',
  dateRange: [currentTerm, currentTerm] as string[],
  subjectCodeFrom: '' as string,
  subjectCodeTo: '' as string,
  maxLevel: 1,
  showAux: false,
  hideZeroBalance: false,
  hideNoActivityAndZeroBalance: true,
  hidePeriodRowsWhenNoActivity: false
})

const filterForm = reactive({...queryParams})
const filterVisible = ref(false)

const subjectTreeOptions = ref<SubjectTreeNode[]>([])
const subjectTreeProps = {
  value: 'code',
  label: 'label',
  children: 'children'
}

const recordsList = ref<LedgerItem[]>([])
const subjectCount = ref(0)
const loading = ref(false)
const exporting = ref(false)
const warnings = ref<string[]>([])
const trialBalanced = ref(true)
const periodDebitTotal = ref<number | string | null>(null)
const periodCreditTotal = ref<number | string | null>(null)
const closingDebitTotal = ref<number | string | null>(null)
const closingCreditTotal = ref<number | string | null>(null)

function disabledDate(time: Date) {
  if (!bookStore.termCurrent || !bookStore.termStart) {
    return false
  }
  const now = new Date(`${bookStore.termCurrent}-01`)
  const start = new Date(`${bookStore.termStart}-01`)
  return start.getTime() > time.getTime() || time.getTime() > now.getTime()
}

function mapSubjectNode(node: any): SubjectTreeNode {
  const children = (node?.children || []).map((child: any) => mapSubjectNode(child))
  return {
    id: node.id,
    code: node.code,
    name: node.name,
    label: `${node.code} ${node.displayName || node.name}`,
    children: children.length ? children : undefined
  }
}

function loadSubjectTree() {
  return subjectApi.getTree({bookId: bookStore.bookId}).then((res: any) => {
    subjectTreeOptions.value = (res.data || []).map((node: any) => mapSubjectNode(node))
  })
}

function buildQuery() {
  return {
    periodType: queryParams.periodType,
    dateRange: queryParams.dateRange,
    subjectCodeFrom: queryParams.subjectCodeFrom || undefined,
    subjectCodeTo: queryParams.subjectCodeTo || undefined,
    maxLevel: queryParams.maxLevel,
    showAux: queryParams.showAux,
    hideZeroBalance: queryParams.hideZeroBalance,
    hideNoActivityAndZeroBalance: queryParams.hideNoActivityAndZeroBalance,
    hidePeriodRowsWhenNoActivity: queryParams.hidePeriodRowsWhenNoActivity
  }
}

function getList() {
  if (!queryParams.dateRange || queryParams.dateRange.length !== 2) {
    ElMessage.warning('请选择期间')
    return
  }
  loading.value = true
  getGeneralLedger(buildQuery()).then((response: any) => {
    const report = response.data || {}
    recordsList.value = report.items || []
    subjectCount.value = report.subjectCount || 0
    warnings.value = report.warnings || []
    trialBalanced.value = report.trialBalanced !== false
    periodDebitTotal.value = report.periodDebitTotal ?? null
    periodCreditTotal.value = report.periodCreditTotal ?? null
    closingDebitTotal.value = report.closingDebitTotal ?? null
    closingCreditTotal.value = report.closingCreditTotal ?? null
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  getList()
}

function applyFilter() {
  queryParams.subjectCodeFrom = filterForm.subjectCodeFrom
  queryParams.subjectCodeTo = filterForm.subjectCodeTo
  queryParams.maxLevel = filterForm.maxLevel
  queryParams.showAux = filterForm.showAux
  queryParams.hideZeroBalance = filterForm.hideZeroBalance
  queryParams.hideNoActivityAndZeroBalance = filterForm.hideNoActivityAndZeroBalance
  queryParams.hidePeriodRowsWhenNoActivity = filterForm.hidePeriodRowsWhenNoActivity
  filterVisible.value = false
  getList()
}

function resetFilter() {
  filterForm.subjectCodeFrom = ''
  filterForm.subjectCodeTo = ''
  filterForm.maxLevel = 1
  filterForm.showAux = false
  filterForm.hideZeroBalance = false
  filterForm.hideNoActivityAndZeroBalance = true
  filterForm.hidePeriodRowsWhenNoActivity = false
}

function handleExport() {
  if (!queryParams.dateRange || queryParams.dateRange.length !== 2) {
    ElMessage.warning('请选择期间')
    return
  }
  exporting.value = true
  generalLedgerExport(buildQuery()).then((data: Blob) => {
    const range = queryParams.dateRange.join('至')
    downloadData(data, `总账${range} ${parseTime(new Date())}.xlsx`)
  }).finally(() => {
    exporting.value = false
  })
}

function spanMethod({row, columnIndex}: { row: LedgerItem; columnIndex: number }) {
  if (columnIndex === 0 || columnIndex === 1) {
    const span = row.rowSpan ?? 0
    if (span > 0) {
      return {rowspan: span, colspan: 1}
    }
    return {rowspan: 0, colspan: 0}
  }
  return {rowspan: 1, colspan: 1}
}

function goSubLedger(row: LedgerItem) {
  const end = queryParams.dateRange?.[1] || currentTerm
  router.push({
    path: '/voucher/sub-ledger',
    query: {
      subjectCode: row.subjectCode,
      date: end
    }
  })
}

onMounted(async () => {
  Object.assign(filterForm, {
    subjectCodeFrom: queryParams.subjectCodeFrom,
    subjectCodeTo: queryParams.subjectCodeTo,
    maxLevel: queryParams.maxLevel,
    showAux: queryParams.showAux,
    hideZeroBalance: queryParams.hideZeroBalance,
    hideNoActivityAndZeroBalance: queryParams.hideNoActivityAndZeroBalance,
    hidePeriodRowsWhenNoActivity: queryParams.hidePeriodRowsWhenNoActivity
  })
  await loadSubjectTree()
  getList()
})
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  align-items: center;
}

.filter-form {
  .subject-select {
    width: 100%;
  }
}

.footer-count {
  margin-top: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.trial-box {
  margin-bottom: 12px;
}

.trial-alert + .trial-alert {
  margin-top: 8px;
}

.trial-numbers {
  margin-top: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
</style>
