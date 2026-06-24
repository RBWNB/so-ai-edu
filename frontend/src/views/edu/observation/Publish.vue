<template>
  <div class="publish-observation">
    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-header">
          <el-button text @click="goBack">
            <el-icon><ArrowLeft /></el-icon> 返回
          </el-button>
          <span class="header-title">发布观察记录</span>
          <span></span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="top"
        v-loading="submitting"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="给你的观察取个名字" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="描述你观察到的内容..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="观察地点" prop="locationName">
              <el-input v-model="form.locationName" placeholder="如：深圳湾公园" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="观察时间" prop="observedAt">
              <el-date-picker
                v-model="form.observedAt"
                type="datetime"
                placeholder="选择观察时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input v-model.number="form.latitude" placeholder="如：22.5264" type="number" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input v-model.number="form.longitude" placeholder="如：113.9558" type="number" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="物种（可选）">
          <el-select
            v-model="form.speciesId"
            placeholder="搜索选择物种"
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
              :label="s.chineseName"
              :value="s.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="照片">
          <el-upload
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            :on-change="handlePhotoChange"
            :on-remove="handlePhotoRemove"
            :file-list="fileList"
            list-type="picture-card"
            accept="image/*"
            :limit="1"
            v-loading="photoUploading"
          >
            <el-icon :size="28"><Plus /></el-icon>
          </el-upload>
          <div v-if="photoUploading" class="upload-tip">正在上传到云端...</div>
        </el-form-item>

        <el-form-item style="margin-top: 16px;">
          <el-button type="primary" @click="submitForm" :loading="submitting">
            提交观察
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { ArrowLeft, Plus } from "@element-plus/icons-vue";
import { suggestSpecies } from "@/api/species";
import { createObservation, uploadObservationPhoto } from "@/api/observation";

const router = useRouter();
const formRef = ref(null);
const submitting = ref(false);
const speciesLoading = ref(false);
const speciesOptions = ref([]);
const fileList = ref([]);
const photoUploading = ref(false);
const uploadedMediaId = ref(null);

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

const rules = {
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  locationName: [{ required: true, message: "请输入观察地点", trigger: "blur" }],
  observedAt: [{ required: true, message: "请选择观察时间", trigger: "change" }],
};

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
  } finally {
    speciesLoading.value = false;
  }
};

const handlePhotoChange = async (uploadFile) => {
  const file = uploadFile.raw;
  if (!file) return;

  // 文件大小限制 10MB
  if (file.size / 1024 / 1024 > 10) {
    ElMessage.error("图片大小不能超过 10MB");
    fileList.value = [];
    form.photoFile = null;
    uploadedMediaId.value = null;
    return;
  }

  form.photoFile = file;

  // 自动上传到七牛云
  photoUploading.value = true;
  try {
    const res = await uploadObservationPhoto(file);
    if (res.data.success) {
      uploadedMediaId.value = res.data.data.mediaId;
      ElMessage.success("图片上传成功");
    } else {
      ElMessage.warning(res.data.message || "图片上传失败");
      fileList.value = [];
      form.photoFile = null;
      uploadedMediaId.value = null;
    }
  } catch (err) {
    ElMessage.error("图片上传失败，请重试");
    fileList.value = [];
    form.photoFile = null;
    uploadedMediaId.value = null;
  } finally {
    photoUploading.value = false;
  }
};

const handlePhotoRemove = () => {
  form.photoFile = null;
  uploadedMediaId.value = null;
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    // 如果选择了图片但还没上传完成，提示等待
    if (form.photoFile && !uploadedMediaId.value) {
      ElMessage.warning("图片正在上传中，请稍候...");
      return;
    }
    submitting.value = true;
    try {
      const payload = {
        title: form.title,
        description: form.description,
        locationName: form.locationName,
        observedAt: form.observedAt,
        latitude: form.latitude,
        longitude: form.longitude,
        speciesId: form.speciesId,
        photoMediaId: uploadedMediaId.value || null,
      };
      const res = await createObservation(payload);
      if (res.data.success) {
        ElMessage.success(res.data.message || "观察记录提交成功，等待审核");
        router.push("/profile?tab=observation");
      } else {
        ElMessage.warning(res.data.message || "提交失败");
      }
    } catch (err) {
      ElMessage.error("提交失败，请稍后重试");
    } finally {
      submitting.value = false;
    }
  });
};

const goBack = () => {
  router.push("/profile?tab=observation");
};
</script>

<style scoped>
.publish-observation {
  padding: 0;
}

.form-card {
  background: rgba(255, 255, 255, 0.65) !important;
  backdrop-filter: blur(24px) saturate(120%);
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  border-radius: 24px !important;
  box-shadow: 0 12px 32px rgba(0, 50, 150, 0.04);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
}

.upload-tip {
  font-size: 12px;
  color: #86909c;
  margin-top: 4px;
}
</style>
