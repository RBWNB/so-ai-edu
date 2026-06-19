<template>
  <div class="login-page">
    <div class="ambient-background">
      <div class="ambient-glow glow-blue"></div>
      <div class="ambient-glow glow-seafoam"></div>
      <div class="caustics-layer"></div>
      <div class="topography-bg"></div>
    </div>
    <div class="login-container">
      <div class="glass-panel">
        <!-- 左侧：品牌展示 -->
        <div class="panel-left brand-section" ref="brandSectionRef">
          <canvas ref="waveCanvas" class="wave-canvas"></canvas>
          <div class="brand-content">
            <!-- 🐋 Q 版简笔平涂蓝鲸 Logo -->
            <svg class="whale-logo" viewBox="0 0 96 64" width="96" height="64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <!-- 尾鳍（两叶圆润片状） -->
              <path d="M22 34 C14 28, 6 30, 4 34 C10 34, 16 34, 22 34"
                    fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M22 34 C14 40, 6 38, 4 34 C10 34, 16 34, 22 34"
                    fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <!-- 身体（圆滚滚短胖纺锤形） -->
              <path d="M22 34
                       C22 20, 36 12, 56 14
                       C72 16, 82 20, 86 30
                       C88 38, 84 42, 80 40
                       C74 38, 68 44, 56 46
                       C42 49, 26 46, 22 38 Z"
                    fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linejoin="round"/>
              <!-- 腹部平涂（浅蓝，无描边，贴合身体下缘） -->
              <path d="M24 36
                       C32 45, 46 48, 58 45
                       C68 43, 76 38, 80 38
                       C76 42, 68 45, 56 46
                       C42 48, 30 45, 24 36 Z"
                    fill="#e0edff"/>
              <!-- 胸鳍（圆润片状，从腹部伸出） -->
              <path d="M44 42 C38 48, 30 54, 28 54 C32 50, 38 44, 44 42"
                    fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <!-- 背鳍（小圆钝三角形，身体后1/3处） -->
              <path d="M48 17 C48 8, 56 8, 56 19"
                    fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <!-- 左眼 -->
              <circle cx="65" cy="26" r="5.5" fill="#fafcff" stroke="#1a2b5c" stroke-width="1.8"/>
              <circle class="pupil-left" cx="65" cy="26" r="2.5" fill="#1a2b5c"/>
              <circle cx="63" cy="24" r="1.4" fill="#fff"/>
              <path class="blink-line blink-left" d="M 61 26 Q 65 21.5, 69 26"
                    fill="none" stroke="#1a2b5c" stroke-width="2.2" stroke-linecap="round" opacity="0"/>
              <!-- 右眼 -->
              <circle cx="75" cy="26" r="5.5" fill="#fafcff" stroke="#1a2b5c" stroke-width="1.8"/>
              <circle class="pupil-right" cx="75" cy="26" r="2.5" fill="#1a2b5c"/>
              <circle cx="73" cy="24" r="1.4" fill="#fff"/>
              <path class="blink-line blink-right" d="M 71 26 Q 75 21.5, 79 26"
                    fill="none" stroke="#1a2b5c" stroke-width="2.2" stroke-linecap="round" opacity="0"/>
              <!-- 腮红 -->
              <circle cx="59" cy="32" r="3.2" fill="#ffd6e0" opacity="0.6"/>
              <circle cx="81" cy="32" r="3.2" fill="#ffd6e0" opacity="0.6"/>
              <!-- 微笑 -->
              <path d="M68 34 Q71 37, 74 34" fill="none" stroke="#1a2b5c" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
            <h1 class="brand-title">海洋学堂<br>智能教育平台</h1>
            <p class="brand-slogan">探索蔚蓝世界，学习海洋知识</p>
          </div>
        </div>
        <!-- 右侧：操作区 -->
        <div class="panel-right auth-section">
          <el-tabs v-model="activeTab" class="auth-tabs" @tab-change="handleTabChange">
            <el-tab-pane label="登录" name="login">
              <Transition name="auth-fade" mode="out-in">
                <el-form
                    key="login-form"
                    ref="loginFormRef"
                    :model="loginForm"
                    :rules="loginRules"
                    label-position="top"
                    @keyup.enter="handleEnterLogin"
                    class="auth-form login-form"
                >
                  <el-form-item label="账号" prop="username">
                    <el-input v-model="loginForm.username" placeholder="请输入账号" />
                  </el-form-item>
                  <el-form-item label="密码" prop="password">
                    <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
                  </el-form-item>
                  <div class="slider-captcha" :class="{ 'is-verified': isVerified }">
                    <div class="slider-track" ref="sliderTrackRef">
                      <div class="slider-bg" :style="{ width: (sliderLeft + 46) + 'px' }"></div>
                      <div class="slider-text">{{ isVerified ? '验证通过' : '请按住滑块，拖动到最右边' }}</div>
                      <div
                          class="slider-thumb"
                          :style="{ left: sliderLeft + 'px' }"
                          @mousedown="startDrag"
                          @touchstart.passive="startDrag"
                      >
                        <el-icon v-if="!isVerified"><Right /></el-icon>
                        <el-icon v-else><Select /></el-icon>
                      </div>
                    </div>
                  </div>
                  <el-button type="primary" class="auth-btn" :loading="loading" :disabled="!isVerified" @click="handleLogin">
                    登 录
                  </el-button>
                </el-form>
              </Transition>
            </el-tab-pane>
            <el-tab-pane label="注册" name="register">
              <Transition name="auth-fade" mode="out-in">
                <div key="register-container" class="register-container">
                  <!-- 步骤指示器（海洋主题风格） -->
                  <div class="step-indicator">
                    <div
                        v-for="(step, index) in registerSteps"
                        :key="index"
                        class="step-item"
                        :class="{ active: currentStep === index+1, completed: currentStep > index+1 }"
                    >
                      <span class="step-number">{{ currentStep > index+1 ? '✓' : index+1 }}</span>
                      <span class="step-label">{{ step.label }}</span>
                    </div>
                    <div class="step-line" :style="{ width: (currentStep-1)*50 + '%' }"></div>
                  </div>
                  <!-- 步骤1：账号信息（与登录表单高度一致） -->
                  <Transition name="step-slide" mode="out-in">
                    <div v-if="currentStep === 1" key="step1" class="step-content">
                      <el-form ref="registerFormRef" :model="registerForm" :rules="registerStep1Rules" label-position="top">
                        <el-form-item label="账号" prop="username">
                          <el-input v-model="registerForm.username" placeholder="请输入账号" />
                        </el-form-item>
                        <el-form-item label="邮箱" prop="email">
                          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
                        </el-form-item>
                        <el-form-item label="密码" prop="password">
                          <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" />
                        </el-form-item>
                      </el-form>
                    </div>
                    <!-- 步骤2：个人信息 -->
                    <div v-else key="step2" class="step-content">
                      <el-form ref="registerFormRef" :model="registerForm" :rules="registerStep2Rules" label-position="top">
                        <el-form-item label="用户名" prop="realName">
                          <el-input v-model="registerForm.realName" placeholder="请输入用户名" />
                        </el-form-item>
                        <el-form-item label="手机号" prop="phone">
                          <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
                        </el-form-item>
                        <el-form-item label="注册意向角色" prop="applyRole">
                          <el-select v-model="registerForm.applyRole" placeholder="请选择角色" style="width:100%">
                            <el-option label="访客 (VISITOR)" value="VISITOR" />
                          </el-select>
                        </el-form-item>
                        <el-form-item label="确认密码" prop="confirmPassword" class="confirm-password-item">
                          <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" />
                        </el-form-item>
                      </el-form>
                    </div>
                  </Transition>
                  <!-- 操作按钮组 -->
                  <div class="step-buttons">
                    <el-button
                        v-if="currentStep > 1"
                        class="back-btn"
                        @click="prevStep"
                        :disabled="loading"
                    >
                      上一步
                    </el-button>
                    <el-button
                        type="primary"
                        class="auth-btn step-btn"
                        :loading="loading"
                        @click="currentStep === 1 ? nextStep() : handleRegister()"
                    >
                      {{ currentStep === 1 ? '下一步' : '完成注册' }}
                    </el-button>
                  </div>
                </div>
              </Transition>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { Right, Select, Reading } from "@element-plus/icons-vue";
