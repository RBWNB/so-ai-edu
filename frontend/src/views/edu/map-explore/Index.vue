<template>
  <div class="map-page">
    <div class="map-toolbar">
      <div class="toolbar-left">
        <span style="color:#1677ff;font-weight:500">物种地理分布</span>
        <span style="margin-left:12px;color:#909399">
          已加载 {{ speciesCount }} 个物种记录
        </span>
      </div>
      <el-button size="small" @click="refreshData">刷新</el-button>
    </div>
    <div ref="mapRef" class="map-container"></div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from "vue";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import markerIcon2x from "leaflet/dist/images/marker-icon-2x.png";
import markerIcon from "leaflet/dist/images/marker-icon.png";
import markerShadow from "leaflet/dist/images/marker-shadow.png";
import { getSpeciesRawList } from "@/api/visual";

L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
});

const mapRef = ref(null);
const speciesCount = ref(0);

let map = null;

const initMap = () => {
  if (!mapRef.value || map) return;
  map = L.map(mapRef.value, { zoomControl: true }).setView([20.5, 112.5], 5);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors",
  }).addTo(map);
};

const loadSpeciesData = async () => {
  try {
    const resp = await getSpeciesRawList({ pageNum: 1, pageSize: 500 });
    const records = resp.data?.records || resp.data || [];
    speciesCount.value = records.length;
  } catch {
    speciesCount.value = 0;
  }
};

const refreshData = async () => {
  await loadSpeciesData();
};

const handleResize = () => map?.invalidateSize();

onMounted(async () => {
  initMap();
  window.addEventListener("resize", handleResize);
  await refreshData();
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
  if (map) { map.remove(); map = null }
});
</script>

<style scoped>
.map-page {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.map-toolbar {
  height: 48px;
  padding: 0 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.map-container {
  flex: 1;
  min-height: 420px;
}
</style>
