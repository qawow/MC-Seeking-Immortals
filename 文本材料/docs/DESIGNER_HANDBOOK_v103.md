# 寻仙问道 · 策划阅读手册 v103

> 无代码。建议按下列顺序读设定包，避免一上来淹没在词条海里。

## 0. 五分钟上手

1. `README_WORLDPACK.md` — 版本与关键表  
2. `data/item_economy_index_v92.json` → `content_counts_v102` / `catalog_refs` — 规模与文件索引  
3. `docs/LORE_EXPANSION_v102.md` — 最近连接层说明  

## 1. 世界骨架（先建立坐标）

| 顺序 | 文件 | 用途 |
|------|------|------|
| 1 | `hanli_timeline_items_v100.json` | 主线时间与关键道具节点 |
| 2 | `main_quest_rewards_v101.json` | 任务链奖励对照 |
| 3 | `travel_routes_v102.json` | 地图枢纽与旅行时间 |
| 4 | `realm_breakthrough_v98.json` | 境界突破与失败惩罚 |
| 5 | `reputation_unlocks_v102.json` | 势力声望门槛 |

## 2. 战斗与成长

| 顺序 | 文件 | 用途 |
|------|------|------|
| 1 | `loadout_by_realm_v99.json` | 各境配装 |
| 2 | `manual_catalog_v92.json` + `manual_descriptions_v96.json` + `manual_conflict_matrix_v100.json` | 功法与冲突 |
| 3 | `spirit_beast_companion_v93.json` + `spirit_beast_growth_v99.json` | 灵兽伙伴与养成 |
| 4 | `ten_poisons_antidotes_v99.json` | 毒与专解 |
| 5 | `formation_array_catalog_v97.json` | 阵法 |

## 3. 经济与产出

| 顺序 | 文件 | 用途 |
|------|------|------|
| 1 | `market_price_master_v100.json` | 地区物价 + 通货总表 |
| 2 | `item_economy_tags_v101.json` | trade/quest/unique 过滤 |
| 3 | `newgame_plus_economy_v102.json` | 瓶/绿液/NG+ 防炸 |
| 4 | `pill_formulary_v92.json` + `pill_recipes_detailed_v98.json` | 丹与辅料 |
| 5 | `region_spawn_tables_v98.json` + `beast_materials_loot_v92.json` | 刷怪与素材 |

## 4. 词条库（需要时查，不必通读）

- 生物：`bestiary_*` + **`bestiary_summary_v101.csv`（优先）** + `bestiary_index_v97.json`  
- 物品说明：`item_descriptions_v95~v102`  
- 灵药/材料/法宝/符箓：`herb_*` `materials_*` `artifact_*` `talisman_*`  
- 具名事件：`named_npc_loot_rewards_v97.json`  

## 5. 设计红线（必守）

1. **掌天瓶 / 绿液**：唯一、禁交易、年次数帽、冷却、禁与芥子无帽叠乘  
2. **唯一物**：虚天鼎、八灵尺、炼神术、真魂丹、详尽节点等不复制、不早掉  
3. **真灵 / 伙伴**：不进日常刷怪表  
4. **功勋 ≠ 灵石** 无限兑换  
5. **突破失败**不吞唯一剧情物  

## 6. 推荐制作切片（模组落地顺序）

1. 炼气→筑基：血色禁地 + 四主药 + 筑基丹 + 黄枫声望  
2. 筑基→结丹：乱星海 + 降尘 + 风雷翅线  
3. 结丹→元婴：虚天/云梦/定灵参丹  
4. 元婴中盘：赌战庚精、坠魔造化、昆吾飞升包  
5. 灵界：天渊功勋、地渊、广寒  

## 7. 本版新增入口

- `data/numeric_overview_v103.json` — 数值一页总览  
- `data/name_alias_glossary_v103.json` — 中文名/别名对照  
- `docs/NUMERIC_ONEPAGER_v103.md` — 人读版数值页  


## 8. v104 秘境深挖

- 血色：`data/secret_realm_blood_forbidden_v104.json`
- 虚天：`data/secret_realm_xutian_v104.json`
- 坠魔：`data/secret_realm_zhuimo_v104.json`


## 9. v105 后半程深挖

- 昆吾：`data/secret_realm_kunwu_v105.json`
- 阴阳窟：`data/secret_realm_yinyang_ku_v105.json`
- 天渊地渊：`data/region_tianyuan_diyuan_v105.json`


## 10. v106 终局与宗门货架

- 广寒马良：`data/secret_realm_guanghan_maliang_v106.json`
- 化神火狱：`data/path_deity_huoyu_v106.json`
- 三宗门货架：`data/sect_shelves_v106.json`


## 11. v107 天南货架续与乱星海

- 掩月/落云/鬼灵：`data/sect_shelves_more_v107.json`
- 乱星海六连殿：`data/region_chaotic_sea_liulian_v107.json`
- 总目录：`docs/CATALOG_MASTER_v107.md`


## 12. v108 星宫大晋与药园

- 星宫/大晋：`data/region_xinggong_dajin_v108.json`
- 药园绿液：`data/garden_liquid_calendar_v108.json`
- 校对表：`docs/CONSISTENCY_AUDIT_v108.md`


## 13. v109 合并与三秘境

- 合并桥：`data/merge_bridge_v109.json`
- 千竹 / 冥河 / 阴罗冢 专页
- 原包 `secret_realms.json` 与深挖 id 对齐


## 14. v110 七脉/古修/天澜

炼气考校、模板洞府、慕兰风战场。见 `LORE_EXPANSION_v110.md`。


## 15. v111 十九境收官

见 `secret_realms_final_seven_v111.json` 与 `LORE_EXPANSION_v111.md`。九仙非大众、灵缈软顶、修罗投影。


## 16. v112 原包目录对齐

查 `catalog_description_align_v112.json`：任意 pills/herbs/materials/artifacts id → orig_* 详释。


## 17. v113 加长与商店

关键丹宝见 `item_descriptions_v113`；42兽见 `bestiary_v113`；店见 `merchant_shops_guide_v113`。


## 18. v114 任务宗门功法

见 `quest_sect_guide_v114.json` 与 `item_descriptions_v114.json`。


## 19. v115 时间线

人生节奏见 `timeline_guide_v115.json`；纪年与钩见 `item_descriptions_v115`。


## 20. v116 深稿

说明文案优先 `catalog_description_align_v116` 与 `item_descriptions_v116_*` 分卷；NPC 见 `named_npcs_v116`。


## 21. v117 叙事

主线分镜见 `quest_chains_deep_v117`；对话见 `npc_dialogues_v117`；秘境三遍见 `secret_realm_*_third_v117`。


## 22. v118 视觉

外貌与特效见 `visual_style_v118.json` 与 `item_descriptions_v118.json`（含 appearance / vfx 字段）。


## 23. v119–v120 视觉

分镜 `visual_storyboards_v119`；效果表 `visual_effect_sheets_v120`；圣经 `VISUAL_BIBLE_v120.md`。


## 24–25. v121–v122 视觉

管线 `visual_fx_pipeline_v121`；外貌卡 `visual_look_cards_v122`；圣经 `VISUAL_BIBLE_v122.md`。
