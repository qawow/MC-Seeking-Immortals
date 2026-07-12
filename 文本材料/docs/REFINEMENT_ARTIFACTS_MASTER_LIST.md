# 炼器 / 法宝 全量清单（数据层）
> 工作区 `data/` + 关联文档；法宝 **169** 条，炼器配方 **73** 条。
## 一、相关数据文件
- `artifacts_catalog.json` — 法宝总目录 169- `refinement_recipes.json` — 炼器配方 73- `refinement_system.json` — 炼器多方块与错配规则- `refine_manual_index.json` — 配方↔手册 forge_grade- `refinement_failure_loot.json` — 炼器失败掉落- `artifact_tier_rules.json` — 法器/法宝/符宝/古宝/本命规则- `artifact_eleven_tier_map.json` — 十一级 game_tier 映射- `artifact_taxonomy_111.json` — 汇编 §11.1 四类分类- `talisman_treasure_templates.json` — 符宝模板- `ancient_treasure_index.json` — 古宝/碎片/仿索引- `artifact_realm_drops.json` — 秘境法宝权重池- `artifact_faction_specialty.json` — 宗门炼器倾向- `wanbao_auction_artifacts.json` — 万宝楼/大晋拍卖- `forge_artifact_priority.json` — Forge P0/P1/P2- `moditems_artifacts_draft.json` — ModItems 注册草稿 22- `flight_vehicles.json` — 灵舟/云轿/御风车 6
**文档**：`docs/ARTIFACTS_FANREN.md`、`docs/ARTIFACTS_AND_PILLS.md`
## 二、炼器系统（refinement_system.json）
- **工作站**：炼器阵 `refinement_forge` + 锻心 `refinement_anvil` + 灵风囊 `refinement_bellows`（品阶 1–6）
- **错配**：阵品阶不足 / 境界不足 / 缺高阶手册 / 古宝无残片 → 失败与材料损耗
- **成功率因子**：阵品阶、炼器技能、材料品质、神识、手册解锁
## 三、品阶规则摘要（artifact_tier_rules.json）

