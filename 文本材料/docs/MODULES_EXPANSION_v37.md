# 各模块扩充 v37

> 汇编 §18 宗门专精标签 · 丹炉炉+盖+火 · 慕兰法士灵术种族

## 黄枫谷丹道闭环

| 环节 | 数据 |
|------|------|
| 功法 | 长春功 + **黄枫丹经**（丹童，不替代主修） |
| 任务链 | `huangfeng_cultivation_path` → `alchemy_loop_ref` |
| 丹方 | `alchemy_recipes.json#huangfeng_loop`：辟谷/筑基配额、地火室解锁 |
| 功勋 | `sect_contribution_shop` 黄枫 `alchemy_tier_unlock` |
| 技能树 | `alchemy_huangfeng`：丹方 tier、地火、`craft_ref` |
| 任务 | 内门晋升、丹童线已有 hook |

炸炉规则指向 `pill_furnace.json`（结构惩罚，非随机成功率表）。

## 慕兰法士

| 文件 | 内容 |
|------|------|
| `playable_races.json` v2 | 默认 `mulan_wind_spirit_art`；人族需 `mulan_side` 转修 |
| `techniques/fashi.json` v3 | 慕兰术 `race_required` + `race_override_quest` |
| `skill_trees.json` v4 | `fashi_mulan` 种族门控 |
| 任务 | 灵石充能操演 |
| 商店 | 已有 `mulan_fashi_supply` |

天澜术挂 **天澜圣兽诀** + `tianlan_temple` 势力。

## 天南宗门专精

| 宗门 | 原型 | 功法/树 |
|------|------|---------|
| 掩月宗 | 幻阵 | 掩月幻心诀 → illusion |
| 清虚门 | 制符 | 天符灵经 |
| 灵兽山 | 御兽 | 灵兽御兽诀 → beast_lingshou |
| 黄枫谷 | 炼丹 | alchemy_huangfeng |

`region_cards/tiannan.json` 汇总势力与任务链。

## 任务钩 +5

掩月阵试、清虚符考、灵兽契约、黄枫内门、慕兰操演。

## 下一步

- `illusion.json` 挂 `yanyue_illusion_art`
- 七玄门 / 化刀坞入门链
- 打包 v37 zip