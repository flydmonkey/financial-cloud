<template>
  <div class="app-container">
    <el-card class="common-card">
      <el-form
        :inline="true"
        :model="query"
        label-width="90px"
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
          <el-checkbox v-model="query.includeZero">
            含零余额
          </el-checkbox>
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
        @row-click="onRowClick"
      >
        <el-table-column
          prop="counterpartName"
          label="往来单位"
          min-width="160"
        />
        <el-table-column
          prop="opening"
          label="期初"
          align="right"
        />
        <el-table-column
          prop="periodDebit"
          label="本期借方"
          align="right"
        />
        <el-table-column
          prop="periodCredit"
          label="本期贷方"
          align="right"
        />
        <el-table-column
          prop="ending"
          label="期末"
          align="right"
        />
        <el-table-column
          label="操作"
          width="160"
        >
          <template #default="scope">
            <el-button
              link
              type="primary"
              @click.stop="goDetail(scope.row)"
            >
              明细
            </el-button>
            <el-button
              link
              type="primary"
              @click.stop="exportStatement(scope.row)"
            >
              对账单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {reactive, ref, onMounted, getCurrentInstance} from 'vue'
import {fetchArapBalance, exportArapStatement} from '@/api/arap'
import bookStore from '@/store/modules/bookStore'

const proxy: any = getCurrentInstance()!.proxy
const store = bookStore()
const loading = ref(false)
const rows = ref<any[]>([])
const term = (store.termCurrent || '').toString()
const query = reactive({
  side: 'AR',
  periodStart: term || undefined,
  periodEnd: term || undefined,
  includeZero: false,
})

async function load() {
  loading.value = true
  try {
    const res: any = await fetchArapBalance({...query})
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

function goDetail(row: any) {
  proxy.$tab.openPage(
    `/arap/detail?side=${query.side}&counterpartId=${row.counterpartId}&periodStart=${query.periodStart}&periodEnd=${query.periodEnd}&name=${encodeURIComponent(row.counterpartName || '')}`,
  )
}

function onRowClick(row: any) {
  goDetail(row)
}

async function exportStatement(row: any) {
  const blob: any = await exportArapStatement({
    side: query.side,
    counterpartId: row.counterpartId,
    periodStart: query.periodStart,
    periodEnd: query.periodEnd,
  })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `对账单-${row.counterpartName || row.counterpartId}.xlsx`
  a.click()
  window.URL.revokeObjectURL(url)
}

onMounted(load)
</script>
