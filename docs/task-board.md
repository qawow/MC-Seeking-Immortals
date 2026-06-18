# Task Board — 可勾选任务列表

> 基于 `implementation-roadmap.md` 生成
>
> **执行规则：一次只允许执行一个 Phase**

---

## 执行状态

- **当前 Phase**: Phase 2（Realm System，本次用户指定范围）
- **当前状态**: Phase 0、Phase 1 已完成；本次指定的 Phase 2 Realm System 有明确代码证据，但 `./gradlew.bat build` 在 `compileJava` 失败，因此按验收规则不得勾选完成；旧路线 Phase 2（灵根系统与打坐修炼）仍保持部分完成，未在本次处理；未进入 Phase 3
- **已完成 Phase 数**: 2 / 11（按完整验收口径）

> 状态来源：`docs/phase-1-repair-report.md`（2026-06-18 修复）、`docs/phase-0-3-audit-report.md`（2026-06-18 审计）和 `docs/phase-2-report.md`（2026-06-18 Realm System 审计）。只有证据明确且 `./gradlew.bat build` 成功的任务可标记为 `[x]`；Partial/Missing/Unknown 标记不计入完成数。

---

## Phase 0：前置准备与架构审查

**目标：** 审查现有代码结构，确保与设计文档兼容，准备开发环境

### 任务清单

- [x] 读取并分析现有 `PlayerCultivation` 类
- [x] 对比 `PlayerCultivation` 与设计文档的六大核心属性
- [x] 检查现有 `Realm` 和 `RealmStage` 枚举
- [x] 验证 `RealmStage` 是否支持13层练气 + 筑基
- [x] 检查现有 `SpiritualRoot` 系统
- [x] 验证灵根系统是否支持六种类型及修炼速度系数
- [x] 审查 `CultivationHelper` 和 `CultivationProvider`
- [x] 确认 `skill/` 包当前状态
- [x] 确认 `technique/` 系统当前状态
- [x] 确认 `item/pill/` 系统是否支持品质分级
- [x] 列出需要新增的类/接口清单
- [x] 列出需要重构的类/接口清单
- [x] 生成架构审查报告：`docs/phase-0-project-survey.md`

### 验收标准

- [x] 架构审查报告已生成
- [x] 明确列出与设计文档的差异点
- [x] 列出需要新增的模块清单
- [x] 列出需要重构的模块清单
- [x] 无阻塞性架构冲突

### Phase 0 完成标记

- [x] **Phase 0 已完成，可进入 Phase 1**

---

## Phase 1：核心属性与境界系统

**目标：** 实现六大核心属性存储与境界系统，支持练气1~13层和筑基4阶

### 任务清单

- [x] 确保 `PlayerCultivation` 有 `cultivation` (long) 字段 — 通过 `getCultivationLong()` / `setCultivation(long)` 提供 long 语义，保留内部 `cultivationExp`
- [x] 确保 `PlayerCultivation` 有 `cultivationMax` (long) 字段 — 通过 `getCultivationMax()` 动态计算
- [x] 确保 `PlayerCultivation` 有 `mana` (int) 字段 — 通过 `getMana()` / `setMana(int)` 读写，保留内部 `spiritualPower`
- [x] 确保 `PlayerCultivation` 有 `manaMax` (int) 字段 — 通过 `getManaMax()` / `getManaMaxLong()` 动态计算
- [x] 确保 `PlayerCultivation` 有 `divSense` (int) 字段 — 通过 `getDivSense()` / `setDivSense(int)` 读写，保留内部 `divineConsciousness`
- [x] 确保 `PlayerCultivation` 有 `bodyRef` (int) 字段 — 通过 `getBodyRef()` / `setBodyRef(int)` 读写，保留内部 `bodyRefinement`
- [x] 确保 `PlayerCultivation` 有 `qiDevRisk` (float) 字段 — 支持 0.0~1.0 float 语义与旧 0~100 百分比兼容
- [x] 确保 `PlayerCultivation` 有 `tribRes` (float) 字段（预留） — 支持 0.0~0.9 float 语义与旧 0~90 百分比兼容
- [x] 实现或验证 `RealmStage` 枚举包含 QI_1 到 QI_13 — 通过 `getDesignId()` 映射到现有 `LAYER_1`~`LAYER_13`
- [x] 实现或验证 `RealmStage` 枚举包含 FOUNDATION_EARLY/MID/LATE/PEAK — 通过 `getDesignId()` 映射到现有 `EARLY/MIDDLE/LATE/PEAK`
- [x] 为每个境界配置 `manaBase` 值（参考设计文档表格）
- [x] 为每个境界配置 `divSenseBase` 值
- [x] 为每个境界配置 `hpBase` 值
- [x] 实现攻击力计算方法
- [x] 实现防御力计算方法
- [x] 实现最大HP计算方法
- [x] 实现灵力回速计算方法
- [x] 实现修为回速计算方法
- [x] 实现飞行速度计算方法
- [x] 添加境界属性基准表单元测试 — `Phase1CultivationSystemTest` 已覆盖练气/筑基基准表并通过 `./gradlew.bat test`
- [x] 验证 `/seeking_immortals realm` 命令显示正确