## 四、炼器配方全表（73）
| recipe_id | 成品 id | 显示名 | 最低境界 | forge_grade | 成功率 |
|-----------|---------|--------|----------|-------------|--------|
| refine_cloud_boots | cloud_boots | 踏云靴 | QI_REFINING | 1 | 0.68 |
| refine_flying_needle_set | flying_needle_set | 飞针匣 | QI_REFINING | 1 | 0.65 |
| refine_flying_sword_low | flying_sword_low | 低阶飞剑 | QI_REFINING | 1 | 0.7 |
| refine_giant_ape_token | giant_ape_puppet_token | 巨猿傀儡符 | QI_REFINING | 1 | 0.55 |
| refine_green_bamboo_leaf_sword | green_bamboo_leaf_sword | 竹叶剑 | QI_REFINING | 1 | 0.7 |
| refine_huangsi_robe | huangsi_robe_artifact | 黄丝衫 | QI_REFINING | 1 | 0.7 |
| refine_qingye_fan | qingye_leaf_fan | 青叶法器 | QI_REFINING | 1 | 0.72 |
| refine_snake_pearl | snake_pearl | 蛇珠 | QI_REFINING | 1 | 0.62 |
| refine_spirit_gathering_bead | spirit_gathering_bead | 定神珠 | QI_REFINING | 1 | 0.65 |
| refine_azure_ice_sword | azure_ice_sword | 玄冰剑 | FOUNDATION | 2 | 0.44 |
| refine_azure_rope_net | azure_rope_net | 青索网 | FOUNDATION | 2 | 0.5 |
| refine_beast_soul_bell | beast_soul_bell | 兽魂铃 | FOUNDATION | 2 | 0.48 |
| refine_beast_taming_whip | beast_taming_whip | 御兽鞭 | FOUNDATION | 2 | 0.48 |
| refine_bedrock_shield | bedrock_shield | 磐石盾 | FOUNDATION | 2 | 0.46 |
| refine_black_boots | black_boots | 乌靴 | FOUNDATION | 2 | 0.48 |
| refine_black_gold_shield | black_gold_shield | 乌金盾 | FOUNDATION | 2 | 0.48 |
| refine_dark_iron_ring | dark_iron_ring | 玄铁环 | FOUNDATION | 2 | 0.5 |
| refine_fire_crow_fan | fire_crow_fan | 火鸦扇 | FOUNDATION | 2 | 0.46 |
| refine_fire_rain_needles | fire_rain_needles | 火雨针 | FOUNDATION | 2 | 0.45 |
| refine_flat_crown_replica | flat_crown_replica | 平山冠（仿） | FOUNDATION | 2 | 0.5 |
| refine_lengyue_blade | lengyue_blade | 冷月刀 | FOUNDATION | 2 | 0.44 |
| refine_lieyang_sword | lieyang_short_sword | 烈阳剑 | FOUNDATION | 2 | 0.44 |
| refine_moon_shadow_disk | moon_shadow_disk | 月影轮 | FOUNDATION | 2 | 0.46 |
| refine_poluo_beads | poluo_beads | 婆罗珠 | FOUNDATION | 2 | 0.45 |
| refine_potian_shovel | potian_shovel | 破天锹 | FOUNDATION | 2 | 0.44 |
| refine_qingning_mirror | qingning_mirror | 青凝镜 | FOUNDATION | 2 | 0.42 |
| refine_silver_giant_sword | silver_giant_sword | 银色巨剑 | FOUNDATION | 2 | 0.38 |
| refine_silver_spirit_mirror | silver_spirit_mirror | 银灵镜 | FOUNDATION | 2 | 0.45 |
| refine_soul_capturing_bell | soul_capturing_bell | 摄魂铃 | FOUNDATION | 2 | 0.4 |
| refine_soul_gathering_bowl | soul_gathering_bowl | 聚魂钵 | FOUNDATION | 2 | 0.4 |
| refine_soul_summon_bell | soul_summon_bell | 引魂钟 | QI_REFINING | 2 | 0.52 |
| refine_spirit_beast_bridle | spirit_beast_bridle | 御兽环 | FOUNDATION | 2 | 0.5 |
| refine_storage_bracelet | storage_bracelet_low | 储物镯 | QI_REFINING | 2 | 0.55 |
| refine_vajra_shield | vajra_shield | 金刚罩 | FOUNDATION | 2 | 0.42 |
| refine_wind_escape_sail | wind_escape_sail | 风遁帆 | FOUNDATION | 2 | 0.45 |
| refine_xuantie_shield | xuantie_flying_shield | 玄铁飞天盾 | FOUNDATION | 2 | 0.42 |
| refine_yellow_umbrella | yellow_umbrella | 黄罗伞 | FOUNDATION | 2 | 0.5 |
| refine_bone_wind_cart | bone_wind_cart | 御风车 | FOUNDATION | 3 | 0.4 |
| refine_demon_ape_armor | demon_ape_armor | 铁猿甲 | FOUNDATION | 3 | 0.4 |
| refine_evil_illusion_mirror | evil_illusion_mirror | 邪幻镜 | FOUNDATION | 3 | 0.35 |
| refine_giant_turtle_core | giant_turtle_puppet_core | 巨龟傀儡核心 | FOUNDATION | 3 | 0.42 |
| refine_green_bamboo_sword | green_bamboo_cloud_sword | 青竹灵云剑（仿） | FOUNDATION | 3 | 0.35 |
| refine_invisible_needles | invisible_needle_set | 无形针匣 | FOUNDATION | 3 | 0.35 |
| refine_seven_star_disk | seven_star_disk | 七星盘 | FOUNDATION | 3 | 0.38 |
| refine_dragon_scale_armor | dragon_scale_armor | 龙鳞甲 | CORE_FORMATION | 4 | 0.26 |
| refine_glazed_guard | glazed_guard_shield | 琉璃罩 | CORE_FORMATION | 4 | 0.28 |
| refine_gold_demon_chain | gold_demon_chain | 金蚨子母刃 | CORE_FORMATION | 4 | 0.3 |
| refine_gold_light_brick | gold_light_brick | 金光砖符宝 | CORE_FORMATION | 4 | 0.22 |
| refine_hunyuan_bowl | hunyuan_bowl | 混元钵 | CORE_FORMATION | 4 | 0.25 |
| refine_hunyuan_replica | hunyuan_bowl_replica | 混元钵（仿） | CORE_FORMATION | 4 | 0.25 |
| refine_peerless_knives | peerless_flying_knives | 无双飞刀 | CORE_FORMATION | 4 | 0.28 |
| refine_phoenix_feather_fan | phoenix_feather_fan | 凤羽扇 | CORE_FORMATION | 4 | 0.28 |
| refine_red_thread_replica | red_thread_needles_replica | 红线遁光针（仿） | FOUNDATION | 4 | 0.22 |
| refine_scarlet_dragon | scarlet_dragon_blade | 赤龙刃 | CORE_FORMATION | 4 | 0.3 |
| refine_talisman_demon_seal | talisman_treasure_demon_seal | 封魔符宝 | CORE_FORMATION | 4 | 0.18 |
| refine_talisman_fire_spear | talisman_treasure_fire_spear | 火蛟矛符宝 | CORE_FORMATION | 4 | 0.22 |
| refine_talisman_golden_wheel | talisman_treasure_golden_wheel | 金轮符宝 | CORE_FORMATION | 4 | 0.24 |
| refine_talisman_ice_shield | talisman_treasure_ice_shield | 玄冰盾符宝 | CORE_FORMATION | 4 | 0.24 |
| refine_talisman_soul_charm | talisman_treasure_soul_charm | 定魂符宝 | CORE_FORMATION | 4 | 0.25 |
| refine_talisman_thunder_rod | talisman_treasure_thunder_rod | 霹雳神雷符宝 | CORE_FORMATION | 4 | 0.2 |
| refine_thousand_bee_needles | thousand_bee_needles | 千蜂针 | CORE_FORMATION | 4 | 0.3 |
| refine_void_cold_jade_pendant | void_palace_cold_jade_pendant | 寒玉佩 | CORE_FORMATION | 4 | 0.2 |
| refine_xuanguang_replica | xuanguang_mirror_replica | 玄光镜（仿） | CORE_FORMATION | 4 | 0.18 |
| refine_four_symbols_ruler | four_symbols_ruler_replica | 四象尺（仿） | CORE_FORMATION | 5 | 0.12 |
| refine_ice_fire_orb | ice_fire_dual_orb | 冰火珠 | CORE_FORMATION | 5 | 0.15 |
| refine_natal_embryo | natal_artifact_embryo | 本命法宝胚 | CORE_FORMATION | 5 | 0.15 |
| refine_natal_sword_embryo | natal_sword_embryo | 本命飞剑胚 | CORE_FORMATION | 5 | 0.14 |
| refine_nine_dragon_replica | nine_dragon_cauldron_replica | 九龙神火罩（仿） | CORE_FORMATION | 5 | 0.1 |
| refine_thunder_pearl | thunder_pearl_talisman | 天雷子 | CORE_FORMATION | 5 | 0.12 |
| refine_xuanguang_shard | xuanguang_mirror | 玄光镜（残片重铸） | CORE_FORMATION | 5 | 0.12 |
| refine_xuanhuang_shard | xuanhuang_mirror | 玄黄镜（残片重铸） | NASCENT_SOUL | 5 | 0.08 |
| refine_three_flame_fan | three_flame_fan_replica | 三焰扇（仿） | VOID_REFINEMENT | 6 | 0.08 |
| refine_void_refining_bell | void_refining_bell | 炼虚钟 | VOID_REFINEMENT | 6 | 0.06 |

