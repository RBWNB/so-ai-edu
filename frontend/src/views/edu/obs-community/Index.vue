<template>
  <div class="community-page">
    <div class="community-header">
      <div class="header-top-row">
        <h2 class="page-title">
          <span class="title-icon">🌊</span>
          海友社区
        </h2>
        <div class="search-box">
          <el-input
              v-model="searchKeyword"
              placeholder="搜索帖子标题..."
              clearable
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
              class="search-input"
          />
          <el-button type="primary" class="search-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
      </div>
      <div class="header-bottom-row">
        <span class="page-subtitle">分享你的海洋观察发现</span>
        <div class="header-tools">
          <button class="sort-toggle" @click="switchSort(sortMode === 'latest' ? 'hot' : 'latest')">
            <el-icon :size="15"><component :is="sortMode === 'latest' ? Clock : Lightning" /></el-icon>
            {{ sortMode === 'latest' ? '最新' : '最热' }}
          </button>
          <span class="stat-badge">共 {{ totalCount }} 条</span>
          <el-button type="primary" class="publish-btn" @click="goPublish">
            <el-icon><Plus /></el-icon> 发布
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="loading && posts.length === 0" class="loading-feed">
      <div v-for="i in 3" :key="i" class="skeleton-feed-card">
        <el-skeleton :rows="4" animated />
      </div>
    </div>

    <el-empty v-else-if="!loading && posts.length === 0" description="暂无观察分享" :image-size="120">
      <template #description><span>还没有人分享观察记录，快来发布第一条吧！</span></template>
      <el-button type="primary" @click="goPublish">发布观察</el-button>
    </el-empty>

    <div v-else class="bili-feed-list">
      <div
          v-for="post in sortedPosts"
          :key="post.id"
          class="feed-card"
          @click="openDetail(post)"
      >
        <div class="feed-header">
          <div class="feed-avatar-frame" :class="'frame-' + (post.avatarFrame || 'default')">
            <el-avatar :size="46" :src="formatAvatar(post.avatarUrl)">
              <el-icon :size="20"><User /></el-icon>
            </el-avatar>
          </div>
          <div class="feed-user-info">
            <div class="feed-user-name">
              <span class="username">{{ post.username }}</span>
              <span v-if="post.userTitle" class="user-badge">{{ post.userTitle }}</span>
            </div>
            <div class="feed-time">{{ post.createdAt }}</div>
          </div>
        </div>

        <div class="feed-body">
          <h3 class="feed-title">{{ post.title }}</h3>
          <p class="feed-desc" v-if="post.description">{{ post.description }}</p>

          <div class="feed-img-wrapper" v-if="post.photoUrl">
            <el-image
                :src="post.photoUrl"
                fit="cover"
                class="feed-img"
                lazy
            />
          </div>
        </div>

        <div class="feed-hot-comment" v-if="post.hotComment">
          <div class="hc-tag">
            <el-icon><svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path fill="currentColor" d="M128 224v512a64 64 0 0 0 64 64h234.688l143.04 143.04a32 32 0 0 0 54.272-22.656V800H832a64 64 0 0 0 64-64V224a64 64 0 0 0-64-64H192a64 64 0 0 0-64 64zm64-128h640a128 128 0 0 1 128 128v576a128 128 0 0 1-128 128H640v140.8a96 96 0 0 1-162.816 67.84L317.056 864H192A128 128 0 0 1 64 736V224A128 128 0 0 1 192 96z"></path></svg></el-icon>
            热评
          </div>
          <div class="hc-text">
            <span class="hc-user">@{{ post.hotComment.username }}</span>：{{ post.hotComment.content }}
          </div>
        </div>

        <div class="feed-footer">
          <div class="footer-btn" @click.stop="handleShare(post)">
            <el-icon><Share /></el-icon> 分享
          </div>
          <div class="footer-btn">
            <el-icon><ChatDotSquare /></el-icon> {{ post.commentCount || '评论' }}
          </div>
          <div class="footer-btn" :class="{ liked: post.liked }" @click.stop="toggleListLike(post)">
            <el-icon><CircleCheck /></el-icon> {{ post.likeCount || '点赞' }}
          </div>
        </div>
      </div>
    </div>

    <div v-if="totalCount > pageSize" class="pagination-wrapper">
      <el-pagination
          v-model:current-page="pageNum"
          :page-size="pageSize"
          :total="totalCount"
          layout="prev, pager, next"
          background
          size="small"
          @current-change="loadPosts"
      />
    </div>

    <el-backtop :right="30" :bottom="80" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "@/store/auth";
