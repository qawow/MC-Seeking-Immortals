# 乱星海设定卡（扩展）

> 数据：`region_cards/chaotic_sea.json` · `chaotic_sea_factions.json` · `economy_tiers.json`

## 社会结构

百姓向岛主/星宫纳 **灵石** 换庇护；与天南「修仙与世俗脱节」不同。

## 双雄

| 势力 | 定位 | 玩家 |
|---|---|---|
| **星宫** | 内海秩序、双圣、执法巡防 | 筑基可入，补天丹配额、传送许可 |
| **逆星盟** | 割据、黑市、走私 | 需接触任务，星宫声望受限 |

关系：战争 -80，走私贸易 +10（`faction_graph`）。

## 三大天灾

妖兽潮、天风、鬼雾 — 对应 `spawn_tables` / `disasters`。

## 经济

- 拍卖：补天丹、高阶材料  
- 逆星盟黑市：赃物、妖丹  
- 岛间渡船：灵石计费（`flight_vehicles`）

## 秘境

**虚天殿** 三百年一现（`cycle_void_palace`），密钥见 `artifacts_catalog.void_key`。