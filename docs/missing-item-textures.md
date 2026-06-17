# Missing Item Textures Report

## Summary

- Registered items count: 121
- Existing item models count: 99
- Existing item textures count: 86
- **Missing textures count (含占位贴图): 94**
  - 完全缺失贴图（无 PNG 文件）: 34
  - 仅有占位贴图（需替换为正式贴图）: 60
- Items with proper textures: 27
- Missing model JSON count: 0

---

## Missing Textures — 完全缺失（无模型 + 无贴图）

以下物品已在 Java 中注册，但既没有模型 JSON 也没有贴图文件。

### immortal_jade

- 中文名: 仙玉
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/immortal_jade.png
- 现有模型 JSON: No
- 分类推测: crystal
- 建议视觉描述: translucent jade-green immortal crystal with soft inner glow

### qi_recovery_pill

- 中文名: 回灵丹
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/qi_recovery_pill.png
- 现有模型 JSON: Yes (引用 item/qi_recovery_pill, PNG 缺失)
- 分类推测: pill
- 建议视觉描述: pale blue round pill with faint qi energy swirls

### cultivation_pill

- 中文名: 凝气丹
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/cultivation_pill.png
- 现有模型 JSON: Yes (引用 item/cultivation_pill, PNG 缺失)
- 分类推测: pill
- 建议视觉描述: glowing golden pill used to boost cultivation

### breakthrough_pill

- 中文名: 破境丹
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/breakthrough_pill.png
- 现有模型 JSON: Yes (引用 item/breakthrough_pill, PNG 缺失)
- 分类推测: pill
- 建议视觉描述: radiant crimson pill with breakthrough rune markings

### rejuvenation_pill_low

- 中文名: 下品回春丹
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/rejuvenation_pill_low.png
- 现有模型 JSON: No
- 分类推测: pill
- 建议视觉描述: small green pill with vitality-restoring herbal aroma

### foundation_building_pill_low

- 中文名: 下品筑基丹
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/foundation_building_pill_low.png
- 现有模型 JSON: No
- 分类推测: pill
- 建议视觉描述: amber pill with swirling foundation-building energy

### healing_pill_low

- 中文名: 下品疗伤丹
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/healing_pill_low.png
- 现有模型 JSON: No
- 分类推测: pill
- 建议视觉描述: soft pink healing pill with gentle restorative glow

### clear_spirit_powder_low

- 中文名: 下品清灵散
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/clear_spirit_powder_low.png
- 现有模型 JSON: No
- 分类推测: pill
- 建议视觉描述: fine white-blue powder in a small packet or pouch

### fasting_pill_low

- 中文名: 下品辟谷丹
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/fasting_pill_low.png
- 现有模型 JSON: No
- 分类推测: pill
- 建议视觉描述: plain beige pill resembling a compressed grain of rice

### calming_pill_low

- 中文名: 稳神丹（下品）
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/calming_pill_low.png
- 现有模型 JSON: No
- 分类推测: pill
- 建议视觉描述: serene lavender pill that calms the mind and reduces qi deviation risk

### spirit_charm

- 中文名: 灵力护符
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/spirit_charm.png
- 现有模型 JSON: Yes (引用 item/spirit_charm, PNG 缺失)
- 分类推测: talisman
- 建议视觉描述: jade-green protective talisman charm with spiritual rune inscriptions

### fire_talisman

- 中文名: 火弹符
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/fire_talisman.png
- 现有模型 JSON: Yes (引用 item/fire_talisman, PNG 缺失)
- 分类推测: talisman
- 建议视觉描述: yellow paper talisman with red fire runes

### armor_talisman

- 中文名: 金甲符
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/armor_talisman.png
- 现有模型 JSON: Yes (引用 item/armor_talisman, PNG 缺失)
- 分类推测: talisman
- 建议视觉描述: golden paper talisman with metallic armor sigil

### speed_talisman

- 中文名: 疾行符
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/speed_talisman.png
- 现有模型 JSON: Yes (引用 item/speed_talisman, PNG 缺失)
- 分类推测: talisman
- 建议视觉描述: light blue paper talisman with wind-speed rune markings

### spirit_ore (BlockItem)

