<template>
  <div class="dashboard-page" v-loading="loading">
    <div style="display: flex; justify-content: flex-end; gap: 12px; margin-bottom: 16px;">

      <el-button
          v-if="canExport"
          type="success"
          :icon="MagicStick"
          @click="handleTriggerHighlight"
          :loading="triggering"
      >
        AI 一键生成高光广播
      </el-button>

      <el-button
          v-if="canExport"
          type="primary"
          :icon="Bell"
          @click="broadcastDialogVisible = true"
      >
        发布全站广播
      </el-button>
    </div>

    <el-row :gutter="16" class="summary-row">
    </el-row>
    <el-dialog v-model="broadcastDialogVisible" title="发布全站广播" width="450px" destroy-on-close>
      <el-form :model="broadcastForm" label-width="80px">
        <el-form-item label="广播内容" required>
          <el-input
              v-model="broadcastForm.content"
              type="textarea"
              :rows="4"
              maxlength="200"
              show-word-limit
              placeholder="请输入要发送给所有用户的系统通知..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="broadcastDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitBroadcast" :loading="broadcasting">
            确认发送
          </el-button>
        </span>
      </template>
    </el-dialog>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="12" :sm="12" :lg="6" v-for="card in kpiCards" :key="card.label">
        <el-card shadow="hover" class="summary-card">
          <div class="summary-icon" :class="card.color">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
          <div class="summary-content">
            <div class="summary-value">
              {{ card.value }}
            </div>
            <div class="summary-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :xs="24" :lg="16">
        <el-card>
          <template #header>
            <div class="card-title-row">
              <span class="card-title">近7天活跃答题人次趋势</span>
              <el-button v-if="canExport" size="small" @click="exportChartPDF('trend', '活跃趋势')">导出报表</el-button>
            </div>
          </template>
          <div ref="trendRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="card-title-row">
              <span class="card-title">用户热点提问词云</span>
              <el-button v-if="canExport" size="small" @click="exportChartPDF('ai', '用户热点提问词云')">导出报表</el-button>
            </div>
          </template>
          <div ref="aiSceneRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="card-title-row">
              <span class="card-title">物种保护等级分布</span>
              <el-button v-if="canExport" size="small" @click="exportChartPDF('pie', '保护等级分布')">导出报表</el-button>
            </div>
          </template>
          <div ref="pieRef" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="card-title-row">
              <span class="card-title">物种分类单元</span>
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

      <el-col :xs="24" :lg="8">
        <el-card>
          <template #header>
            <div class="card-title-row">
              <span class="card-title">生态系统物种分布</span>
              <el-button v-if="canExport" size="small" @click="exportChartPDF('ecosystem', '生态系统物种分布')">导出报表</el-button>
            </div>
          </template>
          <div ref="ecosystemRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, markRaw, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { User, Cpu, Document, EditPen, Bell,MagicStick} from "@element-plus/icons-vue";
import * as echarts from "echarts";
import dayjs from "dayjs";
import jsPDF from "jspdf";
import html2canvas from "html2canvas";
import { useAuthStore } from "@/store/auth";
import 'echarts-wordcloud';

import {
  getAdminDashboardKpi,
  getAiWordCloud,
  getActivityTrend,
  getSpeciesProtectionStats,
  getSpeciesRawList,
  getSpeciesTaxonomyStats,
  getEcosystemStats,
  sendSystemBroadcast,
  triggerHighlightBroadcast
} from "@/api/visual";

const authStore = useAuthStore();

const canExport = computed(() => {
  return authStore.hasRole('ADMIN') || authStore.hasRole('MANAGER');
});

const loading = ref(true);

// 顶部 KPI 数据模型
const kpiCards = reactive([
  { label: "平台总用户", value: 0, key: "userCount", icon: markRaw(User), color: "blue" },
  { label: "累计答题人次", value: 0, key: "quizAttempts", icon: markRaw(EditPen), color: "teal" },
  { label: "知识库文档", value: 0, key: "kbCount", icon: markRaw(Document), color: "orange" },
  { label: "AI 调用总数", value: 0, key: "aiCallCount", icon: markRaw(Cpu), color: "purple" }
]);

