<template>
  <div class="role-manage">
    <el-card>
      <template #header>
        <div class="card-header">角色管理</div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="角色名称">
          <el-input v-model="searchForm.roleName" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="searchForm.roleCode" placeholder="请输入角色编码" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openCreateDialog">新增角色</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="roleCode" label="角色编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="角色描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="(val) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center">
          <template #default="{ row }">
            <el-button type="primary" plain size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" plain size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog
      v-model="formDialog.visible"
      :title="formDialog.mode === 'create' ? '新增角色' : '编辑角色'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formDialog.form" :rules="formRules" label-position="top">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formDialog.form.roleName" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="formDialog.form.roleCode"
            placeholder="例如 ADMIN"
            :disabled="formDialog.mode === 'edit'"
          />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="formDialog.form.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formDialog.form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="formDialog.submitting" @click="submitRole">确认</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限已废弃：固定角色 ADMIN/MANAGER/VISITOR 不再支持动态菜单授权 -->
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { createRole, deleteRole, getRolePage, updateRole, updateRoleStatus } from "@/api/role";

const loading = ref(false);
const tableData = ref([]);
const formRef = ref(null);

const searchForm = reactive({
  roleName: "",
  roleCode: "",
  status: undefined,
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

const formDialog = reactive({
  visible: false,
  mode: "create",
  submitting: false,
  form: {
    id: null,
    roleName: "",
    roleCode: "",
    description: "",
    status: 1,
  },
});

const formRules = {
  roleName: [{ required: true, message: "请输入角色名称", trigger: "blur" }],
  roleCode: [{ required: true, message: "请输入角色编码", trigger: "blur" }],
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
};

const fetchRolePage = async () => {
  loading.value = true;
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      roleName: searchForm.roleName || undefined,
      roleCode: searchForm.roleCode || undefined,
      status: searchForm.status === "" ? undefined : searchForm.status,
    };

    const res = await getRolePage(params);
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
  fetchRolePage();
};

const handleReset = () => {
  searchForm.roleName = "";
  searchForm.roleCode = "";
  searchForm.status = undefined;
  pagination.current = 1;
  fetchRolePage();
};

const handleCurrentChange = (val) => {
  pagination.current = val;
  fetchRolePage();
};

const handleSizeChange = (val) => {
  pagination.size = val;
  pagination.current = 1;
  fetchRolePage();
};

const resetDialogForm = () => {
  formDialog.form.id = null;
  formDialog.form.roleName = "";
  formDialog.form.roleCode = "";
  formDialog.form.description = "";
  formDialog.form.status = 1;
};

const openCreateDialog = () => {
  formDialog.mode = "create";
  resetDialogForm();
  formDialog.visible = true;
};

const openEditDialog = (row) => {
  formDialog.mode = "edit";
  formDialog.form.id = row.id;
  formDialog.form.roleName = row.roleName || "";
  formDialog.form.roleCode = row.roleCode || "";
  formDialog.form.description = row.description || "";
  formDialog.form.status = Number(row.status ?? 1);
  formDialog.visible = true;
};

const submitRole = async () => {
  if (!formRef.value) return;

  await formRef.value.validate(async (valid) => {
    if (!valid) return;

    formDialog.submitting = true;
    try {
      const payload = {
        roleName: formDialog.form.roleName?.trim(),
        roleCode: formDialog.form.roleCode?.trim().toUpperCase(),
        description: formDialog.form.description?.trim(),
        status: formDialog.form.status,
      };

      if (formDialog.mode === "create") {
        await createRole(payload);
        ElMessage.success("角色创建成功");
      } else {
        await updateRole(formDialog.form.id, payload);
        ElMessage.success("角色更新成功");
      }

      formDialog.visible = false;
      fetchRolePage();
    } catch (err) {
      ElMessage.error(err.response?.data || "操作失败");
    } finally {
      formDialog.submitting = false;
    }
  });
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除角色【${row.roleName}】吗？`, "提示", {
      type: "warning",
      confirmButtonText: "确定",
      cancelButtonText: "取消",
    });

    await deleteRole(row.id);
    ElMessage.success("角色已删除");
    fetchRolePage();
  } catch (err) {
    if (err !== "cancel" && err !== "close") {
      ElMessage.error(err.response?.data || "删除失败");
    }
  }
};

const handleStatusChange = async (row, val) => {
  const previous = row.status;
  row.status = val;
  try {
    await updateRoleStatus(row.id, val);
    ElMessage.success(val === 1 ? "角色已启用" : "角色已禁用");
  } catch (err) {
    row.status = previous;
    ElMessage.error(err.response?.data || "状态更新失败");
  }
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

onMounted(() => {
  fetchRolePage();
});
</script>

<style scoped>
.role-manage {
  padding: 0;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: var(--theme-text-primary);
}

.search-form {
  margin-bottom: 14px;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
