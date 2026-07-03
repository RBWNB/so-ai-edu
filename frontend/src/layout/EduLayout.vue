<template>
  <el-container class="edu-layout">
    <!-- 动态波浪背景（复用 B端同款全局样式） -->
    <div class="ambient-background">
      <div class="ambient-glow glow-blue"></div>
      <div class="ambient-glow glow-seafoam"></div>
      <div class="caustics-layer"></div>
      <div class="topography-bg"></div>
    </div>

    <!-- Three.js 粒子背景层：全屏固定，不拦截交互，z-index 同 ambient-background（靠 DOM 顺序叠加在其上） -->
    <canvas ref="threeCanvas" class="particle-canvas"></canvas>

    <!-- 顶部导航：深海悬浮岛 —— 始终可点击，鉴权由路由守卫处理 -->
    <el-header class="edu-header" height="60px">
      <div class="header-inner">
        <!-- Logo -->
        <div class="logo" @click="$router.push('/home')">
          <svg class="logo-whale" viewBox="0 0 96 64" width="36" height="24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M22 34 C14 28, 6 30, 4 34 C10 34, 16 34, 22 34"
                  fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M22 34 C14 40, 6 38, 4 34 C10 34, 16 34, 22 34"
                  fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M22 34 C22 20, 36 12, 56 14 C72 16, 82 20, 86 30 C88 38, 84 42, 80 40 C74 38, 68 44, 56 46 C42 49, 26 46, 22 38 Z"
                  fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linejoin="round"/>
            <path d="M24 36 C32 45, 46 48, 58 45 C68 43, 76 38, 80 38 C76 42, 68 45, 56 46 C42 48, 30 45, 24 36 Z"
                  fill="#e0edff"/>
            <path d="M44 42 C38 48, 30 54, 28 54 C32 50, 38 44, 44 42"
                  fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M48 17 C48 8, 56 8, 56 19"
                  fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="65" cy="26" r="5.5" fill="#fafcff" stroke="#1a2b5c" stroke-width="1.8"/>
            <circle cx="65" cy="26" r="2.5" fill="#1a2b5c"/>
            <circle cx="63" cy="24" r="1.4" fill="#fff"/>
            <circle cx="75" cy="26" r="5.5" fill="#fafcff" stroke="#1a2b5c" stroke-width="1.8"/>
            <circle cx="75" cy="26" r="2.5" fill="#1a2b5c"/>
            <circle cx="73" cy="24" r="1.4" fill="#fff"/>
            <circle cx="59" cy="32" r="3.2" fill="#ffd6e0" opacity="0.6"/>
            <circle cx="81" cy="32" r="3.2" fill="#ffd6e0" opacity="0.6"/>
            <path d="M68 34 Q71 37, 74 34" fill="none" stroke="#1a2b5c" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span class="logo-text">海洋学堂</span>
        </div>

        <!-- 导航链接 + 流体滑动指示器 -->
        <nav ref="navRef" class="nav-links">
          <div
            class="nav-indicator"
            :style="{
              transform: `translateX(${indicatorX}px)`,
              width: `${indicatorW}px`,
              opacity: indicatorVisible ? 1 : 0,
            }"
          ></div>
          <router-link to="/home"          class="nav-item" active-class="nav-active">首页</router-link>
          <router-link to="/encyclopedia"  class="nav-item" active-class="nav-active">海洋百科</router-link>
          <router-link to="/ai-assistant"  class="nav-item" active-class="nav-active">智识海物</router-link>
          <router-link to="/obs-community"   class="nav-item" active-class="nav-active">海友社区</router-link>
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
          <!-- 主题切换按钮 -->
          <div class="theme-toggle" @click="themeStore.toggle()" :title="themeStore.isDark() ? '切换为正常模式' : '切换为护眼模式'">
            <el-icon :size="18">
              <Sunny v-if="themeStore.isDark()" />
              <Moon v-else />
            </el-icon>
          </div>

          <!--  消息铃铛  -->
          <div
              v-if="authStore.isLoggedIn"
              class="notification-wrapper"
              @mouseenter="onBellEnter"
              @mouseleave="onBellLeave"
          >
            <el-badge :is-dot="unreadCount > 0" class="bell-badge">
              <el-icon class="bell-icon" :class="{ 'is-ringing': unreadCount > 0 }">
                <Bell />
              </el-icon>
            </el-badge>

            <Transition name="hologram-menu">
              <div v-if="bellVisible" class="notification-popover">
                <div class="noti-header">
                  <span>消息通知</span>
                  <div class="noti-actions">
                    <span class="action-btn" @click="handleReadAll">全部已读</span>
                    <span class="action-btn danger" @click="handleClearAll">清空</span>
                  </div>
                </div>

                <div class="noti-list" v-if="notificationList.length > 0">
                  <div
                      v-for="(item, index) in notificationList"
                      :key="item.id"
                      class="noti-item"
                      :class="{ 'is-unread': item.isRead === 0 }"
                      @click="goToPost(item)"
                  >
                    <el-avatar :size="32" :src="item.senderAvatar" />
                    <div class="noti-content">
                      <div class="noti-title">
                        <span class="sender-name">
                          {{ item.type === 'broadcast' || item.type === 'broadcast_link' ? '【系统广播】' : item.senderName }}
                        </span>

                        <span class="action-text">
                          {{
                            item.type === 'broadcast' ? item.content
                                : item.type === 'broadcast_link' ? item.content
                                    : item.type.includes('like') ? '赞了你的内容'
                                        : '回复了你'
                          }}
                        </span>
                      </div>
                      <div class="noti-time">{{ item.createdAt }}</div>
                    </div>

                    <div class="noti-delete" @click.stop="handleDelete(item, index)">
                      <el-icon><Close /></el-icon>
                    </div>
                  </div>
                </div>
                <div v-else class="noti-empty">暂无新消息</div>
              </div>
            </Transition>
          </div>
          <!-- RAG 智能问答按钮 -->
          <AgentFloatWidget
              v-if="authStore.isLoggedIn"
              @click="openRagChat"
          />

          <!-- 已登录：头像 + 下拉菜单（自定义 hover 控制，告别 el-popover） -->
          <template v-if="authStore.isLoggedIn">
            <div
              class="user-dropdown-wrapper"
              @mouseenter="onUserMenuEnter"
              @mouseleave="onUserMenuLeave"
            >
              <!-- 触发区：小头像 + 名 + 箭头 -->
              <div class="user-trigger" :class="{ 'is-hovering': menuVisible }">
                <div
                  class="header-avatar-frame"
                  :class="[
                    'frame-' + (authStore.avatarFrame || 'default'),
                    { 'is-hidden': menuVisible }
                  ]"
                >
                  <el-avatar :size="28" :src="userAvatar"
                             class="nav-small-avatar"
                             :class="{ 'is-hidden': menuVisible }"
                  >
                    <el-icon><User /></el-icon>
                  </el-avatar>
                </div>
                <span class="username">{{ authStore.displayName }}</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>

              <!-- 下拉菜单面板 -->
              <Transition name="hologram-menu">
                <div v-if="menuVisible" class="user-popover-panel">
                  <div class="popover-content">
                    <div class="popover-avatar-wrapper">
                      <div
                          class="popover-avatar-frame"
                          :class="'frame-' + (authStore.avatarFrame || 'default')"
                      >
                        <el-avatar :size="76" :src="userAvatar" class="popover-avatar">
                          <el-icon :size="36"><User /></el-icon>
                        </el-avatar>
                      </div>
                    </div>

                    <div class="popover-body">
                      <div class="popover-user-info">
                        <div class="pop-username">{{ authStore.displayName }}</div>
                        <div class="pop-role">
                          <el-tag size="small" class="custom-tag">{{ isAdmin ? '系统管理员' : '深海探索者' }}</el-tag>
                        </div>
                      </div>

                      <div class="popover-stats">
                        <div class="stat-item">
                          <div class="stat-val">Lv.{{ userInfo.level || 1 }}</div>
                          <div class="stat-label">当前等级</div>
                        </div>
                        <div class="stat-item">
                          <div class="stat-val">{{ userInfo.availablePoints || 0 }}</div>
                          <div class="stat-label">可用积分</div>
                        </div>
                      </div>

                      <div class="popover-menu">
                        <div class="menu-item" @click="handleUserCommand('profile')">
                          <el-icon><User /></el-icon><span>个人中心</span>
                        </div>
                        <div v-if="isAdmin" class="menu-item" @click="handleUserCommand('admin')">
                          <el-icon><Setting /></el-icon><span>进入后台</span>
                        </div>
                        <div class="menu-divider"></div>
                        <div class="menu-item logout" @click="handleUserCommand('logout')">
                          <el-icon><SwitchButton /></el-icon><span>退出登录</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </Transition>
            </div>
          </template>

          <!-- 未登录：登录按钮 -->
          <el-button
            v-else
            class="login-glass-btn"
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
      <router-view v-slot="{ Component }">
        <keep-alive exclude="Publish">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </el-main>

    <!-- 底部 -->
    <el-footer class="edu-footer" height="48px">
      <span>© 2026 海洋学堂 · 探索蔚蓝世界</span>
    </el-footer>

    <!-- RAG 智能问答浮窗 -->
    <RagChatWindow ref="ragChatRef" />
  </el-container>
