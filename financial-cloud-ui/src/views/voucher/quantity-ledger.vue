<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="80px"
      >
        <el-form-item label="科目">
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
        <span class="ledger-title">{{ ledger.subjectCode }} {{ ledger.subjectName }}</span>
        <span>期初结存：{{ ledger.openingQuantity }} 件 / {{ formatAmount(ledger.openingAmount, '') }}</span>
        <span class="ledger-closing">期末结存：{{ ledger.closingQuantity }} 件 / {{ formatAmount(ledger.closingAmount, '') }}</span>
      </div>
      <el-table
        v-loading="loading"
        :data="ledger?.rows || []"
        border
        size="small"
        max-height="620"
      >
        <el-table-column
          label="日期"
          prop="voucherDate"
          width="105"
          align="center"
          fixed="left"
        />
        <el-table-column
          label="凭证字号"
          width="100"
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
          min-width="150"
          align="left"
          fixed="left"
          :show-overflow-tooltip="true"
        />
        <el-table-column
          label="收入"
          header-align="center"
        >
          <el-table-column
            label="数量"
            width="80"
            align="right"
          >
            <template #default="scope">
              {{ scope.row.inQuantity ?? '' }}
            </template>
          </el-table-column>
          <el-table-column
            label="单价"
            width="100"
            align="right"
          >
            <template #default="scope">
              {{ formatAmount(scope.row.inPrice, '') }}
            </template>
          </el-table-column>
          <el-table-column
            label="金额"
            width="110"
            align="right"
          >
            <template #default="scope">
              {{ formatAmount(scope.row.inAmount, '') }}
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column
          label="发出"
          header-align="center"
        >
          <el-table-column
            label="数量"
            width="80"
            align="right"
          >
            <template #default="scope">
              {{ scope.row.outQuantity ?? '' }}
            </template>
          </el-table-column>
          <el-table-column
            label="单价"
            width="100"
            align="right"
          >
            <template #default="scope">
              {{ formatAmount(scope.row.outPrice, '') }}
            </template>
          </el-table-column>
          <el-table-column
            label="金额"
            width="110"
            align="right"
          >
            <template #default="scope">
              {{ formatAmount(scope.row.outAmount, '') }}
            </template>
          </el-table-column>
        </el-table-column>
        <el-table-column
          label="结存"
          header-align="center"
        >
          <el-table-column
            label="数量"
            width="80"
            align="right"
          >
            <template #default="scope">
              <span style="font-weight: bold">{{ scope.row.balanceQuantity }}</span>
            </template>
          </el-table-column>
          <el-table-column
            label="单价"
            width="100"
            align="right"
          >
            <template #default="scope">
              {{ formatAmount(scope.row.balancePrice, '') }}
            </template>
          </el-table-column>
          <el-table-column
            label="金额"
            width="110"
            align="right"
          >
            <template #default="scope">
              <span style="font-weight: bold">{{ formatAmount(scope.row.balanceAmount, '') }}</span>
            </template>
          </el-table-column>
        </el-table-column>
        <template #empty>
          <div class="empty-text">
            请选择科目并查询
          </div>
        </template>
      </el-table>
      <div
        v-if="ledger"
        class="ledger-foot"
      >
        <span>本期收入合计：{{ ledger.periodInQuantity }} 件 / {{ formatAmount(ledger.periodInAmount, '') }}</span>
        <span>本期发出合计：{{ ledger.periodOutQuantity }} 件 / {{ formatAmount(ledger.periodOutAmount, '') }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup name="QuantityLedger" lang="ts">
import {getCurrentInstance, reactive, ref, toRefs} from 'vue'
import {useRouter} from 'vue-router'
import {formatAmount, downloadData} from '@/utils'
import {parseTime} from '@/utils/financialCloud'
import {quantityLedger, quantityLedgerExportPdf} from '@/api/voucher/voucher'
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
    proxy?.$modal?.msgWarning('请选择科目')
    return
  }
  loading.value = true
  quantityLedger({
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

/** 打印：收入/发出/结存九列渲染（可另存 PDF） */
function handlePrint() {
  if (!ledger.value) {
    return
  }
  const esc = (v: any) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const amt = (v: any) => formatAmount(v, '')
  const qty = (v: any) => (v ?? '') === '' ? '' : esc(v)
  const body = (ledger.value.rows || []).map((row: any) => `<tr>
    <td class="c">${esc(row.voucherDate)}</td>
    <td class="c">${esc(row.word)}</td>
    <td>${esc(row.summary)}</td>
    <td class="r">${qty(row.inQuantity)}</td>
    <td class="r">${amt(row.inPrice)}</td>
    <td class="r">${amt(row.inAmount)}</td>
    <td class="r">${qty(row.outQuantity)}</td>
    <td class="r">${amt(row.outPrice)}</td>
    <td class="r">${amt(row.outAmount)}</td>
    <td class="r">${qty(row.balanceQuantity)}</td>
    <td class="r">${amt(row.balancePrice)}</td>
    <td class="r">${amt(row.balanceAmount)}</td>
  </tr>`).join('')
  const company = currBookStore.getBookItem()?.companyName || ''
  openTablePrintWindow({
    title: '数量金额明细账',
    subtitle: `核算单位：${company}　科目：${ledger.value.subjectCode} ${ledger.value.subjectName}`
      + `　期间：${queryParams.value.startMonth} 至 ${queryParams.value.endMonth}`
      + `　期初结存：${ledger.value.openingQuantity ?? 0} 件 / ${amt(ledger.value.openingAmount)}`
      + `　期末结存：${ledger.value.closingQuantity ?? 0} 件 / ${amt(ledger.value.closingAmount)}`,
    tableHtml: `<thead>
      <tr>
        <th rowspan="2">日期</th><th rowspan="2">凭证字号</th><th rowspan="2">摘要</th>
        <th colspan="3">收入</th><th colspan="3">发出</th><th colspan="3">结存</th>
      </tr>
      <tr><th>数量</th><th>单价</th><th>金额</th><th>数量</th><th>单价</th><th>金额</th><th>数量</th><th>单价</th><th>金额</th></tr>
    </thead><tbody>${body}</tbody>`,
  })
}

function handleExportPdf() {
  const path = queryParams.value.subjectCodePath
  const subjectCode = Array.isArray(path) && path.length ? String(path[path.length - 1]) : ''
  if (!subjectCode) {
    proxy?.$modal?.msgWarning('请选择科目')
    return
  }
  quantityLedgerExportPdf({
    subjectCode,
    startDate: queryParams.value.startMonth + '-01',
    endDate: lastDayOfMonth(queryParams.value.endMonth),
  }).then((data: any) => {
    downloadData(data, `数量金额账${subjectCode}_${queryParams.value.startMonth}_${queryParams.value.endMonth}.pdf`)
  })
}

getSubjectList()
</script>

<style lang="scss" scoped>
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

.ledger-foot {
  display: flex;
  gap: 32px;
  margin-top: 10px;
  color: #606266;
  font-size: 13px;
  font-weight: bold;
}
</style>
