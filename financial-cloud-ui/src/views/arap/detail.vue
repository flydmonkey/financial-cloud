<template>
  <div class="app-container">
    <el-card class="common-card">
      <el-form
        :inline="true"
        :model="query"
        label-width="90px"
      >
        <el-form-item label="类型">
          <el-radio-group v-model="query.side">
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
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="期间起">
          <el-date-picker
            v-model="query.periodStart"
            type="month"
            value-format="YYYY-MM"
          />
        </el-form-item>
        <el-form-item label="期间止">
          <el-date-picker
            v-model="query.periodEnd"
            type="month"
            value-format="YYYY-MM"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="load"
          >
            查询
          </el-button>
          <el-button @click="exportStatement">
            导出对账单
          </el-button>
        </el-form-item>
      </el-form>
      <p
        v-if="counterpartName"
        class="hint"
      >
        往来单位：{{ counterpartName }}
      </p>
      <el-table
        v-loading="loading"
        :data="rows"
        border
      >
        <el-table-column
          prop="voucherDate"
          label="日期"
          width="120"
        />
        <el-table-column
          prop="voucherWord"
          label="凭证字号"
          width="120"
        />
        <el-table-column
          prop="summary"
          label="摘要"
          min-width="160"
        />
        <el-table-column
          prop="subjectName"
          label="科目"
          min-width="140"
        />
        <el-table-column
          prop="debitAmount"
          label="借方"
          align="right"
        />
        <el-table-column
          prop="creditAmount"
          label="贷方"
          align="right"
        />
        <el-table-column
          prop="runningBalance"
          label="余额"
          align="right"
        />
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref, onMounted} from 'vue'
import {useRoute} from 'vue-router'
import {fetchArapDetail, exportArapStatement} from '@/api/arap'
import bookStore from '@/store/modules/bookStore'

const route = useRoute()
const store = bookStore()
const loading = ref(false)
const rows = ref<any[]>([])
const counterpartName = ref('')
const term = (store.termCurrent || '').toString()
const query = reactive({
  side: (route.query.side as string) || 'AR',
  counterpartId: (route.query.counterpartId as string) || '',
  periodStart: (route.query.periodStart as string) || term,
  periodEnd: (route.query.periodEnd as string) || term,
})
counterpartName.value = decodeURIComponent((route.query.name as string) || '')

async function load() {
  if (!query.counterpartId) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    const res: any = await fetchArapDetail({...query})
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function exportStatement() {
  if (!query.counterpartId) return
  const blob: any = await exportArapStatement({...query})
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `对账单.xlsx`
  a.click()
  window.URL.revokeObjectURL(url)
}

onMounted(load)
</script>

<style scoped>
.hint {
  color: var(--el-text-color-secondary);
  margin: 0 0 12px;
}
</style>
