<template>
  <div
      class="login-page"
      @mousemove="handleMouseMove"
      @click="handlePageClick"
  >
    <!-- 海洋背景 Canvas -->
    <canvas ref="waveCanvas" class="wave-canvas" />

    <!-- 丁达尔效应光束（从左上角射出） -->
    <div class="tyndall-container">
      <div class="tyndall-beam beam-t1"></div>
      <div class="tyndall-beam beam-t2"></div>
      <div class="tyndall-beam beam-t3"></div>
      <div class="tyndall-beam beam-t4"></div>
      <div class="tyndall-beam beam-t5"></div>
      <div class="tyndall-glow"></div>
    </div>

    <!-- 动态光斑（Caustics） -->
    <div class="caustics-layer"></div>

    <!-- 动态渐变背景（呼吸光晕） -->
    <div class="gradient-bg"></div>

    <!-- 水草（80簇） -->
    <div class="seaweed-container">
      <div
          v-for="weed in seaweeds"
          :key="'weed-' + weed.id"
          class="seaweed"
          :style="weed.style"
      ></div>
    </div>

    <!-- 鱼群 -->
    <div class="fish-container">
      <div
          v-for="fish in fishes"
          :key="'fish-' + fish.id"
          class="fish"
          :style="fish.style"
      >
        <div class="fish-body"></div>
        <div class="fish-tail"></div>
        <div class="fish-eye"></div>
      </div>
    </div>

    <!-- 水母 -->
    <div class="jellyfish-container">
      <div
          v-for="jelly in jellyfishes"
          :key="'jelly-' + jelly.id"
          class="jellyfish"
          :style="jelly.style"
      >
        <div class="jelly-bell"></div>
        <div class="jelly-tentacles"></div>
      </div>
    </div>

    <!-- 浮动几何图形 -->
    <div class="floating-shapes">
      <div
          v-for="shape in floatingShapes"
          :key="'shape-' + shape.id"
          class="shape"
          :style="shape.style"
      ></div>
    </div>

    <!-- 闪烁星点 -->
    <div class="sparkles">
      <div
          v-for="star in stars"
          :key="'star-' + star.id"
          class="star"
          :style="star.style"
      ></div>
    </div>

    <!-- 原有光束 -->
    <div class="light-beam beam-1"></div>
    <div class="light-beam beam-2"></div>
    <div class="light-beam beam-3"></div>
    <div class="light-beam beam-4"></div>

    <!-- 粒子 -->
    <div
        v-for="particle in particles"
        :key="'particle-' + particle.id"
        class="particle"
        :style="particle.style"
    />

    <!-- 气泡容器（纯 DOM 管理） -->
    <div class="bubble-container" ref="bubbleContainerRef"></div>

    <!-- 鲸鱼 -->
    <div
        class="whale-wrapper"
        :class="{ jump: whaleJump }"
        :style="whaleStyle"
        @click.stop="triggerBlink"
    >
      <svg viewBox="0 0 400 260" class="whale-svg">
        <!-- 水柱 -->
        <path
            d="M135 40 Q130 15 140 5 Q145 0 150 5 Q158 12 152 30 Q155 28 160 20 Q165 12 168 20 Q172 30 160 45"
            fill="none"
            stroke="rgba(150,210,255,0.5)"
            stroke-width="2.5"
            stroke-linecap="round"
            class="spout"
        />
        <!-- 背鳍 -->
        <path
            d="M170 42 Q190 10 220 40"
            fill="#1a6aff"
            stroke="#0e42d2"
            stroke-width="1.5"
            stroke-linejoin="round"
        />
        <!-- 尾鳍 -->
        <path
            d="M290 90 Q330 50 370 55 Q360 75 365 85 Q370 95 360 105 Q365 115 370 125 Q330 130 290 90"
            fill="#1a6aff"
            stroke="#0e42d2"
            stroke-width="1.5"
            stroke-linejoin="round"
        />
        <path
            d="M330 80 Q345 70 355 65"
            fill="none"
            stroke="#3b82f6"
            stroke-width="1.5"
            stroke-linecap="round"
            opacity="0.4"
        />
        <!-- 身体 -->
        <path
            d="M80 90 Q80 55 130 45 Q180 35 230 50 Q280 65 300 90 Q280 115 230 130 Q180 145 130 135 Q80 125 80 90Z"
            fill="#1a6aff"
            stroke="#0e42d2"
            stroke-width="1.5"
            stroke-linejoin="round"
        />
        <!-- 腹部高光 -->
        <path
            d="M90 100 Q130 135 200 120 Q240 112 270 100 Q240 118 200 125 Q130 140 90 100Z"
            fill="#dbeafe"
            opacity="0.6"
        />
        <!-- 胸鳍 -->
        <path
            d="M175 115 Q170 150 150 165 Q145 168 148 158 Q155 145 165 120Z"
            fill="#1a6aff"
            stroke="#0e42d2"
            stroke-width="1.5"
            stroke-linejoin="round"
        />
        <!-- 眼睛 -->
        <ellipse cx="132" cy="80" rx="13" ry="14" fill="#f0f9ff" stroke="#0e42d2" stroke-width="1.5" />
        <ellipse cx="132" cy="80" rx="13" :ry="whaleBlink ? 1.5 : 14" fill="#f0f9ff" stroke="#0e42d2" stroke-width="1.5" />
        <g :style="{ opacity: whaleBlink ? 0 : 1 }">
          <circle cx="128" cy="80" r="6.5" fill="#0e42d2" />
          <circle cx="126" cy="77" r="3" fill="#fff" opacity="0.8" />
          <circle cx="132" cy="83" r="1.5" fill="#fff" opacity="0.4" />
        </g>
        <path d="M92 100 Q110 118 138 112" fill="none" stroke="#0e42d2" stroke-width="2" stroke-linecap="round" />
        <ellipse cx="100" cy="95" rx="8" ry="5" fill="#fda4af" opacity="0.3" />
      </svg>
    </div>

    <!-- 登录卡 -->
    <div class="glass-panel">
      <div class="panel-glow"></div>

      <div class="title-group">
        <h1>海洋学堂<br>智能教育平台</h1>
        <p>探索蔚蓝世界，学习海洋知识</p>
      </div>

      <!-- 使用 el-tabs 切换登录/注册 -->
      <el-tabs v-model="mode" class="auth-tabs" @tab-change="handleTabChange">
        <!-- ========== 登录 Tab ========== -->
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
              <!-- 滑块验证（仅登录） -->
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
                    <span v-if="!isVerified">▶</span>
                    <span v-else>✓</span>
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
                登 录
              </el-button>
            </el-form>
          </Transition>
        </el-tab-pane>

        <!-- ========== 注册 Tab（两步注册） ========== -->
        <el-tab-pane label="注册" name="register">
          <Transition name="auth-fade" mode="out-in">
            <div key="register-container" class="register-container">
              <!-- 步骤指示器 -->
              <div class="step-indicator">
                <div
                    v-for="(step, index) in registerSteps"
                    :key="index"
                    class="step-item"
                    :class="{ active: currentStep === index + 1, completed: currentStep > index + 1 }"
                >
                  <span class="step-number">{{ currentStep > index + 1 ? '✓' : index + 1 }}</span>
                  <span class="step-label">{{ step.label }}</span>
                </div>
                <div class="step-line" :style="{ width: (currentStep - 1) * 50 + '%' }"></div>
              </div>

              <!-- 步骤1：账号信息 -->
              <Transition name="step-slide" mode="out-in">
                <div v-if="currentStep === 1" key="step1" class="step-content">
                  <el-form
                      ref="registerFormRef"
                      :model="registerForm"
                      :rules="registerStep1Rules"
                      label-position="top"
                  >
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
                  <el-form
                      ref="registerFormRef"
                      :model="registerForm"
                      :rules="registerStep2Rules"
                      label-position="top"
                  >
                    <el-form-item label="用户名" prop="realName">
                      <el-input v-model="registerForm.realName" placeholder="请输入用户名" />
                    </el-form-item>
                    <el-form-item label="手机号" prop="phone">
                      <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
                    </el-form-item>
                    <el-form-item label="注册意向角色" prop="applyRole">
                      <el-select v-model="registerForm.applyRole" placeholder="请选择角色" style="width:100%">
                        <el-option label="访客 (VISITOR)" value="VISITOR" />
                        <el-option label="学生 (STUDENT)" value="STUDENT" />
                        <el-option label="教师 (TEACHER)" value="TEACHER" />
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

      <div class="hint">海洋学堂服务</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/auth';
