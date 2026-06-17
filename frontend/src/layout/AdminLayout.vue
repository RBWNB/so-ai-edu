<template>
  <el-container class="layout-shell">
    <div class="ambient-background">
      <div class="ambient-glow glow-blue"></div>
      <div class="ambient-glow glow-seafoam"></div>
      <div class="caustics-layer"></div>
      <div class="topography-bg"></div>
    </div>

    <el-aside width="248px" class="sidebar">
      <div class="brand">
        <div class="brand-icon">
          <el-icon><Ship /></el-icon>
        </div>
        <div class="brand-copy">
          <strong>海洋生物管理系统</strong>
          <span>守护蓝色家园</span>
        </div>
      </div>
      <div class="menu-wrapper">
        <el-menu :default-active="activeMenu" class="menu" router>
          <el-menu-item
              v-for="item in authStore.menus"
              :key="item.id"
              :index="item.path"
          >
            <el-icon v-if="item.icon"><component :is="iconMap[item.icon]" /></el-icon>
            <span>{{ item.name }}</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="page-heading">
            <h1>{{ currentTitle }}</h1>
            <p>科学管理海洋生物多样性数据</p>
          </div>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info-dropdown">
              <el-avatar :size="32" :src="userAvatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <span class="username-text">{{ authStore.username }}</span>
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </div>

            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { ElMessageBox, ElMessage } from "element-plus";
import { ArrowDown, Ship, SwitchButton, User } from "@element-plus/icons-vue";
import { iconMap } from "@/utils/iconMap";
import { getUserProfile } from "@/api/sysUser";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const activeMenu = computed(() => route.path);
const currentTitle = computed(() => route.meta?.title || "首页");

const userAvatar = computed(() => {
  if (authStore.avatarUrl) {
    if (authStore.avatarUrl.startsWith("http") || authStore.avatarUrl.startsWith("/api")) {
      return authStore.avatarUrl;
    }
    return `/api${authStore.avatarUrl}`;
  }
  return "";
});

const handleCommand = (command) => {
  if (command === "profile") {
    router.push("/profile");
  } else if (command === "logout") {
    handleLogout();
  }
};

const handleLogout = () => {
  ElMessageBox.confirm("确定要退出登录系统吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
      .then(async () => {
        await authStore.logoutAction();
        router.push("/home");
        ElMessage.success("已安全退出登录");
      })
      .catch(() => {
        // ignore cancel
      });
};
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
  background: transparent;
}

.sidebar {
  background: var(--theme-card-bg) !important;
  border-right: 1px solid var(--theme-border);
  color: var(--theme-text-primary);
  box-shadow: 2px 0 8px rgba(22, 93, 255, 0.05);
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: sticky;
  top: 0;
}

.menu-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.menu-wrapper::-webkit-scrollbar {
  width: 6px;
}
.menu-wrapper::-webkit-scrollbar-track {
  background: transparent;
}
.menu-wrapper::-webkit-scrollbar-thumb {
  background: rgba(0, 47, 167, 0.15);
  border-radius: 3px;
}
.menu-wrapper::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 47, 167, 0.28);
}

.brand {
  height: 76px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--theme-border-light);
  background: #ffffff;
  flex-shrink: 0;
}

.brand-icon {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #ffffff;
  font-size: 22px;
  background: var(--theme-primary);
  box-shadow: 0 8px 18px rgba(22, 93, 255, 0.22);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-copy strong {
  color: var(--theme-primary-dark);
  font-size: 17px;
  line-height: 1.3;
}

.brand-copy span {
  margin-top: 3px;
  color: var(--theme-text-muted);
  font-size: 12px;
}

.menu {
  border-right: none;
  background: transparent;
  height: 100%;
}

.header {
  height: 76px;
  background: #ffffff !important;
  border-bottom: 1px solid var(--theme-border-light) !important;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 6px rgba(22, 93, 255, 0.04) !important;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left {
  flex: 1;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-heading h1 {
  margin: 0;
  color: var(--theme-text-primary);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.1;
}

.page-heading p {
  margin: 4px 0 0;
  color: var(--theme-text-muted);
  font-size: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  padding-right: 20px;
}

.user-info-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
  padding: 8px 12px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.user-info-dropdown:hover {
  background-color: var(--theme-primary-soft);
}

.username-text {
  margin: 0 8px;
  font-weight: 500;
  color: var(--theme-text-primary);
}

.header-right :deep(.el-dropdown) {
  line-height: 1;
}

.main {
  background: transparent;
  padding: 24px;
  overflow-y: auto;
  height: calc(100vh - 76px);
}

.main::-webkit-scrollbar {
  width: 8px;
}

.main::-webkit-scrollbar-track {
  background: transparent;
}

.main::-webkit-scrollbar-thumb {
  background: rgba(0, 47, 167, 0.12);
  border-radius: 4px;
}

.main::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 47, 167, 0.22);
}

@media (max-width: 768px) {
  .sidebar {
    width: 180px !important;
  }

  .username-text {
    display: none;
  }

  .main {
    padding: 12px;
  }

  .page-heading p,
  .header-left :deep(.el-breadcrumb) {
    display: none;
  }
}
</style>
