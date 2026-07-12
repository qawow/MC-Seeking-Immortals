# Seeking Immortals · 世界观设定总纲（维护版 v1）

> **用途**：本对话中已整理的《凡人修仙传》灵感向 MC 模组设定汇总，供持续扩充与落地 JSON/任务/区域。  
> **维护规则**：新增设定只改本文件与 `MAINTENANCE_LOG.md`，不另建重复总览。  
> **版权**：结构借鉴凡人流；对外发布时对强绑定专属名（掌天瓶、青竹蜂云剑等）建议改名。  
> **关联**：汇编《设定资料汇编 v3》、项目 `seeking_immortals`、知识库 `mortal-cultivation-inspired-mod-knowledge`。

---

## 0. 文档索引

| 文件 | 内容 |
|---|---|
| `WORLD_SETTING_BIBLE.md` | 本文件：世界设定总纲 |
| `CHRONICLE.md` | 道历纪年表、大时代、周期事件 |
| `EXPANSION_ROADMAP.md` | 缺口扫描 + 扩充方向与价值 |
| `MAINTENANCE_LOG.md` | 变更记录 |
| `data/chronicle_events.json` | 纪年事件 49 条 |
| `data/sects.json` | 宗门/势力 20 条 |
| `data/faction_graph.json` | 势力关系边 |
| `data/beast_bestiary.json` | 妖兽 30 + 灵宠 5 |
| `docs/BEAST_BESTIARY.md` | 图鉴人类可读索引 |
| `data/region_cards/` | 地区设定卡（天南/乱星海/慕兰） |
| `data/items_by_region.json` | 地区特产 |
| `docs/ITEMS_BY_REGION.md` | 特产索引 |
| `data/secret_realms.json` | 8 大秘境模板 |
| `data/chaotic_sea_factions.json` | 星宫 / 逆星盟详表 |
| `docs/CHAOTIC_SEA.md` | 乱星海扩展 |
| `data/talisman_catalog.json` | 符箓 11 |
| `data/item_synergy.json` | 定神珠/邪幻镜/傀儡等联动 |
| `docs/UNDERWORLD_REALMS.md` | 冥河·阴冥·坠魔谷 |
| `data/secret_realm_template.json` | 秘境四要素（汇编§13） |
| `data/economy_reference_magnitudes.json` | 经济量级（汇编§16） |
| `data/daily_random_events.json` | 地区日常随机事件 |
| `data/alchemy_system.json` | 炉盖火 + 错配爆炸 |
| `data/alchemy_recipes.json` | 丹方与材料 |
| `data/sect_contribution_shop.json` | 宗门贡献兑换 |
| `data/quest_chains.json` | 任务链编排 |
| `docs/ALCHEMY_DESIGN.md` | 炼丹实现说明 |
| `data/puppet_definitions.json` | 傀儡树（木人→混元→灵界）|
| `data/talisman_catalog.json` | 符箓×16 + 纸墨规则 |
| `data/spirit_herbs_catalog.json` | 灵草×15 + 年龄档 |
| `data/materials_catalog.json` | 材料×23 |
| `data/formation_catalog.json` | 阵法×15 |
| `data/trade_routes.json` | 跨区商路与拍卖枢纽 |
| `data/dimensions_catalog.json` | **维度 ID + 入境门槛 + 旅行矩阵（P0）** |
| `data/spirit_realm_interface.json` | 飞升/天渊/灵界门/分身下界 |
| `docs/DIMENSIONS_DESIGN.md` | 五界→模组维度说明 |
| `data/skill_trees.json` | 功法→法术→神通技能树 |
| `data/spirit_roots.json` | 灵根类型与属性门控 |
| `data/demonic_six_sects.json` | 魔道六宗专精 |
| `data/ascension_flow.json` | 飞升天渊→灵界结构 |
| `data/yin_underworld_cluster.json` | 阴司冥河集群 |
| `data/barbarian_demon_kings.json` | 蛮荒七妖王结构 |
| `data/spatial_nodes_catalog.json` | 跨界空间节点 |
| `data/spirit_realm_clan_quests.json` | 灵界十八族任务 |
| `data/star_palace_internal_factions.json` | 星宫执法/贸易派 |
| `data/mulan_tianlan_war.json` | 慕兰天澜战役 |
| `data/schema_validation_report.json` | JSON schema 扫描 |
| `data/wutu_mulan_feud.json` | 突兀慕兰世仇 |
| `data/yin_luo_hall.json` | 阴罗殿 |
| `data/techniques/ghost.json` | 阴罗鬼术门派 |
| `region_cards/kunwu.json` | 昆吾山 |
| `data/tianyuan_city.json` | 天渊城 |
| `data/ghost_sect_ban_rules.json` | 鬼修禁宗后果 |
| `region_cards/spirit_fengyuan.json` | 风元大陆 |
| `region_cards/barbarian_wasteland.json` | 蛮荒 |
| `data/human_clan_league.json` | 风元世家联盟 |
| `data/patchouli_static_entries.json` | Patchouli 静态页 |
| `secret_realm_template#barbarian_king_territories` | 七王域 |
| `daily_random_events#spirit_realm_void_great` | 炼虚大乘池 |
| `dimensions_catalog` 灵界 subdimensions | v54 |
| `spirit_realm_clan_quests` 双钩 | v54 |
| `merchant_shops` 十八族兑换 | v55 |
| `tribulation_types` 灵界变体 | v55 |
| `mortal_to_spirit_bridge` | v55 |
| `inverse_star_quest_network.json` | 逆星盟任务网 |
| `scripts/validate_all.py` | 全量 JSON 校验 |
| `novel_items_master_index.json` | 原著向物品总索引 v57 |
| `scripts/expand_items_novel_complete.py` | 物品批量合并 |
| `novel_items_waves.json` | 原著具名物品种子库 |
| `scripts/expand_items_novel_all.py` | 全量合并 waves+结构 |
| `novel_cultivation_waves.json` | 功法法术神通种子 |
| `scripts/expand_cultivation_novel_all.py` | 修炼体系全量合并 |
| `seeking_immortals_cultivation_v59.zip` | 功法法术 v59 包 |
| `novel_cultivation_content_waves.json` | 功法法术**具体内容**种子 |
| `scripts/enrich_cultivation_content.py` | 补 description/effect/消耗 |
| `seeking_immortals_cultivation_v60_content.zip` | v60 内容包 |
| `novel_cultivation_setting_waves.json` | 功法法术**设定**种子 |
| `scripts/enrich_cultivation_setting.py` | 挂载 setting 块 |
| `seeking_immortals_cultivation_v61_setting.zip` | v61 设定包 |
| `novel_cultivation_learn_requirements_waves.json` | **学习限制**种子 |
| `scripts/enrich_cultivation_learn.py` | 挂载 learn_requirements |
| `seeking_immortals_cultivation_v62_learn.zip` | v62 学习限制包 |
| `novel_beast_puppet_waves.json` | 灵兽傀儡设定/学习种子 |
| `scripts/enrich_beast_puppet.py` | 灵兽傀儡扩充 |
| `seeking_immortals_beast_puppet_v63.zip` | v63 灵兽傀儡包 |
| `novel_world_expansion_waves_v64.json` | 世界模块扩充种子 |
| `scripts/expand_world_modules_v64.py` | v64 扩充脚本 |
| `seeking_immortals_world_v64.zip` | v64 世界包 |
| `novel_world_expansion_waves_v65.json` | v65 种子 |
| `scripts/expand_world_modules_v65.py` | v65 脚本 |
| `seeking_immortals_world_v65.zip` | v65 包 |
| `novel_world_expansion_waves_v66.json` | v66 种子 |
| `scripts/expand_world_modules_v66.py` | v66 脚本 |
| `seeking_immortals_world_v66.zip` | v66 包 |
| `novel_world_expansion_waves_v67.json` | v67 种子 |
| `scripts/expand_world_modules_v67.py` | v67 脚本 |
| `seeking_immortals_world_v67.zip` | v67 包 |
| `novel_world_expansion_waves_v68.json` | v68 种子 |
| `scripts/expand_world_modules_v68.py` | v68 脚本 |
| `seeking_immortals_world_v68.zip` | v68 包 |
| `novel_world_expansion_waves_v69.json` | v69 种子 |
| `scripts/expand_world_modules_v69.py` | v69 脚本 |
| `seeking_immortals_world_v69.zip` | v69 包 |
| `novel_world_expansion_waves_v70.json` | v70 种子 |
| `scripts/expand_world_modules_v70.py` | v70 脚本 |
| `seeking_immortals_world_v70.zip` | v70 包 |
| `docs/WORLD_EXPANSION_v70.md` | v70 说明 |
| `docs/WORLD_EXPANSION_v69.md` | v69 说明 |
| `docs/WORLD_EXPANSION_v68.md` | v68 说明 |
| `docs/WORLD_EXPANSION_v67.md` | v67 说明 |
| `docs/WORLD_EXPANSION_v66.md` | v66 说明 |
| `docs/WORLD_EXPANSION_v65.md` | v65 说明 |
| `docs/WORLD_EXPANSION_v64.md` | v64 说明 |
| `docs/BEAST_PUPPET_v63.md` | v63 说明 |
| `docs/CULTIVATION_LEARN_v62.md` | v62 学习限制说明 |
| `docs/CULTIVATION_SETTING_v61.md` | v61 设定说明 |
| `docs/CULTIVATION_CONTENT_v60.md` | v60 具体内容说明 |
| `docs/CULTIVATION_EXPANSION_v59.md` | v59 名录说明 |
| `seeking_immortals_items_v58_full.zip` | 物品 v58 全量包 |
| `docs/ITEMS_EXPANSION_v58.md` | v58「全部」说明 |
| `seeking_immortals_items_v57.zip` | 物品 v57 包 |
| `docs/ITEMS_EXPANSION_v57.md` | 物品扩充说明 |
| `seeking_immortals_lore_v56.zip` | v56 全量包 |
| `docs/MODULES_EXPANSION_v56.md` | v56 说明 |
| `seeking_immortals_lore_v55.zip` | v55 全量包 |
| `docs/MODULES_EXPANSION_v55.md` | v55 说明 |
| `seeking_immortals_lore_v54.zip` | v54 全量包 |
| `docs/MODULES_EXPANSION_v54.md` | v54 说明 |
| `seeking_immortals_lore_v53.zip` | v53 大量扩充包 |
| `docs/MODULES_EXPANSION_v53.md` | v53 说明 |
| `seeking_immortals_lore_v52.zip` | v52 全量包 |
| `docs/MODULES_EXPANSION_v52.md` | 世家/妖王/Patchouli v52 |
| `seeking_immortals_lore_v51.zip` | v51 全量包 |
| `docs/MODULES_EXPANSION_v51.md` | 风元/功勋/润色 v51 |
| `docs/SECRET_REALM_POLISH_CHECKLIST_v51.md` | 秘境润色清单 |
| `seeking_immortals_lore_v50.zip` | v50 里程碑包 |
| `docs/MODULES_EXPANSION_v50.md` | 天渊/鬼修/秘境 v50 |
| `seeking_immortals_lore_v49.zip` | v49 1.20.1 加载器 |
| `docs/MODULES_EXPANSION_v49.md` | 昆吾/ghost v49 |
| `seeking_immortals_lore_v48.zip` | v48 全量包 |
| `docs/MODULES_EXPANSION_v48.md` | 坠魔谷/大晋/阴罗 v48 |
| `seeking_immortals_lore_v47.zip` | v47 全量包 |
| `docs/MODULES_EXPANSION_v47.md` | 慕兰天澜 v47 |
| `seeking_immortals_lore_v46.zip` | v46 含 ReloadListener |
| `docs/MODULES_EXPANSION_v46.md` | 采珠/派系/Forge v46 |
| `seeking_immortals_lore_v45.zip` | v45 含 LorePackLoader |
| `docs/MODULES_EXPANSION_v45.md` | 补天丹/外海坊市 v45 |
| `seeking_immortals_lore_v44.zip` | v44 全量包 |
| `docs/NEOFORGE_DATA_LOADER.md` | 数据加载草案 |
| `docs/MODULES_EXPANSION_v44.md` | 黑市/星宫门控 v44 |
| `seeking_immortals_lore_v43.zip` | 全量 data+docs 包 |
| `docs/MODULES_EXPANSION_v43.md` | 星宫逆星/虚天殿/打包 v43 |
| `docs/MODULES_EXPANSION_v42.md` | 十八族/空间节点 v42 |
| `docs/MODULES_EXPANSION_v41.md` | 阴司/蛮荒/地渊 v41 |
| `docs/MODULES_EXPANSION_v40.md` | 魔道六宗/飞升灵界 v40 |
| `docs/MODULES_EXPANSION_v39.md` | 千竹御灵傀儡/大晋世家 v39 |
| `docs/MODULES_EXPANSION_v38.md` | 掩月/七玄/化刀巨剑 v38 |
| `docs/MODULES_EXPANSION_v37.md` | 黄枫丹道/慕兰法士 v37 |
| `docs/MODULES_EXPANSION_v36.md` | 五行灵根/坠魔古魔 v36 |
| `docs/MODULES_EXPANSION_v35.md` | 天符/大晋昆吾 v35 |
| `docs/SKILL_TREE_AND_GHOST_v34.md` | 技能树 + 鬼修闭环 v34 |
| `docs/CHAOTIC_SEA_EXPAND_v33.md` | 乱星海星宫/逆星 v33 |
| `data/merchant_shops.json` | NPC 店铺与货币 |
| `data/spirit_roots_catalog.json` | 灵根品阶/变异/丹器灵根 |
| `data/cultivation_progression.json` | 境界小阶/寿元/瓶颈 |
| `data/tribulation_rules.json` | 天劫类型与波数 |
| `data/boss_loot_tables.json` | 秘境 Boss 掉落 |
| `data/data_manifest.json` | 全数据包索引 |
| `data/npc_dialogue_templates.json` | NPC 原型与台词键 |
| `docs/NPC_AND_REALM_DESIGN.md` | NPC 与灵界秘境 |
| `docs/SPIRIT_REALM_INTERFACE.md` | 灵界接口说明 |
| `data/techniques_sample.json` | 术法×9（含法士、血煞） |
| `docs/ECONOMY_DESIGN.md` | 经济重平衡说明 |
| `data/tribulation_items.json` | 避雷与加雷 |
| `data/techniques/` | 术法分卷 **178** 条（`index.json`） |
| `docs/SPIRIT_COST_FORMULA.md` | 灵力公式 |
| `docs/MULAN_TIANLAN_WAR.md` | 慕兰战争线 |
| `docs/MULAN_TIANLAN_WAR_EXPAND.md` | 慕兰/天澜扩充 v31 |
| `docs/MODULES_EXPANSION_v30.md` | 多模块扩充 v30 |
| `docs/MODULES_EXPANSION_v32.md` | 术法/天渊/千竹塔 v32 |
| `docs/REALM_NAME_MAPPING.md` | 引气/炼气 境界对照 |

