# 物品扩充 v13（2026-07-03）

> 对齐汇编 **结构**（丹药分类、法宝阶位、地区特产），数值用 `economy_reference_magnitudes` 量级，非原著精确价。

## 本批变更

### 炼丹
- `alchemy_recipes.json` v2：**23 条丹方**（原 6 → 全覆盖目录丹 + 补天丹）
- 材料 id 与 `spirit_herbs_catalog` / `materials_catalog` **统一**（黄精、灵参、龙血草等）
- 字段：`ideal_fire_tier`、`requires_earth_fire_room`（与模组 0.1.74+ 一致）
- 新增 `pill_quality.json`：**成丹四档**（下/中/上/极）药效系数

### 丹药目录
- `pills_catalog.json` v2：+补天丹、培婴丹、回灵丹、淬体丹、镇魔丹（共 **23 种**）

### 材料
- `materials_catalog.json`：+千年灵芝、珊瑚灵珠、真灵血滴、灵铁、寒铁、金精石、蛟鳞、风灵草、血参（与 `items_by_region` 对齐）

### 法宝
- `artifacts_catalog.json`：+银月天狼、青凝镜、破天锹、蛇珠、无双飞刀、磐石盾、元婴符宝模板（共 **25 件**）

### 其它消耗
- 新文件 `consumables_catalog.json`：灵食、符墨、灵砂、避雷符、拍卖请柬等 **10 条**

### 联动
- `item_synergy.json`：补天丹、镇魔丹、回阳真水、青凝镜等

## 模组同步建议（下一波）

1. 从 `alchemy_recipes` 导入新配方 data + `requires_earth_fire_room`
2. 注册新丹药 / 材料 / 法宝 id + `zh_cn` lang
3. 成丹读 `pill_quality.json` 写 NBT
4. 培婴丹方：任务或年表解锁后再给玩家

## 仍未单独建表（可 v14）

- 炼器配方（仅材料用途标签）
- 符箓成品与 `talisman_catalog` 一一物品化
- `merchant_shops` 上架新丹药与材料