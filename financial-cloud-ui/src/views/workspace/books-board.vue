<template>
  <div class="app-container books-board">
    <el-form
      :inline="true"
      class="toolbar"
      @submit.prevent
    >
      <el-form-item label="关注月">
        <el-date-picker
          v-model="focusMonth"
          type="month"
          value-format="YYYY-MM"
          placeholder="选择月"
          :clearable="false"
          @change="load"
        />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input
          v-model="keyword"
          clearable
          placeholder="账套/单位名称"
          style="width: 180px"
          @keyup.enter="load"
        />
      </el-form-item>
      <el-form-item>
        <el-checkbox
          v-model="onlyTodo"
          @change="load"
        >
          仅有待办
        </el-checkbox>
      </el-form-item>
      <el-form-item label="结账">
        <el-radio-group
          v-model="closeStatusFilter"
          size="small"
        >
          <el-radio-button value="ALL">全部</el-radio-button>
          <el-radio-button value="BEHIND">落后</el-radio-button>
          <el-radio-button value="OPEN">未结</el-radio-button>
          <el-radio-button value="CLOSED">已结</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          @click="load"
        >
          刷新
        </el-button>
      </el-form-item>
      <el-form-item v-if="canExport">
        <el-checkbox v-model="includeVoucherList">
          账本包含凭证清单
        </el-checkbox>
        <el-button
          type="success"
          :disabled="!selectedClosedIds.length"
          :loading="batchLoading"
          @click="batchExport"
        >
          批量导出账本包 ({{ selectedClosedIds.length }})
        </el-button>
      </el-form-item>
    </el-form>

    <div
      v-if="summary.total || loading"
      class="summary-strip"
    >
      <span>共 <b>{{ summary.total }}</b> 套</span>
      <span class="text-danger">落后 <b>{{ summary.behind }}</b></span>
      <span>未结 <b>{{ summary.open }}</b></span>
      <span>已结 <b>{{ summary.closed }}</b></span>
      <span>有待办 <b>{{ summary.withTodo }}</b></span>
    </div>

    <el-alert
      v-if="board.truncated"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
      :title="`授权账套 ${board.totalGranted} 套，列表已截断至 200 套`"
    />

    <el-table
      v-loading="loading"
      :data="displayedRows"
      border
      @selection-change="onSelectionChange"
    >
      <el-table-column
        v-if="canExport"
        type="selection"
        width="48"
        :selectable="rowSelectable"
      />
      <el-table-column
        prop="bookName"
        label="账套"
        min-width="140"
        show-overflow-tooltip
      />
      <el-table-column
        prop="companyName"
        label="单位"
        min-width="140"
        show-overflow-tooltip
      />
      <el-table-column
        prop="currentTerm"
        label="当前账期"
        width="100"
      />
      <el-table-column
        label="待审"
        width="80"
        align="right"
      >
        <template #default="{ row }">
          {{ row.voucherReviewed ? row.pendingAuditCount : '—' }}
        </template>
      </el-table-column>
      <el-table-column
        prop="pendingPostCount"
        label="待过账"
        width="80"
        align="right"
      />
      <el-table-column
        label="折旧"
        width="90"
      >
        <template #default="{ row }">
          {{ row.depreciationPending ? '待计提' : '—' }}
        </template>
      </el-table-column>
      <el-table-column
        label="结账"
        width="90"
      >
        <template #default="{ row }">
          <span :class="{ 'text-danger': row.closeStatus === 'BEHIND' }">
            {{ closeLabel(row.closeStatus) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
        label="状态"
        width="80"
      >
        <template #default="{ row }">
          {{ bookStatusLabel(row) }}
        </template>
      </el-table-column>
      <el-table-column
        label="阻塞"
        width="100"
      >
        <template #default="{ row }">
          {{ blockerLabel(row.blocker) }}
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="200"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :disabled="row.sealed"
            @click="enterBook(row)"
          >
            进入处理
          </el-button>
          <el-button
            v-if="canExport"
            link
            type="success"
            :disabled="row.closeStatus !== 'CLOSED'"
            :loading="exportingId === row.bookId"
            @click="exportOne(row)"
          >
            导出账本包
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty
      v-if="!loading && !displayedRows.length"
      description="暂无授权账套或无匹配结果"
    >
      <el-button
        type="primary"
        @click="goOnboarding"
      >
        去建账
      </el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {ElMessage} from "element-plus";
import {
  exportBooksPackBatch,
  exportMonthlyBooksPackForBook,
  fetchBooksBoard,
} from "@/api/workspace/booksBoard";
import {switchBook} from "@/api/idm/user";
import {downloadData} from "@/utils";
import useUserStore from "@/store/modules/user";
import {
  booksBoardBlockerPath,
  filterBooksBoardRows,
  summarizeBooksBoardRows,
  type CloseStatusFilter,
} from "@/utils/booksBoard";

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const batchLoading = ref(false);
const exportingId = ref("");
const onlyTodo = ref(false);
const keyword = ref("");
const includeVoucherList = ref(true);
const closeStatusFilter = ref<CloseStatusFilter>("ALL");
const focusMonth = ref(defaultFocusMonth());
const selected = ref<any[]>([]);
const board = reactive<any>({rows: [], totalGranted: 0, truncated: false, focusPeriod: ""});

const canExport = computed(() => {
  const roles: string[] = userStore.roles || [];
  return roles.some((r) =>
    ["ROLE_ADMINISTRATORS", "ROLE_BOOKKEEPER", "ROLE_REVIEWER"].includes(r)
  );
});

const displayedRows = computed(() =>
  filterBooksBoardRows(board.rows || [], closeStatusFilter.value)
);

const summary = computed(() => summarizeBooksBoardRows(board.rows || []));

const selectedClosedIds = computed(() =>
  selected.value.filter((r) => r.closeStatus === "CLOSED").map((r) => r.bookId)
);

function defaultFocusMonth(): string {
  const now = new Date();
  const d = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const m = String(d.getMonth() + 1).padStart(2, "0");
  return `${d.getFullYear()}-${m}`;
}

function closeLabel(status: string): string {
  switch (status) {
    case "CLOSED":
      return "已结";
    case "OPEN":
      return "未结";
    case "BEHIND":
      return "落后";
    default:
      return "—";
  }
}

function bookStatusLabel(row: any): string {
  if (row.sealed || row.bookStatus === 2) return "封存";
  if (row.bookStatus === 0) return "禁用";
  return "启用";
}

function blockerLabel(blocker: string): string {
  const map: Record<string, string> = {
    AUDIT: "待审",
    POST: "待过账",
    DEPRECIATION: "待折旧",
    READY_CLOSE: "可结账",
    READY_PACK: "可交账",
    BEHIND: "落后",
    NONE: "—",
  };
  return map[blocker] || "—";
}

function blockerPath(row: any): string {
  return booksBoardBlockerPath(row.blocker);
}

function rowSelectable(row: any): boolean {
  return row.closeStatus === "CLOSED";
}

function onSelectionChange(rows: any[]): void {
  selected.value = rows || [];
}

function load(): void {
  loading.value = true;
  fetchBooksBoard({
    focusPeriod: focusMonth.value,
    onlyTodo: onlyTodo.value,
    keyword: keyword.value || undefined,
  })
    .then((res: any) => {
      if (res.code === 0) {
        Object.assign(board, res.data || {});
        if (res.data?.focusPeriod) {
          focusMonth.value = res.data.focusPeriod;
        }
      }
    })
    .finally(() => {
      loading.value = false;
    });
}

async function enterBook(row: any): Promise<void> {
  if (row.sealed) {
    ElMessage.warning("封存账套不可进入处理");
    return;
  }
  const path = blockerPath(row);
  try {
    await switchBook(row.bookId);
    window.location.assign(path);
  } catch (e: any) {
    ElMessage.error(e?.message || "切换账套失败");
  }
}

async function exportOne(row: any): Promise<void> {
  exportingId.value = row.bookId;
  try {
    const blob = await exportMonthlyBooksPackForBook({
      yearPeriod: focusMonth.value,
      includeVoucherList: includeVoucherList.value,
      bookId: row.bookId,
    });
    downloadData(blob, `${row.bookName || "账套"}_本月账本包_${focusMonth.value}.zip`);
    ElMessage.success("账本包已生成");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "导出失败");
  } finally {
    exportingId.value = "";
  }
}

async function batchExport(): Promise<void> {
  if (!selectedClosedIds.value.length) {
    ElMessage.warning("请勾选关注月已结的账套");
    return;
  }
  batchLoading.value = true;
  try {
    const blob = await exportBooksPackBatch({
      bookIds: selectedClosedIds.value,
      yearPeriod: focusMonth.value,
      includeVoucherList: includeVoucherList.value,
    });
    downloadData(blob, `批量账本包_${focusMonth.value}.zip`);
    ElMessage.success("批量账本包已生成");
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || error?.message || "批量导出失败");
  } finally {
    batchLoading.value = false;
  }
}

onMounted(load);

function goOnboarding(): void {
  router.push("/onboarding");
}
</script>

<style scoped>
.toolbar {
  margin-bottom: 8px;
}
.summary-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  color: #606266;
  font-size: 13px;
}
.mb8 {
  margin-bottom: 8px;
}
.text-danger {
  color: var(--el-color-danger);
}
</style>