---

## 1. 宇宙结构与世界层级

### 1.1 五界

| 界面 | 定位 | 模组阶段 |
|---|---|---|
| **人界** | 主线前期，灵气受古魔入侵压制，化神上限约初期 | 引气～化神 |
| **灵界** | 飞升后，灵界百族，大乘为顶 | 化神～大乘 |
| **阴司之界** | 鬼魂/亡灵背景，冥河类秘境 | 鬼修、秘境 |
| **上古魔界** | 古魔、魔化来源 | 终局、裂隙事件 |
| **仙界** | 法则、道祖 | 远期 DLC |

### 1.2 跨界面机制

- **空间裂缝**：偷渡高风险；可见/隐形裂缝。  
- **魔化**：古魔魔气强化装备，威力↑失控↑。  
- **分身下界**：灵界高阶避劫，人界复活点式传承。  
- **飞升链**：人界→灵界→仙界；渡劫非独立境界而是大乘巅峰状态。

### 1.3 人界区域模式（四种社会结构）

| 区域 | 世俗与修仙关系 | 模组标签 |
|---|---|---|
| 天南（越国等） | 脱节，家族为桥 | `region_tiannan` |
| 乱星海 | 百姓纳灵石换庇护 | `region_chaotic_sea` |
| 大晋 | 幕后控政权，世家 | `region_dajin` |
| 慕兰草原 | 修士被信仰 | `region_mulan` |
| 突兀族 | 天澜兽信仰，与慕兰世仇 | `region_tianlan` |