import { ElMessage } from "element-plus";
import {
  Plus, User, Clock, CircleCheck, ChatDotSquare, Share,
  Lightning, Search
} from "@element-plus/icons-vue";
import { getCommunityObservations } from "@/api/observation";
import { toggleLike as toggleLikeApi } from "@/api/like";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// ===== 社区列表（单列信息流） =====

const posts = ref([]);
const loading = ref(false);
const pageNum = ref(1);
const pageSize = ref(10);
const totalCount = ref(0);
const sortMode = ref("latest");
const searchKeyword = ref('');

const handleSearch = () => {
  pageNum.value = 1;
  loadPosts();
};

const switchSort = (mode) => {
  if (sortMode.value === mode) return;
  sortMode.value = mode;
  pageNum.value = 1;
  loadPosts();
};

const sortedPosts = computed(() => {
  if (sortMode.value === "hot") {
    return [...posts.value].sort(
        (a, b) => (b.likeCount + b.commentCount * 2) - (a.likeCount + a.commentCount * 2)
    );
  }
  return posts.value;
});

const formatAvatar = (url) => {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("/api")) return url;
  return `/api${url}`;
};

const loadPosts = async () => {
  loading.value = true;
  try {
    const res = await getCommunityObservations({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sort: sortMode.value,
      keyword: searchKeyword.value.trim() || undefined,
    });

    if (res.data.success) {
      const d = res.data.data;
      posts.value = d.records || [];
      totalCount.value = d.total || 0;
    } else {
      posts.value = [];
      totalCount.value = 0;
    }
  } catch (err) {
    console.error("加载社区观察失败", err);
    ElMessage.error("加载失败");
    posts.value = [];
  } finally {
    loading.value = false;
  }
};

const goPublish = () => router.push("/observation/publish");

// 外部名片流快速点赞
const toggleListLike = async (post) => {
  try {
    const res = await toggleLikeApi("user_observation", post.id);
    if (res.data.success) {
      post.liked = res.data.data.liked;
      post.likeCount = res.data.data.count;
    }
  } catch (err) { console.error(err); }
};

// 点击帖子 → 跳转到独立详情页面，并保存滚动位置
const openDetail = (post) => {
  sessionStorage.setItem('community_scroll_top', window.scrollY);
  sessionStorage.setItem('community_page_num', String(pageNum.value));
  sessionStorage.setItem('community_sort', sortMode.value);
  sessionStorage.setItem('community_keyword', searchKeyword.value);
  router.push({ name: 'EduObservationDetail', params: { id: post.id } });
};

// 页面完全重新加载时：恢复上次浏览位置
const restoreScroll = async () => {
  const savedScroll = sessionStorage.getItem('community_scroll_top');
  if (savedScroll) {
    sessionStorage.removeItem('community_scroll_top');
    await nextTick();
    window.scrollTo(0, parseInt(savedScroll));
  }
};

onMounted(async () => {
  // 恢复搜索/排序/分页状态
  const savedPage = sessionStorage.getItem('community_page_num');
  const savedSort = sessionStorage.getItem('community_sort');
  const savedKeyword = sessionStorage.getItem('community_keyword');
  if (savedPage) {
    pageNum.value = parseInt(savedPage);
    sessionStorage.removeItem('community_page_num');
  }
  if (savedSort) {
    sortMode.value = savedSort;
    sessionStorage.removeItem('community_sort');
  }
  if (savedKeyword) {
    searchKeyword.value = savedKeyword;
    sessionStorage.removeItem('community_keyword');
  }

  await loadPosts();
  await restoreScroll();

  // 兼容旧版 detail 查询参数 → 跳转到新详情页
  const detailId = route.query.detail;
  if (detailId) {
    router.replace({ name: 'EduObservationDetail', params: { id: detailId } });
  }
});