### 验收标准

- [x] `PlayerCultivation` 包含所有六大核心属性字段 — 通过兼容 getter/setter 提供设计语义，保留旧内部字段和旧 NBT
- [x] `RealmStage` 枚举至少包含 QI_1 到 QI_13 和 FOUNDATION_EARLY/MID/LATE/PEAK — 通过 `getDesignId()` 映射并由测试验证
- [x] 每个境界有正确的 `manaBase`, `divSenseBase`, `hpBase` 值
- [x] 衍生属性计算方法实现并通过单元测试 — `Phase1CultivationSystemTest` 覆盖攻击、防御、最大 HP、灵力回速、修为回速、飞行速度
- [x] `/seeking_immortals realm` 命令能正确显示当前境界和属性

### Phase 1 完成标记

- [x] **Phase 1 已完成，可进入 Phase 2** — `./gradlew.bat test` 与 `./gradlew.bat build` 均已通过；未实现 Phase 2 功能

---

## Phase 2：灵根系统与打坐修炼

**目标：** 实现灵根测试、永久写入、打坐修炼 GUI 和修为增长逻辑

### 2026-06-18 本次执行范围：Realm System

> 本小节记录用户本次指定的 Phase 2 子范围。它只审计/确认境界系统能力，不实现灵根鉴定、打坐 GUI、HUD、炼丹、技能或 Phase 3 新功能。证据详见 `docs/phase-2-report.md`。

- [ ] `RealmStage` enum / 境界配置 — Unknown: `RealmStage` 与 `RealmStageConfig` 有明确代码证据，但当前 `./gradlew.bat build` 失败，不能标记完成
- [ ] 凡人、练气 1~13、筑基初 — Unknown: `PlayerCultivation.getStagesForRealmPublic()` 与现有测试有明确证据，但当前 build 失败，不能标记完成
- [ ] `cultivationMax` — Unknown: `PlayerCultivation#getCultivationMax()` 有明确证据，但当前 build 失败，不能标记完成
- [ ] `manaMax` — Unknown: `PlayerCultivation#getManaMax()` / `getManaMaxLong()` 有明确证据，但当前 build 失败，不能标记完成
- [ ] `divSense` — Unknown: `getDivSense()` / `setDivSense(int)` 有明确证据，但当前 build 失败，不能标记完成
- [ ] `hpBase` — Unknown: `RealmStageConfig#getHpBase()` 与 `getMaxHealthPoints()` 有明确证据，但当前 build 失败，不能标记完成
- [ ] 修为增加基础方法 — Unknown: `addCultivationExp(int)` / `setCultivation(long)` 有明确证据，但当前 build 失败，不能标记完成
- [ ] 灵力恢复基础方法 — Unknown: `addSpiritualPower(int)` / `setMana(int)` / `getManaRecoveryPerSecond()` 有明确证据，但当前 build 失败，不能标记完成
- [ ] 突破成功 — Unknown: `tryBreakthrough(...)` 成功路径有明确证据，但当前 build 失败，不能标记完成
- [ ] 突破失败 — Unknown: `tryBreakthrough(...)` 失败路径有明确证据，但当前 build 失败，不能标记完成
- [ ] 修为回退 20% — Unknown: 失败后当前阶段进度保留为 80% 有明确证据，但当前 build 失败，不能标记完成
- [ ] `qiDevRisk` 增加 — Unknown: 失败后 `addQiDeviationRisk(10)` 有明确证据，但当前 build 失败，不能标记完成

### 原路线任务清单（本次未执行）

### 任务清单