</template>

<script setup>
import { computed, ref, reactive, watch, nextTick, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import * as THREE from "three";
import { useAuthStore } from "@/store/auth";
import { ElMessageBox, ElMessage } from "element-plus";
import { ArrowDown, Bell, ChatDotRound, Close, Moon, Setting, Ship, Sunny, SwitchButton, User } from "@element-plus/icons-vue";
import RagChatWindow from "@/components/RagChatWindow.vue";
import AgentFloatWidget from "@/components/AgentFloatWidget.vue";
import { getUnreadCount, getNotificationList, markNotificationRead, markAllRead, deleteNotification, clearAllNotifications } from "@/utils/notification";
import { useThemeStore } from "@/store/theme";

const route = useRoute();
const $router = useRouter();
const authStore = useAuthStore();
const themeStore = useThemeStore();
const ragChatRef = ref(null);
const threeCanvas = ref(null);

const bellVisible = ref(false);
const unreadCount = ref(0);
const notificationList = ref([]);
let bellLeaveTimer = null;
let unreadPollingTimer = null; // 全局未读条数定时轮询器

// 获取最新的未读总数
const fetchUnreadNumber = async () => {
  if (!authStore.isLoggedIn) return;
  try {
    const res = await getUnreadCount();
    if (res?.data?.success) {
      unreadCount.value = res.data.data || 0;
    }
  } catch (err) {
    console.error("轮询消息未读数异常", err);
  }
};

// 鼠标悬停进入铃铛：显示通知面板，动态加载前5条最新互动
const onBellEnter = async () => {
  clearTimeout(bellLeaveTimer);
  bellVisible.value = true;
  if (!authStore.isLoggedIn) return;
  try {
    const res = await getNotificationList({ pageNum: 1, pageSize: 5 });
    if (res?.data?.success) {
      notificationList.value = res.data.data.records || [];
    }
  } catch (err) {
    console.error("加载消息面板失败", err);
  }
};

// 鼠标移出铃铛：延时关闭面板
const onBellLeave = () => {
  bellLeaveTimer = setTimeout(() => {
    bellVisible.value = false;
  }, 250);
};

// 点击具体通知项：设为已读并跳转对应的帖子详情页
const goToPost = async (item) => {
  try {
    if (item.isRead === 0) {
      await markNotificationRead(item.id);
      item.isRead = 1;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }

    // 判断 postId 是否有效（不跳转）：雪花 ID 精度敏感，统一用字符串比较
    // 后端广播 post_id=0，可能序列化为数字 0 或字符串 "0"，都需要拦截
    const pidStr = String(item.postId ?? '');
    const isInvalidPid = !pidStr || pidStr === '0' || pidStr === 'null' || pidStr === 'undefined';

    // 纯广播 / 广播链接 且无有效跳转目标：仅关闭面板
    const isBroadcastType = item.type === 'broadcast' || item.type === 'broadcast_link';
    if (isBroadcastType && isInvalidPid) {
      bellVisible.value = false;
      return;
    }

    // 兜底：任何类型的通知，postId 无效都不跳转
    if (isInvalidPid) {
      bellVisible.value = false;
      return;
    }

    bellVisible.value = false;
    // broadcast_link / like / reply，用 String() 包裹防止雪花 ID 精度丢失
    $router.push(`/obs-community/detail/${pidStr}`);
  } catch (err) {
    ElMessage.error("操作失败，请重试");
  }
};

// 点击一键全部已读
const handleReadAll = async () => {
  try {
    const res = await markAllRead();
    if (res?.data?.success) {
      unreadCount.value = 0;
      notificationList.value.forEach(x => x.isRead = 1);
      ElMessage.success("全部通知已标记为已读");
    }
  } catch (err) {
    ElMessage.error("一键已读处理失败");
  }
};

// 单条删除
const handleDelete = async (item, index) => {
  try {
    // 1. 乐观更新：直接从列表中移除
    notificationList.value.splice(index, 1);

    // 如果删除的是未读消息，未读数减1
    if (item.isRead === 0) {
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }

    // 2. 异步请求后端删除
    await deleteNotification(item.id);
  } catch (err) {
    ElMessage.error("删除失败");
  }
};

// 清空所有通知
const handleClearAll = async () => {
  try {
    // 1. 乐观更新：清空列表和红点
    notificationList.value = [];
    unreadCount.value = 0;

    // 2. 请求后端清空
    await clearAllNotifications();
    ElMessage.success("消息已清空");
  } catch (err) {
    ElMessage.error("清空失败");
  }
};

// 监听登录状态：一旦登入立刻开启未读轮询；登出则注销并清空状态
watch(() => authStore.isLoggedIn, (isLoggedIn) => {
  if (isLoggedIn) {
    fetchUnreadNumber();
    unreadPollingTimer = setInterval(fetchUnreadNumber, 30000); // 每30秒自动检测一次新消息
  } else {
    unreadCount.value = 0;
    notificationList.value = [];
    if (unreadPollingTimer) {
      clearInterval(unreadPollingTimer);
      unreadPollingTimer = null;
    }
  }
}, { immediate: true });

// ── Three.js 粒子背景\
/** 粒子数量响应式分级（按屏幕宽度） */
const getParticleCount = () => {
  const w = window.innerWidth;
  if (w < 768) return 700;   // 移动端
  if (w < 1024) return 1100; // 平板
  return 1600;               // 桌面端
};
let PARTICLE_COUNT = getParticleCount();

/** 视差幅度：移动端/平板自动降低 */
const getParallaxPx = () => {
  const w = window.innerWidth;
  if (w < 768) return 10;
  if (w < 1024) return 15;
  return 20;
};
let PARALLAX_PX = getParallaxPx();

/** 帧率自适应：无交互 3s 后降至 30fps */
let lastInteractionTime = performance.now();
const IDLE_TIMEOUT = 3000; // ms
let isIdleMode = false;
let frameSkipCounter = 0;

/** 标记用户交互（所有事件处理器调用） */
const markInteraction = () => {
  lastInteractionTime = performance.now();
  if (isIdleMode) {
    isIdleMode = false;
    frameSkipCounter = 0;
  }
};

const COLOR_PRIMARY = new THREE.Color("#165dff");
const COLOR_AUX = new THREE.Color("#36cfc9");

let renderer = null;
let scene = null;
let camera = null;
let points = null;
let animationId = null;
let glowTexture = null;

// ── 交互状态 ──
// 鼠标视差（归一化 -1..1，带缓动）
let targetMouseX = 0;
let targetMouseY = 0;
let currentMouseX = 0;
let currentMouseY = 0;
const LERP_FACTOR = 0.05; // 缓动系数，值越小跟随越平滑

// 滚动联动
let scrollProgress = 0;          // 0..1 页面滚动进度
let lastScrollY = 0;
let scrollVelocity = 0;          // 瞬时滚动速度（px/frame）
const SCROLL_VELOCITY_DECAY = 0.92; // 每帧衰减系数

// ── 导航 Hover 联动 ──
let navHoverWorldX = 0;          // hover 导航项在世界的 X 坐标
let navHoverIntensity = 0;       // 当前强度 0..1（缓动）
let navHoverTarget = 0;          // 目标强度
const NAV_HOVER_RANGE = 6;       // 粒子吸引范围（世界单位）
const NAV_ATTRACT_FORCE = 0.005; // 吸引力系数

// ── 点击波纹 ──
const MAX_RIPPLES = 2;
const RIPPLE_DURATION = 0.6;     // 波纹持续时间（秒）
const RIPPLE_MAX_RADIUS = 12;    // 最大扩散半径（世界单位）
let ripples = [];                // { worldX, worldY, startTime }

// 世界半宽/半高缓存（供事件处理器换算用）
let worldHalfW = 25;
let worldHalfH = 25;

/** 检测 WebGL 是否可用 */
const checkWebGL = () => {
  try {
    const c = document.createElement("canvas");
    const gl = c.getContext("webgl") || c.getContext("experimental-webgl");
    return !!gl;
  } catch {
    return false;
  }
};

const createGlowTexture = () => {
  const size = 64;
  const c = document.createElement("canvas");
  c.width = size;
  c.height = size;
  const ctx = c.getContext("2d");
  const gradient = ctx.createRadialGradient(
    size / 2,
    size / 2,
    0,
    size / 2,
    size / 2,
    size / 2,
  );
  gradient.addColorStop(0, "rgba(255,255,255,1)");
  gradient.addColorStop(0.18, "rgba(255,255,255,0.9)");
  gradient.addColorStop(0.4, "rgba(255,255,255,0.45)");
  gradient.addColorStop(0.7, "rgba(255,255,255,0.1)");
  gradient.addColorStop(1, "rgba(255,255,255,0)");
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, size, size);
  return new THREE.CanvasTexture(c);
};

