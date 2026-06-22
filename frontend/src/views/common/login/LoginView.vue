<template>
  <div class="login-page">
    <!-- 动态深海背景 -->
    <div class="ambient-background">
      <div class="ambient-glow glow-blue"></div>
      <div class="ambient-glow glow-seafoam"></div>
      <div class="caustics-layer"></div>
      <div class="topography-bg"></div>
      <div class="bubble-glow bubble-1"></div>
      <div class="bubble-glow bubble-2"></div>
      <div class="bubble-glow bubble-3"></div>
    </div>

    <div class="login-container">
      <div class="glass-panel">
        <!-- 左侧：品牌展示 -->
        <div class="panel-left brand-section" ref="brandSectionRef">
          <canvas ref="waveCanvas" class="wave-canvas"></canvas>
          <div class="brand-content">
            <!-- 🐋 Q 版简笔平涂蓝鲸 Logo -->
            <svg class="whale-logo" viewBox="0 0 96 64" width="96" height="64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <!-- 尾鳍 -->
              <path d="M22 34 C14 28, 6 30, 4 34 C10 34, 16 34, 22 34"
                    fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M22 34 C14 40, 6 38, 4 34 C10 34, 16 34, 22 34"
                    fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <!-- 身体 -->
              <path d="M22 34 C22 20, 36 12, 56 14 C72 16, 82 20, 86 30 C88 38, 84 42, 80 40 C74 38, 68 44, 56 46 C42 49, 26 46, 22 38 Z"
                    fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linejoin="round"/>
              <!-- 腹部平涂 -->
              <path d="M24 36 C32 45, 46 48, 58 45 C68 43, 76 38, 80 38 C76 42, 68 45, 56 46 C42 48, 30 45, 24 36 Z"
                    fill="#e0edff"/>
              <!-- 胸鳍 -->
              <path d="M44 42 C38 48, 30 54, 28 54 C32 50, 38 44, 44 42"
                    fill="#165dff" stroke="#1a2b5c" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <!-- 背鳍 -->
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
            <h1 class="brand-title">海洋学堂</h1>
            <p class="brand-subtitle">智能教育平台</p>
            <p class="brand-slogan">探索蔚蓝世界，学习海洋知识</p>
            <div class="brand-features">
              <div class="feature-item">
                <span class="feature-dot"></span>
                <span>沉浸式海洋知识学习</span>
              </div>
              <div class="feature-item">
                <span class="feature-dot"></span>
                <span>AI 智能问答助手</span>
              </div>
              <div class="feature-item">
                <span class="feature-dot"></span>
                <span>趣味互动闯关评测</span>
              </div>
            </div>
          </div>
          <div class="brand-footer">
            <span class="copyright">© 2026 海洋学堂</span>
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
                  <div class="form-header">
                    <h2 class="form-title">欢迎回来</h2>
                    <p class="form-desc">请登录您的账号以继续</p>
                  </div>

                  <el-form-item label="账号" prop="username">
                    <el-input
                        v-model="loginForm.username"
                        placeholder="请输入账号"
                        :prefix-icon="User"
                    />
                  </el-form-item>
                  <el-form-item label="密码" prop="password">
                    <el-input
                        v-model="loginForm.password"
                        type="password"
                        show-password
                        placeholder="请输入密码"
                        :prefix-icon="Lock"
                    />
                  </el-form-item>

                  <div class="slider-captcha" :class="{ 'is-verified': isVerified }">
                    <div class="slider-track" ref="sliderTrackRef">
                      <div class="slider-bg" :style="{ width: (sliderLeft + 46) + 'px' }"></div>
                      <div class="slider-text">
                        <el-icon class="slider-icon"><Lock /></el-icon>
                        {{ isVerified ? '验证通过' : '请按住滑块拖动验证' }}
                      </div>
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

                  <el-button
                      type="primary"
                      class="auth-btn"
                      :loading="loading"
                      :disabled="!isVerified"
                      @click="handleLogin"
                  >
                    {{ loading ? '登录中...' : '登 录' }}
                  </el-button>
                </el-form>
              </Transition>
            </el-tab-pane>

            <el-tab-pane label="注册" name="register">
              <Transition name="auth-fade" mode="out-in">
                <div key="register-container" class="register-container">
                  <div class="form-header">
                    <h2 class="form-title">创建账号</h2>
                    <p class="form-desc">加入海洋学堂，开启学习之旅</p>
                  </div>

                  <!-- 步骤指示器 -->
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

                  <!-- 步骤1：账号信息 -->
                  <Transition name="step-slide" mode="out-in">
                    <div v-if="currentStep === 1" key="step1" class="step-content">
                      <el-form ref="registerFormRef" :model="registerForm" :rules="registerStep1Rules" label-position="top">
                        <el-form-item label="账号" prop="username">
                          <el-input v-model="registerForm.username" placeholder="请输入账号" :prefix-icon="User" />
                        </el-form-item>
                        <el-form-item label="邮箱" prop="email">
                          <el-input v-model="registerForm.email" placeholder="请输入邮箱" :prefix-icon="Message" />
                        </el-form-item>
                        <el-form-item label="密码" prop="password">
                          <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" />
                        </el-form-item>
                      </el-form>
                    </div>
                    <!-- 步骤2：个人信息 -->
                    <div v-else key="step2" class="step-content">
                      <el-form ref="registerFormRef" :model="registerForm" :rules="registerStep2Rules" label-position="top">
                        <el-form-item label="用户名" prop="realName">
                          <el-input v-model="registerForm.realName" placeholder="请输入用户名" :prefix-icon="UserFilled" />
                        </el-form-item>
                        <el-form-item label="手机号" prop="phone">
                          <el-input v-model="registerForm.phone" placeholder="请输入手机号" :prefix-icon="Iphone" />
                        </el-form-item>
                        <el-form-item label="注册意向角色" prop="applyRole">
                          <el-select v-model="registerForm.applyRole" placeholder="请选择角色" style="width:100%">
                            <el-option label="访客 (VISITOR)" value="VISITOR" />
                          </el-select>
                        </el-form-item>
                        <el-form-item label="确认密码" prop="confirmPassword" class="confirm-password-item">
                          <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="请再次输入密码" :prefix-icon="Lock" />
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
import {
  Right, Select, User, Lock,
  Message, UserFilled, Iphone
} from "@element-plus/icons-vue";
import * as THREE from "three";
import { useAuthStore } from "@/store/auth";
import { loginApi, registerApi } from "@/api/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const activeTab = ref("login");
const loading = ref(false);
const waveCanvas = ref(null);
const brandSectionRef = ref(null);