import * as THREE from "three";
import KnowledgeBase from "@/views/admin/knowledge/Index.vue";
import { useAuthStore } from "@/store/auth";
import { loginApi, registerApi } from "@/api/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const activeTab = ref("login");
const loading = ref(false);
const knowledgeVisible = ref(false);
const waveCanvas = ref(null);
const brandSectionRef = ref(null);

// ── Three.js 波浪背景 ─────────────────────────────────────────────
let waveRenderer = null;
let waveScene = null;
let waveCamera = null;
let wavePlane = null;
let waveAnimationId = null;
let waveBasePositions = null;   // 顶点初始位置快照（用于动画计算）
let waveHoverIntensity = 0;     // 当前 hover 强度
let waveTargetHover = 0;        // 目标 hover 强度

const WAVE_BREAKPOINT = 768;    // 小于此宽度关闭 Three.js
const WAVE_PERIOD = 8;          // 完整波浪周期（秒）
const WAVE_HOVER_BOOST = 0.5;   // hover 时振幅增强比例

/** WebGL 可用性检测 */
const checkWebGL = () => {
  try {
    const c = document.createElement("canvas");
    const gl = c.getContext("webgl") || c.getContext("experimental-webgl");
    return !!gl;
  } catch {
    return false;
  }
};

/** 帧率自适应：无交互 3s 后降至 30fps */
let waveLastInteraction = performance.now();
const WAVE_IDLE_TIMEOUT = 3000;
let waveIdleMode = false;
let waveFrameSkip = 0;

