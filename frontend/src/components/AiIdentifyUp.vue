<template>
  <el-card class="identify-card" shadow="hover">
    <template #header>
      <div class="header-row">
        <span class="title">海洋生物图像识别</span>
        <el-button type="primary" :loading="loading" :disabled="!file" @click="handleIdentify">
          开始识别
        </el-button>
      </div>
    </template>

    <el-upload
      class="upload"
      drag
      action="#"
      :auto-upload="false"
      :show-file-list="false"
      :on-change="handleFileChange"
      :on-remove="handleRemove"
      accept="image/*"
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div class="el-upload__text">将图片拖到此处，或 <em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip">支持常见图片格式，建议小于 10MB</div>
      </template>
    </el-upload>

    <div v-if="previewUrl" class="preview-wrap">
      <img :src="previewUrl" alt="预览图" class="preview-img" />
    </div>

    <el-empty v-if="!result && !loading" description="上传图片后可进行AI识别" />

    <el-skeleton v-if="loading" :rows="4" animated style="margin-top: 14px" />

    <el-card v-if="result && !loading" class="result-card" shadow="never">
      <div class="result-head">
        <div>
          <div class="species-name">{{ result.speciesName || "未知物种" }}</div>
          <div class="scientific-name" v-if="result.scientificName">{{ result.scientificName }}</div>
        </div>
        <el-tag :type="confidenceTagType(result.confidence)">
          置信度：{{ confidenceText(result.confidence) }}
        </el-tag>
      </div>

      <div class="result-content">
        <div class="result-desc">{{ result.summary || "暂无描述" }}</div>
      </div>

      <!-- 物种匹配结果 -->
      <div v-if="matchedSpecies !== null" class="match-section">
        <div v-if="matchedSpecies" class="match-found">
          <el-tag type="success" effect="plain" class="match-tag">该物种已收录</el-tag>
          <el-button
            type="success"
            size="small"
            @click="goToDetail(matchedSpecies.id)"
          >
            🔍 查看该物种详情
          </el-button>
          <el-button type="warning" size="small" @click="goToPublish">
            📝 发布到观察帖子
          </el-button>
        </div>
        <div v-else class="match-not-found">
          <el-tag type="info" effect="plain" class="match-tag">该物种暂未收录</el-tag>
          <el-button type="warning" size="small" @click="goToPublish">
            📝 发布到观察帖子
          </el-button>
        </div>
      </div>
      <div v-else-if="lookingUp" class="match-section">
        <el-tag type="info" effect="plain" class="match-tag lookup-tag">
          <el-icon class="loading-icon"><Loading /></el-icon>
          正在查询物种库...
        </el-tag>
      </div>
    </el-card>

    <!-- 物种详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="物种详情"
      width="720px"
      destroy-on-close
      append-to-body
      class="ai-detail-dialog"
      :close-on-click-modal="true"
    >
      <div v-loading="detailLoading" class="detail-content">
        <template v-if="detailData">
          <!-- 头图区 -->
          <div class="detail-hero">
            <div class="hero-image-wrap">
              <el-image
                v-if="detailData.imageUrl"
                :src="getImageUrl(detailData.imageUrl)"
                fit="cover"
                class="hero-image"
              >
                <template #error><div class="hero-placeholder"><el-icon :size="60"><Picture /></el-icon></div></template>
              </el-image>
              <div v-else class="hero-placeholder hero-full"><el-icon :size="60"><Picture /></el-icon></div>
              <div class="hero-gradient"></div>
            </div>
            <div class="hero-info">
              <h2 class="hero-name">{{ detailData.chineseName || '未命名物种' }}</h2>
              <p class="hero-latin">{{ detailData.scientificName || '' }}</p>
              <div class="hero-tags">
                <el-tag v-if="detailData.conservationStatus" :type="conservationTagType(detailData.conservationStatus)" effect="dark" round size="small">
                  {{ detailData.conservationStatus }}
                </el-tag>
                <el-tag v-if="detailData.phylum" type="info" effect="plain" round size="small">{{ detailData.phylum }}</el-tag>
                <el-tag v-if="detailData.habitat" type="" effect="plain" round size="small">{{ detailData.habitat }}</el-tag>
              </div>
            </div>
          </div>

          <!-- 信息面板 -->
          <div class="detail-info">
            <el-descriptions :column="2" border size="small" class="desc-table">
              <el-descriptions-item label="界">{{ detailData.kingdom || '-' }}</el-descriptions-item>
              <el-descriptions-item label="门">{{ detailData.phylum || '-' }}</el-descriptions-item>
              <el-descriptions-item label="纲">{{ detailData.className || '-' }}</el-descriptions-item>
              <el-descriptions-item label="目">{{ detailData.orderName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="科">{{ detailData.familyName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="属">{{ detailData.genusName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="分布区域" :span="2">{{ detailData.distributionArea || '-' }}</el-descriptions-item>
              <el-descriptions-item label="数据来源">{{ detailData.dataSource || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 特征描述 -->
          <div class="detail-sections" v-if="detailData.morphologyDesc || detailData.habitDesc">
            <div v-if="detailData.morphologyDesc" class="detail-section">
              <h4 class="section-title"><el-icon><EditPen /></el-icon>形态特征</h4>
              <p class="section-body">{{ detailData.morphologyDesc }}</p>
            </div>
            <div v-if="detailData.habitDesc" class="detail-section">
              <h4 class="section-title"><el-icon><Sunny /></el-icon>生活习性</h4>
              <p class="section-body">{{ detailData.habitDesc }}</p>
            </div>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { Loading, UploadFilled, Picture, EditPen, Sunny } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { identifySpecies, identifySpeciesByImage } from "@/api/ai";
import { suggestSpecies, getSpeciesById } from "@/api/species";
import { uploadObservationPhoto } from "@/api/observation";

const router = useRouter();

const file = ref(null);
const previewUrl = ref("");
const loading = ref(false);
const result = ref(null);
const matchedSpecies = ref(null); // null=未查询, object=找到, null-ish=未找到
const lookingUp = ref(false);

// 详情弹窗状态
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailData = ref(null);

const handleFileChange = (uploadFile) => {
  const raw = uploadFile?.raw;
  if (!raw) {
    return;
  }
  file.value = raw;
  result.value = null;
  matchedSpecies.value = null;
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
  }
  previewUrl.value = URL.createObjectURL(raw);
};

const handleRemove = () => {
  file.value = null;
  result.value = null;
  matchedSpecies.value = null;
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value);
  }
  previewUrl.value = "";
};

const confidenceText = (confidence) => {
  if (confidence === undefined || confidence === null || Number.isNaN(Number(confidence))) {
    return "未知";
  }
  const c = Number(confidence);
  if (c <= 1) {
    return `${(c * 100).toFixed(1)}%`;
  }
  return `${c.toFixed(1)}%`;
};

const confidenceTagType = (confidence) => {
  const c = Number(confidence);
  if (Number.isNaN(c)) {
    return "info";
  }
  const normalized = c <= 1 ? c : c / 100;
  if (normalized >= 0.8) {
    return "success";
  }
  if (normalized >= 0.5) {
    return "warning";
  }
  return "danger";
};

const parseIdentifyResponse = (data) => {
  if (typeof data === "string") {
    return {
      speciesName: data,
      scientificName: "",
      confidence: null,
      summary: "",
      rawText: data,
    };
  }

  const payload = data?.data || data?.result || data || {};
  return {
    speciesName:
      payload.speciesName ||
      payload.name ||
      payload.chineseName ||
      payload.species ||
      payload.label ||
      "未知物种",
    scientificName: payload.scientificName || payload.sciName || "",
    confidence: payload.confidence ?? payload.score ?? payload.probability ?? null,
    summary: payload.summary || payload.description || payload.intro || "",
    rawText:
      payload.rawText || payload.raw || (typeof payload === "object" ? JSON.stringify(payload) : String(payload)),
  };
};

const lookUpSpecies = async (name) => {
  if (!name || name === "无法识别") {
    matchedSpecies.value = null;
    return;
  }
  lookingUp.value = true;
  matchedSpecies.value = null;
  try {
    const resp = await suggestSpecies(name);
    const candidates = resp?.data?.data || resp?.data?.result || resp?.data || [];
    const list = Array.isArray(candidates) ? candidates : Array.isArray(candidates.records) ? candidates.records : [];
    const match = list.find(
      (s) => s.chineseName === name || s.speciesName === name
    );
    matchedSpecies.value = match || false;
  } catch {
    matchedSpecies.value = false;
  } finally {
    lookingUp.value = false;
  }
};

const handleIdentify = async () => {
  if (!file.value) {
    ElMessage.warning("请先上传图片");
    return;
  }

  loading.value = true;
  try {
    let response;
    try {
      response = await identifySpeciesByImage(file.value);
    } catch {
      response = await identifySpecies({ fileName: file.value.name });
    }
    const parsed = parseIdentifyResponse(response?.data);
    result.value = parsed;
    ElMessage.success("识别完成");

    // 识别成功后查询物种库
    lookUpSpecies(parsed.speciesName);
  } catch (error) {
    console.error(error);
    ElMessage.error("识别失败，请稍后重试");
  } finally {
    loading.value = false;
  }
};

const getImageUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  if (url.startsWith('/uploads/')) return `/api${url}`;
  if (url.startsWith('/')) return `/api/uploads${url}`;
  return `/api/uploads/${url}`;
};

const conservationTagType = (status) => {
  const map = { CR: 'danger', EN: 'warning', VU: 'warning', NT: 'info', LC: 'success' };
  return map[status] || 'info';
};

const goToDetail = async (id) => {
  if (!id) return;
  detailLoading.value = true;
  detailData.value = null;
  detailVisible.value = true;

  try {
    const res = await getSpeciesById(id);
    detailData.value = res.data;
  } catch (error) {
    console.error('获取物种详情失败:', error);
    // fallback: 用当前列表中的数据
    if (matchedSpecies.value && matchedSpecies.value.id === id) {
      detailData.value = { ...matchedSpecies.value };
    }
  } finally {
    detailLoading.value = false;
  }
};

const goToPublish = async () => {
  const info = result.value || {};
  // 组合 AI 识别结果：物种名 + 拉丁名 + 描述
  const descParts = [];
  if (info.speciesName && info.speciesName !== "未知物种") {
    descParts.push(`【AI 识别】物种：${info.speciesName}`);
  }
  if (info.scientificName) {
    descParts.push(`拉丁名：${info.scientificName}`);
  }
  if (info.summary) {
    descParts.push(`描述：${info.summary}`);
  }
  const description = descParts.join("\n");

  // 先上传当前图片，获取 mediaId
  let mediaId = '';
  let photoUrl = '';
  if (file.value) {
    try {
      ElMessage.info('正在上传图片...');
      const res = await uploadObservationPhoto(file.value);
      if (res.data.success) {
        mediaId = res.data.data.mediaId || '';
        photoUrl = res.data.data.photoUrl || '';
      }
    } catch (e) {
      console.error('上传照片失败', e);
    }
  }

  router.push({
    path: "/observation/publish",
    query: {
      aiDescription: description,
      mediaId,
      photoUrl,
      from: "ai-assistant",
    },
  });
};
</script>

<style scoped>
.identify-card {
  border-radius: 12px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.upload {
  margin-bottom: 14px;
}

.upload-icon {
  font-size: 34px;
  color: #409eff;
}

.preview-wrap {
  margin: 10px 0 16px;
  display: flex;
  justify-content: center;
}

.preview-img {
  width: 100%;
  max-height: 260px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.result-card {
  margin-top: 6px;
  background: linear-gradient(180deg, #f8fbff 0%, #ffffff 100%);
  border: 1px solid #e7efff;
}

.result-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.species-name {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
}

.scientific-name {
  color: #6b7280;
  margin-top: 3px;
}

.result-content {
  margin-top: 4px;
}

.result-desc {
  line-height: 1.8;
  color: #374151;
  font-size: 14px;
  text-align: justify;
}

.match-section {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.match-found {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.match-not-found {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.match-tag {
  font-size: 12px;
  white-space: nowrap;
}

.lookup-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>

<!-- ═════════ 全局弹窗样式（dialog 被 Teleport 到 body 下） ═════════ -->
<style>
.ai-detail-dialog {
  border-radius: 20px !important;
  overflow: hidden;
  background: rgb(8, 22, 38) !important;
  --el-dialog-bg-color: rgb(8, 22, 38) !important;
  --el-bg-color: rgb(8, 22, 38) !important;
  --el-bg-color-overlay: rgb(8, 22, 38) !important;
  backdrop-filter: blur(28px);
  border: 1px solid rgba(0, 180, 216, 0.3) !important;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6) !important;
}

.ai-detail-dialog .el-dialog__header {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding: 18px 24px 14px;
  margin: 0;
  background: rgb(8, 22, 38) !important;
}

.ai-detail-dialog .el-dialog__title {
  font-weight: 700;
  font-size: 18px;
  color: #ffffff !important;
}

.ai-detail-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
}

.ai-detail-dialog .el-dialog__headerbtn .el-dialog__close:hover {
  color: #ffffff;
}

.ai-detail-dialog .el-dialog__body {
  padding: 0 !important;
  background: rgb(8, 22, 38) !important;
}

.ai-detail-dialog .el-dialog__footer {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding: 14px 24px 18px;
  background: rgb(8, 22, 38) !important;
}

.ai-detail-dialog .detail-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 24px;
  background: rgb(6, 18, 32) !important;
}

.ai-detail-dialog .detail-content::-webkit-scrollbar {
  width: 5px;
}

.ai-detail-dialog .detail-content::-webkit-scrollbar-thumb {
  background: rgba(0, 180, 216, 0.35);
  border-radius: 4px;
}

.ai-detail-dialog .detail-hero {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.ai-detail-dialog .hero-image-wrap {
  width: 220px;
  height: 150px;
  border-radius: 14px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
}

.ai-detail-dialog .hero-image {
  width: 100%;
  height: 100%;
}

.ai-detail-dialog .hero-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.15);
  background: linear-gradient(145deg, rgba(0, 50, 80, 0.4), rgba(0, 90, 130, 0.25));
}

.ai-detail-dialog .hero-placeholder.hero-full {
  border-radius: 14px;
}

.ai-detail-dialog .hero-gradient {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 40%;
  background: linear-gradient(to top, rgba(0, 20, 40, 0.5), transparent);
  pointer-events: none;
}

.ai-detail-dialog .hero-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.ai-detail-dialog .hero-name {
  font-size: 22px;
  font-weight: 800;
  margin: 0;
  color: #f1f5f9;
}

.ai-detail-dialog .hero-latin {
  font-size: 14px;
  font-style: italic;
  color: #48cae4;
  margin: 0;
}

.ai-detail-dialog .hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-detail-dialog .detail-info {
  margin-bottom: 18px;
}

.ai-detail-dialog .detail-sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-detail-dialog .detail-section {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px 18px;
  transition: all 0.25s ease;
}

.ai-detail-dialog .detail-section:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(0, 180, 216, 0.3);
}

.ai-detail-dialog .section-title {
  font-size: 14px;
  font-weight: 700;
  color: #48cae4;
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-detail-dialog .section-body {
  font-size: 13.5px;
  line-height: 1.75;
  color: #e2e8f0;
  margin: 0;
  white-space: pre-wrap;
}

/* el-descriptions 深色覆盖 */
.ai-detail-dialog .detail-info .el-descriptions {
  --el-descriptions-table-border: 1px solid rgba(255, 255, 255, 0.1) !important;
  --el-fill-color-blank: transparent !important;
  --el-border-color: rgba(255, 255, 255, 0.1) !important;
  --el-text-color-primary: rgba(255, 255, 255, 0.9) !important;
  --el-text-color-regular: rgba(255, 255, 255, 0.85) !important;
  --el-bg-color: transparent !important;
  --el-bg-color-overlay: transparent !important;
  background: transparent !important;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
}

.ai-detail-dialog .detail-info .el-descriptions table {
  background: transparent !important;
  border-collapse: collapse;
}

.ai-detail-dialog .detail-info .el-descriptions table th,
.ai-detail-dialog .detail-info .el-descriptions table td {
  background: transparent !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: rgba(255, 255, 255, 0.88) !important;
  transition: background 0.2s;
}

.ai-detail-dialog .detail-info .el-descriptions__label {
  background: rgba(0, 110, 170, 0.18) !important;
  color: #48cae4 !important;
  font-weight: 650 !important;
  font-size: 13px !important;
  width: 90px;
  padding: 10px 14px !important;
}

.ai-detail-dialog .detail-info .el-descriptions__content {
  background: rgba(255, 255, 255, 0.02) !important;
  color: rgba(255, 255, 255, 0.92) !important;
  font-size: 13.5px !important;
  padding: 10px 16px !important;
}

.ai-detail-dialog .detail-info .el-descriptions table tr:hover th,
.ai-detail-dialog .detail-info .el-descriptions table tr:hover td {
  background: rgba(0, 180, 216, 0.06) !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .ai-detail-dialog {
    width: 94% !important;
    margin-top: 3vh !important;
  }

  .ai-detail-dialog .detail-hero {
    flex-direction: column;
    gap: 14px;
  }

  .ai-detail-dialog .hero-image-wrap {
    width: 100%;
    height: 200px;
  }

  .ai-detail-dialog .detail-info .el-descriptions__label {
    width: 70px !important;
    padding: 8px 10px !important;
    font-size: 12px !important;
  }

  .ai-detail-dialog .detail-info .el-descriptions__content {
    padding: 8px 10px !important;
    font-size: 12.5px !important;
  }
}
</style>