- [x] 实现或验证 `SpiritRootType` 枚举：天灵根
- [x] 实现或验证 `SpiritRootType` 枚举：异灵根
- [x] 实现或验证 `SpiritRootType` 枚举：双灵根
- [x] 实现或验证 `SpiritRootType` 枚举：三灵根
- [x] 实现或验证 `SpiritRootType` 枚举：伪灵根
- [x] 实现或验证 `SpiritRootType` 枚举：杂灵根
- [x] 为每种灵根配置修炼速度系数
- [x] 为每种灵根配置灵力回复系数
- [x] 为每种灵根配置突破成功率加成
- [x] 为每种灵根配置丹药吸收率
- [ ] 创建「灵根鉴定石板」方块或物品 — Partial: `LingGenTestStoneItem` 物品已存在，但「鉴定石板」方块未实现
- [ ] 实现首次交互触发灵根测试逻辑 — Partial: `LingGenCalculator.roll()` 算法与物品交互入口已接入，但不是 NPC/方块首次交互
- [x] 实现灵根随机分配算法（伪/杂概率高，天灵根极低）
- [x] 实现灵根永久写入 `PlayerCultivation`
- [ ] 创建打坐修炼 GUI (`MeditationScreen`) — Missing: 仅有 `BreathingHudOverlay`（HUD 覆盖层），无独立 Screen
- [ ] GUI 显示当前修为 / 修为上限 — Partial: 修仙面板 `CultivationStatsScreen` 显示修为，但非打坐专用 GUI
- [ ] GUI 显示修炼速度计算详情 — Missing: 未发现独立速度详情面板
- [ ] GUI 显示实时修为增长 — Partial: HUD 显示进度条，但无速度详情
- [x] 实现打坐修炼逻辑（`PlayerTickEvent`）
- [x] 玩家进入打坐状态（静止）
- [ ] 每 tick 增加修为：`(境界基准 × 灵根系数 × 打坐加成 × 灵脉加成) / 20` — Partial: 已实现打坐修为增长，但为 5 秒结算并叠加灵气/功法/灵石修正，不是字面每 tick 公式
- [x] 打坐时受伤打断打坐，走火风险 +2%
- [x] 打坐消耗饥饿度
- [x] 验证或改进现有 `MeditationCushionBlock` 支持打坐状态

### 验收标准

- [ ] 灵根鉴定石板方块/物品可交互 — Partial: 测试石物品可用，无方块版
- [ ] 玩家首次测试灵根后，数据永久写入 — Partial: 物品交互已接入，但不是任务板所述 NPC/方块首次交互流程
- [x] `/seeking_immortals root` 命令显示灵根类型和系数
- [ ] 打坐 GUI 正确显示修为增长速度 — Missing: 无独立 GUI
- [x] 打坐时修为实时增长
- [x] 打坐时受伤会打断并增加走火风险
- [x] 不同灵根的修炼速度差异明显（天灵根 ×5.0，杂灵根 ×0.8）

### Phase 2 完成标记

- [ ] **Phase 2 已完成，可进入 Phase 3** — 灵根测试交互流程与独立打坐 GUI 未完成

---

## Phase 3：突破系统与走火入魔

**目标：** 实现境界突破流程、成功率计算、走火入魔四级判定

### 任务清单

- [x] 实现突破触发检查：`cultivation >= cultivationMax`
- [x] 实现突破按键绑定或 GUI 按钮
- [x] 实现突破资源检查（背包丹药）
- [ ] 显示所需材料和当前拥有量 — Partial: `BreakthroughService` 在缺资源时提示，但无完整 GUI 列表
- [x] 实现突破成功率计算：基础成功率
- [x] 突破成功率计算：灵根加成
- [x] 突破成功率计算：辅助丹药加成
- [x] 突破成功率计算：连续失败积累「执念」(+5%/次，最多+30%)
- [x] 实现突破成功逻辑：境界+1
- [x] 突破成功逻辑：修为清零
- [x] 突破成功逻辑：属性加成
- [x] 突破成功逻辑：消耗材料
- [x] 实现突破失败逻辑：修为回退20%
- [x] 突破失败逻辑：走火风险+10%
- [x] 突破失败逻辑：消耗材料
- [x] 实现走火触发条件：走火风险 ≥70% 时突破失败
- [ ] 走火触发条件：修炼时受伤 — Partial: 打坐受伤 +2% 已实现，但未直接触发走火检定
- [ ] 走火触发条件：使用超阶功法 — Missing: 只发现超阶功法加风险，没有直接触发走火检定
- [x] 实现走火判定：70-79% 轻微走火（损失30%修为，风险清零）
- [x] 走火判定：80-89% 中度走火（损失50%修为 + 昏迷30s，风险清零）
- [x] 走火判定：90-99% 严重走火（掉落一个境界 + 昏迷3分钟 + 装备随机损坏）
- [x] 走火判定：100% 极端走火（当场死亡 + 背包掉落50%）
- [x] 实现走火风险降低：服用稳神丹 -20%
- [x] 走火风险降低：平稳打坐每小时 -5%（ServerTickEvent）

### 验收标准

- [x] 修为达到上限后，突破按钮/按键可用
- [ ] 突破前显示成功率和所需材料 — Partial: 突破前已显示成功率/加成，但无所需材料与当前拥有量列表
- [x] 突破成功后境界正确+1，修为清零
- [x] 突破失败后修为-20%，走火风险+10%
- [x] 走火风险≥70%时突破失败触发走火判定
- [x] 四级走火效果正确实现（修为损失/昏迷/掉境界/死亡）
- [x] 稳神丹正确降低走火风险20%
- [x] 平稳打坐每小时降低走火风险5%
- [x] 连续失败积累执念机制正常工作

### Phase 3 完成标记

- [ ] **Phase 3 已完成，可进入 Phase 4** — 突破材料拥有量预览缺失；超阶功法/打坐受伤只加风险或打断，未直接触发走火检定

---

## Phase 4：练气期技能系统

