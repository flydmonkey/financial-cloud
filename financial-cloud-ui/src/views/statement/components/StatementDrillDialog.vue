<template>
  <el-dialog
    v-model="visible"
    :title="`行次构成：${itemName || itemCode}`"
    width="780"
    append-to-body
  >
    <div
      v-if="data"
      class="drill-summary"
    >
      <span>账期：{{ data.yearPeriod }}</span>
      <span>科目数：{{ data.subjects?.length || 0 }}</span>
      <span class="drill-total">合计：{{ formatAmount(data.total, '') }}</span>
    </div>
    <el-table
      v-loading="loading"
      :data="data?.subjects || []"
      border
      size="small"
      max-height="420"
    >
      <el-table-column
        label="科目"
        min-width="210"
        align="left"
        header-align="center"
      >
        <template #default="scope">
          <el-link
            type="primary"
            :underline="false"
            @click="goSubLedger(scope.row)"
          >
            {{ scope.row.subjectCode }} {{ scope.row.subjectName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        label="取数规则"
        width="130"
        align="center"
      >
        <template #default="scope">
          <dict-tag
            :options="ruleDict"
            :value="scope.row.rule"
          />
        </template>
      </el-table-column>
      <el-table-column
        label="计算"
        prop="symbol"
        width="60"
        align="center"
      />
      <el-table-column
        :label="type === 'income' ? '借方发生' : '借方余额'"
        align="right"
        header-align="center"
        width="120"
      >
        <template #default="scope">
          {{ formatAmount(scope.row.debit, '') }}
        </template>
      </el-table-column>
      <el-table-column
        :label="type === 'income' ? '贷方发生' : '贷方余额'"
        align="right"
        header-align="center"
        width="120"
      >
        <template #default="scope">
          {{ formatAmount(scope.row.credit, '') }}
        </template>
      </el-table-column>
      <el-table-column
        label="贡献金额"
        align="right"
        header-align="center"
        width="120"
      >
        <template #default="scope">
          <span :style="{fontWeight: 'bold'}">{{ formatAmount(scope.row.amount, '') }}</span>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-text">
          该行次未配置取数规则，或本期无数据
        </div>
      </template>
    </el-table>
  </el-dialog>
</template>

<script setup name="StatementDrillDialog" lang="ts">
import {ref, getCurrentInstance} from 'vue'
import {useRouter} from 'vue-router'
import {formatAmount} from '@/utils'
import {balanceSheetDrill} from '@/api/statement/statement'
import {incomeDrill} from '@/api/statement/statement-income'
import DictTag from '@/components/DictTag/index.vue'

const router = useRouter()
const {proxy} = getCurrentInstance() as any
const {account_balance_type, account_income_balance_type} =
  proxy?.useDict('account_balance_type', 'account_income_balance_type')

const visible = ref(false)
const loading = ref(false)
const data = ref<any>(null)
const itemCode = ref('')
const itemName = ref('')
const type = ref<'balance-sheet' | 'income'>('balance-sheet')
const ruleDict = ref<any[]>([])

/**
 * 打开下钻对话框。
 * options: { type: 'balance-sheet' | 'income', itemCode, itemName, periodType, reportDate }
 */
function open(options: any) {
  type.value = options.type
  itemCode.value = options.itemCode
  itemName.value = options.itemName
  ruleDict.value = options.type === 'income'
    ? account_income_balance_type.value
    : account_balance_type.value
  visible.value = true
  loading.value = true
  data.value = null
  const api = options.type === 'income' ? incomeDrill : balanceSheetDrill
  api({
    periodType: options.periodType,
    reportDate: options.reportDate,
    reportQuarter: options.reportQuarter,
    itemCode: options.itemCode
  }).then((res: any) => {
    data.value = res.data
  }).finally(() => {
    loading.value = false
  })
}

/** 跳转明细账（按科目与账期） */
function goSubLedger(row: any) {
  router.push({
    path: '/voucher/sub-ledger',
    query: {
      subjectCode: row.subjectCode,
      date: data.value?.yearPeriod || ''
    }
  })
}

defineExpose({open})
</script>

<style lang="scss" scoped>
.drill-summary {
  display: flex;
  gap: 24px;
  margin-bottom: 10px;
  color: #606266;
  font-size: 13px;

  .drill-total {
    font-weight: bold;
    color: #303133;
  }
}
</style>
