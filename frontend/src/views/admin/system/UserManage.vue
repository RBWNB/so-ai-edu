<template>
  <div class="user-manage">
    <el-card>
      <template #header>
        <div class="card-header">用户管理</div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="账号">
          <el-input v-model="searchForm.username" placeholder="请输入账号" clearable style="width: 180px;" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.realName" placeholder="请输入用户名" clearable style="width: 180px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :src="formatAvatarUrl(row.avatarUrl)" :size="36">
              {{ row.username?.[0] || "U" }}
            </el-avatar>
          </template>
        </el-table-column>

        <el-table-column prop="username" label="账号" min-width="100" show-overflow-tooltip />
        <el-table-column prop="realName" label="用户名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="120" />

        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <template v-if="displayRoles(row).length > 0">
              <el-tag
                v-for="role in displayRoles(row)"
                :key="role"
                size="small"
                class="role-tag"
              >
                {{ role }}
              </el-tag>
            </template>
            <span v-else class="muted">未分配</span>
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>

        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="230" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <template v-if="row.auditStatus === 0">
                <el-button type="success" plain size="small" class="action-btn" @click="handleApprove(row)">通过</el-button>
                <el-button type="danger" plain size="small" class="action-btn" @click="openRejectDialog(row)">驳回</el-button>
              </template>
              <el-button type="primary" plain size="small" class="role-btn" @click="openRoleDialog(row)">分配角色</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-wrap">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 分配角色弹窗 -->
    <el-dialog
      v-model="roleDialog.visible"
      title="分配角色"
      width="420px"
      destroy-on-close
    >
      <div class="dialog-user">
        当前用户：<strong>{{ roleDialog.user?.username || "-" }}</strong>
      </div>

      <el-radio-group v-model="roleDialog.selectedRoleId" class="role-radio-group">
        <el-radio
          v-for="item in roleOptions"
          :key="item.id"
          :value="item.id"
          border
          class="role-radio"
        >
          {{ item.label }} <span class="role-code">({{ item.code }})</span>
        </el-radio>
      </el-radio-group>

      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="roleDialog.submitting" @click="submitRoles">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { assignUserRoles, approveUser, getUserPage, rejectUser, updateUserStatus } from "@/api/sysUser";
import { getEnabledRoleList } from "@/api/role";

const formatAvatarUrl = (url) => {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("/api")) {
    return url;
  }
  return `/api${url}`;
};

const loading = ref(false);
const tableData = ref([]);
const roleOptions = ref([]);
const roleLabelMap = ref({});

