<template>
  <div class="profile-container">
    <el-row :gutter="24">
      <el-col :xs="24" :md="8">
        <el-card class="user-card" shadow="never" v-loading="loading">
          <div class="card-bg"></div>
          <div class="avatar-container">
            <el-upload
              class="avatar-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleAvatarChange"
              accept="image/*"
            >
              <div class="avatar-wrapper" v-loading="uploading">
                <el-avatar :size="100" :src="profileForm.avatarUrl" class="user-avatar">
                  <img src="https://cube.elemecdn.com/e/fd/0fc769396203ba652971805f60932png.png" />
                </el-avatar>
                <div class="avatar-mask">
                  <el-icon :size="24"><Camera /></el-icon>
                </div>
              </div>
            </el-upload>
          </div>

          <div class="user-info-center">
            <div class="main-name">{{ profileForm.realName || profileForm.username || '未命名用户' }}</div>
            <div class="sub-name">@{{ profileForm.username }}</div>
            <div class="role-tags">
              <el-tag v-for="role in displayRoles" :key="role" size="small" effect="light" class="custom-tag">
                {{ role }}
              </el-tag>
              <el-tag v-if="!displayRoles.length" size="small" type="info">暂无角色</el-tag>
            </div>
          </div>

          <el-divider border-style="dashed" />

          <div class="user-stats">
            <div class="stat-item">
              <div class="stat-label">账号状态</div>
              <div class="stat-value" :class="userStatus === 1 ? 'text-seafoam' : 'text-danger'">
                {{ userStatus === 1 ? '正常' : '已禁用' }}
              </div>
            </div>
            <div class="stat-item">
              <div class="stat-label">当前角色</div>
              <div class="stat-value">{{ primaryRole }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16">
        <el-card class="settings-card" shadow="never">
          <el-tabs v-model="activeTab" class="profile-tabs">

            <el-tab-pane label="基本资料" name="basic">
              <div class="tab-content">
                <el-form ref="formRef" :model="profileForm" :rules="rules" label-width="80px" label-position="top">
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="账号" prop="username">
                        <el-input v-model="profileForm.username" disabled />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="用户名" prop="realName">
                        <el-input v-model="profileForm.realName" placeholder="请输入用户名" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-row :gutter="20">
                    <el-col :span="12">
                      <el-form-item label="邮箱" prop="email">
                        <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="手机号" prop="phone">
                        <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item style="margin-top: 12px;">
                    <el-button type="primary" @click="submitProfile" :loading="submitting">保存修改</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

            <el-tab-pane label="安全设置" name="security">
              <div class="tab-content">
                <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="80px" label-position="top">
                  <el-form-item label="原密码" prop="oldPassword">
                    <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password style="max-width: 320px;" />
                  </el-form-item>
                  <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码（至少6位）" show-password style="max-width: 320px;" />
                  </el-form-item>
                  <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password style="max-width: 320px;" />
                  </el-form-item>
                  <el-form-item style="margin-top: 12px;">
                    <el-button type="primary" @click="submitPassword" :loading="changingPassword">更新密码</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>

          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Camera } from "@element-plus/icons-vue";
import { getUserProfile, updatePasswordApi, updateUserProfile, uploadAvatarApi } from "@/api/sysUser";
import { useAuthStore } from "@/store/auth";

const authStore = useAuthStore();
const activeTab = ref("basic");

const ROLE_MAP = { ADMIN: "系统管理员", MANAGER: "管理人员", VISITOR: "普通用户" };
const displayRoles = computed(() => {
  return authStore.roles.map(code => ROLE_MAP[code] || code);
});
const primaryRole = computed(() => {
  if (!authStore.roles.length) return "未分配";
  return ROLE_MAP[authStore.roles[0]] || authStore.roles[0];
});

const loading = ref(false);
const uploading = ref(false);
const submitting = ref(false);
const formRef = ref(null);
const userStatus = ref(1);

const profileForm = reactive({
  username: "",
  realName: "",
  email: "",
  phone: "",
  avatarUrl: ""
});

const rules = {
  realName: [{ required: true, message: "用户名不能为空", trigger: "blur" }],
  email: [
    { required: true, message: "邮箱不能为空", trigger: "blur" },
    { type: "email", message: "请输入正确的邮箱格式", trigger: "blur" }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: "请输入正确的手机号格式", trigger: "blur" }
  ]
};

// 格式化头像 URL 的辅助函数，利用 /api 走本地 Vite 代理
const formatAvatarUrl = (url) => {
  if (!url) return "";
  // 如果已经是完整 http 链接，或者已经包含了 /api 前缀，则直接返回
  if (url.startsWith("http") || url.startsWith("/api")) {
    return url;
  }
  // 否则加上 /api 前缀，让 vite 代理到后端去请求图片
  return `/api${url}`;
};

