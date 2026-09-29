<template>
  <div class="app-container">
    <el-card
      v-loading="loading"
      class="common-card"
    >
      <template #header>
        <span>凭证与结账参数</span>
      </template>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="仅影响当前账套。凭证审核与账套编辑为同一字段；往来校验为软提示，关闭后月结向导不再展示该项。"
        style="margin-bottom: 16px"
      />
      <el-form
        label-width="160px"
        @submit.prevent
      >
        <el-divider content-position="left">凭证</el-divider>
        <el-form-item label="凭证审核">
          <el-switch
            v-model="voucherReviewedOn"
            :disabled="!canEdit"
          />
          <span class="hint">开启后制单人与审核人分离，待审核凭证进入首页待办。</span>
        </el-form-item>
        <el-form-item label="断号整理">
          <span class="hint">不在本页执行。请到凭证列表或月末结账向导「凭证整理」步骤检查并补齐断号。</span>
          <div style="margin-top: 8px">
            <el-button @click="router.push('/voucher/voucher-index')">凭证列表</el-button>
            <el-button @click="router.push('/settlement/settle-list')">月末结账</el-button>
          </div>
        </el-form-item>
        <el-divider content-position="left">结账校验</el-divider>
        <el-form-item label="硬闸（不可关闭）">
          <ul class="hard-gates">
            <li
              v-for="(label, idx) in hardGateLabels"
              :key="idx"
            >
              {{ label }}
            </li>
          </ul>
        </el-form-item>
        <el-form-item label="往来账龄提示">
          <el-switch
            v-model="arapVerifyEnabled"
            :disabled="!canEdit"
          />
          <span class="hint">关闭后月结系统校验不再列出往来汇总；逾期仍不阻断结账。</span>
        </el-form-item>
        <el-form-item v-if="canEdit">
          <el-button
            type="primary"
            :loading="saving"
            @click="save"
          >
            保存
          </el-button>
        </el-form-item>
        <el-form-item v-else>
          <span class="hint">仅账套管理员可修改。</span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import {onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {getVoucherSettlementParams, saveVoucherSettlementParams} from "@/api/config/voucher-settlement";
import booksSetStore from "@/store/modules/bookStore";
import modal from "@/plugins/modal";

const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const canEdit = ref(false);
const voucherReviewedOn = ref(false);
const arapVerifyEnabled = ref(true);
const hardGateLabels = ref<string[]>([]);

function load(): void {
  loading.value = true;
  getVoucherSettlementParams().then((res: any) => {
    if (res.code === 0 && res.data) {
      voucherReviewedOn.value = res.data.voucherReviewed === 1;
      arapVerifyEnabled.value = !!res.data.arapVerifyEnabled;
      canEdit.value = !!res.data.canEdit;
      hardGateLabels.value = res.data.hardGateLabels || [];
    }
  }).finally(() => {
    loading.value = false;
  });
}

function save(): void {
  saving.value = true;
  saveVoucherSettlementParams({
    voucherReviewed: voucherReviewedOn.value ? 1 : 0,
    arapVerifyEnabled: arapVerifyEnabled.value
  }).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess(res.message || "保存成功");
      booksSetStore().refreshData();
      load();
    } else {
      modal.msgError(res.message || "保存失败");
    }
  }).catch((error: any) => {
    modal.msgError(error?.response?.data?.message || error?.message || "保存失败");
  }).finally(() => {
    saving.value = false;
  });
}

onMounted(load);
</script>

<style scoped lang="scss">
.hint {
  margin-left: 8px;
  color: #909399;
  font-size: 13px;
}

.hard-gates {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  line-height: 1.7;
}
</style>
