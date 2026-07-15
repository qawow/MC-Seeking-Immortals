# 设定三线同扩 v139

> 无代码。同时交付：秘境层示意、区域细化、对话分支。

## A. 秘境层内结构 + 刷怪 + 掉落
`secret_realm_layer_diagrams_v139.json`

| 秘境 | 层数 | 关键结构 |
|------|------|----------|
| 血色禁地 | 5（雾林→药田→墨蛟潭→争夺带→内圈） | 界碑、血池、试炼坛、出口阵 |
| 虚天殿 | 4（外殿→药苑→宝库→撤离） | 门残、机关廊、钥孔、主殿、鼎座 |
| 千竹机关塔 | 5 层叠塔 | 机关塔层、总控台、傀儡工坊 |

每层含：threat、structures、spawns、loot_tables、map_hint、boss（如有）

## B. 区域卡细化
`region_cards_detail_v139.json`
- 天南宗门驻地卡：**14**（七派+千竹/万妙/神兵/天星/合欢/鬼灵等）
- 乱星海岛卡：**7**（星宫本岛、坊市岛、逆星暗岛、妙音、灯塔链、深渊边、虚天集结）
- `region_cards_v138` 已挂 detail_ref

## C. 对话分支 + 声望条件
`npc_dialogue_branches_v139.json`：**12** 棵树  
条件：rep_gte/lt/hostile、令牌、境界、阵状态、任务旗等  
覆盖：贡献执事、传送看守、星宫、逆星、天渊、真言、轮回、天庭、秘境领队

## 交叉
- secret_realms 挂 v139_layer_diagram  
- 对话模板挂 branch_tree_id  
