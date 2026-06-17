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
          <el-tag type="success" effect="plain" class="match-tag">已存在于生物展示馆</el-tag>
          <el-tag
            class="species-link-tag"
            effect="plain"
            @click="goToDetail(matchedSpecies.id)"
          >
            <el-icon><Link /></el-icon>
            {{ matchedSpecies.chineseName }} · 点击查看详情
          </el-tag>
        </div>
        <div v-else class="match-not-found">
          <el-tag type="info" effect="plain" class="match-tag">该物种暂未收录</el-tag>
          <el-button type="success" size="small" @click="goToAddSpecies">
            + 新增该物种
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
  </el-card>
</template>

<script setup>
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { Link, Loading, UploadFilled } from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { identifySpecies, identifySpeciesByImage } from "@/api/ai";
import { suggestSpecies } from "@/api/species";

const router = useRouter();

const file = ref(null);
const previewUrl = ref("");
const loading = ref(false);
const result = ref(null);
const matchedSpecies = ref(null); // null=未查询, object=找到, null-ish=未找到
const lookingUp = ref(false);

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

const goToDetail = (id) => {
  router.push({ name: "speciesDetail", params: { id } });
};

const goToAddSpecies = () => {
  router.push({ path: "/species", query: { add: "true", name: result.value?.speciesName || "" } });
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

.species-link-tag {
  cursor: pointer;
  font-size: 13px;
  padding: 6px 12px;
  border-radius: 20px;
  background: #ecfdf5 !important;
  border-color: #a7f3d0 !important;
  color: #065f46 !important;
  transition: all 0.2s;
}

.species-link-tag:hover {
  background: #d1fae5 !important;
  border-color: #6ee7b7 !important;
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
