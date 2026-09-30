<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="88px"
        class="recon-query-form"
      >
        <el-form-item label="资金账户">
          <el-select
            v-model="query.accId"
            filterable
            placeholder="选择账户"
            class="recon-acc-select"
            @change="handleQuery"
          >
            <el-option
              v-for="acc in accountList"
              :key="acc.id"
              :label="`${acc.accName}（${acc.accCode}）`"
              :value="acc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="对账期间">
          <el-date-picker
            v-model="query.yearPeriod"
            type="month"
            value-format="YYYY-MM"
            :clearable="false"
            style="width: 120px"
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
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="data">
      <el-card class="common-card">
        <div class="recon-summary">
          <div class="recon-item">
            <div class="recon-label">
              企业日记账余额
            </div>
            <div class="recon-value">
              {{ formatAmount(data.bookBalance) }}
            </div>
          </div>
          <div class="recon-item">
            <div class="recon-label">
              银行对账单余额
            </div>
            <div class="recon-value statement-input">
              <el-input-number
                v-model="statementBalance"
                :precision="2"
                :controls="false"
                placeholder="录入对账单余额"
                style="width: 160px"
              />
              <el-button
                type="primary"
                plain
                size="small"
                @click="saveStatementBalance"
              >
                保存
              </el-button>
            </div>
          </div>
          <div class="recon-item">
            <div class="recon-label">
              加：企业已收银行未收
            </div>
            <div class="recon-value">
              {{ formatAmount(data.unreconciledIncome) }}
            </div>
          </div>
          <div class="recon-item">
            <div class="recon-label">
              减：企业已付银行未付
            </div>
            <div class="recon-value">
              {{ formatAmount(data.unreconciledExpenditure) }}
            </div>
          </div>
          <div class="recon-item">
            <div class="recon-label">
              调节后银行余额
            </div>
            <div class="recon-value">
              {{ data.adjustedStatement === null ? '-' : formatAmount(data.adjustedStatement) }}
            </div>
          </div>
          <div class="recon-item">
            <div class="recon-label">
              差额
            </div>
            <div
              class="recon-value"
              :class="differenceClass"
            >
              {{ data.difference === null ? '-' : formatAmount(data.difference) }}
            </div>
          </div>
        </div>
        <el-alert
          v-if="data.difference !== null && data.difference !== 0"
          :type="isBalanced ? 'success' : 'warning'"
          :closable="false"
          :title="isBalanced ? '调节平衡' : '调节后仍有差额，请继续勾对流水或核对对账单余额'"
        />
      </el-card>

      <el-card class="common-card">
        <template #header>
          <div class="table-header">
            <span>流水勾对（截至期末，勾选表示银行已入账）</span>
            <div>
              <el-button
                size="small"
                :disabled="!selectedIds.length"
                @click="markSelected(true)"
              >
                标记已对账
              </el-button>
              <el-button
                size="small"
                :disabled="!selectedIds.length"
                @click="markSelected(false)"
              >
                取消对账
              </el-button>
            </div>
          </div>
        </template>
        <el-table
          v-loading="loading"
          border
          size="small"
          :data="data.entries"
          max-height="560"
          @selection-change="onSelectionChange"
        >
          <el-table-column
            type="selection"
            width="45"
            :selectable="(row: any) => !row.opening"
          />
          <el-table-column
            label="日期"
            prop="tradeDate"
            width="105"
            align="center"
          />
          <el-table-column
            label="摘要"
            prop="summary"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column
            label="收入"
            width="120"
            align="right"
          >
            <template #default="{ row }">
              {{ formatAmount(row.income, '') }}
            </template>
          </el-table-column>
          <el-table-column
            label="支出"
            width="120"
            align="right"
          >
            <template #default="{ row }">
              {{ formatAmount(row.expenditure, '') }}
            </template>
          </el-table-column>
          <el-table-column
            label="余额"
            width="120"
            align="right"
          >
            <template #default="{ row }">
              {{ formatAmount(row.balance, '') }}
            </template>
          </el-table-column>
          <el-table-column
            label="对账状态"
            width="100"
            align="center"
          >
            <template #default="{ row }">
              <el-tag
                v-if="row.opening"
                size="small"
                type="info"
              >
                期初
              </el-tag>
              <el-tag
                v-else-if="row.reconciled"
                size="small"
                type="success"
              >
                已对账
              </el-tag>
              <el-tag
                v-else
                size="small"
                type="warning"
              >
                未达
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
    <el-empty
      v-else
      description="请选择资金账户并查询"
    />
  </div>
