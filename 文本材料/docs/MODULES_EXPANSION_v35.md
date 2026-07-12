# 各模块扩充 v35

> 汇编 §18 技能树 · §13 昆吾秘境四要素 · §9 大晋世家/拍卖

## 技能

| 文件 | 变更 |
|------|------|
| `techniques/talisman.json` v2 | 天符灵经门控 + 缚灵/金刚/雷符暴雨 |
| `techniques/fashi.json` | 慕兰/天澜 `requires_method` |
| `techniques/dao.json` v2 | 清虚玄功门控 |
| `skill_trees.json` v2 | +7 树（丹道、大衍、化刀、烈焰、灵兽、星宫、逆星）；天符/慕兰细化 |

术法总数见 `techniques/index.json`。

## 大晋 / 昆吾

| 文件 | 变更 |
|------|------|
| `region_cards/dajin.json` | 拍卖、万宝阁、昆吾秘境 |
| `region_cards/kunwu.json` | 雪峰、封印、结丹+ |
| `secret_realm_template.json` | `kunwu_mountain` 四要素 |
| `quest_chains.json` | `dajin_kunwu_line` |
| `quest_hooks.json` | 昆吾情报/远征/封印研究/拍卖/世家巡护 |
| `cultivation_methods.json` | 世家祖传诀、昆吾寒罡诀 |
| `daily_random_events.json` | 世家械斗、昆吾寒潮 |

## Patchouli

`patchouli_item_book.json` v4：技能树、鬼修、乱星海、大晋昆吾、天符/青元树条目名

## 规模

- 技能树 **11**
- 任务链 **+1**（大晋昆吾）
- 汇编合规：结构门槛，古宝用碎片/仿名

## 下一步

- `techniques/elemental.json` 按灵根 `element_required` 门控
- 坠魔谷 / 古魔线联动 `demon_rift` 维度
- Patchouli 实际 `.json` 页面文件（若模组已接 Patchouli 资源目录）