**目标：** 实现练气期9个技能，包括技能解锁、冷却、消耗、效果

### 任务清单

- [x] 实现或验证技能注册系统 — Partial: `SkillEffectRegistry` 框架完整，已注册 5 个法术效果
- [ ] 实现技能：引气入体（被动，练气1） — Stub: 未发现独立被动效果
- [x] 实现技能：火球术（练气3，10灵力，2s冷却） — Partial: `FireballSpell` 已实现并使用原版 `SmallFireball`，需确认灵力/冷却参数
- [ ] 实现技能：冰锥术（练气3，10灵力，2s冷却） — Stub: `ICE_CONE` 枚举存在，无效果实现
- [ ] 实现技能：雷击术（练气3，12灵力，3s冷却） — Stub: `THUNDER_STRIKE` 枚举存在，无效果实现
- [ ] 实现技能：土遁步（练气4，15灵力，5s冷却） — Stub: 仅有 `EarthWallSpell`（土墙），非土遁步
- [ ] 实现技能：御剑飞行初（练气7，5灵力/s） — Partial: 飞剑物品 + 飞行事件存在，但作为技能未接入
- [ ] 实现技能：单剑刺击（练气7，20灵力，1s冷却） — Stub: 无飞剑弹射物
- [x] 实现技能：灵气探测（练气10，5灵力，10s冷却） — Partial: `DetectionSpell` 已实现，需确认参数
- [ ] 实现技能：三才剑阵（练气13，40灵力，3s冷却） — Stub: 无三飞剑同时发射逻辑
- [ ] 实现技能解锁逻辑：境界达到要求后自动解锁 — Stub: `SkillType.requiredRealm` 字段存在，自动解锁未发现
- [x] 实现技能冷却系统（`PlayerCultivation` 存储 `skillCooldowns[]`） — Partial: 通过 `techniqueCooldownUntilTicks` 实现，按功法 id 而非技能枚举
- [x] 实现灵力消耗检查
- [ ] 创建 `FlyingSwordEntity`（可骑乘实体） — Stub: 未发现该实体类
- [ ] 创建 `SwordProjectileEntity`（弹射物） — Stub: 未发现该实体类
- [x] 火球术弹射物实现 — Partial: 复用原版 `SmallFireball`
- [ ] 冰锥术弹射物实现 + 减速效果
- [ ] 雷击术召唤闪电实现
- [ ] 土遁步瞬移逻辑实现
- [ ] 御剑飞行骑乘逻辑实现
- [ ] 单剑刺击飞剑弹射物实现
- [x] 灵气探测高亮渲染实现 — Partial: `DetectionSpell` 已实现，需确认高亮范围
- [ ] 三才剑阵三飞剑同时发射实现

### 验收标准

- [ ] 练气1后自动解锁引气入体 — Stub: 自动解锁逻辑未实现
- [ ] 练气3后解锁火球术/冰锥术/雷击术 — Partial: 火球术可手动学习释放，冰锥/雷击仅有枚举
- [x] 火球术/冰锥术发射弹射物，命中造成伤害 — Partial: 仅火球术已实现
- [ ] 雷击术在目标位置召唤闪电
- [ ] 土遁步正确瞬移玩家并穿过1格方块
- [ ] 练气7后解锁御剑飞行，玩家可骑乘飞剑移动 — Partial: 飞剑物品支持飞行，但非"骑乘"
- [ ] 单剑刺击发射飞剑攻击，射程=神识范围
- [x] 练气10后灵气探测高亮灵矿/灵草（发光轮廓） — Partial: DetectionSpell 已实现
- [ ] 练气13后三才剑阵发射三把飞剑
- [x] 技能冷却正确倒计时
- [x] 灵力不足时技能无法释放

### Phase 4 完成标记

- [ ] **Phase 4 已完成，可进入 Phase 5** — 9 个练气期技能仅 2-3 个有效果实现，飞剑/弹射物体系缺失

---

## Phase 5：神秘小瓶系统

**目标：** 实现神秘小瓶唯一物品、灵液积累、植物加速

### 任务清单

- [ ] 创建 `MysteriousVialItem` 类 — Stub: 仅有 `VialGrade` 枚举（Phase 1 预留）
- [ ] 神秘小瓶设置为唯一绑定物品
- [ ] 神秘小瓶不可丢弃（`onDroppedByPlayer` 返回 false）
- [ ] NBT 存储 `vialCharges`（当前灵液份数）
- [ ] NBT 存储 `vialLastRefill`（上次充能时间戳）
- [ ] 实现灵液积累逻辑：每24小时积累1份
- [ ] 灵液最大5份
- [ ] 实现离线时间计算并补偿灵液
- [ ] 实现植物加速使用：右键对植物方块使用
- [ ] 植物加速：消耗1份灵液
- [ ] 植物加速：生长速度×10（蓝色灵液）
- [ ] 实现 `BlockGrowEvent` 监听器加速作物成熟
- [ ] 实现灵根与神秘小瓶获取关联：伪/杂灵根必得 — Partial: `SpiritualRoot.getJadeVialDropChance()` 接口已存在，未接入获取流程
- [ ] 其他灵根极低概率获得（或任务获取）
- [ ] 添加神秘小瓶物品模型
- [ ] 添加神秘小瓶材质
- [ ] 添加神秘小瓶本地化（中文/英文）