const taxonomyLevel = ref("phylum");

// 图表 DOM 引用
const trendRef = ref(null);
const aiSceneRef = ref(null);
const pieRef = ref(null);
const taxRef = ref(null);
const ecosystemRef = ref(null);

// 图表实例
let trendChart, aiSceneChart, pieChart, taxChart, ecosystemChart;

const GALLERY_PALETTE = ["#165DFF", "#36CFC9", "#FF7D00", "#52C41A", "#722ED1", "#F53F3F"];

// ════════ 广播逻辑 ════════
const broadcastDialogVisible = ref(false);
const broadcasting = ref(false);
const broadcastForm = reactive({ content: "" });

const submitBroadcast = async () => {
  if (!broadcastForm.content.trim()) {
    ElMessage.warning("广播内容不能为空");
    return;
  }
  broadcasting.value = true;
  try {
    const res = await sendSystemBroadcast({ content: broadcastForm.content });
    if (res?.data?.success) {
      ElMessage.success(res.data.message || "广播发送成功");
      broadcastDialogVisible.value = false;
      broadcastForm.content = ""; // 清空表单
    } else {
      ElMessage.error(res?.data?.message || "发送失败");
    }
  } catch (e) {
    ElMessage.error("系统异常");
  } finally {
    broadcasting.value = false;
  }
};

// ════════ 触发 AI 高光广播逻辑 ════════
const triggering = ref(false);

const handleTriggerHighlight = async () => {
  try {
    triggering.value = true;
    const res = await triggerHighlightBroadcast();
    // 兼容可能存在的不同数据包装层级
    if (res?.data?.success || res?.success) {
      ElMessage.success("触发指令已发送，AI正在提炼文案，稍后将下发至全站");
    } else {
      ElMessage.error(res?.data?.message || res?.message || "触发失败，可能今日暂无素材");
    }
  } catch (e) {
    ElMessage.error("系统异常，请检查后端服务");
  } finally {
    triggering.value = false;
  }
};

// ============ 数据处理工具 ============
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

// ============ 加载与渲染数据 ============

//  加载 KPI 大盘数据
const loadKpi = async () => {
  try {
    const resp = await getAdminDashboardKpi();
    const d = resp.data || resp;
    if (d) {
      kpiCards.forEach(c => { c.value = d[c.key] || 0 });
    }
  } catch (e) {
    console.error("加载 KPI 失败", e);
  }
};

// 加载近7天活跃趋势 (折线图)
const loadTrend = async () => {
  try {
    const resp = await getActivityTrend();
    const d = resp.data || resp;
    if (!d || !d.dates) return;

    if (!trendRef.value) return;
    if (!trendChart) trendChart = echarts.init(trendRef.value);

    trendChart.setOption({
      color: ["#165DFF"],
      tooltip: { trigger: "axis" },
      grid: { left: "3%", right: "4%", bottom: "3%", containLabel: true },
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: d.dates,
        axisLine: { lineStyle: { color: "#E5E6EB" } },
        axisLabel: { color: "#4E5969" },
      },
      yAxis: {
        type: "value",
        splitLine: { lineStyle: { type: "dashed", color: "#E5E6EB" } },
        axisLabel: { color: "#4E5969" },
      },
      series: [
        {
          name: "答题人次",
          type: "line",
          smooth: true,
          symbolSize: 8,
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: "rgba(22, 93, 255, 0.2)" },
              { offset: 1, color: "rgba(22, 93, 255, 0)" },
            ]),
          },
          data: d.quizData,
        },
      ],
    });
  } catch (e) {
    console.error("加载活跃趋势失败", e);
  }
};

// ============ 词云关键词提取 ============