## 五、法宝目录全表（169）
| id | 显示名 | tier | type | realm_min | game_tier | 可炼 |
|----|--------|------|------|-----------|-----------|------|
| ancient_forest_disk_shard | 万森轮盘碎片 | ancient_treasure | material_artifact | NASCENT_SOUL | 10 | — |
| ancient_sea_halberd_shard | 平海戈碎片 | ancient_treasure | material_artifact | NASCENT_SOUL | 10 | — |
| black_moon_ruler | 黑月尺 | ancient_treasure | offense | NASCENT_SOUL | 10 | — |
| demon_refining_pot | 炼妖壶（仿） | ancient_treasure | beast_refine | NASCENT_SOUL | 10 | — |
| falling_star_ring | 落星环 | ancient_treasure | offense | NASCENT_SOUL | 10 | — |
| flat_mountain_seal_replica | 平山印（仿） | ancient_treasure | offense | NASCENT_SOUL | 10 | — |
| forbidden_demon_ring_set | 禁魔环（仿） | ancient_treasure | control | NASCENT_SOUL | 10 | — |
| green_copper_bell | 青铜古钟 | ancient_treasure | control | CORE_FORMATION | 10 | — |
| jade_ruyi_replica | 玉如意（仿） | ancient_treasure | defense | NASCENT_SOUL | 10 | — |
| kunwu_seal_fragment | 昆吾令碎片 | ancient_treasure | material_artifact | CORE_FORMATION | 10 | — |
| nine_dragon_cauldron_replica | 九龙神火罩（仿·古宝模板） | ancient_treasure | defense | CORE_FORMATION | 10 | ✓ |
| nine_dragon_cauldron_shard | 九龙神火罩碎片 | ancient_treasure | material_artifact | CORE_FORMATION | 10 | — |
| two_luminaries_shuttle | 日月梭（仿） | ancient_treasure | offense | NASCENT_SOUL | 10 | — |
| xuanguang_mirror | 玄光镜 | ancient_treasure | soul_attack | CORE_FORMATION | 10 | ✓ |
| xuanhuang_mirror | 玄黄镜 | ancient_treasure | soul_destroy | NASCENT_SOUL | 10 | ✓ |
| yuan_yang_veil_replica | 元阳纱（仿） | ancient_treasure | defense | CORE_FORMATION | 9 | — |
| natal_artifact_embryo | 本命法宝胚 | high | material_artifact | CORE_FORMATION | 9 | ✓ |
| natal_sword_embryo | 本命飞剑胚 | high | natal_slot | CORE_FORMATION | 9 | ✓ |
| nine_dragon_barrier_token | 九龙罩阵符 | high | formation_token | CORE_FORMATION | 9 | — |
| six_ding_armor_talisman | 六丁天甲符器 | high | talisman_treasure | CORE_FORMATION | 9 | — |
| demon_suppress_tower_replica | 镇妖塔（仿） | high | beast_control | CORE_FORMATION | 8 | — |
| four_symbols_ruler_replica | 四象尺（仿） | high | control | CORE_FORMATION | 8 | ✓ |
| great_shift_token | 大挪移令 | high | teleport_protection | CORE_FORMATION | 8 | — |
| great_shift_token_replica | 大挪移护符（仿） | high | teleport_protection | CORE_FORMATION | 8 | — |
| ice_fire_dual_orb | 冰火珠 | high | offense | CORE_FORMATION | 8 | ✓ |
| mirror_space_scroll | 画轴芥子器 | high | storage | CORE_FORMATION | 8 | — |
| nascent_soul_talisman_treasure | 元婴符宝（模板） | high | talisman_treasure | NASCENT_SOUL | 8 | — |
| silver_moon_wolf | 银月天狼 | high | beast_spirit | CORE_FORMATION | 8 | — |
| soul_scattering_mirror_shard | 灭魂镜碎片 | high | material_artifact | CORE_FORMATION | 8 | — |
| space_rift_compass | 裂隙罗盘 | high | utility | NASCENT_SOUL | 8 | — |
| spirit_talisman_vessel | 化灵符器 | high | defense | CORE_FORMATION | 8 | — |
| sun_essence_stone_inlay | 太阳精石镶件 | high | material_artifact | CORE_FORMATION | 8 | — |
| talisman_treasure_demon_seal | 封魔符宝 | high | talisman_treasure | CORE_FORMATION | 8 | ✓ |
| thunder_pearl_talisman | 天雷子 | high | talisman_treasure | FOUNDATION | 8 | ✓ |
| void_heaven_ruler_shard | 虚天尺碎片 | high | material_artifact | CORE_FORMATION | 8 | — |
| xuanguang_mirror_replica | 玄光镜（仿） | high | soul_attack | CORE_FORMATION | 8 | ✓ |
| xuanguang_mirror_shard | 玄光镜碎片 | high | material_artifact | CORE_FORMATION | 8 | — |
| xuanhuang_mirror_shard | 玄黄镜碎片 | high | material_artifact | CORE_FORMATION | 8 | — |
| bixie_blade | 辟邪神兵（仿） | high | offense | CORE_FORMATION | 7 | — |
| blood_thunder_pearl | 血雷珠 | high | offense | CORE_FORMATION | 7 | — |
| celestial_silk_armor | 天蚕法衣 | high | defense | CORE_FORMATION | 7 | — |
| cloud_sedan_token | 云轿令牌 | high | vehicle_key | CORE_FORMATION | 7 | — |
| demonized_ancient_sword | 魔化古剑 | high | offense | CORE_FORMATION | 7 | — |
| dragon_scale_armor | 龙鳞甲 | high | defense | CORE_FORMATION | 7 | ✓ |
| dual_element_ring | 两仪环（仿） | high | defense | CORE_FORMATION | 7 | — |
| fire_cloud_banner | 火云旗 | high | formation_deploy | CORE_FORMATION | 7 | — |
| five_element_orb | 五行灵珠 | high | offense | CORE_FORMATION | 7 | — |
| geng_gold_inlay | 庚精镶件 | high | material_artifact | CORE_FORMATION | 7 | — |
| glazed_guard_shield | 琉璃罩 | high | defense | CORE_FORMATION | 7 | ✓ |
| gold_demon_chain | 金蚨子母刃 | high | offense | CORE_FORMATION | 7 | ✓ |
| hunyuan_bowl | 混元钵 | high | hybrid_puppet_core | CORE_FORMATION | 7 | ✓ |
| hunyuan_bowl_replica | 混元钵（仿） | high | hybrid_puppet_core | CORE_FORMATION | 7 | ✓ |
| illusion_transform_clay | 化形泥 | high | material_artifact | CORE_FORMATION | 7 | — |
| phoenix_feather_fan | 凤羽扇 | high | offense | CORE_FORMATION | 7 | ✓ |
| purple_fire_bead | 紫罗天火珠 | high | offense | CORE_FORMATION | 7 | — |
| scarlet_dragon_blade | 赤龙刃 | high | offense | CORE_FORMATION | 7 | ✓ |
| sky_bracelet_replica | 弥天镯（仿） | high | defense | CORE_FORMATION | 7 | — |
| talisman_treasure_flat_mountain | 平山印符宝 | high | talisman_treasure | CORE_FORMATION | 7 | — |
| talisman_treasure_thunder_rod | 霹雳神雷符宝 | high | talisman_treasure | FOUNDATION | 7 | ✓ |
| thunder_dragon_horn | 雷蛟角枪 | high | thunder | CORE_FORMATION | 7 | — |
| vermillion_bird_ring | 朱雀环（仿） | high | offense | CORE_FORMATION | 7 | — |
| void_key | 虚天殿密钥 | high | quest_key | CORE_FORMATION | 7 | — |
| void_palace_chain | 镇海链 | high | control | CORE_FORMATION | 7 | — |
| void_palace_cold_jade_pendant | 寒玉佩 | high | defense | CORE_FORMATION | 7 | ✓ |
| gold_light_brick | 金光砖符宝 | high | talisman_treasure | FOUNDATION | 6 | ✓ |
| green_bamboo_cloud_sword | 青竹灵云剑（仿） | high | flying_sword | FOUNDATION | 6 | ✓ |
| peerless_flying_knives | 无双飞刀 | high | offense | CORE_FORMATION | 6 | ✓ |
| red_thread_needles_replica | 红线遁光针（仿） | high | offense | FOUNDATION | 6 | ✓ |
| talisman_treasure_fire_spear | 火蛟矛符宝 | high | talisman_treasure | FOUNDATION | 6 | ✓ |
| talisman_treasure_golden_wheel | 金轮符宝 | high | talisman_treasure | FOUNDATION | 6 | ✓ |
| talisman_treasure_ice_shield | 玄冰盾符宝 | high | talisman_treasure | FOUNDATION | 6 | ✓ |
| thousand_bee_needles | 千蜂针 | high | offense | CORE_FORMATION | 6 | ✓ |
| talisman_treasure_soul_charm | 定魂符宝 | high | talisman_treasure | FOUNDATION | 5 | ✓ |
| flying_needle_set | 飞针匣 | low | offense | QI_REFINING | 3 | ✓ |
| quhun_iron_puppet | 驱魂铁傀（战利品） | low | puppet_summon | QI_REFINING | 3 | — |
| spirit_repelling_mirror | 驱灵镜 | low | anti_illusion | QI_REFINING | 3 | — |
| storage_bracelet_low | 储物镯（低阶） | low | storage | QI_REFINING | 3 | ✓ |
| tortoise_shell_shield | 龟壳盾 | low | defense | QI_REFINING | 3 | — |
| wood_spirit_staff | 碧玉杖 | low | offense | QI_REFINING | 3 | — |
| artifact_repair_kit | 法宝修补匣 | low | utility | QI_REFINING | 2 | — |
| auction_sealed_hammer | 拍卖行密锤 | low | quest_key | QI_REFINING | 2 | — |
| aura_conceal_cloth | 敛气残帛 | low | utility | QI_REFINING | 2 | — |
| cloud_boots | 踏云靴 | low | movement | QI_REFINING | 2 | ✓ |
| copper_bell_charm | 铜钟护符 | low | defense | QI_REFINING | 2 | — |
| flying_sword_low | 低阶飞剑 | low | flying_sword | QI_REFINING | 2 | ✓ |
| giant_ape_puppet_token | 巨猿傀儡符 | low | puppet_summon | QI_REFINING | 2 | ✓ |
| green_bamboo_leaf_sword | 竹叶剑 | low | flying_sword | QI_REFINING | 2 | ✓ |
| huangsi_robe_artifact | 黄丝衫 | low | defense | QI_REFINING | 2 | ✓ |
| iron_bone_claw | 铁骨爪 | low | offense | QI_REFINING | 2 | — |
| iron_chain_sickle | 铁链镰 | low | offense | QI_REFINING | 2 | — |
| mixed_element_bowl_low | 混元药钵（凡） | low | utility | QI_REFINING | 2 | — |
| montain_five_friends_token | 蒙山五友信物 | low | quest_key | QI_REFINING | 2 | — |
| qingye_leaf_fan | 青叶法器 | low | offense | QI_REFINING | 2 | ✓ |
| snake_pearl | 蛇珠 | low | poison | QI_REFINING | 2 | ✓ |
| spirit_gathering_bead | 定神珠 | low | anti_illusion | QI_REFINING | 2 | ✓ |
| spirit_lamp | 引灵灯 | low | utility | QI_REFINING | 2 | — |
| wanbao_pavilion_coupon | 万宝楼鉴宝券 | low | utility | QI_REFINING | 2 | — |
| artifact_spirit_awakening_incense | 启灵香 | mid | utility | CORE_FORMATION | 6 | — |
| evil_mirage_mirror_shard | 邪幻镜碎片 | mid | material_artifact | FOUNDATION | 6 | — |
| spirit_nourish_silk | 养宝丝 | mid | utility | CORE_FORMATION | 6 | — |
| spirit_sand_hourglass | 流沙漏 | mid | control | FOUNDATION | 6 | — |
| azure_ice_sword | 玄冰剑 | mid | flying_sword | FOUNDATION | 5 | ✓ |
| bedrock_shield | 磐石盾 | mid | defense | FOUNDATION | 5 | ✓ |
| blood_drinking_hook | 饮血钩 | mid | offense | FOUNDATION | 5 | — |
| bone_wind_cart | 御风车 | mid | vehicle_key | FOUNDATION | 5 | ✓ |
| demon_ape_armor | 铁猿甲 | mid | defense | FOUNDATION | 5 | ✓ |
| demon_blood_saber | 魔血刀 | mid | offense | FOUNDATION | 5 | — |
| demon_subduing_staff | 降魔杵 | mid | offense | FOUNDATION | 5 | — |
| evil_illusion_mirror | 邪幻镜 | mid | illusion | FOUNDATION | 5 | ✓ |
| fire_crow_fan | 火鸦扇 | mid | offense | FOUNDATION | 5 | ✓ |
| fire_rain_needles | 火雨针 | mid | offense | FOUNDATION | 5 | ✓ |
| flat_crown | 平山冠 | mid | defense | FOUNDATION | 5 | — |
| flat_crown_replica | 平山冠（仿） | mid | defense | FOUNDATION | 5 | ✓ |
| giant_sword_gate_relic | 巨剑门残刃 | mid | material_artifact | FOUNDATION | 5 | — |
| invisible_needle_set | 无形针匣 | mid | offense | FOUNDATION | 5 | ✓ |
| lengyue_blade | 冷月刀 | mid | offense | FOUNDATION | 5 | ✓ |
| lieyang_short_sword | 烈阳剑（法器） | mid | offense | FOUNDATION | 5 | ✓ |
| mixed_element_ruler_low | 量天尺（法器） | mid | offense | FOUNDATION | 5 | — |
| moon_shadow_disk | 月影轮 | mid | offense | FOUNDATION | 5 | ✓ |
| potian_shovel | 破天锹 | mid | offense | FOUNDATION | 5 | ✓ |
| reverse_star_assassin_blade | 逆星刺刃 | mid | offense | FOUNDATION | 5 | — |
| seven_star_disk | 七星盘 | mid | formation_deploy | FOUNDATION | 5 | ✓ |
| silver_giant_sword | 银色巨剑 | mid | flying_sword | FOUNDATION | 5 | ✓ |
| sky_cleaver_axe | 开天斧（法器） | mid | offense | FOUNDATION | 5 | — |
| soul_capturing_bell | 摄魂铃 | mid | soul_attack | FOUNDATION | 5 | ✓ |
| soul_devouring_chain | 噬魂链 | mid | yin | FOUNDATION | 5 | — |
| soul_refining_banner | 炼魂幡 | mid | yin | FOUNDATION | 5 | — |
| star_palace_standard_sword | 星宫制式飞剑 | mid | flying_sword | FOUNDATION | 5 | — |
| thunder_palm_artifact | 天雷符器 | mid | thunder | FOUNDATION | 5 | — |
| vajra_shield | 金刚罩 | mid | defense | FOUNDATION | 5 | ✓ |
| wind_fire_wheels | 风火轮 | mid | offense | FOUNDATION | 5 | — |
| xuantie_flying_shield | 玄铁飞天盾 | mid | defense | FOUNDATION | 5 | ✓ |
| yellow_demon_banner | 黄泉幡（仿） | mid | yin | FOUNDATION | 5 | — |
| ancient_treasure_appraisal_lens | 鉴宝瞳镜 | mid | utility | FOUNDATION | 4 | — |
| azure_rope_net | 青索网 | mid | control | FOUNDATION | 4 | ✓ |
| beast_soul_bell | 兽魂铃 | mid | beast_control | FOUNDATION | 4 | ✓ |
| beast_taming_whip | 御兽鞭 | mid | beast_control | FOUNDATION | 4 | ✓ |
| black_boots | 乌靴 | mid | movement | FOUNDATION | 4 | ✓ |
| black_gold_shield | 乌金盾 | mid | defense | FOUNDATION | 4 | ✓ |
| blood_jade_shield | 血玉盾 | mid | defense | FOUNDATION | 4 | — |
| chaotic_sea_navigation_compass | 星海罗盘 | mid | utility | FOUNDATION | 4 | — |
| dark_iron_ring | 玄铁环 | mid | offense | FOUNDATION | 4 | ✓ |
| giant_turtle_puppet_core | 巨龟傀儡核心 | mid | puppet_core | FOUNDATION | 4 | ✓ |
| ice_silk_net | 冰蚕丝网 | mid | control | FOUNDATION | 4 | — |
| jade_law_seal | 法印 | mid | control | FOUNDATION | 4 | — |
| poluo_beads | 婆罗珠 | mid | soul_stabilize | FOUNDATION | 4 | ✓ |
| qingning_mirror | 青凝镜 | mid | defense | FOUNDATION | 4 | ✓ |
| silver_spirit_mirror | 银灵镜 | mid | defense | FOUNDATION | 4 | ✓ |
| soul_gathering_bowl | 聚魂钵 | mid | yin | FOUNDATION | 4 | ✓ |
| soul_lock_chain | 锁魂链 | mid | yin | FOUNDATION | 4 | — |
| spirit_beast_bridle | 御兽环 | mid | beast_control | FOUNDATION | 4 | ✓ |
| spirit_boat_model | 灵舟模型 | mid | vehicle_key | FOUNDATION | 4 | — |
| star_palace_cipher_talisman | 星宫密符器 | mid | utility | FOUNDATION | 4 | — |
| stealth_cloak_artifact | 匿形斗篷 | mid | utility | FOUNDATION | 4 | — |
| whirlpool_pearl | 旋涡珠 | mid | control | FOUNDATION | 4 | — |
| wind_escape_sail | 风遁帆 | mid | movement | FOUNDATION | 4 | ✓ |
| yellow_umbrella | 黄罗伞 | mid | defense | FOUNDATION | 4 | ✓ |
| soul_summon_bell | 引魂钟 | mid | puppet_control | QI_REFINING | 3 | ✓ |
| black_wind_flag_fragment | 黑风旗残角 | spirit_treasure | material_artifact | VOID_REFINEMENT | 11 | — |
| demon_dragon_blade_blank | 魔龙刃胚 | spirit_treasure | material_artifact | VOID_REFINEMENT | 11 | — |
| demon_wind_flag_replica | 黑风旗（仿） | spirit_treasure | space_control | VOID_REFINEMENT | 11 | — |
| eight_spirit_ruler_shard | 八灵尺碎片 | spirit_treasure | material_artifact | VOID_REFINEMENT | 11 | — |
| great_vehicle_vajra_replica | 金刚杵（仿） | spirit_treasure | offense | GREAT_VEHICLE | 11 | — |
| mixed_element_disk_replica | 混元尺（仿） | spirit_treasure | control | VOID_REFINEMENT | 11 | — |
| seven_flame_fan_replica | 七焰扇（仿） | spirit_treasure | offense | VOID_REFINEMENT | 11 | — |
| three_flame_fan_replica | 三焰扇（仿） | spirit_treasure | offense | VOID_REFINEMENT | 11 | ✓ |
| tianyuan_border_talisman_artifact | 天渊护身佩 | spirit_treasure | defense | VOID_REFINEMENT | 11 | — |
| void_heaven_cauldron_shard | 虚天鼎碎片 | spirit_treasure | material_artifact | VOID_REFINEMENT | 11 | — |
| void_refining_bell | 炼虚钟（灵宝模板） | spirit_treasure | space_control | VOID_REFINEMENT | 11 | ✓ |

