<template>
  <div class="species-count-page" v-loading="loading">
    <el-row :gutter="16">
      <!-- 左侧：分类单元饼图 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-title-row">
              <span class="card-title">各分类单元占比</span>
              <el-select v-model="taxonomyLevel" size="small" style="width:100px" @change="loadSpeciesCountStats">
                <el-option label="界" value="kingdom" />
                <el-option label="门" value="phylum" />
                <el-option label="纲" value="class" />
                <el-option label="目" value="order" />
                <el-option label="科" value="family" />
                <el-option label="属" value="genus" />
              </el-select>
            </div>
          </template>
          <div ref="taxonomyPieRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <!-- 右侧：保护等级饼图 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-title-row">
              <span class="card-title">{{ protectionChartTitle }}</span>
            </div>
          </template>
          <div ref="protectionPieRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部：物种列表 -->
    <el-row style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-title-row">
              <span class="card-title">{{ selectedCategoryTitle }}</span>
              <div class="header-actions">
                <el-tag v-if="selectedCategory" type="info" closable @close="clearSelection">
                  {{ selectedCategory }}
                </el-tag>
                <el-button
                    v-if="filteredSpeciesList.length > 0"
                    type="primary"
                    size="small"
                    @click="exportToExcel"
                    :icon="Download"
                >
                  导出报表
                </el-button>
              </div>
            </div>
          </template>

          <!-- 搜索栏 -->
          <div class="search-bar" v-if="filteredSpeciesList.length > 0">
            <el-input
                v-model="searchKeyword"
                placeholder="搜索中文名或学名..."
                clearable
                prefix-icon="Search"
                style="max-width: 400px"
            />
          </div>

          <!-- 搜索结果提示 -->
          <el-alert
              v-if="searchKeyword && searchResultMessage"
              :title="searchResultMessage"
              :type="searchResultType"
              show-icon
              :closable="false"
              style="margin-bottom: 16px"
          />

          <el-table :data="displaySpeciesList" stripe max-height="400" v-if="filteredSpeciesList.length > 0">
            <el-table-column prop="chineseName" label="中文名" min-width="150" />
            <el-table-column prop="scientificName" label="学名" min-width="200" />
            <el-table-column prop="conservationStatus" label="保护等级" width="120">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.conservationStatus)" size="small">
                  {{ row.conservationStatus || '未标注' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phylum" label="门" width="120" />
            <el-table-column prop="className" label="纲" width="120" />
            <el-table-column prop="orderName" label="目" width="120" />
            <el-table-column prop="familyName" label="科" width="120" />
            <el-table-column prop="genusName" label="属" width="120" />
          </el-table>
          <el-empty v-else description="请点击饼图选择分类查看物种列表" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from "vue";
import { ElMessage } from "element-plus";
import { Download } from "@element-plus/icons-vue";
import * as echarts from "echarts";
import * as XLSX from 'xlsx';
import { getSpeciesCountStats, getSpeciesRawList } from "@/api/visual";

const loading = ref(true);
const taxonomyLevel = ref("phylum");
const selectedCategory = ref(null);
const selectedChartType = ref(null); // 'taxonomy' or 'protection'
const searchKeyword = ref('');

const taxonomyPieRef = ref(null);
const protectionPieRef = ref(null);

let taxonomyChart = null;
let protectionChart = null;

const allSpeciesList = ref([]);
const taxonomyData = ref([]);
const protectionData = ref([]);

const GALLERY_PALETTE = ["#165DFF", "#36CFC9", "#FF7D00", "#52C41A", "#4080FF", "#F7BA1E", "#C96198"];

const protectionChartTitle = computed(() => {
  if (selectedCategory.value && selectedChartType.value === 'taxonomy') {
    return `保护等级占比（${selectedCategory.value}）`;
  }
  return '保护等级占比';
});

const selectedCategoryTitle = computed(() => {
  if (!selectedCategory.value) return "物种列表";
  const chartType = selectedChartType.value === 'taxonomy' ? '分类单元' : '保护等级';
  return `${chartType} - ${selectedCategory.value} 的物种`;
});

const filteredSpeciesList = computed(() => {
  if (!selectedCategory.value || !selectedChartType.value) return [];

  let filtered = allSpeciesList.value;

  // 如果先选择了分类单元，再选择保护等级，需要同时满足两个条件
  if (selectedChartType.value === 'taxonomy') {
    const level = taxonomyLevel.value;
    const fieldMap = {
      kingdom: 'kingdom',
      phylum: 'phylum',
      class: 'className',
      order: 'orderName',
      family: 'familyName',
      genus: 'genusName'
    };
    const field = fieldMap[level];
    filtered = filtered.filter(s => {
      const value = s[field] || '未分类';
      return value === selectedCategory.value;
    });
  } else if (selectedChartType.value === 'protection') {
    filtered = filtered.filter(s => {
      const status = s.conservationStatus || '未标注';
      return status === selectedCategory.value;
    });

    // 如果之前已经选择了分类单元，需要同时过滤
    if (taxonomyFilterCache.value) {
      const level = taxonomyLevel.value;
      const fieldMap = {
        kingdom: 'kingdom',
        phylum: 'phylum',
        class: 'className',
        order: 'orderName',
        family: 'familyName',
        genus: 'genusName'
      };
      const field = fieldMap[level];
      const taxonomyValue = taxonomyFilterCache.value;
      filtered = filtered.filter(s => {
        const value = s[field] || '未分类';
        return value === taxonomyValue;
      });
    }
  }

  return filtered;
});

// 显示的物种列表（根据搜索结果过滤）
const displaySpeciesList = computed(() => {
  if (!searchKeyword.value) {
    return filteredSpeciesList.value;
  }

  const keyword = searchKeyword.value.toLowerCase().trim();
  return filteredSpeciesList.value.filter(species => {
    const chineseName = (species.chineseName || '').toLowerCase();
    const scientificName = (species.scientificName || '').toLowerCase();
    return chineseName.includes(keyword) || scientificName.includes(keyword);
  });
});

// 搜索结果提示信息
const searchResultMessage = computed(() => {
  if (!searchKeyword.value) return '';

  const totalInScope = filteredSpeciesList.value.length;
  const searchResults = displaySpeciesList.value.length;

  if (searchResults === 0) {
    return `未找到包含"${searchKeyword.value}"的物种`;
  }

  if (searchResults < totalInScope) {
    return `在当前范围内找到 ${searchResults} 个相关物种（共 ${totalInScope} 个）`;
  }

  return '';
});

const searchResultType = computed(() => {
  if (!searchKeyword.value) return 'info';
  return displaySpeciesList.value.length > 0 ? 'success' : 'warning';
});

const getStatusTagType = (status) => {
  const typeMap = {
    'CR': 'danger',
    'EN': 'warning',
    'VU': 'warning',
    'NT': 'info',
    'LC': 'success'
  };
  return typeMap[status] || '';
};

const taxonomyFilterCache = ref(null); // 缓存当前选择的分类单元

// 导出Excel功能 - 使用浏览器默认下载
const exportToExcel = () => {
  try {
    const dataToExport = displaySpeciesList.value;

    if (dataToExport.length === 0) {
      ElMessage.warning('没有可导出的数据');
      return;
    }

    // 准备导出数据，按照指定列顺序
    const exportData = dataToExport.map(species => ({
      '中文名': species.chineseName || '',
      '学名': species.scientificName || '',
      '保护等级': species.conservationStatus || '未标注',
      '门': species.phylum || '',
      '纲': species.className || '',
      '目': species.orderName || '',
      '科': species.familyName || '',
      '属': species.genusName || ''
    }));

    // 创建工作簿和工作表
    const worksheet = XLSX.utils.json_to_sheet(exportData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, '物种数据');

    // 设置列宽
    const colWidths = [
      { wch: 20 }, // 中文名
      { wch: 30 }, // 学名
      { wch: 12 }, // 保护等级
      { wch: 15 }, // 门
      { wch: 15 }, // 纲
      { wch: 15 }, // 目
      { wch: 15 }, // 科
      { wch: 15 }  // 属
    ];
    worksheet['!cols'] = colWidths;

    // 生成文件名
    const timestamp = new Date().toISOString().slice(0, 10);
    const scopeInfo = selectedCategory.value ? `_${selectedCategory.value}` : '';
    const fileName = `物种数据${scopeInfo}_${timestamp}.xlsx`;

    // 使用浏览器默认下载功能
    XLSX.writeFile(workbook, fileName);

    ElMessage.success(`成功导出 ${dataToExport.length} 条物种数据到下载文件夹`);
  } catch (error) {
    console.error('导出失败:', error);
    ElMessage.error('导出失败，请重试');
  }
};

const loadSpeciesCountStats = async () => {
  try {
    const resp = await getSpeciesCountStats(taxonomyLevel.value);
    const data = resp.data;

    if (data) {
      taxonomyData.value = data.taxonomyStats || [];

      // 如果有选中的分类单元，计算该分类下的保护等级分布
      if (selectedCategory.value && selectedChartType.value === 'taxonomy') {
        const level = taxonomyLevel.value;
        const fieldMap = {
          kingdom: 'kingdom',
          phylum: 'phylum',
          class: 'className',
          order: 'orderName',
          family: 'familyName',
          genus: 'genusName'
        };
        const field = fieldMap[level];

        // 过滤出该分类单元的所有物种
        const filteredSpecies = allSpeciesList.value.filter(s => {
          const value = s[field] || '未分类';
          return value === selectedCategory.value;
        });

        // 计算这些物种的保护等级分布
        const protectionMap = {};
        filteredSpecies.forEach(s => {
          const status = s.conservationStatus || '未标注';
          protectionMap[status] = (protectionMap[status] || 0) + 1;
        });

        protectionData.value = Object.entries(protectionMap).map(([name, value]) => ({
          name,
          value
        })).sort((a, b) => b.value - a.value);
      } else {
        // 否则显示全部数据的保护等级分布
        protectionData.value = data.protectionStats || [];
      }

      renderTaxonomyPie();
      renderProtectionPie();
    }
  } catch (error) {
    console.error(error);
    ElMessage.error("加载物种统计数据失败");
  }
};

const loadAllSpecies = async () => {
  try {
    const resp = await getSpeciesRawList({ pageNum: 1, pageSize: 1000 });
    const records = resp.data?.records || resp.data || [];
    allSpeciesList.value = records;
  } catch (error) {
    console.error(error);
  }
};

const renderTaxonomyPie = () => {
  if (!taxonomyPieRef.value) return;

  if (taxonomyChart) {
    taxonomyChart.dispose();
  }

  taxonomyChart = echarts.init(taxonomyPieRef.value);

  const option = {
    color: GALLERY_PALETTE,
    tooltip: {
      trigger: "item",
      formatter: "{b}: {c} ({d}%)"
    },
    legend: {
      bottom: 0,
      type: "scroll",
      textStyle: { color: "#595959" }
    },
    series: [{
      type: "pie",
      radius: ["35%", "60%"],
      avoidLabelOverlap: true,
      label: {
        formatter: "{b}\n{c}",
        color: "#595959"
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      },
      data: taxonomyData.value.length ? taxonomyData.value : [{ name: "暂无数据", value: 1, itemStyle: { color: "rgba(0,0,0,0.06)" } }],
    }],
  };

  taxonomyChart.setOption(option);

  taxonomyChart.on('click', (params) => {
    if (params.name !== '暂无数据') {
      selectedCategory.value = params.name;
      selectedChartType.value = 'taxonomy';
      taxonomyFilterCache.value = params.name;
      searchKeyword.value = ''; // 清除搜索

      // 重新渲染保护等级饼图，显示该分类下的保护等级分布
      loadSpeciesCountStats();
    }
  });
};

const renderProtectionPie = () => {
  if (!protectionPieRef.value) return;

  if (protectionChart) {
    protectionChart.dispose();
  }

  protectionChart = echarts.init(protectionPieRef.value);

  const option = {
    color: GALLERY_PALETTE,
    tooltip: {
      trigger: "item",
      formatter: "{b}: {c} ({d}%)"
    },
    legend: {
      bottom: 0,
      type: "scroll",
      textStyle: { color: "#595959" }
    },
    series: [{
      type: "pie",
      radius: ["35%", "60%"],
      avoidLabelOverlap: true,
      label: {
        formatter: "{b}\n{c}",
        color: "#595959"
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      },
      data: protectionData.value.length ? protectionData.value : [{ name: "暂无数据", value: 1, itemStyle: { color: "rgba(0,0,0,0.06)" } }],
    }],
  };

  protectionChart.setOption(option);

  protectionChart.on('click', (params) => {
    if (params.name !== '暂无数据') {
      selectedCategory.value = params.name;
      selectedChartType.value = 'protection';
      searchKeyword.value = ''; // 清除搜索
    }
  });
};

const clearSelection = () => {
  selectedCategory.value = null;
  selectedChartType.value = null;
  taxonomyFilterCache.value = null;
  searchKeyword.value = ''; // 清除搜索

  // 重置保护等级饼图为全局数据
  loadSpeciesCountStats();
};

const handleResize = () => {
  taxonomyChart?.resize();
  protectionChart?.resize();
};

onMounted(async () => {
  window.addEventListener("resize", handleResize);
  loading.value = true;
  try {
    await Promise.all([loadAllSpecies(), loadSpeciesCountStats()]);
  } catch (e) {
    console.error(e);
    ElMessage.error("加载数据失败");
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  taxonomyChart?.dispose();
  protectionChart?.dispose();
});
</script>

<style scoped>
.species-count-page {
  min-height: calc(100vh - 120px);
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--theme-text-primary);
}

.chart-box {
  height: 400px;
}

.search-bar {
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
