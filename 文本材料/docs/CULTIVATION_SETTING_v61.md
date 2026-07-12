# 功法·法术「设定」扩充 v61

在 v60 **具体内容**（`description` / `effect` / 消耗）之上，为**全部**条目挂载 **`setting`** 块，对齐汇编 **§18 三层技能树** 与 **§9 宗门专精标签**。

## `setting` 字段（功法）

| 键 | 含义 |
|----|------|
| `cultivation_type` | `qi` / `body` / `fashi` / `ghost` / `demon` |
| `sect_specialty` | 宗门功能标签：sword / alchemy / talisman / formation_seal / poison / dual_charm … |
| `spirit_root_gate` | 灵根硬性/加成（`required_any` / `bonus` / `forbidden`） |
| `breakthrough` | 突破与转修说明（结构向，非精确概率） |
| `combat_style` | 战斗定位 |
| `faction` / `region` | 势力与地图 |
| `karma` / `reputation_gate` | 魔道、声望门控 |
| `layers_max` | 炼气层数等（如长春功 13 层） |

**87** 条功法均有 `setting`；**27** 条为 `novel_cultivation_setting_waves.json` 详述（黄枫、坤吴、魔道六宗、乱星海、慕兰、灵界等）。

## `setting` 字段（法术/神通）

| 键 | 含义 |
|----|------|
| `cast_type` | instant / sustained / ritual |
| `target` | single / area / self / battlefield / summon |
| `range` | short / medium / long / dash |
| `lore` | 原著向施法设定一句 |
| `counter` / `synergy` / `limitations` | 克制、连招、禁制 |
| `region_bonus` / `needs_artifact` / `needs_item` | 环境、飞剑、尸材等 |
| `compliance` | 化名神通标注 |

**293** 条术法均有 `setting`；**16** 条关键术有定制 `technique_setting`；其余按 **流派默认**（`school_default_setting`）生成后再与条目合并。

## 新增功法（修引用）

- **`kunwu_seal_art` 坤吴印诀** — 修复 `formation.json` 中 `requires_method: kunwu_seal_art` 无对应功法的问题  
- 灵武门、幼童门、皇金宗、太一门等天南/大晋小派占位

## 脚本链

1. `expand_cultivation_novel_all.py` — 名录  
2. `enrich_cultivation_content.py` — 描述与 effect  
3. **`enrich_cultivation_setting.py`** — **设定块**（v61）

编辑 **`novel_cultivation_setting_waves.json`** 后重跑 **3**。

## 打包

`seeking_immortals_cultivation_v61_setting.zip`