// ── Three.js 波浪背景 ─────────────────────────────────────────────
let waveRenderer = null;
let waveScene = null;
let waveCamera = null;
let wavePlane = null;
let waveAnimationId = null;
let waveBasePositions = null;
let waveHoverIntensity = 0;
let waveTargetHover = 0;

const WAVE_BREAKPOINT = 768;
const WAVE_PERIOD = 8;
const WAVE_HOVER_BOOST = 0.5;

const checkWebGL = () => {
  try {
    const c = document.createElement("canvas");
    const gl = c.getContext("webgl") || c.getContext("experimental-webgl");
    return !!gl;
  } catch {
    return false;
  }
};

let waveLastInteraction = performance.now();
const WAVE_IDLE_TIMEOUT = 3000;
let waveIdleMode = false;
let waveFrameSkip = 0;

const waveMarkInteraction = () => {
  waveLastInteraction = performance.now();
  waveIdleMode = false;
  waveFrameSkip = 0;
};

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

  waveScene = new THREE.Scene();
  waveCamera = new THREE.PerspectiveCamera(40, w / h, 0.1, 20);
  waveCamera.position.set(0, 0, 5.5);
  waveCamera.lookAt(0, 0, 0);

  waveRenderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true });
  waveRenderer.setSize(w, h);
  waveRenderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  waveRenderer.setClearColor(0x000000, 0);

  const planeH = Math.tan((40 / 2) * (Math.PI / 180)) * 5.5 * 2;
  const planeW = planeH * (w / h);
  const sw = window.innerWidth;
  const segDiv = sw < 768 ? 20 : sw < 1024 ? 16 : 12;
  const segW = Math.max(Math.floor(w / segDiv), 24);
  const segH = Math.max(Math.floor(h / segDiv), 24);

  const geometry = new THREE.PlaneGeometry(planeW, planeH, segW, segH);
  const posArr = geometry.attributes.position.array;
  waveBasePositions = new Float32Array(posArr.length);
  waveBasePositions.set(posArr);

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

  brand.addEventListener("mouseenter", onBrandEnter);
  brand.addEventListener("mouseleave", onBrandLeave);

  animateWave();
};

