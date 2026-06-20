<template>
  <div class="wrong-book-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">📖 错题本</h2>
        <span class="page-sub" v-if="total > 0">共 {{ total }} 道错题待复习</span>
      </div>
      <el-button type="primary" size="default" @click="goToQuiz">
        <el-icon><EditPen /></el-icon> 去答题巩固
      </el-button>
    </div>

    <el-divider border-style="dashed" />

    <!-- 错题列表 -->
    <template v-if="total > 0">
      <div class="wrong-list" v-loading="loading">
        <div
          class="wrong-item"
          v-for="item in wrongList"
          :key="item.questionId"
        >
          <div class="wrong-header">
            <el-tag
              size="small"
              :type="questionTypeColor(item.questionType)"
              effect="dark"
            >
              {{ item.questionType }}
            </el-tag>
            <span class="wrong-count">
              做错 <b>{{ item.wrongCount }}</b> 次
            </span>
            <span class="wrong-time">{{ item.lastWrongTime }}</span>
          </div>

          <div class="wrong-stem">{{ item.stem }}</div>

          <!-- 解析（可展开） -->
          <el-collapse v-if="item.explanation" class="explanation-collapse">
            <el-collapse-item title="查看解析">
              <div class="explanation-content">{{ item.explanation }}</div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page.pageNum"
          :page-size="page.pageSize"
          :total="page.total"
          layout="prev, pager, next"
          background
          size="small"
          @current-change="fetchWrongBook"
        />
      </div>
    </template>

    <!-- 空状态 -->
    <el-empty v-else-if="!loading" description="太棒了，暂无错题！">
      <el-button type="primary" @click="goToQuiz">去做新题</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { EditPen } from "@element-plus/icons-vue";
import { getWrongBook } from "@/api/learning";

const router = useRouter();
const loading = ref(false);
const wrongList = ref([]);
const total = ref(0);
const page = reactive({ pageNum: 1, pageSize: 10, total: 0 });

const questionTypeColor = (type) => {
  if (type === "单选题") return "";
  if (type === "多选题") return "success";
  if (type === "判断题") return "warning";
  return "info";
};

const fetchWrongBook = async () => {
  loading.value = true;
  try {
    const res = await getWrongBook(page.pageNum, page.pageSize);
    const d = res.data.data;
    wrongList.value = d.records ?? [];
    page.total = d.total ?? 0;
    total.value = d.total ?? 0;
  } catch (err) {
    ElMessage.error("获取错题本失败");
    wrongList.value = [];
  } finally {
    loading.value = false;
  }
};

const goToQuiz = () => {
  router.push("/quiz");
};

onMounted(fetchWrongBook);
</script>

<style scoped>
.wrong-book-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 8px 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--theme-text-primary);
  margin: 0;
}

.page-sub {
  font-size: 14px;
  color: var(--theme-text-muted);
  margin-left: 12px;
}

.wrong-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 200px;
}

.wrong-item {
  border: 1px solid var(--theme-border-light);
  border-radius: 12px;
  padding: 18px 20px;
  background: var(--theme-card-bg);
  transition: box-shadow 0.3s ease;
}

.wrong-item:hover {
  box-shadow: var(--theme-shadow);
}

.wrong-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.wrong-count {
  font-size: 13px;
  color: var(--theme-coral);
}

.wrong-count b {
  font-weight: 700;
}

.wrong-time {
  font-size: 13px;
  color: var(--theme-text-muted);
  margin-left: auto;
}

.wrong-stem {
  font-size: 15px;
  color: var(--theme-text-primary);
  line-height: 1.7;
  margin-bottom: 8px;
}

.explanation-collapse {
  border: none;
  margin-top: 4px;
}

:deep(.explanation-collapse .el-collapse-item__header) {
  font-size: 13px;
  color: var(--theme-klein-blue);
  border: none;
  padding: 0;
  height: 32px;
  line-height: 32px;
}

:deep(.explanation-collapse .el-collapse-item__wrap) {
  border: none;
}

:deep(.explanation-collapse .el-collapse-item__content) {
  padding: 8px 0 0;
}

.explanation-content {
  font-size: 14px;
  color: var(--theme-text-secondary);
  line-height: 1.8;
  padding: 12px 16px;
  background: var(--theme-primary-soft);
  border-radius: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .wrong-header {
    flex-wrap: wrap;
  }

  .wrong-time {
    margin-left: 0;
    width: 100%;
  }
}
</style>
