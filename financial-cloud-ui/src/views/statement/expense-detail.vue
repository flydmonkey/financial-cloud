<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <div class="queryForm">
        <el-form
          :model="queryParams"
          :inline="true"
          label-width="82px"
        >
          <el-form-item
            label="期间"
            prop="dateRange"
          >
            <el-date-picker
              v-model="queryParams.dateRange"
              type="monthrange"
              unlink-panels
              range-separator="至"
              start-placeholder="起始月份"
              end-placeholder="结束月份"
              value-format="YYYY-MM"
              format="YYYY年MM期"
              :clearable="false"
            />
          </el-form-item>
          <el-form-item
            label="科目"
            prop="subjectCodes"
          >
            <el-tree-select
              v-model="queryParams.subjectCodes"
              class="subject-tree-select"
              :data="subjectTreeOptions"
              :props="subjectTreeProps"
              node-key="code"
              multiple
              filterable
              clearable
              check-strictly
              show-checkbox
              collapse-tags
              collapse-tags-tooltip
              :max-collapse-tags="2"
              :render-after-expand="false"
              placeholder="请选择费用科目"
            />
          </el-form-item>
          <el-form-item
            label="显示层级"
            prop="maxLevel"
          >
            <el-select
              v-model="queryParams.maxLevel"
              style="width: 120px"
            >
              <el-option
                label="至末级"
                :value="0"
              />
              <el-option
                label="一级"
                :value="1"
              />
              <el-option
                label="二级"
                :value="2"
              />
              <el-option
                label="三级"
                :value="3"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            label="辅助核算"
            prop="showAux"
          >
            <el-switch v-model="queryParams.showAux" />
          </el-form-item>
          <el-form-item
            label="仅已过账"
            prop="postedOnly"
          >
            <el-switch v-model="queryParams.postedOnly" />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              @click="handleQuery"
            >
              查询
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="common-card">
      <div class="btn-form">
        <div class="btn-form-right">
          <el-checkbox
            :model-value="showTreeAll"
            label="展开所有层级"
            @change="toggleExpandAll"
          />
          <el-button
            :loading="exporting"
            @click="handleExport"
          >
            导出
          </el-button>
        </div>
      </div>

      <el-table
        :key="tableKey"
        v-loading="loading"
        :data="recordsList"
        row-key="sourceId"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        :expand-row-keys="expandsIds"
        :default-expand-all="false"
        show-summary
        :summary-method="summaryMethod"
        height="590"
      >
        <el-table-column
          label="编码"
          prop="subjectCode"
          width="140"
          show-overflow-tooltip
        />
        <el-table-column
          label="名称"
          prop="subjectName"
          width="200"
          show-overflow-tooltip
        />
        <el-table-column
          v-for="period in periods"
          :key="period"
          :label="formatPeriod(period)"
          :prop="`amounts.${period}`"
          align="right"
          header-align="center"
          min-width="120"
        >
          <template #default="{ row }">
            {{ formatAmount(row.amounts?.[period], '') }}
          </template>
        </el-table-column>
        <el-table-column
          :label="yearLabel"
          prop="yearTotal"
          fixed="right"
          align="right"
          header-align="center"
          min-width="130"
          class-name="col-year-total"
        >
          <template #default="{ row }">
            {{ formatAmount(row.yearTotal, '') }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="StatementExpenseDetail" lang="ts">
import {computed, nextTick, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {expenseDetailExport, getExpenseDetail} from '@/api/statement/statement-expense-detail'
import * as subjectApi from '@/api/standard/standard-subject'
import booksSetStore from '@/store/modules/bookStore'
import {downloadData, formatAmount} from '@/utils'
import {parseTime} from '@/utils/Jinbooks'
import {getSubjectAllNodeIds} from '@/utils/Subjects'

interface ExpenseDetailItem {
  sourceId: string
  parentId?: string
  subjectCode: string
  subjectName: string
  amounts?: Record<string, number | string>
  yearTotal?: number | string
  children?: ExpenseDetailItem[]
}

interface ExpenseDetailReport {
  periods?: string[]
  yearLabel?: string
  items?: ExpenseDetailItem[]
  totals?: Record<string, number | string>
}

interface SubjectTreeNode {
  id?: string
  code: string
  name: string
  label?: string
  children?: SubjectTreeNode[]
}

const DEFAULT_SUBJECT_CODES = ['5601', '5602', '5603']
const EXPENSE_PREFIXES = ['5601', '5602', '5603', '6601', '6602', '6603']

const bookStore = booksSetStore()
const nowPeriod = parseTime(new Date(), '{y}-{m}')
const currentTerm = bookStore.termCurrent || nowPeriod
const yearStart = `${currentTerm.substring(0, 4)}-01`

const queryParams = reactive({
  periodType: 'between',
  dateRange: [yearStart, currentTerm] as string[],
  subjectCodes: [...DEFAULT_SUBJECT_CODES] as string[],
  maxLevel: 0,
  showAux: false,
  postedOnly: true
})

const subjectTreeOptions = ref<SubjectTreeNode[]>([])
const subjectTreeProps = {
  value: 'code',
  label: 'label',
  children: 'children'
}

const recordsList = ref<ExpenseDetailItem[]>([])
const periods = ref<string[]>([])
const yearLabel = ref('区间合计')
const totals = ref<Record<string, number | string>>({})
const expandsIds = ref<string[]>([])
const tableKey = ref(0)
const loading = ref(false)
const exporting = ref(false)

const showTreeAll = computed(() => {
  const allIds = getSubjectAllNodeIds(recordsList.value)
  return allIds.length > 0 && expandsIds.value.length >= allIds.length
})

function isExpenseSubjectCode(code?: string) {
  if (!code) {
    return false
  }
  return EXPENSE_PREFIXES.some(prefix => code === prefix || code.startsWith(`${prefix}.`) || code.startsWith(prefix))
}

function mapSubjectNode(node: any): SubjectTreeNode | null {
  if (!node?.code || !isExpenseSubjectCode(node.code)) {
    // still keep if any expense descendant
    const children = (node?.children || [])
      .map((child: any) => mapSubjectNode(child))
      .filter(Boolean) as SubjectTreeNode[]
    if (!children.length) {
      return null
    }
    return {
      id: node.id,
      code: node.code,
      name: node.name,
      label: `${node.code} ${node.displayName || node.name}`,
      children
    }
  }
  const children = (node?.children || [])
    .map((child: any) => mapSubjectNode(child))
    .filter(Boolean) as SubjectTreeNode[]
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
    const tree = (res.data || [])
      .map((node: any) => mapSubjectNode(node))
      .filter(Boolean) as SubjectTreeNode[]
    subjectTreeOptions.value = tree

    const available = new Set<string>()
    const collect = (nodes: SubjectTreeNode[]) => {
      nodes.forEach(node => {
        available.add(node.code)
        if (node.children?.length) {
          collect(node.children)
        }
      })
    }
    collect(tree)

    const preferred = DEFAULT_SUBJECT_CODES.filter(code => available.has(code))
    queryParams.subjectCodes = preferred.length
      ? preferred
      : Array.from(available).filter(code => EXPENSE_PREFIXES.includes(code)).slice(0, 3)
  })
}