const waveMarkInteraction = () => {
  waveLastInteraction = performance.now();
  waveIdleMode = false;
  waveFrameSkip = 0;
};

/** 初始化波浪画布 */
const initWave = () => {
  if (!checkWebGL()) {
    console.warn("[波浪背景] WebGL 不可用，降级为 CSS 背景");
    return;
  }
  const brand = brandSectionRef.value;
  const canvas = waveCanvas.value;
  if (!brand || !canvas) return;
  if (window.innerWidth < WAVE_BREAKPOINT) return;

  const rect = brand.getBoundingClientRect();
  const w = rect.width;
  const h = rect.height;
  if (w <= 0 || h <= 0) return;

  // ── 场景 / 相机 ──
  waveScene = new THREE.Scene();

  // 透视相机：轻微俯角，让 Z 轴波浪可见
  waveCamera = new THREE.PerspectiveCamera(40, w / h, 0.1, 20);
  waveCamera.position.set(0, 0, 5.5);
  waveCamera.lookAt(0, 0, 0);

  // ── 渲染器 ──
  waveRenderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
  waveRenderer.setSize(w, h);
  waveRenderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  waveRenderer.setClearColor(0x000000, 0);

  // ── 平面几何体（分段数按设备性能分级） ──
  // 可见半高 = tan(fov/2) * distance = tan(20°) * 5.5 ≈ 2.0
  const planeH = Math.tan((40 / 2) * (Math.PI / 180)) * 5.5 * 2; // ≈ 4.0
  const planeW = planeH * (w / h);
  // 响应式分段：移动端少 → 降低顶点数，桌面多 → 波浪更平滑
  const sw = window.innerWidth;
  const segDiv = sw < 768 ? 20 : sw < 1024 ? 16 : 12;
  const segW = Math.max(Math.floor(w / segDiv), 24);
  const segH = Math.max(Math.floor(h / segDiv), 24);

  const geometry = new THREE.PlaneGeometry(planeW, planeH, segW, segH);

  // 保存初始位置（Z 始终为 0，只存 X/Y 用于动画计算）
  const posArr = geometry.attributes.position.array;
  waveBasePositions = new Float32Array(posArr.length);
  waveBasePositions.set(posArr);

  // ── 着色器材质：渐变 + 顶点动画 ──
  const material = new THREE.ShaderMaterial({
    uniforms: {
      uColor1: { value: new THREE.Color("#165dff") },
      uColor2: { value: new THREE.Color("#36cfc9") },
      uOpacity: { value: 0.15 },
    },
    vertexShader: /* glsl */ `
      varying vec2 vUv;
      void main() {
        vUv = uv;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
      }
    `,
    fragmentShader: /* glsl */ `
      varying vec2 vUv;
      uniform vec3 uColor1;
      uniform vec3 uColor2;
      uniform float uOpacity;
      void main() {
        vec3 color = mix(uColor1, uColor2, vUv.y);
        gl_FragColor = vec4(color, uOpacity);
      }
    `,
    transparent: true,
    depthWrite: false,
    depthTest: false,
  });

  wavePlane = new THREE.Mesh(geometry, material);
  waveScene.add(wavePlane);

  // 绑定 hover 事件
  brand.addEventListener("mouseenter", onBrandEnter);
  brand.addEventListener("mouseleave", onBrandLeave);

  animateWave();
};