</template>

<script setup name="JournalReconciliation" lang="ts">
import {computed, reactive, ref} from 'vue'
import {formatAmount} from '@/utils'
import {parseTime} from '@/utils/financialCloud'
import {getReconciliation, saveStatement, markReconciled} from '@/api/journal/reconciliation'
import {findAll} from '@/api/journal/journalaccountservice'
import modal from '@/plugins/modal'
import bookStore from '@/store/modules/bookStore'

const currBookStore = bookStore()

const accountList = ref<any[]>([])
const data = ref<any>(null)
const loading = ref(false)
const statementBalance = ref<number | null>(null)
const selectedIds = ref<string[]>([])

const query = reactive({
  accId: '',
  yearPeriod: currBookStore.termCurrent || parseTime(new Date(), '{y}-{m}')
})

const isBalanced = computed(() => data.value?.difference === 0 || Math.abs(data.value?.difference ?? 1) < 0.005)
const differenceClass = computed(() => (isBalanced.value ? 'diff-ok' : 'diff-bad'))

function loadAccounts() {
  findAll({bookId: currBookStore.bookId}).then((res: any) => {
    accountList.value = (res.data || []).filter((a: any) => a.category === 'deposit')
  })
}

function handleQuery() {
  if (!query.accId) {
    return
  }
  loading.value = true
  getReconciliation({accId: query.accId, yearPeriod: query.yearPeriod}).then((res: any) => {
    data.value = res.data
    statementBalance.value = res.data?.statementBalance ?? null
  }).finally(() => {
    loading.value = false
  })
}

function saveStatementBalance() {
  if (statementBalance.value === null || statementBalance.value === undefined) {
    modal.msgWarning('请录入银行对账单期末余额')
    return
  }
  saveStatement({
    accId: query.accId,
    yearPeriod: query.yearPeriod,
    statementBalance: statementBalance.value
  }).then(() => {
    modal.msgSuccess('对账单余额已保存')
    handleQuery()
  })
}

function onSelectionChange(rows: any[]) {
  selectedIds.value = rows.map((r) => r.id)
}

function markSelected(reconciled: boolean) {
  markReconciled({entryIds: selectedIds.value, reconciled}).then(() => {
    modal.msgSuccess(reconciled ? '已标记对账' : '已取消对账')
    handleQuery()
  })
}

loadAccounts()
</script>

<style lang="scss" scoped>
.recon-query-form {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  align-items: center;
}
.recon-acc-select {
  width: min(220px, 100%);
}
@media (max-width: 768px) {
  .recon-acc-select {
    width: 100%;
  }
  .recon-query-form :deep(.el-form-item) {
    margin-right: 0;
  }
}

.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

.recon-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 28px;
  margin-bottom: 12px;
}

.recon-item {
  .recon-label {
    color: #909399;
    font-size: 12px;
    margin-bottom: 4px;
  }

  .recon-value {
    font-size: 18px;
    font-weight: bold;
    color: #303133;
  }

  .statement-input {
    display: flex;
    gap: 8px;
    align-items: center;
  }
}

.diff-ok {
  color: #67c23a;
}

.diff-bad {
  color: #f56c6c;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.common-card {
  margin-bottom: 12px;
}
</style>