- 中文名: 灵石矿
- 注册来源: ModItems.java (BlockItem → ModBlocks.SPIRIT_ORE)
- 期望贴图路径: textures/block/spirit_ore.png
- 现有模型 JSON: Yes (block model, block texture PNG 缺失)
- 分类推测: block_item
- 建议视觉描述: stone block with embedded glowing blue spirit crystal veins

### spirit_grass

- 中文名: 灵草
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/spirit_grass.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: green glowing spiritual grass blade with faint qi aura

### cloud_mushroom

- 中文名: 云雾菇
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/cloud_mushroom.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: pale white-blue mushroom surrounded by wispy cloud vapor

### phoenix_feather_flower

- 中文名: 凤羽花
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/phoenix_feather_flower.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: red-orange flower with petals shaped like phoenix feathers

### dragon_blood_grass

- 中文名: 龙血草
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/dragon_blood_grass.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: dark crimson grass stained with dragon blood essence

### immortal_ginseng

- 中文名: 仙参
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/immortal_ginseng.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: golden immortal ginseng root with faint divine aura

### beast_core

- 中文名: 妖兽内丹
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/beast_core.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: dark purple spherical beast core with swirling inner energy

### spirit_beast_bone

- 中文名: 灵兽骨
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/spirit_beast_bone.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: pale ivory bone fragment with faint spiritual energy traces

### dragon_scale

- 中文名: 龙鳞
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/dragon_scale.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: iridescent dragon scale with rainbow-shifting surface

### phoenix_feather

- 中文名: 凤凰羽
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/phoenix_feather.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: brilliant red-gold phoenix feather radiating warmth

### true_dragon_blood

- 中文名: 真龙血
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/true_dragon_blood.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: vial of glowing crimson true dragon blood with golden flecks

### spirit_iron

- 中文名: 灵铁
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/spirit_iron.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: dark metallic iron ingot with blue spiritual energy veins

### cold_jade

- 中文名: 寒玉
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/cold_jade.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: frost-blue jade piece emanating cold mist

### star_meteorite

- 中文名: 星陨铁
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/star_meteorite.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: dark metallic meteorite fragment with starlight specks

### celestial_crystal

- 中文名: 天晶石
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/celestial_crystal.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: prismatic celestial crystal refracting heavenly light

### chaos_gold

- 中文名: 混沌金
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/chaos_gold.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: swirling golden ingot with chaotic primordial energy patterns

### soul_fragment

- 中文名: 魂魄碎片
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/soul_fragment.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: ethereal ghostly fragment with translucent soul energy wisps

### void_crystal

- 中文名: 虚空结晶
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/void_crystal.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: deep purple-black crystal with void-space distortions inside

### time_sand

- 中文名: 时光之砂
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/time_sand.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: golden shimmering sand grains with temporal energy particles

### primordial_essence

- 中文名: 先天本源
- 注册来源: ModItems.java
- 期望贴图路径: textures/item/primordial_essence.png
- 现有模型 JSON: No
- 分类推测: material
- 建议视觉描述: swirling multicolor orb of primordial creation essence

---

## Missing Textures — 仅有占位贴图（传承卷轴 × 60）

以下 60 个传承卷轴物品均有模型 JSON，且模型引用 `*_placeholder.png` 占位贴图文件。
占位贴图文件存在，但视觉上是通用卷轴图，需为每个传承卷轴制作独特贴图。

