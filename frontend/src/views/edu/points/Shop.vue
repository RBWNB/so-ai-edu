<template>
  <div class="shop-container">
    <!-- 顶部：积分余额 -->
    <div class="shop-header">
      <div class="header-left">
        <h2 class="shop-title">🏪 积分商店</h2>
      </div>
      <div class="header-balance">
        <span class="balance-label">我的积分</span>
        <span class="balance-num">{{ availablePoints }}</span>
      </div>
    </div>

    <el-divider border-style="dashed" />

    <!-- 商品网格 -->
    <el-row :gutter="16" class="shop-grid" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="item in shopItems" :key="item.id">
        <div class="shop-card">
          <div class="item-icon">{{ typeIcon(item.itemType) }}</div>
          <div class="item-name">{{ item.name }}</div>
          <div class="item-desc">{{ item.description }}</div>
          <div class="item-meta">
            <span class="item-price">
              <el-icon :size="14"><Coin /></el-icon>
              {{ item.pointsPrice }} 积分
            </span>
            <span class="item-stock" v-if="item.stock !== null">
              库存 {{ item.stock }}
            </span>
            <span class="item-stock unlimited" v-else>不限量</span>
          </div>
          <el-button
              type="primary"
              size="default"
              class="exchange-btn"
              :disabled="item.stock === 0 || availablePoints < item.pointsPrice"
              :loading="exchangingId === item.id"
              @click="handleExchange(item)"
          >
            {{ item.stock === 0 ? '已售罄' : (availablePoints < item.pointsPrice ? '积分不足' : '立即兑换') }}
          </el-button>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && shopItems.length === 0" description="暂无商品" :image-size="100" />
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Coin } from "@element-plus/icons-vue";
import { getPointsAccount, getShopItems, exchangeItem } from "@/api/points";

const loading = ref(false);
const exchangingId = ref(null);
const availablePoints = ref(0);
const shopItems = ref([]);

const typeIcon = (type) => {
  const icons = {
    virtual_item: "🎁",
    coupon: "🎫",
    avatar_frame: "🖼️",
    badge: "🏅",
  };
  return icons[type] || "📦";
};

const fetchData = async () => {
  loading.value = true;
  try {
    const [accountRes, itemsRes] = await Promise.all([
      getPointsAccount(),
      getShopItems(),
    ]);
    availablePoints.value = accountRes.data.data?.availablePoints ?? 0;
    shopItems.value = itemsRes.data.data ?? [];
  } catch (err) {
    ElMessage.error("加载商店失败");
  } finally {
    loading.value = false;
  }
};

const handleExchange = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确认消耗 ${item.pointsPrice} 积分兑换「${item.name}」？`,
      "确认兑换",
      { confirmButtonText: "确认", cancelButtonText: "取消", type: "info" }
    );
  } catch {
    return; // 用户取消
  }

  exchangingId.value = item.id;
  try {
    const res = await exchangeItem(item.id);
    if (res.data.success) {
      ElMessage.success(res.data.message);
      fetchData(); // 刷新余额 + 商品库存
    } else {
      ElMessage.warning(res.data.message);
    }
  } catch (err) {
    ElMessage.error("兑换失败");
  } finally {
    exchangingId.value = null;
  }
};

onMounted(fetchData);
</script>

<style scoped>
.shop-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px 0 32px;
  color: #1d2129;
  animation: fadeIn 0.5s ease;
}

/* ── 顶部 Header 区域 ── */
.shop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 8px;
}

.shop-title {
  font-size: 24px;
  font-weight: 800;
  color: #1d2129;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

/* 余额悬浮胶囊 */
.header-balance {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 24px rgba(0, 50, 150, 0.05);
  transition: transform 0.3s ease;
}
.header-balance:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(22, 93, 255, 0.1);
}

.balance-label {
  font-size: 14px;
  font-weight: 500;
  color: #86909c;
}

/* 渐变高亮积分数字 */
.balance-num {
  font-size: 26px;
  font-weight: 800;
  line-height: 1;
  background: linear-gradient(135deg, #165dff 0%, #00d2ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 4px rgba(0, 210, 255, 0.2));
}

/* 虚线分割线 */
:deep(.el-divider--horizontal) {
  margin: 20px 0 24px;
  border-top: 1px dashed rgba(0, 0, 0, 0.08);
}


.shop-grid {
  row-gap: 20px;
  min-height: 200px;
}

/* 白透玻璃卡片 */
.shop-card {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  padding: 28px 20px 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
}

/* 悬浮光晕反馈 */
.shop-card:hover {
  transform: translateY(-6px);
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(22, 93, 255, 0.25);
  box-shadow:
      0 16px 32px rgba(22, 93, 255, 0.1),
      0 4px 12px rgba(0, 210, 255, 0.05);
}

/* ── 商品图标 (模拟圆形展台) ── */
.item-icon {
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38px;
  line-height: 1;
  background: linear-gradient(135deg, rgba(22, 93, 255, 0.05) 0%, rgba(0, 210, 255, 0.12) 100%);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  margin-bottom: 4px;
  box-shadow:
      inset 0 2px 4px rgba(255, 255, 255, 0.8),
      0 4px 12px rgba(0, 150, 255, 0.08);
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.shop-card:hover .item-icon {
  transform: scale(1.1) rotate(5deg);
}

/* 商品文字信息 */
.item-name {
  font-size: 17px;
  font-weight: 700;
  color: #1d2129;
  letter-spacing: 0.5px;
}
.item-desc {
  font-size: 13px;
  color: #86909c;
  line-height: 1.5;
  min-height: 39px; /* 保证两行高度对齐 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 价格与库存信息 */
.item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  margin-top: 4px;
  margin-bottom: 6px;
}
.item-price {
  color: #165dff;
  font-weight: 700;
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(22, 93, 255, 0.08);
  padding: 4px 10px;
  border-radius: 8px;
}
.item-stock {
  color: #86909c;
  font-weight: 500;
}
.item-stock.unlimited {
  color: #00b42a;
}

/* ══════════════════════════════════════════════════════════════════════
   兑换按钮 (液态胶囊)
   ══════════════════════════════════════════════════════════════════════ */
.exchange-btn {
  width: 100%;
  height: 40px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  border: none;
  transition: all 0.3s ease;
}

/* 可用状态：海蓝渐变 */
.exchange-btn:not(.is-disabled) {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(79, 172, 254, 0.3);
}
.exchange-btn:not(.is-disabled):hover {
  box-shadow: 0 6px 16px rgba(79, 172, 254, 0.5);
  transform: translateY(-2px);
}

/* 禁用状态：磨砂白玻璃 */
.exchange-btn.is-disabled {
  background: rgba(255, 255, 255, 0.4) !important;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
  color: #c9cdd4 !important;
  box-shadow: none !important;
  cursor: not-allowed;
}

/* 进入动画 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 响应式调整 */
@media (max-width: 768px) {
  .shop-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding: 0 8px;
  }
  .header-balance {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
