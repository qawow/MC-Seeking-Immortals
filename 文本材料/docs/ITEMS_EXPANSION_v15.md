# 物品扩充 v15（2026-07-03）

> 掉落闭环 + 符箓配方补全 + 傀儡部件 + 汇编向扩展。

## 本批变更

### Boss 掉落 v2
- `boss_loot_tables.json`：全部 **item id 对齐** 现有目录（龙血草、残片、丹方 scroll、成品丹/法宝）
- 新增 Boss：**乱星海蛟王**（蛟鳞、珊瑚珠、补天方/丹极低概率）
- 各秘境 Boss 可掉：**formula_scroll** 类型（对应 `alchemy_recipes` id）

### 妖兽通用掉落
- 新文件 `beast_loot_tiers.json`：妖兽 **1–4 / 5–8 / 9–13** 三档材料池

### 制符配方
- `talisman_recipes.json`：**10 → 19 条**（与 `talisman_catalog` 除任务专属外全覆盖）
- 含：锁魂、传送、御兽、定空、风刃、土墙、缚灵、三清雷霄、替身

### 傀儡
- `puppet_parts_catalog.json`：灵核、铁木机架、雷火珠、石灵核心、大衍残页、修缮包等 **8 件**

### 法宝 / 商店
- 法宝：+巨猿傀儡符、平山冠仿、灵舟模型（去重邪幻镜）
- 乱星海岛杂货：蛟鳞、寒铁、珊瑚原珠、回灵/聚魂丹、驱魔符、拍卖请柬

### 索引
- `item_id_index.json`：各目录 **条数统计 + 文件映射**，方便模组批量生成 lang

## 汇编结构对齐（§18）
- 妖兽 **十三阶** → 三档 loot tier（非逐阶 13 表，可 v16 细分）
- 傀儡 **铁木→巨猿→巨龟→石灵→混元** → parts + boss 掉 `ancient_puppet_method`
- 古宝 **残片重铸** → Boss/秘境掉 `xuanguang_mirror_shard`

## v16 建议
- `ghost_cultivation_manual` 等物品 id 写入 `materials_catalog`
- 炼器配方扩至 18 件法宝全覆盖
- Patchouli「物品总览」条目链 `item_id_index`