## 六、无炼器配方条目（96）— 掉落/拍卖/剧情/碎片/消耗

### 其他(待补配方或专属掉落) (46)

flat_crown, demon_refining_pot, silver_moon_wolf, nine_dragon_barrier_token, quhun_iron_puppet, fire_cloud_banner, blood_jade_shield, tortoise_shell_shield, wind_fire_wheels, soul_lock_chain, vermillion_bird_ring, falling_star_ring, green_copper_bell, demon_suppress_tower_replica, iron_chain_sickle, thunder_palm_artifact, soul_refining_banner, demon_subduing_staff, jade_law_seal, five_element_orb, star_palace_standard_sword, reverse_star_assassin_blade, void_palace_chain, ice_silk_net, celestial_silk_armor, blood_drinking_hook, spirit_repelling_mirror, mixed_element_ruler_low, bixie_blade, demonized_ancient_sword, yellow_demon_banner, whirlpool_pearl, thunder_dragon_horn, purple_fire_bead, sky_cleaver_axe, soul_devouring_chain, tianyuan_border_talisman_artifact, demon_blood_saber, wood_spirit_staff, iron_bone_claw, spirit_sand_hourglass, dual_element_ring, sky_bracelet_replica, blood_thunder_pearl, spirit_talisman_vessel, mirror_space_scroll

