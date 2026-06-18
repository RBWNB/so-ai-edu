<template>
  <div class="knowledge-page">
    <!-- 页面标题 + 操作 -->
    <div class="page-toolbar">
      <h2 class="page-title">知识库管理</h2>
      <div class="toolbar-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文档标题/内容..."
          clearable
          :prefix-icon="Search"
          style="width: 260px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" :icon="Upload" @click="openUploadDialog">上传文档</el-button>
        <el-button :icon="Edit" @click="openCreateDialog">手动创建</el-button>
      </div>
    </div>

    <!-- 文档表格 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        style="width: 100%"
        empty-text="暂无知识库文档"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="文档标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="分类" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="sourceTypeTag(row.sourceType)">
              {{ sourceTypeLabel(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">
              查看
            </el-button>
            <el-popconfirm
              title="确定删除该文档及其全部知识分块吗？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          small
          @current-change="fetchDocuments"
        />
      </div>
    </el-card>

    <!-- 上传文档对话框 -->
    <el-dialog
      v-model="uploadVisible"
      title="上传知识库文档"
      width="520px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="uploadFormRef" :model="uploadForm" label-width="80px">
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-exceed="() => ElMessage.warning('一次只能上传一个文件')"
            :on-remove="() => (uploadFile = null)"
            accept=".pdf,.doc,.docx,.txt"
            drag
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF / Word / TXT 格式，文件不超过 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">
          {{ uploading ? '解析中...' : '开始上传' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 手动创建文档对话框 -->
    <el-dialog
      v-model="createVisible"
      title="手动创建文档"
      width="640px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="文档标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入文档标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="文档内容" prop="content">
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="10"
            placeholder="请输入文档正文内容..."
            maxlength="50000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="createForm.source" placeholder="来源（选填）" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">
          创建并向量化
        </el-button>
      </template>
    </el-dialog>

    <!-- 文档详情对话框 -->
    <el-dialog v-model="detailVisible" title="文档详情" width="720px" destroy-on-close>
      <div v-if="currentDoc" class="doc-detail">
        <div class="detail-header">
          <h3>{{ currentDoc.title }}</h3>
          <div class="detail-meta">
            <el-tag size="small" :type="sourceTypeTag(currentDoc.sourceType)">
              {{ sourceTypeLabel(currentDoc.sourceType) }}
            </el-tag>
            <span>来源：{{ currentDoc.source || '无' }}</span>
            <span>创建时间：{{ formatTime(currentDoc.createdAt) }}</span>
            <span>更新时间：{{ formatTime(currentDoc.updatedAt) }}</span>
          </div>
        </div>
        <el-divider />
        <div class="detail-content">{{ currentDoc.content }}</div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Edit, Search, Upload, UploadFilled } from "@element-plus/icons-vue";
import { getDocumentPage, uploadDocument, createDocument, deleteDocument } from "@/api/knowledge";

// ========== 列表 ==========
const loading = ref(false);
const tableData = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const searchKeyword = ref("");

const fetchDocuments = async () => {
  loading.value = true;
  try {
    const res = await getDocumentPage({
      keyword: searchKeyword.value || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    });
    const data = res?.data?.data || res?.data;
    if (data && data.records) {
      tableData.value = data.records;
      total.value = data.total || 0;
    } else if (Array.isArray(data)) {
      tableData.value = data;
      total.value = data.length;
    } else {
      tableData.value = [];
      total.value = 0;
    }
  } catch (err) {
    console.error("获取文档列表失败:", err);
    ElMessage.error("获取文档列表失败");
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pageNum.value = 1;
  fetchDocuments();
};

// ========== 上传 ==========
const uploadVisible = ref(false);
const uploading = ref(false);
const uploadFile = ref(null);
const uploadFormRef = ref(null);
const uploadRef = ref(null);

const openUploadDialog = () => {
  uploadFile.value = null;
  uploadVisible.value = true;
};

const handleFileChange = (file) => {
  uploadFile.value = file.raw;
};

const handleUpload = async () => {
  if (!uploadFile.value) {
    ElMessage.warning("请选择文件");
    return;
  }
  uploading.value = true;
  try {
    const formData = new FormData();
    formData.append("file", uploadFile.value);
    await uploadDocument(formData);
    ElMessage.success("上传成功，文档已自动解析并向量化");
    uploadVisible.value = false;
    await fetchDocuments();
  } catch (err) {
    console.error("上传失败:", err);
    ElMessage.error("上传失败：" + (err?.response?.data?.message || err.message));
  } finally {
    uploading.value = false;
  }
};

// ========== 手动创建 ==========
const createVisible = ref(false);
const creating = ref(false);
const createFormRef = ref(null);
const createForm = ref({
  title: "",
  content: "",
  source: "",
});
const createRules = {
  title: [{ required: true, message: "请输入文档标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入文档内容", trigger: "blur" }],
};

const openCreateDialog = () => {
  createForm.value = { title: "", content: "", source: "" };
  createVisible.value = true;
};

const handleCreate = async () => {
  try {
    await createFormRef.value?.validate();
  } catch {
    return;
  }
  creating.value = true;
  try {
    await createDocument({
      title: createForm.value.title.trim(),
      content: createForm.value.content.trim(),
      source: createForm.value.source.trim() || undefined,
      sourceType: "manual",
      status: 1,
    });
    ElMessage.success("文档创建成功，已自动分块并向量化");
    createVisible.value = false;
    await fetchDocuments();
  } catch (err) {
    console.error("创建文档失败:", err);
    ElMessage.error("创建失败：" + (err?.response?.data?.message || err.message));
  } finally {
    creating.value = false;
  }
};

// ========== 删除 ==========
const handleDelete = async (id) => {
  try {
    await deleteDocument(id);
    ElMessage.success("已删除文档及关联向量数据");
    await fetchDocuments();
  } catch (err) {
    console.error("删除失败:", err);
    ElMessage.error("删除失败");
  }
};

// ========== 详情 ==========
const detailVisible = ref(false);
const currentDoc = ref(null);

const openDetail = (row) => {
  currentDoc.value = row;
  detailVisible.value = true;
};

// ========== 工具 ==========
const sourceTypeLabel = (type) =>
  ({ upload: "上传文档", manual: "手动创建", species: "物种关联", ecosystem: "生态关联" }[type] || type || "未知");

const sourceTypeTag = (type) =>
  ({ upload: "success", manual: "", species: "warning", ecosystem: "warning" }[type] || "info");

const formatTime = (t) => {
  if (!t) return "-";
  return t.replace("T", " ").substring(0, 19);
};

onMounted(() => {
  fetchDocuments();
});
</script>

<style scoped>
.knowledge-page {
  min-height: calc(100vh - 120px);
}

.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--theme-text-primary);
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 10px 0;
}

.doc-detail .detail-header h3 {
  margin: 0 0 12px;
  font-size: 18px;
  color: var(--theme-text-primary);
}

.doc-detail .detail-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--theme-text-muted);
  flex-wrap: wrap;
}

.doc-detail .detail-content {
  font-size: 14px;
  color: var(--theme-text-secondary);
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 50vh;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .page-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
  .toolbar-actions {
    width: 100%;
  }
  .toolbar-actions :deep(.el-input) {
    width: 100% !important;
  }
}
</style>
