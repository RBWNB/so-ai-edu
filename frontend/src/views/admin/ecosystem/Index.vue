<template>
  <div class="eco-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryForm" inline label-width="80px" @keyup.enter="handleSearch">
        <el-row :gutter="16">
          <el-col :xs="12" :sm="8" :md="5">
            <el-form-item label="关键词">
              <el-input v-model="queryForm.keyword" placeholder="名称/描述/物种" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="5">
            <el-form-item label="典型物种">
              <el-input v-model="queryForm.typicalSpecies" placeholder="输入物种名称" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="12" :sm="8" :md="5">
            <el-form-item label="主要威胁">
              <el-input v-model="queryForm.threats" placeholder="输入威胁因素" clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="6" class="search-btns">
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="toolbar">
          <span class="toolbar-title">生态系统列表（共 {{ total }} 条）</span>
          <el-button type="success" @click="openCreate">+ 新增生态系统</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column type="index" label="#" width="55" />
        <el-table-column prop="name" label="名称" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="typicalSpecies" label="典型物种" min-width="200" show-overflow-tooltip />
        <el-table-column prop="threats" label="主要威胁" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="mode === 'create' ? '新增生态系统' : '编辑生态系统'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="输入名称后，AI 将自动推荐其他信息" />
        </el-form-item>

        <!-- AI 智能推荐面板 -->
        <div v-if="suggestion.show" class="suggest-panel">
          <div class="suggest-header">
            <el-icon><Promotion /></el-icon>
            <span>AI 推荐信息</span>
            <el-tag v-if="suggestion.loading" size="small" type="warning">推荐中...</el-tag>
          </div>
          <div class="suggest-body">
            <p v-if="suggestion.data.description" class="suggest-item">
              <strong>描述：</strong>
              <span class="suggest-value" @click="applySuggestion('description', suggestion.data.description)">{{ suggestion.data.description }}</span>
            </p>
            <p v-if="suggestion.data.typicalSpecies" class="suggest-item">
              <strong>典型物种：</strong>
              <span class="suggest-value" @click="applySuggestion('typicalSpecies', suggestion.data.typicalSpecies)">{{ suggestion.data.typicalSpecies }}</span>
            </p>
            <p v-if="suggestion.data.threats" class="suggest-item">
              <strong>主要威胁：</strong>
              <span class="suggest-value" @click="applySuggestion('threats', suggestion.data.threats)">{{ suggestion.data.threats }}</span>
            </p>
          </div>
          <div class="suggest-footer">点击上方任意字段自动填入表单</div>
        </div>

        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="典型物种">
          <el-input v-model="form.typicalSpecies" placeholder="常见物种列表" />
        </el-form-item>
        <el-form-item label="主要威胁">
          <el-input v-model="form.threats" placeholder="如：全球变暖、污染" />
        </el-form-item>

        <el-divider content-position="left">影像资料</el-divider>
        <el-form-item label="生态系统图片">
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
            <div v-else-if="form.imageUrl" class="image-preview">
              <el-image :src="getImageUrl(form.imageUrl)" fit="contain" />
              <el-button size="small" type="danger" text @click="removeImage">移除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Promotion, UploadFilled } from "@element-plus/icons-vue";
import { getEcosystemPage, createEcosystem, updateEcosystem, deleteEcosystem, suggestEcosystemByAI, uploadEcosystemImage } from "@/api/ecosystem";

const loading = ref(false);
const submitLoading = ref(false);
const imagePreview = ref("");
const imageFile = ref(null);
const tableData = ref([]);
const total = ref(0);
const dialogVisible = ref(false);
const mode = ref("create");
const formRef = ref();
let suggestTimer = null;

const form = reactive({ id: undefined, name: "", description: "", typicalSpecies: "", threats: "", imageUrl: "" });
const rules = { name: [{ required: true, message: "请输入名称", trigger: "blur" }] };

const queryForm = reactive({
  keyword: "",
  typicalSpecies: "",
  threats: "",
});

const pagination = reactive({
  current: 1,
  size: 10,
});

const suggestion = reactive({
  show: false,
  loading: false,
  data: { description: "", typicalSpecies: "", threats: "" },
});

const clearSuggestion = () => {
  suggestion.show = false;
  suggestion.loading = false;
  suggestion.data = { description: "", typicalSpecies: "", threats: "" };
};

const fetchSuggestion = async (name) => {
  if (!name || name.length < 2) { clearSuggestion(); return }
  suggestion.loading = true;
  suggestion.show = true;
  try {
    const resp = await suggestEcosystemByAI(name);
    const result = resp.data;
    if (result.success && result.data) {
      let parsed;
      try {
        parsed = JSON.parse(result.data);
      } catch {
        const cleaned = result.data.replace(/```json\s*/g, "").replace(/```/g, "").trim();
        parsed = JSON.parse(cleaned);
      }
      suggestion.data = {
        description: parsed.description || "",
        typicalSpecies: parsed.typicalSpecies || "",
        threats: parsed.threats || "",
      };
    } else {
      clearSuggestion();
    }
  } catch {
    clearSuggestion();
  } finally {
    suggestion.loading = false;
  }
};

