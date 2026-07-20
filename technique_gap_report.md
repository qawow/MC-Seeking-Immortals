# 术法实现缺口完整清单

## 总体概况

- **JSON 术法总数**: 747 条
- **SkillType 枚举**: 388 个
- **已注册效果**: 373 个 register() 调用
- **已接线 ID 别名**: 20 个 registerTechniqueAlias() 调用
- **接线率**: 2.7% (20/747)
- **未接线术法**: 727 条 (97.3%)

## 关键发现

### 1. 接线状态严重不足

仅有 20 条术法通过 `registerTechniqueAlias()` 连接到 SkillType：
- fireball, earth_spike, five_elements_escape, ice_cone, thunder_strike
- earth_escape, aura_detection, entangling, voice_transmission, object_control
- quicksand, water_shield, earth_prison, wind_binding, wind_wall
- big_dipper_sword_array, light_body/lightness_skill, soul_search, demon_subdue

其余 727 条 JSON 术法无法通过 technique_id 查询到对应的 SkillEffect。

### 2. CAST_* 符箓系统状态

- **SkillType 中的 CAST_* 枚举**: 26 个
- **已注册效果**: 26 个（全部注册）
- **使用 TalismanConsumeSpell**: 25 个
- **使用 SelfBuffSpell**: 1 个 (CAST_GHOST_HIDE_TALISMAN)

**评估**: CAST_* 系列已完成接线，但 TalismanConsumeSpell 的 effectKey 是否真实消费了相应效果需要运行时验证。

### 3. 召唤术实现状态

- **JSON 中 summon 类型**: 24 条
- **HonestSummonSpell 使用**: 24 次
- **已接线召唤术**: 24 个 SkillType 条目

**评估**: 召唤术覆盖完整，但需检查召唤的实体是否真实存在：
- puppet_summon_basic, nascent_soul_avatar, spirit_art_beast_call
- spirit_art_holy_beast_call, ghost_king_avatar, guiling_corpse_summon
- beast_summon, gold_devour_swarm, summon_wood_puppet, puppet_swarm
- iron_puppet, puppet_control_basic, puppet_swarm_command
- beast_soul_puppet_bind, second_nascent_soul, dayan_puppet_legion
- blood_demon_avatar, ghost_king_summon, mulan_holy_bird_call
- beast_soul_fusion_secret, demonic_guiling_ultimate_secret
- yin_cluster_ghost_ultimate_secret, puppet_qianzhu_ultimate_secret
- puppet_yuling_ultimate_secret, beast_soul_fusion

### 4. Ultimate 终极技状态

- **JSON 中 ultimate 标签**: 28 条
- **effect type = ultimate**: 22 条
- **合计**: 47 条终极技

**已接线**: 0 条

**缺失终极技代表**:
- wuxing_full_world (大五行幻世)
- xuewu_grand_curse (血巫大咒)
- reincarnation_judgment (轮回裁断)
- miaoyin_finale_kill (妙音杀调)
- wanhu_thousand_phantom_domain (万狐千幻域)
- fallen_demon_transform (坠魔变)
- spirit_severing_flash (斩灵一闪)
- void_refining_domain (炼虚域)
- great_vehicle_dharma_body (大乘法身)
- true_immortal_sword_art (真仙剑诀)

### 5. Secret Art 秘术状态

- **effect type = secret_art**: 21 条
- **tags 包含 secret**: 16 条

**已接线**: 0 条

**缺失秘术代表**:
- artifact_spirit_awaken_secret (器灵初醒)
- auction_bid_insight_secret (竞价天机)
- blood_shadow_escape (血影遁)
- void_rift_step (虚空裂步)
- inverse_star_black_market_burst (黑市爆逃)

### 6. 元素族 VFX 支持缺口

**ElementalProjectileSpell 已支持**:
- FIRE, WATER, METAL, DARK, LIGHT, WIND, WOOD, ICE, THUNDER
- 特殊变体: ICE_SPEAR, FLAME_BURST, FIRE_SERPENT

**缺失常见元素**:
- earth/土 (6条 projectile 术法)
- yin/阴 (10条 projectile 术法)
- yang/阳 (需要)
- soul/神魂 (2条 projectile 术法)
- blood/血 (3条 projectile 术法)
- void/虚空 (需要)
- neutral/中性 (32条 projectile 术法)

**建议**: 扩展 CultivationFireballEntity.SpellElement 枚举以支持：
- EARTH, YIN, YANG, SOUL, BLOOD, VOID, NEUTRAL

