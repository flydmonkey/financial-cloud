<template>
  <div class="app-container">
    <el-card class="common-card">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="账龄按凭证日期先进先出估算，非核销账龄；仅供参考。"
        style="margin-bottom: 12px"
      />
      <el-form
        :inline="true"
        :model="query"
      >
        <el-form-item label="类型">
          <el-radio-group
            v-model="query.side"
            @change="load"
          >
            <el-radio-button value="AR">
              应收
            </el-radio-button>
            <el-radio-button value="AP">
              应付
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截至日期">
          <el-date-picker
            v-model="query.asOfDate"
            type="date"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="load"
          >
            查询
          </el-button>
        </el-form-item>
      </el-form>
      <el-table
        v-loading="loading"
        :data="rows"
        border
      >
        <el-table-column
          prop="counterpartName"
          label="往来单位"
          min-width="160"
        />
        <el-table-column
          prop="bucket0To30"
          label="30天内"
          align="right"
        />
        <el-table-column
          prop="bucket31To60"
          label="31-60天"
          align="right"
        />
        <el-table-column
          prop="bucket61To90"
          label="61-90天"
          align="right"
        />
        <el-table-column
          prop="bucket91To180"
          label="91-180天"
          align="right"
        />
        <el-table-column
          prop="bucketOver180"
          label="180天以上"
          align="right"
        />
        <el-table-column
          prop="total"
          label="合计"
          align="right"
        />
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref, onMounted} from 'vue'
import {fetchArapAging} from '@/api/arap'
import bookStore from '@/store/modules/bookStore'

const store = bookStore()
const loading = ref(false)
const rows = ref<any[]>([])
const term = (store.termCurrent || '').toString()
function endOfMonth(ym: string) {
  const [y, m] = ym.split('-').map(Number)
  if (!y || !m) return undefined
  const d = new Date(y, m, 0)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mm}-${dd}`
}
const query = reactive({
  side: 'AR',
  asOfDate: term ? endOfMonth(term) : undefined,
})

async function load() {
  loading.value = true
  try {
    const res: any = await fetchArapAging({...query})
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
