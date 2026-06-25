<template>
  <div class="species-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline label-width="80px" @keyup.enter="handleSearch">
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="6">
            <el-form-item label="关键词">
              <el-input v-model="queryForm.keyword" placeholder="名称/特征/习性" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6">
            <el-form-item label="保护等级">
              <el-select v-model="queryForm.conservationStatus" placeholder="全部" clearable style="width:100%; min-width: 120px">
                <el-option label="极危 CR" value="CR" />
                <el-option label="濒危 EN" value="EN" />
                <el-option label="易危 VU" value="VU" />
                <el-option label="近危 NT" value="NT" />
                <el-option label="无危 LC" value="LC" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6">
            <el-form-item label="门">
              <el-input v-model="queryForm.phylum" placeholder="脊索动物门" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6">
            <el-form-item label="纲">
              <el-input v-model="queryForm.className" placeholder="哺乳纲" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6">
            <el-form-item label="目">
              <el-input v-model="queryForm.orderName" placeholder="鲸偶蹄目" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="6">
            <el-form-item label="分布区域">
              <el-input v-model="queryForm.distributionArea" placeholder="南海、珠江口" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" class="search-btns">
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="toolbar">
          <span class="toolbar-title">物种列表（共 {{ total }} 条）</span>
          <!-- 新增按钮组容器 -->
          <div class="toolbar-buttons">
            <el-button
                v-if="authStore.hasRole('ADMIN') || authStore.hasRole('MANAGER')"
                type="success"
                @click="openCreateDialog">
              + 新增物种
            </el-button>
            <el-button
                v-if="authStore.hasRole('ADMIN') || authStore.hasRole('MANAGER')"
                type="primary"
                @click="openImportDialog">
              导入CSV
            </el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="#" width="55" />
        <el-table-column prop="chineseName" label="中文名" min-width="130" />
        <el-table-column prop="scientificName" label="学名" min-width="160" />
        <el-table-column prop="conservationStatus" label="保护等级" width="100">
          <template #default="{ row }">
            <el-tag
              v-if="row.conservationStatus"
              :type="statusType(row.conservationStatus)"
              effect="dark"
              size="small"
            >
              {{ row.conservationStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phylum" label="门" width="120" />
        <el-table-column prop="className" label="纲" width="100" />
        <el-table-column prop="orderName" label="目" width="100" />
        <el-table-column prop="distributionArea" label="分布区域" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>

            <el-button
              v-if="authStore.hasRole('ADMIN') || authStore.hasRole('MANAGER')"
              link type="primary" @click="openEditDialog(row)">编辑</el-button>

            <el-button
              v-if="authStore.hasRole('ADMIN') || authStore.hasRole('MANAGER')"
              link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增物种' : '编辑物种'"
      width="860px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formModel" :rules="rules" label-width="110px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="中文名" prop="chineseName">
              <el-input v-model="formModel.chineseName" placeholder="请输入中文名，AI 将自动推荐其他信息" />
            </el-form-item>
            <!-- AI 智能推荐面板 -->
            <div v-if="suggestion.show" class="suggest-panel">
              <div class="suggest-header">
                <el-icon><Promotion /></el-icon>
                <span>AI 推荐信息</span>
                <el-tag v-if="suggestion.loading" size="small" type="warning">推荐中...</el-tag>
                <el-button v-else size="small" text type="primary" @click="retrySuggestion" style="margin-left: auto;">
                  <el-icon><Refresh /></el-icon> 重新获取
                </el-button>
              </div>
              <div class="suggest-body">
                <el-descriptions :column="2" size="small" border>
                  <el-descriptions-item v-if="suggestion.data.scientificName" label="学名" :span="2">
                    <span class="suggest-value" @click="applySuggestion('scientificName', suggestion.data.scientificName)">{{ suggestion.data.scientificName }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="suggestion.data.kingdom" label="界">
                    <span class="suggest-value" @click="applySuggestion('kingdom', suggestion.data.kingdom)">{{ suggestion.data.kingdom }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="suggestion.data.phylum" label="门">
                    <span class="suggest-value" @click="applySuggestion('phylum', suggestion.data.phylum)">{{ suggestion.data.phylum }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="suggestion.data.className" label="纲">
                    <span class="suggest-value" @click="applySuggestion('className', suggestion.data.className)">{{ suggestion.data.className }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="suggestion.data.orderName" label="目">
                    <span class="suggest-value" @click="applySuggestion('orderName', suggestion.data.orderName)">{{ suggestion.data.orderName }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="suggestion.data.familyName" label="科">
                    <span class="suggest-value" @click="applySuggestion('familyName', suggestion.data.familyName)">{{ suggestion.data.familyName }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="suggestion.data.genusName" label="属">
                    <span class="suggest-value" @click="applySuggestion('genusName', suggestion.data.genusName)">{{ suggestion.data.genusName }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="suggestion.data.conservationStatus" label="保护等级">
                    <span class="suggest-value" @click="applySuggestion('conservationStatus', suggestion.data.conservationStatus)">{{ suggestion.data.conservationStatus }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="suggestion.data.habitat" label="栖息地" :span="2">
                    <span class="suggest-value" @click="applySuggestion('habitat', suggestion.data.habitat)">{{ suggestion.data.habitat }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="suggestion.data.distributionArea" label="分布区域" :span="2">
                    <span class="suggest-value" @click="applySuggestion('distributionArea', suggestion.data.distributionArea)">{{ suggestion.data.distributionArea }}</span>
                  </el-descriptions-item>
                </el-descriptions>
                <div v-if="suggestion.data.morphologyDesc || suggestion.data.habitDesc" class="suggest-desc">
                  <div v-if="suggestion.data.morphologyDesc" class="suggest-desc-item">
                    <strong>形态特征：</strong>
                    <span class="suggest-value" @click="applySuggestion('morphologyDesc', suggestion.data.morphologyDesc)">{{ suggestion.data.morphologyDesc }}</span>
                  </div>
                  <div v-if="suggestion.data.habitDesc" class="suggest-desc-item">
                    <strong>生活习性：</strong>
                    <span class="suggest-value" @click="applySuggestion('habitDesc', suggestion.data.habitDesc)">{{ suggestion.data.habitDesc }}</span>
                  </div>
                </div>
                <div class="suggest-hint">点击上方任意字段自动填入表单</div>
              </div>
            </div>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学名" prop="scientificName">
              <el-input v-model="formModel.scientificName" placeholder="请输入学名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">分类信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="界"><el-input v-model="formModel.kingdom" placeholder="动物界" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="门"><el-input v-model="formModel.phylum" placeholder="脊索动物门" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="纲"><el-input v-model="formModel.className" placeholder="哺乳纲" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="目"><el-input v-model="formModel.orderName" placeholder="鲸偶蹄目" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="科"><el-input v-model="formModel.familyName" placeholder="海豚科" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="属"><el-input v-model="formModel.genusName" placeholder="驼海豚属" /></el-form-item></el-col>
        </el-row>

        <el-divider content-position="left">生态信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="保护等级">
              <el-select v-model="formModel.conservationStatus" placeholder="请选择" clearable style="width:100%">
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
              <el-input v-model="formModel.habitat" placeholder="近岸浅海、河口水域" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="数据来源">
              <el-input v-model="formModel.dataSource" placeholder="如：IUCN" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="分布区域">
          <el-input v-model="formModel.distributionArea" placeholder="如：中国南海沿岸、珠江口、北部湾" />
        </el-form-item>

        <el-divider content-position="left">特征描述</el-divider>
        <el-form-item label="形态特征">
          <el-input v-model="formModel.morphologyDesc" type="textarea" :rows="3" placeholder="请描述形态特征" />
        </el-form-item>
        <el-form-item label="生活习性">
          <el-input v-model="formModel.habitDesc" type="textarea" :rows="3" placeholder="请描述生活习性" />
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
            <div v-else-if="formModel.imageUrl" class="image-preview">
              <el-image :src="formModel.imageUrl" fit="contain" />
              <el-button size="small" type="danger" text @click="removeImage">移除</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="视频链接">
          <el-input v-model="formModel.videoUrl" placeholder="请输入视频链接 URL" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- CSV 导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="批量导入物种"
      width="520px"
      destroy-on-close
      @closed="resetImport">
      <div style="margin-bottom: 16px; padding: 12px; background: #f5f7fa; border-radius: 6px; font-size: 13px; line-height: 1.8;">
        <p><strong>CSV文件要求：</strong></p>
        <p>1. 文件编码为 <code>UTF-8</code></p>
        <p>2. 必须包含17列表头：<br/><code>{{ csvHeaderExample }}</code></p>
        <p>3. 中文名和学名为必填字段</p>
        <p>4. 重复的学名将自动跳过</p>
      </div>

      <el-upload
        ref="csvUploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".csv"
        :on-change="handleCsvChange"
        :on-remove="handleCsvRemove"
        :file-list="csvFileList">
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将CSV文件拖拽到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持 .csv 格式文件。可从"爬虫工具"生成，或
            <el-button type="primary" link size="small" @click="downloadCsvTemplate">
              下载CSV模板
            </el-button>
          </div>
        </template>
      </el-upload>

      <!-- 导入结果 -->
      <div v-if="importResult" style="margin-top: 16px;">
        <el-alert
          :type="importResult.success ? 'success' : 'error'"
          :title="importResult.message"
          :closable="false" />
        <div v-if="importResult.errors && importResult.errors.length > 0" style="margin-top: 8px; max-height: 160px; overflow-y: auto;">
          <p style="font-size: 12px; color: #909399; margin-bottom: 4px;">详细错误：</p>
          <p v-for="(err, i) in importResult.errors" :key="i" style="font-size: 12px; color: #e6a23c; margin: 2px 0;">
            {{ err }}
          </p>
        </div>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importLoading" :disabled="!csvFile" @click="handleImport">
          {{ importLoading ? '导入中...' : '开始导入' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Promotion, UploadFilled, Refresh } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import {
  createSpecies,
  deleteSpecies,
  getSpeciesPage,
  updateSpecies,
  updateSpeciesByBody,
  uploadSpeciesImage,
  suggestSpeciesByAI,
  importSpeciesCsv,
} from "@/api/species";

import { useAuthStore } from "@/store/auth"; // 引入 authStore
const authStore = useAuthStore(); // 实例化

const router = useRouter();
const route = useRoute();

const loading = ref(false);
const submitLoading = ref(false);
const imagePreview = ref("");
const imageFile = ref(null);

const tableData = ref([]);
const total = ref(0);

const queryForm = reactive({
  keyword: "",
  conservationStatus: "",
  phylum: "",
  className: "",
  orderName: "",
  distributionArea: "",
});

const pagination = reactive({
  current: 1,
  size: 10,
});

const dialogVisible = ref(false);
const dialogMode = ref("create");
const formRef = ref();

// ---- CSV 导入 ----
const csvHeaderExample = "chinese_name,scientific_name,kingdom,phylum,class_name,order_name,family_name,genus_name,species_name,conservation_status,habitat,distribution_area,morphology_desc,habit_desc,image_url,video_url,data_source";
const importDialogVisible = ref(false);
const importLoading = ref(false);
const csvFile = ref(null);
const csvFileList = ref([]);
const csvUploadRef = ref(null);
const importResult = ref(null);

const statusType = (s) => {
  const map = { CR: "danger", EN: "warning", VU: "warning", NT: "info", LC: "success" };
  return map[s] || "info";
};

const createDefaultForm = () => ({
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

const formModel = reactive(createDefaultForm());

const rules = {
  chineseName: [{ required: true, message: "请输入中文名", trigger: "blur" }],
  scientificName: [{ required: true, message: "请输入学名", trigger: "blur" }],
};

// ---- AI 智能推荐 ----
const suggestion = reactive({
  show: false,
  loading: false,
  data: {
    scientificName: "",
    kingdom: "",
    phylum: "",
    className: "",
    orderName: "",
    familyName: "",
    genusName: "",
    conservationStatus: "",
    habitat: "",
    distributionArea: "",
    morphologyDesc: "",
    habitDesc: "",
  },
});

let suggestTimer = null;

const fetchSuggestion = async (name) => {
  if (!name || name.length < 2) {
    suggestion.show = false;
    return;
  }
  suggestion.loading = true;
  suggestion.show = true;
  try {
    const resp = await suggestSpeciesByAI(name);
    const result = resp.data;
    if (result.success && result.data) {
      // AI 返回的是 JSON 字符串，需要解析
      let parsed;
      try {
        parsed = JSON.parse(result.data);
      } catch {
        // 尝试去掉代码块标记
        const cleaned = result.data.replace(/```json\s*/g, "").replace(/```/g, "").trim();
        parsed = JSON.parse(cleaned);
      }
      Object.keys(suggestion.data).forEach((k) => {
        suggestion.data[k] = parsed[k] || "";
      });
    } else {
      clearSuggestion();
      ElMessage.warning("AI 推荐暂时不可用，请手动填写或稍后重试");
    }
  } catch {
    clearSuggestion();
    ElMessage.error("AI 推荐失败，请检查网络后重试");
  } finally {
    suggestion.loading = false;
  }
};

const clearSuggestion = () => {
  Object.keys(suggestion.data).forEach((k) => (suggestion.data[k] = ""));
  suggestion.show = false;
};

const applySuggestion = (field, value) => {
  if (value) {
    formModel[field] = value;
  }
};

    // 监听中文名变化，触发 AI 推荐（仅新增模式 + 防抖）
watch(
  () => formModel.chineseName,
  (val) => {
    if (dialogMode.value !== "create") {
      clearSuggestion();
      return;
    }
    if (suggestTimer) clearTimeout(suggestTimer);
    if (!val || val.length < 2) {
      clearSuggestion();
      return;
    }
    suggestTimer = setTimeout(() => fetchSuggestion(val.trim()), 600);
  }
);

// 手动重新获取 AI 推荐
const retrySuggestion = () => {
  const name = formModel.chineseName?.trim();
  if (!name || name.length < 2) {
    ElMessage.warning("请先输入至少2个字的中文名");
    return;
  }
  fetchSuggestion(name);
};

const parsePageData = (payload) => {
  const candidates = [payload, payload?.data, payload?.result, payload?.page];
  for (const c of candidates) {
    if (!c) continue;
    if (Array.isArray(c)) return { records: c, total: c.length };
    if (Array.isArray(c.records)) return { records: c.records, total: Number(c.total ?? c.records.length) };
    if (Array.isArray(c.list)) return { records: c.list, total: Number(c.total ?? c.list.length) };
    if (Array.isArray(c.rows)) return { records: c.rows, total: Number(c.total ?? c.rows.length) };
  }
  return { records: [], total: 0 };
};

const toPayload = () => {
  const p = {};
  const fields = [
    "chineseName", "scientificName", "kingdom", "phylum", "className",
    "orderName", "familyName", "genusName", "speciesName",
    "conservationStatus", "habitat", "distributionArea",
    "morphologyDesc", "habitDesc", "imageUrl", "videoUrl", "dataSource",
  ];
  for (const f of fields) {
    const value = formModel[f];
    if (f === 'imageUrl') {
      p[f] = value === "" ? "" : (typeof value === 'string' ? (value.trim() || null) : null);
    } else {
      p[f] = typeof value === 'string' ? (value.trim() || null) : null;
    }
  }
  return p;
};

const fetchList = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.size,
      current: pagination.current,
      size: pagination.size,
    };
    for (const k of Object.keys(queryForm)) {
      if (queryForm[k]) params[k] = queryForm[k].trim();
    }
    const response = await getSpeciesPage(params);
    const { records, total: count } = parsePageData(response.data);
    tableData.value = records;
    total.value = count;
  } catch (error) {
    ElMessage.error("查询物种数据失败");
  } finally {
    loading.value = false;
  }
};

const handleImageChange = async (uploadFile) => {
  const file = uploadFile.raw;
  if (!file) return;

  // 本地预览
  imagePreview.value = URL.createObjectURL(file);
  imageFile.value = file;
};

const removeImage = () => {
  imagePreview.value = "";
  imageFile.value = null;
  formModel.imageUrl = "";
};

const uploadImageIfNeeded = async () => {
  if (!imageFile.value) return;
  try {
    const resp = await uploadSpeciesImage(imageFile.value);
    const data = resp.data || {};
    if (data.success && data.url) {
      formModel.imageUrl = data.url;
      imageFile.value = null;
    } else if (resp.code === 200 && resp.data) {
      formModel.imageUrl = resp.data;
      imageFile.value = null;
    } else {
      ElMessage.warning(data.message || "图片上传失败，请稍后重试");
    }
  } catch (error) {
    console.error('图片上传错误:', error);
    ElMessage.warning("图片上传失败，请稍后再试");
  }
};

const resetForm = () => {
  Object.assign(formModel, createDefaultForm());
  imagePreview.value = "";
  imageFile.value = null;
  clearSuggestion();
  formRef.value?.clearValidate();
};

const openCreateDialog = () => {
  dialogMode.value = "create";
  resetForm();
  dialogVisible.value = true;
};

const openEditDialog = (row) => {
  dialogMode.value = "edit";
  resetForm();
  const fields = [
    "id", "chineseName", "scientificName", "kingdom", "phylum", "className",
    "orderName", "familyName", "genusName", "speciesName",
    "conservationStatus", "habitat", "distributionArea",
    "morphologyDesc", "habitDesc", "imageUrl", "videoUrl", "dataSource",
  ];
  for (const f of fields) {
    formModel[f] = row[f] ?? "";
  }
  if (formModel.imageUrl) {
    imagePreview.value = getImageUrl(formModel.imageUrl);
  }
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  try {
    await formRef.value.validate();
  } catch {
    return;
  }

  submitLoading.value = true;
  try {
    if (imageFile.value) {
      await uploadImageIfNeeded();
    }

    const payload = toPayload();
    if (dialogMode.value === "create") {
      await createSpecies(payload);
      ElMessage.success("新增成功");
    } else {
      const payloadWithId = { id: formModel.id, ...payload };
      await updateSpecies(formModel.id, payloadWithId);
      ElMessage.success("更新成功");
    }
    dialogVisible.value = false;
    await fetchList();
  } catch (error) {
    console.error(dialogMode.value === "create" ? '新增失败:' : '更新失败:', error);
    ElMessage.error(dialogMode.value === "create" ? "新增失败" : "更新失败");
  } finally {
    submitLoading.value = false;
  }
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除物种「${row.chineseName}」吗？此操作将被记录。`, "删除确认", { type: "warning" });
    await deleteSpecies(row.id);
    ElMessage.success("删除成功（已记录操作日志）");
    if (tableData.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1;
    }
    await fetchList();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      console.error(error);
      ElMessage.error("删除失败");
    }
  }
};

const goDetail = (row) => {
  router.push({ name: "speciesDetail", params: { id: row.id } });
};

const handleSearch = () => {
  pagination.current = 1;
  fetchList();
};

const handleReset = () => {
  Object.keys(queryForm).forEach((k) => (queryForm[k] = ""));
  pagination.current = 1;
  fetchList();
};

const handleSizeChange = (size) => {
  pagination.size = size;
  pagination.current = 1;
  fetchList();
};

const handleCurrentChange = (current) => {
  pagination.current = current;
  fetchList();
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

// ---- CSV 导入方法 ----
const openImportDialog = () => {
  resetImport();
  importDialogVisible.value = true;
};

const resetImport = () => {
  csvFile.value = null;
  csvFileList.value = [];
  importResult.value = null;
};

const handleCsvChange = (uploadFile) => {
  const file = uploadFile.raw;
  if (!file || !file.name.toLowerCase().endsWith(".csv")) {
    ElMessage.warning("仅支持.csv格式文件");
    csvFileList.value = [];
    return;
  }
  csvFile.value = file;
  csvFileList.value = [uploadFile];
};

const handleCsvRemove = () => {
  csvFile.value = null;
  importResult.value = null;
};

const downloadCsvTemplate = () => {
  const header = csvHeaderExample;
  const sampleRow = "蓝鲸,Balaenoptera musculus,Animalia,Chordata,Mammalia,Cetacea,Balaenopteridae,Balaenoptera,musculus,EN,海洋水域,全球海域,形态描述示例,习性描述示例,https://example.com/image.jpg,https://search.bilibili.com/all?keyword=Balaenoptera%20musculus,GBIF API";
  const bom = "﻿";
  const content = bom + header + "\n" + sampleRow;
  const blob = new Blob([content], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "species_import_template.csv";
  a.click();
  URL.revokeObjectURL(url);
};

const handleImport = async () => {
  if (!csvFile.value) {
    ElMessage.warning("请先选择CSV文件");
    return;
  }
  importLoading.value = true;
  importResult.value = null;
  try {
    const resp = await importSpeciesCsv(csvFile.value);
    const data = resp.data || resp;
    if (data.success) {
      importResult.value = {
        success: true,
        message: data.message || `成功导入 ${data.imported || 0} 条记录`,
        errors: data.errors || [],
      };
      ElMessage.success(data.message || "导入完成");
      // Refresh list
      await fetchList();
    } else {
      importResult.value = {
        success: false,
        message: data.message || "导入失败",
        errors: data.errors || [],
      };
      ElMessage.error(data.message || "导入失败");
    }
    csvFile.value = null;
    csvFileList.value = [];
  } catch (error) {
    console.error("CSV导入错误:", error);
    importResult.value = {
      success: false,
      message: "导入请求失败：" + (error.response?.data?.message || error.message),
      errors: [],
    };
    ElMessage.error("导入请求失败");
  } finally {
    importLoading.value = false;
  }
};

onMounted(async () => {
  await fetchList();
  // 从物种识别页跳转过来，自动打开新增对话框
  if (route.query.add === "true" && route.query.name) {
    openCreateDialog();
    formModel.chineseName = route.query.name;
  }
});
</script>

<style scoped>
.species-page {
  min-height: calc(100vh - 140px);
}

.search-card {
  margin-bottom: 12px;
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(12px) !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
}

.search-btns {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 18px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-buttons {
  display: flex;
  gap: 8px; /* 按钮之间的间距，可根据需要调整 */
  align-items: center; /* 垂直居中对齐 */
}

.toolbar .el-button + .el-button {
  margin-left: 4px;
}

.toolbar-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--theme-text-primary);
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
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

/* AI 推荐面板 */
.suggest-panel {
  margin-top: 6px;
  border: 1px solid rgba(221, 205, 180, 0.4);
  border-radius: 6px;
  background: rgba(221, 205, 180, 0.08);
  backdrop-filter: blur(8px);
  overflow: hidden;
}

.suggest-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-khaki);
  border-bottom: 1px solid rgba(221, 205, 180, 0.3);
  background: rgba(221, 205, 180, 0.1);
}

.suggest-body {
  padding: 8px 10px;
}

.suggest-body :deep(.el-descriptions__cell) {
  padding: 4px 8px !important;
}

.suggest-value {
  color: var(--color-khaki);
  cursor: pointer;
  border-bottom: 1px dashed rgba(221, 205, 180, 0.6);
  transition: all 0.15s;
}

.suggest-value:hover {
  background: rgba(221, 205, 180, 0.15);
  border-bottom-color: var(--color-khaki);
}

.suggest-desc {
  margin-top: 6px;
}

.suggest-desc-item {
  margin: 4px 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--theme-text-secondary);
}

.suggest-desc-item strong {
  color: var(--theme-text-primary);
}

.suggest-hint {
  margin-top: 6px;
  font-size: 11px;
  color: var(--theme-text-muted);
  text-align: center;
}

:deep(.el-card) {
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(12px) !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
}

:deep(.el-dialog) {
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(12px) !important;
}
</style>