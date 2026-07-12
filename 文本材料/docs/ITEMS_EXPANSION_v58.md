# 原著物品「全量」扩充 v58

> 汇编 **§18**：保留**结构**（灵石四级、丹药品质、符箓三阶、法宝十一级、妖兽十三级材料），通用名词可复用；主角强绑定**化名**。

## 「全部」在本项目中的含义

原著正文物品**成千上万**且大量只出现一次，无法在 JSON 里逐字穷尽。v58 采用**两层全覆盖**：

| 层 | 内容 |
|----|------|
| **A. 具名种子** | `novel_items_waves.json`：丹/草/材/宝/符/消 **原著通用名** 批量合并（约 260+ 条种子，去重入 catalog） |
| **B. 结构占位** | 13 阶妖兽材料、11 阶炼器胚/通称法宝、8 境辅修丹、10 地区特产灵草 — **体系无缺口** |

二者合并后，模组侧可用 **id 索引 + 结构 tier** 表达「原著里任何同类物品」，具体掉落再指向具名或占位 id。

## 合规

- 筑基丹、灵石、符箓、飞剑、妖丹等：**直接 display**
- 掌天瓶、青竹蜂云剑、虚天鼎、山河珠等：**仿/碎片/化名**（见 artifacts `note`）

## 脚本（可重复跑，仅补缺 id）

1. `expand_items_novel_complete.py` — v57 首批  
2. `expand_items_novel_all.py` — **waves + 结构层**（推荐）

## 索引

- `novel_items_master_index.json` v2 — `full_merge_v58` 统计  
- `item_id_index.json` v4 — 各 catalog 计数  

## 打包

**`seeking_immortals_items_v58_full.zip`**

## 若仍要「更多具名」

在 `novel_items_waves.json` 末尾追加 `{id, display, ...}` 后重跑 `expand_items_novel_all.py` 即可；无需改 catalog 手工维护。