const animateWave = () => {
  waveAnimationId = requestAnimationFrame(animateWave);

  const nowMs = performance.now();
  if (nowMs - waveLastInteraction > WAVE_IDLE_TIMEOUT) {
    waveIdleMode = true;
  }
  if (waveIdleMode) {
    waveFrameSkip++;
    if (waveFrameSkip % 2 !== 0) return;
  }

  if (!wavePlane || !waveRenderer || !waveCamera) return;

  waveHoverIntensity += (waveTargetHover - waveHoverIntensity) * 0.04;

  const posArr = wavePlane.geometry.attributes.position.array;
  const time = performance.now() * 0.001;
  const omega = (2 * Math.PI) / WAVE_PERIOD;
  const t = time * omega;
  const amp = 1 + waveHoverIntensity * WAVE_HOVER_BOOST;

  for (let i = 0; i < posArr.length; i += 3) {
    const bx = waveBasePositions[i];
    const by = waveBasePositions[i + 1];
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

const onBrandEnter = () => {
  waveMarkInteraction();
  waveTargetHover = 1;
};
const onBrandLeave = () => {
  waveMarkInteraction();
  waveTargetHover = 0;
};

const resizeWave = () => {
  waveMarkInteraction();
  const brand = brandSectionRef.value;
  if (!brand || !waveRenderer || !waveCamera) return;

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
let blinkTimer = null;
const EYE_MAX_OFFSET = 1.8;
const EYE_LERP = 0.08;

const getLoginPage = () => document.querySelector(".login-page");

const onPageEyeMove = (e) => {
  const page = getLoginPage();
  if (!page) return;
  const rect = page.getBoundingClientRect();
  const nx = ((e.clientX - rect.left) / rect.width) * 2 - 1;
  const ny = ((e.clientY - rect.top) / rect.height) * 2 - 1;
  eyeTrackTargetX = nx * EYE_MAX_OFFSET;
  eyeTrackTargetY = ny * EYE_MAX_OFFSET;
};

const onPageLeave = () => {
  eyeTrackTargetX = 0;
  eyeTrackTargetY = 0;
};

const blinkWhale = () => {
  const leftLine = document.querySelector(".blink-left");
  const rightLine = document.querySelector(".blink-right");
  const leftPupil = document.querySelector(".pupil-left");
  const rightPupil = document.querySelector(".pupil-right");
  if (!leftLine || !rightLine) return;

  if (blinkTimer) { clearTimeout(blinkTimer); blinkTimer = null; }

  leftLine.setAttribute("opacity", "1");
  rightLine.setAttribute("opacity", "1");
  if (leftPupil) leftPupil.setAttribute("opacity", "0");
  if (rightPupil) rightPupil.setAttribute("opacity", "0");

  blinkTimer = setTimeout(() => {
    if (leftLine) leftLine.setAttribute("opacity", "0");
    if (rightLine) rightLine.setAttribute("opacity", "0");
    if (leftPupil) leftPupil.setAttribute("opacity", "1");
    if (rightPupil) rightPupil.setAttribute("opacity", "1");
    blinkTimer = null;
  }, 250);
};

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

const validateConfirmPassword = (rule, value, callback) => {
  callback(value !== registerForm.password ? new Error("两次输入的密码不一致") : undefined);
};
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

const currentStep = ref(1);
const registerSteps = [
  { label: "账号信息" },
  { label: "个人信息" }
];
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
const prevStep = () => {
  currentStep.value = 1;
  registerFormRef.value.clearValidate(['username', 'email', 'password']);
};
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
const handleEnterLogin = () => {
  if (!isVerified.value) {
    ElMessage.warning('请先完成滑块验证');
    return;
  }
  handleLogin();
};
</script>

<style scoped>
/* ═══════════════════════════════════════════════════
   登录页 — 深海主题 UI
   设计语言：通透、柔和、海洋蓝调
   ═══════════════════════════════════════════════════ */

.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(180deg, rgba(255,255,255,0.85), rgba(240,247,255,0.92)),
    radial-gradient(ellipse at 20% 50%, rgba(22,93,255,0.06) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 50%, rgba(54,207,201,0.05) 0%, transparent 60%);
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

.glow-blue {
  width: 700px;
  height: 700px;
  background: radial-gradient(circle, rgba(22, 93, 255, 0.18) 0%, transparent 70%);
  bottom: -20%;
  left: -10%;
}

.glow-seafoam {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(54, 207, 201, 0.16) 0%, transparent 70%);
  top: -15%;
  right: -5%;
  animation-delay: -7s;
}

/* 漂浮气泡光晕 */
.bubble-glow {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 70%);
  animation: bubbleFloat 20s infinite ease-in-out;
  pointer-events: none;
}
.bubble-1 {
  width: 200px; height: 200px;
  left: 15%; bottom: 10%;
  animation-delay: -5s;
  animation-duration: 25s;
}
.bubble-2 {
  width: 120px; height: 120px;
  right: 20%; top: 15%;
  animation-delay: -12s;
  animation-duration: 18s;
}
.bubble-3 {
  width: 80px; height: 80px;
  left: 45%; top: 25%;
  animation-delay: -3s;
  animation-duration: 22s;
}

@keyframes bubbleFloat {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.3; }
  25%  { transform: translate(30px, -40px) scale(1.2); opacity: 0.6; }
  50%  { transform: translate(-20px, -80px) scale(0.9); opacity: 0.4; }
  75%  { transform: translate(40px, -50px) scale(1.1); opacity: 0.5; }
}

