# 物品扩充 v29（原著向 · 2026-07-03）

> 对齐汇编：**境界结构**、**宗门专精**、**秘境四要素**、通用丹药名（筑基丹/降尘丹等），避免主角专属古宝直搬。

## 丹药（天南炼气～筑基生态）

| id | 显示名 | 定位 |
|----|--------|------|
| jiangchen_pill | 降尘丹 | 弱于筑基丹的破境辅助 |
| huanglong_pill | 黄龙丹 | 炼气增修 |
| heqi_pill | 合气丹 | 炼气合气修炼 |
| ningshen_pill | 凝神丹 | 抗心魔 |
| yanghun_pill | 养魂丹 | 神魂温养 |

各配 `recipe_*`（纸方/玉简），黄枫谷、掩月宗、星宫贡献/坊市有售或兑换。

## 血色禁地

- 材料：**禁地血参**、**古修遗物**
- `loot_tables.blood_forbidden_chest`：血参、妖丹、灵石、降尘丹、低阶储物袋、降尘丹方
- `secret_realms.blood_forbidden` 扩展 `loot_tiers`

## 储物与坊市

- **低/中阶储物袋**、**封灵石袋**（consumables）
- 天南坊市：降尘/黄龙/合气丹、储物袋、丹方

## 宗门专精（数据层）

- **黄枫谷**：炼丹向 → 降尘/凝神方
- **掩月宗**：双修/幻术向 → 合气方、凝神丹
- **星宫**：乱星海秩序 → 养魂方（功勋）

## 规模（pipeline 后）

见 `item_id_index.json` counts；`MAINTENANCE_LOG` 记 v29。

## 法宝第二轮（2026-07-04 追加）

- `artifacts_catalog` **65 → 93**；炼器 **46** 条
- `talisman_treasure_templates.json`、`ancient_treasure_index.json`
- 详见 `docs/ARTIFACTS_FANREN.md`

## 未做（刻意）

- 掌天瓶、青竹蜂云剑等**主角绑定**古宝未作同名成品（仅灵木/碎片类材料向）。