### 1.5 纪年与「现在」（详见 `CHRONICLE.md`）

- **道历**为模组通用纪年；默认 **道历 12,960** 为玩家入场年。  
- **五灵纪**：蒙昧 → 上古鼎盛 → 封魔劫 → 诸国争霸 → 灵衰（今）。  
- **封魔劫（约道历前 3 万）** 导致人界化神上限跌至初期。  
- 周期秘境：血色禁地每 5 年、虚天殿每 300 年（见纪年表 §九）。

### 1.4 灵界地标（待扩充细节）

- 天渊城（修炼×2 区）
- 九仙山（万宝大会）  
- 风元大陆西北（人妖相对弱）  
- 上族 1～9 阶、圣族（合体+）  
- 地渊、冥河之地、广寒界、魔金山脉、小修罗界（秘境模板）

---

## 2. 修炼境界体系

### 2.1 大阶段

| 阶段 | 境界 | 寿元（共识） | 元神/特征 |
|---|---|---|---|
| 下境 | 炼气 13 层 | ~百载 | 气态 |
| 下境 | 筑基 | ~二百 | 液态真元，真火 |
| 下境 | 结丹 | ~四五百 | 固态，本命法宝 |
| 下境 | 元婴 | ~千余 | 婴儿状，夺舍 |
| 下境 | 化神 | ~二千 | 飞升灵界 |
| 中境 | 炼虚→合体→大乘 | 天劫制 | 法则借用 |
| 上境 | 渡劫/飞升 | — | 入仙界 |

