<template>
  <div class="agent-widget-wrapper">
    <div class="agent-widget-click" @click="handleClick">
      <div class="widget-box">
        <img
            class="widget-img"
            src="http://tgig77s29.hn-bkt.clouddn.com/123"
            alt="RAG 智能问答"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
const emit = defineEmits(["click"]);
const handleClick = () => {
  emit("click");
};
</script>

<style scoped>
.agent-widget-wrapper {
  /* 主色：深邃科技蓝 */
  --tech-blue-primary: #165DFF;
  /* 主色浅调：清透天蓝 */
  --tech-blue-light: #4080FF;
  /* 辅助色1：梦幻紫 */
  --tech-purple: #7B61FF;
  /* 辅助色2：浅紫蓝 */
  --tech-purple-light: #A089FF;
  /* 辅助色3：青蓝过渡色 */
  --tech-cyan: #36CFC9;
  /* 新增过渡色：蓝青中间色 */
  --tech-blue-cyan: #25B2E6;
  /* 新增过渡色：青紫中间色 */
  --tech-cyan-purple: #6099FF;
}

.agent-widget-wrapper {
  display: inline-flex;
  align-items: center;
  margin-right: 8px;
}

.agent-widget-click {
  cursor: pointer;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.agent-widget-click:hover {
  transform: scale(1.06);
}

.agent-widget-click:active {
  transform: scale(0.96);
}

.widget-box {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
      120deg,
      var(--tech-blue-primary),
      var(--tech-blue-light),
      var(--tech-blue-cyan),
      var(--tech-cyan),
      var(--tech-cyan-purple),
      var(--tech-purple),
      var(--tech-purple-light),
      var(--tech-blue-light),
      var(--tech-blue-primary)
  );
  background-size: 600% 100%;

  animation: rgb-flow 20s ease-in-out infinite;
  box-shadow: 0 4px 14px rgba(22, 93, 255, 0.25);
  transition: box-shadow 0.25s ease, filter 0.25s ease;
}


@keyframes rgb-flow {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 600% 50%;
  }
}

.agent-widget-click:hover .widget-box {

  box-shadow: 0 6px 18px rgba(22, 93, 255, 0.35);
  filter: brightness(1.05);
  animation-duration: 10s;
}

.widget-img {
  width: 24px;
  height: 24px;
  display: block;
  user-select: none;
  -webkit-user-drag: none;
}
</style>
