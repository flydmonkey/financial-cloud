<template>
  <div class="sidebar-panel">
    <el-scrollbar
      class="sidebar-scroll"
      :class="sideTheme"
      wrap-class="scrollbar-wrapper"
    >
      <el-menu
        class="app-el-menu"
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="sideTheme === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground"
        :text-color="sideTheme === 'theme-dark' ? variables.menuColor : variables.menuLightColor"
        :unique-opened="true"
        :active-text-color="theme"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item
          v-for="route in sidebarRouters"
          :key="route.name || route.path"
          :item="route"
          :base-path="isParentView(route) ? '' : route.path"
        />
      </el-menu>
    </el-scrollbar>

    <div
      class="sidebar-collapse"
      :class="{ 'is-collapsed': isCollapse }"
      @click="toggleSideBar"
    >
      <el-tooltip
        :content="appStore.sidebar.opened ? '收缩' : '展开'"
        placement="right"
      >
        <hamburger
          class="sidebar-collapse__icon"
          :is-active="appStore.sidebar.opened"
        />
      </el-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from "vue"
import {useRoute} from "vue-router";

import SidebarItem from './SidebarItem.vue'
import variables from '@/assets/styles/variables.module.scss'
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import Hamburger from "@/components/Hamburger/index.vue";

const route = useRoute();
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()

const sidebarRouters = computed(() => permissionStore.sidebarRouters);
const sideTheme = computed(() => settingsStore.sideTheme);
const theme = computed(() => settingsStore.theme);
const isCollapse = computed(() => !appStore.sidebar.opened);

const activeMenu = computed(() => {
  const {path} = route;
  return path;
})

function toggleSideBar() {
  appStore.toggleSideBar()
}

const isParentView = (route: any) => {
  if (!route.raw) {
    return false
  }

  return !route.raw.requestUrl
}

</script>
<style scoped lang="scss">
.sidebar-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sidebar-scroll {
  flex: 1;
  min-height: 0;
}

.sidebar-collapse {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  cursor: pointer;
  border-top: 1px solid rgba(153, 153, 153, 0.35);
  transition: background 0.2s;

  &:hover {
    background: rgba(0, 0, 0, 0.04);
  }

  &.is-collapsed {
    justify-content: center;
  }

  :deep(.sidebar-collapse__icon) {
    padding: 0;
  }
}
</style>
