# Item Art Plan — 物品贴图清单

**更新日期**: 2026-06-15
**版本**: 0.1.47
**贴图规格**: 16×16 或 32×32 像素（MC 标准）
**资源路径**: `assets/seeking_immortals/textures/item/`

---

## 贴图状态图例

| 标记 | 含义 |
|------|------|
| ✅ | 已有正式贴图 |
| 🟡 | 已有占位贴图（placeholder） |
| ❌ | 缺少贴图 |
| 🎨 | 已有 AI 生成原图（`generated_art/raw/`），待缩放适配 |

---

## 1. 通用灵石（4 个）

基础灵力吸收载体，无属性。

| item_id | 中文名 | 贴图文件 | 状态 |
|---------|--------|----------|------|
| `spirit_stone` | 下品灵石 | `spirit_stone.png` | ❌ |
| `spirit_stone_mid` | 中阶灵石 | `spirit_stone_mid.png` | ❌ |
| `spirit_stone_high` | 高阶灵石 | `spirit_stone_high.png` | ❌ |
| `spirit_stone_superior` | 极品灵石 | `spirit_stone_superior.png` | ❌ |

**设计要点**: 半透明晶体，品级越高越亮。色系：淡蓝白 → 深蓝紫。

---

## 2. 五行灵石（20 个）

按灵根属性提供修炼增幅。每属性 4 品级（下/中/上/极）。

### 2.1 金属性 — 色系：金黄 / 橙

| item_id | 中文名 | 贴图文件 | 状态 |
|---------|--------|----------|------|
| `metal_spirit_stone` | 下品金灵石 | `metal_spirit_stone.png` | ✅ 🎨 |
| `metal_spirit_stone_mid` | 中品金灵石 | `metal_spirit_stone_mid.png` | ✅ 🎨 |
| `metal_spirit_stone_high` | 上品金灵石 | `metal_spirit_stone_high.png` | ✅ 🎨 |
| `metal_spirit_stone_superior` | 极品金灵石 | `metal_spirit_stone_superior.png` | ✅ 🎨 |

### 2.2 木属性 — 色系：翠绿 / 碧

| item_id | 中文名 | 贴图文件 | 状态 |
|---------|--------|----------|------|
| `wood_spirit_stone` | 下品木灵石 | `wood_spirit_stone.png` | ✅ 🎨 |
| `wood_spirit_stone_mid` | 中品木灵石 | `wood_spirit_stone_mid.png` | ✅ 🎨 |
| `wood_spirit_stone_high` | 上品木灵石 | `wood_spirit_stone_high.png` | ✅ 🎨 |
| `wood_spirit_stone_superior` | 极品木灵石 | `wood_spirit_stone_superior.png` | ✅ 🎨 |

### 2.3 水属性 — 色系：浅蓝 / 青

| item_id | 中文名 | 贴图文件 | 状态 |
|---------|--------|----------|------|
| `water_spirit_stone` | 下品水灵石 | `water_spirit_stone.png` | ✅ 🎨 |
| `water_spirit_stone_mid` | 中品水灵石 | `water_spirit_stone_mid.png` | ✅ 🎨 |
| `water_spirit_stone_high` | 上品水灵石 | `water_spirit_stone_high.png` | ✅ 🎨 |
| `water_spirit_stone_superior` | 极品水灵石 | `water_spirit_stone_superior.png` | ✅ 🎨 |

### 2.4 火属性 — 色系：赤红 / 橙

| item_id | 中文名 | 贴图文件 | 状态 |
|---------|--------|----------|------|
| `fire_element_spirit_stone` | 下品火灵石 | `fire_element_spirit_stone.png` | ✅ 🎨 |
| `fire_element_spirit_stone_mid` | 中品火灵石 | `fire_element_spirit_stone_mid.png` | ✅ 🎨 |
| `fire_element_spirit_stone_high` | 上品火灵石 | `fire_element_spirit_stone_high.png` | ✅ 🎨 |
| `fire_element_spirit_stone_superior` | 极品火灵石 | `fire_element_spirit_stone_superior.png` | ✅ 🎨 |

