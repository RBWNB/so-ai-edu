<template>
  <div class="knowledge-page">
    <div class="page-toolbar">
      <h2 class="page-title">海洋知识库</h2>
      <div class="toolbar-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索知识内容..."
          clearable
          :prefix-icon="Search"
          style="width:280px"
          @input="onSearch"
        />
      </div>
    </div>

    <div class="category-tabs-wrapper">
      <div class="category-tabs">
        <div
          v-for="cat in categories"
          :key="cat.key"
          class="category-tab"
          :class="{ active: activeCategory === cat.key }"
          @click="activeCategory = cat.key"
        >
          <el-icon><component :is="cat.icon" /></el-icon>
          <span>{{ cat.label }}</span>
          <span class="tab-count">{{ getCategoryCount(cat.key) }}</span>
        </div>
      </div>
    </div>

    <el-empty v-if="filteredArticles.length === 0" description="暂无相关知识内容" />

    <el-row v-else :gutter="16" class="articles-grid">
      <el-col :xs="24" :sm="12" :lg="8" v-for="article in filteredArticles" :key="article.id">
        <el-card shadow="hover" class="article-card" @click="openDetail(article)">
          <div class="article-cover">
            <img :src="article.cover" :alt="article.title" @error="onImgError" />
            <el-tag class="article-category-tag" size="small" :type="tagType(article.category)">
              {{ getCategoryLabel(article.category) }}
            </el-tag>
          </div>
          <div class="article-body">
            <h3 class="article-title">{{ article.title }}</h3>
            <p class="article-desc">{{ article.summary }}</p>
            <div class="article-meta">
              <span class="article-date">
                <el-icon><Clock /></el-icon>{{ article.date }}
              </span>
              <span class="article-views">
                <el-icon><View /></el-icon>{{ article.views }}
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="pagination-wrapper" v-if="filteredArticles.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredArticles.length"
        layout="total, prev, pager, next"
        background
        small
      />
    </div>

    <el-dialog v-model="detailVisible" :title="currentArticle?.title" width="720px" destroy-on-close>
      <div class="article-detail" v-if="currentArticle">
        <img :src="currentArticle.cover" :alt="currentArticle.title" class="detail-cover" @error="onImgError" />
        <div class="detail-meta">
          <el-tag size="small" :type="tagType(currentArticle.category)">
            {{ getCategoryLabel(currentArticle.category) }}
          </el-tag>
          <span class="detail-date">{{ currentArticle.date }}</span>
          <span class="detail-views"><el-icon><View /></el-icon>{{ currentArticle.views }}</span>
        </div>
        <div class="detail-body" v-html="currentArticle.content"></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="detailVisible = false">已了解</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, markRaw, ref } from 'vue'
import { Search, Clock, View, Collection, Monitor, Lock } from '@element-plus/icons-vue'

const searchKeyword = ref('')
const activeCategory = ref('all')
const currentPage = ref(1)
const pageSize = ref(9)

const detailVisible = ref(false)
const currentArticle = ref(null)

const categories = [
  { key: 'all', label: '全部', icon: markRaw(Collection) },
  { key: 'species', label: '物种', icon: markRaw(Collection) },
  { key: 'ecosystem', label: '生态系统', icon: markRaw(Monitor) },
  { key: 'protection', label: '保护知识', icon: markRaw(Lock) },
]