// 中文停用词 / 无意义问句词（命中后整句过滤）
const STOP_QUESTIONS = [
  "你好", "在吗", "你是谁", "hello", "hi", "你是什么",
  "你是基于", "大模型", "输出", "流式", "你啊",
];

// 中文停用词（用于拆分短语后排除）
const STOP_WORDS = new Set([
  "什么", "怎么", "为什么", "如何", "吗", "呢", "吧", "的", "了", "是", "在",
  "有", "和", "与", "或", "可以", "能", "要", "会", "这个", "那个", "哪些",
  "哪种", "请问", "我想", "帮我", "告诉我", "解释", "说明", "描述", "介绍",
  "一下", "一个", "一些", "关于", "比如", "例如", "谢谢", "请",
  "能不能", "是否", "怎样", "什么样", "问题", "问一下",
  "来讲", "来说", "讲一下", "说一下", "聊一下", "聊聊",
  "我想问", "我要问", "我想知道", "我想了解", "知不知道",
  "属于", "具有", "还有", "其他", "它的", "他们的",
  "不", "都", "就", "也", "还", "只", "很", "太", "更", "最",
  "啊", "嘛", "哦", "噢", "嗯", "哈", "呀", "哇",
  "这", "那", "哪", "谁", "啥", "咋",
  "用", "做", "去", "来", "看", "说", "问", "给", "让", "叫",
  "应该", "需要", "必须", "一定", "可能", "大概",
  "知道", "了解", "认识", "觉得", "认为", "希望", "想要",
  "比较", "非常", "特别", "十分", "相当", "挺",
  "属于哪", "有哪些", "叫什么", "长什么",
]);

// 海洋领域关键词库（按长度降序保证长词优先匹配）
const MARINE_KEYWORDS = [
  // === 具体物种 ===
  "中华白海豚", "白海豚", "海豚", "蓝鲸", "虎鲸", "座头鲸", "抹香鲸", "灰鲸",
  "鲸鲨", "大白鲨", "锤头鲨", "虎鲨", "蝠鲼", "魔鬼鱼", "鳐鱼",
  "绿海龟", "玳瑁", "棱皮龟", "蠵龟", "丽龟", "海龟",
  "海马", "海龙", "海蛇", "海狮", "海豹", "海牛", "儒艮",
  "红珊瑚", "珊瑚", "海葵", "海星", "海胆", "海参", "水母",
  "章鱼", "乌贼", "鱿鱼", "鹦鹉螺", "砗磲", "牡蛎", "扇贝",
  "招潮蟹", "寄居蟹", "龙虾", "对虾", "磷虾",
  "海草", "海藻", "巨藻", "马尾藻", "红树林", "红树",
  // === 保护相关 ===
  "保护等级", "自然保护区", "保护现状", "保护动物", "栖息地", "保护区",
  "濒危", "极危", "易危", "近危", "无危",
  // === 生物学 ===
  "生物多样性", "海洋生态", "生态系统", "食物链", "食物网",
  "学名", "习性", "分布", "食性", "形态", "特征",
  "寿命", "繁殖", "迁徙", "洄游", "产卵",
  // === 生态 ===
  "珊瑚礁", "海草床", "潮间带", "深海", "热液喷口",
  "全球变暖", "海洋酸化", "过度捕捞", "塑料污染",
  // === 地理 ===
  "珠江口", "北部湾", "南海", "东海", "黄海", "渤海",
  "西沙", "南沙", "中沙", "东沙", "太平洋", "印度洋",
  // === 分类 ===
  "脊索动物", "软体动物", "节肢动物", "腔肠动物",
  "哺乳", "爬行", "两栖", "鱼类",
  // === 平台 ===
  "观察", "识别", "答题", "百科", "收集", "拍照识别",
].sort((a, b) => b.length - a.length); // 长词优先，避免"海豚"吃掉"中华白海豚"

/**
 * 判断是否为纯中文/中英混合的有效短语
 */
