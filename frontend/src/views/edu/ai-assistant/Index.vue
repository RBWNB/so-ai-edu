<template>
  <div class="identify-page">
    <div class="page-header">
      <h1 class="page-title">海洋生物智能探测</h1>
      <p class="page-subtitle">上传影像样本，接入海洋学堂数据库进行深度特征分析</p>
    </div>
    <AiIdentifyUp />
  </div>
</template>

<script>
export default {
  name: "AiAssistantView",
};
</script>

<script setup>
import AiIdentifyUp from "@/components/AiIdentifyUp.vue";
</script>

<style scoped>
.identify-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 10px 0 40px;
  animation: fadeIn 0.6s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 36px;
  font-weight: 900;
  margin: 0 0 12px;
  background: linear-gradient(135deg, #165dff 0%, #00d2ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 2px;
}

.page-subtitle {
  font-size: 15px;
  color: #86909c;
  margin: 0;
  letter-spacing: 1px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 深色模式：副标题改为浅色 */
[data-theme="dark"] .page-subtitle {
  color: #b0bec5;
}
</style>