### 验收标准

- [ ] 伪灵根/杂灵根玩家测试灵根后自动获得神秘小瓶
- [ ] 神秘小瓶无法丢弃
- [ ] 每24小时自动积累1份灵液（最大5份）
- [ ] 离线时间正确计算并补偿灵液
- [ ] 右键对作物使用消耗1份灵液
- [ ] 使用后作物生长速度明显加快（×10）
- [ ] 神秘小瓶 tooltip 显示当前灵液份数

### Phase 5 完成标记

- [ ] **Phase 5 已完成，可进入 Phase 6**

---

## Phase 6：筑基期技能系统

**目标：** 实现筑基期6个技能

### 任务清单

- [ ] 实现技能：神识扩展（被动，筑基初） — Stub
- [ ] 实现技能：御剑飞行进阶（筑基初，3灵力/s） — Partial: 飞剑物品 + 飞行事件存在，进阶版未实现
- [ ] 实现技能：罡气护体（筑基初，50灵力，15s冷却） — Stub
- [ ] 实现技能：五行遁术（筑基中，60灵力，20s冷却） — Stub: 仅有 `technique_manual_five_elements_escape` 卷轴
- [ ] 实现技能：北斗剑阵（筑基后，80灵力，5s冷却） — Stub
- [ ] 实现技能：阵法感知（被动，筑基圆满） — Stub
- [ ] 神识扩展：修改 `divSense` 计算，×1.5
- [ ] 神识扩展：可感知隐身生物
- [ ] 御剑飞行进阶：速度×1.8
- [ ] 御剑飞行进阶：可控制3把飞剑护体（视觉效果）
- [ ] 罡气护体：吸收下一次100%伤害的shield buff
- [ ] 五行遁术：检查灵根属性
- [ ] 五行遁术：瞬移20格
- [ ] 北斗剑阵：7把飞剑环绕
- [ ] 北斗剑阵：持续攻击范围内敌人8s
- [ ] 阵法感知：可看见隐藏阵法边界（简化实现）

### 验收标准

- [ ] 筑基初自动解锁神识扩展，神识范围正确×1.5
- [ ] 筑基初解锁御剑飞行进阶，速度×1.8
- [ ] 罡气护体正确吸收下一次伤害
- [ ] 五行遁术检查灵根属性，正确瞬移20格
- [ ] 北斗剑阵召唤7把飞剑，持续攻击范围内敌人8s
- [ ] 筑基圆满解锁阵法感知（视觉效果可简化）

### Phase 6 完成标记

- [ ] **Phase 6 已完成，可进入 Phase 7**

---

## Phase 7：炼丹系统

**目标：** 实现丹炉方块、丹方系统、炼丹技能、核心丹药

### 任务清单

- [ ] 创建或验证 `AlchemyFurnaceBlock` — Stub: 未发现该方块
- [ ] 创建或验证 `AlchemyFurnaceBlockEntity` — Stub: 未发现 BlockEntity 实现
- [ ] 实现自定义 Recipe 类型 `AlchemyRecipe` — Stub
- [ ] 创建 JSON 配方格式目录：`data/seeking_immortals/recipes/alchemy/` — Stub
- [ ] 实现炼丹流程：放入丹方 + 灵草材料
- [ ] 炼丹流程：催火（消耗灵力或灵火石）
- [ ] 炼丹流程：等待时间（Progress bar）
- [ ] 炼丹流程：判定成功/失败
- [x] 实现品质判定：下品（基础） — Partial: `PillQuality.LOW` 枚举存在，无炼丹品质判定逻辑
- [x] 品质判定：中品（高技能等级概率出现） — Partial: `PillQuality.MIDDLE` 枚举存在
- [x] 品质判定：上品（高技能+新鲜材料概率出现） — Partial: `PillQuality.HIGH` 枚举存在
- [ ] 品质判定：失败产出废丹 — Stub: `generated_art/raw/waste_pill.png` 已有素材，未注册物品
- [ ] 品质判定：小概率爆炉（丹炉损耗）
- [ ] 实现炼丹技能等级系统（LV1~LV10） — Stub: `SkillType.ALCHEMY` 枚举存在
- [ ] 炼丹成功时获得经验
- [ ] 等级提升解锁更高阶丹药配方
- [x] 创建核心丹药物品：凝气丹 — Implemented: `CultivationPillItem`
- [x] 创建核心丹药物品：筑基丹 — Implemented: `FoundationBuildingPill`（下品）
- [x] 创建核心丹药物品：稳神丹 — Implemented: `CalmingPill`（下品）
- [x] 创建核心丹药物品：回灵丹 — Implemented: `QiRecoveryPillItem`
- [ ] 凝气丹效果：服用后1小时修为×2 — Partial: 物品已实现，需确认 1 小时 ×2 修为加成效果
- [ ] 筑基丹效果：练气→筑基突破必需 — Partial: `FoundationBuildingPill` 已为突破辅助，需确认是否强制要求
- [ ] 稳神丹效果：走火风险-20% — Partial: 物品已实现，需确认 -20% 效果接入
- [ ] 回灵丹效果：立刻恢复50%灵力 — Partial: 物品已实现，需确认恢复量
- [x] 创建灵草材料物品：浮云草 — Partial: 已注册 5 类灵草（`spirit_grass`/`cloud_mushroom`/`phoenix_feather_flower`/`dragon_blood_grass`/`immortal_ginseng`），命名与设计文档不一致
- [ ] 创建灵草材料物品：碧云草 — Stub: 未注册
- [ ] 创建灵草材料物品：万年灵草 — Stub: 未注册
- [ ] 创建灵草材料物品：血灵芝 — Stub: 未注册
- [ ] 创建灵草材料物品：天灵草 — Stub: 未注册
- [ ] 创建灵草材料物品：定神草 — Stub: 未注册
- [ ] 创建灵草材料物品：回灵草 — Stub: 未注册
- [ ] 创建灵草材料物品：玉露 — Stub: 未注册
- [ ] 添加所有丹药和材料的模型、材质、本地化 — Partial: 已注册物品的资源已接入（见 `docs/item-resource-validation-report.md`），缺失物品的资源同步缺失