const isValidPhrase = (s) => /^[一-鿿_a-zA-Z0-9]{2,8}$/.test(s);

/**
 * 从完整提问文本中提取关键词列表
 */
const extractKeywords = (text) => {
  const found = [];
  let remaining = text;

  // 1. 按长度降序匹配领域关键词库（长词优先）
  for (const kw of MARINE_KEYWORDS) {
    const idx = remaining.indexOf(kw);
    if (idx !== -1) {
      found.push(kw);
      // 用占位符替换，防止短词重复匹配（如"海豚"二次匹配"中华白海豚"）
      remaining = remaining.slice(0, idx) + "▨".repeat(kw.length) + remaining.slice(idx + kw.length);
    }
  }

  // 2. 剩余文本切分后提取 2-8 字有效短语作为补充
  const fragments = remaining.split(/[▨，,。！？、\s%]+/).filter(s => s.length >= 2 && s.length <= 8);
  for (const frag of fragments) {
    if (isValidPhrase(frag) && !STOP_WORDS.has(frag)) {
      found.push(frag);
    }
  }

  return found;
};

/**
 * 过滤无效提问 + 关键词提取 + 聚合
 * 输入：后端返回的 [{name: "完整提问", value: 次数}, ...]
 * 输出：词云可用数据 [{name: "关键词", value: 热度}, ...]
 */
const buildWordCloudData = (rawData) => {
  // Step 1: 过滤明显的无效提问
  const validItems = rawData.filter(item => {
    const name = item.name || "";
    const hitStop = STOP_QUESTIONS.some(kw => name.includes(kw));
    return !hitStop && name.length >= 4;
  });

  // Step 2: 逐条提取关键词，权重 = 原文出现次数
  const kwCountMap = {};
  for (const item of validItems) {
    const keywords = extractKeywords(item.name);
    if (keywords.length === 0) continue;
    const weight = item.value || 1;
    for (const kw of keywords) {
      kwCountMap[kw] = (kwCountMap[kw] || 0) + weight;
    }
  }

  // Step 3: 转换为 ECharts 词云格式并排���
  const result = Object.entries(kwCountMap)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);

  return result;
};

// ============ 加载 AI 提问词云 (Word Cloud) ============

const loadAiScene = async () => {
  try {
    const resp = await getAiWordCloud();
    // 安全剥离数据层
    const rawData = extractList(resp?.data?.data || resp?.data || resp);
    // 关键词提取 + 聚合
    const data = buildWordCloudData(rawData);

    if (!aiSceneRef.value) return;
    if (!aiSceneChart) aiSceneChart = echarts.init(aiSceneRef.value);

    aiSceneChart.setOption({
      tooltip: {
        show: true,
        backgroundColor: "rgba(255, 255, 255, 0.9)",
        borderColor: "#165DFF",
        formatter: "{b}<br/>热度: {c} 次",
      },
      series: [{
        type: "wordCloud",
        shape: "circle",
        keepAspect: false,
        left: "center",
        top: "center",
        width: "95%",
        height: "95%",
        sizeRange: [14, 60],
        rotationRange: [0, 0],
        rotationStep: 0,
        gridSize: 6,
        drawOutOfBound: false,
        layoutAnimation: true,
        textStyle: {

          color() {
            const colors = [
              "#165DFF", "#36CFC9", "#722ED1", "#14C9C9",'#13C2C2', '#08979C', '#531DAB',
              "#4080FF", "#0E42D2", "#8D4EDA",'#0E42D2', '#2F54EB', '#1D39C4',
            ];
            return colors[Math.floor(Math.random() * colors.length)];
          },
        },
        emphasis: {
          focus: "self",
          textStyle: {
            textShadowBlur: 8 ,
            textShadowColor: "rgba(22, 93, 255, 0.3)",
            color: "#165DFF",
            fontWeight: "bolder",
          },
        },
        data: data.length > 0 ? data : [
          { name: "暂无提问", value: 100 },
        ],
      }],
    });
  } catch (e) {
    console.error("加载词云数据失败", e);
  }
};

