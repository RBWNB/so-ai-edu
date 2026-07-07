<template>
  <div class="ocean-community">
    <!-- ══════════════════════════════════════════ -->
    <!-- 模块一：顶部操作工具栏                         -->
    <!-- ══════════════════════════════════════════ -->
    <div class="community-toolbar">
      <div class="toolbar-top">
        <h2 class="page-title">
          <span class="title-icon">🌊</span>
          海友社区
        </h2>
        <div class="search-capsule">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            v-model="searchKeyword"
            type="text"
            class="search-input"
            placeholder="搜索海洋生物、游记..."
            @input="onSearchInput"
          />
          <el-icon
            v-if="searchKeyword"
            class="clear-icon"
            @click="clearSearch"
          ><CircleClose /></el-icon>
        </div>
      </div>

      <div class="category-tabs-wrapper">
        <div class="category-tabs">
          <button
            v-for="tab in categoryTabs"
            :key="tab.key"
            class="tab-item"
            :class="{ active: activeCategory === tab.key }"
            @click="switchCategory(tab.key)"
          >{{ tab.label }}</button>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════ -->
    <!-- 模块二：CSS Columns 瀑布流                    -->
    <!-- ══════════════════════════════════════════ -->
    <div v-if="loading && displayPosts.length === 0" class="masonry-grid">
      <div v-for="i in 6" :key="i" class="skeleton-card">
        <div class="skeleton-cover" :class="'skel-h-' + (i % 3 + 1)" />
        <div class="skeleton-body">
          <div class="skeleton-line skeleton-line-1" />
          <div class="skeleton-line skeleton-line-2" />
          <div class="skeleton-footer">
            <div class="skeleton-avatar" />
            <div class="skeleton-line skeleton-line-3" />
          </div>
        </div>
      </div>
    </div>

    <el-empty
      v-else-if="!loading && displayPosts.length === 0"
      description="暂无观察分享"
      :image-size="120"
    >
      <template #description>
        <span>还没有人分享观察记录，快来发布第一条吧！</span>
      </template>
      <el-button type="primary" @click="goPublish">发布观察</el-button>
    </el-empty>

    <div v-else class="masonry-grid">
      <div
        v-for="post in displayPosts"
        :key="post.id"
        class="post-card"
        @click="openDetail(post)"
      >
        <!-- 有图：封面图根据数据决定长短比例 -->
        <div v-if="post.photoUrl" class="card-cover">
          <img
            :src="formatPhotoUrl(post.photoUrl)"
            :alt="post.title"
            :style="{ aspectRatio: post.imageRatio || '4/3' }"
            class="cover-img"
            loading="lazy"
            @error="onImgError($event, post)"
          />
          <span v-if="post.locationName" class="cover-tag">
            <el-icon :size="12"><Location /></el-icon>
            {{ post.locationName }}
          </span>
        </div>

        <!-- 无图：纯文字紧凑卡片（瀑布流中的矮卡片） -->
        <div v-else class="card-cover cover-text-only">
          <span v-if="post.locationName" class="cover-tag">
            <el-icon :size="12"><Location /></el-icon>
            {{ post.locationName }}
          </span>
          <div class="text-avatar-frame" :class="'frame-' + (post.avatarFrame || 'default')">
            <el-avatar
              :size="40"
              :src="formatPhotoUrl(post.avatarUrl)"
              class="text-avatar"
            >
              <el-icon :size="20"><User /></el-icon>
            </el-avatar>
          </div>
          <span class="text-username">{{ post.realName || post.username }}</span>
          <p class="text-excerpt">{{ firstSentence(post.description || post.title) }}</p>
        </div>

        <!-- 信息区（有图和无图共用，无图更紧凑） -->
        <div class="card-body" :class="{ 'card-body-compact': !post.photoUrl }">
          <h3 class="card-title">{{ post.title }}</h3>
          <div class="card-footer">
            <div class="footer-author">
              <div class="footer-avatar-frame" :class="'frame-' + (post.avatarFrame || 'default')">
                <el-avatar
                  :size="22"
                  :src="formatPhotoUrl(post.avatarUrl)"
                  class="author-avatar"
                >
                  <el-icon :size="12"><User /></el-icon>
                </el-avatar>
              </div>
              <span class="author-name">{{ post.realName || post.username }}</span>
            </div>
            <div class="footer-stats">
              <span
                class="stat-item"
                :class="{ liked: post.liked }"
                @click.stop="toggleListLike(post)"
              >
                <el-icon :size="13">
                  <component :is="post.liked ? CircleCheckFilled : CircleCheck" />
                </el-icon>
                {{ formatCount(post.likeCount || 0) }}
              </span>
              <span class="stat-item">
                <el-icon :size="13"><ChatDotSquare /></el-icon>
                {{ formatCount(post.commentCount || 0) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
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

    <!-- ══════════════════════════════════════════ -->
    <!-- 模块三：底部悬浮发布按钮                       -->
    <!-- ══════════════════════════════════════════ -->
    <div class="fab-publish" @click="goPublish">
      <div class="fab-inner">
        <el-icon :size="26"><Plus /></el-icon>
      </div>
    </div>

    <el-backtop :right="30" :bottom="140" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Plus, User, CircleCheck, CircleCheckFilled, ChatDotSquare,
  Search, CircleClose, Location
} from '@element-plus/icons-vue'
import { getCommunityObservations } from '@/api/observation'
import { toggleLike as toggleLikeApi } from '@/api/like'

const router = useRouter()
const route = useRoute()

// ===== 状态 =====
const posts = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)
const totalCount = ref(0)
const searchKeyword = ref('')
const activeCategory = ref('all')
const useMock = ref(false)

