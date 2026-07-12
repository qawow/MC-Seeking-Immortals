# 物品扩充 v14（2026-07-03）

> 延续 v13：符箓/炼器/阵盘 **配方表** + 经济上架 + 灵草材料补全。

## 新增数据文件

| 文件 | 内容 |
|------|------|
| `talisman_recipes.json` | 10 条制符配方（符纸+墨+辅料，对齐 paper_grade / ink） |
| `refinement_recipes.json` | 10 条炼器配方（低阶飞剑～玄光镜残片重铸） |
| `formation_items_catalog.json` | 便携阵盘/阵旗/传送核心等 7 种 |

## 扩充目录

- **灵草** +5：灵菇、昆吾冰莲、紫芝、血莲  
- **材料** +8：玄光镜残片、炼器手册、三种符墨、妖丹碎片、珊瑚原珠  
- **消耗品** +5：空白玉简/纸方、灵舟票、冥河渡票、灵石袋  
- **符箓** +3：风刃符、土墙符、缚灵符；目录增加 `recipes_ref`  
- **坊市** 天南灵草摊：丹药/符/材料扩列  
- **贡献堂** 黄枫谷：辟谷方、清虚方、回灵丹、炼器入门篇  

## 汇编对齐说明

- 符箓：**初/中/高 × 纸阶** → `paper_grades` + `realm_to_max_grade`  
- 炼器：**法器→法宝→古宝** → `refinement_recipes` + `artifact_tier_rules`  
- 傀儡联动：混元钵配方标 `puppet_core`  
- 古宝：玄光镜用 **残片×3** 重铸，非批量产出  

## 模组下一波

1. 制符台 + `talisman_recipes` 导入  
2. 炼器炉/炼器阵 + `refinement_recipes`  
3. 便携阵盘右键放置，扣 `uses`  
4. 商人 GUI 读 `merchant_shops` 新 stock  

## v15 候选

- `talisman_recipes` 补全剩余 9 张符  
- Boss 掉落表对齐新法宝/材料 id  
- `boss_loot_tables` + 新丹药成品掉落权重