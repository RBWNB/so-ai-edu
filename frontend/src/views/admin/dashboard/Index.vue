<template>
  <div class="dashboard-page" v-loading="loading">
    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="8" v-for="card in summaryCards" :key="card.label">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-icon" :class="card.color">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-value">{{ card.value }}</div>
            <div class="summary-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <div class="card-title-row">
              <span class="card-title">保护等级分布</span>
              <el-button v-if="canExport" size="small" @click="exportChartPDF('pie', '保护等级分布')">导出报表</el-button>
            </div>
          </template>
          <div ref="pieRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <div class="card-title-row">
              <span class="card-title">分类单元（门）</span>
              <div style="display:flex;gap:8px;align-items:center">
                <el-select v-model="taxonomyLevel" size="small" style="width:100px" @change="loadTaxonomy">
                  <el-option label="门" value="phylum" />
                  <el-option label="纲" value="class" />
                  <el-option label="目" value="order" />
                </el-select>
                <el-button v-if="canExport" size="small" @click="exportChartPDF('tax', '分类单元')">导出报表</el-button>
              </div>
            </div>
          </template>
          <div ref="taxRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>
            <div class="card-title-row">
              <span class="card-title">生态系统统计</span>
              <el-button v-if="canExport" size="small" @click="exportChartPDF('eco', '生态系统统计')">导出报表</el-button>
            </div>
          </template>
          <div ref="ecoRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Collection, Histogram } from "@element-plus/icons-vue";
import * as echarts from "echarts";
import dayjs from "dayjs";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";
import { useAuthStore } from "@/store/auth";
import {
  getDashboardSummary,
  getEcosystemStats,
  getSpeciesProtectionStats,
  getSpeciesRawList,
  getSpeciesTaxonomyStats,
} from "@/api/visual";

const authStore = useAuthStore();

const canExport = computed(() => {
  return authStore.hasRole('ADMIN') || authStore.hasRole('MANAGER');
});

const loading = ref(true);
const summaryCards = reactive([
  { label: "物种总数", value: 0, key: "speciesCount", icon: markRaw(Collection), color: "blue" },
  { label: "生态系统", value: 0, key: "ecosystemCount", icon: markRaw(Histogram), color: "teal" },
]);

const taxonomyLevel = ref("phylum");

const pieRef = ref(null);
const taxRef = ref(null);
const ecoRef = ref(null);

let pieChart, taxChart, ecoChart;

const extractList = (payload) => {
  const candidates = [payload, payload?.data, payload?.result, payload?.page, payload?.records, payload?.rows, payload?.list];
  for (const c of candidates) {
    if (!c) continue;
    if (Array.isArray(c)) return c;
    if (Array.isArray(c.records)) return c.records;
    if (Array.isArray(c.rows)) return c.rows;
    if (Array.isArray(c.list)) return c.list;
  }
  return [];
};

const aggregateByField = (rows, field) => {
  const map = {};
  rows.forEach(item => {
    const key = (item[field] || "未标注").trim() || "未标注";
    map[key] = (map[key] || 0) + 1;
  });
  return Object.entries(map).map(([name, value]) => ({ name, value }));
};

// ============ 加载数据 ============

const loadSummary = async () => {
  try {
    const resp = await getDashboardSummary();
    const d = resp.data;
    if (d) {
      summaryCards.forEach(c => { c.value = d[c.key] ?? 0 });
    }
  } catch { /* ignore */ }
};

const GALLERY_PALETTE = ["#165DFF", "#36CFC9", "#FF7D00", "#52C41A", "#4080FF"];

const renderPie = (refEl, chartRef, data) => {
  if (!refEl.value) return;
  if (!chartRef) chartRef = echarts.init(refEl.value);
  chartRef.setOption({
    color: GALLERY_PALETTE,
    tooltip: { trigger: "item", formatter: "{b}: {c} ({d}%)" },
    legend: { bottom: 0, type: "scroll", textStyle: { color: "#595959" } },
    series: [{
      type: "pie", radius: ["35%", "60%"], avoidLabelOverlap: true,
      label: { formatter: "{b}", color: "#595959" },
      data: data.length ? data : [{ name: "暂无数据", value: 1, itemStyle: { color: "rgba(0,0,0,0.06)" } }],
    }],
  });
  return chartRef;
};

const loadProtection = async () => {
  let data;
  try {
    const resp = await getSpeciesProtectionStats();
    data = extractList(resp?.data);
  } catch {
    const sp = await getSpeciesRawList();
    data = aggregateByField(extractList(sp?.data), "conservationStatus");
  }
  pieChart = renderPie(pieRef, pieChart, data);
};

