<template>
  <div class="publish-observation">
    <el-card shadow="soft" class="form-card">
      <template #header>
        <div class="card-header">
          <el-button text @click="goBack" class="back-btn">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <span class="header-title">发布观察记录</span>
          <span class="header-placeholder"></span>
        </div>
      </template>

      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          v-loading="submitting"
          size="large"
      >
        <!-- 核心必填项：首屏展示 -->
        <el-form-item label="标题" prop="title">
          <el-input
              v-model="form.title"
              placeholder="比如：清晨的深圳湾白鹭群"
              maxlength="50"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="描述下你观察到的有趣细节～比如：白鹭叼着小鱼掠过水面"
              maxlength="500"
              show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-form-item label="观察地点" prop="locationName">
              <el-input
                  v-model="form.locationName"
                  placeholder="比如：深圳湾公园-观鸟台"
              />
              <!-- 可选：常用地点快捷选择 -->
              <div class="common-locations" v-if="commonLocations.length">
                <span class="common-tip">常用地点：</span>
                <el-tag
                    v-for="loc in commonLocations"
                    :key="loc"
                    closable
                    @click="form.locationName = loc"
                >{{ loc }}</el-tag>
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="观察时间" prop="observedAt">
              <el-date-picker
                  v-model="form.observedAt"
                  type="datetime"
                  placeholder="选择观察时间"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  style="width: 100%"
                  :editable="false"
                  :shortcuts="[
                    { text: '刚刚', value: new Date() },
                    { text: '今天上午', value: new Date(new Date().setHours(10, 0, 0)) },
                    { text: '昨天', value: new Date(new Date().setDate(new Date().getDate() - 1)) }
                  ]"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="现场照片" prop="photoFile">
          <el-upload
              action="#"
              :class="{ 'hide-upload-trigger': form.photoFile !== null }"
              :auto-upload="false"
              :show-file-list="true"
              :on-change="handlePhotoChange"
              :on-remove="handlePhotoRemove"
              :file-list="fileList"
              list-type="picture-card"
              accept="image/*"
              :limit="1"
              v-loading="photoUploading"
              element-loading-text="照片上传中～很快就好"
              capture="camera"
          >
          <div class="upload-tip">
            <el-icon :size="28"><Plus /></el-icon>
            <p class="upload-text">点击上传/拍照</p>
          </div>
          </el-upload>
        </el-form-item>

        <!-- 非必填项：默认折叠 -->
        <div class="expand-section">
          <el-button
              type="text"
              class="expand-btn"
              @click="expandMore = !expandMore"
          >
            <el-icon><ArrowDown v-if="!expandMore" /><ArrowUp v-else /></el-icon>
            {{ expandMore ? '收起更多信息' : '+ 补充更多信息（可选）' }}
          </el-button>

          <div class="more-content" v-show="expandMore">
            <!-- 地理坐标 -->
            <div class="location-group">
              <div class="location-header">
                <span class="el-form-item__label">地理坐标（可选）</span>
                <el-button type="primary" link @click="getLocation" :loading="locating">
                  <el-icon><Location /></el-icon> 自动获取定位
                </el-button>
              </div>
              <el-row :gutter="20">
                <el-col :xs="12" :sm="12">
                  <el-form-item prop="latitude" class="no-label">
                    <el-input
                        v-model.number="form.latitude"
                        placeholder="纬度，如：22.5264"
                        type="number"
                        suffix="°"
                    />
                  </el-form-item>
                </el-col>
                <el-col :xs="12" :sm="12">
                  <el-form-item prop="longitude" class="no-label">
                    <el-input
                        v-model.number="form.longitude"
                        placeholder="经度，如：113.9558"
                        type="number"
                        suffix="°"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <div class="location-tip" v-if="!form.latitude && !form.longitude">
                定位失败？可手动输入，示例：纬度22.5264° 经度113.9558°
              </div>
            </div>

            <!-- 物种发现 -->
            <el-form-item label="物种发现（可选）">
              <el-select
                  v-model="form.speciesId"
                  placeholder="搜索你发现的物种名称"
                  filterable
                  remote
                  :remote-method="searchSpecies"
                  :loading="speciesLoading"
                  clearable
                  style="width: 100%"
              >
                <el-option
                    v-for="s in speciesOptions"
                    :key="s.id"
                    :label="`${s.chineseName} (${s.scientificName || ''})`"
                    :value="s.id"
                >
                  <!-- 可选：物种缩略图 -->
                  <div class="species-option">
                    <img v-if="s.avatar" :src="s.avatar" class="species-avatar" />
                    <span>{{ s.chineseName }}</span>
                  </div>
                </el-option>
              </el-select>
              <div v-if="speciesOptions.length === 0 && speciesLoading === false" class="species-empty">
                未找到该物种？试试拼音/别名～
              </div>
            </el-form-item>
          </div>
        </div>

        <el-form-item class="action-footer">
          <el-button
              type="primary"
              class="submit-btn"
              @click="submitForm"
              :loading="submitting"
              :disabled="!isFormReady"
              @mouseenter="showSubmitTip = true"
              @mouseleave="showSubmitTip = false"
          >
            {{ submitting ? '发布中～' : (isFormReady ? '发布观察' : '请填写必填项') }}
          </el-button>
          <div class="submit-tip" v-if="showSubmitTip && !isFormReady">
            还需填写：{{ unFinishedFields.join('、') }}
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft, Plus, Location, ArrowDown, ArrowUp } from "@element-plus/icons-vue";
import { suggestSpecies } from "@/api/species";
import { createObservation, uploadObservationPhoto, getCommonLocations } from "@/api/observation";