/** 初始化 Three.js 场景 */
const initThree = () => {
  if (!checkWebGL()) {
    console.warn("[粒子背景] WebGL 不可用，回退到 CSS 背景");
    return;
  }
  const canvas = threeCanvas.value;
  if (!canvas) return;

  scene = new THREE.Scene();

  const w = window.innerWidth;
  const h = window.innerHeight;

  // 透视相机：营造水下纵深感
  camera = new THREE.PerspectiveCamera(55, w / h, 1, 200);
  camera.position.z = 60;

  // 渲染器
  renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: false });
  renderer.setSize(w, h);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.setClearColor(0x000000, 0);

  // 光晕纹理
  glowTexture = createGlowTexture();

  // ── 构建粒子几何体 ──
  const geometry = new THREE.BufferGeometry();
  const positions = new Float32Array(PARTICLE_COUNT * 3);
  const colors = new Float32Array(PARTICLE_COUNT * 3);
  const sizes = new Float32Array(PARTICLE_COUNT);
  const speeds = new Float32Array(PARTICLE_COUNT);       // 上升速度
  const phases = new Float32Array(PARTICLE_COUNT);       // 水平漂移相位
  const driftAmps = new Float32Array(PARTICLE_COUNT);    // 漂移幅度

  // 可见区域（世界坐标）
  const viewHeight = 50;
  const viewWidth = viewHeight * (w / h);
  worldHalfW = viewWidth / 2;
  worldHalfH = viewHeight / 2;

  for (let i = 0; i < PARTICLE_COUNT; i++) {
    // X / Y 均匀散布；Z 随机深度（离相机越远 → 越小，营造层次感）
    positions[i * 3] = (Math.random() - 0.5) * viewWidth;
    positions[i * 3 + 1] = (Math.random() - 0.5) * viewHeight;
    positions[i * 3 + 2] = (Math.random() - 0.5) * 30;

    // 颜色：主色 #165dff ↔ 辅色 #36cfc9 之间随机混合，加大扰动提亮
    const roll = Math.random();
    if (roll < 0.08) {
      // 8% 生物荧光亮点：亮白 → 亮青
      const sparkT = Math.random();
      const sparkColor = new THREE.Color().setHSL(0.55 + sparkT * 0.08, 0.9, 0.65 + sparkT * 0.3);
      colors[i * 3] = sparkColor.r;
      colors[i * 3 + 1] = sparkColor.g;
      colors[i * 3 + 2] = sparkColor.b;
    } else {
      const t = Math.random();
      const color = new THREE.Color().copy(COLOR_PRIMARY).lerp(COLOR_AUX, t);
      color.r += (Math.random() - 0.5) * 0.12;
      color.g += (Math.random() - 0.5) * 0.12;
      color.b += (Math.random() - 0.5) * 0.12;
      colors[i * 3] = Math.max(0, Math.min(1, color.r));
      colors[i * 3 + 1] = Math.max(0, Math.min(1, color.g));
      colors[i * 3 + 2] = Math.max(0, Math.min(1, color.b));
    }

    // 大小：大部分为中等浮游颗粒，少数较大（模拟远近层次）
    sizes[i] = Math.random() < 0.82
      ? Math.random() * 0.4 + 0.1     // 82% 中等颗粒
      : Math.random() * 0.9 + 0.35;   // 18% 大型颗粒

    speeds[i] = Math.random() * 0.25 + 0.08;          // 上升速度
    phases[i] = Math.random() * Math.PI * 2;           // 漂移相位
    driftAmps[i] = Math.random() * 0.35 + 0.1;         // 漂移幅度
  }

  geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
  geometry.setAttribute("color", new THREE.BufferAttribute(colors, 3));
  geometry.setAttribute("size", new THREE.BufferAttribute(sizes, 1));

  // 材质：光晕纹理 + 加法混合，粒子亮眼且自带氛围光感
  const material = new THREE.PointsMaterial({
    size: 0.65,
    map: glowTexture,
    vertexColors: true,
    depthWrite: false,
    depthTest: false,
    transparent: true,
    opacity: 0.62,
    blending: THREE.AdditiveBlending,
  });

  points = new THREE.Points(geometry, material);
  points.userData = { halfW: worldHalfW, halfH: worldHalfH, speeds, phases, driftAmps };
  scene.add(points);

  animate();
};

