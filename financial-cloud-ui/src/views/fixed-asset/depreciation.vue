<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <el-form
        :inline="true"
        label-width="80px"
      >
        <el-form-item label="会计期间">
          <el-date-picker
            v-model="yearPeriod"
            type="month"
            value-format="YYYY-MM"
            format="YYYY年MM期"
            :clearable="false"
            @change="refresh"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="openParams">
            凭证参数
          </el-button>
          <el-button
            type="primary"
            :loading="accruing"
            :disabled="status.accrued && !status.canReaccrue"
            @click="handleAccrue"
          >
            {{ accrueButtonLabel }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card
      class="common-card"
      style="margin-bottom: 12px"
    >
      <el-descriptions
        title="本期计提状态"
        :column="3"
        border
      >
        <el-descriptions-item label="期间">
          {{ yearPeriod || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="是否已计提">
          <el-tag
            :type="status.accrued ? 'success' : 'info'"
            size="small"
          >
            {{ status.accrued ? '已计提' : '未计提' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="计提金额">
          {{ status.accrued ? (status.totalAmount ?? '-') : '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="折旧凭证">
          <el-link
            v-if="status.voucherId"
            type="primary"
            @click="goVoucher"
          >
            {{ status.voucherWord || status.voucherId }}
          </el-link>
          <span
            v-else
            class="muted"
          >尚未生成</span>
        </el-descriptions-item>
        <el-descriptions-item label="可否重提">
          <template v-if="!status.accrued">
            <span class="muted">尚未计提</span>
          </template>
          <el-tag
            v-else-if="status.canReaccrue"
            type="warning"
            size="small"
          >
            可以（凭证未审核/过账）
          </el-tag>
          <el-tag
            v-else
            type="danger"
            size="small"
          >
            禁止（已审核或已过账）
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="!status.accrued"
        class="empty-tip"
        type="info"
        :closable="false"
        show-icon
        title="本期尚未计提折旧"
        description="确认期间与凭证参数后，点击「计提折旧」生成一张折旧凭证。新增当月不提，次月起提；清理当月仍计提；暂停资产自暂停期间起不再计提。"
      />
      <el-alert
        v-else-if="status.canReaccrue"
        class="empty-tip"
        type="warning"
        :closable="false"
        show-icon
        title="已计提，可重新计提"
        description="重新计提会删除本期未审核的折旧凭证并按当前卡片重算。已审核或已过账的凭证不能重提。"
      />
      <el-alert
        v-else
        class="empty-tip"
        type="success"
        :closable="false"
        show-icon
        title="本期折旧已锁定"
        description="折旧凭证已审核或已过账，不能重新计提。可点击凭证字号查看或修改凭证状态后再操作。"
      />
    </el-card>

    <el-card
      v-if="workList.length > 0"
      class="common-card"
      style="margin-bottom: 12px"
    >
      <template #header>
        <div class="card-header">
          <span>本期工作量录入（工作量法资产）</span>
          <el-button
            type="primary"
            link
            @click="saveWork"
          >
            保存工作量
          </el-button>
        </div>
      </template>
      <el-alert
        class="work-tip"
        type="info"
        :closable="false"
        show-icon
        title="请先录入并保存本期工作量，再计提折旧；未录入或为 0 的资产本期折旧为 0。"
      />
      <el-table
        border
        :data="workList"
      >
        <el-table-column
          prop="code"
          label="编码"
          width="100"
        />
        <el-table-column
          prop="name"
          label="名称"
          min-width="140"
        />
        <el-table-column
          prop="expectedTotalWork"
          label="预计总工作量"
          width="130"
          align="right"
        />
        <el-table-column
          label="本期工作量"
          width="180"
        >
          <template #default="{ row }">
            <el-input-number
              v-model="row.periodWork"
              :min="0"
              :precision="2"
              controls-position="right"
              style="width: 100%"
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card
      v-if="result"
      class="common-card success-panel"
    >
      <el-result
        icon="success"
        title="计提成功"
        :sub-title="`期间 ${result.yearPeriod}，合计 ${result.totalAmount}`"
      >
        <template #extra>
          <el-button
            type="primary"
            @click="goResultVoucher"
          >
            查看凭证 {{ result.voucherWord }}
          </el-button>
          <el-button @click="goDetailReport">
            查看折旧明细
          </el-button>
          <el-button
            v-if="status.canReaccrue"
            @click="handleAccrue"
          >
            重新计提
          </el-button>
          <el-button @click="router.push('/fixed-asset/card')">
            返回卡片
          </el-button>
        </template>
      </el-result>
    </el-card>

    <el-dialog
      v-model="paramsVisible"
      title="凭证参数"
      width="420px"
    >
      <el-form
        label-width="90px"
      >
        <el-form-item label="凭证日期">
          <el-date-picker
            v-model="params.voucherDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="凭证字">
          <el-input v-model="params.voucherWord" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="params.summary" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paramsVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="saveParams"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="FixedAssetDepreciation">
import {
  getDepreciationStatus,
  getDepreciationParams,
  saveDepreciationParams,
  listDepreciationWork,
  saveDepreciationWork,
  accrueDepreciation
} from '@/api/fixed-asset/depreciation'
import bookStore from '@/store/modules/bookStore'
import modal from '@/plugins/modal'
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const currBookStore = bookStore()
const yearPeriod = ref('')
const accruing = ref(false)
const paramsVisible = ref(false)
const workList = ref<any[]>([])
const result = ref<any>(null)
const status = reactive({
  accrued: false,
  voucherId: '',
  voucherWord: '',
  totalAmount: null as number | null,
  canReaccrue: false
})
const params = reactive({
  yearPeriod: '',
  voucherDate: '',
  voucherWord: '记',
  summary: '计提折旧费用'
})

const accrueButtonLabel = computed(() => {
  if (!status.accrued) return '计提折旧'
  if (status.canReaccrue) return '重新计提'
  return '已锁定'
})

function refresh() {
  result.value = null
  loadStatus()
  loadWork()
  loadParams()
}

function loadStatus() {
  getDepreciationStatus({ yearPeriod: yearPeriod.value }).then((res: any) => {
    Object.assign(status, {
      accrued: !!res.data?.accrued,
      voucherId: res.data?.voucherId || '',
      voucherWord: res.data?.voucherWord || '',
      totalAmount: res.data?.totalAmount ?? null,
      canReaccrue: !!res.data?.canReaccrue
    })
    if (res.data?.yearPeriod) {
      yearPeriod.value = res.data.yearPeriod
    }
  })
}

function loadWork() {
  listDepreciationWork({ yearPeriod: yearPeriod.value }).then((res: any) => {
    workList.value = res.data || []
  })
}

function loadParams() {
  getDepreciationParams({ yearPeriod: yearPeriod.value }).then((res: any) => {
    Object.assign(params, {
      yearPeriod: res.data?.yearPeriod || yearPeriod.value,
      voucherDate: res.data?.voucherDate || '',
      voucherWord: res.data?.voucherWord || '记',
      summary: res.data?.summary || '计提折旧费用'
    })
  })
}

function openParams() {
  loadParams()
  paramsVisible.value = true
}

function saveParams() {
  params.yearPeriod = yearPeriod.value
  saveDepreciationParams(params).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess('参数已保存')
      paramsVisible.value = false
    }
  })
}

function saveWork() {
  saveDepreciationWork(workList.value, yearPeriod.value).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess(res.message || '工作量已保存')
    }
  })
}

function handleAccrue() {
  if (status.accrued && !status.canReaccrue) {
    modal.msgError('本期折旧凭证已审核或已过账，禁止重新计提')
    return
  }
  const tip = status.accrued
    ? '确认重新计提？将删除本期未审核折旧凭证并按当前卡片重算。'
    : '确认计提本期折旧并生成一张折旧凭证？'
  modal.confirm(tip).then(() => {
    accruing.value = true
    return accrueDepreciation({
      yearPeriod: yearPeriod.value,
      voucherDate: params.voucherDate || undefined,
      voucherWord: params.voucherWord,
      summary: params.summary
    })
  }).then((res: any) => {
    if (res.code === 0) {
      result.value = res.data
      modal.msgSuccess('计提成功')
      loadStatus()
      loadWork()
    }
  }).catch(() => undefined).finally(() => {
    accruing.value = false
  })
}

function goVoucher() {
  if (status.voucherId) {
    router.push({ path: '/voucher/voucher-edit', query: { id: status.voucherId } })
  }
}

function goResultVoucher() {
  if (result.value?.voucherId) {
    router.push({ path: '/voucher/voucher-edit', query: { id: result.value.voucherId } })
  }
}

function goDetailReport() {
  const p = result.value?.yearPeriod || yearPeriod.value
  router.push({
    path: '/fixed-asset/depreciation-detail',
    query: { startPeriod: p, endPeriod: p }
  })
}

onMounted(() => {
  getDepreciationStatus({}).then((res: any) => {
    yearPeriod.value = res.data?.yearPeriod || ''
    Object.assign(status, {
      accrued: !!res.data?.accrued,
      voucherId: res.data?.voucherId || '',
      voucherWord: res.data?.voucherWord || '',
      totalAmount: res.data?.totalAmount ?? null,
      canReaccrue: !!res.data?.canReaccrue
    })
    loadWork()
    loadParams()
  })
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.empty-tip {
  margin-top: 12px;
}
.work-tip {
  margin-bottom: 12px;
}
.muted {
  color: var(--el-text-color-secondary);
}
</style>
