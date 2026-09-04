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
            <el-step title="凭证整理" />
            <el-step title="计提与结转" />
            <el-step title="系统校验" />
            <el-step title="结账" />
          </el-steps>

          <StepManual
            v-if="active === 0"
          />
          <StepVoucherPrep
            v-else-if="active === 1"
            :unposted="unposted"
            :successive-gaps="successiveGaps"
            :can-leave-step1="canLeaveStep1"
            :loading-step1="loadingStep1"
            :loading-action="loadingAction"
            @refresh="onRefreshStep1"
            @submit-audit-post="onSubmitAuditPost"
            @fix-successive="onFixSuccessive"
          />
          <StepAccrueCarry
            v-else-if="active === 2"
            :carry-rows="carryRows"
            :depr-needed="deprNeeded"
            :depr-accrued="deprAccrued"
            :can-leave-step2="canLeaveStep2"
            :loading-step2="loadingStep2"
            :loading-action="loadingAction"
            @refresh="onRefreshStep2"
            @accrue-depreciation="onAccrueDepreciation"
            @generate-and-post="onGenerateAndPost"
            @open-carry-advanced="goCarryForward"
          />
          <StepVerify
            v-else-if="active === 3"
            :verify-rows="verifyRows"
            :is-verify="isVerify"
            :loading-verify="loadingVerify"
            @jump="onVerifyJump"
          />
          <StepCheckout
            v-else-if="active === 4"
            :is-checkout="isCheckout"
            :checkout-ok="checkoutOk"
            :checkout-error="checkoutError"
            @back-to-verify="backToVerify"
          />

          <div class="wizard-footer">
            <div class="wizard-footer-left">
              <el-checkbox
                v-if="active === 0 && !isCheckout"
                v-model="manualAck"
              >
                我已人工确认上述事项（或确认本期不适用）
              </el-checkbox>
              <el-button
                v-if="active > 0 && !isCheckout"
                @click="goPrev"
              >
                上一步
              </el-button>
            </div>
            <div class="wizard-footer-right">
              <el-button
                v-if="active === 3 && !isCheckout"
                :loading="loadingVerify"
                @click="onRunVerify"
              >
                重新检查
              </el-button>
              <el-button
                v-if="active < 4 && !isCheckout"
                type="primary"
                :disabled="!canLeaveCurrent"
                :loading="nextLoading"
                @click="goNext"
              >
                {{ nextLabel }}
              </el-button>
              <el-button
                v-if="active === 4 && !isCheckout"
                type="primary"
                :loading="loadingCheckout"
                :disabled="!isVerify"
                @click="onCheckout"
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
import { computed, getCurrentInstance, onActivated, ref } from 'vue'
import type { TabsPaneContext } from 'element-plus'
import { ElMessage } from 'element-plus'
import bookStore from '@/store/modules/bookStore'
import StepManual from './wizard/StepManual.vue'
import StepVoucherPrep from './wizard/StepVoucherPrep.vue'
import StepAccrueCarry from './wizard/StepAccrueCarry.vue'
import StepVerify from './wizard/StepVerify.vue'
import StepCheckout from './wizard/StepCheckout.vue'
import { useMonthEndWizard, jumpTargetForItem } from './wizard/useMonthEndWizard'

const proxy: any = getCurrentInstance()!.proxy
const currBookStore = bookStore()
const currentTerm = computed(() => currBookStore.termCurrent)
const activeName = ref('settle-period')

const {
  active,
  manualAck,
  unposted,
  successiveGaps,
  carryRows,
  deprNeeded,
  deprAccrued,
  verifyRows,
  isVerify,
  isCheckout,
  checkoutOk,
  checkoutError,
  loadingStep1,
  loadingStep2,
  loadingVerify,
  loadingCheckout,
  loadingAction,
  canLeaveStep0,
  canLeaveStep1,
  canLeaveStep2,
  canLeaveStep3,
  refreshStep1,
  submitAuditPost,
  fixSuccessive,
  refreshStep2,
  generateAndPostCarry,
  accrueDepreciation,
  runVerify,
  checkout,
  backToVerify,
} = useMonthEndWizard()

const nextLoading = ref(false)

