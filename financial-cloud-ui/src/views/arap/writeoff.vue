<template>
  <div class="app-container">
    <el-card class="common-card">
      <el-form
        :inline="true"
        :model="query"
      >
        <el-form-item label="类型">
          <el-radio-group
            v-model="query.side"
            @change="reload"
          >
            <el-radio-button value="AR">
              应收
            </el-radio-button>
            <el-radio-button value="AP">
              应付
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="往来单位ID">
          <el-input
            v-model="query.counterpartId"
            style="width: 240px"
            placeholder="客户/供应商辅助ID"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="reload"
          >
            查询未清项
          </el-button>
          <el-button @click="onSuggest">
            建议匹配
          </el-button>
          <el-button
            type="success"
            :disabled="selected.length < 2"
            @click="onConfirm"
          >
            确认核销
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="勾选挂账侧与冲减侧未清分录，核销金额可改；两侧合计须相等。建议匹配仅预览，确认后才落库。"
        style="margin-bottom: 12px"
      />

      <el-table
        v-loading="loading"
        :data="openItems"
        border
        @selection-change="onSelect"
      >
        <el-table-column
          type="selection"
          width="48"
        />
        <el-table-column
          prop="voucherDate"
          label="日期"
          width="120"
        />
        <el-table-column
          prop="voucherWord"
          label="凭证"
          width="100"
        />
        <el-table-column
          prop="summary"
          label="摘要"
          min-width="140"
        />
        <el-table-column
          label="方向"
          width="90"
        >
          <template #default="scope">
            {{ scope.row.increaseSide ? '挂账' : '冲减' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="originalAmount"
          label="原额"
          align="right"
        />
        <el-table-column
          prop="writtenOffAmount"
          label="已核销"
          align="right"
        />
        <el-table-column
          prop="remainingAmount"
          label="未核销"
          align="right"
        />
        <el-table-column
          label="本次核销"
          width="140"
        >
          <template #default="scope">
            <el-input-number
              v-model="amounts[scope.row.voucherItemId]"
              :min="0"
              :max="Number(scope.row.remainingAmount)"
              :precision="2"
              controls-position="right"
              style="width: 120px"
            />
          </template>
        </el-table-column>
      </el-table>

      <h4 style="margin-top: 20px">
        核销记录
      </h4>
      <el-table
        :data="history"
        border
      >
        <el-table-column
          prop="writeoffDate"
          label="日期"
          width="160"
        />
        <el-table-column
          prop="amount"
          label="金额"
          align="right"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
        />
        <el-table-column
          label="操作"
          width="100"
        >
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'ACTIVE'"
              link
              type="danger"
              @click="onReverse(scope.row.id)"
            >
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref, onMounted, getCurrentInstance} from 'vue'
import {useRoute} from 'vue-router'
import {
  fetchArapOpenItems,
  fetchArapWriteoffSuggest,
  confirmArapWriteoff,
  reverseArapWriteoff,
  fetchArapWriteoffList,
} from '@/api/arap'

const proxy: any = getCurrentInstance()!.proxy
const route = useRoute()
const loading = ref(false)
const openItems = ref<any[]>([])
const history = ref<any[]>([])
const selected = ref<any[]>([])
const amounts = reactive<Record<string, number>>({})
const query = reactive({
  side: (route.query.side as string) || 'AR',
  counterpartId: (route.query.counterpartId as string) || '',
})

async function reload() {
  if (!query.counterpartId) {
    openItems.value = []
    history.value = []
    return
  }
  loading.value = true
  try {
    const [openRes, listRes]: any[] = await Promise.all([
      fetchArapOpenItems({...query, includeZero: false}),
      fetchArapWriteoffList({...query}),
    ])
    openItems.value = openRes.data || []
    history.value = listRes.data || []
    for (const row of openItems.value) {
      amounts[row.voucherItemId] = Number(row.remainingAmount) || 0
    }
  } finally {
    loading.value = false
  }
}

function onSelect(rows: any[]) {
  selected.value = rows
}

async function onSuggest() {
  if (!query.counterpartId) return
  const res: any = await fetchArapWriteoffSuggest({...query})
  const legs = res.data || []
  for (const key of Object.keys(amounts)) {
    amounts[key] = 0
  }
  for (const leg of legs) {
    amounts[leg.voucherItemId] = Number(leg.amount)
  }
  proxy.$modal.msgSuccess(legs.length ? '已填入建议金额，请确认后提交' : '无可建议匹配')
}

async function onConfirm() {
  const legs = selected.value
    .map((r) => ({
      voucherItemId: r.voucherItemId,
      amount: amounts[r.voucherItemId],
    }))
    .filter((l) => l.amount > 0)
  const res: any = await confirmArapWriteoff({
    side: query.side,
    counterpartId: query.counterpartId,
    legs,
  })
  if (res.code === 0) {
    proxy.$modal.msgSuccess('核销成功')
    await reload()
  } else {
    proxy.$modal.msgError(res.message || '核销失败')
  }
}

async function onReverse(id: string) {
  const res: any = await reverseArapWriteoff(id)
  if (res.code === 0) {
    proxy.$modal.msgSuccess('已撤销')
    await reload()
  } else {
    proxy.$modal.msgError(res.message || '撤销失败')
  }
}

onMounted(reload)
</script>
