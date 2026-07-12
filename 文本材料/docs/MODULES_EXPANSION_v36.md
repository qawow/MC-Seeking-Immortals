# 各模块扩充 v36

> 汇编 **§18** 灵根→属性法术 · **§13** 血色/坠魔谷四要素 · 古魔→`demon_rift` 维度

## 灵根与五行术法

| 文件 | 内容 |
|------|------|
| `spirit_roots.json` | 单/双/伪/天/变异灵根；`element_to_methods` |
| `techniques/elemental.json` v2 | 全术 `element_required`；火系高阶挂 `lieyan_gong` |
| `skill_trees.json` v3 | `elemental_five` 五行基础树 |
| `techniques/index.json` | `spirit_roots_ref` |

伪灵根：可学法术，修炼减速在 `cultivation_progression` 侧处理，非成功率表。

## 坠魔谷 / 古魔 / 血色

| 文件 | 内容 |
|------|------|
| `region_cards/fallen_demon_valley.json` | 魔气、心魔、联动 `demon_rift` |
| `secret_realm_template.json` | 坠魔谷 + **血色禁地** 四要素 |
| `dimensions_catalog.json` | 裂隙←坠魔谷秘境；封印事件时长 |
| `quest_chains.json` | `ancient_demon_line`（侦察→封印研究→可选血色→稳固裂隙） |
| `faction_conflict_events.json` | `ancient_demon_seal_breach` 世界事件 |
| `region_cards/tiannan.json` / `mulan.json` | 天南、慕兰草原势力骨架 |

## 阵法 / 杂学

- `array.json` v2：筑基+ 阵术挂 `kunwu_seal_art`（昆吾阵道向）
- `misc.json` v2：`tier` 标注

## 规模

术法见 `index.json`；任务链 **9**；势力冲突含古魔松动事件。

## 下一步

- `playable_races.json` 与慕兰法士 `fashi` 树联动校验
- 黄枫谷 `huangfeng_cultivation_path` 与 `alchemy` 树物品闭环
- 打包 v36 zip