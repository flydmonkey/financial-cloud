<template>
  <div class="app-container">
    <el-row :gutter="10">
      <el-col :span="24">
        <el-card class="quick-entry">
          <div
            v-for="entry in quickEntries"
            :key="entry.path"
            class="quick-entry-item"
            @click="router.push(entry.path)"
          >
            <el-icon :size="22">
              <component :is="entry.icon" />
            </el-icon>
            <span>{{ entry.label }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="10">
      <el-col :span="24">
        <todo-panel />
      </el-col>
    </el-row>
    <el-row :gutter="10">
      <el-col
        :xs="24"
        :sm="24"
        :md="8"
        :lg="6"
        :xl="6"
      >
        <fund-balance style="height: 350px" />
      </el-col>
      <el-col
        :xs="24"
        :sm="24"
        :md="16"
        :lg="12"
        :xl="12"
      >
        <receivable style="height: 350px" />
      </el-col>
      <el-col
        :xs="24"
        :sm="24"
        :md="8"
        :lg="6"
        :xl="6"
      >
        <expected-available-funds style="height: 350px" />
      </el-col>
      <el-col
        :xs="24"
        :sm="24"
        :md="8"
        :lg="6"
        :xl="6"
      >
        <net-profit style="height: 450px" />
      </el-col>
      <el-col
        :xs="24"
        :sm="24"
        :md="8"
        :lg="6"
        :xl="6"
      >
        <revenue-cost style="height: 450px" />
      </el-col>
      <el-col
        :xs="24"
        :sm="24"
        :md="8"
        :lg="6"
        :xl="6"
      >
        <cost style="height: 450px" />
      </el-col>
      <el-col
        :xs="24"
        :sm="24"
        :md="8"
        :lg="6"
        :xl="6"
      >
        <added-tax style="height: 450px" />
      </el-col>
      <el-col :span="24">
        <other-subjects style="height: 450px" />
      </el-col>
    </el-row>
    <Footer position="relative" />
  </div>
</template>


<script setup name="Index" lang="ts">
import {ref, getCurrentInstance} from "vue";
import {useI18n} from 'vue-i18n'
import {useRoute, useRouter} from "vue-router";
import {EditPen, Tickets, Notebook, Wallet, DataAnalysis, TrendCharts, Checked, Ticket, Document} from '@element-plus/icons-vue'
import Footer from "@/components/Footer/index.vue"
import TodoPanel from "@/views/dashboard/accounting/TodoPanel.vue";
import FundBalance from "@/views/dashboard/accounting/fund_balance.vue";
import receivable from "@/views/dashboard/accounting/receivable.vue";
import ExpectedAvailableFunds from "@/views/dashboard/accounting/expected_available_funds.vue";
import NetProfit from "@/views/dashboard/accounting/net_profit.vue";
import RevenueCost from "@/views/dashboard/accounting/revenue_cost.vue";
import Cost from "@/views/dashboard/accounting/cost.vue";
import AddedTax from "@/views/dashboard/accounting/added_tax.vue";
import OtherSubjects from "@/views/dashboard/accounting/other_subjects.vue";

const {t} = useI18n()
const route: any = useRoute();
const router: any = useRouter();
const proxy: any = getCurrentInstance()!.proxy;

/** 首页快捷入口：覆盖日常做账主路径 */
const quickEntries = [
  {label: '录凭证', path: '/voucher/voucher-edit', icon: EditPen},
  {label: '凭证列表', path: '/voucher/voucher-index', icon: Tickets},
  {label: '明细账', path: '/voucher/sub-ledger', icon: Notebook},
  {label: '日记账', path: '/journal/journalentry', icon: Wallet},
  {label: '资产负债表', path: '/statement/balance-sheet', icon: DataAnalysis},
  {label: '利润表', path: '/statement/income-statement', icon: TrendCharts},
  {label: '费用报销', path: '/expense/claim', icon: Ticket},
  {label: '增值税申报表', path: '/statement/tax-declaration', icon: Document},
  {label: '期末结账', path: '/settlement/settle-list', icon: Checked},
]

</script>
<style scoped lang="scss">
:deep(.el-card) {
  --el-card-border-radius: 10px;
}

.el-col {
  margin-bottom: 20px;
}

.quick-entry {
  :deep(.el-card__body) {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    padding: 14px 18px;
  }
}

.quick-entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 92px;
  padding: 10px 0;
  border-radius: 8px;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;

  &:hover {
    background-color: #ecf5ff;
    color: #409eff;
  }
}
</style>