const articles = ref([
  {
    id: 1,
    category: 'species',
    title: '中华白海豚 —— 粉色海洋精灵',
    summary: '中华白海豚是国家一级保护动物，被誉为"海上大熊猫"，它们会随着年龄增长逐渐变成粉白色...',
    cover: '/uploads/species/2026/05/28/dolpine.png',
    date: '2025-12-15',
    views: 3280,
    content: '<p>中华白海豚（Sousa chinensis），属鲸目海豚科，是国家一级保护野生动物。成年个体体长约2.0~2.5米，体重约150~230千克。</p><p>幼年期的中华白海豚呈深灰色，随着年龄增长体色逐渐变浅，成年后呈现独特的粉白色或乳白色，这是由于皮下血管透过浅色皮肤所呈现的效果。</p><p>主要分布于我国东南沿海，包括珠江口、厦门海域、北部湾等水域，常栖息于水深不超过20米的近岸浅水区域。</p>',
  },
  {
    id: 2,
    category: 'species',
    title: '珊瑚 —— 海洋中的热带雨林',
    summary: '珊瑚礁生态系统是地球上生物多样性最高的生态系统之一，为约25%的海洋生物提供栖息地...',
    cover: '/uploads/species/2026/05/28/shanhu.png',
    date: '2025-12-10',
    views: 4520,
    content: '<p>珊瑚礁是海洋中最富生产力的生态系统之一，虽然仅占海洋面积的不到0.1%，却养育了约25%的海洋物种。</p><p>造礁石珊瑚与虫黄藻形成共生关系，虫黄藻通过光合作用为珊瑚提供能量，这也是珊瑚主要分布在透光层浅水区的原因。</p><p>当前全球珊瑚礁面临白化威胁，海水温度升高1-2°C即可导致珊瑚排出虫黄藻而发生白化，严重时将导致珊瑚死亡。</p>',
  },
  {
    id: 3,
    category: 'species',
    title: '海龟的迁徙之谜',
    summary: '海龟每年会进行数千公里的迁徙洄游，科学家通过卫星追踪技术揭示了它们惊人的导航能力...',
    cover: '/uploads/species/2026/05/28/turtle.png',
    date: '2025-12-08',
    views: 2150,
    content: '<p>海龟是海洋中最古老的爬行动物之一，现存7个物种分别是绿海龟、玳瑁、棱皮龟、蠵龟、肯氏龟、平背龟和丽龟。</p><p>成年雌海龟具有惊人的导航能力，能够在洄游数千公里后精准返回出生地的沙滩产卵，科学家认为它们可能利用地磁场作为导航线索。</p><p>我国南海诸岛是绿海龟和玳瑁的重要产卵地，目前所有海龟物种均已列入《濒危野生动植物种国际贸易公约》（CITES）附录I。</p>',
  },
  {
    id: 4,
    category: 'ecosystem',
    title: '红树林 —— 海岸卫士',
    summary: '红树林生态系统位于热带和亚热带潮间带，具有消浪护岸、净化水质、固碳储碳等关键生态功能...',
    cover: '/uploads/species/2026/05/28/redtree.png',
    date: '2025-12-12',
    views: 3890,
    content: '<p>红树林是由红树植物为主体的常绿乔木或灌木组成的湿地木本植物群落，生长在热带、亚热带海岸潮间带。</p><p>我国的红树林主要分布在海南、广东、广西、福建、浙江南部及台湾等沿海地区，总面积约2.7万公顷。</p><p>红树林生态系统具有极高的生态服务价值：每公顷红树林每年可固碳约1.5吨，同时为鱼类、虾蟹、鸟类等提供栖息和觅食场所。</p>',
  },
  {
    id: 5,
    category: 'ecosystem',
    title: '海草床 —— 被遗忘的蓝色碳汇',
    summary: '海草床是全球重要的"蓝碳"生态系统，其碳埋藏速率远超陆地森林，但保护关注远低于珊瑚礁...',
    cover: '/uploads/species/2026/05/28/oceangrass.png',
    date: '2025-12-05',
    views: 1780,
    content: '<p>海草是唯一能够完全生活在海水中的被子植物，全球约有72种海草，我国记录到22种。</p><p>海草床生态系统虽然全球面积仅约30万平方公里，但其碳埋藏速率是温带森林的35倍以上，承担着重要的"蓝碳"功能。</p><p>然而由于海岸开发、水质污染、拖网捕捞等影响，全球海草床正以每年约7%的速度消失，保护形势严峻。</p>',
  },
  {
    id: 6,
    category: 'ecosystem',
    title: '上升流生态 —— 海洋生产力的引擎',
    summary: '沿岸上升流将富含营养盐的底层海水带到透光层，支撑了全球约50%的渔获量...',
    cover: '/uploads/species/2026/05/28/upeco.png',
    date: '2025-11-28',
    views: 980,
    content: '<p>上升流是海洋中下层海水向表层涌升的现象，通常由沿岸风、海流地形作用等驱动。</p><p>上升流将底层富含硝酸盐、磷酸盐等营养盐的海水带到透光层，促进浮游植物大量繁殖，从而支撑了整个海洋食物网。</p><p>全球四大主要上升流系统（秘鲁、加利福尼亚、本格拉、加那利）虽仅占海洋面积的1%，却贡献了约50%的世界渔获量。</p>',
  },
  {
    id: 7,
    category: 'protection',
    title: '海洋保护区网络建设指南',
    summary: '科学规划海洋保护区网络是实现30×30保护目标的关键路径，我国已建立了271个海洋保护区...',
    cover: '/uploads/species/2026/05/28/protectocean.png',
    date: '2025-12-18',
    views: 5500,
    content: '<p>截至2025年，我国已建立各类海洋保护区271个，总面积约13.8万平方公里，涵盖海洋自然保护区、海洋特别保护区和海洋公园三大类型。</p><p>"30×30"目标（即到2030年保护全球30%的海洋）是国际社会的重要共识，需要各国协力推进海洋保护区的科学选址和有效管理。</p><p>海洋保护区网络应基于生态连通性原则进行规划，确保关键物种的洄游通道、产卵场和索饵场受到有效保护。</p>',
  },
  {
    id: 8,
    category: 'protection',
    title: '微塑料 —— 看不见的海洋威胁',
    summary: '每年约800万吨塑料进入海洋，微塑料已渗透到从表层海水到深海沉积物的各个角落...',
    cover: '/uploads/species/2026/05/28/pastic.png',
    date: '2025-12-01',
    views: 6200,
    content: '<p>微塑料指粒径小于5毫米的塑料碎片和颗粒，来源包括大块塑料的光降解破碎、化妆品中的微珠、合成纤维洗涤脱落等。</p><p>研究发现微塑料已广泛存在于全球海洋环境中，从北极海冰到马里亚纳海沟均检测到微塑料的存在。</p><p>微塑料不仅自身具有毒性效应，还能吸附海水中的持久性有机污染物（POPs），成为污染物的"载体"，通过食物链逐级传递并产生生物放大效应。</p>',
  },
  {
    id: 9,
    category: 'protection',
    title: '海洋酸化 —— 珊瑚礁的隐形杀手',
    summary: '大气CO₂浓度升高导致海洋酸化加速，pH值每下降0.1个单位，珊瑚钙化率将降低15-30%...',
    cover: '/uploads/species/2026/05/28/co.png',
    date: '2025-11-20',
    views: 3150,
    content: '<p>自工业革命以来，全球海洋表层海水pH值已从约8.2下降至约8.1，下降了约0.1个单位，相当于酸度增加了约30%。</p><p>海洋酸化直接影响碳酸钙饱和度，降低珊瑚、贝类、钙化浮游生物等造壳生物的钙化能力，威胁整个海洋食物网的基础。</p><p>根据IPCC预测，如果碳排放持续当前趋势，到2100年海洋表层pH值可能进一步下降0.3-0.4个单位，届时珊瑚礁将面临系统性崩溃的风险。</p>',
  },
])

