# 各模块扩充 v39

> 汇编 §18 功法→法术→神通 · §13 千竹塔秘境四要素 · §9 大晋世家

## 千竹教傀儡线

| 环节 | 数据 |
|------|------|
| 宗门 | `qianzhu_sect`（傀儡专精） |
| 功法 | 千竹傀儡术 → 大衍诀 → 大衍真解 |
| 任务链 | `qianzhu_puppet_path`（学徒→千竹塔→大衍残页→维护） |
| 秘境 | `thousand_bamboo_puppet_tower` 四要素 |
| 炼制 | `puppet_craft_recipes.json#faction_loops` |
| 商店 | 千竹傀儡堂 |
| 年表 | `A3_dayan_sage` |

木人傀门槛：**千竹傀儡术** + 学徒任务；巨猿/巨龟：**大衍诀**。

## 御灵宗兽傀线

| 环节 | 数据 |
|------|------|
| 宗门 | `yuling_sect_secret` ↔ `spirit_beast_mountain` |
| 功法 | 御灵兽傀诀 |
| 任务链 | `yuling_puppet_path` |
| 法术 | 兽魂傀绑 |
| 解锁 | 灵兽山声望暗线 |

## 大晋世家

- 任务链 **`dajin_clan_line`**：巡护→拍卖→祖传试炼→（可选昆吾）
- 功法 **世家祖传诀**、**昆吾寒罡诀**
- `region_cards/dajin.json` v3：`clan_system`、双任务链

## 技能树

- `puppet_qianzhu`、`puppet_yuling`
- `puppet_dayan` 前置千竹术

## 汇编合规

傀儡炼制保留 `realm_min` 与功法门控；`base_success_rate` 标为模组参考，非原著铁律。

## 规模

任务链 **15**；术法见 `index.json`（傀儡术 +2）。

## 下一步

- 魔道六宗（鬼灵门等）专精标签
- 灵界天渊城 / 飞升线数据
- 打包 v39 zip