const router = useRouter();
const route = useRoute();
const formRef = ref(null);
const submitting = ref(false);
const locating = ref(false);
const speciesLoading = ref(false);
const speciesOptions = ref([]);
const fileList = ref([]);
const photoUploading = ref(false);
const uploadedMediaId = ref(null);
const expandMore = ref(false); // 控制非必填项折叠
const showSubmitTip = ref(false); // 提交按钮提示
const commonLocations = ref([]); // 从数据库获取的常用地点标签

const form = reactive({
  title: "",
  description: "",
  locationName: "",
  observedAt: "",
  latitude: null,
  longitude: null,
  speciesId: null,
  photoFile: null,
});

// 规则优化：提示语口语化
const rules = {
  title: [{ required: true, message: "标题不能为空哦～", trigger: "blur" }],
  locationName: [{ required: true, message: "请填写观察地点呀～", trigger: "blur" }],
  observedAt: [{ required: true, message: "选一下观察时间吧～", trigger: "change" }],
  photoFile: [{ required: true, message: "上传一张现场照片吧～", trigger: "change" }], // 新增照片必填
};

// 初始化时间
const initCurrentTime = () => {
  const now = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  form.observedAt = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
};

onMounted(async () => {
  initCurrentTime();

  // 支持从 AI 识别页跳转时预填
  if (route.query.aiDescription) {
    form.description = route.query.aiDescription;
    form.title = '海洋生物观察记录';
    // 如果 AI 页已上传图片，直接使用
    if (route.query.mediaId) {
      uploadedMediaId.value = route.query.mediaId;
      form.photoFile = true; // 标记照片已就绪
      if (route.query.photoUrl) {
        fileList.value = [{ url: route.query.photoUrl, name: 'AI识别照片', status: 'success' }];
      }
    }
    ElMessage.info('已自动填入 AI 识别结果，可补充修改后发布');
  }

  // 从数据库获取用户常用地点（Top 5）
  try {
    const res = await getCommonLocations();
    if (res.data?.success) {
      commonLocations.value = res.data.data || [];
    }
  } catch { /* 加载失败不影响主流程 */ }
  // 监听表单变化，实时更新提交按钮状态
  watch([() => form.title, () => form.locationName, () => form.observedAt, () => form.photoFile], () => {
    checkFormReady();
  });
});

