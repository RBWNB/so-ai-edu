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
        <div class="panel-left brand-section">
          <div class="brand-content">
            <!-- 🐋 极简蓝鲸流线 Logo -->
            <svg class="brand-logo" viewBox="0 0 96 64" width="96" height="64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <!-- 鲸身 -->
              <path d="M8 34
                       Q18 18 40 16
                       Q60 10 74 20
                       Q82 22 84 28
                       Q86 24 88 28
                       Q86 32 84 28
                       Q82 34 74 34
                       Q60 40 40 36
                       Q22 34 14 38
                       Z"
                    fill="var(--theme-klein-blue)" opacity="0.88" />
              <!-- 背鳍 -->
              <path d="M54 18 Q56 8 60 17" fill="var(--theme-klein-blue)" opacity="0.7" />
              <!-- 胸鳍 -->
              <path d="M36 30 Q32 42 38 44 Q40 38 40 32" fill="var(--theme-klein-blue)" opacity="0.55" />
              <!-- 眼睛 -->
              <circle cx="68" cy="22" r="1.8" fill="#FAF9F6" opacity="0.9" />
              <circle cx="68.5" cy="21.7" r="0.7" fill="var(--theme-klein-blue)" opacity="0.5" />
              <!-- 流线 — 腹白线 -->
              <path d="M16 36 Q38 34 64 32" fill="none" stroke="#FAF9F6" stroke-width="0.8" opacity="0.4" />
            </svg>
            <h1 class="brand-title">海洋生物多样性<br>信息管理系统</h1>
            <p class="brand-slogan">守护蓝色家园，科学管理海洋</p>
            <el-button class="knowledge-preview-btn" @click="knowledgeVisible = true">
              <el-icon><Reading /></el-icon>
              海洋知识预览
            </el-button>
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

    <!-- 海洋知识预览弹窗 -->
    <el-dialog
      v-model="knowledgeVisible"
      title=""
      width="90%"
      top="3vh"
      destroy-on-close
      class="knowledge-dialog"
    >
      <KnowledgeBase />
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { Right, Select, Reading } from "@element-plus/icons-vue";
import KnowledgeBase from "@/views/admin/knowledge/Index.vue";
import { useAuthStore } from "@/store/auth";
import { loginApi, registerApi } from "@/api/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const activeTab = ref("login");
const loading = ref(false);
const knowledgeVisible = ref(false);

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
    // 方式1：使用 Promise 风格验证指定字段（推荐，Element Plus 2.3+ 支持）
    // validateField 返回 Promise，resolve 结果为 { 字段名: 错误信息 }
    const errorFields = await registerFormRef.value.validateField(['username', 'email', 'password']);

    // 判断是否有验证错误：检查返回的错误对象是否为空
    const hasError = Object.keys(errorFields).some(key => errorFields[key]);
    if (!hasError) {
      currentStep.value = 2;
      // 清除第二步可能存在的验证错误
      registerFormRef.value.clearValidate(['realName', 'phone', 'applyRole', 'confirmPassword']);
    }
  } catch (error) {
    // 捕获验证异常（极少出现，兜底）
    console.error('第一步验证失败:', error);
    ElMessage.warning('表单验证失败，请检查输入内容');
  }
};

// 上一步方法
const prevStep = () => {
  currentStep.value = 1;
  // 清除第一步的验证错误
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

      // 先重置步骤，再延迟重置表单（避免闪烁）
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
    // 清除所有验证错误
    if (registerFormRef.value) {
      registerFormRef.value.clearValidate();
    }
  }
};

// 处理回车登录（增加滑块验证判断）
const handleEnterLogin = () => {
  // 检查滑块验证状态
  if (!isVerified.value) {
    ElMessage.warning('请先完成滑块验证');
    return;
  }
  // 验证通过，执行登录
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
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.brand-logo {
  display: block;
  margin: 0 auto 28px auto;
  filter: drop-shadow(0 8px 16px rgba(22, 93, 255, 0.16));
  animation: logo-subtle 5s ease-in-out infinite;
}

@keyframes logo-subtle {
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

/* ═══ 海洋知识预览按钮 ═══ */
.knowledge-preview-btn {
  margin-top: 28px;
  padding: 10px 28px;
  border-radius: 8px;
  border: 1px solid var(--theme-primary);
  background: transparent;
  color: var(--theme-primary);
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 1px;
  transition: all 0.3s ease;
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
  .brand-logo {
    width: 64px;
    height: 42px;
    margin-bottom: 18px;
  }
  .auth-form {
    min-height: 320px;
  }
  .register-container {
    min-height: 320px;
  }
}
</style>