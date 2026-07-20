# 术法缺口详细分类（续）

## 八、治疗与恢复类

### 30. 治疗类 (heal) - 缺口 9 条

**缺失术法**:
- spring_wood_heal_minor (长春回春) - 木系治疗
- spirit_art_holy_light (圣光灵术) - 灵修圣光
- spirit_art_heal (愈灵术) - 灵修治疗
- heal_qi (回气术) - 回气治疗
- body_repair (疗伤术) - 疗伤
- group_heal (群疗术) - 群体治疗
- revive_weak (救死扶伤) - 复苏术
- medicine_king_heal (药王疗术) - 药王治疗
- sanzhuan_full_restore (三转圆满) - 三转恢复

**现状**: RecoverySpell 包含 HEAL_QI, BODY_REPAIR, GROUP_HEAL, REVIVE_WEAK 形态，但未接线到 JSON。

### 31. 回神类 (heal_spirit) - 缺口 2 条

**缺失术法**:
- spirit_recovery (凝神术) - 神识恢复
- sanzhuan_rebuild (三转固元) - 三转固本

**现状**: RecoverySpell 包含 SPIRIT_RECOVERY 形态，但未接线。

### 32. 净化类 (cleanse) - 缺口 4 条

**缺失术法**:
- qingxu_clear_mind (清虚涤念) - 道家涤念
- qingxu_seal_noise (清虚封喧) - 道家封喧
- detoxify (解毒术) - 解毒
- medicine_king_detox (药王解灵毒) - 药王解毒

**现状**: RecoverySpell 包含 DETOXIFY 形态，但未接线。

## 九、持续伤害与特殊效果类

### 33. 持续伤害类 (dot) - 缺口 4 条

**缺失术法**:
- qingluo_heart_toxin (蚀心毒) - 剧毒DOT
- xuewu_hex_decay (血巫朽咒) - 诅咒DOT
- blood_corruption (污血术) - 污血DOT
- yin_corrosion (阴蚀) - 阴蚀DOT

**现状**: 无DOT专用 Spell 基类，需用 MobEffect 模拟，但无法表达灵力持续损耗、境界腐蚀等修仙特有DOT。

### 34. 范围持续伤害类 (aoe_dot) - 缺口 2 条

**缺失术法**:
- qingluo_toxin_mist (青罗毒瘴) - 毒瘴范围DOT
- soul_devouring_cloud (噬魂云) - 噬魂云范围DOT

**现状**: XuanYinSpell 包含 SOUL_DEVOURING_CLOUD 形态，但未接线。

### 35. 吸取类 (drain) - 缺口 5 条

**缺失术法**:
- yin_soul_devour (噬魂阴风) - 噬魂吸取
- gray_soul_drain (灰界抽魂) - 灰界抽魂
- suyu_cycle_drain (素女轮回吸) - 素女吸取
- hehuan_dual_siphon (合欢摄元) - 合欢摄元
- spirit_absorb (摄灵术) - 摄灵吸取

**现状**: 吸取类无实现，需要生命/灵力转移机制。

### 36. 锥形类 (cone) - 缺口 3 条

**缺失术法**:
- nongyan_flame_burst (弄焰喷火) - 火焰锥形
- luoyun_spirit_flame_combat (落云丹火（战）) - 丹火锥形
- frost_breath (寒息) - 冰息锥形

**现状**: IceConeSpell 存在，但仅支持冰系，未覆盖其他元素的锥形攻击。

### 37. 连锁类 (chain) - 缺口 1 条

**缺失术法**:
- lightning_chain (连锁雷) - 连锁雷电

**现状**: ElementalAreaSpell 包含 CHAIN_THUNDER 形态，但 lightning_chain 作为单独术法未接线。

## 十、终极技与秘术类

### 38. 终极技类 (ultimate) - 缺口 47 条

**按门派分类的终极技缺口**:

#### 道家终极技 (7条):
- wuxing_full_world (大五行幻世)
- dao_ancestor_glimpse_domain (道祖窥影域)
- qingxu_ultimate_strike (清虚一击)
- great_vehicle_world_press (大乘界压)
- void_refining_domain (炼虚域)
- star_palace_heaven_seal (星宫天印)

#### 魔道终极技 (8条):
- xuewu_grand_curse (血巫大咒)
- tianmo_demon_body_secret (天魔魔体秘)
- fallen_demon_transform (坠魔变)
- diyuan_blood_sacrifice_ultimate (地渊血祭终式)
- qingluo_ten_poison_seal (青罗十毒印)
- hehuan_union_secret (合欢合一秘)

#### 神识终极技 (3条):
- reincarnation_judgment (轮回裁断)
- spirit_severing_flash (斩灵一闪)
- soul_severing_ultimate (断魂终式)

#### 幻术终极技 (4条):
- miaoyin_finale_kill (妙音杀调)
- wanhu_thousand_phantom_domain (万狐千幻域)
- yanyue_phantom_array (掩月幻阵)
- illusion_world (幻世)