/** 动画循环 */
const animate = () => {
  animationId = requestAnimationFrame(animate);

  // ── 0. 帧率自适应：3s 无交互 → 降至 30fps ──
  const nowMs = performance.now();
  if (nowMs - lastInteractionTime > IDLE_TIMEOUT) {
    isIdleMode = true;
  }
  if (isIdleMode) {
    frameSkipCounter++;
    if (frameSkipCounter % 2 !== 0) return; // 跳帧
  }

  if (!points || !renderer || !camera) return;

  // ── 1. 鼠标视差：缓动跟随（模拟水流惯性） ──
  currentMouseX += (targetMouseX - currentMouseX) * LERP_FACTOR;
  currentMouseY += (targetMouseY - currentMouseY) * LERP_FACTOR;

  // 像素偏移 → 世界坐标换算
  // 相机 fov=55°, z=60 → 可见半高 = tan(27.5°) × 60 ≈ 31.2
  const halfFovRad = (camera.fov / 2) * (Math.PI / 180);
  const visibleHalfH = Math.tan(halfFovRad) * camera.position.z;
  const visibleHalfW = visibleHalfH * (window.innerWidth / window.innerHeight);
  const worldParallaxX = (PARALLAX_PX / window.innerWidth) * visibleHalfW * 2;
  const worldParallaxY = (PARALLAX_PX / window.innerHeight) * visibleHalfH * 2;

  // 应用视差到相机位置（基准 0, 0, 60）
  camera.position.x = currentMouseX * worldParallaxX;
  camera.position.y = -currentMouseY * worldParallaxY + scrollProgress * 5; // +滚动深度偏移

  // ── 2. 滚动速度自然衰减 ──
  scrollVelocity *= SCROLL_VELOCITY_DECAY;

  // ── 3. 导航 Hover 缓动 ──
  navHoverIntensity += (navHoverTarget - navHoverIntensity) * 0.06;

  // ── 4. 清理过期波纹 ──
  const frameTime = nowMs * 0.001;
  ripples = ripples.filter(r => frameTime - r.startTime < RIPPLE_DURATION);

  // ── 5. 更新粒子 ──
  const posAttr = points.geometry.getAttribute("position");
  const pos = posAttr.array;
  const { halfH, speeds, phases, driftAmps } = points.userData;

  const now = frameTime;
  const baseSpeed = 0.018;
  const scrollBoost = 1 + scrollVelocity * 0.03;             // 滚动越快 → 粒子上升越快
  const speedScale = baseSpeed * Math.min(scrollBoost, 2.5); // 上限 2.5×，防止过快

  for (let i = 0; i < PARTICLE_COUNT; i++) {
    const idx = i * 3;

    // 主运动：缓慢上升（受滚动速度调制）
    pos[idx + 1] += speeds[i] * speedScale;

    // 水平微漂：正弦振荡，模拟水流的轻柔推动
    pos[idx] += Math.sin(now * 0.7 + phases[i]) * driftAmps[i] * 0.004;

    // ── 导航 Hover 吸引 + 亮度提升 ──
    if (navHoverIntensity > 0.01) {
      const dx = navHoverWorldX - pos[idx];
      const distX = Math.abs(dx);
      if (distX < NAV_HOVER_RANGE) {
        // 距离越近，影响越强
        const influence = (1 - distX / NAV_HOVER_RANGE) * navHoverIntensity;
        // X 轴吸引力 → 粒子向导航项下方聚集
        pos[idx] += dx * influence * NAV_ATTRACT_FORCE;
        // Z 轴前推 → 粒子"浮起"靠近相机，因透视而显得更大更亮
        pos[idx + 2] += influence * 0.8;
      }
    }

    // ── 点击波纹推力（模拟水面涟漪扩散） ──
    for (let r = 0; r < ripples.length; r++) {
      const ripple = ripples[r];
      const elapsed = frameTime - ripple.startTime;
      const progress = Math.min(elapsed / RIPPLE_DURATION, 1); // 0→1
      const radius = progress * RIPPLE_MAX_RADIUS;
      // sin 包络：波纹从零起→峰→零落
      const amplitude = Math.sin(progress * Math.PI);

      const rx = pos[idx] - ripple.worldX;
      const ry = pos[idx + 1] - ripple.worldY;
      const dist = Math.sqrt(rx * rx + ry * ry);
      const ringHalfWidth = 2.2; // 波纹环带半宽

      if (Math.abs(dist - radius) < ringHalfWidth) {
        // 环带内的粒子被向外推开
        const ringT = 1 - Math.abs(dist - radius) / ringHalfWidth; // 1（在环上）→ 0（在边缘）
        const push = ringT * amplitude * 0.55;
        if (dist > 0.001) {
          pos[idx] += (rx / dist) * push;
          pos[idx + 1] += (ry / dist) * push;
        }
        // 波纹经过时粒子略微"浮起"
        pos[idx + 2] += ringT * amplitude * 0.25;
      }
    }

    // 超出顶部 → 从底部循环
    if (pos[idx + 1] > halfH) {
      pos[idx + 1] = -halfH;
      pos[idx] = (Math.random() - 0.5) * halfH * 2 * (window.innerWidth / window.innerHeight || 1.6);
    }

    // 超出 Z 轴范围 → 钳制（防止 Hover/波纹推太远）
    if (pos[idx + 2] > 15) pos[idx + 2] = 15;
    if (pos[idx + 2] < -15) pos[idx + 2] = -15;
  }

  posAttr.needsUpdate = true;
  renderer.render(scene, camera);
};

/** 响应式重建粒子几何体（保留场景/相机/渲染器/材质） */
const rebuildParticles = () => {
  if (!points || !scene) return;
  scene.remove(points);
  points.geometry.dispose();
  // 保留材质（含 glowTexture 引用）

  const w = window.innerWidth;
  const h = window.innerHeight;
  const viewHeight = 50;
  const viewWidth = viewHeight * (w / h);

  const geometry = new THREE.BufferGeometry();
  const positions = new Float32Array(PARTICLE_COUNT * 3);
  const colors = new Float32Array(PARTICLE_COUNT * 3);
  const sizes = new Float32Array(PARTICLE_COUNT);
  const speeds = new Float32Array(PARTICLE_COUNT);
  const phases = new Float32Array(PARTICLE_COUNT);
  const driftAmps = new Float32Array(PARTICLE_COUNT);

  for (let i = 0; i < PARTICLE_COUNT; i++) {
    positions[i * 3] = (Math.random() - 0.5) * viewWidth;
    positions[i * 3 + 1] = (Math.random() - 0.5) * viewHeight;
    positions[i * 3 + 2] = (Math.random() - 0.5) * 30;
    const roll = Math.random();
    if (roll < 0.08) {
      const sparkT = Math.random();
      const sparkColor = new THREE.Color().setHSL(0.55 + sparkT * 0.08, 0.9, 0.65 + sparkT * 0.3);
      colors[i * 3] = sparkColor.r;
      colors[i * 3 + 1] = sparkColor.g;
      colors[i * 3 + 2] = sparkColor.b;
    } else {
      const t = Math.random();
      const color = new THREE.Color().copy(COLOR_PRIMARY).lerp(COLOR_AUX, t);
      color.r += (Math.random() - 0.5) * 0.12;
      color.g += (Math.random() - 0.5) * 0.12;
      color.b += (Math.random() - 0.5) * 0.12;
      colors[i * 3] = Math.max(0, Math.min(1, color.r));
      colors[i * 3 + 1] = Math.max(0, Math.min(1, color.g));
      colors[i * 3 + 2] = Math.max(0, Math.min(1, color.b));
    }
    sizes[i] = Math.random() < 0.82
      ? Math.random() * 0.4 + 0.1
      : Math.random() * 0.9 + 0.35;
    speeds[i] = Math.random() * 0.25 + 0.08;
    phases[i] = Math.random() * Math.PI * 2;
    driftAmps[i] = Math.random() * 0.35 + 0.1;
  }

  geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
  geometry.setAttribute("color", new THREE.BufferAttribute(colors, 3));
  geometry.setAttribute("size", new THREE.BufferAttribute(sizes, 1));

  // 复用材质
  const newPoints = new THREE.Points(geometry, points.material);
  newPoints.userData = {
    halfW: worldHalfW,
    halfH: worldHalfH,
    speeds,
    phases,
    driftAmps,
  };
  scene.add(newPoints);
  points = newPoints;
};

/** 窗口尺寸变化 */
const handleResize = () => {
  if (!renderer || !camera) return;

  const w = window.innerWidth;
  const h = window.innerHeight;

  camera.aspect = w / h;
  camera.updateProjectionMatrix();
  renderer.setSize(w, h);
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

  // 响应式粒子数量：跨分级阈值时重建几何体
  const newCount = getParticleCount();
  const newParallax = getParallaxPx();
  if (newCount !== PARTICLE_COUNT) {
    PARTICLE_COUNT = newCount;
    PARALLAX_PX = newParallax;
    rebuildParticles();
  } else {
    PARALLAX_PX = newParallax;
  }

  // 同步更新可见区域边界
  if (points) {
    const viewHeight = 50;
    const viewWidth = viewHeight * (w / h);
    worldHalfW = viewWidth / 2;
    worldHalfH = viewHeight / 2;
    points.userData.halfW = worldHalfW;
    points.userData.halfH = worldHalfH;
  }
};

/** 鼠标移动 → 更新视差目标（归一化到 -1..1，屏幕中心为原点） */
const handleMouseMove = (e) => {
  markInteraction();
  targetMouseX = (e.clientX / window.innerWidth) * 2 - 1;
  targetMouseY = (e.clientY / window.innerHeight) * 2 - 1;
};