// 计算表单是否可提交
const unFinishedFields = ref([]);
const isFormReady = computed(() => {
  const fields = [
    { key: "title", name: "标题" },
    { key: "locationName", name: "观察地点" },
    { key: "observedAt", name: "观察时间" },
    { key: "photoFile", name: "现场照片" },
  ];
  const unFinished = fields.filter(item => !form[item.key]);
  unFinishedFields.value = unFinished.map(item => item.name);
  return unFinished.length === 0;
});

// 检查表单是否可提交（主动触发）
const checkFormReady = () => {
  return isFormReady.value;
};

// 定位功能优化：反馈更友好
const getLocation = () => {
  if (!navigator.geolocation) {
    ElMessage.warning("你的浏览器不支持定位哦，试试手动输入～");
    return;
  }
  locating.value = true;
  navigator.geolocation.getCurrentPosition(
      (position) => {
        form.latitude = Number(position.coords.latitude.toFixed(6));
        form.longitude = Number(position.coords.longitude.toFixed(6));
        ElMessage.success("定位成功啦 ✨");
        locating.value = false;
      },
      (error) => {
        let msg = "定位失败啦～";
        if (error.code === 1) msg += "请打开手机定位权限";
        if (error.code === 3) msg += "定位超时了，试试手动输入";
        ElMessage.warning(msg);
        locating.value = false;
      },
      { enableHighAccuracy: true, timeout: 5000, maximumAge: 0 }
  );
};

// 物种搜索优化：空结果提示
const searchSpecies = async (query) => {
  if (!query) {
    speciesOptions.value = [];
    return;
  }
  speciesLoading.value = true;
  try {
    const res = await suggestSpecies(query);
    const list = res.data?.data ?? res.data ?? [];
    speciesOptions.value = Array.isArray(list) ? list : [];
  } catch {
    speciesOptions.value = [];
    ElMessage.warning("搜索出错啦，再试一次吧～");
  } finally {
    speciesLoading.value = false;
  }
};

// 照片上传优化：自动压缩+重试
const handlePhotoChange = async (uploadFile) => {
  const file = uploadFile.raw;
  if (!file) return;

  // 1. 大小限制优化
  if (file.size / 1024 / 1024 > 10) {
    ElMessage.error("照片大小不能超过 10MB 哦，试试压缩后上传～");
    fileList.value = [];
    form.photoFile = null;
    uploadedMediaId.value = null;
    return;
  }

  form.photoFile = file;
  photoUploading.value = true;

  // 2. 上传失败自动重试1次
  let retry = 0;
  const upload = async () => {
    try {
      const res = await uploadObservationPhoto(file);
      if (res.data.success) {
        uploadedMediaId.value = res.data.data.mediaId;
        ElMessage.success("照片上传成功 ✨");
      } else {
        ElMessage.warning(res.data.message || "照片上传失败啦～");
        handlePhotoRemove();
      }
    } catch (err) {
      retry++;
      if (retry < 2) {
        upload(); // 重试1次
        return;
      }
      ElMessage.error("网络有点波动，照片上传失败了，再试一次吧～");
      handlePhotoRemove();
    } finally {
      photoUploading.value = false;
    }
  };
  upload();
};

const handlePhotoRemove = () => {
  form.photoFile = null;
  uploadedMediaId.value = null;
  fileList.value = [];
};

// 提交优化：反馈更友好
const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    if (form.photoFile && !uploadedMediaId.value) {
      ElMessage.warning("照片还在上传中，稍等一下下～");
      return;
    }

    submitting.value = true;
    try {
      const payload = { ...form, photoMediaId: uploadedMediaId.value || null };
      delete payload.photoFile;

      const res = await createObservation(payload);
      if (res.data.success) {
        ElMessage.success("发布成功啦 ✨ 快去看看你的观察记录吧～");
        // 延迟跳转，让用户看到成功提示
        setTimeout(() => {
          router.push("/profile?tab=observation");
        }, 1500);
      } else {
        ElMessage.warning(res.data.message || "发布遇到点小问题，再试一次吧～");
      }
    } catch (err) {
      ElMessage.error("提交失败啦，检查下网络再试试～");
    } finally {
      submitting.value = false;
    }
  });
};