| # | item_id | 中文名 | 分类 | 建议视觉描述 |
|---|---------|--------|------|-------------|
| 1 | technique_manual_azure_origin_sword_art | 青元剑诀传承卷轴 | talisman | azure scroll with sword-light calligraphy |
| 2 | technique_manual_azure_origin_sword_support | 青元剑诀辅助传承卷轴 | talisman | lighter azure scroll with supporting sword diagrams |
| 3 | technique_manual_six_paths_sage_created | 六道极圣所创传承卷轴 | talisman | six-colored scroll with sage rune seals |
| 4 | technique_manual_demonic | 魔道传承卷轴 | talisman | dark crimson scroll with demonic sigils |
| 5 | technique_manual_yao_bird_cultivator | 妖族禽修传承卷轴 | talisman | feathered scroll with avian yao beast motifs |
| 6 | technique_manual_five_elements_escape | 五行遁术传承卷轴 | talisman | five-color elemental scroll with escape runes |
| 7 | technique_manual_thousand_illusion_sect | 千幻宗传承卷轴 | talisman | shimmering illusory scroll with mirage patterns |
| 8 | technique_manual_nangong_wan_main | 南宫婉主修传承卷轴 | talisman | elegant feminine scroll with soft jade glow |
| 9 | technique_manual_yao | 妖族传承卷轴 | talisman | primal beast-hide scroll with yao claw marks |
| 10 | technique_manual_ghost | 鬼道传承卷轴 | talisman | ghostly pale scroll with spectral rune chains |
| 11 | technique_manual_purple_luo_mystic_skill | 紫罗玄功传承卷轴 | talisman | deep purple scroll with mystic energy spirals |
| 12 | technique_manual_orthodox | 正道传承卷轴 | talisman | radiant golden scroll with righteous qi seals |
| 13 | technique_manual_azure_origin_sword_derivative | 青元剑诀衍生传承卷轴 | talisman | derivative azure-white scroll with branching sword paths |
| 14 | technique_manual_common | 通用传承卷轴 | talisman | plain beige scroll with generic cultivation diagrams |
| 15 | technique_manual_spirit_taming_basic | 御灵宗基础传承卷轴 | talisman | green scroll with basic spirit-beast taming glyphs |
| 16 | technique_manual_common_tricks | 通用小技巧传承卷轴 | talisman | small tattered scroll with practical cultivation tips |
| 17 | technique_manual_azure_origin_sword_support_skill | 青元剑诀辅助功法传承卷轴 | talisman | supplementary azure scroll with sword-qi formulas |
| 18 | technique_manual_mystic_yin_appendix | 玄阴经附属传承卷轴 | talisman | dark yin scroll with cold moon-phase diagrams |
| 19 | technique_manual_formation | 阵法类传承卷轴 | talisman | geometric scroll with formation circle blueprints |
| 20 | technique_manual_ancient_sword_sect | 古剑门传承卷轴 | talisman | ancient stone-gray scroll with archaic sword engravings |
| 21 | technique_manual_spirit_taming_sect | 御灵宗传承卷轴 | talisman | emerald scroll with advanced beast-spirit binding seals |
| 22 | technique_manual_azure_origin_sword_spirit_realm_pre | 青元剑诀·灵界篇前置传承卷轴 | talisman | transitional azure scroll with realm-crossing sword intent |
| 23 | technique_manual_azure_origin_sword_spirit_realm | 青元剑诀·灵界篇传承卷轴 | talisman | powerful azure scroll with spirit-realm sword aura |
| 24 | technique_manual_thousand_bamboo_heritage | 千竹教传承传承卷轴 | talisman | bamboo-green scroll with thousand bamboo leaf patterns |
| 25 | technique_manual_great_development_formula | 大衍诀传承卷轴 | talisman | complex mathematical scroll with derivation formulas |
| 26 | technique_manual_great_development_master | 大衍神君传承卷轴 | talisman | divine-tier scroll with great development god-seals |
| 27 | technique_manual_han_li_self_created | 韩立自创传承卷轴 | talisman | humble yet profound scroll with personal annotations |
| 28 | technique_manual_chaotic_star_sea_demonic | 乱星海魔修传承卷轴 | talisman | chaotic starfield scroll with demonic corruption veins |
| 29 | technique_manual_top_demonic | 魔道顶阶传承卷轴 | talisman | supreme dark-red scroll with top-tier demonic authority |
| 30 | technique_manual_mystic_herder_nascent_appendix | 玄牧化婴附属传承卷轴 | talisman | pastoral mystic scroll with nascent-soul herding diagrams |
| 31 | technique_manual_chaotic_star_sea | 乱星海传承卷轴 | talisman | star-sea scroll with chaotic cosmic currents |
| 32 | technique_manual_heavenly_lan_temple | 天澜圣殿传承卷轴 | talisman | sacred temple scroll with heavenly blue wave motifs |
| 33 | technique_manual_ancient_secret_art | 上古秘术传承卷轴 | talisman | ancient cracked scroll with primordial secret glyphs |
| 34 | technique_manual_supreme_demonic | 魔道无上传承卷轴 | talisman | ultimate dark scroll with supreme demonic crown seal |
| 35 | technique_manual_nascent_soul_common | 元婴修士通用传承卷轴 | talisman | standard nascent-soul scroll with golden embryo icon |
| 36 | technique_manual_evergreen_appendix | 长春功附载传承卷轴 | talisman | evergreen scroll with perpetual spring vitality motifs |
| 37 | technique_manual_common_low | 通用低阶传承卷轴 | talisman | simple faded scroll with basic low-tier cultivation charts |
| 38 | technique_manual_mortal_martial | 世俗武林传承卷轴 | talisman | worn martial-arts scroll with mortal combat techniques |
| 39 | technique_manual_seven_mysteries_sect | 七玄门传承卷轴 | talisman | seven-symbol scroll with mysterious gate insignia |
| 40 | technique_manual_lost_true_immortal_art | 上古失传真仙术传承卷轴 | talisman | ancient divine scroll with lost immortal-realm calligraphy |
| 41 | technique_manual_nascent_soul_late_plus | 元婴后期以上传承卷轴 | talisman | advanced golden scroll with late nascent-soul diagrams |
| 42 | technique_manual_yuancha_saint_ancestor | 元刹圣祖传承卷轴 | talisman | primordial saint scroll with ancestral origin seals |
| 43 | technique_manual_ancient_demon_secret | 古魔秘术传承卷轴 | talisman | ancient demon scroll with forbidden secret runes |
| 44 | technique_manual_ancient_demon | 古魔传承卷轴 | talisman | weathered ancient scroll with primeval demon imagery |
| 45 | technique_manual_brahma_sacred_fragment | 梵圣真片传承卷轴 | talisman | sacred fragment scroll with brahma divine light |
| 46 | technique_manual_buddhist | 佛门传承卷轴 | talisman | golden Buddhist scroll with dharma wheel and lotus |
| 47 | technique_manual_ancient_demonic_skill | 上古魔功传承卷轴 | talisman | ancient dark scroll with primeval demonic power glyphs |
| 48 | technique_manual_great_jin | 大晋传承卷轴 | talisman | imperial scroll with great jin dynasty dragon seal |
| 49 | technique_manual_black_wind_flag_spirit | 黑风旗器灵传承卷轴 | talisman | dark wind scroll with black flag spirit-art bound |
| 50 | technique_manual_little_pole_palace | 小极宫传承卷轴 | talisman | ice-blue scroll with little pole palace frost motifs |
| 51 | technique_manual_gold_magnetic_spirit_wood | 金磁灵木传承卷轴 | talisman | golden-green scroll with magnetic spirit-wood grain |
| 52 | technique_manual_self_created | 自创传承卷轴 | talisman | blank-style scroll with personal creation aura |
| 53 | technique_manual_kunpeng_red_cloud_created | 鲲鹏族红云老祖所创传承卷轴 | talisman | cosmic scroll with kunpeng and red cloud ancestor imagery |
| 54 | technique_manual_immortal_thunder_origin | 仙界雷法本源传承卷轴 | talisman | crackling thunder scroll with immortal-realm lightning |
| 55 | technique_manual_demon_domain_body_refining | 魔域顶级炼体功传承卷轴 | talisman | dark muscular scroll with demon-domain body refining diagrams |
| 56 | technique_manual_immortal_realm_skill | 仙界功法传承卷轴 | talisman | divine white-gold scroll with immortal-realm aura |
| 57 | technique_manual_demon_race_secret | 魔族秘传传承卷轴 | talisman | sealed dark scroll with demon-race secret blood seals |
| 58 | technique_manual_true_word_sect_heritage | 真言门传承传承卷轴 | talisman | scroll with true-word mantra glyphs and sect gate seal |
| 59 | technique_manual_azure_sea_true_lord_skill | 碧海真君成名功法传承卷轴 | talisman | ocean-blue scroll with azure-sea true-lord wave patterns |
| 60 | technique_manual_gray_immortal_heritage | 灰仙传承传承卷轴 | talisman | gray misty scroll with gray immortal heritage sigils |

