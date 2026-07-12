# 设定扩充 v89（妖兽十三级 / 天劫叙事 / 乱星海）

## 妖兽十三级 ↔ 秘境 Boss

新表 **`beast_boss_tier_secret_realm_map.json`**：

- 将 `secret_realms.json` 中主要 **Boss** 标到 **典型妖兽阶位（1–13）** 与 **loot_band（1-4 / 5-8 / 9-13）**
- **五阶起出妖丹**、**七阶化形小劫** 规则见 `beast_tier_schema.json`
- 傀儡/鬼修类单独标 `category` 或 `equivalent_threat`，避免硬套妖丹逻辑

完整十三阶描述仍在 **`beast_thirteen_tier_map.json`** + **`beast_bestiary.json`**。

## 天劫小 / 大 / 仙（叙事层）

新表 **`tribulation_narrative_lore.json`**（不改 `tribulation_rules.json` 波数）：

| 等级 | 原著向要点 |
|------|------------|
| **小劫** | 元婴小境界、七阶妖兽化形；败多跌境重伤 |
| **大劫** | 化神心魔/雷、炼虚雷、合体双劫、大乘天劫 |
| **仙劫** | 飞升与飞升前兆；露天、禁秘境幻阵渡劫 |

备劫物品标签对齐 **`tribulation_items.json`**；环境见 rules 中 `environment` / `realm_context_modifiers`。

## 乱星海势力

- **`chaotic_sea_lore_compendium.json`**：内外海、深渊、虚天殿筹码；星宫 / 逆星 / 商盟 / 蓬莱 / 外海魔宫 lore 与三条玩家路径。
- **`chaotic_sea_faction_quests.json`**：补四个 **hub**（登记处、黑市、商盟码头、深渊前哨），链到既有 war phases 与 `chaotic_sea_politics` 任务链。

## 说明

仅 JSON 设定与文档，无模组代码。v88 秘境/符箓/七派见 **`LORE_EXPANSION_v88.md`**。