# 术法缺口详细分类（按类型）

## 一、通用伤害类

### 1. 弹射类 (projectile) - 缺口 110 条

#### 按元素分布：
- **neutral/中性**: 32 条（最大缺口）
- **metal/金**: 13 条
- **yin/阴**: 10 条
- **fire/火**: 10 条
- **water/水**: 7 条
- **ice/冰**: 7 条
- **earth/土**: 6 条
- **wood/木**: 6 条
- **thunder/雷**: 5 条
- **wind/风**: 4 条
- **light/光**: 3 条
- **blood/血**: 3 条
- **soul/神魂**: 2 条
- **demon/魔**: 1 条
- **dark/暗**: 1 条

**典型缺失术法**:
- metal_needle (金针术) - 金系基础术
- water_arrow (水箭术) - 水系基础术
- ice_shard (冰锥术) - 冰系基础术
- vine_arrow (木箭术) - 木系基础术
- wind_blade (风刃术) - 风系基础术
- scroll_strike (书卷击) - 儒家术法
- elemental_burst_* 系列 - 各元素爆发术

**现状**: ElementalProjectileSpell 已支持 9 种基础元素 + 3 种变体，但缺少 earth/yin/yang/soul/blood/void/neutral 支持。

### 2. 范围伤害类 (aoe) - 缺口 95 条

#### 按元素分布：
- **neutral/中性**: 14 条
- **fire/火**: 14 条
- **metal/金**: 13 条
- **yin/阴**: 12 条
- **earth/土**: 8 条
- **water/水**: 5 条
- **wind/风**: 5 条
- **thunder/雷**: 4 条
- **yang/阳**: 3 条
- **light/光**: 3 条
- **ice/冰**: 3 条
- **demon/魔**: 2 条
- **soul/神魂**: 2 条
- **wood/木**: 2 条
- **illusion/幻**: 2 条
- **blood/血**: 1 条
- **earth_wind/土风**: 1 条
- **space/空间**: 1 条

**典型缺失术法**:
- flame_ring (火环术) - 自身范围火焰
- lava_burst (熔岩爆) - 火系AOE
- demon_flame (魔焰术) - 魔道火焰
- primordial_magnet_sphere (元磁神光) - 金系终极AOE
- bagua_seal (八卦印) - 道家AOE
- five_thunder (五雷正法) - 雷法大威力
- buddha_light (佛光) - 佛门普照
- cyclone (旋风术) - 风系AOE
- blizzard (暴雪术) - 冰系AOE
- ink_sea (墨海) - 儒家AOE
- mist_rain (雾雨术) - 水系AOE

**现状**: ElementalAreaSpell 支持 6 种形态（LAVA, MIST_RAIN, SAND_STORM, BLIZZARD, CYCLONE, CHAIN_THUNDER），CoreElementalAreaSpell 支持 4 种高阶形态，但大量中低阶通用AOE未覆盖。

### 3. 射线类 (beam) - 缺口 43 条

#### 按元素分布：
- **metal/金**: 11 条（剑气主要元素）
- **neutral/中性**: 5 条
- **demon/魔**: 4 条
- **soul/神魂**: 4 条
- **yin/阴**: 4 条
- **yang/阳**: 3 条
- **fire/火**: 3 条
- **void/虚空**: 2 条
- **wood/木**: 2 条
- **blood/血**: 2 条
- **thunder/雷**: 1 条
- **light/光**: 1 条
- **illusion/幻**: 1 条

**典型缺失术法**:
- gold_beam (金芒术) - 金系射线基础
- pure_yang_sword (纯阳剑气) - 道家剑气
- qingxu_sword_clear (清虚剑意) - 剑修射线
- body_hardness (金刚不坏) - 体修射线
- barbarian_roar (蛮荒咆哮) - 体修吼技
- vajra_palm (金刚掌) - 佛门掌法
- confucian_righteous_qi (浩然正气) - 儒家正气
- underworld_flame (冥火) - 阴系火焰
- soul_attack_wave (神魂冲击) - 神识攻击

**现状**: 射线类没有统一的 Spell 基类，现有术法分散在各形态库中（DaoSpell, BuddhistSpell, SwordTechniqueSpell, DivineSenseSpell 等）。

## 二、状态增益/减益类

### 4. 自身增益类 (buff_self) - 缺口 110 条

**典型缺失术法**:
- vajra_body (金刚诀) - 体修防御buff
- iron_skin (铁肤术) - 体修皮肤硬化
- dragon_strength (龙力诀) - 体修力量提升
- tianmo_blood_armor (天魔血甲) - 魔修血甲
- sarira_shield (舍利护体) - 佛门护盾
- aura_body_shield (灵气护体) - 通用护体
- frost_armor (冰甲术) - 冰系防御

**现状**: SelfBuffSpell 支持 MobEffect 双层buff，但大量中性/特殊效果的buff（如境界临时提升、灵根加成、灵力回复加速等）无法用原版 MobEffect 表达。

### 5. 目标增益类 (buff) - 缺口 35 条

**典型缺失术法**:
- spirit_beast_contract (灵兽契约术) - 御兽buff
- tianlan_beast_soul_link (天澜兽魂契) - 兽魂增益

### 6. 目标减益类 (debuff) - 缺口 41 条

**典型缺失术法**:
- word_suppress (言镇) - 儒家言出法随
- taoist_seal (道家法印) - 道家封印
- immortal_rope (捆仙绳术) - 道家束缚
- soul_devour (摄魂术) - 魔道摄魂
- demon_contract (魔契) - 魔道契约

**现状**: TargetedDebuffSpell 支持双层 MobEffect debuff，但无法表达修仙特有的减益（封印灵力、封印神识、境界压制、心魔种植等）。

### 7. 控制类 (control) - 缺口 24 条

**典型缺失术法**:
- wood_spirit_vine (木灵藤) - 木系束缚控制
- xuantian_ice_prison (玄天冰牢) - 冰系囚禁
- great_vehicle_seal (大乘印) - 佛门封印
- zhenyan_command_bind (真言缚) - 儒家言咒

**现状**: 控制类效果散布在各处，无统一 Spell 基类。

## 三、领域/场地类

### 8. 领域类 (domain) - 缺口 4 条

**缺失终极领域**:
- wuxing_full_world (大五行幻世) - 道家五行领域
- dao_ancestor_glimpse_domain (道祖窥影域) - 道家终极领域
- gray_nether_domain (灰冥域) - 鬼道领域
- immortal_sword_domain (仙剑域) - 剑修领域

**现状**: 无 domain 专用 Spell 基类。

### 9. 场地增益类 (field) - 缺口 24 条

**典型缺失术法**:
- tiansha_prison_domain (天煞狱域) - 体修领域
- qingxu_cloud_sword_array (清云小剑阵) - 剑阵场地
- fallen_demon_mist (坠魔雾) - 魔道迷雾

### 10. 区域增益类 (buff_zone) - 缺口 7 条

**典型缺失术法**:
- spirit_gather_array (聚灵阵) - 聚灵阵场地
- defense_formation (守阵) - 防御阵法
- heaven_patrol_aura (巡天威) - 道家威压光环
- tianlan_war_front_oath (天澜前线誓) - 佛门战阵

**现状**: FormationSpell 包含部分阵法，但 buff_zone 类型的持续范围增益无专用实现。