---

## Missing Textures — 五行灵石（需艺术升级）


| # | item_id | 中文名 | 属性 | 品级 | 建议视觉描述 |
|---|---------|--------|------|------|-------------|
| 1 | metal_spirit_stone | 下品金灵石 | Metal | Low | small metallic gold gemstone with faint qi shimmer |
| 2 | metal_spirit_stone_mid | 中品金灵石 | Metal | Mid | medium metallic gold crystal with brighter inner glow |
| 3 | metal_spirit_stone_high | 上品金灵石 | Metal | High | large radiant gold crystal pulsing with metal qi |
| 4 | metal_spirit_stone_superior | 极品金灵石 | Metal | Superior | brilliant golden spirit gem radiating supreme metal essence |
| 5 | wood_spirit_stone | 下品木灵石 | Wood | Low | small green gemstone with leaf-vein patterns inside |
| 6 | wood_spirit_stone_mid | 中品木灵石 | Wood | Mid | medium emerald crystal with growing vine motifs |
| 7 | wood_spirit_stone_high | 上品木灵石 | Wood | High | large verdant crystal radiating lush wood qi |
| 8 | wood_spirit_stone_superior | 极品木灵石 | Wood | Superior | brilliant jade-green gem with supreme life-force glow |
| 9 | water_spirit_stone | 下品水灵石 | Water | Low | small deep-blue gemstone with ripple patterns |
| 10 | water_spirit_stone_mid | 中品水灵石 | Water | Mid | medium sapphire crystal with flowing water currents |
| 11 | water_spirit_stone_high | 上品水灵石 | Water | High | large oceanic crystal pulsing with water qi |
| 12 | water_spirit_stone_superior | 极品水灵石 | Water | Superior | brilliant deep-blue gem radiating supreme water essence |
| 13 | fire_element_spirit_stone | 下品火灵石 | Fire | Low | small red gemstone with flickering flame core |
| 14 | fire_element_spirit_stone_mid | 中品火灵石 | Fire | Mid | medium ruby crystal with swirling fire energy |
| 15 | fire_element_spirit_stone_high | 上品火灵石 | Fire | High | large blazing crystal radiating intense fire qi |
| 16 | fire_element_spirit_stone_superior | 极品火灵石 | Fire | Superior | brilliant crimson gem with supreme inferno essence |
| 17 | earth_spirit_stone | 下品土灵石 | Earth | Low | small brown-amber gemstone with earth-layer striations |
| 18 | earth_spirit_stone_mid | 中品土灵石 | Earth | Mid | medium amber crystal with mountain-core patterns |
| 19 | earth_spirit_stone_high | 上品土灵石 | Earth | High | large earthen crystal pulsing with grounded qi |
| 20 | earth_spirit_stone_superior | 极品土灵石 | Earth | Superior | brilliant amber gem radiating supreme earth essence |