/** 波浪动画循环 */
const animateWave = () => {
  waveAnimationId = requestAnimationFrame(animateWave);

  // ── 帧率自适应：3s 无交互 → 降至 30fps ──
  const nowMs = performance.now();
  if (nowMs - waveLastInteraction > WAVE_IDLE_TIMEOUT) {
    waveIdleMode = true;
  }
  if (waveIdleMode) {
    waveFrameSkip++;
    if (waveFrameSkip % 2 !== 0) return; // 跳帧
  }

  if (!wavePlane || !waveRenderer || !waveCamera) return;

  // 缓动 hover 强度
  waveHoverIntensity += (waveTargetHover - waveHoverIntensity) * 0.04;

  const posArr = wavePlane.geometry.attributes.position.array;
  const time = performance.now() * 0.001;
  const omega = (2 * Math.PI) / WAVE_PERIOD; // 8s 周期
  const t = time * omega;
  const amp = 1 + waveHoverIntensity * WAVE_HOVER_BOOST; // hover 增强

  for (let i = 0; i < posArr.length; i += 3) {
    const bx = waveBasePositions[i];
    const by = waveBasePositions[i + 1];

    // 多层正弦叠加，模拟自然海面
    posArr[i + 2] = (
      Math.sin(bx * 2.2 + t) * 0.1 +
      Math.sin(by * 1.6 + t * 0.65) * 0.08 +
      Math.sin((bx + by) * 1.1 + t * 0.8) * 0.055 +
      Math.cos(bx * 0.7 - by * 0.9 + t * 0.45) * 0.04
    ) * amp;
  }

  wavePlane.geometry.attributes.position.needsUpdate = true;
  waveRenderer.render(waveScene, waveCamera);
};

/** 鼠标进入品牌区 → 增强波浪 */
const onBrandEnter = () => {
  waveMarkInteraction();
  waveTargetHover = 1;
};
const onBrandLeave = () => {
  waveMarkInteraction();
  waveTargetHover = 0;
};

/** 更新波浪画布尺寸 */
const resizeWave = () => {
  waveMarkInteraction();
  const brand = brandSectionRef.value;
  if (!brand || !waveRenderer || !waveCamera) return;

  // 跨过断点 → 销毁
  if (window.innerWidth < WAVE_BREAKPOINT) {
    destroyWave();
    return;
  }

  const w = brand.getBoundingClientRect().width;
  const h = brand.getBoundingClientRect().height;
  if (w <= 0 || h <= 0) return;

  waveCamera.aspect = w / h;
  waveCamera.updateProjectionMatrix();
  waveRenderer.setSize(w, h);
  waveRenderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

  // 重建几何体以匹配新尺寸（使用响应式分段数）
  if (wavePlane) {
    const planeH = Math.tan((40 / 2) * (Math.PI / 180)) * 5.5 * 2;
    const planeW = planeH * (w / h);
    const sw = window.innerWidth;
    const segDiv = sw < 768 ? 20 : sw < 1024 ? 16 : 12;
    const segW = Math.max(Math.floor(w / segDiv), 24);
    const segH = Math.max(Math.floor(h / segDiv), 24);

    const newGeo = new THREE.PlaneGeometry(planeW, planeH, segW, segH);
    waveBasePositions = new Float32Array(newGeo.attributes.position.array.length);
    waveBasePositions.set(newGeo.attributes.position.array);

    wavePlane.geometry.dispose();
    wavePlane.geometry = newGeo;
  }
};

/** 完整清理 */
const destroyWave = () => {
  if (waveAnimationId) {
    cancelAnimationFrame(waveAnimationId);
    waveAnimationId = null;
  }
  if (wavePlane) {
    if (wavePlane.geometry) wavePlane.geometry.dispose();
    if (wavePlane.material) wavePlane.material.dispose();
    wavePlane = null;
  }
  if (waveRenderer) {
    waveRenderer.dispose();
    try { waveRenderer.forceContextLoss(); } catch { /* ignore */ }
    waveRenderer = null;
  }
  if (waveScene) {
    waveScene.clear();
    waveScene = null;
  }
  waveCamera = null;
  waveBasePositions = null;

  // 解绑 hover
  const brand = brandSectionRef.value;
  if (brand) {
    brand.removeEventListener("mouseenter", onBrandEnter);
    brand.removeEventListener("mouseleave", onBrandLeave);
  }
};

onMounted(() => {
  initWave();
  window.addEventListener("resize", resizeWave);
});

onUnmounted(() => {
  window.removeEventListener("resize", resizeWave);
  destroyWave();
});
// ── Three.js 波浪背景 END ─────────────────────────────────────────

// ── Q 版蓝鲸瞳孔追踪 ───────────────────────────────────────────
let eyeTrackTargetX = 0;
let eyeTrackTargetY = 0;
let eyeTrackCurrentX = 0;
let eyeTrackCurrentY = 0;
let eyeAnimId = null;
let blinkTimer = null;          // 闭眼恢复定时器
const EYE_MAX_OFFSET = 1.8;
const EYE_LERP = 0.08;

/** 获取 login-page 容器并绑定页面级事件 */
const getLoginPage = () => document.querySelector(".login-page");

/** 全页面鼠标移动 → 瞳孔跟随（坐标系基于 .login-page） */
const onPageEyeMove = (e) => {
  const page = getLoginPage();
  if (!page) return;
  const rect = page.getBoundingClientRect();
  const nx = ((e.clientX - rect.left) / rect.width) * 2 - 1;
  const ny = ((e.clientY - rect.top) / rect.height) * 2 - 1;
  eyeTrackTargetX = nx * EYE_MAX_OFFSET;
  eyeTrackTargetY = ny * EYE_MAX_OFFSET;
};

