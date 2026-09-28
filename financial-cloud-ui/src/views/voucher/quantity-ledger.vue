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
import {formatAmount} from '@/utils'
import {parseTime} from '@/utils/financialCloud'
import {quantityLedger} from '@/api/voucher/voucher'
import * as subjectApi from '@/api/standard/standard-subject'
import {cascaderSubjectProps} from '@/utils/Subjects'
import bookStore from '@/store/modules/bookStore'

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
