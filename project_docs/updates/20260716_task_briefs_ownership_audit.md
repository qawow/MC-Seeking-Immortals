# 20260716 任务简报核对增补与 worktree 路径修正

## 变更类别

docs-only（项目文档;无代码/资源/构建逻辑改动,不升 mod_version,协议保持 19）。

## 背景

用户要求"完整读一遍文本材料,拆成 N 份互不重叠任务简报,每任务建 git worktree"。检查发现 2026-07-15 已有一版 17 份简报(M00–M16)与 17 个 worktree(f258f0fa),但存在两类问题:一是简报/进度文档里的 worktree 路径写的是 Windows 侧 `/root/mc-mod-worktrees/`,当前 WSL 环境实际在 `/root/mc-mod-worktrees/`;二是本轮对 `文本材料/` 640 文件(数据 469 JSON + 169 MD)的分域复读发现多份简报的"语料侧涉及文件"清单只列了代表文件,大量同域文件未声明所有权,会造成后续并行开发时文件归属歧义。

## 更新内容

1. **路径修正**:全部 18 个简报文件 + `step_progress.md` 第 450 条 + `updates/20260715_task_briefs_and_worktrees.md` 中 `D:/codex/mc-mod-worktrees` → `/root/mc-mod-worktrees`(与 `git worktree list` 实际一致)。
2. **语料所有权增补**(基于本轮全量分域通读,只增不改归属原则,无一文件двух主):
   - M01:补 cultivation_progression 10 境界字段说明、spirit_roots_catalog(10 元素×5 品阶+觉醒率 0.08)、tribulation 全 5 文件、realm_breakthrough_v98 结构说明、novel_cultivation_setting/learn_requirements waves。
   - M02:补 method_layer_technique_matrix_v125–v130(v130 权威)、method_path_taxonomy_v123、technique_method_bridge、six_sects/tiannan seven sects technique packs、technique_field_fill_report_v138、manual_conflict_matrix_v100。
   - M04:补 pill_formulary_v92、pill_chain_alignment_v145、flame_water_catalog_v93、refinement_system/failure_loot/refine_manual_index/forge_artifact_priority、cave_garden_production_v95、crafting_blueprints_v94、craft_daily_loops。
   - M05:补 market_shelves_v94、market_shelf_price_align_v144(货架价格权威)、merchant_shops_guide_v113、auction_catalog_v93、faction_shop_stock_v94、dajin_economy_bands、tianyuan_merit_economy、item_economy_index_v92。
   - M06:补 region_cards_deep_v116/v138/detail_v139/v140(v140 权威)、region_structure_presence_v137、region_tianyuan_diyuan_v105、region_xinggong_dajin_v108、region_chaotic_sea_liulian_v107、tianyuan_daily_events。
   - M07:补 multiblock v132–v135 全系列(v135 权威 86 结构)+ operational_states_v134 状态机(intact/damaged/critical/disabled + 8 交互动词)+ multiplayer_sequences + build_materials/material_prices。
   - M08:补 faction_pages_v117、faction_lore_compendium_v144(lore 权威)、tiannan 七宗三文件、luoyun_late_game_pack_v127、dajin_line_packs_v129、reputation_thresholds_v129、opening_list_v128、xianjie_reputation_v130、identity_reputation_v95(声望轨/伪装/悬赏)、三个战争数据文件(阶段/边权归 M08,战役任务化归 M11)。
   - M09:补 deep dive 三个波次系列(v104–v110/v111/deep_v116/third_v117)、layer_diagrams v139/v140/v143(v143 权威)、secret_realm_loot_v93、beast_boss_tier_secret_realm_map、boss_extra_loot_index、storage_bag_loot_templates_v94;注明 ancient_treasure_index 法宝段归 M15。
   - M10:补 bestiary 全系列明细(compendium_v95→v122 波次+index_v97+summary_v101+key_novel_v145 权威)、ecology_habitats_v95、novel_beast_puppet_waves、spirit_demon_economy_v94;修正文件名笔误(thirteen_tier_map→beast_thirteen_tier_map、companion_growth_v99→spirit_beast_growth_v99)。
   - M11:补 quest_chains 四版本演进(playable_v141 为可玩步骤权威)、quest_lines_full_descriptions_v146/v147、quest_reward_economy_v142、item_price_quest_crosswalk_v143、daily_quest_templates、tiannan_faction_quests、endgame 三文件(v143/v144)、path_deity_huoyu_v106、xianjie_quest_chains_v130、opening_starts_and_paths_v137、dialogue_effect_quest_links_v140(数据归 M11,运行时归 M12)。
   - M12:补 npc_dialogue_templates_v138 archetype 结构说明(9 类 lines+binds+branch_tree_id)、npc_dialogue_branches_v139 condition_ops 枚举(10 种)、named_npc_seeds_v137、npc_vendor_roster_v96。
   - M13:补 dimension_travel_methods_v136(六层宇宙观+交通目录权威)、dimension_travel_costs_v137、immortal_realms_v130、xianjie_immortal_packs_v130、spirit_realm_diyuan_compendium、ascension_flow 流程说明(含偷渡支线)。
   - M14:**关键更正**——`status_effects.json` 语料中尚不存在(WORLD_SETTING_BIBLE 第 488 行列为规划),统一状态 id 清单实际在 WORLD_SETTING_BIBLE 第 471 行;M14 首项改为"据清单自建权威 JSON 并走 M00 管线登记"。补 consumable_combat_items_v94、identity_reputation_v95 敛息/换形字段。
   - M15:补 artifact_catalog_v92、artifact_tier_map、tier_rules 阶梯说明(法器低/中→法宝→古宝→灵宝)、tiannan_faction_specialty、talisman_treasure_templates、ancient_treasure_index 法宝段。
   - M16:补 visual 五件套 v118–v122 明细+VISUAL_BIBLE v122 权威、asset_texture 三件套(330 注册项队列)、item_descriptions v119–v147 归属(v95–v118 归 M03)、catalog_description_align、novel_canon_audit/checklist、setting_crosslink_matrix_v137、merge_bridge_v109。
