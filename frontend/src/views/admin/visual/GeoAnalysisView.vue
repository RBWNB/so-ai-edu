<template>
  <div class="geo-analysis-page" v-loading="loading">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-icon blue">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-value">{{ totalEcosystems }}</div>
            <div class="summary-label">生态系统数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 生态系统分布（扇形图） -->
    <el-row style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-title-row">
              <span class="card-title">生态系统物种分布</span>
              <el-button
                  v-if="canExport"
                  size="small"
                  @click="exportChartPDF('ecosystem', '生态系统物种分布')"
              >
                导出报表
              </el-button>
            </div>
          </template>
          <div ref="ecosystemChartRef" class="chart-box-medium"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 生态系统详细数据表格 -->
    <el-row style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-title-row">
              <span class="card-title">生态系统详细统计</span>
              <el-button
                  type="primary"
                  size="small"
                  @click="exportEcosystemData"
                  :icon="Download"
              >
                导出数据
              </el-button>
            </div>
          </template>

          <el-table
              :data="ecosystemData"
              stripe
              max-height="400"
          >
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="name" label="生态系统名称" min-width="150">
              <template #default="{ row }">
                <el-tag type="info">{{ row.name }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="260" show-overflow-tooltip />
            <el-table-column prop="typicalSpecies" label="典型物种" min-width="180" show-overflow-tooltip />
            <el-table-column prop="threats" label="主要威胁" min-width="150" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from "vue";
import { ElMessage } from "element-plus";
import { Collection, Download } from "@element-plus/icons-vue";
import * as echarts from "echarts";
import dayjs from "dayjs";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";
import * as XLSX from 'xlsx';
import { useAuthStore } from "@/store/auth";
import { getEcosystemStats } from "@/api/visual";

const authStore = useAuthStore();

const canExport = computed(() => {
  return authStore.hasRole('ADMIN') || authStore.hasRole('MANAGER');
});

const loading = ref(true);
const ecosystemData = ref([]);

const totalEcosystems = computed(() => ecosystemData.value.length);

const ecosystemChartRef = ref(null);
let ecosystemChart = null;

const GALLERY_PALETTE = ["#165DFF", "#36CFC9", "#FF7D00", "#52C41A", "#4080FF", "#F7BA1E", "#C96198"];

const loadEcosystemStats = async () => {
  try {
    const resp = await getEcosystemStats();
    ecosystemData.value = resp.data || [];
    renderEcosystemChart();
  } catch (error) {
    console.error(error);
    ElMessage.error("加载生态系统统计数据失败");
  }
};

const renderEcosystemChart = () => {
  if (!ecosystemChartRef.value) return;

  if (ecosystemChart) {
    ecosystemChart.dispose();
  }

  ecosystemChart = echarts.init(ecosystemChartRef.value);

  const data = ecosystemData.value.map(item => ({
    name: item.name,
    value: (item.typicalSpecies || "").split(/[,，]/).filter(Boolean).length || 1,
    description: item.description || "",
    threats: item.threats || "",
  }));

  const option = {
    color: GALLERY_PALETTE,
    tooltip: {
      trigger: 'item',
      formatter: function(params) {
        return `
          <div style="font-weight:bold;margin-bottom:5px">${params.name}</div>
          <div>关联物种: ${params.value} 种</div>
          <div>占比: ${params.percent}%</div>
        `;
      }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: "#595959" },
      formatter: function(name) {
        const item = ecosystemData.value.find(d => d.name === name);
        if (item) {
          const count = (item.typicalSpecies || "").split(/[,，]/).filter(Boolean).length || 1;
          return `${name} (物种:${count})`;
        }
        return name;
      }
    },
    series: [
      {
        name: '生态系统',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          color: "#595959",
          fontSize: 12
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        },
        data: data.length ? data : [{ name: "暂无数据", value: 1, itemStyle: { color: "rgba(0,0,0,0.06)" } }]
      }
    ]
  };

  ecosystemChart.setOption(option);
};

const exportEcosystemData = () => {
  if (ecosystemData.value.length === 0) {
    ElMessage.warning('没有可导出的数据');
    return;
  }

  const exportData = ecosystemData.value.map((item, index) => ({
    '序号': index + 1,
    '生态系统名称': item.name,
    '描述': item.description || '',
    '典型物种': item.typicalSpecies || '',
    '主要威胁': item.threats || '',
  }));

  const worksheet = XLSX.utils.json_to_sheet(exportData);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, '生态系统统计');

  const colWidths = [
    { wch: 8 },
    { wch: 20 },
    { wch: 40 },
    { wch: 30 },
    { wch: 20 },
  ];
  worksheet['!cols'] = colWidths;

  const timestamp = dayjs().format('YYYY-MM-DD');
  const fileName = `生态系统统计_${timestamp}.xlsx`;

  XLSX.writeFile(workbook, fileName);
  ElMessage.success(`成功导出 ${exportData.length} 条生态系统数据`);
};

const exportChartPDF = async (chartKey, title) => {
  const chartRefs = { ecosystem: ecosystemChartRef };
  const ref = chartRefs[chartKey];

  if (!ref?.value) {
    ElMessage.warning("图表未加载完成");
    return;
  }

  try {
    const cardEl = ref.value.closest(".el-card");
    if (!cardEl) {
      ElMessage.warning("未找到图表容器");
      return;
    }

    const btns = cardEl.querySelectorAll(".el-button--small");
    btns.forEach(b => { b.style.visibility = "hidden" });

    const canvas = await html2canvas(cardEl, {
      backgroundColor: "#fff",
      scale: 2,
      useCORS: true
    });

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

const handleResize = () => {
  ecosystemChart?.resize();
};

onMounted(async () => {
  window.addEventListener("resize", handleResize);
  loading.value = true;
  try {
    await loadEcosystemStats();
  } catch (e) {
    console.error(e);
    ElMessage.error("加载数据失败");
  } finally {
    loading.value = false;
  }
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  ecosystemChart?.dispose();
});
</script>

<style scoped>
.geo-analysis-page {
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

.chart-box-medium {
  height: 450px;
}
</style>