### 2.5 土属性 — 色系：褐色 / 土黄

| item_id | 中文名 | 贴图文件 | 状态 |
|---------|--------|----------|------|
| `earth_spirit_stone` | 下品土灵石 | `earth_spirit_stone.png` | ✅ 🎨 |
| `earth_spirit_stone_mid` | 中品土灵石 | `earth_spirit_stone_mid.png` | ✅ 🎨 |
| `earth_spirit_stone_high` | 上品土灵石 | `earth_spirit_stone_high.png` | ✅ 🎨 |
| `earth_spirit_stone_superior` | 极品土灵石 | `earth_spirit_stone_superior.png` | ✅ 🎨 |

**设计要点**: 锯齿状矿石晶体轮廓；下品暗淡粗糙，极品明亮且有魔力光晕。同属性共享轮廓，靠明度和饱和度区分品级。

---

## 3. 货币（1 个）

| item_id | 中文名 | 贴图文件 | 状态 |
|---------|--------|----------|------|
| `immortal_jade` | 仙玉 | `immortal_jade.png` | ❌ |

**设计要点**: 半透明翡翠令牌，边缘发光，区别于灵石。

---

## 4. 丹药（9 个）

### 4.1 传统丹药（3 个）

| item_id | 中文名 | 贴图文件 | 状态 | 备注 |
|---------|--------|----------|------|------|
| `qi_recovery_pill` | 回灵丹 | `qi_recovery_pill.png` | ❌ 🎨 | `generated_art/raw/spirit_recovery_pill.png` |
| `cultivation_pill` | 凝气丹 | `cultivation_pill.png` | ❌ 🎨 | `generated_art/raw/qi_condensing_pill.png` |
| `breakthrough_pill` | 破境丹 | `breakthrough_pill.png` | ❌ 🎨 | `generated_art/raw/foundation_establishment_pill.png` |

### 4.2 品质丹药 · 下品（6 个）

| item_id | 中文名 | 贴图文件 | 状态 | 备注 |
|---------|--------|----------|------|------|
| `rejuvenation_pill_low` | 下品回春丹 | `rejuvenation_pill_low.png` | ❌ | 绿色系 |
| `foundation_building_pill_low` | 下品筑基丹 | `foundation_building_pill_low.png` | ❌ | 金色系 |
| `healing_pill_low` | 下品疗伤丹 | `healing_pill_low.png` | ❌ | 红色系 |
| `clear_spirit_powder_low` | 下品清灵散 | `clear_spirit_powder_low.png` | ❌ | 白色粉末/小瓶 |
| `fasting_pill_low` | 下品辟谷丹 | `fasting_pill_low.png` | ❌ | 米黄色 |
| `calming_pill_low` | 稳神丹（下品） | `calming_pill_low.png` | ❌ 🎨 | `generated_art/raw/mind_stabilizing_pill.png`，蓝色系 |

**设计要点**: 圆形丹丸为主（清灵散为小瓶），品级低则色泽暗淡；后续中/上/极品变体可加光环和纹理复杂度。

---

## 5. 符箓（3 个）

| item_id | 中文名 | 贴图文件 | 状态 | 色系 |
|---------|--------|----------|------|------|
| `fire_talisman` | 火弹符 | `fire_talisman.png` | ❌ | 赤红底 + 火纹符箓 |
| `armor_talisman` | 金甲符 | `armor_talisman.png` | ❌ | 金黄底 + 盾纹符箓 |
| `speed_talisman` | 疾行符 | `speed_talisman.png` | ❌ | 青蓝底 + 风纹符箓 |

**设计要点**: 矩形符纸轮廓，折叠质感；符文用对应属性颜色。统一外形方便辨识。

---

## 6. 法器 / 工具（6 个）