import { loginApi, registerApi } from '@/api/auth';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// ========== 登录 / 注册 模式切换 ==========
const mode = ref('login');
const loading = ref(false);

// ========== 登录表单 ==========
const loginFormRef = ref(null);
const loginForm = reactive({ username: '', password: '' });
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

// ========== 滑块验证 ==========
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
  if (!isVerified.value) sliderLeft.value = 0;
};

// ========== 登录提交 ==========
const handleLogin = async () => {
  if (!loginFormRef.value) return;
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      const res = await loginApi(loginForm);
      authStore.setAuth(res.data.token, res.data.username, res.data.roles || [], res.data.avatarUrl || '');
      ElMessage.success('登录成功');
      const redirect = route.query?.redirect;
      if (redirect && typeof redirect === 'string' && redirect !== '/login') {
        await router.push(redirect);
      } else {
        const roles = res.data.roles || [];
        const isAdmin = roles.some((r) => ['ADMIN', 'MANAGER'].includes(r));
        await router.push(isAdmin ? '/admin/dashboard' : '/home');
      }
    } catch (error) {
      ElMessage.error(error.response?.data || '登录失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  });
};

// 回车登录
const handleEnterLogin = () => {
  if (!isVerified.value) {
    ElMessage.warning('请先完成滑块验证');
    return;
  }
  handleLogin();
};

// ========== 两步注册 ==========
const registerFormRef = ref(null);
const registerForm = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  applyRole: 'VISITOR',
});

// 密码一致性验证
const validateConfirmPassword = (rule, value, callback) => {
  callback(value !== registerForm.password ? new Error('两次输入的密码不一致') : undefined);
};

// 基础验证规则（与老版本完全一致）
const registerRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: ['blur', 'change'] },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
};

const registerSteps = [{ label: '账号信息' }, { label: '个人信息' }];
const currentStep = ref(1);

const registerStep1Rules = {
  username: registerRules.username,
  email: registerRules.email,
  password: registerRules.password,
};
const registerStep2Rules = {
  realName: registerRules.realName,
  phone: registerRules.phone,
  applyRole: [{ required: true, message: '请选择角色', trigger: 'blur' }],
  confirmPassword: registerRules.confirmPassword,
};

// 下一步
const nextStep = async () => {
  if (!registerFormRef.value) return;
  try {
    await registerFormRef.value.validate();
    currentStep.value = 2;
    registerFormRef.value.clearValidate(['realName', 'phone', 'applyRole', 'confirmPassword']);
  } catch (error) {
    ElMessage.warning('表单验证失败，请检查输入内容');
  }
};