const getCategoryLabel = (key) => {
  const cat = categories.find(c => c.key === key)
  return cat ? cat.label : ''
}

const getCategoryCount = (key) => {
  if (key === 'all') return articles.value.length
  return articles.value.filter(a => a.category === key).length
}

const tagType = (cat) => {
  return { species: '', ecosystem: 'success', protection: 'warning' }[cat] || 'info'
}

const filteredArticles = computed(() => {
  let list = articles.value

  if (activeCategory.value !== 'all') {
    list = list.filter(a => a.category === activeCategory.value)
  }

  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    list = list.filter(a =>
      a.title.toLowerCase().includes(kw) ||
      a.summary.toLowerCase().includes(kw) ||
      a.content.toLowerCase().includes(kw)
    )
  }

  return list
})

const onSearch = () => {
  currentPage.value = 1
}

const openDetail = (article) => {
  currentArticle.value = article
  detailVisible.value = true
}

const onImgError = (e) => {
  e.target.src = 'data:image/svg+xml,' + encodeURIComponent(`
    <svg xmlns="http://www.w3.org/2000/svg" width="400" height="200" viewBox="0 0 400 200">
      <rect fill="#e8f4f8" width="400" height="200"/>
      <text fill="#93c5fd" font-size="40" font-family="Arial" text-anchor="middle" x="200" y="115">🌊</text>
    </svg>
  `)
}
</script>

<style scoped>
.knowledge-page {
  min-height: calc(100vh - 120px);
}

.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--theme-text-primary);
}

.category-tabs-wrapper {
  margin-bottom: 20px;
}

.category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.category-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid var(--theme-border-light);
  color: var(--theme-text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  user-select: none;
}

.category-tab:hover {
  border-color: var(--theme-primary-light);
  color: var(--theme-primary);
  background: var(--theme-primary-soft);
}

.category-tab.active {
  background: var(--theme-primary);
  border-color: var(--theme-primary);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(22, 93, 255, 0.25);
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.06);
  color: var(--theme-text-muted);
}

.category-tab.active .tab-count {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.articles-grid {
  min-height: 300px;
}

.article-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(22, 93, 255, 0.12) !important;
}

.article-cover {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  height: 180px;
  margin-bottom: 14px;
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.article-card:hover .article-cover img {
  transform: scale(1.06);
}

.article-category-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.article-body {
  min-height: 100px;
}

.article-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--theme-text-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--theme-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--theme-text-muted);
}

.article-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 20px 0 10px;
}

.article-detail {
  max-height: 60vh;
  overflow-y: auto;
}

.detail-cover {
  width: 100%;
  border-radius: 10px;
  margin-bottom: 16px;
  max-height: 320px;
  object-fit: cover;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--theme-border-light);
  font-size: 13px;
  color: var(--theme-text-muted);
}

.detail-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.detail-body {
  font-size: 14px;
  color: var(--theme-text-secondary);
  line-height: 1.9;
}

.detail-body :deep(p) {
  margin: 0 0 14px;
  text-indent: 2em;
}

@media (max-width: 768px) {
  .page-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions :deep(.el-input) {
    width: 100% !important;
  }

  .category-tabs {
    gap: 6px;
  }

  .category-tab {
    padding: 8px 14px;
    font-size: 13px;
  }

  .article-cover {
    height: 140px;
  }
}
</style>