### 古宝/灵宝(剧情秘境) (10)

black_moon_ruler, seven_flame_fan_replica, great_vehicle_vajra_replica, flat_mountain_seal_replica, mixed_element_disk_replica, demon_wind_flag_replica, jade_ruyi_replica, two_luminaries_shuttle, yuan_yang_veil_replica, forbidden_demon_ring_set

### 消耗/utility (15)

great_shift_token, aura_conceal_cloth, mixed_element_bowl_low, spirit_lamp, stealth_cloak_artifact, copper_bell_charm, wanbao_pavilion_coupon, space_rift_compass, artifact_repair_kit, spirit_nourish_silk, artifact_spirit_awakening_incense, ancient_treasure_appraisal_lens, chaotic_sea_navigation_compass, great_shift_token_replica, star_palace_cipher_talisman

### 碎片/材料 (17)

evil_mirage_mirror_shard, xuanhuang_mirror_shard, void_heaven_ruler_shard, eight_spirit_ruler_shard, black_wind_flag_fragment, ancient_sea_halberd_shard, ancient_forest_disk_shard, soul_scattering_mirror_shard, giant_sword_gate_relic, kunwu_seal_fragment, xuanguang_mirror_shard, nine_dragon_cauldron_shard, demon_dragon_blade_blank, illusion_transform_clay, sun_essence_stone_inlay, geng_gold_inlay, void_heaven_cauldron_shard

