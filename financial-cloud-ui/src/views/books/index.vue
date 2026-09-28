<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <div class="queryForm">
        <el-form
          ref="queryRef"
          :model="queryParams"
          :inline="true"
          @submit.native.prevent
        >
          <el-form-item label="账套名称">
            <el-input
              v-model="queryParams.name"
              clearable
              style="width: 200px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button @click="handleQuery">
              {{ t('org.button.query') }}
            </el-button>
            <el-button @click="resetQuery">
              {{ t('org.button.reset') }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    <el-card class="common-card">
      <div class="btn-form">
        <el-button
          type="primary"
          @click="handleAdd"
        >
          {{ t('org.button.add') }}
        </el-button>
        <el-button
          v-if="hasAdminBooks"
          type="danger"
          :disabled="ids.length === 0"
          @click="onBatchDelete"
        >
          {{ t('org.button.deleteBatch') }}
        </el-button>
        <el-button
          type="warning"
          plain
          icon="Upload"
          @click="restoreOpen = true"
        >
          恢复备份
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="setsList"
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          v-if="hasAdminBooks"
          type="selection"
          width="55"
          align="center"
          :selectable="isBookAdmin"
        />
        <el-table-column
          prop="id"
          label="编码"
          align="center"
          min-width="80"
          :show-overflow-tooltip="true"
        />
        <el-table-column
          prop="name"
          label="名称"
          align="left"
          min-width="100"
          :show-overflow-tooltip="true"
        />
        <el-table-column
          prop="companyName"
          label="单位名称"
          align="left"
          min-width="100"
          :show-overflow-tooltip="true"
        />
        <el-table-column
          prop="vatType"
          label="纳税性质"
          align="center"
          min-width="70"
        >
          <template #default="scope">
            <dict-tag-number
              :options="books_vat_type"
              :value="scope.row.vatType"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="standardsName"
          label="会计准则"
          align="left"
          min-width="80"
          :show-overflow-tooltip="true"
        />
        <el-table-column
          prop="enableDate"
          label="建账期间"
          align="center"
          min-width="40"
          :show-overflow-tooltip="true"
        />
        <el-table-column
          prop="voucherReviewed"
          label="凭证审核"
          align="center"
          min-width="40"
        >
          <template #default="scope">
            <span v-if="scope.row.voucherReviewed === 0">关闭</span>
            <span v-if="scope.row.voucherReviewed === 1">开启</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          :label="t('org.status')"
          align="center"
          min-width="40"
        >
          <template #default="scope">
            <span v-if="scope.row.status === 1"><el-icon color="green"><SuccessFilled
              class="success"
            /></el-icon></span>
            <span v-if="scope.row.status === 0"><el-icon color="#808080"><CircleCloseFilled /></el-icon></span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('jbx.text.action')"
          align="center"
          width="160"
        >
          <template #default="scope">
            <template v-if="isBookAdmin(scope.row)">
              <el-tooltip content="编辑">
                <el-button
                  link
                  icon="Edit"
                  @click="handleUpdate(scope.row)"
                />
              </el-tooltip>
              <el-tooltip content="成员授权">
                <el-button
                  link
                  type="primary"
                  @click="openMembers(scope.row)"
                >
                  成员
                </el-button>
              </el-tooltip>
              <el-tooltip content="导出账套业务备份包（ZIP）">
                <el-button
                  link
                  type="warning"
                  :loading="backupLoadingId === scope.row.id"
                  @click="handleBackup(scope.row)"
                >
                  备份
                </el-button>
              </el-tooltip>
              <el-tooltip content="移除">
                <el-button
                  link
                  icon="Delete"
                  type="danger"
                  @click="handleDelete(scope.row)"
                />
              </el-tooltip>
            </template>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNumber"
        v-model:limit="queryParams.pageSize"
        :total="total"
        :page-sizes="queryParams.pageSizeOptions"
        @pagination="getList"
      />
    </el-card>
    <edit-form
      :title="title"
      :open="open"
      :form-id="id"
      :accounting_standards="standardList"
      :vat_types="books_vat_type"
      :books_industry="books_industry"
      @dialog-of-closed-methods="dialogOfClosedMethods"
    />
    <members-drawer
      :open="membersOpen"
      :book-id="membersBookId"
      :book-name="membersBookName"
      @close="closeMembers"
    />
    <el-dialog
      v-model="restoreOpen"
      title="恢复账套备份"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="上传账套备份包（ZIP），将恢复为一个新账套，不会影响现有账套数据。"
      />
      <div style="margin-top: 16px">
        <el-upload
          ref="restoreUploadRef"
          :auto-upload="false"
          :limit="1"
          accept=".zip"
          :on-change="onRestoreFileChange"
          :on-remove="onRestoreFileRemove"
          drag
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">拖拽备份 ZIP 到这里，或 <em>点击选择文件</em></div>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="restoreOpen = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!restoreFile"
          :loading="restoreLoading"
          @click="handleRestore"
        >
          开始恢复
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {useI18n} from "vue-i18n";
import {computed, getCurrentInstance, reactive, ref, toRefs} from "vue";
import editForm from "./edit.vue";
import membersDrawer from "./members.vue";
import modal from "@/plugins/modal";
import DictTagNumber from "@/components/DIctTagNumber/index.vue";
import {listBooksSets, deleteBatch, exportBookBackup, restoreBookBackup} from "@/api/book/book";
import {listStandardsAll} from "@/api/standard/standard";
import SvgIcon from "@/components/SvgIcon/index.vue";
import booksSetStore from "@/store/modules/bookStore";
import {downloadData} from "@/utils";
import {UploadFilled} from "@element-plus/icons-vue";

const {t} = useI18n()

const proxy: any = getCurrentInstance()!.proxy;

const {books_vat_type, books_industry}
    = proxy?.useDict("books_vat_type", "books_industry");

const membersOpen: any = ref(false);
const membersBookId: any = ref("");
const membersBookName: any = ref("");

function isBookAdmin(row: any): boolean {
  return row?.roleId === "ROLE_ADMINISTRATORS";
}

function openMembers(row: any): any {
  if (!isBookAdmin(row)) {
    modal.msgWarning("仅账套管理员可管理成员");
    return;
  }
  membersBookId.value = row.id;
  membersBookName.value = row.name || row.companyName || row.id;
  membersOpen.value = true;
}

function closeMembers(): any {
  membersOpen.value = false;
  membersBookId.value = "";
  membersBookName.value = "";
}

// ---------- 账套备份与恢复 ----------
const backupLoadingId: any = ref("");
const restoreOpen: any = ref(false);
const restoreLoading: any = ref(false);
const restoreFile: any = ref<File | null>(null);
const restoreUploadRef: any = ref(null);

/** 导出备份包 */
async function handleBackup(row: any): Promise<void> {
  if (!isBookAdmin(row)) {
    modal.msgWarning("仅账套管理员可导出备份");
    return;
  }
  backupLoadingId.value = row.id;
  try {
    const blob = await exportBookBackup(row.id);
    if (blob?.type?.includes("json")) {
      const text = JSON.parse(await blob.text());
      modal.msgError(text?.message || "备份导出失败");
      return;
    }
    const stamp = new Date().toISOString().slice(0, 10).replaceAll("-", "");
    downloadData(blob, `账套备份_${row.name || row.id}_${stamp}.zip`);
    modal.msgSuccess("备份包已导出");
  } catch (error: any) {
    modal.msgError(error?.response?.data?.message || error?.message || "备份导出失败");
  } finally {
    backupLoadingId.value = "";
  }
}

function onRestoreFileChange(file: any): any {
  restoreFile.value = file?.raw || null;
}

function onRestoreFileRemove(): any {
  restoreFile.value = null;
}

/** 上传备份包并恢复为新账套 */
async function handleRestore(): Promise<void> {
  if (!restoreFile.value) {
    return;
  }
  restoreLoading.value = true;
  try {
    const res: any = await restoreBookBackup(restoreFile.value);
    if (res.code === 0) {
      modal.msgSuccess(`已恢复为新账套「${res.data?.name || ""}」`);
      restoreOpen.value = false;
      restoreFile.value = null;
      restoreUploadRef.value?.clearFiles();
      getList();
      booksSetStore().refreshData();
    } else {
      modal.msgError(res.message || "备份恢复失败");
    }
  } catch (error: any) {
    modal.msgError(error?.response?.data?.message || error?.message || "备份恢复失败");
  } finally {
    restoreLoading.value = false;
  }
}
const data: any = reactive({
  queryParams: {
    pageNumber: 1,
    pageSize: 10,
    pageSizeOptions: [10, 20, 50]
  }
});

const {queryParams} = toRefs(data);

const setsList: any = ref<any>([]);
const hasAdminBooks = computed(() =>
  (setsList.value || []).some((row: any) => isBookAdmin(row))
);
const open: any = ref(false);
const subjectOpen: any = ref(false);
const loading: any = ref(true);
const title: any = ref("");
const id: any = ref(undefined);
const total: any = ref(0);
const ids: any = ref<any>([]);
const selectionlist: any = ref<any>([]);
//会计准则
const standardList: any = ref<any>([]);


/**
 * 获取列表
 */
function getList(): any {
  listBooksSets(queryParams.value).then((res: any) => {
    if (res.code === 0) {
      loading.value = false;
      setsList.value = res.data.records;
      total.value = res.data.total;
    }
  })
}

/** 多选操作*/
function handleSelectionChange(selection: any): any {
  selectionlist.value = selection;
  ids.value = selectionlist.value.map((item: any) => item.id);
}

/**
 * 查询
 */
function handleQuery(): any {
  queryParams.value.pageNumber = 1;
  getList();
}

/**
 * 重置
 */
function resetQuery(): any {
  queryParams.value.code = undefined;
  queryParams.value.name = undefined;
  queryParams.value.category = undefined;
  handleQuery();
}

function handleAdd(): any {
  id.value = undefined;
  title.value = t('jbx.text.add')
  open.value = true;
}

/*关闭抽屉*/
function dialogOfClosedMethods(val: any): any {
  open.value = false;
  subjectOpen.value = false;
  id.value = undefined;
  if (val) {
    getList();
    // 新建账套后刷新顶栏可选账套列表
    booksSetStore().refreshData();
  }
}


/** 多选删除操作*/
function onBatchDelete(): any {
  modal.confirm(t('jbx.confirm.text.delete')).then(function () {
    return deleteBatch({listIds: ids.value});
  }).then((res: any) => {
    if (res.code === 0) {
      handleQuery();
      modal.msgSuccess(t('jbx.alert.delete.success'));
    } else {
      modal.msgError(res.message);
    }
  }).catch(() => {
  });
}

interface TreeNode {
  id: string | number; // 根据你的数据实际情况调整类型
  children?: TreeNode[]; // 子节点可能不存在
}

/** 修改按钮操作 */
function handleUpdate(row: any): any {
  if (!isBookAdmin(row)) {
    modal.msgWarning("仅账套管理员可编辑账套");
    return;
  }
  id.value = row.id;
  title.value = t('org.titleEdit');
  open.value = true;
}

/** 删除按钮操作 */
function handleDelete(row: any): any {
  if (!isBookAdmin(row)) {
    modal.msgWarning("仅账套管理员可删除账套");
    return;
  }
  modal.confirm(t('org.deleteTip1') + row.name + t('org.deleteTip2')).then(function () {
    return deleteBatch({listIds: [row.id]});
  }).then((res: any) => {
    if (res.code === 0) {
      getList();
      modal.msgSuccess(t('jbx.alert.delete.success'));
    } else {
      modal.msgError(res.message);
    }
  }).catch(() => {
  });
}

function getStandards(): any {
  listStandardsAll({status: 1}).then((res: any) => {
    if (res.code === 0) {
      standardList.value = res.data;
    }
  });
}

/*查看关联的会计科目*/
function showSubjects(row: any): any {
  id.value = row.id;
  title.value = '已关联的会计科目'
  subjectOpen.value = true;
}

getStandards();
getList();
</script>

<style lang="scss" scoped>
.btn-form {
  margin-bottom: 10px;
}

.common-card {
  margin-bottom: 15px;
}

.app-container {
  padding: 0;
  background-color: #f5f7fa;
}

.text-muted {
  color: #c0c4cc;
}
</style>