| item_id | 中文名 | 贴图文件 | 状态 | 备注 |
|---------|--------|----------|------|------|
| `spirit_charm` | 灵力护符 | `spirit_charm.png` | ❌ | Curios 饰品，翡翠护符 |
| `flying_sword` | 青竹飞剑 | `flying_sword.png` | ✅ | Curios artifact 槽 |
| `flying_artifact` | 御风法宝 | `flying_artifact.png` | ✅ | Curios artifact 槽 |
| `ling_gen_test_stone` | 灵根测试石 | `ling_gen_test_stone.png` | ✅ | 5 次使用，NBT 计数 |
| `spirit_detector` | 测灵盘 | `spirit_detector.png` | ✅ | 玉框罗盘造型 |
| `leyline_compass` | 寻脉罗盘 | `leyline_compass.png` | ✅ | 青铜罗盘造型 |

---

## 7. 方块物品（3 个）

| item_id | 中文名 | 贴图文件 | 状态 | 备注 |
|---------|--------|----------|------|------|
| `spirit_ore` | 灵石矿 | `spirit_ore.png` | ❌ | 方块贴图 `textures/block/` 也缺 |
| `meditation_cushion` | 蒲团 | `meditation_cushion.png` | ✅ (block) | `textures/block/meditation_cushion.png` 已有 |
| `spirit_gathering_array` | 聚灵阵 | `spirit_gathering_array.png` | ✅ (block) | `textures/block/spirit_gathering_array.png` 已有 |

**备注**: 方块物品的 item 贴图通常引用 block 贴图。`meditation_cushion` 和 `spirit_gathering_array` 的 block 贴图已就位，需确认 item 模型是否正确引用。`spirit_ore` 的 block 贴图也缺失。

---

## 8. 材料物品（19 个）

炼丹/炼器原材料。已全部注册语言文件但均无专属贴图。

### 8.1 灵草类（5 个）

| item_id | 中文名 | 贴图文件 | 状态 | 色系建议 |
|---------|--------|----------|------|----------|
| `spirit_grass` | 灵草 | `spirit_grass.png` | ❌ | 淡绿 |
| `cloud_mushroom` | 云雾菇 | `cloud_mushroom.png` | ❌ | 白/淡紫 |
| `phoenix_feather_flower` | 凤羽花 | `phoenix_feather_flower.png` | ❌ | 红/金 |
| `dragon_blood_grass` | 龙血草 | `dragon_blood_grass.png` | ❌ | 暗红 |
| `immortal_ginseng` | 仙参 | `immortal_ginseng.png` | ❌ | 米白/淡金 |

### 8.2 兽材类（5 个）

| item_id | 中文名 | 贴图文件 | 状态 | 色系建议 |
|---------|--------|----------|------|----------|
| `beast_core` | 妖兽内丹 | `beast_core.png` | ❌ | 深紫 |
| `spirit_beast_bone` | 灵兽骨 | `spirit_beast_bone.png` | ❌ | 乳白 |
| `dragon_scale` | 龙鳞 | `dragon_scale.png` | ❌ | 金/绿 |
| `phoenix_feather` | 凤凰羽 | `phoenix_feather.png` | ❌ | 赤金 |
| `true_dragon_blood` | 真龙血 | `true_dragon_blood.png` | ❌ | 深红（小瓶） |

### 8.3 矿物类（5 个）

| item_id | 中文名 | 贴图文件 | 状态 | 色系建议 |
|---------|--------|----------|------|----------|
| `spirit_iron` | 灵铁 | `spirit_iron.png` | ❌ | 银灰 |
| `cold_jade` | 寒玉 | `cold_jade.png` | ❌ | 冰蓝 |
| `star_meteorite` | 星陨铁 | `star_meteorite.png` | ❌ | 暗蓝/银 |
| `celestial_crystal` | 天晶石 | `celestial_crystal.png` | ❌ | 天蓝/白 |
| `chaos_gold` | 混沌金 | `chaos_gold.png` | ❌ | 黑金 |

