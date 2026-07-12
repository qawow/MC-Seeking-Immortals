# 物品扩充 v16（2026-07-03）

> 手册/货币结构化 + 炼器配方扩至 20 + Patchouli 骨架 + 丹药/联动补全。

## 新增文件

| 文件 | 作用 |
|------|------|
| `manuals_catalog.json` | 15 种可学习手册（炼器/傀儡/鬼修/丹方/阵法/任务玉简） |
| `currency_items.json` | 灵石四级 + 阴石 + 贡献/功勋/战勋符 |
| `patchouli_item_book.json` | 《修仙物品总览》分类与 data 引用骨架 |

## 扩充

- **炼器配方** 10 → **20**（引魂钟、聚魂钵、婆罗珠、蛇珠、破天锹、磐石盾、无双飞刀、平山冠仿、邪幻镜、巨猿傀儡符）
- **丹药** +解毒丹、护灵丹、驭兽丹；**丹方** +3
- **材料** +鬼修录、法阵要诀、试炼功勋、灰岛地契、灵界通行符
- **item_synergy** +6 条联动

## 汇编 §18 对齐

- **配方书/玉简** → `manuals_catalog` + `consume_action`
- **灵石四级阶梯** → `currency_items` 兑换链
- **法宝十一级结构** → 仍以 `artifact_tier_rules` 五档 + 炼器方覆盖主流可制法宝

## 模组

- 从 `patchouli_item_book.json` 生成 Patchouli book JSON（可脚本扫 `*_catalog.json`）
- 手册物品实现 `LearnManualItem` 写玩家 NBT 已学 recipe id

## v17 候选

- `items_by_region.json` 与新区块生物群系 id 对齐
- 符纸/丹炉/炼器台方块物品化 `block_items.json`
- 全 id 导出 CSV 供策划表