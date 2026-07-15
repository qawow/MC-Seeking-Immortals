# 天南七派精表 + 青元原著层对照 v126

> 无代码。

## 1. 青元剑诀：13 层可玩 × 原著锚点

数据：`qingyuan_novel_layer_map_v126.json`

| 层 | 名称 | 境界 | 本层新术 | 原著/剧情锚点 |
|----|------|------|----------|----------------|
| 1 | 第一层·剑气初凝 | FOUNDATION | `qingyuan_sword_ray, sword_shield` | 筑基后初修剑诀 |
| 2 | 第二层·剑芒成形 | FOUNDATION | `qingyuan_sword_ray, flying_sword_strike, sword_shield` | 可稳定施展剑芒 |
| 3 | 第三层·御剑小成 | FOUNDATION | `qingyuan_sword_ray, flying_sword_strike, sword_escape, green_bamboo_sword_qi` | 御剑斩、短距剑遁 |
| 4 | 第四层·剑丝初现 | FOUNDATION | `qingyuan_sword_silk, green_bamboo_sword_qi, sword_escape, dual_sword_dance` | 剑气细化切割 |
| 5 | 第五层·剑意渐显 | FOUNDATION | `qingyuan_sword_silk, qingyuan_layer5_intent, sword_formation_basic, dual_sword_dance, invisible_sword` | 韩立筑基中期气质；小剑阵雏形 |
| 6 | 第六层·筑基圆满剑 | FOUNDATION | `sword_merge, invisible_sword, sword_formation_basic, qingyuan_sword_silk` | 备结丹；人剑合一雏形 |
| 7 | 第七层·结丹剑基 | CORE_FORMATION | `qingyuan_sword_pill_intent, sword_merge, qingyuan_bamboo_cloud_drive` | 结丹后剑基稳固 |
| 8 | 第八层·本命剑成 | CORE_FORMATION | `qingyuan_bamboo_cloud_drive, green_bamboo_sword_ray, qingyuan_sword_pill_intent` | 青竹蜂云剑本命御使（需本命剑） |
| 9 | 第九层·元婴剑阶 | NASCENT_SOUL | `thousand_sword_array, qingyuan_layer9_split_mastery, green_bamboo_sword_ray, qingyuan_sword_light_split, qingyuan_bamboo_cloud_drive` | 原著元婴前后「九层」气质锚点；剑光分化 |
| 10 | 第十层·剑光分化大成 | NASCENT_SOUL | `qingyuan_sword_light_split, thousand_sword_array, sword_domain` | 一剑多芒熟练 |
| 11 | 第十一层·剑域展开 | NASCENT_SOUL | `sword_domain, wan_sword_return, qingyuan_sword_light_split` | 剑域/万剑归宗气质 |
| 12 | 第十二层·辟邪雷载 | NASCENT_SOUL | `pixie_thunder_sword, sword_domain, wan_sword_return` | 青竹蜂云剑辟邪神雷 |
| 13 | 第十三层·青元圆满带 | DEITY_TRANSFORMATION | `pixie_thunder_sword, sword_domain, qingyuan_sword_light_split, wan_sword_return` | 化神前大圆满气质；之后主修可转元磁等 |

韩立时间线提示：

- **筑基后黄枫谷** → 层 1-5：得诀开修，剑芒御剑
- **结丹前后** → 层 6-8：本命青竹蜂云剑
- **元婴** → 层 9-11：约第九层气质+剑域
- **化神前** → 层 12-13：辟邪雷、圆满带

与 v125 七带映射：1-2→带1 … 13→带7（见 JSON `mapping_from_v125_7bands`）。


## 2. 天南七派（本卷主名单）

本卷主名单：黄枫谷、掩月宗、灵兽山、清虚门、化刀坞、天阙堡、巨剑门（sects.json 气质）

备选名单见 `tiannan_seven_sects_lore.json`：huangfeng_valley, yanyue_sect, tianfu_gate, tianque_fort, tianlan_temple, lingyun_sect, qingyan_sect

### 黄枫谷（58 术）

- 定位：综合散功、药园、玩家友好开局
- 功法：changchun_gong, qingyuan_sword_art, sanzhuan_zhongyuan_gong, huangfeng_alchemy_scripture
- 注：青元非公开课，李化元线；长春为前序

