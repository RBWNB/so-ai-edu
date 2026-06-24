<template>
  <div class="observation-review">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>观察帖子审核</span>
          <div class="header-tools">
            <el-radio-group v-model="statusFilter" size="small" @change="handleFilterChange">
              <el-radio-button :value="null">全部</el-radio-button>
              <el-radio-button :value="2">待审核</el-radio-button>
              <el-radio-button :value="1">已通过</el-radio-button>
              <el-radio-button :value="0">已下架</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border style="width: 100%">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="用户" width="150">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="28" :src="formatAvatar(row.avatarUrl)">
                <el-icon :size="16"><User /></el-icon>
              </el-avatar>
              <div class="user-cell-info">
                <span class="user-name">{{ row.username }}</span>
                <span v-if="row.userTitle" class="user-badge">{{ row.userTitle }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="speciesName" label="物种" width="120" show-overflow-tooltip />
        <el-table-column label="图片" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.photoUrl"
              :src="row.photoUrl"
              fit="cover"
              style="width: 44px; height: 44px; border-radius: 4px; cursor: pointer;"
              :preview-src-list="[row.photoUrl]"
              preview-teleported
            />
            <el-icon v-else :size="20" style="color: rgba(255,255,255,0.2)"><Picture /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="locationName" label="地点" width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
            <div v-if="row.status === 0 && row.auditRemark" class="remark-text">
              {{ row.auditRemark }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="160" />
        <el-table-column label="操作" width="270" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" plain @click="openDetail(row)">详情</el-button>
            <template v-if="row.status === 2">
              <el-button type="primary" size="small" @click="approveObservation(row)">通过</el-button>
              <el-button type="danger" size="small" @click="openRejectDialog(row)">下架</el-button>
            </template>
            <template v-else-if="row.status === 1">
              <el-button type="warning" size="small" @click="openRejectDialog(row)">下架</el-button>
            </template>
            <template v-else>
              <el-button type="success" size="small" @click="approveObservation(row)">恢复</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          background
          small
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 帖子详情预览弹窗 -->
    <el-dialog v-model="detailDialog.visible" title="帖子详情预览" width="560px" top="5vh" class="detail-dialog" destroy-on-close>
      <div v-loading="detailDialog.loading" class="detail-body">
        <template v-if="detailDialog.data">
          <!-- 用户信息头部 -->
          <div class="detail-user">
            <div class="detail-avatar-frame" :class="'frame-' + (detailDialog.data.avatarFrame || 'default')">
              <el-avatar :size="44" :src="formatAvatar(detailDialog.data.avatarUrl)">
                <el-icon :size="22"><User /></el-icon>
              </el-avatar>
            </div>
            <div class="detail-user-info">
              <div class="detail-user-row">
                <span class="detail-username">{{ detailDialog.data.username }}</span>
                <span v-if="detailDialog.data.userTitle" class="detail-usertitle">{{ detailDialog.data.userTitle }}</span>
              </div>
              <div class="detail-time">
                <el-icon :size="12"><Clock /></el-icon>
                <span>{{ detailDialog.data.createdAt }}</span>
              </div>
            </div>
          </div>

          <!-- 帖子内容 -->
          <div class="detail-content">
            <h3 class="detail-title">{{ detailDialog.data.title }}</h3>
            <p v-if="detailDialog.data.description" class="detail-desc">{{ detailDialog.data.description }}</p>

            <!-- 图片 -->
            <div v-if="detailDialog.data.photoUrl" class="detail-image">
              <el-image
                :src="detailDialog.data.photoUrl"
                fit="contain"
                :preview-src-list="[detailDialog.data.photoUrl]"
                preview-teleported
                style="width: 100%; max-height: 360px; border-radius: 8px;"
              />
            </div>

            <!-- 元信息 -->
            <div class="detail-meta">
              <span v-if="detailDialog.data.speciesName" class="detail-tag species">🐟 {{ detailDialog.data.speciesName }}</span>
              <span v-if="detailDialog.data.locationName" class="detail-tag location">📍 {{ detailDialog.data.locationName }}</span>
              <span v-if="detailDialog.data.observedAt" class="detail-tag date">📅 {{ detailDialog.data.observedAt }}</span>
            </div>
          </div>

          <!-- 审核信息 -->
          <div class="detail-audit">
            <el-divider />
            <div class="audit-row">
              <span class="audit-label">当前状态：</span>
              <el-tag size="small" :type="statusType(detailDialog.data.status)" effect="plain">
                {{ statusLabel(detailDialog.data.status) }}
              </el-tag>
            </div>
            <div v-if="detailDialog.data.status === 0 && detailDialog.data.auditRemark" class="audit-row">
              <span class="audit-label">下架原因：</span>
              <span class="audit-reason">{{ detailDialog.data.auditRemark }}</span>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无数据" :image-size="80" />
      </div>
      <template #footer>
        <div class="detail-footer-actions">
          <template v-if="detailDialog.data">
            <el-button v-if="detailDialog.data.status === 2" type="primary" @click="doDetailApprove">通过审核</el-button>
            <el-button v-if="detailDialog.data.status === 2 || detailDialog.data.status === 1" type="danger" @click="doDetailReject">下架</el-button>
            <el-button v-if="detailDialog.data.status === 0" type="success" @click="doDetailRecover">恢复上架</el-button>
          </template>
          <el-button @click="detailDialog.visible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 下架原因弹窗 -->
    <el-dialog v-model="rejectDialog.visible" title="下架观察" width="420px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="观察标题">
          <el-input :model-value="rejectDialog.title" disabled />
        </el-form-item>
        <el-form-item label="下架原因" required>
          <el-input
            v-model="rejectDialog.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入下架原因（用户将看到此原因）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="rejectDialog.loading" @click="rejectObservation">确认下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { User, Picture } from "@element-plus/icons-vue";
import http from "@/utils/http";

// ── 数据 ──
const loading = ref(false);
const tableData = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const statusFilter = ref(2); // 默认显示待审核

// ── 下架弹窗 ──
const rejectDialog = ref({
  visible: false,
  id: null,
  title: "",
  reason: "",
  loading: false,
});

// ── 详情弹窗 ──
const detailDialog = ref({
  visible: false,
  loading: false,
  data: null,
});

// ── 状态映射 ──
const statusType = (s) => {
  if (s === 1) return "success";
  if (s === 2) return "warning";
  return "info";
};
const statusLabel = (s) => {
  if (s === 1) return "已通过";
  if (s === 2) return "待审核";
  return "已下架";
};

// ── 格式化头像 ──
const formatAvatar = (url) => {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("/api")) return url;
  return `/api${url}`;
};

// ── 加载数据 ──
const loadData = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (statusFilter.value !== null) {
      params.status = statusFilter.value;
    }
    const res = await http.get("/observation/admin/list", { params });
    if (res.data.success) {
      tableData.value = res.data.data.records || [];
      total.value = res.data.data.total || 0;
    } else {
      ElMessage.warning(res.data.message || "加载失败");
    }
  } catch (err) {
    console.error("加载观察列表失败", err);
    ElMessage.error("加载失败");
  } finally {
    loading.value = false;
  }
};