/** 滚动事件 → 更新进度 & 瞬时速度 */
const handleScroll = () => {
  markInteraction();
  const docHeight = document.documentElement.scrollHeight - window.innerHeight;
  if (docHeight > 0) {
    scrollProgress = Math.min(window.scrollY / docHeight, 1);
  }

  const currentScrollY = window.scrollY;
  // 瞬时速度：本次滚动位移（px/frame），取绝对值用于调速
  const delta = Math.abs(currentScrollY - lastScrollY);
  // 只在加速方向上更新（避免速度跳变回零）
  if (delta > scrollVelocity) {
    scrollVelocity = delta;
  }
  lastScrollY = currentScrollY;
};

/** 导航项 Hover 委托 —— 计算 hover 位置在世界空间的 X 坐标 */
const handleNavHover = (e) => {
  markInteraction();
  const navItem = e.target.closest(".nav-item");
  if (!navItem) return;
  const rect = navItem.getBoundingClientRect();
  const centerX = rect.left + rect.width / 2;
  // 屏幕像素 → 归一化 -1..1 → 世界 X
  const normalizedX = (centerX / window.innerWidth) * 2 - 1;
  navHoverWorldX = normalizedX * worldHalfW;
  navHoverTarget = 1;
};

/** 导航项离开 → 粒子恢复 */
const handleNavLeave = (e) => {
  markInteraction();
  const navItem = e.target.closest(".nav-item");
  if (!navItem) return;
  navHoverTarget = 0;
};

/** 页面点击 → 产生粒子波纹 */
const handlePageClick = (e) => {
  markInteraction();
  // 屏幕坐标 → 世界坐标
  const screenX = (e.clientX / window.innerWidth) * 2 - 1;
  const screenY = -(e.clientY / window.innerHeight) * 2 + 1;
  const rippleX = screenX * worldHalfW;
  const rippleY = screenY * worldHalfH;

  ripples.push({
    worldX: rippleX,
    worldY: rippleY,
    startTime: performance.now() * 0.001,
  });

  // 超过上限 → 移除最旧的
  if (ripples.length > MAX_RIPPLES) {
    ripples.shift();
  }
};

/** 完整销毁（防止内存泄漏） */
const destroyThree = () => {
  if (animationId) {
    cancelAnimationFrame(animationId);
    animationId = null;
  }
  if (points) {
    if (points.geometry) points.geometry.dispose();
    if (points.material) {
      // points.material.map === glowTexture，材质释放 map 后不再重复 dispose
      if (points.material.map) points.material.map.dispose();
      points.material.dispose();
    }
    points = null;
    glowTexture = null; // 已在 material.map.dispose() 中释放
  }
  if (glowTexture) {
    // 仅在 points 已为 null 但 glowTexture 未释放时执行（异常情况兜底）
    glowTexture.dispose();
    glowTexture = null;
  }
  if (renderer) {
    renderer.dispose();
    try {
      renderer.forceContextLoss();
    } catch {
      // 部分浏览器不支持，忽略
    }
    renderer = null;
  }
  scene = null;
  camera = null;
};

/** 绑定导航 Hover 委托 */
const bindNavHover = () => {
  const nav = document.querySelector(".nav-links");
  if (nav) {
    nav.addEventListener("mouseover", handleNavHover, true);
    nav.addEventListener("mouseout", handleNavLeave, true);
  }
};
const unbindNavHover = () => {
  const nav = document.querySelector(".nav-links");
  if (nav) {
    nav.removeEventListener("mouseover", handleNavHover, true);
    nav.removeEventListener("mouseout", handleNavLeave, true);
  }
};

onMounted(() => {
  initThree();
  window.addEventListener("resize", handleResize);
  window.addEventListener("mousemove", handleMouseMove, { passive: true });
  window.addEventListener("scroll", handleScroll, { passive: true });
  window.addEventListener("click", handlePageClick, { passive: true });
  // 导航 DOM 已就绪（mounted 后）
  bindNavHover();
});

onUnmounted(() => {
  window.removeEventListener("resize", handleResize);
  window.removeEventListener("mousemove", handleMouseMove);
  window.removeEventListener("scroll", handleScroll);
  window.removeEventListener("click", handlePageClick);
  unbindNavHover();
  destroyThree();
});
// ── Three.js 粒子背景 END ──────────────────────────────────────────
// ── 流体滑动指示器 (Fluid Sliding Indicator) ──
const navRef = ref(null);
const indicatorX = ref(0);
const indicatorW = ref(0);
const indicatorVisible = ref(false);

/** 将指示器定位到指定 nav-item 元素 */
const moveIndicatorTo = (el) => {
  if (!navRef.value || !el) return;
  const navRect = navRef.value.getBoundingClientRect();
  const elRect = el.getBoundingClientRect();
  indicatorX.value = elRect.left - navRect.left;
  indicatorW.value = elRect.width;
  indicatorVisible.value = true;
};

/** 定位到当前路由激活的导航项 */
const syncIndicator = () => {
  if (!navRef.value) return;
  const activeEl = navRef.value.querySelector(".nav-active");
  if (activeEl) {
    moveIndicatorTo(activeEl);
  } else {
    indicatorVisible.value = false;
  }
};

/** 鼠标/指针 进入导航项 → 指示器跟随 */
const onNavPointerEnter = (e) => {
  const item = e.target.closest(".nav-item");
  if (item) moveIndicatorTo(item);
};

/** 鼠标/指针 离开导航区域 → 指示器回归激活项 */
const onNavPointerLeave = () => {
  syncIndicator();
};

// 路由切换后 DOM 更新完毕 → 重新对齐指示器
watch(() => route.fullPath, () => {
  nextTick(syncIndicator);
});

// 窗口尺寸变化可能导致 nav 项位移 → 重新对齐
let _indicatorResizeHandler = null;

onMounted(() => {
  nextTick(syncIndicator);

  const nav = navRef.value;
  if (nav) {
    nav.addEventListener("mouseover", onNavPointerEnter, true);
    nav.addEventListener("mouseout", onNavPointerLeave, true);
  }

  _indicatorResizeHandler = () => syncIndicator();
  window.addEventListener("resize", _indicatorResizeHandler, { passive: true });
});

onUnmounted(() => {
  const nav = navRef.value;
  if (nav) {
    nav.removeEventListener("mouseover", onNavPointerEnter, true);
    nav.removeEventListener("mouseout", onNavPointerLeave, true);
  }
  if (_indicatorResizeHandler) {
    window.removeEventListener("resize", _indicatorResizeHandler);
  }
});

/* 打开 RAG 问答窗口 */
const openRagChat = () => {
  ragChatRef.value?.open();
};

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

/* 用户下拉菜单 — 自定义 hover 控制 */
const menuVisible = ref(false);
let menuLeaveTimer = null;

const userInfo = reactive({
  level: 1,
  availablePoints: 0,
});

const fetchUserInfoBrief = async () => {
  try {
    const { getLearningProfile } = await import("@/api/learning");
    const { getPointsAccount } = await import("@/api/points");
    const [lRes, pRes] = await Promise.all([
      getLearningProfile().catch(() => null),
      getPointsAccount().catch(() => null),
    ]);
    if (lRes?.data?.data) {
      userInfo.level = lRes.data.data.level ?? 1;
    }
    if (pRes?.data?.data) {
      userInfo.availablePoints = pRes.data.data.availablePoints ?? 0;
    }
  } catch { /* 静默 */ }
};

const onUserMenuEnter = () => {
  clearTimeout(menuLeaveTimer);
  fetchUserInfoBrief();
  menuVisible.value = true;
};

const onUserMenuLeave = () => {
  menuLeaveTimer = setTimeout(() => {
    menuVisible.value = false;
  }, 200);
};