| 术 | 境界 | 功法 |
|----|------|------|
| 青竹剑芒（仿） `green_bamboo_sword_ray` | CORE_FORMATION | qingyuan_sword_art |
| 青竹蜂云御 `qingyuan_bamboo_cloud_drive` | CORE_FORMATION | qingyuan_sword_art |
| 剑光分化 `qingyuan_sword_light_split` | CORE_FORMATION | qingyuan_sword_art |
| 剑丸之意 `qingyuan_sword_pill_intent` | CORE_FORMATION | qingyuan_sword_art |
| 万剑阵 `thousand_sword_array` | CORE_FORMATION | qingyuan_sword_art |
| 双剑舞 `dual_sword_dance` | FOUNDATION | qingyuan_sword_art |
| earth系爆发术（通称） `elemental_burst_earth` | FOUNDATION | changchun_gong |
| fire系爆发术（通称） `elemental_burst_fire` | FOUNDATION | changchun_gong |
| ice系爆发术（通称） `elemental_burst_ice` | FOUNDATION | changchun_gong |
| metal系爆发术（通称） `elemental_burst_metal` | FOUNDATION | changchun_gong |
| thunder系爆发术（通称） `elemental_burst_thunder` | FOUNDATION | changchun_gong |
| water系爆发术（通称） `elemental_burst_water` | FOUNDATION | changchun_gong |
| wind系爆发术（通称） `elemental_burst_wind` | FOUNDATION | changchun_gong |
| wood系爆发术（通称） `elemental_burst_wood` | FOUNDATION | changchun_gong |
| 炎爆术 `flame_burst` | FOUNDATION | changchun_gong |
| 御剑斩 `flying_sword_strike` | FOUNDATION | qingyuan_sword_art |
| 青竹剑气 `green_bamboo_sword_qi` | FOUNDATION | qingyuan_sword_art |
| 黄枫火蛇术 `huangfeng_fire_serpent` | FOUNDATION | changchun_gong |
| 冰矛术 `ice_spear` | FOUNDATION | changchun_gong |
| 无形剑 `invisible_sword` | FOUNDATION | qingyuan_sword_art |
| 青元剑意（五层） `qingyuan_layer5_intent` | FOUNDATION | qingyuan_sword_art |
| 青元剑芒 `qingyuan_sword_ray` | FOUNDATION | qingyuan_sword_art |
| 青元剑丝 `qingyuan_sword_silk` | FOUNDATION | qingyuan_sword_art |
| 三转固元 `sanzhuan_rebuild` | FOUNDATION | sanzhuan_zhongyuan_gong |
| 剑遁 `sword_escape` | FOUNDATION | qingyuan_sword_art |
| … | 共 58 | 见 JSON |

### 掩月宗（29 术）

- 定位：幻术/双修，七派之首气质
- 功法：yanyue_gong, yanyue_illusion_art, yanyue_cycle_art, xuan_yin_art
- 注：玄阴可相关兼修，勿与六宗鬼灵完全等同

| 术 | 境界 | 功法 |
|----|------|------|
| 玄阴魂丝 `xuanyin_soul_thread` | CORE_FORMATION | xuan_yin_art |
| 掩月心镜 `yanyue_heart_mirror` | CORE_FORMATION | yanyue_illusion_art |
| 掩月幻阵 `yanyue_phantom_array` | CORE_FORMATION | yanyue_illusion_art |
| 污血术 `blood_corruption` | FOUNDATION | xuan_yin_art |
| 炼尸甲 `corpse_armor` | FOUNDATION | xuan_yin_art |
| 尸爆 `corpse_explosion` | FOUNDATION | xuan_yin_art |
| 鬼王召 `ghost_king_summon` | FOUNDATION | xuan_yin_art |
| 鬼灵玄阴渡 `guiling_xuan_bridge` | FOUNDATION | xuan_yin_art |
| 幻雾术 `illusion_mist` | FOUNDATION | yanyue_gong |
| 逆星匿形 `inverse_star_veil` | FOUNDATION | yanyue_gong |
| 隐匿术 `invisibility_basic` | FOUNDATION | yanyue_gong |
| 夺魂牵引 `poluo_soul_pull` | FOUNDATION | xuan_yin_art |
| 招魂 `soul_banner` | FOUNDATION | xuan_yin_art |
| 噬魂云 `soul_devouring_cloud` | FOUNDATION | xuan_yin_art |
| 素女轮回吸 `suyu_cycle_drain` | FOUNDATION | yanyue_cycle_art |
| 幽冥火 `underworld_flame` | FOUNDATION | xuan_yin_art |
| 月纱障 `veil_of_moon` | FOUNDATION | yanyue_illusion_art |
| 玄阴寒息 `xuanyin_cold_aura` | FOUNDATION | xuan_yin_art |
| 掩月幻景 `yanyue_mirage` | FOUNDATION | yanyue_gong |
| 掩月幻身 `yanyue_moon_illusion` | FOUNDATION | yanyue_gong |
| 掩月纱 `yanyue_moon_veil` | FOUNDATION | yanyue_gong |
| 阴蚀 `yin_corrosion` | FOUNDATION | xuan_yin_art |
| 阴魂链 `yin_soul_chain` | FOUNDATION | xuan_yin_art |
| 分身幻影 `clone_image` | QI_REFINING | yanyue_gong |
| 梦缚 `dream_snare` | QI_REFINING | yanyue_gong |
| … | 共 29 | 见 JSON |