@keyframes breathe {
  0%   { transform: translate(0, 0) scale(1); opacity: 0.5; }
  50%  { transform: translate(40px, -40px) scale(1.15); opacity: 0.9; }
  100% { transform: translate(-20px, 20px) scale(0.95); opacity: 0.7; }
}

/* 地形图 & 焦散层（复用全局样式） */
.topography-bg,
.caustics-layer {
  position: absolute;
  inset: -80px;
  opacity: 0.08;
}

.topography-bg {
  background-image: url("data:image/svg+xml,%3Csvg width='1000' height='300' viewBox='0 0 1000 300' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 90C150 170 350 10 500 90s350-80 500 0' fill='none' stroke='%23165DFF' stroke-width='1'/%3E%3Cpath d='M0 150C150 230 350 70 500 150s350-80 500 0' fill='none' stroke='%23165DFF' stroke-width='1'/%3E%3Cpath d='M0 210C150 290 350 130 500 210s350-80 500 0' fill='none' stroke='%23165DFF' stroke-width='1'/%3E%3C/svg%3E");
  background-size: 1000px 300px;
  animation: topoSlide 60s linear infinite;
}

@keyframes topoSlide {
  from { background-position: 0 0; }
  to { background-position: -1000px 0; }
}

.caustics-layer {
  background:
    linear-gradient(135deg, transparent 35%, rgba(255, 255, 255, 0.45) 45%, transparent 58%),
    linear-gradient(45deg, transparent 34%, rgba(22, 93, 255, 0.06) 50%, transparent 68%);
  background-size: 220% 220%;
  animation: caustics-anim 20s ease-in-out infinite alternate;
}

@keyframes caustics-anim {
  from { background-position: 0 0; }
  to { background-position: 100% 100%; }
}

/* ═══ 主面板 ═══ */
.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 960px;
  padding: 0 20px;
}

.glass-panel {
  position: relative;
  z-index: 10;
  display: flex;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow:
    0 24px 80px rgba(22, 93, 255, 0.10),
    0 8px 32px rgba(22, 93, 255, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  overflow: hidden;
  min-height: 580px;
  transition: box-shadow 0.4s ease;
}

.glass-panel:hover {
  box-shadow:
    0 28px 90px rgba(22, 93, 255, 0.13),
    0 8px 32px rgba(22, 93, 255, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

/* ═══ 左侧品牌区 ═══ */
.brand-section {
  flex: 5;
  background:
    linear-gradient(180deg, rgba(22, 93, 255, 0.06) 0%, rgba(22, 93, 255, 0.01) 100%),
    #f8fbff;
  border-right: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 36px 32px;
  position: relative;
  overflow: hidden;
}

.wave-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* Q 版蓝鲸 Logo */
.whale-logo {
  display: block;
  width: 100px;
  height: 66px;
  margin: 0 auto 20px auto;
  cursor: default;
  filter: drop-shadow(0 6px 14px rgba(22, 93, 255, 0.12));
  transition: transform 0.3s ease;
  animation: whale-float 6s ease-in-out infinite;
}

.whale-logo:hover {
  transform: scale(1.05);
}

.blink-line {
  transition: opacity 0.06s ease-in;
}
.pupil-left,
.pupil-right {
  transition: opacity 0.15s ease-out;
}

@keyframes whale-float {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-4px); }
}

.brand-title {
  font-size: 32px;
  font-weight: 800;
  color: var(--theme-primary-dark);
  line-height: 1.2;
  margin: 0 0 4px 0;
  letter-spacing: 3px;
}

.brand-subtitle {
  font-size: 16px;
  font-weight: 400;
  color: var(--theme-primary);
  letter-spacing: 8px;
  margin: 0 0 16px 0;
  opacity: 0.7;
}

.brand-slogan {
  font-size: 14px;
  color: var(--theme-text-muted);
  letter-spacing: 2px;
  margin: 0 0 28px 0;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--theme-text-secondary);
  transition: transform 0.2s ease;
}

