<template>
  <el-container class="edu-layout">
    <!-- 顶部导航 —— 始终可点击，鉴权由路由守卫处理 -->
    <el-header class="edu-header" height="64px">
      <div class="header-inner">
        <!-- Logo -->
        <div class="logo" @click="$router.push('/home')">
          <el-icon :size="28"><Ship /></el-icon>
          <span class="logo-text">海洋学堂</span>
        </div>

        <!-- 导航链接 -->
        <nav class="nav-links">
          <router-link to="/home"          class="nav-item" active-class="nav-active">首页</router-link>
          <router-link to="/encyclopedia"  class="nav-item" active-class="nav-active">海洋百科</router-link>
          <router-link to="/ai-assistant"  class="nav-item" active-class="nav-active">AI 导师</router-link>
          <router-link to="/map-explore"   class="nav-item" active-class="nav-active">探索地图</router-link>
          <router-link to="/quiz"          class="nav-item" active-class="nav-active">答题闯关</router-link>

          <!-- 管理员可见：进入B端 -->
          <a
            v-if="isAdmin"
            class="nav-item admin-entry"
            @click="$router.push('/admin/dashboard')"
          >
            <el-icon><Setting /></el-icon>
            后台管理
          </a>
        </nav>

        <!-- 右侧用户区 -->
        <div class="user-area">
          <!-- 已登录：头像 + 下拉菜单 -->
          <template v-if="authStore.isLoggedIn">
            <el-dropdown @command="handleUserCommand">
              <div class="user-trigger">
                <el-avatar :size="32" :src="userAvatar">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="username">{{ authStore.username }}</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item v-if="isAdmin" command="admin" divided>
                    <el-icon><Setting /></el-icon>进入后台
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" :divided="!isAdmin">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <!-- 未登录：登录按钮 -->
          <el-button
            v-else
            type="primary"
            :icon="User"
            @click="goLogin"
          >
            登录
          </el-button>
        </div>
      </div>
    </el-header>

    <!-- 主内容区 -->
    <el-main class="edu-main">
      <router-view />
    </el-main>

    <!-- 底部 -->
    <el-footer class="edu-footer" height="48px">
      <span>© 2025 海洋学堂 · 探索蔚蓝世界</span>
    </el-footer>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { ElMessageBox, ElMessage } from "element-plus";
import { ArrowDown, Setting, Ship, SwitchButton, User } from "@element-plus/icons-vue";

const route = useRoute();
const $router = useRouter();
const authStore = useAuthStore();

/* 当前用户是否为管理员 */
const isAdmin = computed(() => {
  const roles = authStore.roles ?? [];
  return roles.some((r) => ["ADMIN", "MANAGER"].includes(r));
});

/* 头像 */
const userAvatar = computed(() => {
  const url = authStore.avatarUrl;
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("/api")) return url;
  return `/api${url}`;
});

/* 跳转登录 —— 带 redirect 使得登录后回到当前页 */
const goLogin = () => {
  $router.push({ path: "/login", query: { redirect: route.fullPath } });
};

/* 用户下拉菜单 */
const handleUserCommand = (command) => {
  if (command === "profile") {
    $router.push("/profile");
  } else if (command === "admin") {
    $router.push("/admin/dashboard");
  } else if (command === "logout") {
    ElMessageBox.confirm("确定要退出登录吗？", "提示", {
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    })
      .then(async () => {
        await authStore.logoutAction();
        $router.push("/login");
        ElMessage.success("已安全退出登录");
      })
      .catch(() => {});
  }
};
</script>

<style scoped>
.edu-layout {
  min-height: 100vh;
  background: linear-gradient(180deg, #f5f9fc 0%, #eaf2f9 100%);
}

/* ── Header ── */
.edu-header {
  background: rgba(255, 255, 255, 0.94) !important;
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 200;
  padding: 0 24px;
}
.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 64px;
  gap: 28px;
}

/* ── Logo ── */
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--theme-primary, #0052d9);
  flex-shrink: 0;
}
.logo-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #00d2ff, #3a7bd5);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* ── Nav ── */
.nav-links {
  display: flex;
  gap: 4px;
  flex: 1;
  justify-content: center;
  align-items: center;
}
.nav-item {
  padding: 8px 16px;
  border-radius: 8px;
  text-decoration: none;
  color: #555;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
}
.nav-item:hover {
  background: rgba(0, 210, 255, 0.08);
  color: var(--theme-primary, #0052d9);
}
.nav-active {
  background: rgba(0, 210, 255, 0.12) !important;
  color: var(--theme-primary, #0052d9) !important;
}

/* 后台管理入口 */
.admin-entry {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--theme-primary, #0052d9) !important;
  background: rgba(0, 47, 167, 0.05);
  border: 1px dashed rgba(0, 47, 167, 0.25);
  font-weight: 600;
}
.admin-entry:hover {
  background: var(--theme-primary, #0052d9) !important;
  color: #fff !important;
  border-color: transparent;
}

/* ── User ── */
.user-area {
  flex-shrink: 0;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 8px;
  transition: background 0.2s;
}
.user-trigger:hover {
  background: rgba(0, 0, 0, 0.04);
}
.username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}
.arrow-icon {
  color: #999;
  font-size: 12px;
}

/* ── Main ── */
.edu-main {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0;
  min-height: calc(100vh - 64px - 48px);
  background: transparent;
}

/* ── Footer ── */
.edu-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8892a4;
  font-size: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  background: transparent;
}
</style>