### 灵兽山（6 术）

- 定位：御兽
- 功法：lingshou_beast_mind_art, beast_taming_basic
- 注：可与御灵宗对立/倒戈叙事

| 术 | 境界 | 功法 |
|----|------|------|
| 兽魂合体 `beast_soul_fusion_secret` | CORE_FORMATION | beast_taming_basic |
| 狂化驱令 `lingshou_frenzy_command` | CORE_FORMATION | lingshou_beast_mind_art |
| 兽人共感 `lingshou_shared_sense` | FOUNDATION | lingshou_beast_mind_art |
| 群兽围猎 `lingshou_pack_hunt` | NASCENT_SOUL | lingshou_beast_mind_art |
| 御兽契约 `beast_tame_bond` | QI_REFINING | lingshou_beast_mind_art |
| 灵兽召唤 `lingshou_call_pet` | QI_REFINING | lingshou_beast_mind_art |

### 清虚门（5 术）

- 定位：剑修清修/道门清修
- 功法：qingxu_pure_tao_art
- 注：清修剑道气质；与巨剑重剑区分

| 术 | 境界 | 功法 |
|----|------|------|
| 清云小剑阵 `qingxu_cloud_sword_array` | CORE_FORMATION | qingxu_pure_tao_art |
| 清虚封喧 `qingxu_seal_noise` | FOUNDATION | qingxu_pure_tao_art |
| 清虚剑意 `qingxu_sword_clear` | FOUNDATION | qingxu_pure_tao_art |
| 清虚涤念 `qingxu_clear_mind` | QI_REFINING | qingxu_pure_tao_art |
| 清虚步 `qingxu_wind_step` | QI_REFINING | qingxu_pure_tao_art |

### 化刀坞（6 术）

- 定位：刀法器修
- 功法：huadao_blade_intent, huadao_slash_art
- 注：blade path → sword 文件 + blade 标签

| 术 | 境界 | 功法 |
|----|------|------|
| 化刀绝斩 `huadao_slash_finisher` | CORE_FORMATION | huadao_slash_art |
| 化刀斩 `huadao_slash` | FOUNDATION | huadao_slash_art |
| 化刀风刃 `huadao_wind_blade` | FOUNDATION | huadao_blade_intent |
| 化刀一刀 `huadao_slash_secret` | NASCENT_SOUL | huadao_slash_art |
| 化刀出鞘 `huadao_draw` | QI_REFINING | huadao_blade_intent |
| 化刀意斩 `huadao_intent_slash` | QI_REFINING | huadao_blade_intent |

### 天阙堡（4 术）

- 定位：堡垒防御
- 功法：tianque_fort_art
- 注：守城阵盾

| 术 | 境界 | 功法 |
|----|------|------|
| 天阙要塞域 `tianque_fortress_domain` | CORE_FORMATION | tianque_fort_art |
| 天阙堡罡 `tianque_bastion_guard` | FOUNDATION | tianque_fort_art |
| 天阙盾击 `tianque_shield_bash` | FOUNDATION | tianque_fort_art |
| 天阙壁阵 `tianque_wall_array` | FOUNDATION | tianque_fort_art |

### 巨剑门（6 术）

- 定位：重剑
- 功法：giant_sword_art
- 注：重劈巨剑

| 术 | 境界 | 功法 |
|----|------|------|
| 巨剑开山 `blade_giant_sword_ultimate_secret` | CORE_FORMATION | giant_sword_art |
| 巨剑投掷 `giant_sword_throw` | CORE_FORMATION | giant_sword_art |
| 巨剑裂地 `giant_sword_cleave` | FOUNDATION | giant_sword_art |
| 巨剑横门 `giant_sword_guard` | FOUNDATION | giant_sword_art |
| 巨剑崩山 `giant_sword_smash` | FOUNDATION | giant_sword_art |
| 巨剑如山 `giant_sword_mountain` | NASCENT_SOUL | giant_sword_art |

## 3. 文件

- `tiannan_seven_sects_packs_v126.json`
- `qingyuan_novel_layer_map_v126.json`
- `method_layer_technique_matrix_v126.json`