const applySuggestion = (field, value) => {
  if (value) { form[field] = value }
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
  form.imageUrl = "";
};

const uploadImageIfNeeded = async () => {
  if (!imageFile.value) return;
  try {
    const resp = await uploadEcosystemImage(imageFile.value);
    const data = resp.data || {};
    if (data.success && data.url) {
      form.imageUrl = data.url;
      imageFile.value = null;
    } else if (resp.code === 200 && resp.data) {
      form.imageUrl = resp.data;
      imageFile.value = null;
    } else {
      ElMessage.warning(data.message || "图片上传失败，请稍后重试");
    }
  } catch (error) {
    console.error('图片上传错误:', error);
    ElMessage.warning("图片上传失败，请稍后再试");
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

// 监听名称，当新增模式且名称变化时触发 AI 推荐
watch(
    () => form.name,
    (val) => {
      if (mode.value !== "create") { clearSuggestion(); return }
      if (suggestTimer) clearTimeout(suggestTimer);
      if (!val || val.length < 2) { clearSuggestion(); return }
      suggestTimer = setTimeout(() => fetchSuggestion(val.trim()), 600);
    }
);

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
    const response = await getEcosystemPage(params);
    const { records, total: count } = parsePageData(response.data);
    tableData.value = records;
    total.value = count;
  } catch {
    ElMessage.error("获取生态系统列表失败")
  }
  finally { loading.value = false }
};

const resetForm = () => {
  form.id = undefined;
  form.name = form.description = form.typicalSpecies = form.threats = form.imageUrl = "";
  imagePreview.value = "";
  imageFile.value = null;
  clearSuggestion();
  formRef.value?.clearValidate();
};

const openCreate = () => { mode.value = "create"; resetForm(); dialogVisible.value = true; };
const openEdit = (row) => {
  mode.value = "edit";
  resetForm();
  Object.assign(form, { id: row.id, name: row.name, description: row.description || "", typicalSpecies: row.typicalSpecies || "", threats: row.threats || "", imageUrl: row.imageUrl || "" });
  if (form.imageUrl) {
    imagePreview.value = getImageUrl(form.imageUrl);
  }
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  try { await formRef.value.validate() } catch { return }
  submitLoading.value = true;
  try {
    if (imageFile.value) {
      await uploadImageIfNeeded();
    }

    const payload = { name: form.name.trim(), description: form.description.trim(), typicalSpecies: form.typicalSpecies.trim(), threats: form.threats.trim(), imageUrl: form.imageUrl.trim() };
    if (mode.value === "create") {
      await createEcosystem(payload);
      ElMessage.success("创建成功");
    } else {
      await updateEcosystem(form.id, payload);
      ElMessage.success("更新成功");
    }
    dialogVisible.value = false;
    await fetchList();
  } catch { ElMessage.error("操作失败") }
  finally { submitLoading.value = false }
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」？`, "确认", { type: "warning" });
    await deleteEcosystem(row.id);
    ElMessage.success("删除成功");
    if (tableData.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1;
    }
    await fetchList();
  } catch (e) { if (e !== "cancel" && e !== "close") ElMessage.error("删除失败") }
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

onMounted(fetchList);
</script>

<style scoped>.eco-page { min-height: calc(100vh - 140px); }

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

.toolbar { display: flex; justify-content: space-between; align-items: center; }
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

.suggest-panel {
  border: 1px solid rgba(221, 205, 180, 0.4);
  border-radius: 6px;
  margin-bottom: 16px;
  margin-left: 110px;
  background: rgba(221, 205, 180, 0.08);
  backdrop-filter: blur(8px);
  overflow: hidden;
}

.suggest-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: rgba(221, 205, 180, 0.1);
  font-size: 13px;
  font-weight: 500;
  color: var(--color-khaki);
}

.suggest-body { padding: 8px 12px; }

.suggest-item {
  margin: 6px 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--theme-text-secondary);
}

.suggest-item strong {
  color: var(--theme-text-primary);
  margin-right: 4px;
}

.suggest-value {
  color: var(--color-khaki);
  cursor: pointer;
  border-bottom: 1px dashed rgba(221, 205, 180, 0.6);
}

.suggest-value:hover {
  background: rgba(221, 205, 180, 0.15);
}

.suggest-footer {
  padding: 4px 12px 8px;
  font-size: 12px;
  color: var(--theme-text-muted);
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

:deep(.el-table) {
  background: transparent !important;
}

:deep(.el-dialog) {
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(12px) !important;
}
</style>