// ── 筛选切换 ──
const handleFilterChange = () => {
  pageNum.value = 1;
  loadData();
};

// ── 通过审核 ──
const approveObservation = async (row) => {
  try {
    const res = await http.put(`/observation/admin/${row.id}/status`, { status: 1 });
    if (res.data.success) {
      ElMessage.success("审核通过");
      loadData();
    } else {
      ElMessage.warning(res.data.message || "操作失败");
    }
  } catch (err) {
    ElMessage.error("操作失败");
  }
};

// ── 打开下架弹窗 ──
const openRejectDialog = (row) => {
  rejectDialog.value = {
    visible: true,
    id: row.id,
    title: row.title,
    reason: "",
    loading: false,
  };
};

// ── 打开详情预览 ──
const openDetail = (row) => {
  detailDialog.value = {
    visible: true,
    loading: false,
    data: { ...row },
  };
};

// ── 详情弹窗内操作：通过 ──
const doDetailApprove = async () => {
  if (!detailDialog.value.data) return;
  try {
    const res = await http.put(`/observation/admin/${detailDialog.value.data.id}/status`, { status: 1 });
    if (res.data.success) {
      ElMessage.success("审核通过");
      detailDialog.value.data.status = 1;
      detailDialog.value.data.auditRemark = null;
      loadData();
    } else {
      ElMessage.warning(res.data.message || "操作失败");
    }
  } catch (err) {
    ElMessage.error("操作失败");
  }
};

// ── 详情弹窗内操作：下架（跳转到下架弹窗） ──
const doDetailReject = () => {
  if (!detailDialog.value.data) return;
  const row = detailDialog.value.data;
  detailDialog.value.visible = false;
  openRejectDialog(row);
};

// ── 详情弹窗内操作：恢复 ──
const doDetailRecover = async () => {
  if (!detailDialog.value.data) return;
  try {
    const res = await http.put(`/observation/admin/${detailDialog.value.data.id}/status`, { status: 1 });
    if (res.data.success) {
      ElMessage.success("已恢复上架");
      detailDialog.value.data.status = 1;
      detailDialog.value.data.auditRemark = null;
      loadData();
    } else {
      ElMessage.warning(res.data.message || "操作失败");
    }
  } catch (err) {
    ElMessage.error("操作失败");
  }
};

