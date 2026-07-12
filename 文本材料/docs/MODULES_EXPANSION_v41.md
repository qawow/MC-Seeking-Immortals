# 各模块扩充 v41

> 汇编 §13 秘境四要素 · 五界阴司集群 · 灵界蛮荒妖王**结构**

## 阴司之界（yin_underworld_cluster）

| 层级 | 内容 |
|------|------|
| 数据 | `yin_underworld_cluster.json` |
| 地区 | `yinming`、`nether_river` 地区卡 |
| 口袋维 | `nether_river_pocket` |
| 闭环 | 鬼修道途 + `ghost_path` + 冥河渡夫 + 阴石 |
| 天劫 | `nether_river_shield_zone`（阴区减伤结构） |
| 任务链 | `yin_cluster_pilgrim`（可与鬼修线并行） |
| 秘境模板 | `yinming_pocket` 四要素 |

## 地渊（灵界）

- `secret_realm_template.json#diyuan` 四要素 + `L3_diyuan`
- 与 `spirit_realm_rise`、`diyuan_scout` 挂钩

## 蛮荒七妖王

| 文件 | 内容 |
|------|------|
| `barbarian_demon_kings.json` | 7 妖王**功能标签**（熊/狐/松/鹏/蛇/虎/龟） |
| `barbarian_wasteland` 地区卡 | 炼虚+、空间裂缝风险 |
| 任务链 | `barbarian_kings_line` |
| 势力 | 蛮荒妖王议会（松散） |
| 日常 | 兽潮过境 |

**合规**：妖王用通用称号+专精 archetype，不绑主角剧情名。

## 技能树

- `yin_cluster_ghost`：阴司集群 + 冥河鬼修诀

## 规模

任务链 **18**、任务钩 **~78**、阴司 2 地区 + 7 妖王条目

## 下一步

- 灵界十八族任务细化（已有 faction_species）
- 人界边界·空间节点
- 打包 v41 zip（data+docs）