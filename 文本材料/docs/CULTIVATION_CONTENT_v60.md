# 功法·法术「具体内容」扩充 v60

在 v59 **名录全量** 基础上，为条目补齐**可进游戏/模组**的字段（汇编 §18：结构优先，数值为游戏量级）。

## 每条功法现在有什么

| 字段 | 说明 |
|------|------|
| `description` | 原著向说明：宗门、路线、门槛、专精 |
| `element` / `path` / `faction` / `race` | 灵根、道途、势力、种族门控 |
| `prerequisite_methods` / `unlocks_techniques_school` | 技能树前置与解锁流派 |
| `realm_min` / `realm_max` | 境界门槛 |

**82** 条功法均已带 `description`；其中 **37** 条在 `novel_cultivation_content_waves.json` → `method_content` 有**详述**（黄枫/星宫/慕兰/鬼灵/合欢/天魔/大晋佛修/灵界等）。

## 每条法术/神通现在有什么

| 字段 | 说明 |
|------|------|
| `description` | 施法效果与设定简述 |
| `spirit_cost_base` | 灵力消耗基准（见 `SPIRIT_COST_FORMULA.md`） |
| `effect` | `type` / `damage_base` / `element` / `tags` |
| `requires_method` | 对应功法门槛（按流派自动挂接） |
| `tier` | `spell` 或 `secret` |

当前 **~293** 条术法（去重后），缺字段的已批量补全；**30+** 条在 `detailed_spells` / `detailed_secrets` 有**定制 effect 与剧情描述**（如黄枫火蛇、镇海潮封、元磁神光、万剑归宗（仿）等）。

## 脚本

1. `expand_cultivation_novel_all.py` — 名录合并（v59）  
2. **`enrich_cultivation_content.py`** — **具体内容**（v60，可重复跑）

编辑 `novel_cultivation_content_waves.json` 的 `method_content` / `detailed_spells` 后重跑 **2** 即可。

## 合规

主角强绑定神通仍为 **（仿）** + `description` 说明；通用法术名与宗门常见术式直接书写。

## 打包

`seeking_immortals_cultivation_v60_content.zip`