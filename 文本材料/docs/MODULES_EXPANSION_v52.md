# 各模块扩充 v52

> 汇编 **§9 宗门专精标签** · **§13** 妖王领地掉落 · **§18** 结构优先

## 人族世家联盟

| 世家 | 专精 | 特产 |
|------|------|------|
| 莫家 | 阵法 | 阵盘碎片 |
| 俞家 | 炼器 | 灵矿捆 |
| 谷家 | 炼丹 | 丹药配额券 |
| 宁家 | 制符 | 高阶符坯 |

- `human_clan_league.json` + `human_clan_quest_network.json`
- 任务链 **5 条世家线** + **`human_clan_league_hub`**
- 与 `dajin_clan_feud` 镜像联动

## 蛮荒七妖王掉落

- `barbarian_demon_kings.json` v2：每王 `secret_realm_hint` + `drops` + 可选 `faction_trade`
- `artifact_realm_drops#barbarian_king_territories`
- **3 枚妖王信物** → `barbarian_council_audience`

## Patchouli

- `patchouli_static_entries.json`：天渊城、风元、七妖王、世家联盟、功勋阁
- `patchouli_item_book` v14 挂 `static_entries_ref`

## 打包

**`seeking_immortals_lore_v52.zip`**

## 下一步

- 十八妖族与 `spirit_realm_clan_quests` 交叉引用
- 妖王领地写入 `secret_realm_template` 子秘境
- 炼虚/大乘灵界日常事件池