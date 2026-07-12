# 经济设计说明

> 汇编 **§16** 物品价值仅为**量级灵感**，不同来源估算不一，**禁止**原样搬进游戏数值。

## 原则

1. 以 `economy_reference_magnitudes.json` 的 **band** 定档位，用 `mod_suggested_defaults` 作初值。  
2. 灵石兑换见 `economy_tiers.json`（下→中→上→极品，各 100:1）。  
3. 宗门贡献、岛税、黑市并行（乱星海见 `chaotic_sea_factions`）。  
4. 拍卖行：乱星海、大晋、天渊城 — 稀有丹与古宝走拍卖，日常走店铺。

## 与玩法挂钩

| 系统 | 消耗点 |
|---|---|
| 飞行 | 灵舟燃料、渡船费 |
| 阵法 | 灵石碎片、材料目录 |
| 突破 | 筑基丹、妖丹、灵草龄 |
| 傀儡 | 铁木、龟甲、灵石维修 |

## 重平衡流程

改 `mod_suggested_defaults` → 跑一周游戏内产出统计 → 调整 `spawn_tables` 掉落与任务 `quest_hooks` 奖励。

## 修炼消耗量级（相对）

`cultivation_progression.json` 的 `spirit_per_sub` 为相对单位，须与日产出联动调参。Boss 稀有掉落见 `boss_loot_tables.json`，概率宜低以防通胀。