# 物品扩充 v37（天渊城 · 逆星黑市 · Patchouli）

## 汇编 §9.7

- **天渊城**：边境功勋令、魔潮侦察简、驻军营粮；功勋殿扩兑驱魔丹、护劫丹、渡劫辅助、边境令
- **修炼环境**：`faction_species#tianyuan_city` 灵气×2，物品不硬编数值
- **逆星盟**：黑市补暗语牌/走私包；`inverse_star_smuggler_loot` 脏灵石、失窃玉简、结丹方概率

## 掉落

| 表 | 用途 |
|----|------|
| `tianyuan_patrol_chest` | 诛魔契约/巡防奖励 |
| `inverse_star_smuggler_loot` | 走私包开启 |

## Patchouli

- `patchouli_item_book.json` v2：区域特产、势力坊市、扩充索引静态页
- `generate_patchouli_items_chapter.py`：consumables 章节、每类上限 80

## 脚本

`scripts/expand_v37.py`