### 8.4 稀有材料（4 个）

| item_id | 中文名 | 贴图文件 | 状态 | 色系建议 |
|---------|--------|----------|------|----------|
| `soul_fragment` | 魂魄碎片 | `soul_fragment.png` | ❌ | 幽紫 |
| `void_crystal` | 虚空结晶 | `void_crystal.png` | ❌ | 深紫/黑 |
| `time_sand` | 时光之砂 | `time_sand.png` | ❌ | 金色沙粒 |
| `primordial_essence` | 先天本源 | `primordial_essence.png` | ❌ | 彩虹/混色光球 |

---

## 9. 功法传承卷轴（60 + 1 个）

按 `cultivation/` JSON 的 `source` 字段自动生成。统一卷轴外形，按传承来源分类着色。

### 通用模板

| item_id | 中文名 | 贴图文件 | 状态 |
|---------|--------|----------|------|
| `technique_manual_default` | 功法卷轴（通用） | `technique_manual_default.png` | ❌ |

### 已有占位贴图的传承卷轴（60 个）

全部使用 `technique_manual_*_placeholder.png` 占位贴图。

| # | item_id | 中文名 | 状态 |
|---|---------|--------|------|
| 1 | `technique_manual_ancient_demon` | 古魔传承卷轴 | 🟡 |
| 2 | `technique_manual_ancient_demon_secret` | 古魔秘术传承卷轴 | 🟡 |
| 3 | `technique_manual_ancient_demonic_skill` | 上古魔功传承卷轴 | 🟡 |
| 4 | `technique_manual_ancient_secret_art` | 上古秘术传承卷轴 | 🟡 |
| 5 | `technique_manual_ancient_sword_sect` | 古剑门传承卷轴 | 🟡 |
| 6 | `technique_manual_azure_origin_sword_art` | 青元剑诀传承卷轴 | 🟡 |
| 7 | `technique_manual_azure_origin_sword_derivative` | 青元剑诀衍生传承卷轴 | 🟡 |
| 8 | `technique_manual_azure_origin_sword_spirit_realm` | 青元剑诀·灵界篇传承卷轴 | 🟡 |
| 9 | `technique_manual_azure_origin_sword_spirit_realm_pre` | 青元剑诀·灵界篇前置传承卷轴 | 🟡 |
| 10 | `technique_manual_azure_origin_sword_support` | 青元剑诀辅助传承卷轴 | 🟡 |
| 11 | `technique_manual_azure_origin_sword_support_skill` | 青元剑诀辅助功法传承卷轴 | 🟡 |
| 12 | `technique_manual_azure_sea_true_lord_skill` | 碧海真君成名功法传承卷轴 | 🟡 |
| 13 | `technique_manual_black_wind_flag_spirit` | 黑风旗器灵传承卷轴 | 🟡 |
| 14 | `technique_manual_brahma_sacred_fragment` | 梵圣真片传承卷轴 | 🟡 |
| 15 | `technique_manual_buddhist` | 佛门传承卷轴 | 🟡 |
| 16 | `technique_manual_chaotic_star_sea` | 乱星海传承卷轴 | 🟡 |
| 17 | `technique_manual_chaotic_star_sea_demonic` | 乱星海魔修传承卷轴 | 🟡 |
| 18 | `technique_manual_common` | 通用传承卷轴 | 🟡 |
| 19 | `technique_manual_common_low` | 通用低阶传承卷轴 | 🟡 |
| 20 | `technique_manual_common_tricks` | 通用小技巧传承卷轴 | 🟡 |
| 21 | `technique_manual_demon_domain_body_refining` | 魔域顶级炼体功传承卷轴 | 🟡 |
| 22 | `technique_manual_demon_race_secret` | 魔族秘传传承卷轴 | 🟡 |
| 23 | `technique_manual_demonic` | 魔道传承卷轴 | 🟡 |
| 24 | `technique_manual_evergreen_appendix` | 长春功附载传承卷轴 | 🟡 |
| 25 | `technique_manual_five_elements_escape` | 五行遁术传承卷轴 | 🟡 |
| 26 | `technique_manual_formation` | 阵法类传承卷轴 | 🟡 |
| 27 | `technique_manual_ghost` | 鬼道传承卷轴 | 🟡 |
| 28 | `technique_manual_gold_magnetic_spirit_wood` | 金磁灵木传承卷轴 | 🟡 |
| 29 | `technique_manual_gray_immortal_heritage` | 灰仙传承传承卷轴 | 🟡 |
| 30 | `technique_manual_great_development_formula` | 大衍诀传承卷轴 | 🟡 |
| 31 | `technique_manual_great_development_master` | 大衍神君传承卷轴 | 🟡 |
| 32 | `technique_manual_great_jin` | 大晋传承卷轴 | 🟡 |
| 33 | `technique_manual_han_li_self_created` | 韩立自创传承卷轴 | 🟡 |
| 34 | `technique_manual_heavenly_lan_temple` | 天澜圣殿传承卷轴 | 🟡 |
| 35 | `technique_manual_immortal_realm_skill` | 仙界功法传承卷轴 | 🟡 |
| 36 | `technique_manual_immortal_thunder_origin` | 仙界雷法本源传承卷轴 | 🟡 |
| 37 | `technique_manual_kunpeng_red_cloud_created` | 鲲鹏族红云老祖所创传承卷轴 | 🟡 |
| 38 | `technique_manual_little_pole_palace` | 小极宫传承卷轴 | 🟡 |
| 39 | `technique_manual_lost_true_immortal_art` | 上古失传真仙术传承卷轴 | 🟡 |
| 40 | `technique_manual_mortal_martial` | 世俗武林传承卷轴 | 🟡 |
| 41 | `technique_manual_mystic_herder_nascent_appendix` | 玄牧化婴附属传承卷轴 | 🟡 |
| 42 | `technique_manual_mystic_yin_appendix` | 玄阴经附属传承卷轴 | 🟡 |
| 43 | `technique_manual_nangong_wan_main` | 南宫婉主修传承卷轴 | 🟡 |
| 44 | `technique_manual_nascent_soul_common` | 元婴修士通用传承卷轴 | 🟡 |
| 45 | `technique_manual_nascent_soul_late_plus` | 元婴后期以上传承卷轴 | 🟡 |
| 46 | `technique_manual_orthodox` | 正道传承卷轴 | 🟡 |
| 47 | `technique_manual_placeholder` | （默认占位） | 🟡 |
| 48 | `technique_manual_purple_luo_mystic_skill` | 紫罗玄功传承卷轴 | 🟡 |
| 49 | `technique_manual_self_created` | 自创传承卷轴 | 🟡 |
| 50 | `technique_manual_seven_mysteries_sect` | 七玄门传承卷轴 | 🟡 |
| 51 | `technique_manual_six_paths_sage_created` | 六道极圣所创传承卷轴 | 🟡 |
| 52 | `technique_manual_spirit_taming_basic` | 御灵宗基础传承卷轴 | 🟡 |
| 53 | `technique_manual_spirit_taming_sect` | 御灵宗传承卷轴 | 🟡 |
| 54 | `technique_manual_supreme_demonic` | 魔道无上传承卷轴 | 🟡 |
| 55 | `technique_manual_thousand_bamboo_heritage` | 千竹教传承传承卷轴 | 🟡 |
| 56 | `technique_manual_thousand_illusion_sect` | 千幻宗传承卷轴 | 🟡 |
| 57 | `technique_manual_top_demonic` | 魔道顶阶传承卷轴 | 🟡 |
| 58 | `technique_manual_true_word_sect_heritage` | 真言门传承传承卷轴 | 🟡 |
| 59 | `technique_manual_yao` | 妖族传承卷轴 | 🟡 |
| 60 | `technique_manual_yao_bird_cultivator` | 妖族禽修传承卷轴 | 🟡 |
| 61 | `technique_manual_yuancha_saint_ancestor` | 元刹圣祖传承卷轴 | 🟡 |

