<template>
  <div class="identify-page">
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
  max-width: 800px;
  margin: 0 auto;
  padding: 4px 0;
}
</style>