// 初始化获取数据
const fetchProfile = async () => {
  loading.value = true;
  try {
    const res = await getUserProfile();
    Object.assign(profileForm, res.data);
    userStatus.value = res.data.status ?? 1;
    // 1. 处理后端返回的相对路径
    profileForm.avatarUrl = formatAvatarUrl(profileForm.avatarUrl);

    // 2. 同步到全局 store，这样刷新页面时顶栏头像也能加载出来
    authStore.avatarUrl = profileForm.avatarUrl;

  } catch (err) {
    ElMessage.error("获取个人资料失败");
  } finally {
    loading.value = false;
  }
};

// 头像上传逻辑
const handleAvatarChange = async (uploadFile) => {
  const file = uploadFile.raw;
  if (file.size / 1024 / 1024 > 2) {
    ElMessage.error("头像图片大小不能超过 2MB!");
    return;
  }

  uploading.value = true;
  try {
    const res = await uploadAvatarApi(file);
    // 1. 处理上传成功后返回的路径
    profileForm.avatarUrl = formatAvatarUrl(res.data.avatarUrl);

    // 2. 同步到全局 store，让顶栏立刻响应变化
    authStore.avatarUrl = profileForm.avatarUrl;
    ElMessage.success("头像上传成功");
  } catch (err) {
    ElMessage.error("头像上传失败");
  } finally {
    uploading.value = false;
  }
};

// 资料提交逻辑
const submitProfile = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        await updateUserProfile({
          realName: profileForm.realName,
          email: profileForm.email,
          phone: profileForm.phone
        });
        ElMessage.success("资料更新成功");
      } catch (err) {
        ElMessage.error(err.response?.data || "更新失败");
      } finally {
        submitting.value = false;
      }
    }
  });
};

const passwordFormRef = ref(null);
const changingPassword = ref(false);
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const validateConfirmPassword = (_rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error("两次输入的密码不一致"));
  } else {
    callback();
  }
};

const passwordRules = {
  oldPassword: [{ required: true, message: "请输入原密码", trigger: "blur" }],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "新密码长度不能少于6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认新密码", trigger: "blur" },
    { validator: validateConfirmPassword, trigger: "blur" },
  ],
};

const submitPassword = async () => {
  if (!passwordFormRef.value) return;
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return;

    changingPassword.value = true;
    try {
      await updatePasswordApi({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      });

      passwordForm.oldPassword = "";
      passwordForm.newPassword = "";
      passwordForm.confirmPassword = "";

      ElMessage.success("密码修改成功，即将返回登录页");
      setTimeout(async () => {
        await authStore.logoutAction();
      }, 1500);
    } catch (err) {
      ElMessage.error(err.response?.data || "密码修改失败");
    } finally {
      changingPassword.value = false;
    }
  });
};

onMounted(fetchProfile);
</script>

<style scoped>
.profile-container {
  padding: 0;
  overflow-x: hidden;
}

/* ═══ 左侧名片样式 ═══ */
.user-card {
  position: relative;
  overflow: hidden;
  padding: 0 !important;
}

:deep(.user-card .el-card__body) {
  padding: 0;
}

.card-bg {
  height: 120px;
  background: linear-gradient(135deg, var(--theme-klein-blue-light) 0%, var(--theme-klein-blue) 100%);
  position: relative;
}

.card-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: url("data:image/svg+xml,%3Csvg width='100' height='100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M0 100 C 20 0 50 0 100 100 Z' fill='rgba(255,255,255,0.05)'/%3E%3C/svg%3E") repeat-x;
  background-size: cover;
}

.avatar-container {
  display: flex;
  justify-content: center;
  margin-top: -50px;
  position: relative;
  z-index: 2;
}

.avatar-wrapper {
  position: relative;
  border-radius: 50%;
  padding: 4px;
  background: rgba(250, 249, 246, 0.8);
  backdrop-filter: blur(8px);
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.user-avatar {
  display: block;
}

.avatar-mask {
  position: absolute;
  inset: 4px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.user-info-center {
  text-align: center;
  padding: 16px 20px;
}

.main-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--theme-text-primary);
  margin-bottom: 4px;
}

.sub-name {
  font-size: 13px;
  color: var(--theme-text-muted);
  margin-bottom: 12px;
}

.custom-tag {
  background: rgba(0, 47, 167, 0.08) !important;
  border: 1px solid rgba(0, 47, 167, 0.2) !important;
  color: var(--theme-klein-blue) !important;
  border-radius: 12px;
  padding: 0 12px;
}

.user-stats {
  display: flex;
  padding: 0 20px 24px;
  justify-content: space-around;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: var(--theme-text-muted);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--theme-text-primary);
}

.text-seafoam { color: var(--theme-success, #67c23a); }
.text-danger { color: var(--theme-coral, #f56c6c); }

/* ═══ 右侧设置样式 ═══ */
.settings-card {
  min-height: 400px;
}

.tab-content {
  padding: 12px 4px;
}

:deep(.profile-tabs .el-tabs__item) {
  font-size: 15px;
}

:deep(.profile-tabs .el-tabs__item.is-active) {
  font-weight: 600;
}
</style>
