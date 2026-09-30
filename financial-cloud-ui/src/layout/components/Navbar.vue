<template>
  <div class="navbar">
    <div class="left-main">
      <Logo
        class="logo"
        :collapse="false"
      />
      <!--      <hamburger id="hamburger-container" :is-active="appStore.sidebar.opened" class="hamburger-container"-->
      <!--                 @toggleClick="toggleSideBar"/>-->
    </div>

    <div class="right-menu">
      <div class="right-menu-item book-context">
        <span class="book-term">当前账期：{{ termCurrent }}</span>
        <el-divider
          class="book-divider"
          direction="vertical"
        />
        <span class="book-label">账套：</span>
        <el-select
          v-model="currentSet"
          class="navbar-book-select"
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
      <!--
      <el-tooltip content="选择语言" placement="top" effect="dark">
        <Language class="right-menu-item hover-effect"></Language>
      </el-tooltip>
      -->
      <el-divider direction="vertical" />
      <div class="right-menu-item avatar-box">
        <el-dropdown placement="bottom">
          <div class="avatar-wrapper">
            <img
              :src="userStore.avatar"
              class="user-avatar"
              alt=""
            >
            <span class="user-display-name">{{ userStore.name }}</span>
            <span class="user-username">({{ userStore.username }})</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>
                <router-link to="/user/profile">
                  <svg-icon icon-class="user" />
                  <span style="margin-left: 5px">个人中心</span>
                </router-link>
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
import {computed, ref} from "vue"
import {ElMessageBox} from 'element-plus'
import useAppStore from '@/store/modules/app'
import * as userService from "@/api/idm/user";
import useUserStore from '@/store/modules/user'
import bookStore from '@/store/modules/bookStore'
import {logoutApi} from "@/api/login";
import Logo from "./Sidebar/Logo.vue";
import SvgIcon from "@/components/SvgIcon/index.vue";
import {parseTime} from "@/utils/financialCloud"

const appStore = useAppStore()
const userStore = useUserStore()
const currBookStore = bookStore()

const currentSet = computed({
  get: () => currBookStore.bookId,
  set: (id) => currBookStore.updateBookId(id),
});
const termCurrent = computed(() => {
  //return parseTime(currBookStore.termCurrent, "{y}年{m}期")
  let  yyyyMM = (currBookStore.termCurrent+"").split("-");
  return yyyyMM[0] + '年' + yyyyMM[1] + '月'
})

currentSet.value = userStore.bookId;

function toggleSideBar() {
  appStore.toggleSideBar()
}

function handleCommand(command: any) {
  switch (command) {
    case "setLayout":
      setLayout();
      break;
    case "logout":
      logout();
      break;
    default:
      break;
  }
}

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    logoutApi().then((res: any) => {
      if (res.code === 0) {
        userStore.logOut().then(() => {
          const base = import.meta.env.VITE_APP_CONTEXT_PATH || '/'
          window.location.href = (base.endsWith('/') ? base : `${base}/`) + 'login'
        })
      }
    });

  }).catch(() => {
  });
}

const emits = defineEmits(['setLayout'])

function setLayout() {
  emits('setLayout');
}

function handleSwitchBook(val: any) {
  currentSet.value = val;
  userStore.bookId = val;
  userService.switchBook(val).then((res: any) => {
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
  min-height: $base-navbar-height;
  overflow: visible;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .left-main {
    position: relative;
    height: $base-navbar-height;

    .hamburger-container {
      line-height: $base-navbar-height;
      height: 100%;
      float: left;
      cursor: pointer;
      transition: background 0.3s;
      -webkit-tap-highlight-color: transparent;

      &:hover {
        background: rgba(0, 0, 0, 0.025);
      }
    }

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

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
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
      display: inline-flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 6px;
      padding: 0 8px;
      color: #000000;
      cursor: pointer;
      outline: none;
      transition: background-color .3s;
      max-width: min(100%, 520px);

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

    .navbar-book-select {
      width: min(250px, 42vw);
      min-width: 0;
      max-width: 100%;
    }

    .avatar-box {
      height: $base-navbar-height;
      line-height: normal;
    }

    .avatar-wrapper {
      height: $base-navbar-height;
      display: inline-flex;
      justify-content: flex-start;
      align-items: center;
      flex-wrap: nowrap;
      gap: 4px;
      max-width: min(100%, 220px);
      min-width: 0;
      cursor: pointer;
      white-space: nowrap;
      writing-mode: horizontal-tb;

      .user-avatar {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        flex: 0 0 auto;
      }

      .user-display-name,
      .user-username {
        display: inline-block;
        flex: 0 1 auto;
        min-width: 0;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        writing-mode: horizontal-tb;
        word-break: keep-all;
        line-height: 1.2;
      }

      .user-display-name {
        margin-left: 5px;
      }
    }
  }

  @media (max-width: 768px) {
    height: auto;
    align-items: flex-start;
    padding: 4px 0;

    .left-main {
      flex: 0 0 auto;

      .logo {
        margin-right: 8px;

        :deep(.sidebar-logo) {
          width: 28px;
          height: 28px;
        }

        :deep(.sidebar-title) {
          font-size: 16px;
          letter-spacing: 0;
        }

        :deep(.sidebar-logo-link) {
          gap: 6px;
          padding: 0 6px;
        }
      }
    }

    .right-menu {
      margin-right: 8px;
      align-items: flex-start;
      padding: 6px 0;
      flex: 1 1 auto;
      min-width: 0;
      max-width: none;
      gap: 2px;

      .right-menu-item.book-context {
        display: grid;
        grid-template-columns: auto minmax(7.5em, 1fr);
        grid-template-areas:
          "term term"
          "label select";
        column-gap: 4px;
        row-gap: 4px;
        align-items: center;
        white-space: normal;
        flex: 1 1 auto;
        min-width: 0;
        max-width: none;
        padding: 0 4px;
      }

      .book-term {
        grid-area: term;
        white-space: nowrap;
        font-size: 13px;
      }

      .book-divider {
        display: none;
      }

      .book-label {
        grid-area: label;
        white-space: nowrap;
      }

      .navbar-book-select {
        grid-area: select;
        width: 100%;
        min-width: 7.5em;
        max-width: 100%;
      }

      .avatar-box {
        flex: 0 0 auto;
        max-width: 7.5em;
        min-width: 0;
        padding: 0 2px;
      }

      .avatar-wrapper {
        max-width: 7.5em;

        .user-display-name {
          max-width: 5.5em;
        }

        /* 窄屏隐藏 (username)，腾出空间给账套名；完整信息仍在下拉菜单 */
        .user-username {
          display: none;
        }
      }
    }
  }
}
</style>
