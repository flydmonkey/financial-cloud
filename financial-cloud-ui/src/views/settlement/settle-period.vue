<!--月结向导-->
<template>
  <div class="app-container">
    <el-card class="common-card">
      <el-tabs
        v-model="activeName"
        type="card"
        class="demo-tabs"
        @tab-click="handleClick"
      >
        <el-tab-pane
          label="期末结转"
          name="carry-forward"
        />
        <el-tab-pane
          label="月结"
          name="settle-period"
        >
          <div class="queryForm">
            <el-form
              :inline="true"
              label-width="120px"
            >
              <el-form-item label="当前账期：">
                <strong>{{ currentTerm }}</strong>
                <el-tag
                  size="small"
                  type="info"
                  style="margin-left: 8px"
                >
                  仅月结（无独立年结）
                </el-tag>
              </el-form-item>
            </el-form>
          </div>

          <el-steps
            :active="active"
            finish-status="success"
            align-center
          >
            <el-step title="人工确认" />
            <el-step title="计提与结转" />
            <el-step title="系统校验" />
            <el-step title="结账" />
          </el-steps>

          <!-- Step 0: manual confirmation -->
          <div
            v-if="active === 0"
            class="step-body"
          >
            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="以下为人工确认项（本期不系统检）。往来应收应付与账龄已在「系统校验」中计算（逾期仅警告，不阻断结账）。"
              style="margin-bottom: 12px"
            />
            <el-table
              :data="manualCheckData"
              border
              style="width: 100%"
            >
              <el-table-column
                prop="item"
                label="检查项目"
                width="180"
              />
              <el-table-column
                prop="content"
                label="说明"
              />
            </el-table>
            <el-checkbox
              v-model="manualAck"
              style="margin-top: 12px"
            >
              我已人工确认上述事项（或确认本期不适用）
            </el-checkbox>
          </div>

          <!-- Step 1: accrue / carry -->
          <div
            v-if="active === 1"
            class="step-body"
          >
            <el-alert
              type="warning"
              :closable="false"
              show-icon
              title="请完成必做损益结转；有应折旧资产时请先计提折旧。完成后返回本页刷新状态。"
              style="margin-bottom: 12px"
            />
            <el-row :gutter="16">
              <el-col :span="12">
                <el-card shadow="never">
                  <template #header>
                    <span>固定资产折旧</span>
                    <el-tag
                      :type="deprStatusTag"
                      size="small"
                      style="margin-left: 8px"
                    >
                      {{ deprStatusLabel }}
                    </el-tag>
                  </template>
                  <p class="hint">
                    {{ deprHint }}
                  </p>
                  <el-button
                    type="primary"
                    link
                    @click="goDepreciation"
                  >
                    打开折旧计提
                  </el-button>
                  <el-button
                    link
                    @click="refreshAccrueCarry"
                  >
                    刷新状态
                  </el-button>
                </el-card>
              </el-col>
              <el-col :span="12">
                <el-card shadow="never">
                  <template #header>
                    <span>必做损益结转</span>
                    <el-tag
                      :type="carryReady ? 'success' : 'danger'"
                      size="small"
                      style="margin-left: 8px"
                    >
                      {{ carryReady ? '已完成' : '未完成' }}
                    </el-tag>
                  </template>
                  <el-table
                    :data="carryStatusRows"
                    size="small"
                    border
                  >
                    <el-table-column
                      prop="name"
                      label="项目"
                    />
                    <el-table-column
                      prop="status"
                      label="状态"
                      width="100"
                    />
                  </el-table>
                  <div style="margin-top: 8px">
                    <el-button
                      type="primary"
                      link
                      @click="goCarryForward"
                    >
                      打开期末结转
                    </el-button>
                    <el-button
                      link
                      @click="refreshAccrueCarry"
                    >
                      刷新状态
                    </el-button>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <!-- Step 2: system verify -->
          <div
            v-if="active === 2"
            class="step-body"
          >
            <el-table
              v-loading="loadingVerify"
              :data="tableVerifyData"
              border
              style="width: 100%"
            >
              <el-table-column
                prop="id"
                label="序号"
                width="70"
              />
              <el-table-column
                prop="item"
                label="检查项目"
                width="200"
              />
              <el-table-column
                label="类型"
                width="100"
              >
                <template #default="scope">
                  <el-tag
                    size="small"
                    :type="scope.row.warning ? 'warning' : (scope.row.hard === false ? 'info' : 'danger')"
                  >
                    {{ scope.row.warning ? '警告' : (scope.row.hard === false ? '人工' : '硬检') }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                label="结果"
                width="120"
              >
                <template #default="scope">
                  <span v-if="scope.row.applicable === false">不适用</span>
                  <span v-else-if="scope.row.warning === true && scope.row.result === true">
                    <el-icon color="#E6A23C"><WarningFilled /></el-icon>
                  </span>
                  <span v-else-if="scope.row.result === true"><el-icon color="#67C23A"><Select /></el-icon></span>
                  <span v-else-if="scope.row.result === false"><el-icon color="#F56C6C"><CloseBold /></el-icon></span>
                </template>
              </el-table-column>
              <el-table-column
                prop="reason"
                label="说明"
              />
            </el-table>
          </div>

          <!-- Step 3: checkout result / action -->
          <div
            v-if="active === 3"
            class="step-body"
          >
            <el-row v-if="isCheckout">
              <el-col
                v-if="checkoutResult"
                :sm="12"
                :lg="8"
              >
                <el-result
                  icon="success"
                  title="结账成功"
                  sub-title="本期月结已完成，已进入下一个账期"
                />
              </el-col>
              <el-col
                v-if="!checkoutResult"
                :sm="12"
                :lg="8"
              >
                <el-result
                  icon="error"
                  title="结账失败"
                  :sub-title="checkoutErrorMsg || '请检查硬门槛后再结账'"
                />
              </el-col>
            </el-row>
            <el-alert
              v-else
              type="success"
              :closable="false"
              show-icon
              title="系统硬检已通过，确认后执行结账（将锁定本期并推进账期）"
            />
          </div>

          <div style="margin-top: 16px">
            <el-button
              v-if="active > 0 && active < 3 && !isCheckout"
              @click="active--"
            >
              上一步
            </el-button>
            <el-button
              v-if="active === 0"
              type="primary"
              :disabled="!manualAck"
              @click="goStep1"
            >
              下一步
            </el-button>
            <el-button
              v-if="active === 1"
              type="primary"
              @click="goStep2"
            >
              下一步：系统校验
            </el-button>
            <el-button
              v-if="active === 2"
              v-loading="loadingVerify"
              type="primary"
              @click="handleVerify"
            >
              重新检查
            </el-button>
            <el-button
              v-if="active === 2 && isVerify"
              type="primary"
              @click="active = 3"
            >
              下一步：结账
            </el-button>
            <el-button
              v-if="active === 3 && !isCheckout"
              v-loading="checkoutButtonLoading"
              type="primary"
              :disabled="!canCheckout"
              @click="handleCheckout"
            >
              结账
            </el-button>
            <el-button
              v-if="isCheckout"
              type="primary"
              @click="handleConfirm"
            >
              确定
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane
          label="结账列表"
          name="settle-list"
        />
      </el-tabs>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {ref, computed, onMounted, onActivated} from 'vue'
import type {TabsPaneContext} from 'element-plus'
import {getCurrentInstance} from 'vue'
import * as settlementApi from '@/api/book/settlement'
import {fetchDepreciationStatus} from '@/api/fixed-asset/depreciation'
import bookStore from '@/store/modules/bookStore'

const currBookStore = bookStore()
const currentTerm = ref(currBookStore.termCurrent)
const active = ref(0)
const isVerify = ref(false)
const isCheckout = ref(false)
const checkoutResult = ref(false)
const checkoutErrorMsg = ref('')
const loadingVerify = ref(false)
const checkoutButtonLoading = ref(false)
const manualAck = ref(false)
const proxy: any = getCurrentInstance()!.proxy
const activeName = ref('settle-period')

const tableVerifyData: any = ref<any[]>([])
const carryStatusRows = ref<{code: string; name: string; status: string; done: boolean}[]>([])
const deprAccrued = ref(false)
const deprNeeded = ref(true)

const REQUIRED_CARRY_CODES = ['qm_jz_sr', 'qm_jz_cbfy']

const manualCheckData = [
  {
    item: '检查银行和现金',
    content: '【人工确认 / 本期不系统检】余额与银行对账单、库存现金盘点表比对（系统暂无银行余额调节）',
  },
  {
    item: '检查存货',
    content: '【人工确认 / 本期不系统检】账实一致（本产品不含进销存）',
  },
  {
    item: '检查主营业务收入',
    content: '【人工确认 / 本期不系统检】与税控发票统计及无票收入备查勾稽',
  },
  {
    item: '检查工资、五险一金',
    content: '【人工确认】可结合薪资模块核对计提与发放',
  },
  {
    item: '检查税费',
    content: '【人工确认 / 本期不系统检】增值税结转、附加税与所得税计提请人工确认',
  },
  {
    item: '检查原始凭证',
    content: '【人工确认】费用审批与附件归档',
  },
  {
    item: '检查科目余额表',
    content: '【人工确认】核对发生额、余额方向是否异常',
  },
]

const canCheckout = computed(() => manualAck.value && isVerify.value && !isCheckout.value)

const deprStatusTag = computed(() => {
  if (!deprNeeded.value) return 'info'
  return deprAccrued.value ? 'success' : 'danger'
})
const deprStatusLabel = computed(() => {
  if (!deprNeeded.value) return '不适用'
  return deprAccrued.value ? '已计提' : '未计提'
})
const deprHint = computed(() => {
  if (!deprNeeded.value) return '本期无应计提折旧的资产（以系统校验结果为准）。'
  return deprAccrued.value ? '本期折旧已计提。' : '请先完成固定资产折旧计提。'
})
const carryReady = computed(() =>
  carryStatusRows.value.length > 0 && carryStatusRows.value.every((r) => r.done),
)

async function refreshAccrueCarry() {
  currentTerm.value = currBookStore.termCurrent
  try {
    const carryRes: any = await settlementApi.fetchcarry({
      pageNumber: 1,
      pageSize: 50,
      category: 1,
    })
    const records = carryRes?.data?.records || []
    const month = String(currentTerm.value || '').slice(5, 7)
    const codes = [...REQUIRED_CARRY_CODES]
    if (month === '12') {
      codes.push('qm_jz_bnlr')
    }
    carryStatusRows.value = codes.map((code) => {
      const row = records.find((r: any) => r.code === code)
      const done = !!(row && row.voucherId)
      return {
        code,
        name: row?.name || code,
        status: !row ? '无模板' : done ? '已生成' : '未生成',
        done: !row && code === 'qm_jz_bnlr' ? true : done,
      }
    })
  } catch {
    carryStatusRows.value = []
  }

  try {
    const deprRes: any = await fetchDepreciationStatus({yearPeriod: currentTerm.value})
    deprAccrued.value = !!deprRes?.data?.accrued
  } catch {
    deprAccrued.value = false
  }

  // Soft hint from verify depreciation N/A if available
  try {
    const verifyRes: any = await settlementApi.verify()
    const deprItem = (verifyRes?.data || []).find((i: any) => i.item === '固定资产折旧')
    if (deprItem) {
      deprNeeded.value = deprItem.applicable !== false
      if (deprItem.applicable === false) {
        deprAccrued.value = true
      } else {
        deprAccrued.value = !!deprItem.result
      }
    }
  } catch {
    // ignore — hard gate still enforced on checkout
  }
}

function goStep1() {
  if (!manualAck.value) return
  active.value = 1
  refreshAccrueCarry()
}

function goStep2() {
  active.value = 2
  handleVerify()
}

function goCarryForward() {
  proxy.$tab.openPage('/settlement/carry-forward')
}

function goDepreciation() {
  proxy.$tab.openPage('/fixed-asset/depreciation')
}

function handleCheckout() {
  if (!canCheckout.value) return
  checkoutButtonLoading.value = true
  checkoutErrorMsg.value = ''
  const year = (currentTerm.value + '').substring(0, 4)
  settlementApi
    .checkout({year, date: currentTerm.value})
    .then((res: any) => {
      checkoutButtonLoading.value = false
      isCheckout.value = true
      if (res.code === 0) {
        checkoutResult.value = true
        currBookStore.getBookItem()
      } else {
        checkoutResult.value = false
        checkoutErrorMsg.value = res.message || ''
      }
    })
    .catch((err: any) => {
      checkoutButtonLoading.value = false
      isCheckout.value = true
      checkoutResult.value = false
      checkoutErrorMsg.value = err?.message || err?.data?.message || ''
    })
}

function handleVerify() {
  loadingVerify.value = true
  isVerify.value = false
  settlementApi
    .verify()
    .then((res: any) => {
      loadingVerify.value = false
      tableVerifyData.value = res.data || []
      const hardFailed = (res.data || []).some(
        (row: any) => row.hard !== false && row.applicable !== false && row.result === false,
      )
      isVerify.value = res.code === 0 && !hardFailed
      const deprItem = (res.data || []).find((i: any) => i.item === '固定资产折旧')
      if (deprItem) {
        deprNeeded.value = deprItem.applicable !== false
      }
    })
    .catch((err: any) => {
      loadingVerify.value = false
      tableVerifyData.value = err?.data || []
      isVerify.value = false
    })
}

function handleConfirm() {
  window.location.reload()
}

const handleClick = (tab: TabsPaneContext) => {
  proxy.$tab.openPage('/settlement/' + tab.paneName)
}

onMounted(() => {
  refreshAccrueCarry()
})

onActivated(() => {
  if (active.value === 1) {
    refreshAccrueCarry()
  }
})
</script>

<style scoped>
.step-body {
  margin-top: 16px;
}
.hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 0 0 8px;
}
</style>