/** 鼠标离开页面 → 瞳孔平滑复位 */
const onPageLeave = () => {
  eyeTrackTargetX = 0;
  eyeTrackTargetY = 0;
};

/** 点击页面任意位置 → 月牙形 ^ ^ 软萌眨眼 */
const blinkWhale = () => {
  const leftLine = document.querySelector(".blink-left");
  const rightLine = document.querySelector(".blink-right");
  const leftPupil = document.querySelector(".pupil-left");
  const rightPupil = document.querySelector(".pupil-right");
  if (!leftLine || !rightLine) return;

  // 清除上一次未完的恢复定时器
  if (blinkTimer) { clearTimeout(blinkTimer); blinkTimer = null; }

  // 闭合：月牙线条快速淡入(60ms) + 瞳孔/高光同步隐藏
  leftLine.setAttribute("opacity", "1");
  rightLine.setAttribute("opacity", "1");
  if (leftPupil) leftPupil.setAttribute("opacity", "0");
  if (rightPupil) rightPupil.setAttribute("opacity", "0");

  // 保持 100ms → 缓慢张开 150ms（共 250ms）
  blinkTimer = setTimeout(() => {
    if (leftLine) leftLine.setAttribute("opacity", "0");
    if (rightLine) rightLine.setAttribute("opacity", "0");
    if (leftPupil) leftPupil.setAttribute("opacity", "1");
    if (rightPupil) rightPupil.setAttribute("opacity", "1");
    blinkTimer = null;
  }, 250);
};

/** 瞳孔缓动动画循环 */
const animateEyes = () => {
  eyeAnimId = requestAnimationFrame(animateEyes);
  eyeTrackCurrentX += (eyeTrackTargetX - eyeTrackCurrentX) * EYE_LERP;
  eyeTrackCurrentY += (eyeTrackTargetY - eyeTrackCurrentY) * EYE_LERP;

  const leftPupil = document.querySelector(".pupil-left");
  const rightPupil = document.querySelector(".pupil-right");
  if (leftPupil) {
    leftPupil.setAttribute("cx", String(65 + eyeTrackCurrentX));
    leftPupil.setAttribute("cy", String(26 + eyeTrackCurrentY));
  }
  if (rightPupil) {
    rightPupil.setAttribute("cx", String(75 + eyeTrackCurrentX));
    rightPupil.setAttribute("cy", String(26 + eyeTrackCurrentY));
  }
};

onMounted(() => {
  const page = getLoginPage();
  if (page) {
    page.addEventListener("mousemove", onPageEyeMove);
    page.addEventListener("mouseleave", onPageLeave);
    page.addEventListener("click", blinkWhale);
  }
  animateEyes();
});

onUnmounted(() => {
  if (eyeAnimId) cancelAnimationFrame(eyeAnimId);
  if (blinkTimer) clearTimeout(blinkTimer);
  const page = getLoginPage();
  if (page) {
    page.removeEventListener("mousemove", onPageEyeMove);
    page.removeEventListener("mouseleave", onPageLeave);
    page.removeEventListener("click", blinkWhale);
  }
});
// ── Q 版蓝鲸瞳孔追踪 END ───────────────────────────────────────

// ═══ 滑块验证逻辑 ═══
const isVerified = ref(false);
const sliderLeft = ref(0);
const isDragging = ref(false);
const sliderTrackRef = ref(null);
let startX = 0;

const startDrag = (e) => {
  if (isVerified.value) return;
  isDragging.value = true;
  startX = e.type.includes('mouse') ? e.clientX : e.touches[0].clientX;
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
  document.addEventListener('touchmove', onDrag, { passive: false });
  document.addEventListener('touchend', stopDrag);
};
const onDrag = (e) => {
  if (!isDragging.value) return;
  if (e.type.includes('touch')) e.preventDefault();
  const currentX = e.type.includes('mouse') ? e.clientX : e.touches[0].clientX;
  let moveX = currentX - startX;
  const trackWidth = sliderTrackRef.value.offsetWidth;
  const thumbWidth = 46;
  const maxMove = trackWidth - thumbWidth;
  if (moveX < 0) moveX = 0;
  if (moveX >= maxMove) {
    moveX = maxMove;
    isVerified.value = true;
    stopDrag();
  }
  sliderLeft.value = moveX;
};
const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
  document.removeEventListener('touchmove', onDrag);
  document.removeEventListener('touchend', stopDrag);
  if (!isVerified.value) {
    sliderLeft.value = 0;
  }
};

