<template>
  <div class="scanner-container">
    <div class="scanner-viewport" :class="{ 'is-analyzing': loading, 'has-image': previewUrl }">

      <div class="hud-corner top-left"></div>
      <div class="hud-corner top-right"></div>
      <div class="hud-corner bottom-left"></div>
      <div class="hud-corner bottom-right"></div>

      <el-upload
          v-if="!previewUrl"
          class="scanner-upload"
          drag
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleFileChange"
          :on-remove="handleRemove"
          accept="image/*"
      >
        <div class="upload-content">
          <div class="upload-icon-pulse">
            <el-icon><UploadFilled /></el-icon>
          </div>
          <div class="upload-title">投放图像样本</div>
          <div class="upload-sub">点击或拖拽至此区域 (Max: 10MB)</div>
        </div>
      </el-upload>

      <div v-else class="viewport-image-wrap">
        <img :src="previewUrl" alt="探测样本" class="viewport-img" />

        <button v-if="!loading && !result" class="reselect-btn" @click="handleRemove">
          <el-icon><Close /></el-icon> 重新选择
        </button>

        <template v-if="loading">
          <div class="laser-beam"></div>
          <div class="scan-grid"></div>
          <div class="analysis-status">
            <div class="status-icon"><el-icon class="is-loading"><Loading /></el-icon></div>
            <div class="status-text">
              <span class="glitch-text" data-text="正在提取生物特征...">正在提取生物特征...</span>
              <span class="status-sub">匹配海洋学堂基因库</span>
            </div>
          </div>
        </template>
      </div>
    </div>

    <div class="scanner-console" v-if="previewUrl && !result && !loading">
      <button class="ignition-btn" @click="handleIdentify">
        <el-icon class="ignition-icon"><Aim /></el-icon>
        启动智能分析
      </button>
    </div>

    <Transition name="slide-up">
      <div v-if="result && !loading" class="report-card">
        <div class="report-header">
          <div class="report-title-area">
            <div class="report-tag">
              <el-icon><DataLine /></el-icon> 鉴定报告
            </div>
            <h2 class="species-title">{{ result.speciesName || "未知物种" }}</h2>
            <div class="species-latin" v-if="result.scientificName">{{ result.scientificName }}</div>
          </div>

          <div class="confidence-badge" :class="confidenceTagType(result.confidence)">
            <svg class="ring-chart" viewBox="0 0 36 36">
              <path class="ring-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
              <path class="ring-fill" :stroke-dasharray="(result.confidence > 1 ? result.confidence : result.confidence * 100) + ', 100'" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
            </svg>
            <div class="badge-content">
              <span class="badge-num">{{ confidenceText(result.confidence) }}</span>
              <span class="badge-label">置信度</span>
            </div>
          </div>
        </div>

        <div class="report-summary">
          <p>{{ result.summary || "特征数据不足，无法生成详细描述。" }}</p>
        </div>

        <div class="report-actions">
          <div v-if="matchedSpecies !== null" class="action-panel">
            <div v-if="matchedSpecies" class="match-status success">
              <el-icon class="status-dot"><Check /></el-icon> 基因库匹配成功
            </div>
            <div v-else class="match-status warning">
              <el-icon class="status-dot"><WarningFilled /></el-icon> 未收录此物种
            </div>

            <div class="action-btns">
              <button v-if="matchedSpecies" class="btn-hologram primary" @click="goToDetail(matchedSpecies.id)">
                <el-icon><Reading /></el-icon> 查阅图鉴
              </button>
              <button class="btn-hologram secondary" @click="goToPublish">
                <el-icon><EditPen /></el-icon> 记录观察
              </button>
              <button class="btn-hologram outline" @click="handleRemove">
                重新识别
              </button>
            </div>
          </div>

          <div v-else-if="lookingUp" class="action-panel loading">
            <el-icon class="is-loading"><Loading /></el-icon> 正在与知识图谱进行基因核对...
          </div>
        </div>
      </div>
    </Transition>
  </div>
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
      <el-button class="glass-btn-close" @click="detailVisible = false">关闭图鉴</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { Loading, UploadFilled, Picture, EditPen, Sunny, View, Check, Aim, DataLine, Close, WarningFilled, Reading } from "@element-plus/icons-vue";
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
 .scanner-container {
   display: flex;
   flex-direction: column;
   gap: 24px;
 }

/* ════════ 1. 全息探测视口 (HUD Viewport) ════════ */
.scanner-viewport {
  position: relative;
  width: 100%;
  min-height: 300px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 16px;
  box-shadow: 0 12px 32px rgba(0, 50, 150, 0.05), inset 0 0 0 1px rgba(255, 255, 255, 0.8);
  transition: all 0.5s cubic-bezier(0.2, 0.8, 0.2, 1);
  overflow: hidden;
}

