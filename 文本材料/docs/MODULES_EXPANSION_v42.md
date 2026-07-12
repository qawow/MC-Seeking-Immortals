# 各模块扩充 v42

> 汇编 §9.7 天渊/灵界势力 · §13 节点门槛 · §18 结构优先于数值

## 灵界十八族任务

| 文件 | 内容 |
|------|------|
| `spirit_realm_clan_quests.json` | 各族 `quest_hooks` + 特产 `specialty_item` + 禁忌 `taboo` |
| 任务链 | `spirit_eighteen_clans`（天渊入伍 → 5 族朝贡 → 可选宝会/修罗） |
| 示例钩 | 天狐幻试、飞灵竞猎、石人巡脉、天渊猎魔周常 |

与 `faction_species.json`、`playable_races` 灵界四族 playable 对齐。

## 人界空间节点

| 文件 | 内容 |
|------|------|
| `spatial_nodes_catalog.json` | 9 节点：固定阵/古裂隙/宗门门/飞升/冥河渡 |
| 类型 | `fixed_teleport_array`、`ancient_rift`、`ascension_gate`、`pocket_gate` |
| 门槛 | 许可 + 灵石 + 境界；**不写传送成功率** |
| 联动 | 乱星海许可、坠魔谷→魔界裂隙、天渊→风元 |

`dimensions_catalog` v4、`spirit_realm_interface` v4、`trade_routes` v2 挂 `spatial_nodes_ref`。

## 其它

- `chaotic_sea` 地区卡：枢纽节点 + 传送许可
- 任务钩：传送阵校准、人界裂缝稳固
- Patchouli v10 条目名

## 规模

节点 **9**、十八族任务表 **18** 势力行、任务链 **19**、任务钩 **~85**

## 下一步

- 星宫/逆星盟据点节点细化
- 全量 data+docs 打包 zip
- 校验 `data_manifest` 与 `item_id_index` 一致性