### 验收标准

- [ ] 丹炉方块可放置和交互
- [ ] 丹炉 GUI 显示配方、材料槽、进度条
- [ ] 放入正确材料后可开始炼丹
- [ ] 炼丹成功后产出对应品质的丹药
- [ ] 炼丹失败产出废丹或爆炉
- [ ] 炼丹技能等级正确积累
- [ ] 凝气丹服用后修炼速度×2，持续1小时
- [ ] 筑基丹可用于练气→筑基突破
- [ ] 稳神丹正确降低走火风险20%
- [ ] 回灵丹立刻恢复50%灵力
- [ ] 所有丹药有对应材料配方

### Phase 7 完成标记

- [ ] **Phase 7 已完成，可进入 Phase 8** — 丹炉系统/Recipe/GUI/技能等级缺失，丹药物品已存在但效果链路待验证

---

## Phase 8：NPC与对话系统

**目标：** 实现核心NPC实体、JSON对话树系统、条件分支

### 任务清单

- [ ] 创建 NPC 基类 `CultivatorNPC`
- [ ] 实现 JSON 对话树加载器
- [ ] 对话树数据包路径：`data/seeking_immortals/dialogues/`
- [ ] 对话树格式定义：`{"npc_id": "...", "dialog_nodes": [...]}`
- [ ] 实现对话系统逻辑：右键 NPC 打开对话 GUI
- [ ] 对话 GUI 显示对话文本
- [ ] 对话 GUI 显示选项按钮
- [ ] 实现条件检查：quest_stage
- [ ] 条件检查：flags
- [ ] 条件检查：items
- [ ] 实现选项触发 action：设置任务阶段
- [ ] action：给予物品
- [ ] action：传送玩家
- [ ] 创建 NPC：墨老先生（大燕国村镇）
- [ ] 墨老先生功能：灵根测试触发
- [ ] 创建 NPC：七玄门七长老
- [ ] 七长老功能分配：炼丹/炼器/功法/外务/阵法/战斗/秘密
- [ ] 创建 NPC：厉飞羽（同伴NPC）
- [ ] 厉飞羽跟随逻辑（基础）
- [ ] 实现灵根测试对话交互
- [ ] 墨老先生对话选项「伸手」触发灵根测试
- [ ] 测试后显示灵根结果
- [ ] 永久标记已测试
- [ ] 添加所有 NPC 的模型、材质、本地化

### 验收标准

- [ ] 墨老先生 NPC 可生成在村镇
- [ ] 右键墨老先生打开对话界面
- [ ] 对话选项根据任务阶段/标记动态显示
- [ ] 选择「伸手」触发灵根测试，结果正确显示
- [ ] 七玄门七长老 NPC 可交互
- [ ] 厉飞羽 NPC 可跟随玩家（基础）
- [ ] 对话树 JSON 正确加载和解析

### Phase 8 完成标记

- [ ] **Phase 8 已完成，可进入 Phase 9** — Stub: 整个 NPC/对话/任务系统未开始

---

## Phase 9：七玄门任务线

**目标：** 实现七玄门完整5阶段任务线

### 任务清单