.scanner-viewport.has-image {
  background: #f7f9fc; /* 有图片时背景变深一点点凸显图片 */
  padding: 8px;
}

.scanner-viewport.is-analyzing {
  box-shadow: 0 12px 40px rgba(0, 210, 255, 0.15), inset 0 0 0 2px rgba(0, 210, 255, 0.4);
}

/* 科技感四角瞄准框 */
.hud-corner {
  position: absolute;
  width: 30px;
  height: 30px;
  border: 3px solid #c9cdd4;
  border-radius: 4px;
  z-index: 10;
  transition: all 0.5s ease;
  pointer-events: none;
}
.top-left { top: 16px; left: 16px; border-right: none; border-bottom: none; }
.top-right { top: 16px; right: 16px; border-left: none; border-bottom: none; }
.bottom-left { bottom: 16px; left: 16px; border-right: none; border-top: none; }
.bottom-right { bottom: 16px; right: 16px; border-left: none; border-top: none; }

.scanner-viewport.is-analyzing .hud-corner {
  border-color: #00d2ff;
  box-shadow: 0 0 12px rgba(0, 210, 255, 0.6);
  width: 40px; height: 40px; /* 扫描时角框变大瞄准 */
}

 /* 有图片时，让四个角贴近边缘 */
 .scanner-viewport.has-image .top-left { top: 8px; left: 8px; }
 .scanner-viewport.has-image .top-right { top: 8px; right: 8px; }
 .scanner-viewport.has-image .bottom-left { bottom: 8px; left: 8px; }
 .scanner-viewport.has-image .bottom-right { bottom: 8px; right: 8px; }

/* 自定义拖拽上传区 */
.scanner-upload { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; }
:deep(.el-upload-dragger) {
  background: transparent !important;
  border: none !important;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.upload-content {
  text-align: center;
  padding: 40px;
}
.upload-icon-pulse {
  width: 80px; height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, rgba(22, 93, 255, 0.1), rgba(0, 210, 255, 0.2));
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 40px;
  color: #165dff;
  box-shadow: 0 0 0 0 rgba(22, 93, 255, 0.2);
  animation: pulseIcon 2s infinite;
  transition: transform 0.3s;
}
:deep(.el-upload-dragger:hover) .upload-icon-pulse { transform: scale(1.1); }

@keyframes pulseIcon {
  0% { box-shadow: 0 0 0 0 rgba(22, 93, 255, 0.4); }
  70% { box-shadow: 0 0 0 20px rgba(22, 93, 255, 0); }
  100% { box-shadow: 0 0 0 0 rgba(22, 93, 255, 0); }
}