// 上一步
const prevStep = () => {
  currentStep.value = 1;
  registerFormRef.value.clearValidate(['username', 'email', 'password']);
};

// 注册提交（包含完整字段）
const handleRegister = async () => {
  if (!registerFormRef.value) return;
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return;
    loading.value = true;
    try {
      await registerApi({
        username: registerForm.username,
        password: registerForm.password,
        realName: registerForm.realName,
        email: registerForm.email,
        phone: registerForm.phone,
        applyRole: registerForm.applyRole,
      });
      ElMessage.success('注册成功，请等待审核');
      currentStep.value = 1;
      setTimeout(() => {
        registerFormRef.value.resetFields();
      }, 300);
      mode.value = 'login';
      isVerified.value = false;
      sliderLeft.value = 0;
    } catch (error) {
      ElMessage.error(error.response?.data || '注册失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  });
};

// Tab 切换时重置状态
const handleTabChange = (tabName) => {
  loading.value = false;
  isVerified.value = false;
  sliderLeft.value = 0;
  if (tabName === 'register') {
    currentStep.value = 1;
    if (registerFormRef.value) {
      registerFormRef.value.clearValidate();
    }
  }
};

// ================================================================
// ==================== 背景特效 ===================================
// ================================================================

const mouseX = ref(0);
const mouseY = ref(0);
const whaleBlink = ref(false);
const whaleJump = ref(false);
const particles = ref([]);
const floatingShapes = ref([]);
const stars = ref([]);
const fishes = ref([]);
const jellyfishes = ref([]);
const seaweeds = ref([]);
const waveCanvas = ref(null);
const bubbleContainerRef = ref(null);

let blinkTimeoutId = null;
let bubbleIntervalId = null;
let resizeHandler = null;
let whaleMoveIntervalId = null;
let whaleXOffset = 0;
let whaleDirection = 1;

const whaleStyle = computed(() => {
  const dx = (mouseX.value - window.innerWidth / 2) * 0.02 + whaleXOffset;
  const dy = (mouseY.value - window.innerHeight / 2) * 0.02;
  return {
    transform: `translate(${dx}px, ${dy}px)`,
  };
});

function triggerBlink() {
  whaleBlink.value = true;
  setTimeout(() => { whaleBlink.value = false; }, 180);
}

function scheduleBlink() {
  blinkTimeoutId = setTimeout(() => {
    triggerBlink();
    scheduleBlink();
  }, 3000 + Math.random() * 4000);
}

function handleMouseMove(e) {
  mouseX.value = e.clientX;
  mouseY.value = e.clientY;
}

function startWhaleDrift() {
  whaleMoveIntervalId = setInterval(() => {
    const speed = 0.3;
    const limit = 60;
    whaleXOffset += speed * whaleDirection;
    if (whaleXOffset > limit) {
      whaleXOffset = limit;
      whaleDirection = -1;
    } else if (whaleXOffset < -limit) {
      whaleXOffset = -limit;
      whaleDirection = 1;
    }
  }, 50);
}

// ─── 粒子 ──────────────
function createParticle() {
  const id = Date.now() + Math.random();
  const size = 2 + Math.random() * 6;
  const colors = ['rgba(255,255,255,0.9)', 'rgba(180,220,255,0.8)', 'rgba(120,200,255,0.7)'];
  particles.value.push({
    id,
    style: {
      left: `${Math.random() * 100}%`,
      top: `${Math.random() * 100}%`,
      width: size + 'px',
      height: size + 'px',
      background: colors[Math.floor(Math.random() * colors.length)],
      boxShadow: `0 0 ${size * 2}px rgba(120,220,255,0.6)`,
      animationDelay: `${Math.random() * 15}s`,
      animationDuration: `${12 + Math.random() * 8}s`,
    },
  });
}

function buildParticles() {
  for (let i = 0; i < 80; i++) createParticle();
}

// ─── 浮动几何 ──────────────
function createShape() {
  const id = Date.now() + Math.random();
  const size = 20 + Math.random() * 60;
  const shapes = ['circle', 'diamond', 'triangle'];
  const type = shapes[Math.floor(Math.random() * shapes.length)];
  let clipPath = '';
  if (type === 'circle') clipPath = 'circle(50%)';
  else if (type === 'diamond') clipPath = 'polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%)';
  else clipPath = 'polygon(50% 0%, 0% 100%, 100% 100%)';
  const colors = ['rgba(22,93,255,0.10)', 'rgba(54,207,201,0.08)', 'rgba(255,255,255,0.06)'];
  floatingShapes.value.push({
    id,
    style: {
      left: `${Math.random() * 100}%`,
      top: `${Math.random() * 100}%`,
      width: size + 'px',
      height: size + 'px',
      clipPath,
      background: colors[Math.floor(Math.random() * colors.length)],
      backdropFilter: 'blur(8px)',
      border: '1px solid rgba(255,255,255,0.08)',
      animationDelay: `${Math.random() * 20}s`,
      animationDuration: `${18 + Math.random() * 12}s`,
    },
  });
}

function buildShapes() {
  for (let i = 0; i < 15; i++) createShape();
}

// ─── 星点 ──────────────
function createStar() {
  const id = Date.now() + Math.random();
  stars.value.push({
    id,
    style: {
      left: `${Math.random() * 100}%`,
      top: `${Math.random() * 100}%`,
      width: '2px',
      height: '2px',
      background: 'white',
      borderRadius: '50%',
      animationDelay: `${Math.random() * 8}s`,
      animationDuration: `${2 + Math.random() * 3}s`,
      boxShadow: '0 0 6px rgba(255,255,255,0.8)',
    },
  });
}

function buildStars() {
  for (let i = 0; i < 30; i++) createStar();
}

// ─── 鱼群 ──────────────
function createFish() {
  const id = Date.now() + Math.random();
  const size = 12 + Math.random() * 18;
  const swimDuration = 16 + Math.random() * 14;
  const yOffset = 5 + Math.random() * 80;
  const xStart = Math.random() * 100;
  const direction = Math.random() > 0.5 ? 1 : -1;
  fishes.value.push({
    id,
    style: {
      left: `${xStart}%`,
      top: `${yOffset}%`,
      width: size + 'px',
      height: size * 0.6 + 'px',
      animationDuration: swimDuration + 's',
      animationDelay: `${Math.random() * 10}s`,
      '--direction': direction,
    },
  });
}

function buildFishes() {
  for (let i = 0; i < 14; i++) createFish();
}

// ─── 水母 ──────────────
function createJellyfish() {
  const id = Date.now() + Math.random();
  const size = 30 + Math.random() * 40;
  const floatDuration = 12 + Math.random() * 8;
  const yOffset = 5 + Math.random() * 70;
  const xOffset = 5 + Math.random() * 90;
  jellyfishes.value.push({
    id,
    style: {
      left: `${xOffset}%`,
      top: `${yOffset}%`,
      width: size + 'px',
      height: size * 1.2 + 'px',
      animationDuration: floatDuration + 's',
      animationDelay: `${Math.random() * 10}s`,
      opacity: 0.2 + Math.random() * 0.3,
    },
  });
}

function buildJellyfishes() {
  for (let i = 0; i < 8; i++) createJellyfish();
}

// ─── 水草（80簇） ──────────────
function createSeaweed() {
  const id = Date.now() + Math.random();
  const height = 35 + Math.random() * 110;
  const left = 0.5 + Math.random() * 99;
  const delay = Math.random() * 8;
  const width = 2 + Math.random() * 5;
  const colors = [
    'rgba(22,93,255,0.20)',
    'rgba(54,207,201,0.16)',
    'rgba(80,180,255,0.14)',
    'rgba(30,144,255,0.22)',
    'rgba(0,200,150,0.13)',
    'rgba(60,120,255,0.18)',
  ];
  seaweeds.value.push({
    id,
    style: {
      left: `${left}%`,
      bottom: '0',
      height: height + 'px',
      width: width + 'px',
      animationDelay: delay + 's',
      animationDuration: `${3.5 + Math.random() * 2.5}s`,
      background: `linear-gradient(to top, ${colors[Math.floor(Math.random() * colors.length)]}, transparent)`,
      borderRadius: '2px 2px 0 0',
      opacity: 0.6 + Math.random() * 0.4,
    },
  });
}

function buildSeaweeds() {
  for (let i = 0; i < 80; i++) createSeaweed();
}

// ─── 气泡（使用 Web Animations API） ──────────────
let bubbleAnimations = [];

function createBubble() {
  const container = bubbleContainerRef.value;
  if (!container) return;

  // 创建气泡元素
  const el = document.createElement('div');
  el.className = 'bubble';

  // 随机参数
  const size = 6 + Math.random() * 18;
  const startX = 5 + Math.random() * 90;
  const startY = 30 + Math.random() * 50;
  const driftX = (Math.random() - 0.5) * 200;
  const riseY = -(150 + Math.random() * 200);
  const duration = 6000 + Math.random() * 5000;

  // 设置样式
  el.style.cssText = `
    left: ${startX}%;
    top: ${startY}%;
    width: ${size}px;
    height: ${size}px;
    position: absolute;
    border-radius: 50%;
    background: radial-gradient(circle at 30% 30%, rgba(255,255,255,0.85), rgba(180,220,255,0.25));
    backdrop-filter: blur(2px);
    border: 1px solid rgba(255,255,255,0.5);
    box-shadow: inset 0 -4px 10px rgba(255,255,255,0.2);
    z-index: 4;
    will-change: transform, opacity;
  `;

  container.appendChild(el);

  // 使用 Web Animations API
  const anim = el.animate(
      [
        { transform: 'translate(0, 0) scale(0.3)', opacity: 0.9 },
        { transform: `translate(${driftX * 0.3}px, ${riseY * 0.3}px) scale(0.7)`, opacity: 1, offset: 0.3 },
        { transform: `translate(${driftX * 0.7}px, ${riseY * 0.7}px) scale(1.1)`, opacity: 0.9, offset: 0.7 },
        { transform: `translate(${driftX}px, ${riseY}px) scale(1.6)`, opacity: 0 },
      ],
      {
        duration: duration,
        easing: 'linear',
      }
  );

  // 动画结束后移除元素
  anim.onfinish = () => {
    if (el.parentNode) el.parentNode.removeChild(el);
    // 从跟踪数组中移除
    const idx = bubbleAnimations.indexOf(anim);
    if (idx > -1) bubbleAnimations.splice(idx, 1);
  };

  bubbleAnimations.push(anim);
}

function startBubbleSystem() {
  // 初始生成一批气泡
  for (let i = 0; i < 15; i++) {
    setTimeout(() => createBubble(), i * 200);
  }
  bubbleIntervalId = setInterval(createBubble, 300);
}

// ─── 涟漪 ──────────────
function handlePageClick(e) {
  createRipple(e.clientX, e.clientY);
  triggerBlink();
}

function createRipple(x, y) {
  const ripple = document.createElement('div');
  ripple.className = 'ripple';
  ripple.style.left = x + 'px';
  ripple.style.top = y + 'px';
  document.body.appendChild(ripple);
  setTimeout(() => ripple.remove(), 1500);
}

// ─── 海浪 Canvas ───────────────
function startWave() {
  const canvas = waveCanvas.value;
  const ctx = canvas.getContext('2d');
  function resize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
  }
  resize();
  resizeHandler = resize;
  window.addEventListener('resize', resizeHandler);
  let t = 0;
  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    for (let i = 0; i < 4; i++) {
      ctx.beginPath();
      for (let x = 0; x < canvas.width; x += 4) {
        const y = canvas.height * 0.78 + Math.sin(x * 0.004 + t + i * 1.2) * 22;
        ctx.lineTo(x, y);
      }
      ctx.strokeStyle = `rgba(80, 180, 255, ${0.12 - i * 0.025})`;
      ctx.lineWidth = 2;
      ctx.stroke();
    }
    t += 0.015;
    requestAnimationFrame(draw);
  }
  draw();
}

