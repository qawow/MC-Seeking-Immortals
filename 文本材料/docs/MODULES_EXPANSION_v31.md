# 各模块扩充 v31（功法三层 + 慕兰战争 + 经济上架）

## 本轮

| 模块 | 变更 |
|------|------|
| `cultivation_methods.json` | +10 功法，挂宗门/种族/地区；schema v2 |
| `techniques_sample.json` | +8 法术（慕兰/天澜/剑/符/魔遁） |
| `faction_conflict_events.json` | +天南慕兰边境战、天澜祭、七派联防等 |
| `chronicle_events.json` | +慕兰入侵、天澜圣兽、虚天周期、古魔封印 |
| `items_by_region.json` | 慕兰/乱星海/冥河/大晋特产补全 |
| `merchant_shops.json` | 天南坊市上架驱魔草、固元丹等 |
| `quest_chains.json` | `chain_mulan_border` |
| `constitution_catalog.json` | +风灵体（结构向，无百分比） |
| 文档 | `MULAN_TIANLAN_WAR_EXPAND.md` |

## 汇编对应

- **§18**：功法/法术/神通三层 → `cultivation_methods` + `techniques` + tier `secret`
- **§9**：宗门专精 → 天符/掩月/灵兽/清虚/化刀功法来源字段
- **§13**：战争 = 环境规则 + 掉落，非副本数值表

## 累计（v30+v31）

见 `MODULES_EXPANSION_v30.md`；法宝仍 169。

## 下一步

- `techniques/index.json` 分卷批量挂 `school: mulan_fashi`
- Patchouli「功法与法术」章
- `spawn_tables` 慕兰法士小队与风狼联动