<template>
  <div class="edu-home">
    <!-- ═══ 轮播图 ═══ -->
    <section class="carousel-section">
      <el-carousel
        :interval="5000"
        arrow="always"
        indicator-position="outside"
        height="420px"
      >
        <el-carousel-item v-for="(slide, i) in slides" :key="i">
          <div class="slide-card" :style="{ background: slide.bg }">
            <div class="slide-text">
              <h2>{{ slide.title }}</h2>
              <p>{{ slide.desc }}</p>
              <el-button
                type="primary"
                size="large"
                round
                @click="handleSlideAction(slide)"
              >
                {{ slide.btn }}
              </el-button>
            </div>
            <div class="slide-icon">
              <el-icon :size="120"><component :is="slide.icon" /></el-icon>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </section>

    <!-- ═══ 功能卡片 ═══ -->
    <section class="feature-section">
      <h3 class="section-title">探索海洋世界</h3>
      <p class="section-sub">选择你感兴趣的方向，开始学习之旅</p>

      <div class="feature-grid">
        <el-card
          v-for="card in featureCards"
          :key="card.title"
          shadow="hover"
          class="feature-card"
          @click="handleFeatureClick(card)"
        >
          <div class="card-icon" :style="{ background: card.color }">
            <el-icon :size="32"><component :is="card.icon" /></el-icon>
          </div>
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
        </el-card>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { ChatDotRound, Compass, Picture, TrophyBase, Search, MagicStick } from "@element-plus/icons-vue";

const $router = useRouter();
const authStore = useAuthStore();

/* 辅助：跳转或去登录 */
const goOrLogin = (path) => {
  if (authStore.isLoggedIn) {
    $router.push(path);
  } else {
    $router.push({ path: "/login", query: { redirect: path } });
  }
};

/* ═══ 轮播数据 ═══ */
const slides = [
  {
    title: "探索海底两万里",
    desc: "从珊瑚礁到深海热泉，认识海洋生物的家园",
    btn: "开始探索",
    icon: "Search",
    bg: "linear-gradient(135deg, #0b2b5e 0%, #0d4f8b 40%, #0077b6 100%)",
    path: "/encyclopedia",
  },
  {
    title: "AI 智能识图",
    desc: "拍张照，让 AI 导师告诉你这是什么海洋生物",
    btn: "试试识图",
    icon: "MagicStick",
    bg: "linear-gradient(135deg, #0a2647 0%, #144272 40%, #205295 100%)",
    path: "/ai-assistant",
  },
  {
    title: "答题闯关挑战",
    desc: "闯关答题，检验你的海洋知识，赢取成就徽章",
    btn: "开始答题",
    icon: "TrophyBase",
    bg: "linear-gradient(135deg, #06283d 0%, #1363df 50%, #47b5ff 100%)",
    path: "/quiz",
  },
];

/* ═══ 轮播按钮点击 ═══ */
const handleSlideAction = (slide) => {
  goOrLogin(slide.path);
};

/* ═══ 功能卡片 ═══ */
const featureCards = [
  {
    title: "海洋百科",
    desc: "探索丰富的海洋生物知识库，从浮游生物到鲸类",
    path: "/encyclopedia",
    icon: "Picture",
    color: "rgba(0, 210, 255, 0.12)",
  },
  {
    title: "AI 导师",
    desc: "智能识别物种，实时答疑解惑，拍照即知",
    path: "/ai-assistant",
    icon: "ChatDotRound",
    color: "rgba(58, 123, 213, 0.12)",
  },
  {
    title: "探索地图",
    desc: "查看全球海洋生态分布，发现你身边的海洋",
    path: "/map-explore",
    icon: "Compass",
    color: "rgba(54, 207, 201, 0.12)",
  },
  {
    title: "答题闯关",
    desc: "趣味闯关答题，巩固所学知识，收集成就徽章",
    path: "/quiz",
    icon: "TrophyBase",
    color: "rgba(255, 184, 77, 0.12)",
  },
];

const handleFeatureClick = (card) => {
  goOrLogin(card.path);
};
</script>

<style scoped>
.edu-home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 24px 48px;
}

/* ── 轮播 ── */
.carousel-section {
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 48px;
  box-shadow: 0 8px 40px rgba(0, 30, 80, 0.18);
}
.carousel-section :deep(.el-carousel__indicators) {
  bottom: 16px;
}
.slide-card {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 80px;
  color: #fff;
  position: relative;
  overflow: hidden;
}
.slide-card::after {
  content: "";
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 70% 50%, rgba(255,255,255,0.06) 0%, transparent 60%);
  pointer-events: none;
}
.slide-text {
  z-index: 1;
  max-width: 480px;
}
.slide-text h2 {
  font-size: 40px;
  font-weight: 800;
  margin: 0 0 12px;
  letter-spacing: 2px;
  text-shadow: 0 2px 12px rgba(0,0,0,0.3);
}
.slide-text p {
  font-size: 17px;
  opacity: 0.88;
  margin: 0 0 28px;
  line-height: 1.6;
}
.slide-icon {
  z-index: 1;
  opacity: 0.2;
  color: #fff;
}

/* ── 功能卡片区 ── */
.feature-section {
  text-align: center;
}
.section-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--theme-text-primary, #1a1a2e);
}
.section-sub {
  font-size: 15px;
  color: var(--theme-text-muted, #8892a4);
  margin: 0 0 36px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.feature-card {
  cursor: pointer;
  border-radius: 14px;
  border: 1px solid rgba(0,0,0,0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: center;
  padding: 8px;
}
.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 36px rgba(0, 47, 167, 0.12);
  border-color: rgba(0, 210, 255, 0.25);
}
.card-icon {
  width: 68px;
  height: 68px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  margin: 8px auto 14px;
  color: var(--theme-primary, #0052d9);
}
.feature-card h3 {
  margin: 0 0 6px;
  font-size: 17px;
  color: var(--theme-text-primary, #1a1a2e);
}
.feature-card p {
  color: var(--theme-text-muted, #8892a4);
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
}

/* ── 响应式 ── */
@media (max-width: 960px) {
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .slide-card {
    padding: 0 32px;
  }
  .slide-text h2 {
    font-size: 28px;
  }
  .slide-icon {
    display: none;
  }
}
@media (max-width: 560px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
