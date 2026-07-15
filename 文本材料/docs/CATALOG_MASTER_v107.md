# 寻仙问道 · 设定包总目录 v107

> 无代码。全文件一览 + 阅读入口。规模以 `item_economy_index_v92.json` 的 `content_counts_v107` 为准。

## 0. 建议阅读顺序

1. `README_WORLDPACK.md`
2. `docs/DESIGNER_HANDBOOK_v103.md`
3. `docs/NUMERIC_ONEPAGER_v103.md`
4. 本目录 → 按需点开专页
5. 词条检索：`bestiary_summary_v101.csv` + `name_alias_glossary_v103.json`

## 1. 索引与总控

| 文件 | 说明 |
|------|------|
| `data/item_economy_index_v92.json` | 总索引 catalog_refs + content_counts |
| `data/bestiary_index_v97.json` | 全生物 id 索引 |
| `data/bestiary_summary_v101.json` / `.csv` | 生物一页摘要 |
| `data/numeric_overview_v103.json` | 数值机读总览 |
| `data/name_alias_glossary_v103.json` | 中文别名 |
| `data/item_economy_tags_v101.json` | 经济标签 |
| `data/main_quest_rewards_v101.json` | 主线奖励链 |
| `data/hanli_timeline_items_v100.json` | 时间线道具节点 |

## 2. 秘境 / 区域深挖

| 文件 | 主题 |
|------|------|
| `secret_realm_blood_forbidden_v104.json` | 血色禁地 |
| `secret_realm_xutian_v104.json` | 虚天殿 |
| `secret_realm_zhuimo_v104.json` | 坠魔谷 |
| `secret_realm_kunwu_v105.json` | 昆吾山 |
| `secret_realm_yinyang_ku_v105.json` | 阴阳窟培婴 |
| `region_tianyuan_diyuan_v105.json` | 天渊 / 地渊功勋 |
| `secret_realm_guanghan_maliang_v106.json` | 广寒 / 马良 |
| `path_deity_huoyu_v106.json` | 火狱回阳化神九步 |
| `region_chaotic_sea_liulian_v107.json` | **乱星海 / 六连殿** |

## 3. 货架与经济

| 文件 | 主题 |
|------|------|
| `sect_shelves_v106.json` | 黄枫 / 天符 / 御灵 |
| `sect_shelves_more_v107.json` | **掩月 / 落云 / 鬼灵** |
| `market_price_master_v100.json` | 地区物价 |
| `newgame_plus_economy_v102.json` | 多周目与瓶规则 |
| `travel_routes_v102.json` | 旅行枢纽路线 |
| `reputation_unlocks_v102.json` | 声望阈值 |

## 4. 成长与战斗

| 文件 | 主题 |
|------|------|
| `realm_breakthrough_v98.json` | 境界突破 |
| `loadout_by_realm_v99.json` | 境界配装 |
| `pill_formulary_v92.json` / `pill_recipes_detailed_v98.json` | 丹方 |
| `formation_array_catalog_v97.json` | 阵法 |
| `ten_poisons_antidotes_v99.json` | 毒与解 |
| `spirit_beast_companion_v93.json` / `spirit_beast_growth_v99.json` | 灵兽 |
| `manual_catalog_v92.json` 等 | 功法与冲突 |
| `region_spawn_tables_v98.json` | 区域刷怪 |

## 5. 生物词条分卷

`bestiary_compendium_v95` → `bestiary_secondary_v96` → `bestiary_more_v97` → `v98`…`v107`

## 6. 物品详释分卷

`item_descriptions_expanded_v95` → `more_v96` → `v97`…`v107`

## 7. 版本说明文档

`docs/LORE_EXPANSION_v10x.md`、`DESIGNER_HANDBOOK`、`NUMERIC_ONEPAGER`、本 `CATALOG_MASTER_v107.md`

## 8. 设计红线（再录）

- 掌天瓶 / 绿液：唯一、年帽、冷却、禁无帽叠乘  
- 通天 / 真魂 / 炼神 / 详尽节点：不复制、不早掉  
- 真灵与伙伴：不日常野刷  
- 功勋 ≠ 灵石无限兑  
- 突破失败不吞唯一剧情物  

## 9. v107 本版新增

- 掩月 / 落云 / 鬼灵货架  
- 乱星海六连殿专页  
- 本总目录  
- bestiary_v107 + item_descriptions_v107  


## 10. v108 新增

- `region_xinggong_dajin_v108.json` 星宫/大晋
- `garden_liquid_calendar_v108.json` 药园绿液年历
- `docs/CONSISTENCY_AUDIT_v108.md` 矛盾校对表
- `bestiary_v108.json` / `item_descriptions_v108.json`


## 11. v109 合并与三秘境

- `data/merge_bridge_v109.json` 原包+深挖桥接
- `secret_realm_qianzhu_tower_v109.json` 千竹机关塔
- `secret_realm_nether_river_v109.json` 冥河之地
- `secret_realm_yinluo_catacomb_v109.json` 阴罗冢
- `bestiary_v109.json` / `item_descriptions_v109.json`


## 12. v110 三秘境

- `secret_realm_seven_meridian_v110.json` 七脉试炼洞
- `secret_realm_ancient_ruins_v110.json` 古修洞府
- `secret_realm_tianlan_grotto_v110.json` 天澜秘境


## 13. v111 十九境收官