### 符宝(部分配方在表) (3)

nascent_soul_talisman_treasure, talisman_treasure_flat_mountain, six_ding_armor_talisman

### 钥匙/飞行令牌 (5)

void_key, spirit_boat_model, montain_five_friends_token, auction_sealed_hammer, cloud_sedan_token

## 七、符宝模板

- `gold_light_brick` 金光砖符宝 — uses 默认 3
- `talisman_treasure_golden_wheel` 金轮符宝 — uses 默认 3
- `talisman_treasure_thunder_rod` 霹雳神雷符宝 — uses 默认 3
- `talisman_treasure_demon_seal` 封魔符宝 — uses 默认 3
- `nascent_soul_talisman_treasure` 元婴符宝（模板） — uses 默认 1
- `talisman_treasure_fire_spear` 火蛟矛符宝 — uses 默认 2
- `talisman_treasure_ice_shield` 玄冰盾符宝 — uses 默认 2
- `talisman_treasure_soul_charm` 定魂符宝 — uses 默认 3
- `thunder_pearl_talisman` 天雷子 — uses 默认 1
- `talisman_treasure_flat_mountain` 平山印符宝 — uses 默认 2

## 八、飞行器具（非攻击法宝）

- `spirit_boat_low` 低阶灵舟 — QI_REFINING speed 1.0
- `spirit_boat_mid` 中阶灵舟 — FOUNDATION speed 1.5
- `chaotic_sea_ferry` 乱星海渡船 — QI_REFINING speed 1.2
- `cloud_sedan` 云轿 — CORE_FORMATION speed 2.0
- `wind_feather_raft` 风羽筏 — FOUNDATION speed 1.8
- `bone_wind_cart_vehicle` 御风车 — FOUNDATION speed 1.6
- 靴类引用：`cloud_boots, black_boots`