小阶段：初/中/后/圆满（炼气为 1～13 层）。

### 2.2 天劫

- 小/大/仙劫；**强度因种族、个体而异**；同类型逐次增强。  
- 避雷：三清雷霄符、御雷签、地渊冥河屏蔽、元合五极山等。  
- 模组：煞气加雷、妖修雷劫 +15% 等。

### 2.3 模组当前映射（seeking_immortals）

- 境界名：引气境、聚气境、凝元境（待对齐人界完整链）。  
- 修为推导境界、突破、破境丹、打坐/蒲团、灵石品阶与兑换。

---

## 3. 灵根与属性

### 3.1 灵根类型

| 类型 | 说明 | 修炼倍率（建议） |
|---|---|---|
| 天灵根 | 单属性 | 2～3× |
| 真灵根 | 双/三属性 | 较快 |
| 伪灵根 | 四/五属性 | 慢，瓶颈难 |
| 变异灵根 | 雷冰风暗等 | 快，有瓶颈 |
| 隐灵根 | 周期消失 | 特殊辅助 |
| 丹灵根/器灵根 | 灵界人造补缺 | 冲炼虚用 |

**门槛**：木灵根才能修长春功类；属性决定功法解锁。

### 3.2 五行与变异（MC 映射）

金木水火土 + 雷冰风光暗空间 → 亲和系数、环境加成、术法 `attribute`。

