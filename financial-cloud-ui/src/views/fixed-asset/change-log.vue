<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="72px"
      >
        <el-form-item label="期间">
          <el-date-picker
            v-model="dateRange"
            type="monthrange"
            value-format="YYYY-MM"
            format="YYYY年MM期"
            range-separator="至"
            :clearable="false"
          />
        </el-form-item>
        <el-form-item label="编码">
          <el-input
            v-model="query.assetCode"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="名称">
          <el-input
            v-model="query.assetName"
            clearable
            @keyup.enter="handleQuery"
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
      <el-table
        v-loading="loading"
        border
        :data="list"
      >
        <el-table-column
          prop="assetCode"
          label="资产编码"
          width="110"
        />
        <el-table-column
          prop="assetName"
          label="资产名称"
          min-width="140"
        />
        <el-table-column
          prop="fieldLabel"
          label="变动项"
          width="130"
        />
        <el-table-column
          prop="beforeValue"
          label="变动前内容"
          min-width="140"
        />
        <el-table-column
          prop="afterValue"
          label="变动后内容"
          min-width="140"
        />
        <el-table-column
          prop="yearPeriod"
          label="变动期间"
          width="110"
        />
        <el-table-column
          prop="modifiedByName"
          label="修改人"
          width="100"
        />
        <el-table-column
          prop="changeTime"
          label="变动时间"
          width="170"
        />
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="query.pageNumber"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts" name="FixedAssetChangeLog">
import { listFixedAssetChange } from '@/api/fixed-asset/change'
import bookStore from '@/store/modules/bookStore'
import { reactive, ref, onMounted } from 'vue'

const curr = bookStore()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string]>([curr.termCurrent, curr.termCurrent])
const query = reactive({
  bookId: curr.bookId,
  pageNumber: 1,
  pageSize: 20,
  assetCode: '',
  assetName: '',
  startPeriod: curr.termCurrent,
  endPeriod: curr.termCurrent
})

function handleQuery() {
  query.pageNumber = 1
  query.startPeriod = dateRange.value?.[0]
  query.endPeriod = dateRange.value?.[1]
  getList()
}

function getList() {
  loading.value = true
  query.startPeriod = dateRange.value?.[0]
  query.endPeriod = dateRange.value?.[1]
  listFixedAssetChange(query).then((res: any) => {
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  }).finally(() => {
    loading.value = false
  })
}

onMounted(getList)
</script>