## 九、万宝楼货架

- gold_demon_chain (mid_stone_15_40)
- silver_giant_sword (low_stone_300_800)
- evil_illusion_mirror (mid_stone_8_20)
- gold_light_brick (auction_only)
- storage_bracelet_low (low_stone_80_200)
- ancient_treasure_appraisal_lens (low_stone_150_400)
- wanbao_pavilion_coupon (low_stone_30)

## 十、大晋拍卖 lot

- xuanguang_mirror_shard 起拍 200 中品灵石
- black_moon_ruler 起拍 500 中品灵石
- talisman_treasure_thunder_rod 起拍 80 中品灵石
- phoenix_feather_fan 起拍 120 中品灵石
- natal_artifact_embryo 起拍 300 中品灵石
- eight_spirit_ruler_shard 起拍 800 中品灵石

## Forge P0_launch

flying_sword_low, cloud_boots, spirit_gathering_bead, yellow_umbrella, qingye_leaf_fan, storage_bracelet_low, snake_pearl, flying_needle_set, black_gold_shield, bedrock_shield, artifact_repair_kit

## Forge P1_content

silver_giant_sword, gold_demon_chain, evil_illusion_mirror, qingning_mirror, gold_light_brick, beast_taming_whip, spirit_beast_bridle, wind_escape_sail, moon_shadow_disk, talisman_treasure_soul_charm, void_palace_cold_jade_pendant

## Forge P2_late

xuanguang_mirror, xuanhuang_mirror, nine_dragon_cauldron_replica, natal_artifact_embryo, natal_sword_embryo, void_refining_bell, seven_flame_fan_replica, thunder_pearl_talisman