### 3.3 灵根品质

伪灵根、杂灵根、三/双/单灵根、天灵根、异灵根 → 突破率、消耗、冷却。

---

## 4. 特殊体质（与种族独立叠加）

### 4.1 原著向

通玉凤髓、龙吟之体、自治之体、不灭之体、金刚不坏、锻金之体、九灵剑体、虚影之体等。

### 4.2 设计原则

- 百科百分比评分为**灵感池**，非原著铁律。  
- **僵尸之体/鬼体/人体** → 用 `body_type` 与体质叠加强化。

### 4.3 叠加 cap

灵根 × 体质 × 血脉 × 区域 buff 建议总修炼速度上限约 **×2.5**。

---

## 5. 种族体系（三层模型）

### 5.1 Capability 字段

```text
race_id, faction_species[], beast_tier, body_type,
constitution_id, true_spirit_bloodline, path_tags[]
```

### 5.2 可玩种族（A 层）

| id | 显示名 | 要点 |
|---|---|---|
| human_cultivator | 人族修士 | 默认，灵根全功法 |
| mulan_fashi | 慕兰法士 | 灵术，神通栏 -1 |
| demon_cultivator | 妖修 | 7 级化形，ancestry |
| ghost_cultivator | 鬼修 | 冥河，怕至阳 |
| corpse_refined | 炼尸道体 | 尸气，丹药受限 |
| demon_path_human | 魔修 | path 非物种 |
| spirit_realm_half | 灵界混血 | 丹灵根/器灵根 |
| puppet_symbiont | 寄灵修士 | 大衍+通灵傀儡 |

