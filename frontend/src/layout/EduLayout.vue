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
          <!-- RAG 智能问答按钮 -->
          <AgentFloatWidget
              v-if="authStore.isLoggedIn"
              @click="openRagChat"
          />

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
      <span>© 2026 海洋学堂 · 探索蔚蓝世界</span>
    </el-footer>

    <!-- RAG 智能问答浮窗 -->
    <RagChatWindow ref="ragChatRef" />
  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import * as THREE from "three";
import { useAuthStore } from "@/store/auth";
import { ElMessageBox, ElMessage } from "element-plus";
import { ArrowDown, ChatDotRound, Setting, Ship, SwitchButton, User } from "@element-plus/icons-vue";
import RagChatWindow from "@/components/RagChatWindow.vue";
import AgentFloatWidget from "@/components/AgentFloatWidget.vue";

const route = useRoute();
const $router = useRouter();
const authStore = useAuthStore();
const ragChatRef = ref(null);
const threeCanvas = ref(null);

// ── Three.js 粒子背景 ──────────────────────────────────────────────
/** 粒子数量响应式分级（按屏幕宽度） */
const getParticleCount = () => {
  const w = window.innerWidth;
  if (w < 768) return 600;   // 移动端
  if (w < 1024) return 900;  // 平板
  return 1200;               // 桌面端
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

/** 生成软光晕纹理（CanvasTexture） */
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
  gradient.addColorStop(0.12, "rgba(255,255,255,0.85)");
  gradient.addColorStop(0.35, "rgba(255,255,255,0.35)");
  gradient.addColorStop(0.65, "rgba(255,255,255,0.06)");
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

    // 颜色：主色 #165dff ↔ 辅色 #36cfc9 之间随机混合，加微量扰动
    const t = Math.random();
    const color = new THREE.Color().copy(COLOR_PRIMARY).lerp(COLOR_AUX, t);
    color.r += (Math.random() - 0.5) * 0.08;
    color.g += (Math.random() - 0.5) * 0.08;
    color.b += (Math.random() - 0.5) * 0.08;
    colors[i * 3] = Math.max(0, Math.min(1, color.r));
    colors[i * 3 + 1] = Math.max(0, Math.min(1, color.g));
    colors[i * 3 + 2] = Math.max(0, Math.min(1, color.b));

    // 大小：大部分为微小的浮游颗粒，少数较大（模拟远近层次）
    sizes[i] = Math.random() < 0.85
      ? Math.random() * 0.22 + 0.06    // 微小颗粒
      : Math.random() * 0.55 + 0.2;    // 少数大颗粒

    speeds[i] = Math.random() * 0.25 + 0.08;          // 上升速度
    phases[i] = Math.random() * Math.PI * 2;           // 漂移相位
    driftAmps[i] = Math.random() * 0.35 + 0.1;         // 漂移幅度
  }

  geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
  geometry.setAttribute("color", new THREE.BufferAttribute(colors, 3));
  geometry.setAttribute("size", new THREE.BufferAttribute(sizes, 1));

  // 材质：光晕纹理 + 半透明混合，与浅色背景自然融合
  const material = new THREE.PointsMaterial({
    size: 0.45,
    map: glowTexture,
    vertexColors: true,
    depthWrite: false,
    depthTest: false,
    transparent: true,
    opacity: 0.45,
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
    const t = Math.random();
    const color = new THREE.Color().copy(COLOR_PRIMARY).lerp(COLOR_AUX, t);
    color.r += (Math.random() - 0.5) * 0.08;
    color.g += (Math.random() - 0.5) * 0.08;
    color.b += (Math.random() - 0.5) * 0.08;
    colors[i * 3] = Math.max(0, Math.min(1, color.r));
    colors[i * 3 + 1] = Math.max(0, Math.min(1, color.g));
    colors[i * 3 + 2] = Math.max(0, Math.min(1, color.b));
    sizes[i] = Math.random() < 0.85
      ? Math.random() * 0.22 + 0.06
      : Math.random() * 0.55 + 0.2;
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
/* ── Three.js 粒子画布 ── */
.particle-canvas {
  position: fixed;
  inset: 0;
  z-index: -1;          /* 与 ambient-background 同级，DOM 在其后 → 渲染在上 */
  pointer-events: none; /* 不拦截任何页面交互 */
}

.edu-layout {
  --theme-klein-blue: #0052d9;
  --theme-klein-blue-light: #6aa1ff;
  --theme-text-primary: #303133;
  --theme-text-muted: #909399;
  --theme-success: #67c23a;
  --theme-coral: #f56c6c;
  /* -------------------------- */

  min-height: 100vh;
  background: transparent;
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

.edu-main {
  width: 100%;
  box-sizing: border-box;
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 64px - 48px);
  background: transparent;
}

.edu-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8892a4;
  font-size: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  background: transparent;
}
.edu-layout {
  min-height: 100vh;
  background: transparent;
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

/* ── RAG 按钮 ── */
.rag-btn {
  margin-right: 8px;
  border-radius: 8px;
  font-weight: 500;
  background: linear-gradient(135deg, rgba(0, 210, 255, 0.08), rgba(58, 123, 213, 0.06));
  border: 1px solid rgba(0, 130, 200, 0.2);
  color: var(--theme-primary, #0052d9);
  transition: all 0.25s ease;
}
.rag-btn:hover {
  background: linear-gradient(135deg, var(--theme-primary, #0052d9), #3a7bd5);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 4px 14px rgba(0, 82, 217, 0.3);
}

/* ── User ── */
.user-area {
  flex-shrink: 0;
  display: flex;
  align-items: center;
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
