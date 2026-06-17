<template>
  <div class="detail-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="detail-header">
          <el-button text @click="goBack">← 返回</el-button>
          <span class="header-title">物种详情</span>
          <el-button
            v-if="authStore.hasRole('ADMIN') || authStore.hasRole('MANAGER')"
            type="primary"
            @click="openEditDialog">
            编辑
          </el-button>
        </div>
      </template>

      <template v-if="species">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="中文名" :span="1">{{ species.chineseName }}</el-descriptions-item>
          <el-descriptions-item label="学名" :span="1">{{ species.scientificName }}</el-descriptions-item>
          <el-descriptions-item label="界" :span="1">{{ species.kingdom || '-' }}</el-descriptions-item>
          <el-descriptions-item label="门" :span="1">{{ species.phylum || '-' }}</el-descriptions-item>
          <el-descriptions-item label="纲" :span="1">{{ species.className || '-' }}</el-descriptions-item>
          <el-descriptions-item label="目" :span="1">{{ species.orderName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="科" :span="1">{{ species.familyName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="属" :span="1">{{ species.genusName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="种" :span="1">{{ species.speciesName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="保护等级" :span="1">
            <el-tag v-if="species.conservationStatus" :type="statusType(species.conservationStatus)" effect="dark" size="small">
              {{ species.conservationStatus }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="栖息地" :span="2">{{ species.habitat || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分布区域" :span="2">{{ species.distributionArea || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">形态特征</el-divider>
        <div class="desc-text">{{ species.morphologyDesc || '暂无描述' }}</div>

        <el-divider content-position="left">生活习性</el-divider>
        <div class="desc-text">{{ species.habitDesc || '暂无描述' }}</div>

        <el-divider v-if="species.imageUrl || species.videoUrl" content-position="left">影像资料</el-divider>
        <div v-if="species.imageUrl" class="media-section">
          <div class="media-label">物种图片：</div>
          <el-image
            :src="getImageUrl(species.imageUrl)"
            :preview-src-list="[getImageUrl(species.imageUrl)]"
            fit="contain"
            class="species-image"
            :hide-on-click-modal="true"
          />
        </div>
        <div v-if="species.videoUrl" class="media-section">
          <div class="media-label">视频链接：</div>
          <el-link :href="species.videoUrl" target="_blank" type="primary">{{ species.videoUrl }}</el-link>
        </div>

        <el-divider content-position="left">系统信息</el-divider>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="数据来源">{{ species.dataSource || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ species.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ species.updatedAt || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <el-empty v-else description="未找到该物种信息" />
    </el-card>

    <el-dialog
      v-model="editDialogVisible"
      title="编辑物种"
      width="860px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="110px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="中文名" prop="chineseName">
              <el-input v-model="editForm.chineseName" placeholder="请输入中文名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学名" prop="scientificName">
              <el-input v-model="editForm.scientificName" placeholder="请输入学名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">分类信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="界"><el-input v-model="editForm.kingdom" placeholder="动物界" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="门"><el-input v-model="editForm.phylum" placeholder="脊索动物门" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="纲"><el-input v-model="editForm.className" placeholder="哺乳纲" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="目"><el-input v-model="editForm.orderName" placeholder="鲸偶蹄目" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="科"><el-input v-model="editForm.familyName" placeholder="海豚科" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="属"><el-input v-model="editForm.genusName" placeholder="驼海豚属" /></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">生态信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="保护等级">
              <el-select v-model="editForm.conservationStatus" placeholder="请选择" clearable style="width:100%">
                <el-option label="极危 CR" value="CR" />
                <el-option label="濒危 EN" value="EN" />
                <el-option label="易危 VU" value="VU" />
                <el-option label="近危 NT" value="NT" />
                <el-option label="无危 LC" value="LC" />
                <el-option label="数据缺乏 DD" value="DD" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="栖息地">
              <el-input v-model="editForm.habitat" placeholder="近岸浅海、河口水域" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数据来源">
              <el-input v-model="editForm.dataSource" placeholder="如：IUCN" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="分布区域">
          <el-input v-model="editForm.distributionArea" placeholder="如：中国南海沿岸、珠江口、北部湾" />
        </el-form-item>

        <el-divider content-position="left">特征描述</el-divider>
        <el-form-item label="形态特征">
          <el-input v-model="editForm.morphologyDesc" type="textarea" :rows="3" placeholder="请描述形态特征" />
        </el-form-item>
        <el-form-item label="生活习性">
          <el-input v-model="editForm.habitDesc" type="textarea" :rows="3" placeholder="请描述生活习性" />
        </el-form-item>

        <el-divider content-position="left">影像资料</el-divider>
        <el-form-item label="物种图片">
          <div class="upload-wrapper">
            <el-upload
              :auto-upload="false"
              :show-file-list="false"
              accept="image/*"
              @change="handleImageChange"
            >
              <el-button type="primary">选择图片</el-button>
              <template #tip>
                <span class="upload-tip">支持 JPG/PNG，建议比例 4:3</span>
              </template>
            </el-upload>
            <div v-if="imagePreview" class="image-preview">
              <el-image :src="imagePreview" fit="contain" />
              <el-button size="small" type="danger" text @click="removeImage">移除</el-button>
            </div>
            <div v-else-if="editForm.imageUrl" class="image-preview">
              <el-image :src="getImageUrl(editForm.imageUrl)" fit="contain" />
              <el-button size="small" type="danger" text @click="removeImage">移除</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="视频链接">
          <el-input v-model="editForm.videoUrl" placeholder="请输入视频链接 URL" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getSpeciesById, updateSpecies, updateSpeciesByBody, uploadSpeciesImage } from "@/api/species";
import { useAuthStore } from "@/store/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const species = ref(null);

const editDialogVisible = ref(false);
const submitLoading = ref(false);
const imagePreview = ref("");
const imageFile = ref(null);
const editFormRef = ref();


const editForm = reactive({
  id: undefined,
  chineseName: "",
  scientificName: "",
  kingdom: "",
  phylum: "",
  className: "",
  orderName: "",
  familyName: "",
  genusName: "",
  speciesName: "",
  conservationStatus: "",
  habitat: "",
  distributionArea: "",
  morphologyDesc: "",
  habitDesc: "",
  imageUrl: "",
  videoUrl: "",
  dataSource: "",
});

const editRules = {
  chineseName: [{ required: true, message: "请输入中文名", trigger: "blur" }],
  scientificName: [{ required: true, message: "请输入学名", trigger: "blur" }],
};

const statusType = (status) => {
  const map = { CR: "danger", EN: "warning", VU: "warning", NT: "info", LC: "success" };
  return map[status] || "info";
};

const fetchDetail = async () => {
  loading.value = true;
  try {
    const id = route.params.id;
    const resp = await getSpeciesById(id);
    species.value = resp.data;
  } catch (err) {
    ElMessage.error("获取物种详情失败");
  } finally {
    loading.value = false;
  }
};

const getImageUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  if (url.startsWith('/')) {
    return `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}${url}`;
  }
  return `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/${url}`;
};

const goBack = () => {
  router.back();
};

const openEditDialog = () => {
  if (!species.value) return;
  Object.assign(editForm, species.value);
  if (editForm.imageUrl) {
    imagePreview.value = getImageUrl(editForm.imageUrl);
  } else {
    imagePreview.value = "";
  }
  imageFile.value = null;
  editDialogVisible.value = true;
};

const handleImageChange = async (uploadFile) => {
  const file = uploadFile.raw;
  if (!file) return;
  imagePreview.value = URL.createObjectURL(file);
  imageFile.value = file;
};

const removeImage = () => {
  imagePreview.value = "";
  imageFile.value = null;
  editForm.imageUrl = "";
};

const uploadImageIfNeeded = async () => {
  if (!imageFile.value) return;
  try {
    const resp = await uploadSpeciesImage(imageFile.value);
    const data = resp.data || {};
    if (data.success && data.url) {
      editForm.imageUrl = data.url;
      imageFile.value = null;
    } else if (resp.code === 200 && resp.data) {
      editForm.imageUrl = resp.data;
      imageFile.value = null;
    } else {
      ElMessage.warning(data.message || "图片上传失败，请稍后重试");
    }
  } catch (error) {
    console.error('图片上传错误:', error);
    ElMessage.warning("图片上传失败，请稍后再试");
  }
};

const handleEditSubmit = async () => {
  try {
    await editFormRef.value.validate();
  } catch {
    return;
  }

  submitLoading.value = true;
  try {
    if (imageFile.value) {
      await uploadImageIfNeeded();
    }

    const payload = {
      id: editForm.id,
      chineseName: editForm.chineseName?.trim() || null,
      scientificName: editForm.scientificName?.trim() || null,
      kingdom: editForm.kingdom?.trim() || null,
      phylum: editForm.phylum?.trim() || null,
      className: editForm.className?.trim() || null,
      orderName: editForm.orderName?.trim() || null,
      familyName: editForm.familyName?.trim() || null,
      genusName: editForm.genusName?.trim() || null,
      speciesName: editForm.speciesName?.trim() || null,
      conservationStatus: editForm.conservationStatus?.trim() || null,
      habitat: editForm.habitat?.trim() || null,
      distributionArea: editForm.distributionArea?.trim() || null,
      morphologyDesc: editForm.morphologyDesc?.trim() || null,
      habitDesc: editForm.habitDesc?.trim() || null,
      imageUrl: editForm.imageUrl === "" ? "" : (editForm.imageUrl?.trim() || null),
      videoUrl: typeof editForm.videoUrl === 'string' ? (editForm.videoUrl.trim() || null) : null,
      dataSource: typeof editForm.dataSource === 'string' ? (editForm.dataSource.trim() || null) : null,
    };

    await updateSpecies(editForm.id, payload);

    ElMessage.success("更新成功");
    editDialogVisible.value = false;
    await fetchDetail();
  } catch (error) {
    console.error('更新失败:', error);
    ElMessage.error("更新失败");
  } finally {
    submitLoading.value = false;
  }
};

onMounted(fetchDetail);
</script>

<style scoped>
.detail-page {
  max-width: 960px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-title {
  flex: 1;
  font-size: 18px;
  font-weight: 600;
  color: var(--theme-text-primary);
}

.desc-text {
  white-space: pre-wrap;
  line-height: 1.8;
  color: var(--theme-text-secondary);
  padding: 0 4px;
}

.media-section {
  margin: 8px 0;
}

.media-label {
  font-weight: 500;
  margin-bottom: 6px;
  color: var(--theme-text-primary);
}

.species-image {
  max-width: 400px;
  max-height: 300px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.upload-wrapper {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-tip {
  font-size: 12px;
  color: var(--theme-text-muted);
  margin-left: 8px;
}

.image-preview {
  display: flex;
  align-items: center;
  gap: 10px;
}

.image-preview .el-image {
  max-width: 200px;
  max-height: 120px;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

:deep(.el-card) {
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(12px) !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
}

:deep(.el-card__header) {
  background: transparent !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15) !important;
}

:deep(.el-dialog) {
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(12px) !important;
}
</style>