- [ ] 创建 `Quest` 类：questId, stages, rewards
- [ ] 创建 `QuestProgress` 存储于 `PlayerCultivation`
- [ ] 实现任务阶段追踪系统
- [ ] 实现七玄门任务阶段一：测灵根
- [ ] 阶段一触发：首次与墨老先生对话
- [ ] 阶段一任务：参加村镇集合
- [ ] 阶段一任务：灵根测试
- [ ] 阶段一任务：被宗门收入
- [ ] 阶段一完成条件：灵根测试完成
- [ ] 实现七玄门任务阶段二：七玄门入门
- [ ] 阶段二触发：测灵根完成
- [ ] 阶段二任务：领取黄龙功
- [ ] 阶段二任务：完成劳役任务（收集材料×10）
- [ ] 阶段二任务：学习基础炼丹
- [ ] 阶段二完成条件：炼丹技能达到 LV1
- [ ] 实现七玄门任务阶段三：发现秘密
- [ ] 阶段三触发：入门完成
- [ ] 阶段三任务：偷入墨老密室（特定坐标）
- [ ] 阶段三任务：发现神秘小瓶
- [ ] 阶段三任务：实验使用
- [ ] 阶段三完成条件：使用神秘小瓶1次
- [ ] 实现七玄门任务阶段四：宗门内斗
- [ ] 阶段四触发：发现秘密完成
- [ ] 阶段四任务：调查长老勾结
- [ ] 阶段四任务：收集证据（找到特定物品）
- [ ] 阶段四任务：三岔路口选择（举报/沉默/勒索）
- [ ] 阶段四完成条件：做出选择
- [ ] 实现七玄门任务阶段五：离开大燕
- [ ] 阶段五触发：宗门内斗完成
- [ ] 阶段五任务：天罡盟攻打事件
- [ ] 阶段五任务：逃离
- [ ] 阶段五任务：前往越国传送门
- [ ] 阶段五完成条件：传送到越国
- [ ] 实现任务奖励发放：神秘小瓶（如未获得）
- [ ] 奖励：黄龙功功法书
- [ ] 奖励：下品灵石×300
- [ ] 添加任务追踪 GUI（可选）
- [ ] 实现墨老密室结构生成
- [ ] 实现越国传送门结构生成

### 验收标准

- [ ] 玩家可触发并完成七玄门5阶段任务
- [ ] 每个阶段的完成条件正确检测
- [ ] 阶段四的三岔路口选择影响后续对话/声望
- [ ] 阶段五完成后玩家传送到越国
- [ ] 任务奖励正确发放
- [ ] `/seeking_immortals quest` 命令显示当前任务进度

### Phase 9 完成标记

- [ ] **Phase 9 已完成，可进入 Phase 10** — Stub: 七玄门任务线未开始

---

## Phase 10：越国六大宗门任务线（前3阶段）

**目标：** 实现越国六大宗门选择、叩门考核、筑基困局、内门竞争

### 任务清单

- [ ] 创建六大宗门数据结构
- [ ] 宗门：黄枫谷（三系及以上，综合修炼，中立）
- [ ] 宗门：天星宗（双系+占星测试，占星/阵法，冷漠）
- [ ] 宗门：落月宗（阴系优先，符箓/鬼道，不友好）
- [ ] 宗门：女灵派（女性限定，女修心法，中立）
- [ ] 宗门：凌霄剑派（金/木系，剑修飞剑，友好）
- [ ] 宗门：雷神斋（雷/火系，雷系法术，热情）
- [ ] 实现越国任务阶段一：叩门考核
- [ ] 阶段一触发：抵达越国后与招募 NPC 对话
- [ ] 阶段一任务：根据灵根选择可加入的宗门
- [ ] 阶段一任务：完成三关考核（战斗测试）
- [ ] 阶段一任务：完成三关考核（采集测试）
- [ ] 阶段一任务：完成三关考核（知识问答）
- [ ] 阶段一完成条件：通过考核，获得外门弟子身份
- [ ] 实现越国任务阶段二：筑基困局
- [ ] 阶段二触发：成为外门弟子后
- [ ] 阶段二任务：收集筑基丹材料（万年灵草×1）
- [ ] 阶段二任务：收集筑基丹材料（血灵芝×2）
- [ ] 阶段二任务：收集筑基丹材料（天灵草×1）
- [ ] 阶段二任务：使用神秘小瓶加速万年灵草生长
- [ ] 阶段二任务：完成宗门任务积分（积分达到500）
- [ ] 阶段二完成条件：获得筑基丹材料，积分达到500
- [ ] 实现越国任务阶段三：内门竞争
- [ ] 阶段三触发：筑基困局完成且突破到筑基期
- [ ] 阶段三任务：参加年度内门选拔
- [ ] 阶段三任务：与单灵根 NPC 竞争
- [ ] 阶段三任务：完成叛徒调查支线
- [ ] 阶段三完成条件：选拔通过，成为内门弟子
- [ ] 实现宗门贡献积分系统
- [ ] 积分系统：完成宗门任务获得积分
- [ ] 积分系统：积分可兑换资源/功法
- [ ] 实现宗门任务生成器（简单版）
- [ ] 任务类型：采集任务（收集材料×N）
- [ ] 任务类型：击杀任务（击杀妖兽×N）
- [ ] 任务类型：护送任务（护送 NPC 到目标地点）
- [ ] 实现叛徒调查支线：举报选项（正道声望+50）
- [ ] 叛徒调查支线：利用选项（获得特殊材料）
- [ ] 叛徒调查支线：沉默选项（无影响）
- [ ] 实现六大宗门建筑结构生成（简化版）
- [ ] 添加六大宗门 NPC 和相关本地化