// ========== 登录相关逻辑 ==========
const loginFormRef = ref(null);
const loginForm = reactive({ username: "", password: "" });
const loginRules = {
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

const handleLogin = async () => {
  if (!loginFormRef.value) return;
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const res = await loginApi(loginForm);
      authStore.setAuth(res.data.token, res.data.username, res.data.roles || [], res.data.avatarUrl || "");
      ElMessage.success("登录成功");
      // 优先回跳 redirect 参数，否则按角色跳转
      const redirect = route.query?.redirect;
      if (redirect && typeof redirect === "string" && redirect !== "/login") {
        await router.push(redirect);
      } else {
        const roles = res.data.roles || [];
        const isAdmin = roles.some((r) => ["ADMIN", "MANAGER"].includes(r));
        await router.push(isAdmin ? "/admin/dashboard" : "/home");
      }
    } catch (error) {
      ElMessage.error(error.response?.data || "登录失败，请稍后重试");
    } finally {
      loading.value = false;
    }
  });
};

// ========== 分步注册核心逻辑 ==========
const registerFormRef = ref(null);
const registerForm = reactive({
  username: "", realName: "", email: "", phone: "",
  password: "", confirmPassword: "", applyRole: "VISITOR",
});

// 密码一致性验证
const validateConfirmPassword = (rule, value, callback) => {
  callback(value !== registerForm.password ? new Error("两次输入的密码不一致") : undefined);
};
// 基础验证规则
const registerRules = {
  username: [{ required: true, message: "请输入账号", trigger: "blur" }],
  realName: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  email: [
    { required: true, message: "请输入邮箱", trigger: "blur" },
    { type: "email", message: "请输入正确的邮箱格式", trigger: ["blur", "change"] },
  ],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "请输入正确的手机号格式", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请再次输入密码", trigger: "blur" },
    { validator: validateConfirmPassword, trigger: "blur" },
  ],
};

// 当前步骤（默认值1，解决"一进来就是完成注册"的关键）
const currentStep = ref(1);
// 步骤配置
const registerSteps = [
  { label: "账号信息" },
  { label: "个人信息" }
];
// 分步验证规则
const registerStep1Rules = {
  username: registerRules.username,
  email: registerRules.email,
  password: registerRules.password,
};
const registerStep2Rules = {
  realName: registerRules.realName,
  phone: registerRules.phone,
  applyRole: [{ required: true, message: "请选择角色", trigger: "blur" }],
  confirmPassword: registerRules.confirmPassword,
};

// 下一步方法
const nextStep = async () => {
  if (!registerFormRef.value) return;
  try {
    const errorFields = await registerFormRef.value.validateField(['username', 'email', 'password']);
    const hasError = Object.keys(errorFields).some(key => errorFields[key]);
    if (!hasError) {
      currentStep.value = 2;
      registerFormRef.value.clearValidate(['realName', 'phone', 'applyRole', 'confirmPassword']);
    }
  } catch (error) {
    console.error('第一步验证失败:', error);
    ElMessage.warning('表单验证失败，请检查输入内容');
  }
};
// 上一步方法
const prevStep = () => {
  currentStep.value = 1;
  registerFormRef.value.clearValidate(['username', 'email', 'password']);
};
// 注册方法（修复步骤重置）
const handleRegister = async () => {
  if (!registerFormRef.value) return;
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await registerApi({
        username: registerForm.username, password: registerForm.password,
        realName: registerForm.realName, email: registerForm.email, phone: registerForm.phone,
        applyRole: registerForm.applyRole,
      });
      ElMessage.success("注册成功，请等待审核");
      currentStep.value = 1;
      setTimeout(() => {
        registerFormRef.value.resetFields();
      }, 300);
      activeTab.value = "login";
    } catch (error) {
      ElMessage.error(error.response?.data || "注册失败，请稍后重试");
    } finally {
      loading.value = false;
    }
  });
};