const canLeaveCurrent = computed(() => {
  switch (active.value) {
    case 0:
      return canLeaveStep0.value
    case 1:
      return canLeaveStep1.value
    case 2:
      return canLeaveStep2.value
    case 3:
      return canLeaveStep3.value
    default:
      return false
  }
})

const nextLabel = computed(() => {
  switch (active.value) {
    case 0:
      return '下一步'
    case 1:
      return '下一步：计提与结转'
    case 2:
      return '下一步：系统校验'
    case 3:
      return '下一步：结账'
    default:
      return '下一步'
  }
})

function gateWarningMessage(): string {
  switch (active.value) {
    case 0:
      return '请先勾选人工确认'
    case 1: {
      const parts: string[] = []
      if (unposted.value.length) parts.push(`未过账 ${unposted.value.length} 张`)
      if (successiveGaps.value.length) parts.push(`断号 ${successiveGaps.value.length} 处`)
      return parts.length ? `尚不能进入下一步：${parts.join('，')}` : '尚不能进入下一步'
    }
    case 2:
      return '请先完成本期折旧（如适用）与必做结转的生成并过账'
    case 3:
      return '请先通过系统硬检'
    default:
      return '尚不能进入下一步'
  }
}

function goPrev() {
  if (active.value <= 0) return
  active.value = (active.value - 1) as typeof active.value
  if (active.value === 1) {
    refreshStep1()
  } else if (active.value === 2) {
    refreshStep2()
  }
}

async function goNext() {
  if (!canLeaveCurrent.value) {
    ElMessage.warning(gateWarningMessage())
    return
  }
  nextLoading.value = true
  try {
    if (active.value === 0) {
      active.value = 1
      await refreshStep1()
      return
    }
    if (active.value === 1) {
      active.value = 2
      await refreshStep2()
      return
    }
    if (active.value === 2) {
      active.value = 3
      await runVerify()
      return
    }
    if (active.value === 3) {
      active.value = 4
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    nextLoading.value = false
  }
}

async function onRefreshStep1() {
  try {
    await refreshStep1()
  } catch (err: any) {
    ElMessage.error(err?.message || '刷新失败')
  }
}

async function onSubmitAuditPost() {
  try {
    await submitAuditPost([])
    ElMessage.success('批量提交审核过账完成')
  } catch (err: any) {
    ElMessage.error(err?.message || '批量过账失败')
  }
}

async function onFixSuccessive() {
  try {
    await fixSuccessive()
    ElMessage.success('断号整理完成')
  } catch (err: any) {
    ElMessage.error(err?.message || '整理断号失败')
  }
}

async function onRefreshStep2() {
  try {
    await refreshStep2()
  } catch (err: any) {
    ElMessage.error(err?.message || '刷新失败')
  }
}

async function onAccrueDepreciation() {
  try {
    await accrueDepreciation()
    ElMessage.success('折旧计提完成')
  } catch (err: any) {
    ElMessage.error(err?.message || '折旧计提失败')
  }
}

async function onGenerateAndPost(code: string) {
  try {
    await generateAndPostCarry(code)
  } catch (err: any) {
    ElMessage.error(err?.message || '生成并过账失败')
  }
}

async function onRunVerify() {
  try {
    await runVerify()
  } catch (err: any) {
    ElMessage.error(err?.message || '校验失败')
  }
}

async function onCheckout() {
  try {
    await checkout()
  } catch (err: any) {
    ElMessage.error(err?.message || '结账失败')
  }
}

function onVerifyJump(item: string) {
  active.value = jumpTargetForItem(item)
  if (active.value === 1) {
    refreshStep1()
  } else if (active.value === 2) {
    refreshStep2()
  }
}

function goCarryForward() {
  proxy.$tab.openPage('/settlement/carry-forward')
}

function handleConfirm() {
  window.location.reload()
}

const handleClick = (tab: TabsPaneContext) => {
  proxy.$tab.openPage('/settlement/' + tab.paneName)
}

onActivated(() => {
  if (active.value === 1) {
    refreshStep1()
  } else if (active.value === 2) {
    refreshStep2()
  }
})
</script>

<style scoped>
.wizard-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.wizard-footer-left,
.wizard-footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.wizard-footer-right {
  margin-left: auto;
}
</style>