let searchTimer = null

// ===== 分类标签 =====
const categoryTabs = [
  { key: 'all', label: '全部' },
  { key: 'latest', label: '最新' },
  { key: 'hot', label: '热门' },
]

// ===== 工具函数 =====
const formatCount = (num) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return String(num)
}

const formatPhotoUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('/api')) return url
  return `/api${url}`
}

const firstSentence = (text) => {
  if (!text) return ''
  const match = text.match(/^(.+?[。！？.!?；;\n])/)
  if (match) return match[1]
  return text.length > 50 ? text.slice(0, 50) + '...' : text
}

// ===== 计算属性 =====
const displayPosts = computed(() => {
  let list = [...posts.value]

  if (activeCategory.value === 'latest') {
    list.sort((a, b) => new Date(b.lastActivityTime || b.createdAt) - new Date(a.lastActivityTime || a.createdAt))
  } else if (activeCategory.value === 'hot') {
    list.sort((a, b) => (b.likeCount + b.commentCount * 2) - (a.likeCount + a.commentCount * 2))
  }

  return list
})

// ===== 方法 =====
const onSearchInput = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    pageNum.value = 1
    loadPosts()
  }, 350)
}

const clearSearch = () => {
  searchKeyword.value = ''
  pageNum.value = 1
  loadPosts()
}

const switchCategory = (key) => {
  if (activeCategory.value === key) return
  activeCategory.value = key
  pageNum.value = 1
  posts.value = []   // 立即清空，避免闪现旧数据
  loadPosts()
}

const onImgError = (e, post) => {
  post.photoUrl = ''
}

const loadPosts = async () => {
  loading.value = true
  try {
    if (useMock.value) {
      await new Promise(r => setTimeout(r, 350))
      let filtered = [...mockPosts]
      const kw = searchKeyword.value.trim()
      if (kw) {
        filtered = filtered.filter(p =>
          p.title.includes(kw) || (p.description && p.description.includes(kw))
        )
      }
      totalCount.value = filtered.length
      const start = (pageNum.value - 1) * pageSize.value
      posts.value = filtered.slice(start, start + pageSize.value)
    } else {
      const sort = activeCategory.value === 'hot' ? 'hot' : 'latest'
      const res = await getCommunityObservations({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        sort,
        keyword: searchKeyword.value.trim() || undefined,
      })
      if (res.data.success) {
        const d = res.data.data
        posts.value = d.records || []
        totalCount.value = d.total || 0
      } else {
        posts.value = []
        totalCount.value = 0
      }
    }
  } catch (err) {
    console.error('加载社区帖子失败', err)
    ElMessage.error('加载失败')
    posts.value = []
  } finally {
    loading.value = false
  }
}