// ========== 生命周期 ==========
onMounted(() => {
  scheduleBlink();
  startWhaleDrift();
  startBubbleSystem();
  buildParticles();
  buildShapes();
  buildStars();
  buildFishes();
  buildJellyfishes();
  buildSeaweeds();
  startWave();
});

onUnmounted(() => {
  if (blinkTimeoutId) clearTimeout(blinkTimeoutId);
  if (bubbleIntervalId) clearInterval(bubbleIntervalId);
  if (whaleMoveIntervalId) clearInterval(whaleMoveIntervalId);
  if (resizeHandler) window.removeEventListener('resize', resizeHandler);
  // 停止所有气泡动画
  bubbleAnimations.forEach(anim => {
    try { anim.cancel(); } catch (e) {}
  });
  bubbleAnimations = [];
});
</script>

<style scoped>
/* ===== 基础布局 ===== */
.login-page {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0b1a33;
}

/* ===== Canvas 海面 ===== */
.wave-canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

/* ===== 丁达尔效应光束 ===== */
.tyndall-container {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}
.tyndall-beam {
  position: absolute;
  top: -5%;
  left: 0;
  height: 140%;
  background: linear-gradient(
      to bottom right,
      rgba(255, 255, 255, 0.7) 0%,
      rgba(200, 235, 255, 0.35) 30%,
      rgba(180, 220, 255, 0.10) 70%,
      transparent 95%
  );
  transform-origin: top left;
  filter: blur(25px);
  border-radius: 0 0 50% 50%;
  animation: tyndallPulse 12s ease-in-out infinite alternate;
  box-shadow: 0 0 80px rgba(255, 255, 255, 0.08);
}
.beam-t1 {
  left: -5%;
  width: 70px;
  transform: rotate(30deg);
  animation-delay: 0s;
  opacity: 0.50;
}
.beam-t2 {
  left: 3%;
  width: 100px;
  transform: rotate(22deg);
  animation-delay: 1.8s;
  opacity: 0.40;
}
.beam-t3 {
  left: 12%;
  width: 130px;
  transform: rotate(15deg);
  animation-delay: 3.6s;
  opacity: 0.45;
}
.beam-t4 {
  left: -8%;
  width: 90px;
  transform: rotate(36deg);
  animation-delay: 5.4s;
  opacity: 0.35;
}
.beam-t5 {
  left: 6%;
  width: 60px;
  transform: rotate(26deg);
  animation-delay: 7.2s;
  opacity: 0.30;
}
.tyndall-glow {
  position: absolute;
  top: -10%;
  left: -10%;
  width: 500px;
  height: 400px;
  background: radial-gradient(ellipse at 30% 30%, rgba(255, 255, 255, 0.25) 0%, transparent 70%);
  filter: blur(80px);
  animation: glowPulse 8s ease-in-out infinite alternate;
}
@keyframes glowPulse {
  0%   { opacity: 0.4; transform: scale(1) rotate(0deg); }
  100% { opacity: 0.8; transform: scale(1.2) rotate(10deg); }
}
@keyframes tyndallPulse {
  0% { opacity: 0.30; transform: scaleY(0.92) rotate(-1deg); }
  50% { opacity: 0.55; }
  100% { opacity: 0.40; transform: scaleY(1.10) rotate(2deg); }
}