// 分享处理函数
const handleShare = async (post) => {
  // 1. 拼接完整的绝对路径 URL（Web Share 和剪贴板通常都需要完整的 URL）
  // 假设你的详情页路由是 /observation/detail/:id
  const targetUrl = `${window.location.origin}/observation/detail/${post.id}`;

  // 2. 准备分享的数据
  const shareData = {
    title: '海友社区 - 观察分享',
    text: `快来看看 ${post.username} 的海洋观察记录：${post.title}`,
    url: targetUrl,
  };

  // 3. 检查当前环境是否支持 Web Share API
  if (navigator.share && navigator.canShare && navigator.canShare(shareData)) {
    try {
      await navigator.share(shareData);
      console.log('唤起系统分享成功');
      // 注意：这里不需要弹 Toast，因为系统面板已经给了明确的反馈
    } catch (err) {
      // 用户主动取消分享也会触发 catch，需要排除 AbortError
      if (err.name !== 'AbortError') {
        console.error('系统分享出错', err);
        fallbackToClipboard(targetUrl);
      }
    }
  } else {
    // 4. 不支持的话，自动降级为复制链接
    fallbackToClipboard(targetUrl);
  }
};

// 降级方案：复制到剪贴板
const fallbackToClipboard = async (text) => {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      // 推荐的现代剪贴板 API (需 HTTPS)
      await navigator.clipboard.writeText(text);
      ElMessage.success('帖子链接已复制，快去分享给好友吧！');
    } else {
      // 更老旧环境的降级方案 (如 HTTP 开发环境)
      const textArea = document.createElement("textarea");
      textArea.value = text;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand("copy");
      document.body.removeChild(textArea);
      ElMessage.success('帖子链接已复制，快去分享给好友吧！');
    }
  } catch (err) {
    console.error('复制失败', err);
    ElMessage.error('分享失败，请重试');
  }
};
</script>

<style scoped>
/* 整个大页面背景变淡灰，突出独立的白色卡片 */
.community-page {
  max-width: 100%;
  min-height: 100vh;
  background: transparent;
  padding: 16px 0 40px;
}

/* 将头部区域限制宽度并居中 — 与帖子列表等宽 */
.community-header {
  max-width: 960px;
  margin: 0 auto 20px;
  padding: 0 12px;
}

.header-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #48cae4, #0077b6, #023e8a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
  letter-spacing: 1px;
}

.title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(0, 210, 255, 0.15);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(0, 210, 255, 0.4);
  font-size: 20px;
  -webkit-text-fill-color: initial;
}

.search-box { display: flex; align-items: center; gap: 8px; }
.search-box .search-input { width: 240px; }
.search-btn {
  border-radius: 20px !important;
  padding: 8px 20px !important;
  background: linear-gradient(135deg, #0077b6, #00b4d8) !important;
  border: none !important;
  font-weight: 600;
}

.header-bottom-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; }
.page-subtitle { font-size: 13px; color: rgba(235, 245, 255, 0.6); }

.header-tools { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.stat-badge {
  font-size: 13px; color: #334155; background: rgba(255,255,255,0.85);
  padding: 4px 12px; border-radius: 20px; border: 1px solid rgba(0,0,0,0.06);
}
.publish-btn { border-radius: 20px; }
.sort-toggle {
  display: inline-flex; align-items: center; gap: 5px; padding: 6px 16px;
  border: 1px solid rgba(0,0,0,0.10); border-radius: 20px; background: rgba(255,255,255,0.85);
  color: #334155; font-size: 13px; cursor: pointer; transition: all 0.25s;
}

/* 垂直单列布局样式 */
.loading-feed {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 12px;
}
.skeleton-feed-card {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 8px;
  padding: 20px;
}

.bili-feed-list {
  max-width: 960px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 0 12px;
}

.feed-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 24px 28px 16px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}
.feed-card:hover {
  box-shadow: 0 8px 24px rgba(0, 210, 255, 0.25);
  transform: translateY(-2px);
}