function buildQuery() {
  const codes = (queryParams.subjectCodes || []).filter(Boolean)
  return {
    periodType: queryParams.periodType,
    dateRange: queryParams.dateRange,
    maxLevel: queryParams.maxLevel,
    showAux: queryParams.showAux,
    postedOnly: queryParams.postedOnly,
    subjectCodes: codes.length ? codes : DEFAULT_SUBJECT_CODES
  }
}

function applyExpandState() {
  // 至末级：默认展开全部；指定级次时收起（结果树本身已截断）
  nextTick(() => {
    if (queryParams.maxLevel === 0) {
      expandsIds.value = getSubjectAllNodeIds(recordsList.value)
    } else {
      expandsIds.value = []
    }
    tableKey.value += 1
  })
}

function getList() {
  if (!queryParams.dateRange || queryParams.dateRange.length !== 2) {
    ElMessage.warning('请选择起始月份和结束月份')
    return
  }

  loading.value = true
  getExpenseDetail(buildQuery()).then((response: { data?: ExpenseDetailReport }) => {
    const report = response.data || {}
    recordsList.value = report.items || []
    periods.value = report.periods || []
    yearLabel.value = report.yearLabel || '区间合计'
    totals.value = report.totals || {}
    applyExpandState()
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  getList()
}

function toggleExpandAll() {
  if (expandsIds.value.length) {
    expandsIds.value = []
  } else {
    expandsIds.value = getSubjectAllNodeIds(recordsList.value)
  }
  tableKey.value += 1
}

function handleExport() {
  if (!queryParams.dateRange || queryParams.dateRange.length !== 2) {
    ElMessage.warning('请选择起始月份和结束月份')
    return
  }

  exporting.value = true
  expenseDetailExport(buildQuery()).then((data: Blob) => {
    const range = queryParams.dateRange.join('至')
    downloadData(data, `费用明细表${range} ${parseTime(new Date())}.xlsx`)
  }).finally(() => {
    exporting.value = false
  })
}

function formatPeriod(period: string) {
  const [year, month] = period.split('-')
  return `${year}年${Number(month)}期`
}

function summaryMethod({columns}: { columns: Array<{ property?: string }> }) {
  return columns.map((column, index) => {
    if (index === 0) {
      return '合计'
    }
    if (column.property === 'yearTotal') {
      return formatAmount(totals.value.yearTotal, '')
    }
    const period = column.property?.replace('amounts.', '')
    return period && periods.value.includes(period)
      ? formatAmount(totals.value[period], '')
      : ''
  })
}

onMounted(async () => {
  await loadSubjectTree()
  getList()
})
</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

.subject-tree-select {
  width: 360px;

  :deep(.el-select__wrapper) {
    flex-wrap: nowrap;
    height: 32px;
    min-height: 32px;
  }

  :deep(.el-select__selection) {
    flex-wrap: nowrap;
    overflow: hidden;
  }

  :deep(.el-select__selected-item) {
    max-width: 140px;
  }
}

.btn-form-right {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  margin-bottom: 15px;
}

:deep(.col-year-total) {
  font-weight: 600;
  background-color: #f5f7fa;
}
</style>