// 标签切换时重置步骤
const handleTabChange = (tabName) => {
  loading.value = false;
  isVerified.value = false;
  sliderLeft.value = 0;
  if (tabName === "register") {
    currentStep.value = 1;
    if (registerFormRef.value) {
      registerFormRef.value.clearValidate();
    }
  }
};
// 处理回车登录（增加滑块验证判断）
const handleEnterLogin = () => {
  if (!isVerified.value) {
    ElMessage.warning('请先完成滑块验证');
    return;
  }
  handleLogin();
};
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
      linear-gradient(180deg, rgba(255,255,255,0.76), rgba(240,247,255,0.95)),
      url("https://picsum.photos/id/1002/1600/1000") center/cover;
  overflow: hidden;
}
/* ═══ 动态环境背景 ═══ */
.ambient-background {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
}
.ambient-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  animation: breathe 15s infinite ease-in-out alternate;
}
/* 左下角：克莱因蓝 */
.glow-blue {
  width: 700px;
  height: 700px;
  background: radial-gradient(circle, rgba(22, 93, 255, 0.18) 0%, transparent 70%);
  bottom: -20%;
  left: -10%;
}
/* 右上角：海沫薄荷绿 */
.glow-seafoam {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(54, 207, 201, 0.16) 0%, transparent 70%);
  top: -15%;
  right: -5%;
  animation-delay: -7s;
}
@keyframes breathe {
  0%   { transform: translate(0, 0) scale(1); opacity: 0.6; }
  50%  { transform: translate(40px, -40px) scale(1.1); opacity: 1; }
  100% { transform: translate(-20px, 20px) scale(0.95); opacity: 0.8; }
}
.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 920px;
  padding: 0 20px;
}
.glass-panel {
  position: relative;
  z-index: 10;
  display: flex;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid var(--theme-border-light);
  box-shadow: 0 18px 60px rgba(22, 93, 255, 0.14);
  overflow: hidden;
  min-height: 580px;
}
.brand-section {
  flex: 5;
  background:
      linear-gradient(180deg, rgba(14, 66, 210, 0.08), rgba(22, 93, 255, 0.02)),
      #f8fbff;
  border-right: 1px solid var(--theme-border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 36px;
  position: relative;
  overflow: hidden; /* 裁剪波浪画布圆角 */
}
/* Three.js 波浪画布：铺满品牌区，位于内容之下 */
.wave-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none; /* 不拦截 hover 事件穿透到父容器 */
}
.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
}
/* Q 版蓝鲸 Logo */
.whale-logo {
  display: block;
  width: 112px;
  height: 74px;
  margin: 0 auto 24px auto;
  cursor: default;
  filter: drop-shadow(0 6px 14px rgba(22, 93, 255, 0.12));
  transition: transform 0.3s ease, opacity 0.3s ease;
  animation: whale-float 6s ease-in-out infinite;
}
.whale-logo:hover {
  transform: scale(1.03);
  opacity: 0.9;
}
/* 眨眼线条 + 瞳孔/高光：闭合60ms + 张开150ms */
.blink-line {
  transition: opacity 0.06s ease-in;
}
.pupil-left,
.pupil-right {
  transition: opacity 0.15s ease-out;
}
@keyframes whale-float {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-3px); }
}
.brand-title {
  font-size: 30px;
  font-weight: 700;
  color: var(--theme-primary-dark);
  line-height: 1.45;
  margin: 0 0 18px 0;
  letter-spacing: 2px;
}
.brand-slogan {
  font-size: 15px;
  color: var(--theme-text-secondary);
  letter-spacing: 1px;
}
/* ═══ 右侧操作区 ═══ */
.auth-section {
  flex: 4;
  padding: 40px 48px;
  background: #ffffff;
  min-height: 580px;
  display: flex;
  flex-direction: column;
}
.auth-tabs {
  --el-tabs-header-margin: 0 0 30px 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}