const searchForm = reactive({
  username: "",
  realName: "",
  auditStatus: undefined,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const rejectDialog = reactive({
  visible: false,
  user: null,
  reason: "",
  submitting: false,
});

const roleDialog = reactive({
  visible: false,
  user: null,
  selectedRoleId: null,
  submitting: false,
});

const fetchEnabledRoles = async () => {
  const res = await getEnabledRoleList();
  const roles = Array.isArray(res?.data) ? res.data : [];
  roleOptions.value = roles.map((item) => ({
    id: Number(item.id),
    code: item.roleCode,
    label: item.roleName,
  }));
  roleLabelMap.value = roleOptions.value.reduce((acc, item) => {
    acc[item.code] = item.label;
    return acc;
  }, {});
};

const buildQueryParams = () => {
  const params = {
    current: pagination.current,
    size: pagination.size,
    username: searchForm.username || undefined,
    realName: searchForm.realName || undefined,
  };
  if (searchForm.auditStatus !== undefined && searchForm.auditStatus !== "") {
    params.auditStatus = searchForm.auditStatus;
  }
  return params;
};

const fetchUserPage = async () => {
  loading.value = true;
  try {
    const res = await getUserPage(buildQueryParams());
    const pageData = res?.data || {};
    tableData.value = Array.isArray(pageData.records) ? pageData.records : [];
    pagination.total = Number(pageData.total || 0);
    pagination.current = Number(pageData.current || pagination.current);
    pagination.size = Number(pageData.size || pagination.size);
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.current = 1;
  fetchUserPage();
};

const handleReset = () => {
  searchForm.username = "";
  searchForm.realName = "";
  searchForm.auditStatus = undefined;
  pagination.current = 1;
  fetchUserPage();
};

const handleCurrentChange = (val) => {
  pagination.current = val;
  fetchUserPage();
};

const handleSizeChange = (val) => {
  pagination.size = val;
  pagination.current = 1;
  fetchUserPage();
};

const openRejectDialog = (row) => {
  rejectDialog.user = row;
  rejectDialog.reason = "";
  rejectDialog.visible = true;
};

const handleStatusChange = async (row, val) => {
  const newStatus = val ? 1 : 0;
  const previous = row.status;
  row.status = newStatus;
  try {
    await updateUserStatus(row.id, newStatus);
    ElMessage.success(newStatus === 1 ? "用户已启用" : "用户已禁用");
  } catch {
    row.status = previous;
  }
};

const openRoleDialog = (row) => {
  roleDialog.user = row;
  roleDialog.selectedRoleId = Array.isArray(row.roleIds) && row.roleIds.length > 0
    ? Number(row.roleIds[0])
    : null;
  roleDialog.visible = true;
};

const submitRoles = async () => {
  if (!roleDialog.user?.id) return;
  if (!roleDialog.selectedRoleId) {
    ElMessage.warning("必须且只能选择一个身份！");
    return;
  }

  roleDialog.submitting = true;
  try {
    const roleIds = [roleDialog.selectedRoleId];
    await assignUserRoles(roleDialog.user.id, roleIds);
    ElMessage.success("角色分配成功");

    const selectedCodes = roleOptions.value.filter((x) => roleIds.includes(x.id)).map((x) => x.code);
    roleDialog.user.roles = selectedCodes;
    roleDialog.user.roleIds = roleIds;

    roleDialog.visible = false;
  } finally {
    roleDialog.submitting = false;
  }
};

const displayRoles = (row) => {
  if (Array.isArray(row.roles) && row.roles.length > 0) {
    return row.roles.map((code) => roleLabelMap.value[code] || code);
  }
  if (Array.isArray(row.roleIds) && row.roleIds.length > 0) {
    return roleOptions.value.filter((item) => row.roleIds.includes(item.id)).map((item) => item.label);
  }
  return [];
};

const formatTime = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

onMounted(async () => {
  await fetchEnabledRoles();
  fetchUserPage();
});
</script>

<style scoped>
.user-manage {
  padding: 0;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: var(--theme-text-primary);
}

.search-form {
  margin-bottom: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.search-form :deep(.el-form-item) {
  margin-right: 0 !important;
  margin-bottom: 0 !important;
}


.role-tag {
  margin-right: 6px;
  margin-bottom: 4px;
  background: rgba(0, 210, 255, 0.25) !important;
  color: var(--theme-cyan) !important;
  border: 1px solid rgba(0, 210, 255, 0.5) !important;
}

.muted {
  color: var(--theme-text-muted);
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.dialog-user {
  margin-bottom: 14px;
  color: var(--theme-text-secondary);
}

.search-form .el-button--primary {
  background: var(--theme-klein-blue);
  border: none;
  box-shadow: 0 4px 12px rgba(0, 47, 167, 0.2);
  border-radius: 6px;
  transition: all 0.25s ease;
}
.search-form .el-button--primary:hover {
  background: var(--theme-klein-blue-light);
  transform: translateY(-1px);
}
.search-form .el-button {
  border-radius: 6px;
}

/* ═══ 表格操作列按钮组 ═══ */
.action-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px; /* 统一间距，呼吸感更好 */
}

/* 剥离原本的自带间距，完全由外层 gap 接管 */
.action-group .el-button {
  margin: 0 !important;
}

.action-btn {
  border-radius: 6px;
}

.role-btn {
  border-radius: 6px;
  background: rgba(0, 47, 167, 0.04) !important;
  border: 1px solid rgba(0, 47, 167, 0.15) !important;
  color: var(--theme-klein-blue) !important;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.role-btn:hover {
  background: var(--theme-klein-blue) !important;
  color: #fff !important;
  box-shadow: 0 4px 12px rgba(0, 47, 167, 0.25) !important;
  border-color: transparent !important;
}

.role-radio-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}
.role-radio {
  margin: 0 !important;
  border-radius: 8px;
  border-color: rgba(0, 0, 0, 0.08) !important;
  background: rgba(255, 255, 255, 0.5);
}
.role-radio.is-checked {
  border-color: var(--theme-klein-blue) !important;
  background: var(--theme-klein-blue-soft);
}
.role-code {
  font-size: 12px;
  color: var(--theme-text-muted);
  margin-left: 4px;
}
</style>