const goPublish = () => router.push('/observation/publish')

const toggleListLike = async (post) => {
  if (useMock.value) {
    post.liked = !post.liked
    post.likeCount += post.liked ? 1 : -1
    return
  }
  try {
    const res = await toggleLikeApi('user_observation', post.id)
    if (res.data.success) {
      post.liked = res.data.data.liked
      post.likeCount = res.data.data.count
    }
  } catch (err) {
    console.error(err)
  }
}

const openDetail = (post) => {
  if (useMock.value) {
    ElMessage.info('Mock 模式：模拟跳转帖子详情 id=' + post.id)
    return
  }
  sessionStorage.setItem('community_scroll_top', window.scrollY)
  sessionStorage.setItem('community_page_num', String(pageNum.value))
  sessionStorage.setItem('community_keyword', searchKeyword.value)
  router.push({ name: 'EduObservationDetail', params: { id: String(post.id) } })
}

const restoreScroll = async () => {
  const savedScroll = sessionStorage.getItem('community_scroll_top')
  if (savedScroll) {
    sessionStorage.removeItem('community_scroll_top')
    await nextTick()
    window.scrollTo(0, parseInt(savedScroll))
  }
}

onMounted(async () => {
  const savedPage = sessionStorage.getItem('community_page_num')
  const savedKeyword = sessionStorage.getItem('community_keyword')
  if (savedPage) {
    pageNum.value = parseInt(savedPage)
    sessionStorage.removeItem('community_page_num')
  }
  if (savedKeyword) {
    searchKeyword.value = savedKeyword
    sessionStorage.removeItem('community_keyword')
  }

  await loadPosts()
  await restoreScroll()

  const detailId = route.query.detail
  if (detailId) {
    router.replace({ name: 'EduObservationDetail', params: { id: String(detailId) } })
  }
})
</script>

<style scoped>
/* ══════════════════════════════════════════ */
/* 容器                                      */
/* ══════════════════════════════════════════ */
.ocean-community {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 16px 120px; /* 底部大内边距 → 无限延伸感 */
  min-height: 100vh;
}

/* ══════════════════════════════════════════ */
/* 模块一：顶部工具栏                          */
/* ══════════════════════════════════════════ */
.community-toolbar {
  margin-bottom: 24px;
}

.toolbar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
}

.page-title {
  font-size: 26px;
  font-weight: 800;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #36cfc9, #165dff, #0e42d2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
  letter-spacing: 1px;
  flex-shrink: 0;
}

.title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(22, 93, 255, 0.1);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(22, 93, 255, 0.25);
  font-size: 22px;
  -webkit-text-fill-color: initial;
}

