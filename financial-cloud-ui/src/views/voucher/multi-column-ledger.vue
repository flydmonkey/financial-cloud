<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="80px"
      >
        <el-form-item label="栏母科目">
          <el-cascader
            v-model="queryParams.subjectCodePath"
            style="width: 260px"
            filterable
            :options="subjectList"
            :props="cascaderProps"
            placeholder="选择科目（可搜编码/名称）"
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item label="起始月份">
          <el-date-picker
            v-model="queryParams.startMonth"
            type="month"
            style="width: 120px"
            value-format="YYYY-MM"
            :clearable="false"
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item label="截止月份">
          <el-date-picker
            v-model="queryParams.endMonth"
            type="month"
            style="width: 120px"
            value-format="YYYY-MM"
            :clearable="false"
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleQuery"
          >
            查询
          </el-button>
          <el-button
            :disabled="!ledger"
            @click="handlePrint"
          >
            打印
          </el-button>
          <el-button
            :disabled="!ledger"
            @click="handleExportPdf"
          >
            导出 PDF
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="common-card">
      <div
        v-if="ledger"
        class="ledger-head"
      >
        <span class="ledger-title">{{ ledger.subjectCode }} {{ ledger.subjectName }}（{{ ledger.direction === '2' ? '贷方栏' : '借方栏' }}）</span>
        <span>期初余额：{{ formatAmount(ledger.openingBalance, '') }}</span>
        <span>本期净额：{{ formatAmount(ledger.periodTotal, '') }}</span>
        <span class="ledger-closing">期末余额：{{ formatAmount(ledger.closingBalance, '') }}</span>
      </div>
      <el-table
        v-loading="loading"
        :data="ledger?.rows || []"
        border
        size="small"
        max-height="620"
        style="width: 100%"
        class="multi-column-table"
      >
        <el-table-column
          label="日期"
          prop="voucherDate"
          width="110"
          align="center"
          fixed="left"
        />
        <el-table-column
          label="凭证字号"
          width="110"
          align="center"
          fixed="left"
        >
          <template #default="scope">
            <el-link
              type="primary"
              :underline="false"
              @click="goVoucher(scope.row)"
            >
              {{ scope.row.word }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column
          label="摘要"
          prop="summary"
          min-width="180"
          align="left"
          fixed="left"
          :show-overflow-tooltip="true"
        />
        <el-table-column
          v-for="col in ledger?.columns || []"
          :key="col.code"
          :label="col.name"
          align="right"
          header-align="center"
          min-width="110"
        >
          <template #header>
            <div>{{ col.name }}</div>
            <div class="col-code">
              {{ col.code }}
            </div>
          </template>
          <template #default="scope">
            {{ formatAmount(scope.row.amounts?.[col.code], '') }}
          </template>
        </el-table-column>
        <el-table-column
          label="合计"
          align="right"
          header-align="center"
          width="120"
        >
          <template #default="scope">
            {{ formatAmount(scope.row.total, '') }}
          </template>
        </el-table-column>
        <el-table-column
          label="余额"
          align="right"
          header-align="center"
          width="120"
        >
          <template #default="scope">
            <span style="font-weight: bold">{{ formatAmount(scope.row.balance, '') }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-text">
            请选择栏母科目并查询
          </div>
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="MultiColumnLedger" lang="ts">
import {getCurrentInstance, reactive, ref, toRefs} from 'vue'
import {useRouter} from 'vue-router'
import {formatAmount, downloadData} from '@/utils'
import {parseTime} from '@/utils/financialCloud'
import {multiColumnLedger, multiColumnLedgerExportPdf} from '@/api/voucher/voucher'
import * as subjectApi from '@/api/standard/standard-subject'
import {cascaderSubjectProps} from '@/utils/Subjects'
import bookStore from '@/store/modules/bookStore'
import {openTablePrintWindow} from '@/utils/tablePrint'

const router = useRouter()
const {proxy} = getCurrentInstance() as any
const currBookStore = bookStore()

const subjectList = ref<any>([])
const ledger = ref<any>(null)
const loading = ref(false)

const cascaderProps = ref<any>({...cascaderSubjectProps, checkStrictly: true})

const data = reactive({
  queryParams: {
    subjectCodePath: [] as string[],
    startMonth: (currBookStore.termCurrent || parseTime(new Date(), '{y}-{m}')),
    endMonth: (currBookStore.termCurrent || parseTime(new Date(), '{y}-{m}')),
  }
})
const {queryParams} = toRefs(data)

function lastDayOfMonth(yyyyMm: string): string {
  const [y, m] = yyyyMm.split('-').map(Number)
  return new Date(y, m, 0).toISOString().slice(0, 10)
}

function getSubjectList() {
  subjectApi.getTree({bookId: currBookStore.bookId}).then((res: any) => {
    subjectList.value = res.data
  })
}

function handleQuery() {
  const path = queryParams.value.subjectCodePath
  const subjectCode = Array.isArray(path) && path.length ? String(path[path.length - 1]) : ''
  if (!subjectCode) {
    ledger.value = null
    proxy?.$modal?.msgWarning('请选择栏母科目')
    return
  }
  loading.value = true
  multiColumnLedger({
    subjectCode,
    startDate: queryParams.value.startMonth + '-01',
    endDate: lastDayOfMonth(queryParams.value.endMonth),
  }).then((res: any) => {
    ledger.value = res.data
  }).finally(() => {
    loading.value = false
  })
}

function goVoucher(row: any) {
  if (!row.voucherId) {
    return
  }
  router.push({path: '/voucher/voucher-edit', query: {id: row.voucherId}})
}

/** 打印：动态栏列渲染（可另存 PDF） */
function handlePrint() {
  if (!ledger.value) {
    return
  }
  const esc = (v: any) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const amt = (v: any) => formatAmount(v, '')
  const cols: any[] = ledger.value.columns || []
  const body = (ledger.value.rows || []).map((row: any) => `<tr>
    <td class="c">${esc(row.voucherDate)}</td>
    <td class="c">${esc(row.word)}</td>
    <td>${esc(row.summary)}</td>
    ${cols.map((col: any) => `<td class="r">${amt(row.amounts?.[col.code])}</td>`).join('')}
    <td class="r">${amt(row.total)}</td>
    <td class="r">${amt(row.balance)}</td>
  </tr>`).join('')
  const company = currBookStore.getBookItem()?.companyName || ''
  const direction = ledger.value.direction === '2' ? '贷方栏' : '借方栏'
  openTablePrintWindow({
    title: '多栏式明细账',
    subtitle: `核算单位：${company}　科目：${ledger.value.subjectCode} ${ledger.value.subjectName}（${direction}）`
      + `　期间：${queryParams.value.startMonth} 至 ${queryParams.value.endMonth}`
      + `　期初余额：${amt(ledger.value.openingBalance)}　期末余额：${amt(ledger.value.closingBalance)}`,
    tableHtml: `<thead><tr>
      <th>日期</th><th>凭证字号</th><th>摘要</th>
      ${cols.map((col: any) => `<th>${esc(col.name)}</th>`).join('')}
      <th>合计</th><th>余额</th>
    </tr></thead><tbody>${body}</tbody>`,
  })
}

function handleExportPdf() {
  const path = queryParams.value.subjectCodePath
  const subjectCode = Array.isArray(path) && path.length ? String(path[path.length - 1]) : ''
  if (!subjectCode) {
    proxy?.$modal?.msgWarning('请选择栏母科目')
    return
  }
  multiColumnLedgerExportPdf({
    subjectCode,
    startDate: queryParams.value.startMonth + '-01',
    endDate: lastDayOfMonth(queryParams.value.endMonth),
  }).then((data: any) => {
    downloadData(data, `多栏账${subjectCode}_${queryParams.value.startMonth}_${queryParams.value.endMonth}.pdf`)
  })
}

getSubjectList()
</script>

<style lang="scss" scoped>
.multi-column-table {
  width: 100%;
}
.multi-column-table :deep(.el-table__empty-block) {
  width: 100% !important;
  min-width: 100%;
}

.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

.ledger-head {
  display: flex;
  gap: 32px;
  margin-bottom: 10px;
  color: #606266;
  font-size: 13px;

  .ledger-title {
    font-weight: bold;
    color: #303133;
  }

  .ledger-closing {
    font-weight: bold;
    color: #303133;
  }
}

.col-code {
  font-size: 11px;
  color: #909399;
  font-weight: normal;
}
</style>
