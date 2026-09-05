<template>
  <div class="no-access">
    <h1>暂无业务权限</h1>
    <p>当前账套未分配产品角色，或账号没有可用菜单。请联系该账套管理员授权后重新登录。</p>
    <div class="actions">
      <el-button type="primary" @click="goLogin">重新登录</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import useUserStore from "@/store/modules/user";
import router from "@/router";

async function goLogin() {
  try {
    await useUserStore().logOut();
  } catch {
    /* ignore */
  }
  router.replace("/login");
}
</script>

<style scoped>
.no-access {
  max-width: 520px;
  margin: 12vh auto;
  padding: 24px;
  text-align: center;
}
.no-access h1 {
  font-size: 22px;
  margin-bottom: 12px;
}
.no-access p {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 24px;
}
</style>