// 保留原有的百科物种饼图渲染逻辑
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
    data = extractList(resp?.data || resp);
  } catch {
    const sp = await getSpeciesRawList();
    data = aggregateByField(extractList(sp?.data || sp), "conservationStatus");
  }
  pieChart = renderPie(pieRef, pieChart, data);
};

const loadTaxonomy = async () => {
  let data;
  try {
    const resp = await getSpeciesTaxonomyStats(taxonomyLevel.value);
    data = extractList(resp?.data || resp);
  } catch { data = [] }
  taxChart = renderPie(taxRef, taxChart, data);
};

const loadEcosystemStats = async () => {
  let data;
  try {
    const resp = await getEcosystemStats();
    const rawList = extractList(resp?.data || resp);
    // 根据 typicalSpecies 逗号分隔计算每类生态系统的关联物种数量
    data = rawList.map(item => ({
      name: item.name,
      value: (item.typicalSpecies || "").split(/[,，]/).filter(Boolean).length || 1,
    }));
  } catch (e) {
    console.error("加载生态系统统计失败", e);
    data = [];
  }
  ecosystemChart = renderPie(ecosystemRef, ecosystemChart, data);
};


// ============ 导出 PDF (复用原有逻辑，增加图表映射) ============

const exportChartPDF = async (chartKey, title) => {
  const chartRefs = {
    pie: pieRef,
    tax: taxRef,
    ecosystem: ecosystemRef,
    trend: trendRef,
    ai: aiSceneRef
  };
  const ref = chartRefs[chartKey];
  if (!ref?.value) { ElMessage.warning("图表未加载完成"); return; }

  try {
    const cardEl = ref.value.closest(".el-card");
    if (!cardEl) { ElMessage.warning("未找到图表容器"); return; }

    // 隐藏按钮和下拉框避免导出到截图里
    const btns = cardEl.querySelectorAll(".el-button--small, .el-select");
    btns.forEach(b => { b.style.visibility = "hidden" });

    const canvas = await html2canvas(cardEl, { backgroundColor: "#fff", scale: 2, useCORS: true });

    btns.forEach(b => { b.style.visibility = "" }); // 恢复显示

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
    pdf.save(`${title}_${dayjs().format("YYYYMMDD")}.pdf`);
    ElMessage.success("报表已导出");
  } catch (e) {
    ElMessage.error("导出失败");
    console.error(e);
  }
};

// ============ 生命周期与自适应 ============

const handleResize = () => {
  [trendChart, aiSceneChart, pieChart, taxChart, ecosystemChart].forEach(c => c?.resize());
};

const initCharts = async () => {
  loading.value = true;
  try {
    // 并发请求所有数据
    await Promise.all([
      loadKpi(),
      loadTrend(),
      loadAiScene(),
      loadProtection(),
      loadTaxonomy(),
      loadEcosystemStats()
    ]);
    await nextTick();
    handleResize();
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
  [trendChart, aiSceneChart, pieChart, taxChart, ecosystemChart].forEach(c => c?.dispose());
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
  border-radius: 8px;
}

.summary-card :deep(.el-card__body) {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.summary-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 26px;
  flex: 0 0 auto;
}

/* 颜色配置，匹配科技教育风 */
.summary-icon.blue { background: linear-gradient(135deg, #4080FF, #165DFF); }
.summary-icon.teal { background: linear-gradient(135deg, #36CFC9, #14C9C9); }
.summary-icon.orange { background: linear-gradient(135deg, #FF9A2E, #FF7D00); }
.summary-icon.purple { background: linear-gradient(135deg, #8D4EDA, #722ED1); }

.summary-content {
  min-width: 0;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  line-height: 1.2;
}

.summary-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.chart-box {
  height: 320px;
}
</style>