/* 用户下拉菜单命令 */
const handleUserCommand = (command) => {
  menuVisible.value = false;
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
/* ══════════════════════════════════════════════════════════════════════
   Three.js 粒子画布（保持不变）
   ══════════════════════════════════════════════════════════════════════ */
.particle-canvas {
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
}

/* ══════════════════════════════════════════════════════════════════════
   全局布局 & CSS 变量
   ══════════════════════════════════════════════════════════════════════ */
.edu-layout {
  --deep-abyss: #0b1a30;
  --cyan-biolume: #00d2ff;
  --cyan-soft: rgba(0, 210, 255, 0.7);
  --white-ghost: rgba(255, 255, 255, 0.9);
  --white-wisp: rgba(255, 255, 255, 0.55);

  width: 100%;
  min-height: 100vh;
  background: transparent;
}

.edu-header {
  width: 100%;
  height: 56px !important;
  padding: 0 16px;
  background: transparent !important;
  pointer-events: none;
  position: sticky;
  top: 16px;
  z-index: 200;
}


.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  gap: 24px;
  position: relative;
  overflow: visible; /* 关键：子元素面板可溢出导航栏不被裁剪 */
  /* 视觉样式：从 .edu-header 迁移至此 */
  max-width: 1280px;
  margin: 0 auto;
  background: rgba(10, 20, 42, 0.75);
  backdrop-filter: blur(20px) saturate(150%);
  -webkit-backdrop-filter: blur(20px) saturate(150%);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 32px;
  padding: 0 12px;
  box-shadow:
    0 10px 30px rgba(0, 0, 0, 0.25),
    0 4px 16px rgba(0, 120, 210, 0.15),
    inset 0 1px 1px rgba(255, 255, 255, 0.1);
  /* 恢复内部点击交互 */
  pointer-events: auto;
  transition: box-shadow 0.4s ease;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
  user-select: none;
}
.logo-whale {
  flex-shrink: 0;
  filter: drop-shadow(0 0 6px rgba(0, 180, 255, 0.4));
  transition: filter 0.3s;
}
.logo:hover .logo-whale {
  filter: drop-shadow(0 0 12px rgba(0, 210, 255, 0.7));
}
.logo-text {
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #00d2ff 0%, #72f0ff 50%, #3a9bd5 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}


.nav-links {
  display: flex;
  gap: 2px;
  flex: 1;
  justify-content: center;
  align-items: center;
  position: relative;
  height: 40px; /* 给指示器一个明确的定位参考 */
}

/* ── 流体滑动指示器 (Fluid Sliding Indicator) ── */
.nav-indicator {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  border-radius: 11px;

  background:
    radial-gradient(ellipse at 50% 100%, rgba(0, 210, 255, 0.18) 0%, transparent 70%),
    rgba(0, 180, 255, 0.08);

  border-bottom: 2px solid rgba(0, 210, 255, 0.5);
  box-shadow:
    0 0 16px rgba(0, 200, 255, 0.12),
    0 0 4px rgba(0, 210, 255, 0.2);

  transition:
    transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1),
    width    0.45s cubic-bezier(0.34, 1.56, 0.64, 1),
    opacity  0.28s ease;
  pointer-events: none;
  z-index: 0;
  will-change: transform, width;
}

.nav-item {
  position: relative;
  z-index: 1;
  padding: 7px 15px;
  border-radius: 10px;
  text-decoration: none;
  color: rgba(230, 245, 255, 0.85);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.3px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.6);
  transition: color 0.3s ease, text-shadow 0.3s ease;
  cursor: pointer;
  white-space: nowrap;
}

.nav-item:hover {
  color: #fff;
  text-shadow: 0 0 14px rgba(0, 210, 255, 0.8), 0 1px 3px rgba(0, 0, 0, 0.6);
}

.nav-active {
  color: #fff !important;
  text-shadow: 0 0 16px rgba(0, 210, 255, 0.9), 0 1px 3px rgba(0, 0, 0, 0.8);
  font-weight: 600;
}
/* 登录按钮 (*/
.login-glass-btn {
  background: rgba(0, 210, 255, 0.08) !important;
  border: 1px solid rgba(0, 210, 255, 0.3) !important;
  color: #00d2ff !important;
  border-radius: 20px;
  padding: 8px 24px;
  font-weight: 600;
  letter-spacing: 1px;
  backdrop-filter: blur(10px);
  transition: all 0.35s cubic-bezier(0.25, 1, 0.5, 1);
}

.login-glass-btn:hover {
  background: linear-gradient(135deg, #165dff, #00d2ff) !important;
  border-color: transparent !important;
  color: #fff !important;
  box-shadow: 0 6px 18px rgba(0, 210, 255, 0.35);
  transform: translateY(-2px);
}

/* 后台管理入口 */
.admin-entry {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-left: 6px;
  /* 深蓝渐变背景 */
  background: linear-gradient(135deg, rgba(0, 55, 130, 0.45), rgba(0, 32, 85, 0.65));
  /* 极细高亮实线边框 */
  border: 1px solid rgba(0, 170, 240, 0.35);
  border-radius: 9px;
  color: rgba(0, 210, 255, 0.85) !important;
  font-weight: 600;
  font-size: 13px;
  padding: 5px 13px;
  transition:
    background 0.3s ease,
    border-color 0.3s ease,
    color 0.3s ease,
    text-shadow 0.3s ease,
    box-shadow 0.35s ease;
}
.admin-entry:hover {
  background: linear-gradient(135deg, rgba(0, 80, 190, 0.55), rgba(0, 50, 120, 0.75)) !important;
  border-color: rgba(0, 220, 255, 0.55);
  color: #fff !important;
  text-shadow: 0 0 16px rgba(0, 210, 255, 0.8);
  /* 外发光 + 内发光 */
  box-shadow:
    0 0 22px rgba(0, 150, 255, 0.3),
    0 0 8px rgba(0, 200, 255, 0.2),
    inset 0 0 18px rgba(0, 180, 255, 0.1);
}


.user-area {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 18px;
}

/* 主题切换按钮 */
.theme-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  cursor: pointer;
  color: rgba(230, 245, 255, 0.85);
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: all 0.3s ease;
}
.theme-toggle:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.25);
  color: #fff;
  box-shadow: 0 0 12px rgba(64, 128, 255, 0.3);
}

/* 下拉菜单容器 */
.user-dropdown-wrapper {
  position: relative;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 10px;
  transition: background 0.25s;
}
.user-trigger:hover,
.user-trigger.is-hovering {
  background: rgba(255, 255, 255, 0.06);
}

/* 小头像 & 头像框：面板弹出瞬间隐身 */
.nav-small-avatar,
.header-avatar-frame {
  transition: all 0.3s ease;
}
.nav-small-avatar.is-hidden,
.header-avatar-frame.is-hidden {
  opacity: 0;
  transform: scale(0.1);
  pointer-events: none;
}

