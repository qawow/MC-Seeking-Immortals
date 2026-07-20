# 术法缺口详细分类（续）

## 四、特殊类型术法

### 11. 墙体类 (wall) - 缺口 3 条

**缺失术法**:
- qingyan_earth_spike_wall (青岩刺壁) - 土墙
- spirit_fengyuan_wind_wall (风元风墙) - 风墙
- tianfu_paper_shield_wall (符墙) - 符纸墙

**现状**: WindWallSpell, EarthWallSpell 已存在，但仅注册了部分 SkillType，未覆盖 JSON 中的 wall 类型术法。

### 12. 陷阱类 (trap) - 缺口 8 条

**缺失术法**:
- small_sword_array (小剑阵) - 剑阵陷阱
- illusion_formation (迷踪阵) - 幻术陷阱
- thunder_trap_array (雷劫阵) - 雷阵陷阱
- kill_sword_formation (杀阵) - 杀阵
- sand_bury (流沙术) - 流沙陷阱

**现状**: FormationSpell 包含部分陷阱阵法，但 trap 类型未完全覆盖。

### 13. 近战类 (melee) - 缺口 31 条

**典型缺失术法**:
- palm_wind (掌风) - 体修掌法
- bone_crush (碎骨拳) - 体修拳法
- qi_burst_palm (气爆掌) - 体修气劲掌
- tiansha_prison_force (天煞镇狱劲) - 体修劲力
- sect_body_intro (体修入门劲) - 体修基础

**现状**: 近战类无统一 Spell 基类，TargetedDebuffSpell 可部分模拟近距攻击，但缺少物理伤害和连招机制。

### 14. 打击类 (strike) - 缺口 4 条

**缺失术法**:
- tianlan_demon_subdue (天澜降魔印) - 佛门降魔
- tianmo_blood_altar_strike (血祭一击) - 魔道血祭
- spirit_art_lightning_palm (雷掌灵术) - 灵修雷掌
- immortal_sword_law_cut (剑则斩) - 剑修法则斩

## 五、移动与遁术类

### 15. 移动类 (movement) - 缺口 12 条

**典型缺失术法**:
- body_flash (身闪) - 体修闪身
- cloud_walk (云游步) - 道家云游
- void_step (虚步) - 虚空步法
- nether_ghost_walk (冥河鬼步) - 鬼道步法

**现状**: 移动类无统一 Spell 基类，部分用 SelfBuffSpell 加速度buff模拟，但缺少瞬移和特殊移动轨迹。

### 16. 突进类 (dash) - 缺口 16 条

**典型缺失术法**:
- qingxu_wind_step (清虚步) - 道家风遁突进
- luoyun_cloud_mist_escape (落云雾遁) - 元素遁术
- blood_escape (血遁) - 血遁
- earth_burrow (土遁) - 土遁

### 17. 短距传送类 (teleport_short) - 缺口 4 条

**缺失术法**:
- smuggle_rift_step (暗港遁步) - 走私线传送
- lingyun_void_step (灵云虚步) - 虚空瞬移
- void_immortal_blink (虚空仙瞬) - 仙人瞬移
- void_rift_step (虚空裂步) - 虚空裂隙

**现状**: EarthEscapeStepSpell 存在，但仅支持有限的传送效果。

### 18. 遁术类 (escape) - 缺口 2 条

**缺失术法**:
- blood_shadow_escape (血影遁) - 血影遁逃
- inverse_star_black_market_burst (黑市爆逃) - 黑市紧急逃脱

## 六、召唤与变形类

### 19. 召唤类 (summon) - 缺口 24 条

**缺失术法**:
- qingluo_worm_puppet (虫傀术) - 青罗虫傀
- xuewu_blood_puppet (血巫血傀) - 血巫傀儡
- spirit_art_beast_call (唤兽灵术) - 灵修唤兽
- lingshou_call_pet (灵兽召唤) - 灵兽召唤
- ghost_summon (鬼物召唤) - 鬼道召唤

