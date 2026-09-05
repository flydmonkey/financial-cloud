<template>
  <el-drawer
    v-model="visible"
    size="560px"
    :close-on-click-modal="false"
    @close="emit('close')"
  >
    <template #header>
      <h4>成员授权 — {{ bookName || bookId }}</h4>
    </template>
    <div class="grant-box">
      <el-form
        :inline="true"
        @submit.native.prevent
      >
        <el-form-item label="用户">
          <el-select
            v-model="form.userId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="搜索用户名/显示名"
            :remote-method="onSearch"
            :loading="searchLoading"
            style="width: 200px"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.userId"
              :label="`${item.displayName || ''} (${item.username})`"
              :value="item.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="form.roleId"
            placeholder="产品角色"
            style="width: 140px"
          >
            <el-option
              v-for="item in roleOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            @click="onGrant"
          >
            授权
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table
      v-loading="loading"
      :data="members"
      border
    >
      <el-table-column
        prop="username"
        label="用户名"
        min-width="100"
      />
      <el-table-column
        prop="displayName"
        label="显示名"
        min-width="100"
      />
      <el-table-column
        prop="roleName"
        label="产品角色"
        min-width="90"
      />
      <el-table-column
        label="操作"
        width="90"
        align="center"
      >
        <template #default="scope">
          <el-button
            type="danger"
            link
            @click="onRevoke(scope.row)"
          >
            撤销
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-drawer>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import modal from "@/plugins/modal";
import {
  grantBookMember,
  listBookMembers,
  revokeBookMember,
  searchBookUsers,
} from "@/api/book/members";

const props = defineProps({
  open: { type: Boolean, default: false },
  bookId: { type: String, default: "" },
  bookName: { type: String, default: "" },
});
const emit = defineEmits(["close"]);

const PRODUCT_ROLES = [
  { id: "ROLE_ADMINISTRATORS", name: "管理员" },
  { id: "ROLE_BOOKKEEPER", name: "做账员" },
  { id: "ROLE_REVIEWER", name: "审核员" },
  { id: "ROLE_VIEWER", name: "查看员" },
];

const visible = ref(false);
const loading = ref(false);
const searchLoading = ref(false);
const members = ref<any[]>([]);
const userOptions = ref<any[]>([]);
const roleOptions = ref(PRODUCT_ROLES);
const form = reactive({
  userId: "",
  roleId: "",
});

watch(
  () => [props.open, props.bookId],
  ([open]) => {
    visible.value = !!open;
    if (open && props.bookId) {
      form.userId = "";
      form.roleId = "";
      userOptions.value = [];
      loadMembers();
    }
  },
  { immediate: true }
);

function loadMembers() {
  if (!props.bookId) {
    return;
  }
  loading.value = true;
  listBookMembers(props.bookId)
    .then((res: any) => {
      if (res.code === 0) {
        members.value = res.data || [];
      } else {
        modal.msgError(res.message || "加载成员失败");
      }
    })
    .finally(() => {
      loading.value = false;
    });
}

function onSearch(q: string) {
  if (!q || !props.bookId) {
    userOptions.value = [];
    return;
  }
  searchLoading.value = true;
  searchBookUsers(props.bookId, q)
    .then((res: any) => {
      if (res.code === 0) {
        userOptions.value = res.data || [];
      }
    })
    .finally(() => {
      searchLoading.value = false;
    });
}

function onGrant() {
  if (!props.bookId) {
    modal.msgError("未指定账套");
    return;
  }
  if (!form.userId) {
    modal.msgError("请选择用户");
    return;
  }
  if (!form.roleId) {
    modal.msgError("请选择产品角色");
    return;
  }
  grantBookMember({
    bookId: props.bookId,
    userId: form.userId,
    roleId: form.roleId,
  }).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess("授权成功");
      form.userId = "";
      form.roleId = "";
      loadMembers();
    } else {
      modal.msgError(res.message || "授权失败");
    }
  });
}

function onRevoke(row: any) {
  modal.confirm(`确认撤销 ${row.username} 在本账套的权限？`).then(() => {
    return revokeBookMember(props.bookId, row.userId);
  }).then((res: any) => {
    if (res.code === 0) {
      modal.msgSuccess("已撤销");
      loadMembers();
    } else {
      modal.msgError(res.message || "撤销失败");
    }
  }).catch(() => {});
}
</script>

<style scoped>
.grant-box {
  margin-bottom: 12px;
}
</style>