/* hover 时，用户名和箭头淡出 */
.username {
  font-size: 14px;
  font-weight: 500;
  color: rgba(235, 245, 255, 0.85);
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.user-trigger.is-hovering .username {
  opacity: 0;
  transform: translateX(-8px);
}

.arrow-icon {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.user-trigger.is-hovering .arrow-icon {
  opacity: 0;
  transform: translateX(-4px);
}

.user-popover-panel {
  position: absolute;
  top: calc(100% + 8px);
  left: calc(27px - 140px);
  width: 280px;
  z-index: 9999;
  background: rgba(10, 18, 38, 0.85);
  backdrop-filter: blur(24px) saturate(150%);
  -webkit-backdrop-filter: blur(24px) saturate(150%);
  border: 1px solid rgba(0, 210, 255, 0.2);
  border-radius: 24px;
  box-shadow:
      0 24px 48px rgba(0, 0, 0, 0.6),
      inset 0 1px 1px rgba(255, 255, 255, 0.1);
  overflow: visible;
  transform-origin: center top;
}

/* Vue Transition — 面板进出动画 */
.hologram-menu-enter-active {
  animation: popoverIn 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.hologram-menu-leave-active {
  animation: popoverOut 0.25s ease;
}
@keyframes popoverIn {
  0%   { opacity: 0; transform: scale(0.75) translateY(-16px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}
@keyframes popoverOut {
  0%   { opacity: 1; transform: scale(1) translateY(0); }
  100% { opacity: 0; transform: scale(0.85) translateY(-8px); }
}

/* ══════════════════════════════════════════════════════════════════════
   Main 主内容区
   ══════════════════════════════════════════════════════════════════════ */
.edu-main {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 24px 24px;
  min-height: calc(100vh - 56px - 32px - 48px); /* header高度 + top偏移 + footer */
  background: transparent;
}

/* ══════════════════════════════════════════════════════════════════════
   Footer
   ══════════════════════════════════════════════════════════════════════ */
.edu-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(160, 190, 215, 0.55);
  font-size: 12px;
  letter-spacing: 0.5px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: transparent;
}

/* ══════════════════════════════════════════════════════════════════════
   响应式调整
   ══════════════════════════════════════════════════════════════════════ */
@media (max-width: 1024px) {
  .edu-header {
    padding: 0 16px;
    top: 10px;
  }
  .header-inner {
    max-width: calc(100% - 32px);
    border-radius: 24px;
    gap: 14px;
  }
  .nav-item {
    padding: 6px 10px;
    font-size: 13px;
  }
  .logo-text {
    font-size: 17px;
  }
}

@media (max-width: 768px) {
  .edu-header {
    padding: 0 12px;
    top: 8px;
    height: 48px !important;
  }
  .header-inner {
    max-width: calc(100% - 16px);
    border-radius: 20px;
    gap: 8px;
  }
  .nav-links {
    gap: 0;
    height: 34px;
  }
  .nav-item {
    padding: 4px 8px;
    font-size: 12px;
    border-radius: 8px;
  }
  .nav-indicator {
    border-radius: 8px;
  }
  .logo-text {
    font-size: 15px;
  }
  .admin-entry {
    padding: 3px 8px;
    font-size: 11px;
  }
  .edu-main {
    padding: 16px 12px;
    min-height: calc(100vh - 48px - 16px - 48px);
  }
}
</style>

<style>
/* ═══ 下拉面板内容样式 ═══ */
.popover-content {
  padding: 0 20px 20px;
  text-align: center;
}

/* 头像向上溢出并带有弹簧放大动画 */
.popover-avatar-wrapper {
  margin-top: -46px; /* 向上溢出面板一半 */
  margin-bottom: 12px;
  display: flex;
  justify-content: center;
}

.popover-avatar {
  border: 4px solid rgba(10, 18, 38, 0.9);
  box-shadow: 0 8px 24px rgba(0, 210, 255, 0.4);
  animation: avatarGrow 0.55s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

@keyframes avatarGrow {
  0%   { transform: translateY(-30px) scale(0.42); opacity: 0; }
  100% { transform: translateY(0)    scale(1);    opacity: 1; }
}

/* 核心特效：菜单主体从头像后方伸展出来 */
.popover-body {
  opacity: 0;
  /* 初始向上收缩并稍微缩小 */
  transform: translateY(-40px) scale(0.9);
  /* 延迟 0.1s 播放，形成交错感 */
  animation: menuExpand 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) 0.1s forwards;
  transform-origin: top center;
}

@keyframes menuExpand {
  0% { opacity: 0; transform: translateY(-40px) scale(0.9); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}

.pop-username {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 6px;
  text-shadow: 0 2px 8px rgba(0, 210, 255, 0.3);
}

.pop-role { margin-bottom: 20px; }
.custom-tag {
  background: rgba(0, 210, 255, 0.1) !important;
  border: 1px solid rgba(0, 210, 255, 0.3) !important;
  color: #00d2ff !important;
  border-radius: 12px;
  padding: 0 14px;
}

/* 资产数据区 */
.popover-stats {
  display: flex;
  justify-content: space-around;
  align-items: center;
  background: rgba(0, 210, 255, 0.05);
  border: 1px solid rgba(0, 210, 255, 0.1);
  border-radius: 16px;
  padding: 16px 12px;
  margin-bottom: 20px;
}

.stat-item { flex: 1; }
.stat-val {
  font-size: 20px;
  font-weight: 800;
  color: #00d2ff;
  line-height: 1.2;
  margin-bottom: 4px;
  text-shadow: 0 0 10px rgba(0, 210, 255, 0.4);
}
.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

/* 交互菜单区 */
.popover-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  color: rgba(235, 245, 255, 0.85);
  font-size: 14px;
  font-weight: 500;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(0, 210, 255, 0.15);
  color: #00d2ff;
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0, 210, 255, 0.1);
}

.menu-item.logout:hover {
  background: rgba(255, 63, 63, 0.15);
  color: #ff3f3f;
  box-shadow: 0 4px 12px rgba(255, 63, 63, 0.1);
}

.menu-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin: 4px 12px;
}
/* ════════════════════════════════════════════════════════════
   铃铛晃动特效
   ════════════════════════════════════════════════════════════ */
@keyframes bell-shake {
  0%   { transform: rotate(0deg); }
  10%  { transform: rotate(15deg); }
  20%  { transform: rotate(-10deg); }
  30%  { transform: rotate(8deg); }
  40%  { transform: rotate(-5deg); }
  50%  { transform: rotate(2deg); }
  60%  { transform: rotate(0deg); }
  100% { transform: rotate(0deg); } /* 留出一段停顿时间 */
}

/* 当有未读消息时，每3秒晃动一次，吸引注意力 */
.bell-icon.is-ringing {
  animation: bell-shake 3s infinite ease-in-out;
  transform-origin: top center; /* 确保是以铃铛顶部为中心摇晃 */
}

/* 鼠标悬停时暂停摇晃，防止点不准 */
.notification-wrapper:hover .bell-icon.is-ringing {
  animation-play-state: paused;
}


/* 下拉面板：清空按钮与单条删除样式 */
.noti-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  font-size: 12px;
  color: #00d2ff;
  font-weight: normal;
  cursor: pointer;
  transition: color 0.3s;
}
.action-btn:hover {
  text-shadow: 0 0 8px rgba(0, 210, 255, 0.6);
}
.action-btn.danger {
  color: rgba(255, 255, 255, 0.5);
}
.action-btn.danger:hover {
  color: #ff3f3f;
  text-shadow: 0 0 8px rgba(255, 63, 63, 0.6);
}

/* 调整 noti-item 为相对定位，以容纳删除按钮 */
.noti-item {
  position: relative;
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  transition: background 0.3s;
  cursor: pointer;
}

/* 单条删除按钮：默认隐藏，悬停时显示 */
.noti-delete {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 63, 63, 0.1);
  color: #ff3f3f;
  opacity: 0;
  transition: opacity 0.3s, background 0.3s, transform 0.2s;
}

.noti-item:hover .noti-delete {
  opacity: 1;
}

.noti-delete:hover {
  background: rgba(255, 63, 63, 0.8);
  color: #fff;
  transform: translateY(-50%) scale(1.1);
}

/* --- 通知中心铃铛样式 --- */
.notification-wrapper {
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  transition: background 0.3s ease, transform 0.2s ease;
}

.bell-icon {
  font-size: 20px;
  color: rgba(235, 245, 255, 0.85);
  transition: all 0.3s ease;
}

.notification-wrapper:hover .bell-icon {
  color: #00d2ff;
  filter: drop-shadow(0 0 8px rgba(0, 210, 255, 0.6));
}

.notification-wrapper:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

.bell-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* 通知面板 */
.notification-popover {
  position: absolute;
  top: calc(100% + 16px);
  right: -10px;
  width: 300px;
  z-index: 9999;
  background: rgba(10, 18, 38, 0.95);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(0, 210, 255, 0.2);
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.6);
  padding: 12px 0;
}

.noti-header {
  display: flex;
  justify-content: space-between;
  padding: 0 16px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 14px;
  color: #fff;
  font-weight: 600;
}

.read-all {
  font-size: 12px;
  color: #00d2ff;
  font-weight: normal;
}

.noti-list {
  max-height: 320px;
  overflow-y: auto;
}

.noti-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  transition: background 0.3s;
}

.noti-item:hover {
  background: rgba(0, 210, 255, 0.08);
}

.noti-item.is-unread {
  background: rgba(0, 210, 255, 0.04);
}

.noti-item.is-unread::before {
  content: '';
  position: absolute;
  left: 6px;
  margin-top: 14px;
  width: 6px;
  height: 6px;
  background: #ff3f3f;
  border-radius: 50%;
}

.noti-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sender-name {
  color: #00d2ff;
  font-weight: 600;
  font-size: 13px;
  margin-right: 6px;
}

.action-text {
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
}

.noti-time {
  color: rgba(255, 255, 255, 0.4);
  font-size: 11px;
}