### 5.3 妖修 ancestry（12 系）

avian, serpent, fox, turtle, insect, dragon_blood, qilin_blood, illusion, grassland, spirit_bird, metal_killer, ancient_stone。

### 5.4 灵界势力种族（B 层，18 条）

tianyuan_city, jiuxian_mount, human_alliance, demon_alliance, flying_spirits, sky_peng, mayfly, wood_spirits, crystal_clan, gold_sea, devil_cities, ancient_demons, true_spirit_cult, star_palace_exile, inverse_star, mulan_beast_god, tianlan_temple, black_abyss_legion。

### 5.5 妖兽 13 级（C 层）

1 级≈炼气 … 7 级化形劫≈结丹后≈元婴战力起点 … 11～13≈化神+。  
掉落：妖丹自 5 级起；化形任务链。

### 5.6 称号链

- **妖族**：妖将→妖王→妖皇→真仙→金仙…  
- **魔族**：魔将→魔帅→魔尊→圣祖→始祖  

---

## 6. 功法 · 法术 · 神通（技能树）

### 6.1 三层结构

1. **功法** = 职业主线 + 属性门槛  
2. **法术** = 7 槽主动技  
3. **神通/秘术** = 化神栏 / 稀有终极  

### 6.2 五大流派

佛（舍利子）、道、儒、魔、妖（如疾风九变）。

### 6.3 已整理术法规模

- 约 **125** 条 JSON（炼气/筑基/结丹/元婴/化神/特殊/通用）。  
- **玄阴经** 18 条链（炼气～元婴 + 鬼道 + 炼尸）。  
- **补充**：隔音、敛气、天火、五遁、惊魂咒、法宝诀等。  
- **化神神通** 21 条（独立栏，非 7 槽）。

### 6.4 关键功法包

长春功、青元剑诀、大衍诀、托天魔功、元磁神光、通宝诀、换形诀等。

### 6.5 字段规范

`id, name, type, attribute, source, summary, realm_min, cost, cooldown_sec, effect, tags, required_path, forbidden_path`。

---

## 7. 物品体系

### 7.1 法宝分层

法器 1～3 阶 → 法宝 1～6 阶 → 通天灵宝；本命/二手/符宝/真宝/古宝/器灵。

### 7.2 符箓（11.4）

初/中/高 × 下/中/上；攻击/防御/禁锢/功能；三清雷霄符终局。