/* 头部发布者信息 */
.feed-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.feed-avatar-frame { display: inline-flex; border-radius: 50%; padding: 3px; flex-shrink: 0; align-items: center; justify-content: center; }
.feed-user-info { display: flex; flex-direction: column; justify-content: center; }
.feed-user-name { display: flex; align-items: center; gap: 8px; }
.username { font-size: 15px; font-weight: 600; color: #fb7299; /* B站标志粉色 */ }
.user-badge { font-size: 10px; color: #b8860b; background: rgba(255,215,0,0.15); padding: 1px 6px; border-radius: 4px; }
.feed-time { font-size: 12px; color: #99a2aa; margin-top: 3px; }

/* 动态内容正文 */
.feed-body { margin-bottom: 12px; padding-left: 62px; /* 对齐头部头像右侧起步线 */ }
.feed-title { font-size: 16px; font-weight: 600; color: #222; margin: 0 0 8px; }
.feed-desc {
  font-size: 15px;
  color: #333;
  line-height: 1.6;
  margin: 0 0 12px;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical;
  overflow: hidden; word-break: break-word;
}
.feed-img-wrapper {
  max-width: 600px;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.04);
}
.feed-img {
  width: 100%;
  max-height: 400px;
  display: block;
}

/* 热门评论包裹盒子 */
.feed-hot-comment {
  margin-left: 62px; margin-bottom: 14px; background: #f4f5f7;
  padding: 10px 14px; border-radius: 6px; font-size: 13px; color: #505050;
  display: flex; gap: 8px; align-items: flex-start;
}
.hc-tag { display: flex; align-items: center; gap: 4px; color: #ff6699; font-weight: 600; flex-shrink: 0; }
.hc-text { line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; word-break: break-word; }
.hc-user { color: #008ac5; font-weight: 500; }

/* 底部功能栏 */
.feed-footer { display: flex; align-items: center; border-top: 1px solid #edf2f9; padding-top: 10px; }
.footer-btn { flex: 1; display: flex; justify-content: center; align-items: center; gap: 6px; font-size: 13px; color: #99a2aa; cursor: pointer; transition: color 0.2s; }
.footer-btn:hover { color: #00a1d6; }
.footer-btn.liked { color: #00a1d6; }

/* ═══ 分页 ═══ */
.pagination-wrapper { display: flex; justify-content: center; margin-top: 28px; }

/* ===== 头像框样式 ===== */
.frame-default {
  background: #dcdfe6;
}

/* 黄金边框 */
.frame-gold {
  background: linear-gradient(135deg, #f6d365, #fda085);
  box-shadow: 0 0 14px rgba(246, 211, 101, 0.6);
}

/* 深海边框 */
.frame-ocean {
  background: linear-gradient(135deg, #00d2ff, #165dff);
  box-shadow: 0 0 16px rgba(0, 210, 255, 0.6);
}

/* 彩虹边框 */
.frame-rainbow {
  background: linear-gradient(90deg, #ff6b6b, #feca57, #48dbfb, #ff9ff3);
  background-size: 200% 100%;
  animation: frame-rainbow-spin 3s linear infinite;
}
@keyframes frame-rainbow-spin {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}

/* 火焰边框 */
.frame-flame {
  background: linear-gradient(135deg, #ff4500, #ff8c00, #ffd700);
  box-shadow: 0 0 18px rgba(255, 69, 0, 0.6);
}

/* 烈焰虚线 — 橙红渐变 + 脉冲光晕 */
.frame-dashed {
  background: repeating-conic-gradient(
      #fd7000 0deg 18deg,
      transparent 18deg 36deg
  );
  box-shadow: 0 0 14px rgba(237, 35, 76, 0.5);
  animation: frame-dash-glow 2s ease-in-out infinite;
}
@keyframes frame-dash-glow {
  0%, 100% { box-shadow: 0 0 14px rgba(237, 35, 76, 0.5); }
  50% { box-shadow: 0 0 26px rgba(237, 35, 76, 0.85); }
}

/* 霓虹光效 — 青 → 品红渐变色 + 双重光晕 */
.frame-neon {
  background: linear-gradient(135deg, #00fff5, #ff00e4);
  box-shadow:
      0 0 14px rgba(0, 255, 245, 0.7),
      0 0 28px rgba(255, 0, 228, 0.4);
  animation: frame-neon-pulse 3s ease-in-out infinite;
}
@keyframes frame-neon-pulse {
  0%, 100% { box-shadow: 0 0 14px rgba(0, 255, 245, 0.7), 0 0 28px rgba(255, 0, 228, 0.4); }
  50% { box-shadow: 0 0 24px rgba(0, 255, 245, 0.9), 0 0 44px rgba(255, 0, 228, 0.6); }
}

/* 极光幻彩 — 绿 → 蓝 → 紫 流动渐变 */
.frame-aurora {
  background: linear-gradient(135deg, #00f260, #0575e6, #a855f7);
  background-size: 200% 200%;
  animation: frame-aurora-shift 4s ease infinite;
  box-shadow: 0 0 16px rgba(5, 117, 230, 0.5);
}
@keyframes frame-aurora-shift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

/* 赛博光轮 — 青紫脉冲 */
.frame-crystal {
  background: linear-gradient(135deg, #00f5ff, #ff00e5);
  box-shadow:
      0 0 16px rgba(0, 245, 255, 0.5),
      0 0 32px rgba(255, 0, 229, 0.3),
      inset 0 0 8px rgba(255, 255, 255, 0.4);
  animation: crystal-breath 2s ease-in-out infinite;
}
@keyframes crystal-breath {
  0%, 100% { box-shadow: 0 0 16px rgba(0, 245, 255, 0.5), 0 0 32px rgba(255, 0, 229, 0.3); }
  50% { box-shadow: 0 0 28px rgba(0, 245, 255, 0.8), 0 0 56px rgba(255, 0, 229, 0.5); }
}

/* 紫金皇冠 — 深紫 + 金线点缀 */
.frame-royal {
  background: linear-gradient(135deg, #6c3cc7, #9b59b6, #f1c40f);
  box-shadow: 0 0 16px rgba(108, 60, 199, 0.6);
}
/* 所有非默认框：头像加 3px 白边（box-sizing:content-box 保持内容区不缩） */
.frame-gold .el-avatar,
.frame-ocean .el-avatar,
.frame-rainbow .el-avatar,
.frame-flame .el-avatar,
.frame-dashed .el-avatar,
.frame-neon .el-avatar,
.frame-aurora .el-avatar,
.frame-royal .el-avatar {
  border: 3px solid #fff;
  border-radius: 50%;
}
.frame-crystal .el-avatar {
  border: 3px solid rgba(255, 255, 255, 0.9);
  border-radius: 50%;
}
.frame-default .el-avatar {
  border: 3px solid rgba(255, 255, 255, 0.7);
  border-radius: 50%;
}

/* ═══ 微交互动效 (Index.vue) ═══ */
/* 点赞 - Q弹放大特效 */
@keyframes heart-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.4); }
  70% { transform: scale(0.9); }
  100% { transform: scale(1); }
}

/* 当包含 liked 类时，内部的 el-icon 触发动画 */
.footer-btn.liked .el-icon {
  animation: heart-pop 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}

/* ═══ 滚动条（弹窗内） ═══ */

/* ═══ 响应式 ═══ */
@media (max-width: 768px) {
  .community-page { padding: 0 0 20px; }
  .community-header {
    padding: 0 12px;
  }

  .header-top-row { flex-wrap: wrap; }
  .search-box { width: 100%; }
  .search-box .search-input { flex: 1; }
  .search-btn { white-space: nowrap; }

  .header-bottom-row { flex-direction: column; align-items: stretch; gap: 8px; }
  .page-subtitle { display: none; }
  .header-tools { justify-content: space-between; width: 100%; }

  /* 移动端信息流调整 */
  .feed-body, .feed-hot-comment { margin-left: 0; margin-top: 10px; }
  .feed-img-wrapper { max-width: 100%; }

}
</style>
