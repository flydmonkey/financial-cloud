<template>
  <el-card
    shadow="hover"
    header-class="el-card-header"
    class="asset-count-card"
    @click="goCards"
  >
    <template #header>
      <div class="card-title">
        <span>固定资产</span>
        <span class="link-hint">卡片列表 ›</span>
      </div>
    </template>

    <div
      v-loading="loading"
      class="card-content"
    >
      <div class="card-content-item bold">
        <div class="flex justify-items-center">
          <span>资产总数</span>
          <el-tooltip
            content="当前账套在册固定资产卡片数（不含已清理）"
            placement="top"
          >
            <el-icon><Warning /></el-icon>
          </el-tooltip>
        </div>
        <div>{{ resData.totalCount }}</div>
      </div>
      <div class="card-content-item">
        <div>正常使用</div>
        <div>{{ resData.inUseCount }}</div>
      </div>
      <div class="card-content-item">
        <div>暂停计提</div>
        <div>{{ resData.suspendedCount }}</div>
      </div>
      <div class="card-content-item bold">
        <div>原值合计</div>
        <div>{{ formatAmount(resData.originalValueSum) }}</div>
      </div>
      <div class="card-content-item bold">
        <div class="flex justify-items-center">
          <span>净值合计</span>
          <el-tooltip
            content="原值 − 累计折旧 − 减值"
            placement="top"
          >
            <el-icon><Warning /></el-icon>
          </el-tooltip>
        </div>
        <div>{{ formatAmount(resData.netValueSum) }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref, toRefs, watch} from "vue";
import {useRouter} from "vue-router";
import {Warning} from "@element-plus/icons-vue";
import {statisticsFixedAssetCount} from "@/api/dashboard";
import booksSetStore from "@/store/modules/bookStore";

const router = useRouter();
const currBookStore = booksSetStore();
const loading = ref(false);

const data = reactive({
  resData: {
    totalCount: 0,
    inUseCount: 0,
    suspendedCount: 0,
    originalValueSum: 0,
    netValueSum: 0
  }
});
const {resData} = toRefs(data);

function formatAmount(value: any): string {
  const n = Number(value || 0);
  return n.toLocaleString("zh-CN", {minimumFractionDigits: 2, maximumFractionDigits: 2});
}

function goCards(): void {
  router.push("/fixed-asset/card");
}

function getList(): void {
  if (!currBookStore.bookId) {
    return;
  }
  loading.value = true;
  statisticsFixedAssetCount().then((res: any) => {
    if (res.code === 0 && res.data) {
      resData.value = res.data;
    }
  }).finally(() => {
    loading.value = false;
  });
}

onMounted(getList);
watch(() => currBookStore.bookId, getList);
</script>

<style scoped lang="scss">
.asset-count-card {
  cursor: pointer;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.link-hint {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 4px 0;
}

.card-content-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #606266;

  &.bold {
    font-weight: 600;
    color: #303133;
    font-size: 14px;
  }
}

.flex {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