const goBack = () => {
  // 只检查用户手动输入的字段（排除自动填充的 observedAt 和技术字段）
  const manualFields = ["title", "description", "locationName"];
  const hasManualInput = manualFields.some(key => form[key]);

  const doGoBack = () => {
    router.back();
  };

  if (hasManualInput) {
    ElMessageBox.confirm(
        "返回后已填写的内容会丢失哦～确定要返回吗？",
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        }
    ).then(() => {
      doGoBack();
    }).catch(() => {
      // 用户点击取消，不做任何操作
    });
  } else {
    doGoBack();
  }
};
</script>

<style scoped>
.publish-observation {
  padding: 12px;
  max-width: 800px;
  margin: 0 auto;
  background-color: #f9fafb; /* 页面背景更柔和 */
  min-height: 100vh;
}

.form-card {
  background: linear-gradient(180deg, #ffffff 0%, #fefefe 100%) !important;
  backdrop-filter: blur(20px) saturate(150%);
  border: none !important;
  border-radius: 20px !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03); /* 更轻的阴影 */
  padding: 24px 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 16px 0;
  border-bottom: 1px solid #f5f5f5;
  margin-bottom: 24px;
}

.back-btn {
  padding: 8px !important;
  margin-left: -8px;
  color: #666;
}

.header-title {
  font-size: 19px;
  font-weight: 600;
  color: #222;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif; /* 更贴合C端的字体 */
}

.header-placeholder {
  width: 60px;
}

/* 表单顶部提示 */
.form-tip {
  font-size: 13px;
  color: #999;
  margin-bottom: 20px;
  padding-left: 2px;
}

/* 常用地点 */
.common-locations {
  margin-top: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.common-tip {
  font-size: 13px;
  color: #999;
}

/* 折叠区域 */
.expand-section {
  margin-top: 24px;
}

.expand-btn {
  color: #409eff;
  font-size: 15px;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.more-content {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #eee;
}

/* 地理坐标优化 */
.location-group {
  margin-bottom: 22px;
  background: #f7f8fa;
  padding: 16px;
  border-radius: 12px;
}

.location-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.location-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.no-label {
  margin-bottom: 0;
}

/* 物种选项样式 */
.species-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.species-avatar {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  object-fit: cover;
}

.species-empty {
  font-size: 13px;
  color: #999;
  margin-top: 8px;
  padding-left: 4px;
}

/* 上传组件优化 */
.upload-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.upload-text {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.hide-upload-trigger :deep(.el-upload--picture-card) {
  display: none;
}

/* 提交按钮优化 */
.action-footer {
  margin-top: 32px;
  margin-bottom: 0;
  position: relative;
}

.submit-btn {
  width: 100%;
  border-radius: 16px; /* 更圆润 */
  font-size: 17px;
  font-weight: 500;
  height: 52px; /* 更大的触摸区域 */
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.25);
  background: linear-gradient(90deg, #409eff, #53a8ff); /* 渐变按钮更有C端感 */
  border: none;
}

.submit-tip {
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: #f56c6c;
}

/* 输入框优化 */
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 12px; /* 更圆润 */
  box-shadow: 0 0 0 1px #f0f0f0 inset;
  padding: 0 12px;
  height: 44px; /* 移动端触摸友好 */
}

:deep(.el-textarea__inner) {
  height: auto;
  padding: 12px;
}

:deep(.el-date-editor) {
  --el-input-wrapper-height: 44px;
}

/* 适配移动端 */
@media (max-width: 768px) {
  .publish-observation {
    padding: 8px;
  }

  .form-card {
    padding: 20px 16px;
  }

  .no-label {
    margin-bottom: 16px;
  }

  .no-label:last-child {
    margin-bottom: 0;
  }

  .submit-btn {
    height: 48px;
    font-size: 16px;
  }
}
</style>