/* ===== 动态光斑 ===== */
.caustics-layer {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  background:
      radial-gradient(circle at 10% 20%, rgba(255, 255, 255, 0.08) 0%, transparent 35%),
      radial-gradient(circle at 90% 80%, rgba(54, 207, 201, 0.06) 0%, transparent 45%),
      radial-gradient(circle at 50% 50%, rgba(22, 93, 255, 0.04) 0%, transparent 60%),
      radial-gradient(circle at 30% 70%, rgba(255, 255, 255, 0.05) 0%, transparent 30%),
      radial-gradient(circle at 70% 20%, rgba(54, 207, 201, 0.04) 0%, transparent 40%);
  animation: causticsRotate 35s linear infinite;
}
@keyframes causticsRotate {
  0%   { transform: rotate(0deg) scale(1); }
  50%  { transform: rotate(180deg) scale(1.08); }
  100% { transform: rotate(360deg) scale(1); }
}

/* ===== 动态渐变背景 ===== */
.gradient-bg {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
      radial-gradient(1200px 800px at 5% 15%, rgba(22, 93, 255, 0.30), transparent 55%),
      radial-gradient(1000px 700px at 95% 85%, rgba(54, 207, 201, 0.20), transparent 50%),
      radial-gradient(800px 600px at 50% 50%, rgba(255, 255, 255, 0.04), transparent 65%),
      radial-gradient(600px 500px at 20% 80%, rgba(80, 180, 255, 0.12), transparent 50%),
      radial-gradient(600px 500px at 80% 20%, rgba(54, 207, 201, 0.08), transparent 50%);
  animation: bgFloat 28s ease-in-out infinite alternate;
}
@keyframes bgFloat {
  0%   { transform: translate(0, 0) scale(1); opacity: 0.9; }
  50%  { transform: translate(40px, -40px) scale(1.04); opacity: 1; }
  100% { transform: translate(-30px, 30px) scale(0.96); opacity: 0.85; }
}

