<template>
  <div class="exception-page">
    <div class="exception-card">
      <div class="exception-code">403</div>
      <h2>访问被拒绝</h2>
      <p>抱歉，您没有权限访问此页面</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
const $router = useRouter();
</script>

<style scoped>
.exception-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a1628 0%, #0d2137 100%);
}
.exception-card {
  text-align: center;
  color: #e0e6ed;
}
.exception-code {
  font-size: 120px;
  font-weight: 800;
  background: linear-gradient(135deg, #00d2ff, #3a7bd5);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1;
  margin-bottom: 16px;
}
.exception-card h2 {
  font-size: 24px;
  margin: 0 0 8px;
}
.exception-card p {
  color: #8892a4;
  margin: 0 0 24px;
}
</style>