3. **所有权边界注记**:跨模块共用文件加"谁拥有哪个字段段落"注记(如 dialogue_effect_quest_links_v140:数据 M11/运行时 M12;战争文件:阶段数据 M08/战役任务 M11;ancient_treasure_index:掉落位 M09/法宝条目 M15;item_descriptions:v95–v118 M03/v119+ M16)。

## 复读验证要点(本轮全量勘察结论)

- 语料规模:640 文件(469 JSON/169 MD/2 CSV,约 25MB),批次 v147,schema v55。
- 关键量级:techniques 747 条(19 分卷)、cultivation_methods 136 部、物品 id_index 总计约 900+(丹 114/法宝 217/材料 214/草 75+19/符 47/消耗 55 等)、bestiary index 1848 条、秘境 19、多方块 86、宗门 20+、任务链 62+任务线 35、region cards 23、维度 11。
- 版本规则确认:`_vNNN` 为增量波次(item_descriptions_v100 有 90 条、v147 只 1 条),不互相取代;同主题多版本时权威版已写入各简报(matrix v130、shelf_align v144、layer_diagrams v143、region detail v140、multiblock v135、quest playable v141、story map v145、lore compendium v144、VISUAL_BIBLE v122、bestiary key v145)。
- 17 份简报覆盖交叉检查:640 语料文件各有唯一属主,无双主;M00(管线)与 M16(只读收口)首尾封闭。

## 版本与协议

- mod_version:保持 `0.1.504`(docs-only 不升版)。
- ModNetwork.PROTOCOL_VERSION:保持 `19`。

## 备份

`.bak/20260716_task_briefs_paths/`(修改前的 HEAD 版本:18 简报 + step_progress.md + 20260715 更新说明)。

## 后续

- 各 worktree 分支(task/m00–m16)当前基于 f258f0fa(旧简报版);本次修订提交进 main 后,各分支应在开工前 `git merge main`(或由各 worktree 内代理自行合并)以拿到修订版简报。worktree 目录与分支本体无需重建。
- 全局阻塞不变:M00 首项(747 vs 346 功法基线 + body.json.json 双后缀)未修复前,其他模块动 `src/` 需按 README 注明聚焦测试范围。
