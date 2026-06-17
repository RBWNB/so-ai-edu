<template>
  <div class="operation-log-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">操作日志</div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="操作人">
          <el-input v-model="searchForm.username" placeholder="请输入操作人账号" clearable />
        </el-form-item>
        <el-form-item label="操作模块">
          <el-input v-model="searchForm.module" placeholder="请输入操作模块" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志表格 -->
      <el-table v-loading="loading" :data="tableData" border stripe>
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="操作人" min-width="120" />
        <el-table-column prop="module" label="操作模块" min-width="140" />
        <el-table-column prop="description" label="操作描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="requestUrl" label="请求URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" min-width="130" />
        <el-table-column prop="executionTime" label="执行耗时(ms)" width="140" align="center" />
        <el-table-column label="操作状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row)" size="small">
              {{ statusLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" min-width="180" />
      </el-table>

      <!-- 分页 -->
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { getOperationLogPage } from "@/api/system";

const loading = ref(false);
const tableData = ref([]);

const searchForm = reactive({
  username: "",
  module: "",
});

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
});

// 获取操作日志列表
const fetchOperationLogPage = async () => {
  loading.value = true;
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      username: searchForm.username || undefined,
      module: searchForm.module || undefined,
    };

    const res = await getOperationLogPage(params);
    const pageData = res?.data || {};
    tableData.value = Array.isArray(pageData.records) ? pageData.records : [];
    pagination.total = Number(pageData.total || 0);
    pagination.current = Number(pageData.current || pagination.current);
    pagination.size = Number(pageData.size || pagination.size);
  } catch (err) {
    ElMessage.error("获取操作日志失败");
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.current = 1;
  fetchOperationLogPage();
};

const handleReset = () => {
  searchForm.username = "";
  searchForm.module = "";
  pagination.current = 1;
  fetchOperationLogPage();
};

const handleCurrentChange = (val) => {
  pagination.current = val;
  fetchOperationLogPage();
};

const handleSizeChange = (val) => {
  pagination.size = val;
  pagination.current = 1;
  fetchOperationLogPage();
};

const statusLabel = (row) => {
  const code = row.resultCode;
  const text = row.status === "SUCCESS" ? "成功" : "失败";
  if (code != null) {
    return `${code} ${text}`;
  }
  return text;
};

const statusTagType = (row) => {
  if (row.status === "SUCCESS") return "success";
  const code = row.resultCode;
  if (code != null && code >= 400 && code < 500) return "warning";
  return "danger";
};

onMounted(() => {
  fetchOperationLogPage();
});
</script>

<style scoped>
.operation-log-page {
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