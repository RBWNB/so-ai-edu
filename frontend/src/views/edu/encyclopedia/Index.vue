<template>
  <div class="encyclopedia-page">
    <!-- 顶部导航 -->
    <div class="encyclopedia-header">
      <h2 class="page-title">
        <span class="title-icon"><el-icon><Compass /></el-icon></span>
        海洋百科
      </h2>
      <div class="category-tabs">
        <div
          class="tab-item"
          :class="{ active: activeCategory === 'species' }"
          @click="switchCategory('species')"
        >
          <el-icon><Trophy /></el-icon>物种图鉴
        </div>
        <div
          class="tab-item"
          :class="{ active: activeCategory === 'ecosystem' }"
          @click="switchCategory('ecosystem')"
        >
          <el-icon><Sunny /></el-icon>生态系统
        </div>
      </div>
      <!-- 搜索框 -->
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索名称 / 描述..."
          clearable
          prefix-icon="Search"
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

    <!-- 内容区 -->
    <div v-loading="loading" class="content-area" :class="'mode-' + activeCategory">
      <el-empty v-if="!loading && dataList.length === 0" description="暂无数据，去探索深蓝世界吧 🌊" />

      <!-- 物种列表 -->
      <div v-if="activeCategory === 'species' && dataList.length > 0" class="card-grid">
        <div
          v-for="item in dataList"
          :key="'sp-' + item.id"
          class="card-item species-card"
          @click="openDetail(item)"
        >
          <div class="card-image-wrap">
            <el-image
              v-if="item.imageUrl"
              :src="getImageUrl(item.imageUrl)"
              fit="cover"
              class="card-img"
            >
              <template #error>
                <div class="img-placeholder"><el-icon :size="40"><Picture /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="img-placeholder"><el-icon :size="40"><Picture /></el-icon></div>

            <!-- 保护状态标签 -->
            <el-tag
              v-if="item.conservationStatus"
              :type="conservationTagType(item.conservationStatus)"
              size="small"
              effect="dark"
              class="conservation-tag"
              round
            >{{ item.conservationStatus }}</el-tag>

            <!-- 收藏按钮 -->
            <div
              class="bookmark-btn"
              :class="{ bookmarked: isBookmarked('species', item.id) }"
              @click.stop="toggleBookmark('species', item)"
            >
              <el-icon :size="18"><component :is="isBookmarked('species', item.id) ? StarFilled : Star" /></el-icon>
            </div>

            <!-- 渐变遮罩 -->
            <div class="card-overlay"></div>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ item.chineseName || '未命名物种' }}</h3>
            <p class="card-scientific">{{ item.scientificName || '' }}</p>
            <div class="card-meta-row">
              <span v-if="item.phylum" class="meta-tag">{{ item.phylum }}</span>
              <span v-if="item.habitat" class="meta-tag">{{ item.habitat }}</span>
            </div>
            <p class="card-desc">{{ (item.morphologyDesc || item.habitDesc || '暂无描述').slice(0, 70) }}{{ (item.morphologyDesc || item.habitDesc || '').length > 70 ? '...' : '' }}</p>
          </div>
        </div>
      </div>

      <!-- 生态系统列表 -->
      <div v-if="activeCategory === 'ecosystem' && dataList.length > 0" class="card-grid">
        <div
          v-for="item in dataList"
          :key="'eco-' + item.id"
          class="card-item eco-card"
          @click="openDetail(item)"
        >
          <div class="card-image-wrap eco-cover" :style="item.imageUrl ? {} : { background: getEcoGradient(item.id) }">
            <el-image
              v-if="item.imageUrl"
              :src="getImageUrl(item.imageUrl)"
              fit="cover"
              class="card-img"
            >
              <template #error>
                <div class="img-placeholder eco-ph"><el-icon :size="48"><Sunny /></el-icon></div>
              </template>
            </el-image>
            <div v-else class="img-placeholder eco-ph"><el-icon :size="48" color="rgba(255,255,255,0.5)"><Sunny /></el-icon></div>

            <!-- 收藏按钮 -->
            <div
              class="bookmark-btn eco-bookmark-btn"
              :class="{ bookmarked: isBookmarked('ecosystem', item.id) }"
              @click.stop="toggleBookmark('ecosystem', item)"
            >
              <el-icon :size="18"><component :is="isBookmarked('ecosystem', item.id) ? StarFilled : Star" /></el-icon>
            </div>

            <div class="card-overlay eco-overlay"></div>
          </div>
          <div class="card-body eco-body">
            <div class="eco-badge">🌊 生态系统</div>
            <h3 class="card-title eco-title">{{ item.name || '未命名生态' }}</h3>
            <p class="card-desc eco-desc">{{ (item.description || '暂无描述').slice(0, 90) }}{{ (item.description || '').length > 90 ? '...' : '' }}</p>
            <div class="eco-tags" v-if="item.typicalSpecies">
              <span class="eco-tag">典型物种</span>
              <span class="eco-tag-val">{{ item.typicalSpecies.slice(0, 30) }}{{ item.typicalSpecies.length > 30 ? '...' : '' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pagination.size" class="pagination-area">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>

    <!-- ═══════ 详情弹窗 ═══════ -->
    <el-dialog
      v-model="detailVisible"
      :title="detailType === 'species' ? '物种详情' : '生态系统详情'"
      width="720px"
      destroy-on-close
      append-to-body
      class="encyclopedia-detail-dialog"
      :close-on-click-modal="true"
    >
      <div v-loading="detailLoading" class="detail-content">

        <!-- 物种详情 -->
        <template v-if="detailType === 'species' && detailData">
          <!-- 头图区 -->
          <div class="detail-hero">
            <div class="hero-image-wrap">
              <el-image
                v-if="detailData.imageUrl"
                :src="getImageUrl(detailData.imageUrl)"
                fit="cover"
                class="hero-image"
              >
                <template #error><div class="hero-placeholder"><el-icon :size="60"><Picture /></el-icon></div></template>
              </el-image>
              <div v-else class="hero-placeholder hero-full"><el-icon :size="60"><Picture /></el-icon></div>
              <div class="hero-gradient"></div>
            </div>
            <div class="hero-info">
              <h2 class="hero-name">{{ detailData.chineseName || '未命名物种' }}</h2>
              <p class="hero-latin">{{ detailData.scientificName || '' }}</p>
              <div class="hero-tags">
                <el-tag v-if="detailData.conservationStatus" :type="conservationTagType(detailData.conservationStatus)" effect="dark" round size="small">
                  {{ detailData.conservationStatus }}
                </el-tag>
                <el-tag v-if="detailData.phylum" type="info" effect="plain" round size="small">{{ detailData.phylum }}</el-tag>
                <el-tag v-if="detailData.habitat" type="" effect="plain" round size="small">{{ detailData.habitat }}</el-tag>
              </div>
            </div>
          </div>

          <!-- 信息面板 -->
          <div class="detail-info">
            <el-descriptions :column="2" border size="small" class="desc-table">
              <el-descriptions-item label="界">{{ detailData.kingdom || '-' }}</el-descriptions-item>
              <el-descriptions-item label="门">{{ detailData.phylum || '-' }}</el-descriptions-item>
              <el-descriptions-item label="纲">{{ detailData.className || '-' }}</el-descriptions-item>
              <el-descriptions-item label="目">{{ detailData.orderName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="科">{{ detailData.familyName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="属">{{ detailData.genusName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="分布区域" :span="2">{{ detailData.distributionArea || '-' }}</el-descriptions-item>
              <el-descriptions-item label="数据来源">{{ detailData.dataSource || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 特征描述 -->
          <div class="detail-sections" v-if="detailData.morphologyDesc || detailData.habitDesc">
            <div v-if="detailData.morphologyDesc" class="detail-section">
              <h4 class="section-title"><el-icon><EditPen /></el-icon>形态特征</h4>
              <p class="section-body">{{ detailData.morphologyDesc }}</p>
            </div>
            <div v-if="detailData.habitDesc" class="detail-section">
              <h4 class="section-title"><el-icon><Sunny /></el-icon>生活习性</h4>
              <p class="section-body">{{ detailData.habitDesc }}</p>
            </div>
          </div>
        </template>

        <!-- 生态系统详情 -->
        <template v-if="detailType === 'ecosystem' && detailData">
          <div class="detail-hero eco-detail-hero">
            <div class="hero-image-wrap eco" :style="!detailData.imageUrl ? { background: getEcoGradient(detailData.id) } : {}">
              <el-image
                v-if="detailData.imageUrl"
                :src="getImageUrl(detailData.imageUrl)"
                fit="cover"
                class="hero-image"
              >
                <template #error><div class="hero-placeholder eco-bg" :style="{ background: getEcoGradient(detailData.id) }"><el-icon :size="60" color="rgba(255,255,255,0.5)"><Sunny /></el-icon></div></template>
              </el-image>
              <div v-else class="hero-placeholder eco-bg hero-full">
                <el-icon :size="60" color="rgba(255,255,255,0.5)"><Sunny /></el-icon>
              </div>
              <div class="hero-gradient"></div>
            </div>
            <div class="hero-info">
              <h2 class="hero-name">{{ detailData.name || '未命名生态' }}</h2>
              <div class="hero-tags">
                <el-tag type="success" effect="dark" round size="small">生态系统</el-tag>
              </div>
            </div>
          </div>

          <div class="detail-sections">
            <div v-if="detailData.description" class="detail-section">
              <h4 class="section-title"><el-icon><Document /></el-icon>概述</h4>
              <p class="section-body">{{ detailData.description }}</p>
            </div>
            <div v-if="detailData.typicalSpecies" class="detail-section">
              <h4 class="section-title"><el-icon><Trophy /></el-icon>典型物种</h4>
              <p class="section-body">{{ detailData.typicalSpecies }}</p>
            </div>
            <div v-if="detailData.threats" class="detail-section">
              <h4 class="section-title"><el-icon><Warning /></el-icon>主要威胁</h4>
              <p class="section-body">{{ detailData.threats }}</p>
            </div>
            <div v-if="detailData.protectionAdvice" class="detail-section">
              <h4 class="section-title"><el-icon><CircleCheck /></el-icon>保护建议</h4>
              <p class="section-body">{{ detailData.protectionAdvice }}</p>
            </div>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detailType === 'species'"
          type="primary"
          :class="{ bookmarked: isBookmarked('species', detailData?.id) }"
          @click="toggleBookmark('species', detailData)"
        >
          {{ isBookmarked('species', detailData?.id) ? '已收藏' : '收藏' }}
        </el-button>
        <el-button
          v-else-if="detailType === 'ecosystem'"
          type="primary"
          :class="{ bookmarked: isBookmarked('ecosystem', detailData?.id) }"
          @click="toggleBookmark('ecosystem', detailData)"
        >
          {{ isBookmarked('ecosystem', detailData?.id) ? '已收藏' : '收藏' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, reactive, watch, onActivated } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  Picture, Star, StarFilled, Sunny, Search,
  Trophy, Compass, EditPen, Document, Warning, CircleCheck
} from '@element-plus/icons-vue';
import { getSpeciesPage, getSpeciesById } from '@/api/species';
import { getEcosystemPage, getEcosystemById } from '@/api/ecosystem';
import { addBookmark as addBookmarkApi, removeBookmark as removeBookmarkApi, getBookmarkList } from '@/api/bookmark';

const router = useRouter();
const route = useRoute();

// ═══ 基础状态 ═══
const loading = ref(false);
const dataList = ref([]);
const total = ref(0);
const activeCategory = ref('species');
const pagination = reactive({ current: 1, size: 12 });
const searchKeyword = ref('');

// ═══ 详情弹窗状态 ═══
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailData = ref(null);
const detailType = ref('');

// ═══ 收藏状态管理 - 仿照 PostDetail 响应式方案 ═══
const bookmarkState = reactive({
  species: {},   // 格式: { [id]: true }
  ecosystem: {},
});

/** 加载用户的收藏列表（仿照 PostDetail 每次都重新加载） */
const loadBookmarks = async () => {
  try {
    const res = await getBookmarkList();
    if (res.data.success && res.data.data) {
      const d = res.data.data;
      // 清空旧状态
      bookmarkState.species = {};
      bookmarkState.ecosystem = {};
      // 填充新状态（使用 reactive 对象的属性赋值，Vue 可追踪）
      (d.species || []).forEach(item => { bookmarkState.species[item.targetId] = true; });
      (d.ecosystem || []).forEach(item => { bookmarkState.ecosystem[item.targetId] = true; });
    }
  } catch (err) {
    console.error('加载收藏失败', err);
  }
};

/** 检查是否已收藏 */
const isBookmarked = (type, id) => {
  if (!id) return false;
  return !!bookmarkState[type]?.[id];
};

/** 收藏 / 取消收藏 */
const toggleBookmark = async (type, item) => {
  if (!item?.id) return;
  const name = type === 'species' ? item.chineseName : item.name;
  const currentlyBookmarked = !!bookmarkState[type]?.[item.id];

  try {
    if (currentlyBookmarked) {
      const res = await removeBookmarkApi(type, item.id);
      if (res.data?.success) {
        delete bookmarkState[type][item.id];        // ✅ 响应式删除，Vue 立即感知
        ElMessage.info(`已取消「${name}」收藏`);
      } else {
        ElMessage.error(res.data?.message || '取消收藏失败');
        return;
      }
    } else {
      const res = await addBookmarkApi(type, item.id);
      if (res.data?.success) {
        bookmarkState[type][item.id] = true;         // ✅ 响应式添加，Vue 立即感知
        ElMessage.success(res.data?.message || `已收藏「${name}」`);
      } else {
        ElMessage.error(res.data?.message || '收藏失败');
        return;
      }
    }
    // 🌟 通知其他页面（如个人中心）即时更新收藏列表
    window.dispatchEvent(new CustomEvent('bookmark-changed', { detail: { targetType: type, targetId: item.id } }));
  } catch (err) {
    ElMessage.error(currentlyBookmarked ? '取消收藏失败' : '收藏失败');
  }
};

// ═══ 切换分类 ═══
const switchCategory = (key) => {
  if (activeCategory.value === key) return;
  activeCategory.value = key;
  pagination.current = 1;
  searchKeyword.value = '';
  dataList.value = [];
  total.value = 0;
  fetchData();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

// ═══ 数据获取 ═══
const getImageUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  if (url.startsWith('/uploads/')) return `/api${url}`;
  if (url.startsWith('/')) return `/api/uploads${url}`;
  return `/api/uploads/${url}`;
};

const conservationTagType = (status) => {
  const map = { CR: 'danger', EN: 'warning', VU: 'warning', NT: 'info', LC: 'success' };
  return map[status] || 'info';
};

const ecoGradients = [
  'linear-gradient(135deg, #0077b6, #00b4d8)',
  'linear-gradient(135deg, #005f73, #0a9396)',
  'linear-gradient(135deg, #006d77, #83c5be)',
  'linear-gradient(135deg, #1b4332, #52b788)',
  'linear-gradient(135deg, #2d6a4f, #95d5b2)',
  'linear-gradient(135deg, #081c15, #40916c)',
  'linear-gradient(135deg, #023e8a, #48cae4)',
  'linear-gradient(135deg, #003049, #fca311)',
];
const getEcoGradient = (id) => ecoGradients[id % ecoGradients.length];

const fetchData = async () => {
  loading.value = true;
  try {
    if (activeCategory.value === 'species') {
      const params = {
        pageNum: pagination.current,
        pageSize: pagination.size,
        keyword: searchKeyword.value.trim() || undefined,
      };
      const res = await getSpeciesPage(params);
      const data = res.data;
      const records = data.records || data.list || data.rows || [];
      dataList.value = records.filter(item => item.chineseName || item.scientificName);
      total.value = Number(data.total || records.length);
    } else {
      const params = {
        pageNum: pagination.current,
        pageSize: pagination.size,
        keyword: searchKeyword.value.trim() || undefined,
      };
      const res = await getEcosystemPage(params);
      const data = res.data;
      const records = data.records || data.list || data.rows || [];
      dataList.value = records;
      total.value = Number(data.total || 0);
    }
  } catch (error) {
    console.error('获取数据失败:', error);
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

// ═══ 打开详情弹窗 ═══
const openDetail = async (item) => {
  const type = activeCategory.value;
  detailType.value = type;
  detailLoading.value = true;
  detailData.value = null;
  detailVisible.value = true;

  try {
    let res;
    if (type === 'species') {
      res = await getSpeciesById(item.id);
    } else {
      res = await getEcosystemById(item.id);
    }
    detailData.value = res.data;
  } catch (error) {
    console.error('获取详情失败:', error);
    // fallback: 用列表中的数据
    detailData.value = { ...item };
  } finally {
    detailLoading.value = false;
  }
};

// ═══ 初始化 ═══
onMounted(() => {
  fetchData();
  loadBookmarks();  // 首次进入时加载
});

/** keep-alive 缓存激活时重新加载数据（仿照 PostDetail） */
onActivated(() => {
  loadBookmarks();  // 每次从其他页面切换回来都刷新收藏状态
});

watch(() => [pagination.current, activeCategory.value], () => {
  loadBookmarks();  // 翻页或切换分类时也重新加载
});

/** 监听路由参数变化（防止从详情页返回时数据不刷新） */
watch(() => route.path, (newPath) => {
  if (newPath.includes('encyclopedia')) {
    loadBookmarks();
  }
});
</script>

<style scoped>
/* ═══ 容器 ═══ */
.encyclopedia-page {
  min-height: calc(100vh - 120px);
  padding: 20px 28px;
}

/* ═══ 顶部导航 ═══ */
.encyclopedia-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 28px;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  background: linear-gradient(135deg, #48cae4, #0077b6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

.title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(0, 180, 216, 0.12);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(72, 202, 228, 0.25);
  color: #48cae4;
  -webkit-text-fill-color: initial;
}

.category-tabs {
  display: flex;
  gap: 6px;
  background: #ffffff;
  padding: 4px;
  border-radius: 14px;
  border: 1px solid #d9e6ff;
  backdrop-filter: blur(10px);
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 22px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: #0077b6;
  transition: all 0.3s ease;
  user-select: none;
}

.tab-item:hover {
  color: #023e8a;
  background: rgba(22, 93, 255, 0.06);
}

.tab-item.active {
  color: #fff;
  background: linear-gradient(135deg, #0077b6, #00b4d8);
  box-shadow: 0 4px 16px rgba(0, 119, 182, 0.35);
}

.search-box {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-box .search-input {
  width: 240px;
}

.search-btn {
  border-radius: 20px !important;
  padding: 8px 20px !important;
  background: linear-gradient(135deg, #0077b6, #00b4d8) !important;
  border: none !important;
  font-weight: 600;
}

.search-btn:hover {
  background: linear-gradient(135deg, #005f8a, #0096c7) !important;
}

:deep(.search-input .el-input__wrapper) {
  background: #ffffff !important;
  border: 1px solid #d9e6ff !important;
  border-radius: 20px !important;
  box-shadow: 0 1px 4px rgba(0, 119, 182, 0.06) !important;
}

:deep(.search-input .el-input__inner) {
  color: #1e293b;
}

:deep(.search-input .el-input__inner::placeholder) {
  color: #94a3b8;
}

:deep(.search-input .el-input__prefix .el-icon) {
  color: #94a3b8;
}

/* ═══ 内容区 ═══ */
.content-area {
  min-height: 300px;
}

/* ═══ 卡片网格 ═══ */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 22px;
}

/* ============================================
   物种卡片 —— 白底蓝字清新风
   ============================================ */
.card-item.species-card {
  border-radius: 16px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e8f4fc;
  box-shadow: 0 2px 12px rgba(0, 120, 180, 0.08);
  transition: all 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  cursor: pointer;
  position: relative;
}

.card-item.species-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0, 140, 200, 0.18);
  border-color: #48cae4;
}

/* ============================================
   生态系统卡片 —— 白底蓝字清新风
   ============================================ */
.card-item.eco-card {
  border-radius: 16px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e8f4fc;
  box-shadow: 0 2px 12px rgba(0, 120, 180, 0.08);
  transition: all 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  cursor: pointer;
  position: relative;
}

.card-item.eco-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0, 140, 200, 0.18);
  border-color: #48cae4;
}

/* ═══ 图片区域 ═══ */
.card-image-wrap {
  width: 100%;
  height: 190px;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.eco-cover {
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-img {
  width: 100%;
  height: 100%;
  transition: transform 0.5s ease;
}

.card-item:hover .card-img {
  transform: scale(1.06);
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(0, 119, 182, 0.15);
  background: linear-gradient(145deg, #e0f2fe, #f0f7ff);
}

.eco-ph {
  background: transparent;
}

/* 渐变遮罩 */
.card-overlay {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 50%;
  background: linear-gradient(to top, rgba(255, 255, 255, 0.3), transparent);
  pointer-events: none;
  z-index: 0;
}

/* 生态系统卡片用更轻的遮罩 */
.eco-overlay {
  background: linear-gradient(to top, rgba(0, 80, 120, 0.25), transparent);
}

/* ═══ 标签 ═══ */
.conservation-tag {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 2;
}

/* ═══ 收藏按钮 ═══ */
.bookmark-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  color: #0077b6;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  z-index: 2;
  border: 1px solid rgba(0, 119, 182, 0.15);
}

.bookmark-btn:hover {
  transform: scale(1.18);
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 119, 182, 0.2);
}

.bookmark-btn.bookmarked {
  background: linear-gradient(135deg, #ffb400, #ff8f00);
  color: #fff;
  border-color: rgba(255, 180, 0, 0.5);
  animation: pop 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

/* 生态系统卡片收藏按钮 —— 蓝色调 */
.eco-bookmark-btn {
  background: rgba(255, 255, 255, 0.85);
  color: #0077b6;
  border-color: rgba(0, 119, 182, 0.15);
}

.eco-bookmark-btn:hover {
  background: #fff;
  transform: scale(1.18);
  box-shadow: 0 2px 8px rgba(0, 119, 182, 0.2);
}

.eco-bookmark-btn.bookmarked {
  background: linear-gradient(135deg, #ffb400, #ff8f00);
  color: #fff;
  border-color: rgba(255, 180, 0, 0.5);
}

@keyframes pop {
  0%   { transform: scale(1); }
  40%  { transform: scale(1.3); }
  70%  { transform: scale(0.95); }
  100% { transform: scale(1); }
}

/* ═══ 信息区域 —— 白底蓝字 ═══ */
.card-body {
  padding: 16px 18px 18px;
  position: relative;
  z-index: 0;
  background: #ffffff;
}

.card-title {
  font-size: 17px;
  font-weight: 700;
  color: #023e8a;
  margin: 0 0 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-scientific {
  font-size: 13px;
  color: #0077b6;
  font-style: italic;
  margin: 0 0 8px;
}

.card-meta-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.meta-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #e0f2fe;
  color: #0077b6;
  border: 1px solid #bae6fd;
}

.card-desc {
  font-size: 13px;
  color: #475569;
  line-height: 1.65;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ═══ 生态系统卡片信息区域 —— 白底蓝字 ═══ */
.eco-body {
  background: #ffffff;
  padding: 18px 20px 20px;
}

.eco-badge {
  font-size: 11px;
  font-weight: 600;
  color: #0077b6;
  background: linear-gradient(135deg, #e0f2fe, #e0f7fa);
  padding: 3px 10px;
  border-radius: 20px;
  display: inline-block;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.eco-title {
  font-size: 18px;
  font-weight: 800;
  color: #023e8a !important;
  margin: 0 0 6px !important;
}

.eco-desc {
  font-size: 13.5px !important;
  color: #334155 !important;
  line-height: 1.7 !important;
  opacity: 0.82;
  -webkit-line-clamp: 2;
}

.eco-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.eco-tag {
  font-size: 11px;
  font-weight: 600;
  color: #0077b6;
  background: #f0f9ff;
  padding: 2px 8px;
  border-radius: 8px;
  border: 1px solid #bae6fd;
  white-space: nowrap;
}

.eco-tag-val {
  font-size: 12px;
  color: #475569;
}

/* ═══ 分页 ═══ */
.pagination-area {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-bottom: 16px;
}

/* ═══ 弹窗内部布局（scoped） ═══ */

.detail-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 24px;
  background: rgb(6, 18, 32) !important;
}

.detail-content::-webkit-scrollbar {
  width: 5px;
}
.detail-content::-webkit-scrollbar-thumb {
  background: rgba(0, 180, 216, 0.35);
  border-radius: 4px;
}

.detail-hero {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.hero-image-wrap {
  width: 220px;
  height: 150px;
  border-radius: 14px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
}

.hero-image-wrap.eco {
  height: 140px;
}

.hero-image {
  width: 100%;
  height: 100%;
}

.hero-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.15);
  background: linear-gradient(145deg, rgba(0, 50, 80, 0.4), rgba(0, 90, 130, 0.25));
}

.hero-placeholder.eco-bg {
  background: transparent;
}

.hero-placeholder.hero-full {
  border-radius: 14px;
}

.hero-gradient {
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: 40%;
  background: linear-gradient(to top, rgba(0, 20, 40, 0.5), transparent);
  pointer-events: none;
}

.hero-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
}

.hero-name {
  font-size: 22px;
  font-weight: 800;
  margin: 0;
  color: #f1f5f9;
}

.hero-latin {
  font-size: 14px;
  font-style: italic;
  color: #48cae4;
  margin: 0;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-info {
  margin-bottom: 18px;
}

.detail-sections {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-section {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 16px 18px;
  transition: all 0.25s ease;
}

.detail-section:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(0, 180, 216, 0.3);
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #48cae4;
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-body {
  font-size: 13.5px;
  line-height: 1.75;
  color: #e2e8f0;
  margin: 0;
  white-space: pre-wrap;
}

/* ═══ 响应式 ═══ */
@media (max-width: 768px) {
  .encyclopedia-page {
    padding: 14px 16px;
  }
  .encyclopedia-header {
    flex-wrap: wrap;
    gap: 14px;
  }
  .page-title {
    font-size: 18px;
  }
  .category-tabs {
    order: 3;
    width: 100%;
    justify-content: center;
  }
  .search-box {
    margin-left: 0;
    width: 100%;
  }
  .search-box .search-input {
    flex: 1;
  }
  .search-btn {
    white-space: nowrap;
  }
  .card-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 14px;
  }
  .card-image-wrap { height: 165px; }

  /* 详情弹窗响应式 */
  .detail-hero {
    flex-direction: column;
    gap: 14px;
  }
  .hero-image-wrap {
    width: 100%;
    height: 200px;
  }
}
</style>

<!-- ═════════ 全局弹窗样式（dialog 被 Teleport 到 body 下） ═════════ -->
<style>
/* ═════ 弹窗容器 — 覆盖 main.css 白色强规则 ═════ */
.el-dialog.encyclopedia-detail-dialog {
  border-radius: 20px !important;
  overflow: hidden;
  background: rgb(8, 22, 38) !important;
  --el-dialog-bg-color: rgb(8, 22, 38) !important;
  --el-bg-color: rgb(8, 22, 38) !important;
  --el-bg-color-overlay: rgb(8, 22, 38) !important;
  backdrop-filter: blur(28px);
  border: 1px solid rgba(0, 180, 216, 0.3) !important;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6) !important;
}

.el-dialog.encyclopedia-detail-dialog .el-dialog__header {
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding: 18px 24px 14px;
  margin: 0;
  background: rgb(8, 22, 38) !important;
}

.el-dialog.encyclopedia-detail-dialog .el-dialog__title {
  font-weight: 700;
  font-size: 18px;
  color: #ffffff !important;
}

.el-dialog.encyclopedia-detail-dialog .el-dialog__headerbtn .el-dialog__close {
  color: rgba(255, 255, 255, 0.5);
  font-size: 16px;
}

.el-dialog.encyclopedia-detail-dialog .el-dialog__headerbtn .el-dialog__close:hover {
  color: #ffffff;
}

.el-dialog.encyclopedia-detail-dialog .el-dialog__body {
  padding: 0 !important;
  background: rgb(8, 22, 38) !important;
}

.el-dialog.encyclopedia-detail-dialog .el-dialog__footer {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding: 14px 24px 18px;
  background: rgb(8, 22, 38) !important;
}

/* 收藏按钮高亮 */
.el-dialog.encyclopedia-detail-dialog .el-dialog__footer .el-button.bookmarked.el-button--primary {
  background: linear-gradient(135deg, #ffb400, #ff8f00) !important;
  border-color: #ffb400 !important;
}

/* ═════ el-descriptions 表格 — 全面深色覆盖 ═════ */
.el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions {
  --el-descriptions-table-border: 1px solid rgba(255, 255, 255, 0.1) !important;
  --el-fill-color-blank: transparent !important;
  --el-border-color: rgba(255, 255, 255, 0.1) !important;
  --el-text-color-primary: rgba(255, 255, 255, 0.9) !important;
  --el-text-color-regular: rgba(255, 255, 255, 0.85) !important;
  --el-bg-color: transparent !important;
  --el-bg-color-overlay: transparent !important;
  background: transparent !important;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
}

/* descriptions 内部 table 根元素 */
.el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions table {
  background: transparent !important;
  border-collapse: collapse;
}

/* 所有单元格基础 */
.el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions table th,
.el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions table td {
  background: transparent !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: rgba(255, 255, 255, 0.88) !important;
  transition: background 0.2s;
}

/* label 列（th）— 略深的底色区分 */
.el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions__label {
  background: rgba(0, 110, 170, 0.18) !important;
  color: #48cae4 !important;
  font-weight: 650 !important;
  font-size: 13px !important;
  width: 90px;
  padding: 10px 14px !important;
}

/* content 列（td） */
.el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions__content {
  background: rgba(255, 255, 255, 0.02) !important;
  color: rgba(255, 255, 255, 0.92) !important;
  font-size: 13.5px !important;
  padding: 10px 16px !important;
}

/* hover 高亮 */
.el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions table tr:hover th,
.el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions table tr:hover td {
  background: rgba(0, 180, 216, 0.06) !important;
}

/* ═════ 分段内容区块 — 深色适配 ═════ */
.el-dialog.encyclopedia-detail-dialog .detail-section {
  background: rgba(255, 255, 255, 0.06) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 12px;
  padding: 16px 18px;
  transition: all 0.25s ease;
}

.el-dialog.encyclopedia-detail-dialog .detail-section:hover {
  background: rgba(255, 255, 255, 0.12) !important;
  border-color: rgba(0, 180, 216, 0.3) !important;
}

.el-dialog.encyclopedia-detail-dialog .section-title {
  font-size: 14px;
  font-weight: 700;
  color: #48cae4 !important;
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.el-dialog.encyclopedia-detail-dialog .section-body {
  font-size: 13.5px;
  line-height: 1.75;
  color: #e2e8f0 !important;
  margin: 0;
  white-space: pre-wrap;
}

/* ═════ 响应式 ═════ */
@media (max-width: 768px) {
  .el-dialog.encyclopedia-detail-dialog {
    width: 94% !important;
    margin-top: 3vh !important;
  }

  .el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions__label {
    width: 70px !important;
    padding: 8px 10px !important;
    font-size: 12px !important;
  }

  .el-dialog.encyclopedia-detail-dialog .detail-info .el-descriptions__content {
    padding: 8px 10px !important;
    font-size: 12.5px !important;
  }
}
</style>
