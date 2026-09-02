<template>
  <div class="navbar">
    <div class="left-main">
      <Logo
        class="logo"
        :collapse="false"
      />
    </div>

    <div class="right-menu">
      <div class="right-menu-item">
        <span>当前账期：{{ termCurrent }}</span>
        <el-divider direction="vertical" />
        <span>账套：</span>
        <el-select
          v-model="currentSet"
          style="width: 250px;"
          @change="handleSwitchBook"
        >
          <el-option
            v-for="dict in currBookStore.setList"
            :key="dict.id"
            :label="dict.name"
            :value="dict.id"
          />
        </el-select>
      </div>
      <el-divider direction="vertical" />
      <div class="right-menu-item">
        <ScreenFull
          id="screenfull"
          class="right-menu-item hover-effect"
        />
      </div>
      <el-divider direction="vertical" />
      <div class="right-menu-item avatar-box">
        <el-dropdown placement="bottom">
          <div class="avatar-wrapper">
            <img
              :src="userStore.avatar"
              class="user-avatar"
              alt=""
            >
            <span style="margin-left: 5px">{{ userStore.name }}</span>
            <span>({{ userStore.username }})</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>
                <router-link to="/user/profile">
                  <svg-icon icon-class="user" />
                  <span style="margin-left: 5px">个人中心</span>
                </router-link>
              </el-dropdown-item>
              <el-dropdown-item>
                <CleanSession class="right-menu-item hover-effect" />
              </el-dropdown-item>
              <el-dropdown-item style="border-top: 1px solid #888888;">
                <div @click="logout">
                  <svg-icon icon-class="logout" />
                  <span style="margin-left: 5px">退出登录</span>
                </div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from "vue"
import {ElMessageBox} from 'element-plus'
import ScreenFull from '@/components/Screenfull/index.vue'
import CleanSession from '@/components/CleanSession/index.vue'
import * as userService from "@/api/idm/user";
import useUserStore from '@/store/modules/user'
import bookStore from '@/store/modules/bookStore'
import {logoutApi} from "@/api/login";
import Logo from "./Sidebar/Logo.vue";
import SvgIcon from "@/components/SvgIcon/index.vue";

const userStore = useUserStore()
const currBookStore = bookStore()

const currentSet = computed({
  get: () => currBookStore.bookId,
  set: (id) => currBookStore.updateBookId(id),
});

const termCurrent = computed(() => {
  const yyyyMM = (currBookStore.termCurrent + "").split("-");
  return yyyyMM[0] + '年' + yyyyMM[1] + '月'
})

currentSet.value = userStore.bookId;

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    logoutApi().then((res: any) => {
      if (res.code === 0) {
        userStore.logOut().then(() => {
          window.location.reload()
        })
      }
    });
  }).catch(() => {
  });
}

function handleSwitchBook(val: any) {
  currentSet.value = val;
  userStore.bookId = val;
  userService.switchBook(val).then(() => {
    window.location.reload()
  })
}

</script>

<style lang='scss' scoped>
@import "@/assets/styles/variables.module";

.navbar {
  position: fixed;
  z-index: 1001;
  width: 100%;
  height: $base-navbar-height;
  overflow: hidden;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .left-main {
    position: relative;
    height: $base-navbar-height;

    .logo {
      float: left;
      text-align: left;
      margin-right: 30px;
      width: auto;

      :deep(.sidebar-logo-container) {
        width: auto;
        text-align: left;
        background: transparent !important;
      }

      :deep(.sidebar-logo-link) {
        justify-content: flex-start;
        padding: 0;
      }

      :deep(.sidebar-title) {
        color: #111827;
      }
    }
  }

  .right-menu {
    margin-right: 30px;
    display: inline-flex;
    justify-content: center;
    align-items: center;
    font-size: 14px;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      padding: 0 8px;
      color: #000000;
      cursor: pointer;
      outline: none;
      transition: background-color .3s;

      &.hover-effect {
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: rgba(0, 0, 0, 0.025);
        }
      }

      .svg-icon {
        font-size: 16px;
      }
    }

    .avatar-box {
      height: $base-navbar-height;
      line-height: normal;
    }

    .avatar-wrapper {
      height: $base-navbar-height;
      display: flex;
      justify-content: flex-start;
      align-items: center;
      cursor: pointer;

      .user-avatar {
        width: 24px;
        height: 24px;
        border-radius: 50%;
      }
    }
  }
}
</style>
