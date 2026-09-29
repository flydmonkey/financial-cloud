<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="96px"
      >
        <el-form-item label="测算期间">
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
            style="width: 110px"
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
        <el-form-item label="税负预警线">
          <el-select
            v-model="rates.burdenThreshold"
            style="width: 90px"
            @change="handleQuery"
          >
            <el-option
              label="0.5%"
              value="0.005"
            />
            <el-option
              label="1%"
              value="0.01"
            />
            <el-option
              label="2%"
              value="0.02"
            />
            <el-option
              label="3%"
              value="0.03"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleQuery"
          >
            测算
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
      <el-alert
        v-if="data.burdenWarning"
        type="warning"
        :closable="false"
        class="burden-alert"
        :title="`增值税税负率 ${formatPercent(data.vatBurdenRate)} 低于预警线 ${formatPercent(data.burdenThreshold)}，请关注申报风险`"
      />
      <el-row :gutter="12">
        <el-col :span="12">
          <el-card class="common-card">
            <template #header>
              <span class="section-title">增值税测算（{{ data.yearMonth }}）</span>
            </template>
            <el-descriptions
              :column="1"
              border
              size="small"
            >
              <el-descriptions-item label="销项税额">
                {{ formatAmount(data.outputTax) }}
              </el-descriptions-item>
              <el-descriptions-item label="进项税额">
                {{ formatAmount(data.inputTax) }}
              </el-descriptions-item>
              <el-descriptions-item label="进项税额转出">
                {{ formatAmount(data.inputTransferOut) }}
              </el-descriptions-item>
              <el-descriptions-item label="应纳税额">
                {{ formatAmount(data.vatPayable) }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="data.vatCredit > 0"
                label="期末留抵税额"
              >
                {{ formatAmount(data.vatCredit) }}
              </el-descriptions-item>
              <el-descriptions-item label="已交税金">
                {{ formatAmount(data.paidTax) }}
              </el-descriptions-item>
              <el-descriptions-item label="本期应补税额">
                <span class="amount-strong">{{ formatAmount(data.vatDue) }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="common-card">
            <template #header>
              <span class="section-title">附加税测算（以应补增值税为依据）</span>
            </template>
            <el-descriptions
              :column="1"
              border
              size="small"
            >
              <el-descriptions-item :label="`城市维护建设税（${formatPercent(data.urbanRate)}）`">
                {{ formatAmount(data.urbanTax) }}
              </el-descriptions-item>
              <el-descriptions-item :label="`教育费附加（${formatPercent(data.eduRate)}）`">
                {{ formatAmount(data.eduTax) }}
              </el-descriptions-item>
              <el-descriptions-item :label="`地方教育附加（${formatPercent(data.localEduRate)}）`">
                {{ formatAmount(data.localEduTax) }}
              </el-descriptions-item>
              <el-descriptions-item label="附加税合计">
                <span class="amount-strong">{{ formatAmount(data.surtaxTotal) }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
          <el-card class="common-card">
            <template #header>
              <span class="section-title">企业所得税测算</span>
            </template>
            <el-descriptions
              :column="1"
              border
              size="small"
            >
              <el-descriptions-item label="利润总额（剔除所得税费用）">
                {{ formatAmount(data.profitBeforeTax) }}
              </el-descriptions-item>
              <el-descriptions-item :label="`测算企业所得税（${formatPercent(data.incomeTaxRate)}）`">
                <span class="amount-strong">{{ formatAmount(data.incomeTax) }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
          <el-card class="common-card">
            <template #header>
              <span class="section-title">税负分析</span>
            </template>
            <el-descriptions
              :column="1"
              border
              size="small"
            >
              <el-descriptions-item label="营业收入">
                {{ formatAmount(data.revenue) }}
              </el-descriptions-item>
              <el-descriptions-item label="增值税税负率">
                <span :class="data.burdenWarning ? 'amount-warn' : 'amount-strong'">
                  {{ formatPercent(data.vatBurdenRate) }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="预警线">
                {{ formatPercent(data.burdenThreshold) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>
      <div class="tax-tip">
        测算口径：已过账凭证分录（与账簿一致），增值税按「销项-进项+进项转出-已交」测算，结果仅供申报前参考，非正式申报表。
      </div>
    </template>
    <el-empty
      v-else
      description="请选择期间并点击测算"
    />
  </div>
</template>

<script setup name="TaxEstimate" lang="ts">
import {reactive, ref, onMounted} from 'vue'
import {formatAmount} from '@/utils'
import {parseTime} from '@/utils/financialCloud'
import {taxEstimate} from '@/api/statement/tax-estimate'
import bookStore from '@/store/modules/bookStore'
import {openTablePrintWindow} from '@/utils/tablePrint'

const currBookStore = bookStore()

const yearMonth = ref(currBookStore.termCurrent || parseTime(new Date(), '{y}-{m}'))
const loading = ref(false)
const data = ref<any>(null)

const rates = reactive({
  urbanRate: '0.07',
  incomeTaxRate: '0.25',
  burdenThreshold: '0.01'
})

function formatPercent(v: any): string {
  if (v === null || v === undefined || v === '') {
    return '-'
  }
  return (Number(v) * 100).toFixed(2) + '%'
}

function handleQuery() {
  if (!yearMonth.value) {
    return
  }
  loading.value = true
  taxEstimate({
    yearMonth: yearMonth.value,
    urbanRate: rates.urbanRate,
    incomeTaxRate: rates.incomeTaxRate,
    burdenThreshold: rates.burdenThreshold
  }).then((res: any) => {
    data.value = res.data
  }).finally(() => {
    loading.value = false
  })
}

onMounted(handleQuery)

/** 打印测算结果（可另存 PDF） */
function handlePrint() {
  if (!data.value) {
    return
  }
  const esc = (v: any) => String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const row = (label: string, value: any) =>
    `<tr><td>${esc(label)}</td><td class="r">${esc(value)}</td></tr>`
  const sec = (title: string) =>
    `<tr><td colspan="2" style="background:#f0f0f0;font-weight:bold">${esc(title)}</td></tr>`
  const d = data.value
  const body = sec('增值税测算')
    + row('销项税额', formatAmount(d.outputTax))
    + row('进项税额', formatAmount(d.inputTax))
    + row('进项税额转出', formatAmount(d.inputTransferOut))
    + row('应纳税额', formatAmount(d.vatPayable))
    + (d.vatCredit > 0 ? row('期末留抵税额', formatAmount(d.vatCredit)) : '')
    + row('已交税金', formatAmount(d.paidTax))
    + row('本期应补税额', formatAmount(d.vatDue))
    + sec('附加税测算')
    + row(`城市维护建设税（${formatPercent(d.urbanRate)}）`, formatAmount(d.urbanTax))
    + row(`教育费附加（${formatPercent(d.eduRate)}）`, formatAmount(d.eduTax))
    + row(`地方教育附加（${formatPercent(d.localEduRate)}）`, formatAmount(d.localEduTax))
    + row('附加税合计', formatAmount(d.surtaxTotal))
    + sec('企业所得税测算')
    + row('利润总额（剔除所得税费用）', formatAmount(d.profitBeforeTax))
    + row(`测算企业所得税（${formatPercent(d.incomeTaxRate)}）`, formatAmount(d.incomeTax))
    + sec('税负分析')
    + row('营业收入', formatAmount(d.revenue))
    + row('增值税税负率', formatPercent(d.vatBurdenRate))
    + row('预警线', formatPercent(d.burdenThreshold))
  const company = currBookStore.getBookItem()?.companyName || ''
  openTablePrintWindow({
    title: '税费测算单',
    subtitle: `核算单位：${company}　测算期间：${d.yearMonth}　口径：已过账凭证分录，仅供申报前参考`,
    tableHtml: `<thead><tr><th>项目</th><th style="width:180px">金额</th></tr></thead><tbody>${body}</tbody>`,
  })
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

.burden-alert {
  margin-bottom: 12px;
}

.section-title {
  font-weight: bold;
}

.amount-strong {
  font-weight: bold;
  color: #303133;
}

.amount-warn {
  font-weight: bold;
  color: #e6a23c;
}

.tax-tip {
  margin-top: 12px;
  color: #909399;
  font-size: 12px;
}

.common-card {
  margin-bottom: 12px;
}
</style>