/* 胶囊搜索框 */
.search-capsule {
  position: relative;
  flex: 1;
  max-width: 520px;
  display: flex;
  align-items: center;
  background: #ffffff;
  border-radius: 28px;
  padding: 0 20px;
  height: 48px;
  border: 1.5px solid var(--theme-border, #d9e6ff);
  box-shadow: 0 2px 12px rgba(22, 93, 255, 0.06);
  transition: all 0.3s ease;
}

.search-capsule:focus-within {
  border-color: var(--theme-primary, #165dff);
  box-shadow: 0 4px 20px rgba(22, 93, 255, 0.15);
}

.search-icon {
  color: var(--theme-text-muted, #8c8c8c);
  font-size: 18px;
  flex-shrink: 0;
  margin-right: 10px;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: var(--theme-text-primary, #262626);
  background: transparent;
  height: 100%;
}

.search-input::placeholder {
  color: #b0b8c8;
}

.clear-icon {
  color: #b0b8c8;
  font-size: 16px;
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.2s;
}

.clear-icon:hover {
  color: var(--theme-text-secondary, #595959);
}

/* 分类标签栏 */
.category-tabs-wrapper {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.category-tabs-wrapper::-webkit-scrollbar {
  display: none;
}

.category-tabs {
  display: flex;
  gap: 4px;
  min-width: max-content;
  padding: 4px 0;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  padding: 8px 20px;
  border: none;
  background: transparent;
  color: var(--theme-text-muted, #8c8c8c);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.25s ease;
  white-space: nowrap;
  position: relative;
}

.tab-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%) scaleX(0);
  width: 24px;
  height: 2.5px;
  border-radius: 2px;
  background: var(--theme-primary, #165dff);
  transition: transform 0.25s ease;
}

.tab-item:hover {
  color: var(--theme-text-primary, #262626);
  background: rgba(22, 93, 255, 0.04);
}

.tab-item.active {
  color: var(--theme-primary, #165dff);
  font-weight: 700;
}

.tab-item.active::after {
  transform: translateX(-50%) scaleX(1);
}

/* ══════════════════════════════════════════ */
/* CSS Columns 瀑布流                          */
/* ══════════════════════════════════════════ */
.masonry-grid {
  column-count: 3;
  column-gap: 16px;
}

/* 骨架屏 → 也用 columns，模拟不同高度 */
.skeleton-card {
  break-inside: avoid;
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.skeleton-cover {
  width: 100%;
  background: linear-gradient(90deg, #eef3fb 25%, #d9e6ff 50%, #eef3fb 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skel-h-1 { aspect-ratio: 3 / 4; }
.skel-h-2 { aspect-ratio: 1 / 1; }
.skel-h-3 { padding-bottom: 56%; }

.skeleton-body {
  padding: 14px 16px 16px;
}

.skeleton-line {
  height: 14px;
  border-radius: 7px;
  background: linear-gradient(90deg, #eef3fb 25%, #d9e6ff 50%, #eef3fb 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  margin-bottom: 10px;
}

.skeleton-line-1 { width: 90%; }
.skeleton-line-2 { width: 65%; }
.skeleton-line-3 { width: 50%; }

.skeleton-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
}

.skeleton-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(90deg, #eef3fb 25%, #d9e6ff 50%, #eef3fb 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ══════════════════════════════════════════ */
/* 卡片                                       */
/* ══════════════════════════════════════════ */
.post-card {
  break-inside: avoid;           /* 不跨列断裂 */
  margin-bottom: 16px;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: auto;                  /* 完全由内容决定 */
}

.post-card:hover {
  box-shadow: 0 8px 28px rgba(22, 93, 255, 0.15);
  transform: translateY(-3px);
}

/* 封面图 — 宽高比由内联 style 动态决定 */
.card-cover {
  position: relative;
  width: 100%;
  background: linear-gradient(135deg, #e8f3ff, #d9e6ff);
  overflow: hidden;
}

.cover-img {
  width: 100%;
  display: block;
  object-fit: cover;
  background: linear-gradient(135deg, #e8f3ff, #f0f7ff);
}

/* 纯文字卡片封面 — 紧凑，充当瀑布流中的矮卡片 */
.cover-text-only {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 16px 12px;
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(160deg, #f4f9ff 0%, #e8f3ff 35%, #dcecff 100%);
}

.cover-text-only::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 25% 20%, rgba(22, 93, 255, 0.04) 0%, transparent 55%),
    radial-gradient(circle at 75% 60%, rgba(54, 207, 201, 0.04) 0%, transparent 55%);
}

.cover-text-only .text-avatar-frame {
  position: relative;
  z-index: 1;
  display: inline-flex;
  border-radius: 50%;
  padding: 3px;
  flex-shrink: 0;
  transition: transform 0.3s ease;
  margin-bottom: 6px;
}

.post-card:hover .cover-text-only .text-avatar-frame {
  transform: scale(1.06);
}

.text-avatar {
  border: 2px solid #ffffff;
  border-radius: 50%;
  box-sizing: content-box;
}

.text-username {
  position: relative;
  z-index: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--theme-text-primary, #262626);
  margin-bottom: 6px;
}

.text-excerpt {
  position: relative;
  z-index: 1;
  font-size: 12px;
  color: var(--theme-text-secondary, #595959);
  line-height: 1.5;
  text-align: center;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
  margin: 0;
}

/* 位置标签 */
.cover-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 3px 8px;
  border-radius: 4px;
  background: rgba(22, 93, 255, 0.82);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  z-index: 2;
  max-width: calc(100% - 16px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 信息区 */
.card-body {
  padding: 12px 14px 14px;
}

.card-body-compact {
  padding: 8px 12px 10px !important;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--theme-text-primary, #262626);
  line-height: 1.45;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

/* 底部作者 & 互动 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.footer-author {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}

.footer-avatar-frame {
  display: inline-flex;
  border-radius: 50%;
  padding: 2px;
  flex-shrink: 0;
}

.author-avatar {
  border: 1.5px solid #ffffff;
  border-radius: 50%;
  box-sizing: content-box;
  transition: border-color 0.2s;
}

.post-card:hover .author-avatar {
  border-color: rgba(22, 93, 255, 0.4);
}

.author-name {
  font-size: 12px;
  color: var(--theme-text-secondary, #595959);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.footer-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--theme-text-muted, #8c8c8c);
  transition: color 0.2s;
  cursor: default;
}

.stat-item:first-child {
  cursor: pointer;
}

.stat-item:first-child:hover {
  color: var(--theme-primary, #165dff);
}

.stat-item.liked {
  color: var(--theme-primary, #165dff);
}

.stat-item.liked .el-icon {
  animation: heart-pop 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}

@keyframes heart-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.35); }
  70% { transform: scale(0.9); }
  100% { transform: scale(1); }
}

/* ══════════════════════════════════════════ */
/* 头像框样式（与系统全局一致）                 */
/* ══════════════════════════════════════════ */
.frame-default { background: #dcdfe6; }
.frame-gold {
  background: linear-gradient(135deg, #f6d365, #fda085);
  box-shadow: 0 0 14px rgba(246, 211, 101, 0.6);
}
.frame-ocean {
  background: linear-gradient(135deg, #00d2ff, #165dff);
  box-shadow: 0 0 16px rgba(0, 210, 255, 0.6);
}
.frame-rainbow {
  background: linear-gradient(90deg, #ff6b6b, #feca57, #48dbfb, #ff9ff3);
  background-size: 200% 100%;
  animation: frame-rainbow-spin 3s linear infinite;
}
@keyframes frame-rainbow-spin {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}
.frame-flame {
  background: linear-gradient(135deg, #ff4500, #ff8c00, #ffd700);
  box-shadow: 0 0 18px rgba(255, 69, 0, 0.6);
}
.frame-dashed {
  background: repeating-conic-gradient(#fd7000 0deg 18deg, transparent 18deg 36deg);
  box-shadow: 0 0 14px rgba(237, 35, 76, 0.5);
  animation: frame-dash-glow 2s ease-in-out infinite;
}
@keyframes frame-dash-glow {
  0%, 100% { box-shadow: 0 0 14px rgba(237, 35, 76, 0.5); }
  50% { box-shadow: 0 0 26px rgba(237, 35, 76, 0.85); }
}
.frame-neon {
  background: linear-gradient(135deg, #00fff5, #ff00e4);
  box-shadow: 0 0 14px rgba(0, 255, 245, 0.7), 0 0 28px rgba(255, 0, 228, 0.4);
  animation: frame-neon-pulse 3s ease-in-out infinite;
}
@keyframes frame-neon-pulse {
  0%, 100% { box-shadow: 0 0 14px rgba(0, 255, 245, 0.7), 0 0 28px rgba(255, 0, 228, 0.4); }
  50% { box-shadow: 0 0 24px rgba(0, 255, 245, 0.9), 0 0 44px rgba(255, 0, 228, 0.6); }
}
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
.frame-crystal {
  background: linear-gradient(135deg, #00f5ff, #ff00e5);
  box-shadow: 0 0 16px rgba(0, 245, 255, 0.5), 0 0 32px rgba(255, 0, 229, 0.3), inset 0 0 8px rgba(255, 255, 255, 0.4);
  animation: crystal-breath 2s ease-in-out infinite;
}
@keyframes crystal-breath {
  0%, 100% { box-shadow: 0 0 16px rgba(0, 245, 255, 0.5), 0 0 32px rgba(255, 0, 229, 0.3); }
  50% { box-shadow: 0 0 28px rgba(0, 245, 255, 0.8), 0 0 56px rgba(255, 0, 229, 0.5); }
}
.frame-royal {
  background: linear-gradient(135deg, #6c3cc7, #9b59b6, #f1c40f);
  box-shadow: 0 0 16px rgba(108, 60, 199, 0.6);
}

/* ══════════════════════════════════════════ */
/* 分页                                      */
/* ══════════════════════════════════════════ */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 36px;
  padding-bottom: 24px;
}

/* ══════════════════════════════════════════ */
/* 模块三：FAB 悬浮发布按钮                    */
/* ══════════════════════════════════════════ */
.fab-publish {
  position: fixed;
  right: 30px;
  bottom: 80px;
  z-index: 100;
  cursor: pointer;
}

.fab-inner {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #165dff, #4080ff);
  box-shadow: 0 4px 20px rgba(22, 93, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.fab-inner::after {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  background: rgba(22, 93, 255, 0.12);
  opacity: 0;
  transition: all 0.3s ease;
}

.fab-publish:hover .fab-inner {
  transform: scale(1.08);
  box-shadow: 0 6px 28px rgba(22, 93, 255, 0.5);
}

.fab-publish:hover .fab-inner::after {
  opacity: 1;
  inset: -8px;
}

.fab-publish:active .fab-inner {
  transform: scale(0.95);
}

/* ══════════════════════════════════════════ */
/* 响应式                                    */
/* ══════════════════════════════════════════ */
@media (max-width: 1024px) {
  .ocean-community {
    padding: 16px 12px 100px;
  }

  .masonry-grid {
    column-count: 2;
    column-gap: 12px;
  }

  .post-card,
  .skeleton-card {
    margin-bottom: 12px;
  }

  .toolbar-top {
    gap: 16px;
  }

  .page-title {
    font-size: 22px;
  }

  .search-capsule {
    max-width: 360px;
    height: 42px;
  }

  .cover-text-only {
    padding: 14px 12px 10px;
  }

  .fab-publish {
    right: 20px;
    bottom: 70px;
  }

  .fab-inner {
    width: 50px;
    height: 50px;
  }
}

@media (max-width: 640px) {
  .ocean-community {
    padding: 12px 8px 80px;
  }

  .masonry-grid {
    column-count: 2;
    column-gap: 8px;
  }

  .post-card,
  .skeleton-card {
    margin-bottom: 8px;
  }

  .toolbar-top {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .page-title {
    font-size: 20px;
  }

  .title-icon {
    width: 34px;
    height: 34px;
    font-size: 18px;
  }

  .search-capsule {
    max-width: none;
    height: 40px;
    padding: 0 14px;
  }

  .search-input {
    font-size: 14px;
  }

  .tab-item {
    padding: 6px 14px;
    font-size: 13px;
  }

  .cover-text-only {
    padding: 12px 10px 8px;
  }

  .cover-text-only .text-avatar-frame .el-avatar {
    width: 32px !important;
    height: 32px !important;
  }

  .text-username {
    font-size: 12px;
  }

  .text-excerpt {
    font-size: 11px;
    -webkit-line-clamp: 2;
  }

  .card-title {
    font-size: 13px;
  }

  .card-body {
    padding: 10px 12px 12px;
  }

  .card-body-compact {
    padding: 6px 10px 8px !important;
  }

  .cover-tag {
    top: 6px;
    left: 6px;
    padding: 2px 6px;
    font-size: 10px;
  }

  .fab-publish {
    right: 16px;
    bottom: 60px;
  }

  .fab-inner {
    width: 46px;
    height: 46px;
  }
}
</style>