/* ===== 水草 ===== */
.seaweed-container {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}
.seaweed {
  position: absolute;
  bottom: 0;
  transform-origin: bottom center;
  animation: seaweedSway ease-in-out infinite alternate;
}
@keyframes seaweedSway {
  0%   { transform: rotate(-12deg) scaleY(1); }
  100% { transform: rotate(12deg) scaleY(1.04); }
}

/* ===== 鱼群 ===== */
.fish-container {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}
.fish {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fishSwim linear infinite;
}
@keyframes fishSwim {
  0% {
    transform: translateX(-30px) scaleX(var(--direction, 1));
  }
  49.9% {
    transform: translateX(calc(100vw - 30px)) scaleX(var(--direction, 1));
  }
  50% {
    transform: translateX(calc(100vw - 30px)) scaleX(calc(var(--direction, 1) * -1));
  }
  100% {
    transform: translateX(-30px) scaleX(calc(var(--direction, 1) * -1));
  }
}
.fish-body {
  width: 100%;
  height: 60%;
  background: radial-gradient(ellipse at 70% 50%, #ff7f50, #ff6348);
  border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
  box-shadow: 0 0 12px rgba(255, 127, 80, 0.3);
  position: relative;
}
.fish-tail {
  width: 30%;
  height: 40%;
  background: #ff6348;
  clip-path: polygon(0 0, 100% 50%, 0 100%);
  margin-left: -10%;
}
.fish-eye {
  position: absolute;
  right: 15%;
  top: 20%;
  width: 12%;
  height: 12%;
  background: white;
  border-radius: 50%;
  box-shadow: inset 0 0 2px #333;
}
.fish-eye::after {
  content: '';
  position: absolute;
  right: 15%;
  top: 20%;
  width: 40%;
  height: 40%;
  background: #222;
  border-radius: 50%;
}

/* ===== 水母 ===== */
.jellyfish-container {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
}
.jellyfish {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: jellyFloat ease-in-out infinite alternate;
}
@keyframes jellyFloat {
  0%   { transform: translateY(0) rotate(-6deg); }
  100% { transform: translateY(-50px) rotate(6deg); }
}
.jelly-bell {
  width: 80%;
  height: 50%;
  background: radial-gradient(ellipse at 50% 30%, rgba(200, 230, 255, 0.6), rgba(150, 200, 255, 0.2));
  border-radius: 50% 50% 30% 30% / 60% 60% 20% 20%;
  box-shadow: 0 0 30px rgba(100, 200, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(2px);
}
.jelly-tentacles {
  width: 40%;
  height: 50%;
  background: radial-gradient(ellipse at 50% 0%, rgba(200, 230, 255, 0.3), transparent);
  border-radius: 0 0 50% 50% / 0 0 100% 100%;
  filter: blur(1px);
}

/* ===== 浮动几何图形 ===== */
.floating-shapes {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
  overflow: hidden;
}
.shape {
  position: absolute;
  opacity: 0.35;
  animation: floatShape 22s ease-in-out infinite alternate;
}
@keyframes floatShape {
  0%   { transform: translate(0, 0) rotate(0deg) scale(1); }
  33%  { transform: translate(50px, -70px) rotate(35deg) scale(1.1); }
  66%  { transform: translate(-40px, 60px) rotate(-25deg) scale(0.9); }
  100% { transform: translate(30px, -40px) rotate(15deg) scale(1.05); }
}

/* ===== 闪烁星点 ===== */
.sparkles {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}
.star {
  position: absolute;
  animation: twinkle 3s ease-in-out infinite alternate;
}
@keyframes twinkle {
  0%   { opacity: 0.15; transform: scale(0.7); }
  100% { opacity: 1; transform: scale(1.3); }
}

/* ===== 原有光束 ===== */
.light-beam {
  position: absolute;
  top: -20%;
  width: 320px;
  height: 140vh;
  filter: blur(40px);
  opacity: 0.10;
  pointer-events: none;
  z-index: 2;
}
.beam-1 {
  left: 3%;
  background: linear-gradient(transparent, rgba(255, 255, 255, 0.6), transparent);
  animation: beamMove1 18s linear infinite;
}
.beam-2 {
  left: 30%;
  background: linear-gradient(transparent, rgba(180, 240, 255, 0.5), transparent);
  animation: beamMove2 22s linear infinite;
}
.beam-3 {
  right: 8%;
  background: linear-gradient(transparent, rgba(255, 255, 255, 0.5), transparent);
  animation: beamMove3 20s linear infinite;
}
.beam-4 {
  left: 55%;
  background: linear-gradient(transparent, rgba(54, 207, 201, 0.3), transparent);
  animation: beamMove1 25s linear infinite reverse;
}

/* ===== 粒子 ===== */
.particle {
  position: absolute;
  border-radius: 50%;
  animation: particleFloat 14s ease-in-out infinite;
  z-index: 3;
}
@keyframes particleFloat {
  0%, 100% { transform: translateY(0) scale(1); opacity: 0.5; }
  50%      { transform: translateY(-45px) scale(1.2); opacity: 1; }
}

/* ===== 气泡容器 ===== */
.bubble-container {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
  overflow: hidden;
}
/* 气泡基样式（JS 负责具体位置和动画） */
.bubble {
  position: absolute;
  border-radius: 50%;
  will-change: transform, opacity;
  /* 背景和边框由 JS 内联设置，但保留默认样式兜底 */
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.85), rgba(180, 220, 255, 0.25));
  backdrop-filter: blur(2px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: inset 0 -4px 10px rgba(255, 255, 255, 0.2);
}

/* ===== 鲸鱼 ===== */
.whale-wrapper {
  position: absolute;
  top: 8%;
  left: 50%;
  width: 420px;
  transform: translateX(-50%);
  z-index: 10;
  cursor: pointer;
  transition: transform 0.15s ease;
}
.whale-svg {
  width: 85%;
  height: auto;
  overflow: visible;
  filter: drop-shadow(0 20px 50px rgba(26, 106, 255, 0.25));
}
.spout {
  animation: spoutPulse 3s ease-in-out infinite;
  transform-origin: bottom center;
}
@keyframes spoutPulse {
  0%, 100% { opacity: 0.3; transform: scaleY(1); }
  50%      { opacity: 0.7; transform: scaleY(1.08); }
}
.whale-wrapper.jump {
  animation: whaleJump 1.2s ease;
}

/* ===== 登录卡 ===== */
.glass-panel {
  position: relative;
  width: 420px;
  padding: 34px;
  border-radius: 28px;
  overflow: hidden;
  z-index: 20;
  background: rgba(255, 255, 255, 0.70);
  backdrop-filter: blur(30px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.4);
}
.panel-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  transform: translateX(-150%);
  animation: cardShine 6s infinite;
}