.noti-empty {
  text-align: center;
  padding: 30px 0;
  color: rgba(255, 255, 255, 0.4);
  font-size: 13px;
}

</style>

<style>
/* ═══ 头像框样式 ═══ */
.header-avatar-frame {
  display: inline-flex;
  border-radius: 50%;
  padding: 3px;
  margin-right: 2px;
  transition: all 0.3s ease;
  flex-shrink: 0;
}
.header-avatar-frame .el-avatar {
  display: block;
  border-radius: 50%;
}

.header-avatar-frame.frame-default { background: #dcdfe6; }

.header-avatar-frame.frame-gold {
  background: linear-gradient(135deg, #f6d365, #fda085);
  box-shadow: 0 0 8px rgba(246, 211, 101, 0.5);
}
.header-avatar-frame.frame-gold .el-avatar { border: 2px solid #fff; }

.header-avatar-frame.frame-ocean {
  background: linear-gradient(135deg, #00d2ff, #165dff);
  box-shadow: 0 0 10px rgba(0, 210, 255, 0.5);
}
.header-avatar-frame.frame-ocean .el-avatar { border: 2px solid #fff; }

.header-avatar-frame.frame-rainbow {
  background: linear-gradient(90deg, #ff6b6b, #feca57, #48dbfb, #ff9ff3);
  background-size: 200% 100%;
  animation: header-rainbow-spin 3s linear infinite;
}
.header-avatar-frame.frame-rainbow .el-avatar { border: 2px solid #fff; }
@keyframes header-rainbow-spin {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}

.header-avatar-frame.frame-flame {
  background: linear-gradient(135deg, #ff4500, #ff8c00, #ffd700);
  box-shadow: 0 0 12px rgba(255, 69, 0, 0.5);
}
.header-avatar-frame.frame-flame .el-avatar { border: 2px solid #fff; }

/* ═══ 导航栏新头像框 ═══ */
.header-avatar-frame.frame-dashed {
  background: repeating-conic-gradient(#fd7000 0deg 18deg, transparent 18deg 36deg);
  box-shadow: 0 0 8px rgba(237, 35, 76, 0.45);
  animation: header-dash-glow 2s ease-in-out infinite;
}
.header-avatar-frame.frame-dashed .el-avatar { border: 2px solid #fff; }
@keyframes header-dash-glow {
  0%, 100% { box-shadow: 0 0 8px rgba(237, 35, 76, 0.45); }
  50% { box-shadow: 0 0 18px rgba(237, 35, 76, 0.75); }
}

.header-avatar-frame.frame-neon {
  background: linear-gradient(135deg, #00fff5, #ff00e4);
  box-shadow: 0 0 10px rgba(0, 255, 245, 0.55), 0 0 20px rgba(255, 0, 228, 0.3);
  animation: header-neon-pulse 3s ease-in-out infinite;
}
.header-avatar-frame.frame-neon .el-avatar { border: 2px solid rgba(255,255,255,0.9); }
@keyframes header-neon-pulse {
  0%, 100% { box-shadow: 0 0 10px rgba(0, 255, 245, 0.55), 0 0 20px rgba(255, 0, 228, 0.3); }
  50% { box-shadow: 0 0 18px rgba(0, 255, 245, 0.75), 0 0 32px rgba(255, 0, 228, 0.5); }
}

.header-avatar-frame.frame-aurora {
  background: linear-gradient(135deg, #00f260, #0575e6, #a855f7);
  background-size: 200% 200%;
  animation: header-aurora-shift 4s ease infinite;
  box-shadow: 0 0 10px rgba(5, 117, 230, 0.4);
}
.header-avatar-frame.frame-aurora .el-avatar { border: 2px solid rgba(255,255,255,0.9); }
@keyframes header-aurora-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.header-avatar-frame.frame-crystal {
  background: linear-gradient(135deg, #00f5ff, #ff00e5);
  box-shadow: 0 0 10px rgba(0, 245, 255, 0.4), 0 0 20px rgba(255, 0, 229, 0.2);
  animation: h-cr-breath 2s ease-in-out infinite;
}
.header-avatar-frame.frame-crystal .el-avatar {
  position: relative;
  z-index: 1;
  border: 2px solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
}
@keyframes h-cr-breath {
  0%, 100% { box-shadow: 0 0 10px rgba(0, 245, 255, 0.4), 0 0 20px rgba(255, 0, 229, 0.2); }
  50% { box-shadow: 0 0 18px rgba(0, 245, 255, 0.65), 0 0 36px rgba(255, 0, 229, 0.4); }
}

.header-avatar-frame.frame-royal {
  background: linear-gradient(135deg, #6c3cc7, #9b59b6, #f1c40f);
  box-shadow: 0 0 12px rgba(108, 60, 199, 0.5);
}
.header-avatar-frame.frame-royal .el-avatar { border: 2px solid #fff; }

.popover-avatar-frame {
  padding: 4px;
  border-radius: 50%;
  display: inline-flex;
}
.popover-avatar-frame .popover-avatar {
  border: 3px solid rgba(10, 18, 38, 0.9);
}

/* 面板头像框渐变（复用导航栏同款配色） */
.popover-avatar-frame.frame-default { background: #dcdfe6; }
.popover-avatar-frame.frame-gold {
  background: linear-gradient(135deg, #f6d365, #fda085);
  box-shadow: 0 0 12px rgba(246, 211, 101, 0.5);
}
.popover-avatar-frame.frame-ocean {
  background: linear-gradient(135deg, #00d2ff, #165dff);
  box-shadow: 0 0 12px rgba(0, 210, 255, 0.5);
}
.popover-avatar-frame.frame-rainbow {
  background: linear-gradient(90deg, #ff6b6b, #feca57, #48dbfb, #ff9ff3);
  background-size: 200% 100%;
  animation: header-rainbow-spin 3s linear infinite;
}
.popover-avatar-frame.frame-flame {
  background: linear-gradient(135deg, #ff4500, #ff8c00, #ffd700);
  box-shadow: 0 0 12px rgba(255, 69, 0, 0.5);
}

/* ═══ 下拉菜单大个头像框（新增款式） ═══ */
.popover-avatar-frame.frame-dashed {
  background: repeating-conic-gradient(#fd7000 0deg 18deg, transparent 18deg 36deg);
  box-shadow: 0 0 12px rgba(237, 35, 76, 0.45);
  animation: header-dash-glow 2s ease-in-out infinite;
}
.popover-avatar-frame.frame-neon {
  background: linear-gradient(135deg, #00fff5, #ff00e4);
  box-shadow: 0 0 14px rgba(0, 255, 245, 0.6), 0 0 28px rgba(255, 0, 228, 0.35);
  animation: header-neon-pulse 3s ease-in-out infinite;
}
.popover-avatar-frame.frame-aurora {
  background: linear-gradient(135deg, #00f260, #0575e6, #a855f7);
  background-size: 200% 200%;
  animation: header-aurora-shift 4s ease infinite;
  box-shadow: 0 0 16px rgba(5, 117, 230, 0.45);
}
.popover-avatar-frame.frame-crystal {
  background: linear-gradient(135deg, #00f5ff, #ff00e5);
  box-shadow: 0 0 14px rgba(0, 245, 255, 0.45), 0 0 28px rgba(255, 0, 229, 0.25);
  animation: p-cr-breath 2s ease-in-out infinite;
}
.popover-avatar-frame.frame-crystal .popover-avatar {
  position: relative;
  z-index: 1;
  border: 3px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.15);
}
@keyframes p-cr-breath {
  0%, 100% { box-shadow: 0 0 14px rgba(0, 245, 255, 0.45), 0 0 28px rgba(255, 0, 229, 0.25); }
  50% { box-shadow: 0 0 24px rgba(0, 245, 255, 0.7), 0 0 48px rgba(255, 0, 229, 0.45); }
}
.popover-avatar-frame.frame-royal {
  background: linear-gradient(135deg, #6c3cc7, #9b59b6, #f1c40f);
  box-shadow: 0 0 16px rgba(108, 60, 199, 0.55);
}
</style>
