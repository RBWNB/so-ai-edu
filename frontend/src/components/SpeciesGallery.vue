<template>
  <div class="species-gallery">
    <!-- Search bar -->
    <el-card class="search-card" shadow="never">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item>
          <el-input
            v-model="query.keyword"
            placeholder="搜索物种名称..."
            clearable
            @clear="handleSearch"
            size="large"
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button @click="handleSearch" type="primary">搜索</el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Loading state -->
    <div v-if="loading" class="gallery-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- Error state -->
    <el-result
      v-else-if="error"
      icon="error"
      title="加载失败"
      :sub-title="error"
    >
      <template #extra>
        <el-button type="primary" @click="fetchSpecies">重新加载</el-button>
      </template>
    </el-result>

    <!-- Empty state -->
    <el-empty
      v-else-if="!loading && speciesList.length === 0"
      description="暂无物种数据"
    />

    <!-- Gallery grid -->
    <template v-else>
      <el-row :gutter="20">
        <el-col
          v-for="item in speciesList"
          :key="item.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          class="gallery-col"
        >
          <el-card
            class="species-card"
            shadow="hover"
            @click="goDetail(item.id)"
          >
            <!-- Species image -->
            <div class="card-image-wrapper">
              <el-image
                v-if="item.imageUrl"
                :src="item.imageUrl"
                fit="cover"
                class="card-image"
                loading="lazy"
              >
                <template #error>
                  <div class="image-placeholder">
                    <el-icon :size="48"><PictureFilled /></el-icon>
                  </div>
                </template>
              </el-image>
              <div v-else class="image-placeholder">
                <el-icon :size="48"><PictureFilled /></el-icon>
              </div>
              <!-- Conservation status badge -->
              <el-tag
                v-if="item.conservationStatus"
                :type="statusType(item.conservationStatus)"
                effect="dark"
                size="small"
                class="status-badge"
              >
                {{ item.conservationStatus }}
              </el-tag>
            </div>

            <!-- Species info -->
            <div class="card-body">
              <h3 class="species-name">{{ item.chineseName }}</h3>
              <p class="scientific-name">{{ item.scientificName }}</p>
              <p class="taxonomy" v-if="item.phylum || item.className">
                {{ [item.phylum, item.className, item.orderName].filter(Boolean).join(' / ') }}
              </p>
              <p class="description" v-if="item.morphologyDesc">
                {{ truncate(item.morphologyDesc, 80) }}
              </p>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Pagination -->
      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[12, 24, 48, 96]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchSpecies"
          @size-change="onSizeChange"
          background
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { Search, PictureFilled } from "@element-plus/icons-vue";
import { getSpeciesPage } from "@/api/species";

const router = useRouter();
const speciesList = ref([]);
const total = ref(0);
const loading = ref(false);
const error = ref("");

const query = reactive({
  pageNum: 1,
  pageSize: 24,
  keyword: "",
});

const statusType = (status) => {
  const map = {
    CR: "danger",
    EN: "danger",
    VU: "warning",
    NT: "warning",
    LC: "success",
    DD: "info",
  };
  return map[status] || "info";
};

const truncate = (text, len) => {
  if (!text) return "";
  return text.length > len ? text.slice(0, len) + "..." : text;
};

const onSizeChange = () => {
  query.pageNum = 1;
  fetchSpecies();
};

const handleSearch = () => {
  query.pageNum = 1;
  fetchSpecies();
};

const fetchSpecies = async () => {
  loading.value = true;
  error.value = "";
  try {
    const params = { ...query };
    const res = await getSpeciesPage(params);
    speciesList.value = res.data?.records || res.data?.rows || [];
    total.value = res.data?.total || 0;
  } catch (e) {
    error.value = e.message || "获取数据失败";
  } finally {
    loading.value = false;
  }
};

const goDetail = (id) => {
  router.push({ name: "speciesDetail", params: { id } });
};

onMounted(() => {
  fetchSpecies();
});
</script>

<style scoped>
.species-gallery {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 0;
}

.search-card {
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(12px) !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
  :deep(.el-card__body) {
    padding: 16px 20px;
  }
}

.search-input {
  width: 400px;
  max-width: 100%;
}

.gallery-col {
  margin-bottom: 20px;
  cursor: pointer;
}

.species-card {
  transition: transform 0.3s, box-shadow 0.3s;
  background: rgba(255, 255, 255, 0.12) !important;
  backdrop-filter: blur(12px) !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
  overflow: hidden;
  :deep(.el-card__body) {
    padding: 0;
  }
}

.species-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15) !important;
}

.card-image-wrapper {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.1);
}

.card-image {
  width: 100%;
  height: 100%;
  display: block;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.05);
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.card-body {
  padding: 16px;
}

.species-name {
  color: var(--color-khaki);
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.scientific-name {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  font-style: italic;
  margin: 0 0 8px 0;
}

.taxonomy {
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
  margin: 0 0 8px 0;
}

.description {
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.gallery-loading {
  padding: 40px;
  :deep(.el-skeleton__item) {
    background: rgba(255, 255, 255, 0.1);
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
