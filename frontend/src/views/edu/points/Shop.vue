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
            :disabled="availablePoints < item.pointsPrice"
            :loading="exchangingId === item.id"
            @click="handleExchange(item)"
          >
            {{ availablePoints < item.pointsPrice ? '积分不足' : '立即兑换' }}
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
  max-width: 960px;
  margin: 0 auto;
  padding: 8px 0;
}

.shop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.shop-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--theme-text-primary);
  margin: 0;
}

.header-balance {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border-radius: 24px;
  background: var(--theme-primary-soft);
}

.balance-label {
  font-size: 13px;
  color: var(--theme-text-muted);
}

.balance-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--theme-klein-blue);
}

.shop-grid {
  row-gap: 16px;
  min-height: 200px;
}

.shop-card {
  border: 1px solid var(--theme-border-light);
  border-radius: 14px;
  padding: 24px 20px;
  text-align: center;
  background: var(--theme-card-bg);
  transition: box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.shop-card:hover {
  box-shadow: var(--theme-shadow);
}

.item-icon {
  font-size: 40px;
  line-height: 1;
}

.item-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--theme-text-primary);
}

.item-desc {
  font-size: 13px;
  color: var(--theme-text-muted);
  line-height: 1.5;
  min-height: 36px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.item-price {
  color: var(--theme-coral);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 3px;
}

.item-stock {
  color: var(--theme-text-muted);
}

.item-stock.unlimited {
  color: var(--theme-success);
}

.exchange-btn {
  width: 100%;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .shop-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