**卷轴配色方案**（替换占位时参考）:

| 传承类别 | 卷轴主色 | 适用 source |
|----------|----------|-------------|
| 剑诀类 | 银白 | `azure_origin_sword_*`, `ancient_sword_sect` |
| 正道类 | 金黄 | `orthodox`, `buddhist`, `heavenly_lan_temple` |
| 魔道类 | 暗紫 | `demonic`, `top_demonic`, `supreme_demonic`, `chaotic_star_sea_demonic` |
| 妖族类 | 翠绿 | `yao`, `yao_bird_cultivator`, `kunpeng_red_cloud_created` |
| 鬼道类 | 灰黑 | `ghost`, `ancient_demon*` |
| 仙界类 | 天蓝/金 | `immortal_realm_skill`, `immortal_thunder_origin`, `lost_true_immortal_art` |
| 通用/世俗 | 棕黄 | `common*`, `mortal_martial`, `self_created` |
| 古修传承 | 古铜/暗金 | `great_development_*`, `han_li_self_created`, `six_paths_sage_created` |

---

## 统计总结

| 类别 | 总数 | ✅ 正式 | 🟡 占位 | ❌ 缺图 | 🎨 待适配 |
|------|------|---------|---------|---------|-----------|
| 通用灵石 | 4 | 0 | 0 | 4 | 0 |
| 五行灵石 | 20 | 20 | 0 | 0 | 20 |
| 货币 | 1 | 0 | 0 | 1 | 0 |
| 丹药 | 9 | 0 | 0 | 9 | 4 |
| 符箓 | 3 | 0 | 0 | 3 | 0 |
| 法器/工具 | 6 | 5 | 0 | 1 | 0 |
| 方块物品 | 3 | 2 | 0 | 1 | 0 |
| 材料 | 19 | 0 | 0 | 19 | 0 |
| 传承卷轴 | 61 | 0 | 61 | 0 | 0 |
| **合计** | **126** | **27** | **61** | **38** | **24** |