.feature-item:hover {
  transform: translateX(4px);
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--theme-primary);
  opacity: 0.5;
  flex-shrink: 0;
}

.brand-footer {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 1;
}

.copyright {
  font-size: 11px;
  color: var(--theme-text-muted);
  opacity: 0.5;
  letter-spacing: 0.5px;
}

/* ═══ 右侧操作区 ═══ */
.auth-section {
  flex: 4;
  padding: 36px 44px;
  background: transparent;
  min-height: 580px;
  display: flex;
  flex-direction: column;
}

.auth-tabs {
  --el-tabs-header-margin: 0 0 24px 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

:deep(.el-tabs__header) {
  margin-bottom: 24px !important;
}

:deep(.el-tabs__content) {
  flex: 1;
  padding: 0 !important;
}

:deep(.el-tabs__nav-wrap::after) {
  background: none;
}

:deep(.el-tabs__item) {
  color: var(--theme-text-muted);
  font-size: 15px;
  font-weight: 500;
  padding: 0 20px;
  transition: color 0.25s;
  height: 40px;
  line-height: 40px;
}

:deep(.el-tabs__item.is-active) {
  color: var(--theme-primary);
  font-weight: 600;
}

:deep(.el-tabs__active-bar) {
  background-color: var(--theme-primary);
  height: 3px;
  border-radius: 3px;
}

/* ═══ 表单样式 ═══ */
.form-header {
  margin-bottom: 24px;
}

.form-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--theme-text-primary);
  margin: 0 0 6px 0;
  line-height: 1.3;
}

.form-desc {
  font-size: 14px;
  color: var(--theme-text-muted);
  margin: 0;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-form-item__label) {
  color: var(--theme-text-secondary) !important;
  font-size: 13px;
  font-weight: 500;
  padding-bottom: 6px !important;
  line-height: 1.4;
}

:deep(.el-input__wrapper) {
  height: 44px;
  border-radius: 10px !important;
  background: #f7f9fc !important;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.06) inset !important;
  padding: 0 12px !important;
  transition: all 0.25s ease !important;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(22, 93, 255, 0.3) inset !important;
  background: #f5f8ff !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px var(--theme-primary) inset !important;
  background: #ffffff !important;
}

:deep(.el-input__inner) {
  color: var(--theme-text-primary) !important;
  caret-color: var(--theme-primary);
  font-size: 14px;
}

:deep(.el-input__inner::placeholder) {
  color: #b0b8c8;
  font-size: 13px;
}

:deep(.el-input__prefix) {
  margin-right: 8px;
}

:deep(.el-input__prefix-inner) .el-icon {
  font-size: 16px;
  color: #b0b8c8;
  transition: color 0.25s ease;
}

:deep(.is-focus .el-input__prefix-inner) .el-icon {
  color: var(--theme-primary);
}

.auth-form {
  min-height: 330px;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.login-form {
  justify-content: flex-start;
}

/* ═══ 滑块验证模块（重新设计） ═══ */
.slider-captcha {
  margin-top: 4px;
  margin-bottom: 20px;
  width: 100%;
  height: 44px;
  border-radius: 10px;
  background: #f7f9fc;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.06) inset;
  position: relative;
  overflow: hidden;
  transform: translateZ(0);
  user-select: none;
  transition: box-shadow 0.3s ease, background 0.3s ease;
}

.slider-captcha:hover {
  box-shadow: 0 0 0 1px rgba(22, 93, 255, 0.2) inset;
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
  background: linear-gradient(90deg, var(--theme-primary-soft), rgba(54, 207, 201, 0.15));
  transition: background 0.4s ease;
  border-radius: inherit;
}

.slider-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: #b0b8c8;
  z-index: 1;
  pointer-events: none;
  transition: color 0.3s ease;
}

.slider-icon {
  font-size: 14px;
}