/* ===== 标题 ===== */
.title-group {
  text-align: center;
  margin-bottom: 24px;
}
.title-group h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #1a6aff, #36cfc9);
  -webkit-background-clip: text;
  color: transparent;
}
.title-group p {
  margin-top: 6px;
  color: rgba(0, 0, 0, 0.5);
  font-size: 13px;
}

/* ===== el-tabs ===== */
.auth-tabs {
  --el-tabs-header-margin: 0 0 20px 0;
}
:deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(0, 0, 0, 0.06);
}
:deep(.el-tabs__item) {
  color: rgba(0, 0, 0, 0.4);
  font-size: 15px;
  transition: color 0.25s;
}
:deep(.el-tabs__item.is-active) {
  color: #2a9df4;
  font-weight: 600;
}
:deep(.el-tabs__active-bar) {
  background-color: #2a9df4;
}
:deep(.el-tabs__content) {
  padding: 0 !important;
}

/* ===== 登录表单 ===== */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.login-form {
  justify-content: center;
}
:deep(.el-form-item) {
  margin-bottom: 14px;
}
:deep(.el-form-item__label) {
  color: rgba(0, 0, 0, 0.5) !important;
  font-size: 13px;
  font-weight: 500;
}
:deep(.el-input__wrapper) {
  height: 44px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
}
:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(50, 170, 255, 0.5);
  box-shadow: 0 0 0 4px rgba(50, 170, 255, 0.10);
}
:deep(.el-input__inner) {
  color: #1a2a3a !important;
}
:deep(.el-input__inner::placeholder) {
  color: rgba(0, 0, 0, 0.3);
}