### 7.3 丹药

修炼丹、战斗丹（回煞、抽髓、毒龙珠、定神珠）、寿元（长生、血气、回阳真水）、毒（断魂、修髓）。

### 7.4 傀儡（11.7）

曲魂→巨猿→巨龟→石灵→机关→大衍人形→通灵；混元钵核心。

### 7.5 阵法（11.5）

困/杀/防/秘术阵；便携阵盘；与术法 id 绑定。

### 7.6 货币

低→中→高→极品灵石 **1:100**；拍卖分层经济。

---

## 8. 社会与势力（摘要）

### 8.1 组织形态

散修、修仙家族/世家、宗门（执事→领事→筑基核心…）。

### 8.2 天南

七玄门、越国七派、魔道六宗（鬼灵门、合欢宗等）。

### 8.3 其他

千竹教（傀儡）、乱星海星宫/逆星盟、大晋世家、灵界天渊城。

### 8.4 四大热门职业

阵法师、炼丹师、炼器师、制符师 → 副职经验与配方解锁。

---

## 9. 地理与秘境（模板四要素）

开放条件 + 环境规则 + 分层探索 + 稀有掉落。

| 秘境 | 区域 | 备注 |
|---|---|---|
| 血色禁地 | 天南 | 五年一开，筑基下 |
| 虚天殿 | 乱星海 | 三百年 |
| 坠魔谷 | 天南 | 古魔战场 |
| 昆吾山 | 大晋 | 封魔 |
| 地渊/冥河/广寒界/魔金山/小修罗界 | 灵界 | 高阶 |

---

## 10. 生物与 Boss

### 10.1 梯度

低阶野怪 → 中阶猎物 → 妖王 → 驯养灵宠（噬金虫、啼魂兽等）。

### 10.2 Boss 三阶段

普攻 → 领域/召唤 → 天赋神通。

### 10.3 名录（节选）

婴鲤兽、银翅夜叉、金身月尸、六翼霜蚣、天虎兽、圣禽、蜃兽、雷劫蛟等。

---

## 11. 战斗与状态

### 11.1 四层战斗

术法 7 槽 + 符箓 + 阵法/阵盘 + 法宝技/傀儡。

### 11.2 状态 ID（统一）

burn, frozen, soul_shock, illusion, karma, demonic_qi, foundation_unstable, marrow_drain, seal_nascent, conceal_qi 等。

### 11.3 天劫 × 种族

见第 5 节与煞气、御雷道具联动。

---

## 12. 待落地数据文件清单

| 路径（建议） | 内容 |
|---|---|
| `techniques/*.json` | 已有 125+，补玄阴/补充 |
| `talismans.json` | ~28 |
| `artifact_active_skills.json` | ~14 |
| `puppet_definitions.json` | T0～T6 |
| `combat_elixirs.json` | ~12 |
| `status_effects.json` | ~20 |
| `races/playable_races.json` | 8 |
| `races/ancestry_passives.json` | 12 |
| `races/faction_species.json` | 18 |
| `races/beast_tiers.json` | 13 |
| `races/constitutions.json` | 16 |
| `true_spirit_bloodlines.json` | 5～8 |
| `race_region_events.json` | 20+ |
| `race_reputation_shops.json` | 声望商店 |

---

## 13. 与模组实现对齐（当前已有）

- 灵力/修为/境界/突破、打坐蒲团、灵石四品与兑换、Curios 护符、Patchouli、JEI。  
- 物品：回灵丹、凝气丹、火弹符、金甲符、疾行符等。  
- **缺口**：完整人界境界名、术法 effect 实装、种族/声望、秘境实例、NPC 宗门。

---

## 14. 版本

- **v1**（2026-06-30）：汇总对话设定，建立维护文档。  
- 下一版目标：补全 `data/` JSON 草案 + 时间线章节。

---

*本文件为设定维护主文档；缺口与扩充优先级见 `EXPANSION_ROADMAP.md`。*