:deep(.el-tabs__content) {
  flex: 1;
  padding: 0 !important;
}
:deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(0, 0, 0, 0.06);
}
:deep(.el-tabs__item) {
  color: var(--theme-text-muted);
  font-size: 16px;
  transition: color 0.25s;
}
:deep(.el-tabs__item.is-active) {
  color: var(--theme-primary);
  font-weight: 600;
}
:deep(.el-tabs__active-bar) {
  background-color: var(--theme-primary);
}
:deep(.el-form-item__label) {
  color: var(--theme-text-secondary) !important;
  font-size: 13px;
  font-weight: 500;
}
:deep(.el-input__wrapper) {
  height: 40px;
}
:deep(.el-input__inner) {
  color: var(--theme-text-primary) !important;
  caret-color: var(--theme-primary);
}
:deep(.el-input__inner::placeholder) {
  color: var(--theme-text-muted);
}
.auth-form {
  min-height: 330px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.login-form {
  justify-content: center;
}
.auth-btn {
  width: 100%;
  margin-top: auto;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 6px;
  border: none !important;
  color: #fff !important;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  /* 默认点亮状态 */
  background: var(--theme-primary) !important;
  box-shadow: 0 4px 14px rgba(22, 93, 255, 0.28) !important;
}
/* 禁用态：暗色静默，暗示不可操作 */
.auth-btn.is-disabled,
.auth-btn:disabled {
  background: #c8cdd5 !important;
  box-shadow: none !important;
  color: rgba(255, 255, 255, 0.7) !important;
  cursor: not-allowed;
}
.auth-btn.is-disabled:hover,
.auth-btn:disabled:hover {
  background: #c8cdd5 !important;
  box-shadow: none !important;
  transform: none;
}
.auth-btn:not(.is-disabled):not(:disabled):hover {
  background: var(--theme-primary-light) !important;
  box-shadow: 0 6px 22px rgba(22, 93, 255, 0.35) !important;
  transform: translateY(-1px);
}
.auth-btn:not(.is-disabled):not(:disabled):active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(0, 47, 167, 0.2) !important;
}
/* ========== 分步注册样式 ========== */
.register-container {
  min-height: 380px;
  display: flex;
  flex-direction: column;
}
.step-indicator {
  position: relative;
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 0 10px;
}
.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 1;
}
.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f0f2f5;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
  transition: all 0.3s ease;
}
.step-item.active .step-number {
  background: var(--theme-primary);
  color: #fff;
  box-shadow: 0 4px 12px rgba(22, 93, 255, 0.3);
}
.step-item.completed .step-number {
  background: var(--theme-success, #67c23a);
  color: #fff;
}
.step-label {
  font-size: 12px;
  color: #909399;
  transition: color 0.3s ease;
}
.step-item.active .step-label {
  color: var(--theme-primary);
  font-weight: 500;
}
.step-line {
  position: absolute;
  top: 14px;
  left: 10%;
  width: 80%;
  height: 2px;
  background: #e4e7ed;
  z-index: 0;
  transition: width 0.3s ease;
}
.step-content {
  flex: 1;
  padding-bottom: 24px;
}
.step-buttons {
  display: flex;
  gap: 12px;
  margin-top: auto;
}
.back-btn {
  flex: 1;
  height: 46px;
  border-radius: 6px;
  border: 1px solid var(--theme-border-light);
  background: #fff;
  color: var(--theme-text-secondary);
  font-weight: 500;
  transition: all 0.25s ease;
}
.back-btn:hover {
  border-color: var(--theme-primary);
  color: var(--theme-primary);
}
.step-btn {
  flex: 2;
  margin-top: 0 !important;
}
.confirm-password-item {
  margin-bottom: 8px !important;
}
/* 过渡动画 */
.auth-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.auth-fade-enter-active {
  transition: all 0.3s ease;
}
.auth-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
.auth-fade-leave-active {
  transition: all 0.2s ease;
}
.step-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
.step-slide-enter-active {
  transition: all 0.3s ease;
}
.step-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
.step-slide-leave-active {
  transition: all 0.2s ease;
}
/* ═══ 滑块验证模块 ═══ */
.slider-captcha {
  margin-top: 8px;
  margin-bottom: 24px;
  width: 100%;
  height: 40px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.03);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
  transform: translateZ(0);
  user-select: none;
}
.slider-track {
  width: 100%;
  height: 100%;
  position: relative;
}
.slider-bg {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: var(--theme-primary-soft);
  transition: background 0.3s ease;
  border-radius: inherit;
}
.slider-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--theme-text-muted);
  z-index: 1;
  pointer-events: none;
  transition: color 0.3s ease;
}
.slider-thumb {
  position: absolute;
  left: 0;
  top: 0;
  width: 46px;
  height: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  z-index: 2;
  color: var(--theme-text-secondary);
  transition: color 0.3s ease, transform 0.1s ease;
}
.slider-thumb:active {
  cursor: grabbing;
}
/* 验证成功状态 */
.slider-captcha.is-verified .slider-bg {
  background: var(--theme-seafoam-light);
}
.slider-captcha.is-verified .slider-text {
  color: var(--theme-seafoam-hover);
  font-weight: 500;
}
.slider-captcha.is-verified .slider-thumb {
  color: var(--theme-seafoam-hover);
  cursor: default;
}
.knowledge-preview-btn:hover {
  background: var(--theme-primary);
  color: #fff;
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.3);
  transform: translateY(-1px);
}
/* ═══ 知识预览弹窗 ═══ */
.knowledge-dialog :deep(.el-dialog__body) {
  padding: 0 20px 20px;
  max-height: 82vh;
  overflow-y: auto;
}
/* ═══ 响应式 ═══ */
@media (max-width: 768px) {
  .glass-panel {
    flex-direction: column;
    min-height: auto;
  }
  .brand-section {
    padding: 32px 20px;
    border-right: none;
    border-bottom: 1px solid rgba(0, 47, 167, 0.06);
  }
  .brand-title {
    font-size: 22px;
  }
  .auth-section {
    padding: 28px 24px;
    min-height: auto;
  }
  .whale-logo {
    width: 74px;
    height: 49px;
    margin-bottom: 16px;
  }
  .auth-form {
    min-height: 320px;
  }
  .register-container {
    min-height: 320px;
  }
}
</style>