.slider-thumb {
  position: absolute;
  left: 0;
  top: 2px;
  width: 40px;
  height: 40px;
  background: #fff;
  border-radius: 9px;
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.10),
    0 1px 2px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  z-index: 2;
  color: var(--theme-text-secondary);
  transition: color 0.3s ease, transform 0.1s ease, box-shadow 0.3s ease;
}

.slider-thumb:active {
  cursor: grabbing;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.15);
}

.slider-thumb:hover {
  box-shadow:
    0 3px 12px rgba(0, 0, 0, 0.12),
    0 1px 2px rgba(0, 0, 0, 0.06);
}

/* 验证成功状态 */
.slider-captcha.is-verified {
  background: rgba(54, 207, 201, 0.06);
  box-shadow: 0 0 0 1px rgba(54, 207, 201, 0.25) inset;
}

.slider-captcha.is-verified .slider-bg {
  background: linear-gradient(90deg, rgba(54, 207, 201, 0.20), rgba(54, 207, 201, 0.08));
}

.slider-captcha.is-verified .slider-text {
  color: var(--theme-seafoam-hover);
  font-weight: 500;
}

.slider-captcha.is-verified .slider-thumb {
  color: var(--theme-seafoam-hover);
  cursor: default;
  background: #f0fffe;
}

/* ═══ 登录按钮 ═══ */
.auth-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 10px;
  border: none !important;
  color: #fff !important;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--theme-primary) 0%, var(--theme-primary-light) 100%) !important;
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.30) !important;
  margin-top: 4px;
}

.auth-btn.is-disabled,
.auth-btn:disabled {
  background: #d0d5de !important;
  box-shadow: none !important;
  color: rgba(255, 255, 255, 0.7) !important;
  cursor: not-allowed;
}

.auth-btn.is-disabled:hover,
.auth-btn:disabled:hover {
  background: #d0d5de !important;
  box-shadow: none !important;
  transform: none;
}

.auth-btn:not(.is-disabled):not(:disabled):hover {
  background: linear-gradient(135deg, var(--theme-primary-light) 0%, #5a92ff 100%) !important;
  box-shadow: 0 8px 28px rgba(22, 93, 255, 0.40) !important;
  transform: translateY(-2px);
}

.auth-btn:not(.is-disabled):not(:disabled):active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(22, 93, 255, 0.25) !important;
}

/* ═══ 分步注册 ═══ */
.register-container {
  min-height: 380px;
  display: flex;
  flex-direction: column;
}

.step-indicator {
  position: relative;
  display: flex;
  justify-content: space-between;
  margin-bottom: 28px;
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
  font-size: 13px;
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
  padding-bottom: 20px;
}

.step-buttons {
  display: flex;
  gap: 12px;
  margin-top: auto;
}

.back-btn {
  flex: 1;
  height: 48px;
  border-radius: 10px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
  color: var(--theme-text-secondary);
  font-weight: 500;
  transition: all 0.25s ease;
}

.back-btn:hover {
  border-color: var(--theme-primary);
  color: var(--theme-primary);
  background: #f8fbff;
}

.step-btn {
  flex: 2;
  margin-top: 0 !important;
}

.confirm-password-item {
  margin-bottom: 8px !important;
}

/* ═══ 过渡动画 ═══ */
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

/* ═══ 响应式 ═══ */
@media (max-width: 768px) {
  .glass-panel {
    flex-direction: column;
    min-height: auto;
    border-radius: 16px;
  }

  .brand-section {
    padding: 28px 20px 20px;
    border-right: none;
    border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  }

  .brand-title {
    font-size: 24px;
  }

  .brand-subtitle {
    font-size: 14px;
    letter-spacing: 6px;
  }

  .brand-features {
    display: none;
  }

  .brand-footer {
    position: static;
    margin-top: 16px;
  }

  .auth-section {
    padding: 24px 20px;
    min-height: auto;
  }

  .whale-logo {
    width: 72px;
    height: 48px;
    margin-bottom: 14px;
  }

  .auth-form {
    min-height: 300px;
  }

  .register-container {
    min-height: 300px;
  }

  .form-title {
    font-size: 20px;
  }
}

/* 超小屏额外适配 */
@media (max-width: 420px) {
  .glass-panel {
    border-radius: 12px;
  }

  .auth-section {
    padding: 20px 16px;
  }

  :deep(.el-input__wrapper) {
    height: 40px;
  }

  .auth-btn {
    height: 44px;
    font-size: 15px;
  }

  .slider-captcha {
    height: 40px;
  }

  .slider-thumb {
    width: 36px;
    height: 36px;
    top: 2px;
  }
}
</style>