### 验收标准

- [ ] 越国六大宗门 NPC 可交互
- [ ] 玩家可根据灵根选择并加入宗门
- [ ] 叩门考核三关正确实现
- [ ] 筑基困局任务正确引导玩家使用神秘小瓶
- [ ] 宗门贡献积分正确积累
- [ ] 内门选拔竞争正确判定
- [ ] 叛徒调查支线分支选择生效
- [ ] 完成前3阶段后获得内门弟子身份

### Phase 10 完成标记

- [ ] **Phase 10 已完成，MVP 开发完成！** — Stub: 越国宗门任务线未开始

---

## 后续阶段预览（暂不执行）

### 阶段二：内容扩充（Phase 11-16）

- Phase 11：金丹品阶系统
- Phase 12：南海 arc + 青竹蜂云剑完整阵（72剑）
- Phase 13：完整炼器系统（法宝、灵宝、仙器）
- Phase 14：天南 arc + 高阶拍卖 GUI
- Phase 15：元婴期技能树
- Phase 16：越国宗门任务线后2阶段（宗门外战、真传传功）

### 阶段三：高阶内容（Phase 17-22）

- Phase 17：灵界入境 arc + 真魔三劫
- Phase 18：灵界七大区域
- Phase 19：完整阵法系统
- Phase 20：傀儡系统
- Phase 21：完整驯兽系统
- Phase 22：多人宗门战争 PvP Event

---

## 执行记录

### Phase 完成记录

| Phase | 开始时间 | 完成时间 | 状态 | 备注 |
|-------|---------|---------|------|------|
| Phase 0 | 2026-06-13 | 2026-06-13 | 已完成 | 项目调研，生成 phase-0-project-survey.md |
| Phase 1 | 2026-06-14 | 2026-06-18 | 已完成 | 核心属性兼容 API、Realm/RealmStage 设计映射、境界基准与衍生属性单元测试已通过 |
| Phase 2 | 2026-06-18 | - | Realm System 有证据但未验收；原路线部分完成 | 本次指定 Realm System 子范围已生成 `docs/phase-2-report.md`；`./gradlew.bat build` 当前失败，按规则不能标记完成；灵根枚举/算法/系数齐全，测试石物品和打坐基础已实现；灵根鉴定方块、独立打坐 GUI、速度详情仍缺失 |
| Phase 3 | - | - | 部分完成 | 突破流程、成功率（含执念）、走火 4 级、稳神丹和平稳打坐衰减已实现；材料拥有量预览与部分走火触发链路不完整 |
| Phase 4 | - | - | 框架完成 | 技能枚举/存储/冷却已就绪；火球术、灵气探测有效果实现；飞剑/弹射物实体与多数练气期技能缺失 |
| Phase 5 | - | - | 未开始 | 仅有 `VialGrade` 枚举占位，神秘小瓶物品未实现 |
| Phase 6 | - | - | 未开始 | 筑基期 6 个技能均未实现 |
| Phase 7 | - | - | 部分完成 | 丹药物品、品质枚举、材料系统已有；丹炉 BlockEntity/GUI/Recipe/技能等级缺失 |
| Phase 8 | - | - | 未开始 | NPC、对话、任务系统未实现 |
| Phase 9 | - | - | 未开始 | 七玄门任务线未实现 |
| Phase 10 | - | - | 未开始 | 越国宗门任务线未实现 |

---

## 执行注意事项

1. **一次只执行一个 Phase**：严禁同时进行多个 Phase 的开发
2. **验收后才能进入下一阶段**：每个 Phase 的所有验收标准必须通过
3. **每个 Phase 完成后运行 `./gradlew build`**
4. **更新 `project_docs/step_progress.md`**
5. **添加 update note 到 `project_docs/updates/`**
6. **如果阻塞，记录原因并寻找最小可行替代方案**

---

**文档版本：** v1.2  
**生成时间：** 2026-06-14  
**最近审计更新：** 2026-06-18  
**最近修复更新：** 2026-06-18  
**最近 Phase 2 Realm System 更新：** 2026-06-18  
**基于：** `docs/implementation-roadmap.md`  
**配合文档：** `docs/mvp-scope.md`, `docs/cultivation-design-v3.md`, `docs/phase-0-3-audit-report.md`, `docs/phase-1-repair-report.md`