---

## 优先级建议

### 第一批 — 核心游戏循环（8 个）
通用灵石 4 + 丹药 3（回灵丹/凝气丹/破境丹）+ 灵石矿 1
> 理由：灵石吸收、丹药服用、挖矿是玩家最早接触的系统。

### 第二批 — 战斗与辅助（7 个）
符箓 3 + 灵力护符 1 + 仙玉 1 + 稳神丹 1 + 清灵散 1
> 理由：战斗消耗和走火入魔对策物品。

### 第三批 — 品质丹药（5 个）
回春丹/筑基丹/疗伤丹/辟谷丹（均为下品）
> 理由：炼丹系统入门物品。

### 第四批 — 材料物品（19 个）
灵草 5 + 兽材 5 + 矿物 5 + 稀有材料 4
> 理由：炼丹/炼器原料，可分批补齐。

### 第五批 — 传承卷轴替换（60 个）
按配色方案批量生成正式卷轴贴图替换占位。
> 理由：占位贴图功能可用，视觉统一度可后续提升。

---

## 美术风格规范

### 通用要求
- **像素风格**: 16×16 或 32×32，硬边缘，无抗锯齿
- **透明背景**: 物品贴图一律透明底（方块除外）
- **轮廓**: 使用深色描边（比主体色深 2-3 级）
- **高光**: 块状高光，不使用渐变
- **可读性**: 缩小到 16×16 时仍能辨识主体轮廓

### 五行色系标准