const loadTaxonomy = async () => {
  let data;
  try {
    const resp = await getSpeciesTaxonomyStats(taxonomyLevel.value);
    data = extractList(resp?.data);
  } catch { data = [] }
  taxChart = renderPie(taxRef, taxChart, data);
};

const loadEcosystemStats = async () => {
  let data;
  try {
    const resp = await getEcosystemStats();
    data = extractList(resp?.data);
  } catch { data = [] }

  if (!ecoRef.value) return;
  if (!ecoChart) ecoChart = echarts.init(ecoRef.value);
  const names = data.map(d => d.name || "未知");
  ecoChart.setOption({
    color: GALLERY_PALETTE,
    tooltip: { trigger: "axis" },
    legend: { bottom: 0, textStyle: { color: "#595959" } },
    grid: { left: 50, right: 20, top: 20, bottom: 50 },
    xAxis: {
      type: "category", data: names,
      axisLabel: { rotate: 30, color: "#595959" },
      axisLine: { lineStyle: { color: "#D9E6FF" } },
      axisTick: { show: false },
    },
    yAxis: {
      type: "value", minInterval: 1,
      axisLabel: { color: "#595959" },
      splitLine: { lineStyle: { color: "#EEF3FB" } },
    },
    series: [
      { name: "典型物种", type: "bar", data: data.map(d => (d.typicalSpecies || "").split(/[,，]/).filter(Boolean).length || 0) },
    ],
  });
};

// ============ 导出 PDF ============

const exportChartPDF = async (chartKey, title) => {
  const chartRefs = { pie: pieRef, tax: taxRef, eco: ecoRef };
  const ref = chartRefs[chartKey];
  if (!ref?.value) { ElMessage.warning("图表未加载完成"); return }
  try {
    const cardEl = ref.value.closest(".el-card");
    if (!cardEl) { ElMessage.warning("未找到图表容器"); return }
    const btns = cardEl.querySelectorAll(".el-button--small, .el-select");
    btns.forEach(b => { b.style.visibility = "hidden" });
    const canvas = await html2canvas(cardEl, { backgroundColor: "#fff", scale: 2, useCORS: true });
    btns.forEach(b => { b.style.visibility = "" });
    const imgData = canvas.toDataURL("image/png");
    const pdf = new jsPDF("landscape", "mm", "a4");
    const pdfW = pdf.internal.pageSize.getWidth();
    const pdfH = pdf.internal.pageSize.getHeight();
    const imgW = canvas.width;
    const imgH = canvas.height;
    const ratio = Math.min(pdfW / imgW, pdfH / imgH) * 0.95;
    const offsetX = (pdfW - imgW * ratio) / 2;
    const offsetY = (pdfH - imgH * ratio) / 2;
    pdf.addImage(imgData, "PNG", offsetX, offsetY, imgW * ratio, imgH * ratio);
    pdf.save(`${title}_${dayjs().format("YYYY-MM-DD")}.pdf`);
    ElMessage.success("报表已导出");
  } catch (e) {
    ElMessage.error("导出失败");
    console.error(e);
  }
};

// ============ 生命周期 ============

const handleResize = () => {
  [pieChart, taxChart, ecoChart].forEach(c => c?.resize());
};

const initCharts = async () => {
  loading.value = true;
  try {
    await Promise.all([loadSummary(), loadProtection(), loadTaxonomy(), loadEcosystemStats()]);
    await nextTick();
    [pieChart, taxChart, ecoChart].forEach(c => c?.resize());
  } catch (e) {
    console.error(e);
    ElMessage.error("加载看板数据失败");
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  window.addEventListener("resize", handleResize);
  initCharts();
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  [pieChart, taxChart, ecoChart].forEach(c => c?.dispose());
});
</script>

<style scoped>
.dashboard-page {
  min-height: calc(100vh - 120px);
}

.summary-row {
  margin-bottom: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: default;
  min-height: 96px;
}

.summary-card :deep(.el-card__body) {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
}

.summary-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 25px;
  flex: 0 0 auto;
}

.summary-icon.blue {
  background: var(--theme-primary);
}

.summary-icon.teal {
  background: var(--theme-secondary);
}

.summary-content {
  min-width: 0;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--theme-text-primary);
  line-height: 1.2;
}

.summary-label {
  font-size: 14px;
  color: var(--theme-text-secondary);
  margin-top: 4px;
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
  height: 360px;
}

</style>