/* ===== 滑块验证 ===== */
.slider-captcha {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.04);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
  transform: translateZ(0);
  user-select: none;
  margin-top: 4px;
  margin-bottom: 8px;
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
  background: rgba(42, 157, 244, 0.15);
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
  color: rgba(0, 0, 0, 0.35);
  z-index: 1;
  pointer-events: none;
  transition: color 0.3s ease;
}
.slider-thumb {
  position: absolute;
  left: 0;
  top: 0;
  width: 46px;
  height: 46px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  z-index: 2;
  color: #2a9df4;
  font-size: 20px;
  transition: color 0.3s ease;
}
.slider-thumb:active { cursor: grabbing; }
.slider-captcha.is-verified .slider-bg { background: rgba(103, 194, 58, 0.3); }
.slider-captcha.is-verified .slider-text { color: #67c23a; font-weight: 500; }
.slider-captcha.is-verified .slider-thumb { color: #67c23a; cursor: default; }

/* ===== 登录按钮 ===== */
.auth-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 12px !important;
  border: none !important;
  color: #fff !important;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(90deg, #2a9df4, #46d9ff, #2a9df4) !important;
  background-size: 300% 300% !important;
  animation: oceanFlow 6s ease infinite;
  box-shadow: 0 10px 25px rgba(42, 157, 244, 0.25) !important;
}
.auth-btn.is-disabled,
.auth-btn:disabled {
  background: #a0b8d0 !important;
  box-shadow: none !important;
  color: rgba(255, 255, 255, 0.6) !important;
  cursor: not-allowed;
  animation: none;
}
.auth-btn:not(.is-disabled):not(:disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 15px 35px rgba(42, 157, 244, 0.35) !important;
}
.auth-btn:not(.is-disabled):not(:disabled):active {
  transform: translateY(0);
}

/* ===== 分步注册 ===== */
.register-container {
  display: flex;
  flex-direction: column;
  min-height: 360px;
}
.step-indicator {
  position: relative;
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
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
  background: rgba(0, 0, 0, 0.06);
  color: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
  transition: all 0.3s ease;
}
.step-item.active .step-number {
  background: #2a9df4;
  color: #fff;
  box-shadow: 0 4px 12px rgba(42, 157, 244, 0.3);
}
.step-item.completed .step-number {
  background: #67c23a;
  color: #fff;
}
.step-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.3);
  transition: color 0.3s ease;
}
.step-item.active .step-label {
  color: #2a9df4;
  font-weight: 500;
}
.step-line {
  position: absolute;
  top: 14px;
  left: 10%;
  width: 80%;
  height: 2px;
  background: rgba(0, 0, 0, 0.06);
  z-index: 0;
  transition: width 0.3s ease;
}
.step-content {
  flex: 1;
  padding-bottom: 16px;
}
.step-content :deep(.el-form-item) {
  margin-bottom: 14px;
}
.step-content :deep(.el-form-item__label) {
  color: rgba(0, 0, 0, 0.5) !important;
  font-size: 13px;
  font-weight: 500;
}
.step-content :deep(.el-input__wrapper) {
  height: 44px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
}
.step-content :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(50, 170, 255, 0.5);
  box-shadow: 0 0 0 4px rgba(50, 170, 255, 0.10);
}
.step-content :deep(.el-input__inner) {
  color: #1a2a3a !important;
}
.step-content :deep(.el-input__inner::placeholder) {
  color: rgba(0, 0, 0, 0.3);
}
.step-content :deep(.el-select .el-input__wrapper) {
  height: 44px;
}
.confirm-password-item {
  margin-bottom: 4px !important;
}
.step-buttons {
  display: flex;
  gap: 12px;
  margin-top: auto;
}
.back-btn {
  flex: 1;
  height: 46px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.5);
  color: rgba(0, 0, 0, 0.5);
  font-weight: 500;
  transition: all 0.25s ease;
}
.back-btn:hover {
  border-color: #2a9df4;
  color: #2a9df4;
}
.step-btn {
  flex: 2;
  margin-top: 0 !important;
}
.step-btn.is-disabled,
.step-btn:disabled {
  background: #a0b8d0 !important;
  box-shadow: none !important;
  color: rgba(255, 255, 255, 0.6) !important;
  cursor: not-allowed;
  animation: none;
}

/* ===== Hint ===== */
.hint {
  margin-top: 12px;
  text-align: center;
  color: rgba(0, 0, 0, 0.25);
  font-size: 12px;
}

/* ===== Ripple ===== */
.ripple {
  position: fixed;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  pointer-events: none;
  background: rgba(100, 200, 255, 0.4);
  transform: translate(-50%, -50%) scale(0);
  animation: rippleExpand 1.5s ease-out;
}

/* ===== 过渡动画 ===== */
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

/* ===== 关键帧 ===== */
@keyframes oceanFlow {
  0%   { background-position: 0% 50%; }
  50%  { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
@keyframes rippleExpand {
  0%   { opacity: 1; transform: translate(-50%, -50%) scale(0); }
  100% { opacity: 0; transform: translate(-50%, -50%) scale(40); }
}
@keyframes whaleJump {
  0%   { transform: translateY(0); }
  50%  { transform: translateY(-120px) rotate(-8deg); }
  100% { transform: translateY(0); }
}
@keyframes beamMove1 {
  0%   { transform: translateX(0) rotate(-20deg); }
  50%  { transform: translateX(80px) rotate(-18deg); }
  100% { transform: translateX(0) rotate(-20deg); }
}
@keyframes beamMove2 {
  0%   { transform: translateX(0) rotate(-15deg); }
  50%  { transform: translateX(-120px) rotate(-12deg); }
  100% { transform: translateX(0) rotate(-15deg); }
}
@keyframes beamMove3 {
  0%   { transform: translateX(0) rotate(15deg); }
  50%  { transform: translateX(100px) rotate(18deg); }
  100% { transform: translateX(0) rotate(15deg); }
}
@keyframes cardShine {
  0%   { transform: translateX(-150%); }
  30%  { transform: translateX(150%); }
  100% { transform: translateX(150%); }
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .glass-panel {
    width: 92%;
    padding: 20px;
    background: rgba(255, 255, 255, 0.75);
  }
  .whale-wrapper {
    width: 200px;
    top: 3%;
  }
  .title-group h1 {
    font-size: 24px;
  }
  .title-group p {
    font-size: 12px;
  }
  .light-beam {
    display: none;
  }
  .shape {
    display: none;
  }
  .fish {
    display: none;
  }
  .jellyfish {
    display: none;
  }
  .seaweed {
    display: none;
  }
  .caustics-layer {
    opacity: 0.3;
  }
  .tyndall-beam {
    opacity: 0.12 !important;
    filter: blur(60px);
  }
  .tyndall-glow {
    display: none;
  }
  :deep(.el-input__wrapper) {
    height: 40px !important;
  }
  .slider-captcha {
    height: 40px;
  }
  .slider-thumb {
    height: 40px;
    width: 40px;
    font-size: 16px;
  }
  .auth-btn {
    height: 42px;
    font-size: 14px;
  }
  .step-number {
    width: 24px;
    height: 24px;
    font-size: 12px;
  }
  .step-label {
    font-size: 10px;
  }
  .step-line {
    top: 12px;
  }
}
</style>