| 属性 | 主色 | 辅色 | 描边色 |
|------|------|------|--------|
| 金 | `#DAA520` | `#FFD700` | `#8B6914` |
| 木 | `#2E8B57` | `#3CB371` | `#1B5E20` |
| 水 | `#4FC3F7` | `#81D4FA` | `#0277BD` |
| 火 | `#E53935` | `#FF7043` | `#B71C1C` |
| 土 | `#8D6E63` | `#A1887F` | `#4E342E` |

### Prompt 模板（AI 生图用）

```
A clear pixel art fantasy RPG inventory icon, same style as a Minecraft mod item sprite,
transparent background, centered single object, chunky pixel clusters, thick dark outline,
bright blocky highlights, high contrast, limited color palette, crisp hard pixel edges,
readable at 16x16 and 32x32, no realistic rendering, no smooth 3D, no text, no watermark,
no UI, no hands.

Subject: [具体物品描述]
Visual details: front view, object fills 80 percent of canvas, [光效描述],
bright blocky highlights, thick dark outline, simple readable silhouette.
Main colors: [主色], [辅色], [高光色] and [描边色] outline.
Avoid: realistic rendering, smooth gradients, complex background,
tiny unreadable details, scene, character, excessive glow, text labels.
```

---

## 已有 AI 生成原图（`generated_art/raw/`）

以下 24 张原图已生成，需缩放至 16×16 或 32×32 后放入 `textures/item/`：

| 原图文件 | 对应 item_id | 备注 |
|----------|-------------|------|
| `metal_spirit_stone.png` | `metal_spirit_stone` | ✅ 已适配 |
| `metal_spirit_stone_mid.png` | `metal_spirit_stone_mid` | ✅ 已适配 |
| `metal_spirit_stone_high.png` | `metal_spirit_stone_high` | ✅ 已适配 |
| `metal_spirit_stone_superior.png` | `metal_spirit_stone_superior` | ✅ 已适配 |
| `wood_spirit_stone.png` | `wood_spirit_stone` | ✅ 已适配 |
| `wood_spirit_stone_mid.png` | `wood_spirit_stone_mid` | ✅ 已适配 |
| `wood_spirit_stone_high.png` | `wood_spirit_stone_high` | ✅ 已适配 |
| `wood_spirit_stone_superior.png` | `wood_spirit_stone_superior` | ✅ 已适配 |
| `water_spirit_stone.png` | `water_spirit_stone` | ✅ 已适配 |
| `water_spirit_stone_mid.png` | `water_spirit_stone_mid` | ✅ 已适配 |
| `water_spirit_stone_high.png` | `water_spirit_stone_high` | ✅ 已适配 |
| `water_spirit_stone_superior.png` | `water_spirit_stone_superior` | ✅ 已适配 |
| `fire_element_spirit_stone.png` | `fire_element_spirit_stone` | ✅ 已适配 |
| `fire_element_spirit_stone_mid.png` | `fire_element_spirit_stone_mid` | ✅ 已适配 |
| `fire_element_spirit_stone_high.png` | `fire_element_spirit_stone_high` | ✅ 已适配 |
| `fire_element_spirit_stone_superior.png` | `fire_element_spirit_stone_superior` | ✅ 已适配 |
| `earth_spirit_stone.png` | `earth_spirit_stone` | ✅ 已适配 |
| `spirit_recovery_pill.png` | `qi_recovery_pill` | ❌ 待适配 |
| `qi_condensing_pill.png` | `cultivation_pill` | ❌ 待适配 |
| `foundation_establishment_pill.png` | `breakthrough_pill` | ❌ 待适配 |
| `mind_stabilizing_pill.png` | `calming_pill_low` | ❌ 待适配 |
| `mystic_vial.png` | — | 备用素材 |
| `spirit_liquid.png` | — | 备用素材 |
| `waste_pill.png` | — | 备用素材 |

---

**文档版本**: v3.0
**下一步**: 按优先级批次使用 AI 绘图工具生成缺失贴图，缩放后放入资源目录