---

## Missing Model JSON

以下物品有贴图文件但缺少对应的 models/item/<item_id>.json：

（无。所有存在贴图文件的物品均有对应模型 JSON。）

### 附注：孤儿模型（有模型但未注册）

以下模型 JSON 存在但对应的物品未在 Java 中注册，可能是旧版遗留：

- `spirit_stone.json` — 对应 lang 条目"下品灵石"（旧版通用灵石，已被五行灵石替代）
- `spirit_stone_mid.json` — 对应 lang 条目"中阶灵石"
- `spirit_stone_high.json` — 对应 lang 条目"高阶灵石"
- `spirit_stone_superior.json` — 对应 lang 条目"极品灵石"

这些模型引用 `item/spirit_stone` 等贴图，但这些贴图 PNG 也不存在。

---

## Notes

### 1. 方块物品状态

- `meditation_cushion`（蒲团）：block 模型和 block 贴图均存在 ✅
- `spirit_gathering_array`（聚灵阵）：block 模型和 block 贴图均存在 ✅
- `spirit_ore`（灵石矿）：block 模型存在，但 **block 贴图缺失** ❌

### 2. 已有正式贴图的物品（无需处理，共 27 个）

- 五行灵石 × 20（贴图存在，但列入上方"需艺术升级"）
- flying_sword, flying_artifact, leyline_compass, ling_gen_test_stone, spirit_detector × 5
- meditation_cushion (block), spirit_gathering_array (block) × 2

### 3. 需要人工确认的项

- `spirit_stone` 系列（旧版通用灵石）：模型 JSON 和 lang 存在但未注册，建议清理或重新注册
- `qi_recovery_pill` 等已有模型 JSON 但贴图 PNG 缺失的物品：需创建贴图或确认模型引用路径
- 传承卷轴占位贴图虽存在文件，但视觉全部相同，建议后续逐个制作独特贴图
