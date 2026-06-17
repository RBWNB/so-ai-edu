<template>
  <div class="quiz-manage">
    <div class="page-header">
      <h2>题库管理</h2>
      <p>管理答题闯关的题目、分类与难度</p>
    </div>

    <el-empty description="题库管理模块建设中，敬请期待...">
      <el-button type="primary">创建第一道题目</el-button>
    </el-empty>
  </div>
</template>

<script setup>
</script>

<style scoped>
.quiz-manage {
  padding: 0;
}
.page-header {
  margin-bottom: 24px;
}
.page-header h2 {
  font-size: 22px;
  margin: 0 0 4px;
}
.page-header p {
  color: #8892a4;
  margin: 0;
  font-size: 14px;
}
</style>