// ── 确认下架 ──
const rejectObservation = async () => {
  if (!rejectDialog.value.reason.trim()) {
    ElMessage.warning("请输入下架原因");
    return;
  }
  rejectDialog.value.loading = true;
  try {
    const res = await http.put(`/observation/admin/${rejectDialog.value.id}/status`, {
      status: 0,
      auditRemark: rejectDialog.value.reason.trim(),
    });
    if (res.data.success) {
      ElMessage.success("已下架");
      rejectDialog.value.visible = false;
      loadData();
    } else {
      ElMessage.warning(res.data.message || "操作失败");
    }
  } catch (err) {
    ElMessage.error("操作失败");
  } finally {
    rejectDialog.value.loading = false;
  }
};

// ── 初始化 ──
onMounted(() => {
  loadData();
});
</script>

<style scoped>
.observation-review {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-cell-info {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-badge {
  font-size: 10px;
  color: #d48806;
  background: rgba(255, 215, 0, 0.12);
  padding: 0 6px;
  border-radius: 8px;
  white-space: nowrap;
  display: inline-block;
  width: fit-content;
}

.remark-text {
  font-size: 11px;
  color: #f56c6c;
  margin-top: 2px;
  line-height: 1.3;
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 暗色主题适配（后台页面继承全局样式） */
:deep(.el-table) {
  --el-table-border-color: rgba(255, 255, 255, 0.1);
  --el-table-header-bg-color: rgba(255, 255, 255, 0.04);
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-table-tr-bg-color: transparent;
}

/* ── 详情弹窗 ── */
.detail-body {
  padding: 0 4px;
}

.detail-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-avatar-frame {
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
  flex-shrink: 0;
}

.detail-avatar-frame > .el-avatar {
  width: 44px;
  height: 44px;
}

.detail-user-info {
  flex: 1;
  min-width: 0;
}

.detail-user-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-username {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.detail-usertitle {
  font-size: 11px;
  color: #d48806;
  background: rgba(255, 215, 0, 0.12);
  padding: 1px 8px;
  border-radius: 10px;
  white-space: nowrap;
}

.detail-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 8px;
}

.detail-desc {
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
  margin: 0 0 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-image {
  margin: 0 -4px 12px;
  display: flex;
  justify-content: center;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 8px;
  overflow: hidden;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 4px;
}

.detail-tag {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  padding: 3px 10px;
  border-radius: 14px;
  white-space: nowrap;
}

.detail-tag.species {
  color: #1a9bc4;
  background: rgba(26, 155, 196, 0.08);
}

.detail-tag.location {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.08);
}

.detail-audit {
  margin-top: 8px;
}

.audit-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
}

.audit-label {
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.audit-reason {
  color: #f56c6c;
  font-size: 13px;
}

.detail-footer-actions {
  display: flex;
  gap: 8px;
}

/* ── 头像框样式（预览弹窗用） ── */
.frame-default { border: 2px solid rgba(0,0,0,0.15); border-radius: 50%; }
.frame-gold { border: 2px solid #ffd700; border-radius: 50%; box-shadow: 0 0 8px rgba(255,215,0,0.4); }
.frame-ocean { border: 2px solid #36cfc9; border-radius: 50%; box-shadow: 0 0 8px rgba(54,207,201,0.4); }
.frame-rainbow { border: 2px solid transparent; border-radius: 50%; background: linear-gradient(135deg, #ff6b6b, #ffd93d, #6bcb77, #4d96ff, #9b59b6) border-box; -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0); -webkit-mask-composite: xor; mask-composite: exclude; }
.frame-flame { border: 2px solid #ff6b35; border-radius: 50%; box-shadow: 0 0 10px rgba(255,107,53,0.5); }
.frame-dashed { border: 2px dashed #7ec8e3; border-radius: 50%; }
.frame-neon { border: 2px solid #39ff14; border-radius: 50%; box-shadow: 0 0 10px rgba(57,255,20,0.5); }
.frame-aurora { border: 2px solid transparent; border-radius: 50%; background: linear-gradient(135deg, #00f5a0, #00d9f5) border-box; -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0); -webkit-mask-composite: xor; mask-composite: exclude; }
.frame-crystal { border: 2px solid rgba(0,0,0,0.2); border-radius: 50%; box-shadow: 0 0 12px rgba(0,0,0,0.06); }
.frame-royal { border: 2px solid #7b2ff7; border-radius: 50%; box-shadow: 0 0 8px rgba(123,47,247,0.4); }
</style>
