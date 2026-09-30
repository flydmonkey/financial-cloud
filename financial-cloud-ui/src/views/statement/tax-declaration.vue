<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="96px"
      >
        <el-form-item label="申报期间">
          <el-date-picker
            v-model="yearMonth"
            type="month"
            value-format="YYYY-MM"
            :clearable="false"
            style="width: 130px"
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item label="城建税率">
          <el-select
            v-model="rates.urbanRate"
            style="width: 90px"
            @change="handleQuery"
          >
            <el-option
              label="7%"
              value="0.07"
            />
            <el-option
              label="5%"
              value="0.05"
            />
            <el-option
              label="1%"
              value="0.01"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="企税税率">
          <el-select
            v-model="rates.incomeTaxRate"
            style="width: 150px"
            @change="handleQuery"
          >
            <el-option
              label="25%（一般）"
              value="0.25"
            />
            <el-option
              label="5%（小微优惠）"
              value="0.05"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleQuery"
          >
            生成
          </el-button>
          <el-button
            :disabled="!data"
            @click="handlePrint"
          >
            打印
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="data">
      <el-card
        v-for="section in sections"
        :key="section"
        class="common-card"
      >
        <template #header>
          <span class="section-title">{{ section }}</span>
        </template>
        <div class="table-scroll-x table-scroll-x--fixed">
          <el-table
            :data="grouped[section]"
            border
            size="small"
            :show-header="true"
          >
            <el-table-column
              label="行次"
              prop="rowNo"
              width="64"
              align="center"
              fixed="left"
              class-name="cell-nowrap"
            />
            <el-table-column
              label="项目"
              prop="item"
              min-width="160"
              fixed="left"
              class-name="tax-item-col"
              :show-overflow-tooltip="true"
            />
            <el-table-column
              label="金额"
              min-width="120"
              align="right"
              class-name="cell-nowrap"
            >
              <template #default="scope">
                <span :class="{ 'amount-strong': isKeyRow(scope.row.rowNo) }">
                  {{ scope.row.amount === null || scope.row.amount === undefined ? '' : formatAmount(scope.row.amount) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
      <div class="tax-tip">
        口径：已过账凭证分录（与账簿一致）；销售额按营业收入估算，空白行次为账面无法拆分项目，请按实际申报数据填写。
        本表为申报底稿，正式申报以电子税务局为准。
      </div>
    </template>
    <el-empty
      v-else
      description="请选择期间并点击生成"
    />
  </div>
</template>

<script setup name="TaxDeclaration" lang="ts">
import {computed, reactive, ref, onMounted} from 'vue'
import {formatAmount} from '@/utils'
import {parseTime} from '@/utils/financialCloud'
import {taxDeclaration} from '@/api/statement/tax-estimate'
import {openTablePrintWindow} from '@/utils/tablePrint'
import bookStore from '@/store/modules/bookStore'

const currBookStore = bookStore()

const yearMonth = ref(currBookStore.termCurrent || parseTime(new Date(), '{y}-{m}'))
const loading = ref(false)
const data = ref<any>(null)

const rates = reactive({
  urbanRate: '0.07',
  incomeTaxRate: '0.25'
})

/** 关键合计行加粗 */
const KEY_ROWS = ['19', '24', '34']
const isKeyRow = (rowNo: string) => KEY_ROWS.includes(rowNo) || rowNo === ''

const sections = computed<string[]>(() => {
  if (!data.value) {
    return []
  }
  const seen: string[] = []
  for (const line of data.value.lines || []) {
    if (!seen.includes(line.section)) {
      seen.push(line.section)
    }
  }
  return seen
})

const grouped = computed<Record<string, any[]>>(() => {
  const map: Record<string, any[]> = {}
  for (const line of data.value?.lines || []) {
    ;(map[line.section] = map[line.section] || []).push(line)
  }
  return map
})

function handleQuery() {
  if (!yearMonth.value) {
    return
  }
  loading.value = true
  taxDeclaration({
    yearMonth: yearMonth.value,
    urbanRate: rates.urbanRate,
    incomeTaxRate: rates.incomeTaxRate
  }).then((res: any) => {
    data.value = res.data
  }).finally(() => {
    loading.value = false
  })
}

/** 打印：按官方行次渲染申报底稿（可另存 PDF） */
function handlePrint() {
  if (!data.value) {
    return
  }
  const esc = (v: any) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const amt = (v: any) => (v === null || v === undefined ? '' : formatAmount(v))
  let body = ''
  let lastSection = ''
  for (const line of data.value.lines || []) {
    if (line.section !== lastSection) {
      lastSection = line.section
      body += `<tr><td colspan="3" style="background:#f0f0f0;font-weight:bold">${esc(line.section)}</td></tr>`
    }
    body += `<tr>
      <td class="c">${esc(line.rowNo)}</td>
      <td>${esc(line.item)}</td>
      <td class="r">${amt(line.amount)}</td>
    </tr>`
  }
  const company = currBookStore.getBookItem()?.companyName || ''
  openTablePrintWindow({
    title: '增值税及附加税费申报表（底稿）',
    subtitle: `核算单位：${company}　申报期间：${data.value.yearMonth}`,
    tableHtml: `<thead><tr>
      <th style="width:60px">行次</th><th>项目</th><th style="width:160px">金额</th>
    </tr></thead><tbody>${body}</tbody>`,
  })
}

onMounted(handleQuery)
</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

.section-title {
  font-weight: bold;
}

.amount-strong {
  font-weight: bold;
  color: #303133;
}

.tax-tip {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.common-card {
  margin-bottom: 12px;
}

:deep(.tax-item-col .cell) {
  white-space: nowrap;
  word-break: keep-all;
}

:deep(.cell-nowrap .cell) {
  white-space: nowrap;
  word-break: keep-all;
}
</style>
