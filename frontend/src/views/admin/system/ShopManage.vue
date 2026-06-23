<template>
  <div class="shop-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>积分商店管理</span>
          <el-button type="primary" size="default" @click="openAddDialog">新增商品</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" border>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ typeLabel(row.itemType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pointsPrice" label="价格(积分)" width="110" align="center" />
        <el-table-column label="库存" width="90" align="center">
          <template #default="{ row }">
            {{ row.stock != null ? row.stock : '不限' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              size="small"
              @change="(val) => toggleStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" plain size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" @click="adjustStock(row)">补货</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑/新增弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'add' ? '新增商品' : '编辑商品'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="dialog.form" :rules="rules" label-position="top">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="dialog.form.name" placeholder="例如：积分盲盒" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="dialog.form.description" type="textarea" :rows="2" placeholder="商品描述" />
        </el-form-item>
        <el-form-item label="类型" prop="itemType">
          <el-select v-model="dialog.form.itemType" style="width: 100%">
            <el-option label="🎁 虚拟物品" value="virtual_item" />
            <el-option label="🎫 优惠券/卡" value="coupon" />
            <el-option label="🏅 徽章" value="badge" />
            <el-option label="🖼️ 头像框" value="avatar_frame" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="积分价格" prop="pointsPrice">
              <el-input-number v-model="dialog.form.pointsPrice" :min="1" :max="99999" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存（空=不限）" prop="stock">
              <el-input-number v-model="dialog.form.stock" :min="0" :max="99999" style="width: 100%" placeholder="不限" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ dialog.mode === 'add' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 补货弹窗 -->
    <el-dialog v-model="stockDialog.visible" title="调整库存" width="320px">
      <el-form label-position="top">
        <el-form-item label="当前库存">
          <span>{{ stockDialog.currentStock != null ? stockDialog.currentStock : '不限量' }}</span>
        </el-form-item>
        <el-form-item label="新库存（空=不限量）">
          <el-input-number v-model="stockDialog.newStock" :min="0" :max="99999" style="width: 100%" placeholder="不限量" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="stockSubmitting" @click="submitStock">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { adminListItems, adminUpdateItem, adminAddItem } from "@/api/pointsAdmin";

const loading = ref(false);
const submitting = ref(false);
const stockSubmitting = ref(false);
const tableData = ref([]);
const formRef = ref(null);

const rules = {
  name: [{ required: true, message: "商品名称不能为空", trigger: "blur" }],
  pointsPrice: [{ required: true, message: "积分价格不能为空", trigger: "blur" }],
  itemType: [{ required: true, message: "请选择类型", trigger: "change" }],
};

const typeLabel = (t) => ({ virtual_item: "虚拟物品", coupon: "优惠券", badge: "勋章", avatar_frame: "头像框" }[t] || t);

// 编辑/新增弹窗
const dialog = reactive({
  visible: false,
  mode: "add",
  form: { name: "", description: "", itemType: "virtual_item", pointsPrice: 1, stock: null },
});

const resetForm = () => {
  Object.assign(dialog.form, { name: "", description: "", itemType: "virtual_item", pointsPrice: 1, stock: null });
};

const openAddDialog = () => {
  dialog.mode = "add";
  resetForm();
  dialog.visible = true;
};

const openEditDialog = (row) => {
  dialog.mode = "edit";
  Object.assign(dialog.form, {
    id: row.id,
    name: row.name,
    description: row.description || "",
    itemType: row.itemType,
    pointsPrice: row.pointsPrice,
    stock: row.stock,
  });
  dialog.visible = true;
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const body = { ...dialog.form };
      if (body.stock === undefined || body.stock === "") body.stock = null;
      if (dialog.mode === "add") {
        await adminAddItem(body);
        ElMessage.success("商品已创建");
      } else {
        await adminUpdateItem(body.id, body);
        ElMessage.success("商品已更新");
      }
      dialog.visible = false;
      fetchData();
    } catch (err) {
      ElMessage.error("操作失败");
    } finally {
      submitting.value = false;
    }
  });
};

// 补货弹窗
const stockDialog = reactive({
  visible: false,
  itemId: null,
  currentStock: null,
  newStock: null,
});

const adjustStock = (row) => {
  stockDialog.itemId = row.id;
  stockDialog.currentStock = row.stock;
  stockDialog.newStock = row.stock;
  stockDialog.visible = true;
};

const submitStock = async () => {
  stockSubmitting.value = true;
  try {
    await adminUpdateItem(stockDialog.itemId, { stock: stockDialog.newStock });
    ElMessage.success("库存已更新");
    stockDialog.visible = false;
    fetchData();
  } catch (err) {
    ElMessage.error("更新失败");
  } finally {
    stockSubmitting.value = false;
  }
};

const toggleStatus = async (row, val) => {
  try {
    await adminUpdateItem(row.id, { status: val });
    ElMessage.success(val === 1 ? "已上架" : "已下架");
    fetchData();
  } catch (err) {
    ElMessage.error("操作失败");
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await adminListItems();
    tableData.value = res.data.data ?? [];
  } catch (err) {
    ElMessage.error("加载商品列表失败");
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