#### 阵法终极技 (3条):
- luoyun_grand_array_node (落云大阵节点催)
- nine_palace_seal_secret (九宫封印秘)
- tianyuan_boundary_break (天元界破)

#### 剑修终极技 (5条):
- sword_formation_secret (剑阵秘)
- immortal_sword_domain (仙剑域)
- true_immortal_sword_art (真仙剑诀)
- wan_sword_return (万剑归宗)
- blade_giant_sword_ultimate_secret (巨剑终式)

#### 佛门终极技 (2条):
- great_vehicle_dharma_body (大乘法身)
- tianlan_ultimate_subdue (天澜终极降魔)

#### 傀儡终极技 (3条):
- dayan_puppet_legion (大衍傀儡军团)
- puppet_qianzhu_ultimate_secret (千傀终式)
- puppet_yuling_ultimate_secret (御灵终式)

#### 鬼道终极技 (3条):
- gray_nether_domain (灰冥域)
- yinluo_soul_harvest (阴罗摄魂终式)
- yin_cluster_ghost_ultimate_secret (阴聚终式)

#### 其他终极技 (9条):
- inverse_star_covert_ultimate_secret (逆星隐匿终式)
- demonic_guiling_ultimate_secret (鬼灵魔宗终式)
- beast_soul_fusion (兽魂融合终式)
- spatial_tear_escape (空间撕裂遁)
- tianmo_berserk (天魔狂化)
- bloodline_awaken (血脉觉醒)
- time_reversion_blink (时光逆转·一瞬)
- void_palace_heaven_earth (虚宫天地)
- inverse_star_black_market_burst (黑市爆逃)

**现状**: 所有终极技均未接线，effect type = ultimate 的 22 条术法需要特殊的高威力/大范围/长CD实现。

### 39. 秘术类 (secret_art) - 缺口 21 条

**典型秘术**:
- artifact_spirit_awaken_secret (器灵初醒) - 器灵唤醒
- auction_bid_insight_secret (竞价天机) - 拍卖洞察
- blood_shadow_escape (血影遁) - 血影遁逃
- void_rift_step (虚空裂步) - 虚空裂隙
- pill_soul_condense_secret (丹魂凝炼秘) - 炼丹秘术
- huadao_slash_secret (化道斩秘) - 化道斩击
- reincarnation_trade_seal (轮回交易契) - 轮回交易

**现状**: SecretElementalSpell 仅包含 3 种元素秘术（FIVE_ELEMENT_FUSION, LIFE_FIRE, TRUE_FIRE_HEAVEN），大量非战斗秘术（炼丹、炼器、交易、拍卖等）无实现。

## 十一、符箓消耗类

### 40. 符箓消耗类 (talisman_consume) - 缺口 29 条

**CAST_* 符箓列表**:
1. cast_fire_burst_talisman (爆炎符·瞬发)
2. cast_ice_seal_talisman (冰封符·瞬发)
3. cast_escape_heaven_talisman (遁天符·瞬发)
4. cast_spirit_fix_talisman (定神符·瞬发)
5. cast_thunder_talisman (雷霄符·瞬发)
6. cast_soul_lock_talisman (锁魂符·瞬发)
7. cast_teleport_array_talisman (传送阵符·瞬发)
8. cast_beast_contract_talisman (御兽契符·瞬发)
9. cast_anti_demon_talisman (驱魔符·瞬发)
10. cast_yin_protect_talisman (阴护符·瞬发)
11. cast_ghost_hide_talisman (隐鬼符·瞬发)
12. cast_space_anchor_talisman (空间锚符·瞬发)
13. cast_life_save_talisman (护命符·瞬发)
14. cast_earth_wall_talisman (土墙符·瞬发)
15. cast_mirage_heart_talisman (幻心符·瞬发)
16. cast_invisibility_talisman (隐身符·瞬发)
17. cast_spirit_gather_talisman (聚灵符·瞬发)
18. cast_illusion_talisman (幻术符·瞬发)
19. cast_star_palace_patrol_talisman (巡星符·瞬发)
20. cast_golden_armor_talisman (金甲符·瞬发)
21. cast_inverse_star_cipher_talisman (逆星密符·瞬发)
22. cast_bu_tian_talisman (补天符·瞬发)
23. cast_wood_bind_talisman (木缚符·瞬发)
24. cast_metal_blade_talisman (金刃符·瞬发)
25. cast_void_palace_key_talisman (虚宫钥符·瞬发)
26. cast_talisman_wooden_ox (木牛符·瞬发)

**现状**: 
- 26 个 CAST_* 已全部注册到 SkillType
- 25 个使用 TalismanConsumeSpell，1 个使用 SelfBuffSpell (CAST_GHOST_HIDE_TALISMAN)
- TalismanConsumeSpell 通过 effectKey 分发到不同效果（projectile/aoe/buff/control/teleport 等）
- **需要验证**: effectKey 对应的实际效果是否已实现