- `secret_realms_final_seven_v111.json` 余下七境
- 覆盖 **19/19**
- `bestiary_v111` / `item_descriptions_v111`


## 14. v112 原包目录对齐

- `item_descriptions_v112.json` 丹114+药79+材221+宝217+消耗57+功法21
- `bestiary_v112.json` 原包42兽+5灵宠加深
- `catalog_description_align_v112.json` id 映射


## 15. v113 加长与货架

- `item_descriptions_v113.json` 筑基/降尘/培婴等加长 + 46店
- `bestiary_v113.json` 42兽原著向加长
- `merchant_shops_guide_v113.json`


## 16. v114 任务宗门功法丹方

- `item_descriptions_v114.json`
- `quest_sect_guide_v114.json`
- `bestiary_v114.json` 生态


## 17. v115 时间线纪年成长

- `timeline_guide_v115.json`
- `item_descriptions_v115.json` 纪年56+钩208+境界…
- `bestiary_v115.json` 渡劫/使者/遭遇包


## 18. v116 模板层深稿

- 术法正本 `item_descriptions_v116_techniques.json`
- 功法 `item_descriptions_v116_methods.json`
- 区域 `region_cards_deep_v116.json`
- 原包目录深解 `item_descriptions_v116_orig_deep.json`
- 收官七境二遍 `secret_realms_final_seven_deep_v116.json`
- 具名NPC `named_npcs_v116.json`
- 合并卷 `item_descriptions_v116.json`
- 对齐 `catalog_description_align_v116.json`


## 19. v117 叙事深稿

- `quest_chains_deep_v117.json` 62链（主线分镜+全链加厚）
- 核心秘境三遍 `secret_realm_*_third_v117.json`
- `item_descriptions_v117.json` 超长关键物/势力页/对话
- `faction_pages_v117.json` `npc_dialogues_v117.json` `bestiary_v117.json`


## 20. v118 视觉外貌特效

- `item_descriptions_v118.json` 丹/药/材/宝/术/功/兽/NPC 外貌与效果表现
- `visual_style_v118.json` 调色板与字段 schema
- `bestiary_v118.json` 兽与 NPC 外貌


## 21. v119–v120 视觉深稿

- v119：分镜/BOSS/肖像/图标 `item_descriptions_v119.json` `visual_storyboards_v119.json`
- v120：效果表现表/施法/装备演出/动画 `item_descriptions_v120.json` `visual_effect_sheets_v120.json`
- 叠用 v118 appearance 字段


## 22–23. v121–v122 视觉

- v121：`visual_fx_pipeline_v121.json` 粒子/逐帧/状态机
- v122：`visual_look_cards_v122.json` 五感外貌/剪影/样式
- 叠 v118–v120


## 24. v123 分类修正

见 `docs/CLASSIFICATION_FIX_v123.md`。path 表与 bridge 在 data/。


## 25. v124 原著功法术法

见 `docs/NOVEL_CANON_METHODS_v124.md`。失神刺/大衍/青元归类以本卷为准。


## 26. v125 层数表与六宗

`docs/METHOD_LAYER_AND_SIX_SECTS_v125.md`


## 27. v126 七派与青元

`docs/SEVEN_SECTS_AND_QINGYUAN_v126.md`


## 28. v127 落云与备选七派

`docs/LUOYUN_AND_ALT_SEVEN_v127.md`


## 29. v128 乱星海与声望

`docs/CHAOTIC_SEA_AND_REP_v128.md`


## 30. v129 收口

`docs/DAJIN_LINGJIE_REP_COMPLETION_v129.md`


## 31. v130 仙界

`docs/XIANJIE_IMMORTAL_EXPANSION_v130.md`


## 32. v131 多方块结构

`docs/MULTIBLOCK_STRUCTURE_EXPANSION_v131.md`


## 33. v132 多方块续

`docs/MULTIBLOCK_STRUCTURE_EXPANSION_v132.md`


## 34. v133 多方块建造与秘境结构

`docs/MULTIBLOCK_STRUCTURE_EXPANSION_v133.md`


## 35. v134 多方块状态机

`docs/MULTIBLOCK_STRUCTURE_EXPANSION_v134.md`


## 36. v135 多方块联机与材料价

`docs/MULTIBLOCK_STRUCTURE_EXPANSION_v135.md`


## 37. v136 维度通行

`docs/DIMENSION_TRAVEL_METHODS_v136.md`


## 38. v137 设定完善

`docs/SETTING_POLISH_v137.md`


## 39. v138 设定完善续

`docs/SETTING_POLISH_v138.md`


## 40. v139 三线同扩

`docs/SETTING_EXPAND_TRIPLE_v139.md`


## 41. v140 贴原著设定完善

`docs/SETTING_CANON_EXPAND_v140.md`


## 42. v141 任务与描述

`docs/SETTING_QUESTS_DESC_v141.md`


## 43. v142 奖励与描述

`docs/SETTING_ECONOMY_DESC_v142.md`


## 44. v143 物价勾稽与终局

`docs/SETTING_CROSSWALK_ENDGAME_v143.md`


## 45. v144 货架与势力

`docs/SETTING_MARKET_FACTION_v144.md`


## 46. v145 生物·主线·丹方

`docs/SETTING_BESTIARY_STORY_PILL_v145.md`


## 47. v146 全任务线描述

`docs/QUEST_LINES_FULL_v146.md`


## 48. v147 任务线描述扩充

`docs/QUEST_LINES_FULL_v147.md`
