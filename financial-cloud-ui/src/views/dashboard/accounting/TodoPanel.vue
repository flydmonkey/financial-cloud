<template>
  <el-card
    v-if="visible"
    shadow="hover"
    class="todo-card"
  >
    <template #header>
      <div class="card-title">
        <span>待办事项</span>
        <span class="term-tag">当前账期 {{ todo.currentTerm || '—' }}</span>
      </div>
    </template>
    <div
      v-loading="loading"
      class="todo-list"
    >
      <div
        v-if="todo.voucherReviewed"
        class="todo-item"
        :class="{muted: !todo.pendingAuditCount}"
        @click="go('/voucher/voucher-index')"
      >
        <span class="label">待审核凭证</span>
        <span class="value">{{ todo.pendingAuditCount || 0 }} 张</span>
      </div>
      <div
        class="todo-item"
        :class="{muted: !todo.pendingPostCount}"
        @click="go('/voucher/voucher-index')"
      >
        <span class="label">待过账凭证</span>
        <span class="value">{{ todo.pendingPostCount || 0 }} 张</span>
      </div>
      <div
        class="todo-item"
        :class="{muted: !todo.depreciationPending}"
        @click="go('/fixed-asset/depreciation')"
      >
        <span class="label">本期折旧计提</span>
        <span class="value">{{ todo.depreciationPending ? '待计提' : '已完成' }}</span>
      </div>
      <div
        class="todo-item"
        :class="{muted: !(todo.overdueReceivable > 0)}"
        @click="go('/arap/aging')"
      >
        <span class="label">逾期应收</span>
        <span class="value">¥{{ fmt(todo.overdueReceivable) }}</span>
      </div>
      <div
        class="todo-item"
        :class="{muted: !(todo.overduePayable > 0)}"
        @click="go('/arap/aging')"
      >
        <span class="label">逾期应付</span>
        <span class="value">¥{{ fmt(todo.overduePayable) }}</span>
      </div>
      <div
        class="todo-item"
        @click="go('/settlement/settle-list')"
      >
        <span class="label">月末结账</span>
        <span class="value">去结账 ›</span>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import request from "@/utils/Request";
import booksSetStore from "@/store/modules/bookStore";

const router = useRouter();
const loading = ref(true);
const todo: any = ref({});

const visible = computed(() => !!booksSetStore().bookId);

function fmt(value: any): string {
  const n = Number(value || 0);
  return n.toLocaleString("zh-CN", {minimumFractionDigits: 2, maximumFractionDigits: 2});
}

function go(path: string): void {
  router.push(path);
}

function load(): void {
  loading.value = true;
  request({url: "/dashboard/todo", method: "get"}).then((res: any) => {
    if (res.code === 0) {
      todo.value = res.data || {};
    }
  }).finally(() => {
    loading.value = false;
  });
}

onMounted(load);
</script>

<style scoped>
.todo-card {
  height: 100%;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.term-tag {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.todo-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
}

.todo-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.todo-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.todo-item .label {
  font-size: 13px;
  color: #606266;
}

.todo-item .value {
  font-size: 18px;
  font-weight: 600;
  color: #e6a23c;
}

.todo-item.muted .value {
  color: #67c23a;
}
</style>