**现状**: HonestSummonSpell 已注册 24 次，但需验证召唤的实体类型是否已实现（如 puppet_summon_basic, nascent_soul_avatar, ghost_king_avatar 等实体是否存在）。

### 20. 变形类 (transform) - 缺口 5 条

**缺失术法**:
- demon_form (魔化) - 魔修变身
- yao_transform_partial (妖修化形（初）) - 妖修化形
- jingzhe_partial_change (惊蛰化形（残）) - 兽修化形
- jingzhe_second_blood (惊蛰二变) - 兽修二次变身
- wanhu_beast_shift (妖狐化形) - 妖狐化形

**现状**: 变形类无实现，需要玩家外观/属性/技能完全替换的系统。

### 21. 召唤场地类 (summon_field) - 缺口 1 条

**缺失术法**:
- wuxing_world_seed (幻世种子) - 五行幻世种子

## 七、探测与功能类

### 22. 探测类 (scout) - 缺口 6 条

**缺失术法**:
- sense_scan (神识扫描) - 基础神识探测
- mind_read (读心) - 神识读心
- miaoyin_sense_echo (妙音回响探) - 声音探测
- time_sense_echo (时光残响探) - 时光回溯探测
- dayan_eye (大衍神眼) - 傀儡师神眼
- treasure_hunt_sense (寻宝神识) - 寻宝探测

**现状**: DivineSenseSpell 包含 SENSE_SCAN 形态，但未接线到 JSON。

### 23. 扫描类 (scan) - 缺口 5 条

**缺失术法**:
- divine_sense_scan (神识探查) - 神识扫描
- divine_sense_lock (神识锁敌) - 神识锁定
- soul_attack_wave (神魂冲击) - 神魂攻击
- spirit_sever_generic_probe (炼神通探) - 炼神探测
- guanghan_map_probe (广寒图探) - 秘境地图探测

### 24. 神魂攻击类 (soul_attack) - 缺口 6 条

**缺失术法**:
- soul_cry_shock (啼魂尖啸) - 神魂尖啸
- jingshen_spike (惊神刺) - 惊神刺
- shishen_spike (失神刺) - 失神刺
- lian_shen_spike (炼神刺) - 炼神刺
- guiling_soul_hook (鬼灵摄魂钩) - 鬼道摄魂
- poluo_soul_pull (婆罗摄魂) - 佛门摄魂

**现状**: DivineSenseSpell 包含 SOUL_ATTACK_WAVE, SOUL_CRY_SHOCK 形态，但未完全覆盖神魂攻击类型。

### 25. 鉴定类 (inspect) - 缺口 1 条

**缺失术法**:
- treasure_appraisal_glimpse (鉴宝灵光) - 鉴宝术

### 26. 功能类 (utility) - 缺口 6 条

**缺失术法**:
- zhenyan_lesson_seal (授业印) - 传承印记
- star_palace_patrol_beacon (巡海信标) - 星宫信标
- luoyun_message_talisman_net (落云传讯网) - 通讯网络
- star_palace_register_seal (星宫注册印) - 星宫注册
- inverse_star_smuggle_route (逆星走私线) - 走私路线
- reincarnation_trade_seal (轮回交易契) - 交易契约

### 27. 战斗功能类 (utility_combat) - 缺口 1 条

**缺失术法**:
- time_reversion_blink (时光逆转·一瞬) - 时光逆转

### 28. 指令类 (command) - 缺口 6 条

**缺失术法**:
- beast_tame_bond (御兽契约) - 御兽契约
- tianlan_beast_soul_link (天澜兽魂契) - 兽魂链接
- lingshou_frenzy_command (狂化驱令) - 狂化指令
- puppet_control_basic (控傀诀) - 傀儡控制
- dayan_puppet_link (大衍傀丝) - 傀儡连线
- beast_soul_merge_command (兽魂融合令) - 兽魂融合

**现状**: 指令类无实现，需要召唤物AI指令系统。

### 29. 炼器门类 (craft_gate) - 缺口 1 条

**缺失术法**:
- beast_soul_puppet_bind (兽魂傀绑) - 兽魂绑定到傀儡

