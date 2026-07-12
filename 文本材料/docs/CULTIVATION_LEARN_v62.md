# 功法·法术「学习限制」扩充 v62

在 v61 **设定** 之上，为全部条目增加 **`learn_requirements`**，落实汇编建议：**灵根作硬性门槛、功法作技能树主线、宗门作专精与来源**。

## 功法 `learn_requirements` 常见键

| 键 | 原著向含义 |
|----|------------|
| `realm_min` / `realm_max_practice` | 可学 / 可修上限境界 |
| `spirit_roots` | `required_any` / `bonus` / `forbidden_traits` |
| `prerequisite_methods` | 前置功法（如长春功→青元剑诀） |
| `prerequisite_realm_layers` | 炼气层数（如长春 13 层） |
| `faction` + `faction_relation_min` | 宗门与身份（外门/内门/弟子） |
| `race_required` / `path_exclusive` | 慕兰法士、鬼修、妖修、灵族 |
| `region_present` | 须在对应地图或剧情阶段 |
| `reputation` / `cannot_learn_if` | 星宫/逆星对立、正道学魔功叛宗 |
| `learn_source` | 藏经阁、贡献兑换、遗迹玉简、师承、血誓 |
| `items_suggested` / `quests_suggested` | 飞剑、尸材、坤吴任务等 |
| `karma` | 魔道因果、心魔风险 |
| `divine_sense_min` | 制符/傀儡对大衍、天符神识要求 |

**87** 条功法均有 `learn_requirements`；**30** 条为 waves **详述**（黄枫、七派、魔道六宗、乱星海、慕兰、鬼修、灵界等）。

## 法术 `learn_requirements` 常见键

| 键 | 含义 |
|----|------|
| `prerequisite_methods` | 必须先修对应功法 |
| `prerequisite_techniques` | 神通前置法术（如剑阵+剑雨） |
| `prerequisite_mode` | `all` / `any_one` |
| `items_required` | 装备飞剑、消耗灵核、噬金虫巢等 |
| `divine_sense_min` | 神识门槛 |
| `terrain_bonus` / `region_present` | 土遁、海域阵法等 |
| `learn_source` | 秘籍、传承、通用法术册 |

**293** 条术法均有 `learn_requirements`；**13** 条关键术详述；其余按 **spell/secret 默认规则** + `requires_method` 自动生成。

## 灵根索引

`spirit_roots.json` v2：`element_to_methods` 由功法 `learn_requirements` / 灵根门控**反查汇总**，供模组解锁 UI 使用。

## 脚本链（完整）

1. `expand_cultivation_novel_all.py` — 名录  
2. `enrich_cultivation_content.py` — 描述 / effect  
3. `enrich_cultivation_setting.py` — setting  
4. **`enrich_cultivation_learn.py`** — **学习限制**（v62）

编辑 **`novel_cultivation_learn_requirements_waves.json`** 后重跑 **4**。

## 打包

`seeking_immortals_cultivation_v62_learn.zip`