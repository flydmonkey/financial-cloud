<template>
  <el-dialog
    :model-value="open"
    title="凭证附件"
    width="560px"
    :close-on-click-modal="false"
    @close="emit('close')"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="支持 PDF / PNG / JPG / WEBP / OFD，单个不超过 10MB；已结账期间不可变更附件。"
      style="margin-bottom: 12px"
    />
    <el-upload
      :show-file-list="false"
      :http-request="doUpload"
      accept=".pdf,.png,.jpg,.jpeg,.webp,.ofd"
      drag
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">拖拽文件到这里，或 <em>点击上传</em></div>
    </el-upload>

    <el-table
      v-loading="loading"
      :data="rows"
      size="small"
      style="margin-top: 12px"
    >
      <el-table-column
        prop="fileName"
        label="文件名"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column
        label="大小"
        width="90"
        align="right"
      >
        <template #default="scope">{{ formatSize(scope.row.contentSize) }}</template>
      </el-table-column>
      <el-table-column
        prop="createdBy"
        label="上传人"
        width="90"
        show-overflow-tooltip
      />
      <el-table-column
        label="操作"
        width="110"
        align="center"
      >
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="download(scope.row)"
          >下载</el-button>
          <el-button
            link
            type="danger"
            @click="remove(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import {ref, watch} from "vue";
import {UploadFilled} from "@element-plus/icons-vue";
import modal from "@/plugins/modal";
import {
  listAttachments,
  uploadAttachment,
  deleteAttachment,
  attachmentDownloadUrl,
} from "@/api/voucher/attachment";

const props = defineProps<{ open: boolean; voucherId: string | null }>();
const emit = defineEmits(["close"]);

const loading = ref(false);
const rows = ref<any[]>([]);

function formatSize(bytes: any): string {
  const n = Number(bytes || 0);
  if (n >= 1024 * 1024) return (n / 1024 / 1024).toFixed(1) + " MB";
  if (n >= 1024) return (n / 1024).toFixed(1) + " KB";
  return n + " B";
}

function load(): void {
  if (!props.voucherId) return;
  loading.value = true;
  listAttachments(props.voucherId).then((res: any) => {
    if (res.code === 0) rows.value = res.data || [];
  }).finally(() => {
    loading.value = false;
  });
}

async function doUpload(options: any): Promise<void> {
  if (!props.voucherId) return;
  try {
    const res: any = await uploadAttachment(props.voucherId, options.file);
    if (res.code === 0) {
      modal.msgSuccess("附件已上传");
      load();
    } else {
      modal.msgError(res.message || "上传失败");
    }
  } catch (error: any) {
    modal.msgError(error?.response?.data?.message || error?.message || "上传失败");
  }
}

function download(row: any): void {
  window.open(attachmentDownloadUrl(row.id), "_blank");
}

function remove(row: any): void {
  modal.confirm(`确认删除附件「${row.fileName}」？`).then(async () => {
    const res: any = await deleteAttachment(row.id);
    if (res.code === 0) {
      modal.msgSuccess("已删除");
      load();
    } else {
      modal.msgError(res.message || "删除失败");
    }
  }).catch(() => {});
}

watch(() => [props.open, props.voucherId], ([open]) => {
  if (open) load();
});
</script>
