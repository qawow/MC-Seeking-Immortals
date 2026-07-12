# 技能树与鬼修闭环 v34

> 汇编 **§18**：功法（职业）→ 法术（主动）→ 神通/秘术（高阶稀有）

## 技能数据

| 文件 | 作用 |
|------|------|
| `skill_trees.json` | 青元剑道、鬼修玄阴、天符、慕兰法士等树 |
| `cultivation_methods.json` | 功法根节点、`unlocks_techniques_school` |
| `techniques/sword.json` v2 | `requires_method: qingyuan_sword_art`，`tier` |
| `techniques/xuan_yin.json` v2 | 鬼修专属术 + 阶段术，`path_required` |
| `techniques/index.json` v3 | `skill_trees_ref` |

## 青元剑道（原著结构）

```
长春功 → 青元剑诀 → 青元剑芒 / 御剑斩 / 剑雨 →（结丹+）剑阵类 secret
```

## 鬼修闭环

```
阴冥脱困 → 阴体（敛魂符）→ 冥河朝圣（阴石）→ 魂锚（聚魂石）
→ 冥河地结冥核 → 守关 Boss → 阴婴
```

| 环节 | 数据挂钩 |
|------|----------|
| 道途 | `ghost_cultivation_path.json` v3 `loop` |
| 维度 | `seeking_immortals:yin_ming_pocket` |
| 秘境 | `nether_river_land` 四要素 |
| 任务链 | `quest_chains#ghost_path` 6 步 |
| 掉落 | `boss_loot_tables#nether_river_guardian` |
| 商店 | `nether_ferry_vendor`（阴石） |
| 功法 | `ghost_nether_art` 解锁 `xuan_yin` 鬼修术 |

## 玄阴 vs 鬼修

- **玄阴诀**：魔道阴功，可习部分 `xuan_yin` 法术，无阴体阶段  
- **冥河鬼修诀**：`path_exclusive: ghost_cultivator`，含阶段术与冥核/阴婴  

## §18 合规

- 用 `realm_min`、`tier`、`requires_method`，不用修炼成功率表  
- Boss `chance` 为模组建议掉率，可配置覆盖  

## 下一步

- `techniques/talisman.json` 挂 `tianfu_spirit_scripture`  
- Patchouli「技能树」「鬼修道途」页