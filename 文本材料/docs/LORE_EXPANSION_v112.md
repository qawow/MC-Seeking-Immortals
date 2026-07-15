# 设定扩充 v112（原包目录全量详释对齐）

> 无代码。秘境已 19/19 后，本轮补 **原包 catalogs → 物品说明** 缺口。

## 对齐规模
| 目录 | 条数 | 详释 id 前缀 |
|------|------|----------------|
| pills_catalog | 114 | orig_pill_* |
| spirit_herbs_catalog | 79 | orig_herb_* |
| materials_catalog | 221 | orig_mat_* |
| artifacts_catalog | 217 | orig_art_* |
| consumables_catalog | 57 | orig_cons_* |
| manuals_catalog | 21 | orig_man_* |
| beast_bestiary | 42+5宠 | orig_beast_* / orig_pet_* |

映射表：`data/catalog_description_align_v112.json`

## 说明
- display 带「（原包详释）」避免与深挖重名混淆
- 稀缺/唯一/绿液/通天仍服从 CONSISTENCY_AUDIT
- 价带仍以 numeric_overview / economy 为准

总索引 schema **v21**。
