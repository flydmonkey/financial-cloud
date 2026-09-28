<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="72px"
      >
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            clearable
            placeholder="全部"
            style="width: 140px"
            @change="handleQuery"
          >
            <el-option
              label="盘点中"
              value="draft"
            />
            <el-option
              label="已完成"
              value="completed"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="handleQuery"
          >
            查询
          </el-button>
          <el-button
            type="primary"
            plain
            @click="openCreate"
          >
            新建盘点单
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
          prop="title"
          label="盘点单标题"
          min-width="160"
        />
        <el-table-column
          prop="checkDate"
          label="盘点日期"
          width="110"
        />
        <el-table-column
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'completed' ? 'success' : 'warning'">
              {{ row.status === 'completed' ? '已完成' : '盘点中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="totalCount"
          label="应盘"
          width="80"
          align="right"
        />
        <el-table-column
          prop="normalCount"
          label="正常"
          width="80"
          align="right"
        />
        <el-table-column
          label="盘盈"
          width="80"
          align="right"
        >
          <template #default="{ row }">
            <span :class="{ 'check-surplus': row.surplusCount > 0 }">{{ row.surplusCount }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="盘亏"
          width="80"
          align="right"
        >
          <template #default="{ row }">
            <span :class="{ 'check-deficit': row.deficitCount > 0 }">{{ row.deficitCount }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          label="备注"
          min-width="120"
          show-overflow-tooltip
        />
        <el-table-column
          label="操作"
          width="220"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="openDetail(row)"
            >
              明细
            </el-button>
            <el-button
              link
              type="primary"
              @click="printCheck(row)"
            >
              打印盘点表
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="success"
              @click="handleComplete(row)"
            >
              完成盘点
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="query.pageNumber"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <el-dialog
      v-model="createVisible"
      title="新建盘点单"
      width="480px"
    >
      <el-form
        label-width="88px"
      >
        <el-form-item
          label="标题"
          required
        >
          <el-input
            v-model="createForm.title"
            placeholder="如：2026年9月资产盘点"
          />
        </el-form-item>
        <el-form-item label="盘点日期">
          <el-date-picker
            v-model="createForm.checkDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="createForm.remark"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="createLoading"
          @click="submitCreate"
        >
          创建（自动快照在册资产）
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      :title="detail?.check?.title || '盘点明细'"
      size="72%"
    >
      <div
        v-if="detail"
        class="check-detail"
      >
        <div class="check-summary">
          <el-tag>应盘 {{ detail.check.totalCount }}</el-tag>
          <el-tag type="success">
            正常 {{ detail.check.normalCount }}
          </el-tag>
          <el-tag type="warning">
            盘盈 {{ detail.check.surplusCount }}
          </el-tag>
          <el-tag type="danger">
            盘亏 {{ detail.check.deficitCount }}
          </el-tag>
          <span class="check-tip">{{ detail.check.status === 'draft' ? '逐项录入实盘数量，完成后自动判定盘盈/盘亏' : '盘点已完成' }}</span>
        </div>
        <el-table
          v-loading="detailLoading"
          border
          :data="detail.items"
          max-height="calc(100vh - 220px)"
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
            prop="location"
            label="账面地点"
            width="120"
            show-overflow-tooltip
          />
          <el-table-column
            prop="bookQuantity"
            label="账面数量"
            width="90"
            align="right"
          />
          <el-table-column
            label="实盘数量"
            width="130"
          >
            <template #default="{ row }">
              <el-input-number
                v-model="row.actualQuantity"
                :min="0"
                :disabled="detail.check.status !== 'draft'"
                size="small"
                style="width: 110px"
                @change="saveItem(row)"
              />
            </template>
          </el-table-column>
          <el-table-column
            label="实盘地点"
            min-width="140"
          >
            <template #default="{ row }">
              <el-input
                v-model="row.actualLocation"
                :disabled="detail.check.status !== 'draft'"
                size="small"
                @change="saveItem(row)"
              />
            </template>
          </el-table-column>
          <el-table-column
            label="结果"
            width="90"
          >
            <template #default="{ row }">
              <el-tag
                v-if="row.result === 'normal'"
                type="success"
                size="small"
              >
                正常
              </el-tag>
              <el-tag
                v-else-if="row.result === 'surplus'"
                type="warning"
                size="small"
              >
                盘盈
              </el-tag>
              <el-tag
                v-else-if="row.result === 'deficit'"
                type="danger"
                size="small"
              >
                盘亏
              </el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column
            label="备注"
            min-width="120"
          >
            <template #default="{ row }">
              <el-input
                v-model="row.remark"
                :disabled="detail.check.status !== 'draft'"
                size="small"
                @change="saveItem(row)"
              />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="FixedAssetCheck">
import {
  listFixedAssetCheck,
  getFixedAssetCheck,
  createFixedAssetCheck,
  updateFixedAssetCheckItem,
  completeFixedAssetCheck,
  deleteFixedAssetCheck
} from '@/api/fixed-asset/check'
import bookStore from '@/store/modules/bookStore'
import modal from '@/plugins/modal'
import { openTablePrintWindow } from '@/utils/tablePrint'
import { reactive, ref, onMounted } from 'vue'

const curr = bookStore()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({
  bookId: curr.bookId,
  pageNumber: 1,
  pageSize: 20,
  status: ''
})

const createVisible = ref(false)
const createLoading = ref(false)
const createForm = reactive({
  title: '',
  checkDate: '',
  remark: ''
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)

function handleQuery() {
  query.pageNumber = 1
  getList()
}

function getList() {
  loading.value = true
  listFixedAssetCheck(query).then((res: any) => {
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function openCreate() {
  createForm.title = ''
  createForm.checkDate = ''
  createForm.remark = ''
  createVisible.value = true
}

function submitCreate() {
  if (!createForm.title.trim()) {
    modal.msgWarning('请填写盘点单标题')
    return
  }
  createLoading.value = true
  createFixedAssetCheck({ ...createForm }).then(() => {
    modal.msgSuccess('盘点单已创建')
    createVisible.value = false
    getList()
  }).finally(() => {
    createLoading.value = false
  })
}

function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  getFixedAssetCheck(row.id).then((res: any) => {
    detail.value = res.data
  }).finally(() => {
    detailLoading.value = false
  })
}

function resultLabel(result: string): string {
  if (result === 'normal') return '正常'
  if (result === 'surplus') return '盘盈'
  if (result === 'deficit') return '盘亏'
  return ''
}

function printCheck(row: any) {
  getFixedAssetCheck(row.id).then((res: any) => {
    const check = res.data?.check
    const items = res.data?.items || []
    const body = items.map((item: any) => `<tr>
      <td class="c">${item.assetCode ?? ''}</td>
      <td>${item.assetName ?? ''}</td>
      <td>${item.location ?? ''}</td>
      <td class="r">${item.bookQuantity ?? ''}</td>
      <td class="r">${item.actualQuantity ?? ''}</td>
      <td>${item.actualLocation ?? ''}</td>
      <td class="c">${resultLabel(item.result)}</td>
      <td>${item.remark ?? ''}</td>
    </tr>`).join('')
    openTablePrintWindow({
      title: '固定资产盘点表',
      subtitle: `${check.title}　盘点日期：${check.checkDate}　应盘 ${check.totalCount}　正常 ${check.normalCount}　盘盈 ${check.surplusCount}　盘亏 ${check.deficitCount}`,
      tableHtml: `<thead><tr>
        <th>资产编码</th><th>资产名称</th><th>账面地点</th><th>账面数量</th>
        <th>实盘数量</th><th>实盘地点</th><th>结果</th><th>备注</th>
      </tr></thead><tbody>${body}</tbody>`
    })
  })
}

function saveItem(row: any) {
  if (detail.value?.check?.status !== 'draft') {
    return
  }
  updateFixedAssetCheckItem({
    id: row.id,
    actualQuantity: row.actualQuantity,
    actualLocation: row.actualLocation,
    remark: row.remark
  }).then((res: any) => {
    row.result = res.data?.result
  })
}

function handleComplete(row: any) {
  modal.confirm('确认完成盘点？完成后将自动判定盘盈/盘亏，且不允许再修改。').then(() => {
    return completeFixedAssetCheck(row.id)
  }).then(() => {
    modal.msgSuccess('盘点已完成')
    getList()
  })
}

function handleDelete(row: any) {
  modal.confirm('确认删除该盘点单？').then(() => {
    return deleteFixedAssetCheck(row.id)
  }).then(() => {
    modal.msgSuccess('已删除')
    getList()
  })
}

onMounted(getList)
</script>

<style scoped>
.check-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.check-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.check-surplus {
  color: var(--el-color-warning);
  font-weight: 600;
}
.check-deficit {
  color: var(--el-color-danger);
  font-weight: 600;
}
</style>
