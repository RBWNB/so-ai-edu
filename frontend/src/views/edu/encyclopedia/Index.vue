<template>
  <div class="gallery-page">
    <el-card shadow="never" class="gallery-card">
      <template #header>
        <div class="gallery-header">
          <span class="gallery-title">生物展览馆</span>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索物种名称..."
            clearable
            style="width: 250px"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
        </div>
      </template>

      <div v-loading="loading" class="gallery-content">
        <el-empty v-if="!loading && galleryData.length === 0" description="暂无物种图片" />

        <el-row v-else :gutter="20">
          <el-col
            v-for="item in galleryData"
            :key="item.id"
            :xs="12"
            :sm="8"
            :md="6"
            :lg="6"
            :xl="6"
            class="gallery-item"
          >
            <el-card shadow="hover" class="species-card" @click="viewDetail(item)">
              <div class="image-wrapper">
                <el-image
                  v-if="item.imageUrl"
                  :src="getImageUrl(item.imageUrl)"
                  fit="cover"
                  class="species-image"
                  :preview-src-list="[getImageUrl(item.imageUrl)]"
                  lazy
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                      <span>图片加载失败</span>
                    </div>
                  </template>
                </el-image>
                <div v-else class="image-placeholder">
                  <el-icon><Picture /></el-icon>
                  <span>暂无图片</span>
                </div>
              </div>

              <div class="species-info">
                <el-tag
                  v-if="item.conservationStatus"
                  :type="getStatusType(item.conservationStatus)"
                  size="small"
                  effect="dark"
                  class="status-tag"
                >
                  {{ item.conservationStatus }}
                </el-tag>
                <div class="species-name">{{ item.chineseName }}</div>
                <div class="scientific-name">{{ item.scientificName }}</div>
                <div class="taxonomy-info">
                  {{ item.phylum || '' }}{{ item.phylum ? ' / ' : '' }}{{ item.className || '' }}{{ item.className && item.orderName ? ' / ' : '' }}{{ item.orderName || '' }}
                </div>
                <div class="species-desc">
                  {{ (item.morphologyDesc || item.habitDesc || '').substring(0, 50) }}{{ (item.morphologyDesc || item.habitDesc || '').length > 50 ? '...' : '' }}
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <div v-if="galleryData.length > 0" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="total"
            :page-sizes="[12, 24, 48]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search, Picture } from '@element-plus/icons-vue';
import { getSpeciesPage } from '@/api/species';

const router = useRouter();
const loading = ref(false);
const searchKeyword = ref('');
const galleryData = ref([]);
const total = ref(0);

const pagination = ref({
  current: 1,
  size: 12,
});

const getStatusType = (status) => {
  const map = { CR: 'danger', EN: 'warning', VU: 'warning', NT: 'info', LC: 'success' };
  return map[status] || 'info';
};

const getImageUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  if (url.startsWith('/uploads/')) {
    return `/api${url}`;
  }
  if (url.startsWith('/')) {
    return `/api/uploads${url}`;
  }
  return `/api/uploads/${url}`;
};

const fetchGalleryData = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.value.current,
      pageSize: pagination.value.size,
      current: pagination.value.current,
      size: pagination.value.size,
    };

    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim();
    }

    const response = await getSpeciesPage(params);
    const data = response.data;
    const records = data.records || data.list || data.rows || [];

    galleryData.value = records.filter(item => item.chineseName || item.scientificName);
    total.value = Number(data.total || records.length);

    console.log('Gallery data loaded:', galleryData.value);
    galleryData.value.forEach(item => {
      console.log(`Species: ${item.chineseName}, Image: ${item.imageUrl}`);
    });
  } catch (error) {
    console.error('获取物种数据失败:', error);
    ElMessage.error('加载物种数据失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.value.current = 1;
  fetchGalleryData();
};

const handleSizeChange = (size) => {
  pagination.value.size = size;
  pagination.value.current = 1;
  fetchGalleryData();
};

const handleCurrentChange = (current) => {
  pagination.value.current = current;
  fetchGalleryData();
};

const viewDetail = (item) => {
  router.push({ name: 'speciesDetail', params: { id: item.id } });
};

onMounted(() => {
  fetchGalleryData();
});
</script>

<style scoped>
.gallery-page {
  min-height: calc(100vh - 140px);
}

/* 外层卡片：完全继承全局毛玻璃主题 */
.gallery-card {
  /* 仅保留布局属性，去除背景和边框的 !important 覆写 */
}

.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.gallery-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--theme-text-primary); /* 深色文本，高对比度 */
}

.gallery-content {
  min-height: 400px;
}

.gallery-item {
  margin-bottom: 20px;
}

/* 内部物种小卡片：浅色拟态毛玻璃 */
.species-card {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: rgba(255, 255, 255, 0.6) !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  border-radius: 12px;
  height: 100%;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 47, 167, 0.04) !important;
}

.species-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 47, 167, 0.12) !important;
  background: rgba(255, 255, 255, 0.75) !important;
}

.image-wrapper {
  width: 100%;
  height: 200px;
  overflow: hidden;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(0, 47, 167, 0.03) 0%, rgba(45, 212, 191, 0.03) 100%);
  position: relative;
  margin-bottom: 8px;
}

.species-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-error,
.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--theme-text-muted);
  gap: 8px;
  background: rgba(0, 47, 167, 0.02);
}

.image-error .el-icon,
.image-placeholder .el-icon {
  font-size: 48px;
  opacity: 0.4;
}

.species-info {
  padding: 12px;
  position: relative;
}

.status-tag {
  position: absolute;
  top: 4px;
  right: 12px;
}

.species-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--theme-text-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 40px;
}

.scientific-name {
  font-size: 12px;
  color: var(--theme-klein-blue);
  font-style: italic;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: 0.7;
}

.taxonomy-info {
  font-size: 11px;
  color: var(--theme-text-secondary);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: 0.65;
}

.species-desc {
  font-size: 12px;
  color: var(--theme-text-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  opacity: 0.75;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

:deep(.el-card__header) {
  background: transparent !important;
  border-bottom: 1px solid rgba(0, 47, 167, 0.06) !important;
}
</style>