.upload-title { font-size: 20px; font-weight: 800; color: #1d2129; margin-bottom: 8px; }
.upload-sub { font-size: 14px; color: #86909c; }

/* 图像预览区 */
.viewport-image-wrap {
  position: relative;
  width: 100%;
  height: 400px;
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.viewport-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 12px;
  z-index: 1;
}

.reselect-btn {
  position: absolute;
  top: 16px; right: 16px;
  z-index: 20;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  border: none;
  color: #fff;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  display: flex; align-items: center; gap: 6px;
  cursor: pointer;
  transition: all 0.3s;
}
.reselect-btn:hover { background: rgba(245, 63, 63, 0.8); }

/* ── 超酷的激光扫描特效 ── */
.scan-grid {
  position: absolute; inset: 0; z-index: 2;
  background-image:
      linear-gradient(rgba(0, 210, 255, 0.1) 1px, transparent 1px),
      linear-gradient(90deg, rgba(0, 210, 255, 0.1) 1px, transparent 1px);
  background-size: 30px 30px;
  opacity: 0;
  animation: gridFade 0.5s forwards;
}
.laser-beam {
  position: absolute; top: 0; left: 0; right: 0; height: 6px; z-index: 3;
  background: #00d2ff;
  box-shadow: 0 0 20px 4px #00d2ff, 0 100px 50px -50px rgba(0, 210, 255, 0.2) inset;
  animation: laserScan 2.5s ease-in-out infinite alternate;
}

.analysis-status {
  position: absolute; z-index: 5;
  top: 50%; left: 50%; transform: translate(-50%, -50%);
  background: rgba(10, 20, 42, 0.85);
  backdrop-filter: blur(12px);
  padding: 20px 32px;
  border-radius: 24px;
  border: 1px solid rgba(0, 210, 255, 0.4);
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.4), 0 0 0 4px rgba(0, 210, 255, 0.1);
}
.status-icon { font-size: 28px; color: #00d2ff; }
.status-text { display: flex; flex-direction: column; }
.glitch-text { font-size: 16px; font-weight: 700; color: #fff; letter-spacing: 1px; }
.status-sub { font-size: 12px; color: rgba(0, 210, 255, 0.7); margin-top: 4px; }

@keyframes laserScan {
  0% { transform: translateY(0); }
  100% { transform: translateY(394px); }
}
@keyframes gridFade { to { opacity: 1; } }

/* ════════ 2. 点火控制台 (启动按钮) ════════ */
.scanner-console {
  display: flex; justify-content: center;
}
.ignition-btn {
  background: linear-gradient(135deg, #165dff, #00d2ff);
  border: none; color: #fff;
  font-size: 18px; font-weight: 800; letter-spacing: 2px;
  padding: 18px 48px; border-radius: 40px;
  display: flex; align-items: center; gap: 10px;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(22, 93, 255, 0.3), inset 0 2px 2px rgba(255,255,255,0.4);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.ignition-btn:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 16px 32px rgba(22, 93, 255, 0.4), inset 0 2px 2px rgba(255,255,255,0.4);
}
.ignition-icon { font-size: 24px; animation: spinIcon 6s linear infinite; }
@keyframes spinIcon { 100% { transform: rotate(360deg); } }

/* ════════ 3. 鉴定报告档案卡 (SSR 展示) ════════ */
.report-card {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(24px);
  border: 1px solid #fff;
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 24px 48px rgba(0, 50, 150, 0.08);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.report-tag {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 700; color: #165dff;
  background: rgba(22, 93, 255, 0.08);
  padding: 6px 12px; border-radius: 8px; margin-bottom: 12px;
}

.species-title { font-size: 32px; font-weight: 900; color: #1d2129; margin: 0 0 4px; letter-spacing: 1px; }
.species-latin { font-size: 16px; font-style: italic; color: #86909c; font-weight: 500; }

/* 环形置信度徽章 */
.confidence-badge {
  position: relative; width: 80px; height: 80px; flex-shrink: 0;
}
.ring-chart { width: 100%; height: 100%; transform: rotate(-90deg); }
.ring-bg { fill: none; stroke: rgba(0,0,0,0.05); stroke-width: 3; }
.ring-fill { fill: none; stroke: currentColor; stroke-width: 3; stroke-linecap: round; transition: stroke-dasharray 1s ease-out; }

.badge-content {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.badge-num { font-size: 18px; font-weight: 900; color: #1d2129; line-height: 1.1; }
.badge-label { font-size: 11px; color: #86909c; font-weight: 600; }

.confidence-badge.success { color: #00b42a; }
.confidence-badge.warning { color: #ff7d00; }
.confidence-badge.danger { color: #f53f3f; }

.report-summary {
  background: #f7f9fc;
  border-left: 4px solid #165dff;
  padding: 20px 24px;
  border-radius: 0 16px 16px 0;
  font-size: 16px; color: #4e5969; line-height: 1.8; font-weight: 500;
  margin-bottom: 24px;
}

/* 底部操作台 */
.report-actions {
  border-top: 1px dashed rgba(0,0,0,0.1);
  padding-top: 24px;
}
.action-panel {
  display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px;
}
.match-status { font-size: 15px; font-weight: 700; display: flex; align-items: center; gap: 8px; }
.match-status.success { color: #00b42a; }
.match-status.warning { color: #ff7d00; }
.status-dot { font-size: 18px; }

.action-btns { display: flex; gap: 12px; }

/* 胶囊幻彩按钮 */
.btn-hologram {
  border: none; padding: 10px 24px; border-radius: 20px; font-size: 14px; font-weight: 600;
  display: flex; align-items: center; gap: 6px; cursor: pointer; transition: all 0.3s;
}
.btn-hologram.primary { background: linear-gradient(135deg, #165dff, #00d2ff); color: #fff; box-shadow: 0 4px 12px rgba(22, 93, 255, 0.3); }
.btn-hologram.primary:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(22, 93, 255, 0.4); }
.btn-hologram.secondary { background: rgba(22, 93, 255, 0.1); color: #165dff; }
.btn-hologram.secondary:hover { background: rgba(22, 93, 255, 0.15); transform: translateY(-2px); }
.btn-hologram.outline { background: transparent; border: 1px solid #c9cdd4; color: #4e5969; }
.btn-hologram.outline:hover { background: #f2f3f5; color: #1d2129; }

/* 过渡动画 */
.slide-up-enter-active, .slide-up-leave-active { transition: all 0.5s cubic-bezier(0.2, 0.8, 0.2, 1); }
.slide-up-enter-from { opacity: 0; transform: translateY(30px); }
.slide-up-leave-to { opacity: 0; transform: translateY(-30px); }

/* 响应式 */
@media (max-width: 768px) {
  .scanner-viewport { min-height: 250px; }
  .viewport-image-wrap { height: 300px; }
  .report-header { flex-direction: column; gap: 16px; }
  .action-panel { flex-direction: column; align-items: flex-start; }
  .action-btns { width: 100%; flex-wrap: wrap; }
  .btn-hologram { flex: 1; justify-content: center; }
}
</style>

<style>
.ai-detail-dialog {
  border-radius: 24px !important;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.85) !important;
  backdrop-filter: blur(30px) saturate(150%) !important;
  -webkit-backdrop-filter: blur(30px) saturate(150%) !important;
  border: 1px solid rgba(255, 255, 255, 1) !important;
  box-shadow: 0 24px 60px rgba(0, 50, 150, 0.15) !important;
}

.ai-detail-dialog .el-dialog__header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding: 20px 24px;
  margin: 0;
  background: transparent !important;
}

.ai-detail-dialog .el-dialog__title {
  font-weight: 800;
  font-size: 18px;
  color: #1d2129 !important;
}

.ai-detail-dialog .el-dialog__headerbtn .el-dialog__close {
  color: #86909c;
  font-size: 18px;
}

.ai-detail-dialog .el-dialog__headerbtn .el-dialog__close:hover {
  color: #165dff;
}

.ai-detail-dialog .el-dialog__body {
  padding: 0 !important;
  background: transparent !important;
}

.ai-detail-dialog .el-dialog__footer {
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  padding: 16px 24px;
  background: transparent !important;
}

.ai-detail-dialog .detail-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 24px;
}

.ai-detail-dialog .detail-content::-webkit-scrollbar { width: 6px; }
.ai-detail-dialog .detail-content::-webkit-scrollbar-thumb { background: rgba(0, 0, 0, 0.15); border-radius: 10px; }

.ai-detail-dialog .detail-hero { display: flex; gap: 24px; margin-bottom: 24px; }

.ai-detail-dialog .hero-image-wrap {
  width: 220px;
  height: 160px;
  border-radius: 16px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  box-shadow: 0 8px 20px rgba(0,0,0,0.06);
}
.ai-detail-dialog .hero-image { width: 100%; height: 100%; }

.ai-detail-dialog .hero-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  color: #c9cdd4;
  background: #f7f8fa;
}

.ai-detail-dialog .hero-info { display: flex; flex-direction: column; justify-content: center; gap: 8px; }
.ai-detail-dialog .hero-name { font-size: 24px; font-weight: 800; margin: 0; color: #1d2129; }
.ai-detail-dialog .hero-latin { font-size: 15px; font-style: italic; color: #86909c; margin: 0; }
.ai-detail-dialog .hero-tags { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 4px;}

.ai-detail-dialog .detail-sections { display: flex; flex-direction: column; gap: 16px; margin-top: 20px; }
.ai-detail-dialog .detail-section {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 16px 20px;
  box-shadow: 0 4px 12px rgba(0, 50, 150, 0.02);
}

.ai-detail-dialog .section-title {
  font-size: 15px; font-weight: 700; color: #165dff; margin: 0 0 10px; display: flex; align-items: center; gap: 6px;
}
.ai-detail-dialog .section-body { font-size: 14px; line-height: 1.8; color: #4e5969; margin: 0; white-space: pre-wrap; }

/* el-descriptions 浅色玻璃覆盖 */
.ai-detail-dialog .detail-info .el-descriptions {
  --el-descriptions-table-border: 1px solid rgba(0, 0, 0, 0.04) !important;
  background: rgba(255, 255, 255, 0.6) !important;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  box-shadow: 0 4px 12px rgba(0, 50, 150, 0.02);
}
.ai-detail-dialog .detail-info .el-descriptions table { background: transparent !important; }
.ai-detail-dialog .detail-info .el-descriptions table th,
.ai-detail-dialog .detail-info .el-descriptions table td {
  background: transparent !important;
  border-color: rgba(0, 0, 0, 0.04) !important;
}
.ai-detail-dialog .detail-info .el-descriptions__label {
  background: rgba(22, 93, 255, 0.03) !important;
  color: #1d2129 !important;
  font-weight: 700 !important;
  width: 90px;
}
.ai-detail-dialog .detail-info .el-descriptions__content {
  color: #4e5969 !important;
  font-weight: 500;
}

/* 底部关闭按钮 */
.glass-btn-close {
  background: rgba(0, 0, 0, 0.04);
  border: none;
  color: #4e5969;
  font-weight: 600;
  border-radius: 10px;
  padding: 10px 24px;
}
.glass-btn-close:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #1d2129;
}

/* 响应式 */
@media (max-width: 768px) {
  .ai-detail-dialog { width: 94% !important; margin-top: 5vh !important; }
  .ai-detail-dialog .detail-hero { flex-direction: column; gap: 16px; }
  .ai-detail-dialog .hero-image-wrap { width: 100%; height: 200px; }
}
</style>
