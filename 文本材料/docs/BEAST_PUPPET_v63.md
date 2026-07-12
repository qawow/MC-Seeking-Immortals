# 灵兽·傀儡 设定与学习限制 v63

对齐汇编：**灵宠 = 驯服/伙伴关系（精血认主）**；**傀儡 = 装备/合成产物（功法+图纸+材料）**，两套系统独立可并存。

## 灵兽（`beast_bestiary.json` v3）

每条妖兽/灵兽含：

| 块 | 内容 |
|----|------|
| `setting` | `role`、`combat_style`、`lore`、`habitat`、化形相关 |
| `learn_requirements` | **`tame_requirements`**（不可驯则 `tame_allowed: false`） |

### 驯服 `tame_requirements` 常见键

| 键 | 原著向 |
|----|--------|
| `realm_min` | 主人境界 |
| `ritual` | `blood_contract` / `blood_contract_minor` / `queen_bond` / `soul_acknowledge` |
| `items` | 御兽环、镇兽链、诱饵、虫巢等 |
| `method_any` | **御兽基础诀**、高阶御兽诀 |
| `success_base` | 基础成功率（游戏化，非原著精确值） |
| `quests` / `cannot_tame_if` | 机缘、魔道因果 |
| `compliance` | 噬金虫、啼魂兽等 **（仿）** |

**42** 条生物（+10 新增：噬金虫、啼魂兽、三尾狐、云丝蛛等）；**可驯**按阶位默认规则，**8+** 条 waves 详述。

## 傀儡（`puppet_definitions.json` v2）

每条傀儡含：

| 块 | 内容 |
|----|------|
| `setting` | `control`（大衍神识丝/引魂钟/古阵）、`fuel`、`role`、`lore` |
| `learn_requirements` | **`craft_learn`**（制作/驱策资格） |

### 制作 `craft_learn` 常见键

| 键 | 原著向 |
|----|--------|
| `realm_min` | 与傀儡阶 T0–T6 对应 |
| `prerequisite_methods` | **千竹傀儡诀**（入门）、**大衍诀**（中高阶） |
| `items_required` / `quests` | 图纸、核心、混元钵、大衍残页 |
| `divine_sense_min` | 分神控傀 |
| `learn_source` | 与 `puppet_craft_recipes.json` 联动 |

**8** 条傀儡定义全部带 setting + learn；配方中的 `requires_method` / `quest_unlock` 会**回写**到 `craft_learn`。

## 新增功法

- **`qianzhu_puppet_art` 千竹傀儡诀** — 千竹教入门，任务 `qianzhu_puppet_apprentice`
- **`beast_taming_basic` 御兽基础诀** — 驯兽入门（若已存在则仅补 learn/setting）

## 脚本

```bash
python3 scripts/enrich_beast_puppet.py
```

编辑种子：**`novel_beast_puppet_waves.json`**

## 打包

`seeking_immortals_beast_puppet_v63.zip`