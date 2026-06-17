<template>
  <div class="edu-quiz">
    <div class="quiz-header">
      <h2>答题闯关</h2>
      <p>通过趣味答题巩固海洋生物知识，收集你的成就徽章</p>
    </div>

    <el-empty description="题库建设中，敬请期待...">
      <el-button type="primary" @click="$router.push('/encyclopedia')">先去百科学习</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
const $router = useRouter();
</script>

<style scoped>
.edu-quiz {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}
.quiz-header {
  text-align: center;
  padding: 40px 20px;
  margin-bottom: 32px;
}
.quiz-header h2 {
  font-size: 32px;
  margin: 0 0 8px;
}
.quiz-header p {
  color: #8892a4;
  margin: 0;
}
</style>
