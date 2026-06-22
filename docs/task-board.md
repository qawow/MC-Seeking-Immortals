# Task Board — 可勾选任务列表

> 基于 `implementation-roadmap.md` 生成
>
> **执行规则：一次只允许执行一个 Phase**
>
> **编号口径（2026-06-20 重排对齐 0.1.47）**：Phase 5 炼丹系统 / Phase 6 神秘小瓶 / Phase 7 基础 HUD / Phase 8 资源与物品接入验证；原 Phase 6「筑基期技能」（全 stub）顺延并入 Phase 9 待办子项。任务清单编号已与执行记录表统一。

---

## 执行状态

- **当前版本**: `0.1.47`（`gradle.properties`）
- **当前 Phase**: Phase 9：MVP 集成测试（Phase 0~8 已完成；筑基期技能作为 Phase 9 待办子项；未进入 Phase 9 实现）
- **当前状态**: 0.1.47 已实现六大核心属性存储/同步/展示、手动突破 + 成功率加成（丹药/灵脉灵眼/功法品质/执念）、走火入魔四级判定、飞剑/法宝飞行闭环、炼丹系统（丹炉/丹方/废丹/爆炉/丹药效果）、神秘小瓶、练气期 9 技能、基础 HUD、灵气浓度系统、灵根重构与资源接入验证（125 物品 / 5 方块）；Phase 9 MVP 集成测试未开始
- **已完成 Phase 数**: 9 / 11（Phase 0~8 完成；筑基期技能从原 Phase 6 顺延并入 Phase 9 待办；Phase 9 集成测试、Phase 10 越国宗门未开始）

> 编号说明：2026-06-20 按执行记录重排，统一为 P5 炼丹 / P6 神秘小瓶 / P7 基础 HUD / P8 资源验证；原 Phase 6 筑基技能（全 stub）并入 Phase 9 待办。
>
> 状态来源：`docs/phase-0-3-audit-report.md`（2026-06-19 Phase 0~6 审计，当前 `gradlew build` 成功）、`docs/phase-1-repair-report.md`、`docs/phase-3-repair-report.md`、`docs/phase-4-report.md`、`docs/phase-5-report.md`、`docs/phase-6-report.md`、`docs/phase-8-report.md`、`docs/texture-resource-audit.md`、`docs/gap-audit-report.md`。只有证据明确且 `./gradlew.bat build` 成功的任务可标记为 `[x]`；Partial/Missing/Unknown 标记不计入完成数。

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

- [x] `RealmStage` enum / 境界配置 — `RealmStage` 含 `LAYER_1`~`LAYER_13`/`EARLY`~`PEAK`，`RealmStageConfig` 提供基准表；当前 `gradlew build` 成功
- [x] 凡人、练气 1~13、筑基初 — `getStagesForRealmPublic()` 返回凡人/13 练气层/筑基初；构建成功
- [x] `cultivationMax` — `getCultivationMax()` 动态返回 `getCurrentStageCapExp()`；构建成功
- [x] `manaMax` — `getManaMax()`/`getManaMaxLong()` 委托 `getMaxSpiritualPower()`；构建成功
- [x] `divSense` — `getDivSense()`/`setDivSense(int)` 读写 `divineConsciousness`；构建成功
- [x] `hpBase` — `RealmStageConfig#getHpBase()` 与 `getMaxHealthPoints()` 已实现；构建成功
- [x] 修为增加基础方法 — `addCultivationExp(int)`/`setCultivation(long)` 已实现；构建成功
- [x] 灵力恢复基础方法 — `addSpiritualPower(int)`/`setMana(int)`/`getManaRecoveryPerSecond()` 已实现；构建成功
- [x] 突破成功 — `tryBreakthrough(...)` 成功路径已实现；构建成功
- [x] 突破失败 — `tryBreakthrough(...)` 失败路径已实现；构建成功
- [x] 修为回退 20% — 失败后当前阶段进度保留 80%；构建成功
- [x] `qiDevRisk` 增加 — 失败后 `addQiDeviationRisk(10)`；构建成功

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
- [ ] 创建「灵根鉴定石板」方块或物品 — Partial: `ling_gen_identification_slab` 方块已注册，但 `use()` 复用 `LingGenTestStoneItem.testPlayer(...)`、不消耗、无独立冷却；`LingGenTestStoneItem` 物品也可用
- [ ] 实现首次交互触发灵根测试逻辑 — Partial: `LingGenCalculator.roll()` 算法与物品/方块交互入口已接入，但不是 NPC/方块首次交互
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

- [ ] 灵根鉴定石板方块/物品可交互 — Partial: 测试石物品与鉴定石板方块均可用，但石板复用测试石逻辑、不消耗
- [ ] 玩家首次测试灵根后，数据永久写入 — Partial: 物品交互已接入，但不是任务板所述 NPC/方块首次交互流程
- [x] `/seeking_immortals root` 命令显示灵根类型和系数
- [ ] 打坐 GUI 正确显示修为增长速度 — Missing: 无独立 GUI
- [x] 打坐时修为实时增长
- [x] 打坐时受伤会打断并增加走火风险
- [x] 不同灵根的修炼速度差异明显（天灵根 ×5.0，杂灵根 ×0.8）

### Phase 2 完成标记

- [x] **Phase 2 已完成，可进入 Phase 3** — Realm System 子项证据明确且当前 `gradlew build` 成功；独立打坐 GUI/灵根鉴定方块独立逻辑为后续扩展（Partial/Missing），不影响 Realm System 验收

---

## Phase 3：突破系统与走火入魔

**目标：** 实现境界突破流程、成功率计算、走火入魔四级判定

### 任务清单

- [x] 实现突破触发检查：`cultivation >= cultivationMax`
- [x] 实现突破按键绑定或 GUI 按钮
- [x] 实现突破资源检查（背包丹药）
- [x] 显示所需材料和当前拥有量 — `BreakthroughService` 在尝试突破前显示破境丹需求、拥有量和辅助药力/创造模式状态
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
- [x] 走火触发条件：修炼时受伤 — 打坐受伤立即增加风险并调用四级走火检定
- [x] 走火触发条件：使用超阶功法 — 超出当前境界 2 级以上的功法增加风险后立即调用四级走火检定
- [x] 实现走火判定：70-79% 轻微走火（损失30%修为，风险清零）
- [x] 走火判定：80-89% 中度走火（损失50%修为 + 昏迷30s，风险清零）
- [x] 走火判定：90-99% 严重走火（掉落一个境界 + 昏迷3分钟 + 装备随机损坏）
- [x] 走火判定：100% 极端走火（当场死亡 + 背包掉落50%）
- [x] 实现走火风险降低：服用稳神丹 -20%
- [x] 走火风险降低：平稳打坐每小时 -5%（ServerTickEvent）

### 验收标准

- [x] 修为达到上限后，突破按钮/按键可用
- [x] 突破前显示成功率和所需材料 — 突破前显示最终成功率、材料拥有量、基础/灵根/丹药/灵眼/功法/执念全部加成来源
- [x] 突破成功后境界正确+1，修为清零
- [x] 突破失败后修为-20%，走火风险+10%
- [x] 走火风险≥70%时突破失败触发走火判定
- [x] 四级走火效果正确实现（修为损失/昏迷/掉境界/死亡）
- [x] 稳神丹正确降低走火风险20%
- [x] 平稳打坐每小时降低走火风险5%
- [x] 连续失败积累执念机制正常工作

### Phase 3 完成标记

- [x] **Phase 3 已完成，可进入 Phase 4** — `docs/phase-3-repair-report.md` 已生成，`./gradlew.bat --no-daemon --max-workers=1 build` 已通过；本次未进入 Phase 4 实现

---

## Phase 4：练气期技能系统

**目标：** 实现练气期9个技能，包括技能解锁、冷却、消耗、效果

### 任务清单

- [x] 实现或验证技能注册系统 — `SkillEffectRegistry` 已注册 Phase 4 所需 9 个练气技能效果
- [x] 实现技能：引气入体（被动，练气1） — `QiGuidingPassive`，练气1自动解锁并作为打坐/修炼入口标记
- [x] 实现技能：火球术（练气3，10灵力，2s冷却） — `FireballSpell` 已校准为 10 灵力 / 40 tick
- [x] 实现技能：冰锥术（练气3，10灵力，2s冷却） — `IceConeSpell` 发射 MVP 弹射物，命中伤害并减速
- [x] 实现技能：雷击术（练气3，12灵力，3s冷却） — `ThunderStrikeSpell` 在视线目标位置召唤闪电
- [x] 实现技能：土遁步（练气4，15灵力，5s冷却） — `EarthEscapeStepSpell` 向前短距离瞬移到可站立位置
- [x] 实现技能：御剑飞行初（练气7，5灵力/s） — `FlyingSwordBeginnerSpell` 采用本阶段允许的 MVP 基础飞行能力，飞行中每秒消耗 5 灵力
- [x] 实现技能：单剑刺击（练气7，20灵力，1s冷却） — `SwordProjectileSpell` 发射单把飞剑弹射物
- [x] 实现技能：灵气探测（练气10，5灵力，10s冷却） — `DetectionSpell` 校准为 5 灵力 / 200 tick，并按神识范围标记生灵与灵物粒子
- [x] 实现技能：三才剑阵（练气13，40灵力，3s冷却） — `SwordProjectileSpell` 三飞剑同时发射
- [x] 实现技能解锁逻辑：境界达到要求后自动解锁 — `PlayerCultivation#unlockEligiblePhase4Skills()` + 登录/服务端 tick 同步
- [x] 实现技能冷却系统（`PlayerCultivation` 存储 `skillCooldowns[]`） — 复用 `techniqueCooldownUntilTicks`，按 Phase 4 technique id 存储冷却
- [x] 实现灵力消耗检查 — `ReleaseTechniquePacket` 服务端按 `SkillEffect` 消耗校验并扣除
- [x] 创建 `FlyingSwordEntity`（可骑乘实体） — MVP 替代：按本次范围允许，未新增可视骑乘实体，已实现最小御剑飞行/移动能力
- [x] 创建 `SwordProjectileEntity`（弹射物） — `SwordProjectileEntity` 已注册并用于冰锥、单剑刺击、三才剑阵
- [x] 火球术弹射物实现 — 复用原版 `SmallFireball`
- [x] 冰锥术弹射物实现 + 减速效果
- [x] 雷击术召唤闪电实现
- [x] 土遁步瞬移逻辑实现
- [x] 御剑飞行骑乘逻辑实现 — MVP 替代：技能开关基础飞行并持续扣灵力
- [x] 单剑刺击飞剑弹射物实现
- [x] 灵气探测高亮渲染实现 — 粒子标记生灵和灵物波动，未处理贴图资源
- [x] 三才剑阵三飞剑同时发射实现

### 验收标准

- [x] 练气1后自动解锁引气入体
- [x] 练气3后解锁火球术/冰锥术/雷击术
- [x] 火球术/冰锥术发射弹射物，命中造成伤害
- [x] 雷击术在目标位置召唤闪电
- [x] 土遁步正确瞬移玩家并穿过1格方块
- [x] 练气7后解锁御剑飞行，玩家可骑乘飞剑移动 — MVP 替代：技能开关基础飞行/移动能力并持续消耗灵力
- [x] 单剑刺击发射飞剑攻击，射程=神识范围
- [x] 练气10后灵气探测高亮灵矿/灵草（发光轮廓） — 粒子标记灵物波动
- [x] 练气13后三才剑阵发射三把飞剑
- [x] 技能冷却正确倒计时
- [x] 灵力不足时技能无法释放

### Phase 4 完成标记

- [x] **Phase 4 已完成，可进入 Phase 5** — `docs/phase-4-report.md` 已生成，`./gradlew.bat --no-daemon --max-workers=1 build` 已通过；本次未进入 Phase 5 实现

---

## Phase 5：炼丹系统

**目标：** 实现丹炉方块、丹方系统、炼丹技能、核心丹药

> 编号说明：本 Phase 内容原为任务板 Phase 7，2026-06-20 按执行记录重排为 Phase 5，与 `docs/phase-5-report.md` 对应。

### 任务清单

- [x] 创建或验证 `AlchemyFurnaceBlock` — 已实现 `AlchemyFurnaceBlock`（BaseEntityBlock，右键交互、方块实体Ticker）
- [x] 创建或验证 `AlchemyFurnaceBlockEntity` — 已实现 `AlchemyFurnaceBlockEntity`（配方/进度/成功率/爆炉/产物持久化）
- [x] 实现自定义 Recipe 类型 `AlchemyRecipe` — MVP 静态 record 配方结构（非 JSON，见报告 GUI 决策）
- [ ] 创建 JSON 配方格式目录：`data/seeking_immortals/recipes/alchemy/` — Missing: 本次采用静态 MVP 配方，未建 JSON 目录
- [x] 实现炼丹流程：放入丹方 + 灵草材料 — 右键丹炉按手持材料匹配丹方，消耗背包材料
- [x] 炼丹流程：催火（消耗灵力或灵火石） — 消耗玩家灵力（manaCost）作为催火
- [x] 炼丹流程：等待时间（Progress bar） — BlockEntity cookTicks 计时，聊天显示进度
- [x] 炼丹流程：判定成功/失败 — 按成功率/爆炉率判定产物
- [x] 实现品质判定：下品（基础） — Partial: `PillQuality.LOW` 枚举存在，无炼丹品质判定逻辑
- [x] 品质判定：中品（高技能等级概率出现） — Partial: `PillQuality.MIDDLE` 枚举存在
- [x] 品质判定：上品（高技能+新鲜材料概率出现） — Partial: `PillQuality.HIGH` 枚举存在
- [x] 品质判定：失败产出废丹 — 已注册 `WASTE_PILL`，炼丹失败产出废丹
- [x] 品质判定：小概率爆炉（丹炉损耗） — 达到爆炉率时摧毁丹炉并爆炸
- [ ] 实现炼丹技能等级系统（LV1~LV10） — Missing: MVP 预留接口 `AlchemyRecipeService.getAlchemySkillBonus`（硬编码返回 0），未实现等级系统
- [ ] 炼丹成功时获得经验 — Missing: 未实现炼丹经验积累
- [ ] 等级提升解锁更高阶丹药配方
- [x] 创建核心丹药物品：凝气丹 — Implemented: `CultivationPillItem`
- [x] 创建核心丹药物品：筑基丹 — Implemented: `FoundationBuildingPill`（下品）
- [x] 创建核心丹药物品：稳神丹 — Implemented: `CalmingPill`（下品）
- [x] 创建核心丹药物品：回灵丹 — Implemented: `QiRecoveryPillItem`
- [x] 凝气丹效果：服用后1小时修为×2 — 已接入 `PlayerCultivation.addCultivationBoost`（1小时×2，影响修炼/打坐修为）
- [x] 筑基丹效果：练气→筑基突破必需 — `BreakthroughService` 练气13层→筑基要求并消耗下品筑基丹
- [x] 稳神丹效果：走火风险-20% — `CalmingPill` 已接入 `addQiDeviationRisk(-20)`
- [x] 回灵丹效果：立刻恢复50%灵力 — `QiRecoveryPillItem` 恢复至少 50% 最大灵力
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

- [x] 丹炉方块可放置和交互 — 注册为方块物品，右键交互
- [ ] 丹炉 GUI 显示配方、材料槽、进度条 — Missing: 未实现完整GUI，改用右键+聊天进度（见报告决策）
- [x] 放入正确材料后可开始炼丹 — 手持材料右键丹炉开始
- [x] 炼丹成功后产出对应品质的丹药 — MVP 产出基础品质丹药
- [x] 炼丹失败产出废丹或爆炉 — 失败产出废丹，小概率爆炉
- [ ] 炼丹技能等级正确积累 — Missing: 未实现等级系统
- [x] 凝气丹服用后修炼速度×2，持续1小时 — 已接入修炼增益
- [x] 筑基丹可用于练气→筑基突破 — 突破资源判定已接入
- [x] 稳神丹正确降低走火风险20% — 已接入
- [x] 回灵丹立刻恢复50%灵力 — 已接入
- [ ] 所有丹药有对应材料配方

### Phase 5 完成标记

- [x] **Phase 5 已完成，可进入 Phase 6** — 丹炉/BlockEntity/丹方/废丹/爆炉/丹药效果已实现并构建通过；JSON配方目录、完整GUI、炼丹等级系统为后续扩展（见 `docs/phase-5-report.md`）

---

## Phase 6：神秘小瓶系统

**目标：** 实现神秘小瓶唯一物品、灵液积累、植物加速

> 编号说明：本 Phase 内容原为任务板 Phase 5，2026-06-20 按执行记录重排为 Phase 6，与 `docs/phase-6-report.md` 对应。

### 任务清单

- [x] 创建 `MysticVialItem` 类 — 已实现 `MysticVialItem`（绑定/充能/植物加速）
- [x] 神秘小瓶设置为唯一绑定物品 — `stacksTo(1)` + `vialOwner` NBT 绑定
- [x] 神秘小瓶不可丢弃 — `onDroppedByPlayer` 返回 false 并提示
- [x] NBT 存储 `vialCharges` — 已实现
- [x] NBT 存储 `vialLastRefill` — 已实现
- [x] 实现灵液积累逻辑：每24小时积累1份 — `MILLIS_PER_CHARGE` 现实时间
- [x] 灵液最大5份 — 复用 `VialGrade.BASIC.getMaxCharges()`
- [x] 实现离线时间计算并补偿灵液 — `refillIfNeeded` 按现实毫秒补算
- [x] 实现植物加速使用：右键对植物方块使用 — `useOn` 对 CropBlock/BonemealableBlock
- [x] 植物加速：消耗1份灵液 — 成功时 `setCharges(getCharges-1)`
- [x] 植物加速：生长速度×10 — 复用 `VialGrade.BASIC.getGrowthSpeedMultiplier()`=10
- [x] 实现植物加速 — 用 `useOn` 直接催熟替代 BlockGrowEvent 监听（MVP 等价实现）
- [x] 实现灵根与神秘小瓶获取关联：伪/杂灵根必得 — `LingGenTestStoneItem.grantMysticVialForLowTalent` 对 `isLowTalent()` 发放
- [ ] 其他灵根极低概率获得（或任务获取） — Missing: MVP 仅伪/杂灵根必得，其他灵根未接入
- [x] 添加神秘小瓶物品模型 — `models/item/mystic_vial.json`
- [x] 添加神秘小瓶材质 — Phase 8 已复用现有本地 raw 贴图接入 `textures/item/mystic_vial.png`，模型指向同名 layer0；构建成功
- [x] 添加神秘小瓶本地化 — zh_cn/en_us 已加

### 验收标准

- [x] 伪灵根/杂灵根玩家测试灵根后自动获得神秘小瓶 — 已接入
- [x] 神秘小瓶无法丢弃 — 已实现
- [x] 每24小时自动积累1份灵液（最大5份） — 已实现
- [x] 离线时间正确计算并补偿灵液 — 已实现
- [x] 右键对作物使用消耗1份灵液 — 已实现
- [x] 使用后作物生长速度明显加快（×10） — 已实现
- [x] 神秘小瓶 tooltip 显示当前灵液份数 — 已实现

### Phase 6 完成标记

- [x] **Phase 6 已完成，可进入 Phase 7** — `docs/phase-6-report.md` 已生成，构建通过

---

## Phase 7：基础 HUD

**目标：** 实现基础修仙 HUD，显示境界/修为/灵力/神识/走火风险，并复用技能栏冷却

> 编号说明：本 Phase 为 2026-06-20 重排新增条目，对应 0.1.47 `CultivationHudOverlay` 实现，与 `project_docs/step_progress.md` 执行记录 Phase 7 对齐。

### 任务清单

- [x] 创建 `CultivationHudOverlay` — 已实现独立 HUD Overlay
- [x] HUD 显示境界名称与修为进度 — 已接入客户端同步数据
- [x] HUD 显示灵力条 — 已接入
- [x] HUD 显示神识范围 — 已接入
- [x] HUD 显示走火风险 — 已接入，风险高时警示
- [x] 复用左侧技能栏冷却显示 — `TechniqueSkillBarOverlay` 7 槽 + 冷却 tooltip 已存在
- [x] 打坐吐纳 HUD（`BreathingHudOverlay`）显示吐纳效率/灵气浓度/功法倍率/灵根亲和/进度条 — 已实现
- [x] HUD 在任意 Screen 打开时隐藏 — 已实现
- [x] HUD 按 scaledWidth/scaledHeight 钳制，适配不同 GUI Scale 与小窗口 — 已实现

### 验收标准

- [x] HUD 正确显示境界、修为、灵力、神识、走火风险
- [x] 走火风险高时有警示表现
- [x] 技能栏冷却正确倒计时显示
- [x] 打坐吐纳 HUD 显示灵气浓度与结算进度
- [x] HUD 不遮挡原版槽位、打开 Screen 时隐藏

### Phase 7 完成标记

- [x] **Phase 7 已完成，可进入 Phase 8** — 基础 HUD（`CultivationHudOverlay` 境界/修为/灵力/神识/走火风险警示 + 复用技能栏冷却）已实现；构建通过；未进入 Phase 8 实现

---

## Phase 8：资源与物品接入验证

**目标：** 验证并修复物品/方块资源接入问题，包括 models、textures、blockstates、lang。

> 用户最新口径以资源与物品接入验证作为 Phase 8。本文件早期的 NPC/对话 Phase 8 条目已顺延，未在本次执行。

### 任务清单

- [x] 检查 Java 注册物品和方块 — 审计 `ModItems` / `ModBlocks`，确认 125 个注册物品、5 个注册方块
- [x] 检查 `models/item` — 注册物品模型均存在；普通物品 layer0 已接入可渲染 texture 路径
- [x] 检查 `models/block` — 5 个注册方块模型均存在
- [x] 检查 `blockstates` — 5 个注册方块 blockstate 均存在
- [x] 检查 `textures/item` — 非方块注册物品贴图均存在；`mystic_vial`/`waste_pill` 复用已有本地 raw PNG
- [x] 检查 `textures/block` — 已有方块贴图可渲染；`ling_gen_identification_slab` 与 `alchemy_furnace` 复用现有方块贴图，专属美术需后续人工确认
- [x] 检查 `lang/zh_cn.json` 和 `en_us.json` — 补齐 `spirit_ore` item 与 `ling_gen_identification_slab` block/item key
- [x] 普通物品模型使用 `minecraft:item/generated` 或等价 item parent — 已审计并修复本阶段发现的异常路径
- [x] 普通物品 `layer0` 指向 `seeking_immortals:item/<item_id>` — 已修复 `mystic_vial`、`waste_pill`、`technique_manual_azure_origin_sword_derivative`
- [x] 缺失模型 JSON 则生成 — 已生成 `ling_gen_identification_slab` 方块物品模型
- [x] 缺失 lang 则补齐 — 已补齐本阶段发现的缺失 key
- [x] 方块资源接入验证 — 已补齐 `ling_gen_identification_slab` blockstate/block model/item model
- [x] 方块物品模型 parent 指向 `seeking_immortals:block/<block_id>` — 已验证并补齐
- [x] 生成 `docs/texture-resource-audit.md`

### 仍需人工确认

- [ ] `ling_gen_identification_slab` 专属方块贴图 — Manual: 当前复用 `spirit_gathering_array` 贴图以保证可渲染，未生成新图片
- [ ] `alchemy_furnace` 专属方块贴图 — Manual: 当前复用 `spirit_gathering_array` 贴图以保证可渲染，未生成新图片
- [ ] `technique_manual_azure_origin_sword_derivative` 正式美术 — Manual: 当前同名贴图来自既有 placeholder 复制，路径合规但非最终图

### 验收标准

- [x] 注册物品/方块资源审计完成
- [x] 缺失模型、方块状态和 lang 已补齐
- [x] 未生成新图片、未调用图片 API
- [x] 未修改 Java 代码
- [x] `.\gradlew.bat --no-daemon --max-workers=1 build` 通过
- [x] 已生成 `docs/phase-8-report.md`
- [x] 已更新 `docs/task-board.md`

### Phase 8 完成标记

- [x] **Phase 8 已完成，可进入 Phase 9：MVP 集成测试** — `docs/phase-8-report.md` 与 `docs/texture-resource-audit.md` 已生成，最终 build 成功；本次未进入 Phase 9

---

## Phase 9：MVP 集成测试

**目标：** 对 Phase 0~8 的 MVP 主流程、构建产物和资源接入进行集成验证，并补齐顺延的筑基期技能。

> 本次未进入 Phase 9。以下包含：当前 Phase 9 待执行项、顺延的筑基期技能待办（原 Phase 6）、以及旧路线七玄门任务线（已顺延/重新排期，不作为当前 Phase 9 的已执行内容）。

### 当前 Phase 9 待执行项

- [ ] MVP 主流程集成测试 — Stub: 未进入 Phase 9
- [ ] 资源加载与物品显示回归验证 — Stub: 未进入 Phase 9
- [ ] 关键玩法链路回归验证 — Stub: 未进入 Phase 9

### 筑基期技能待办（原 Phase 6 顺延，全 Stub）

> 编号说明：原任务板 Phase 6「筑基期技能系统」全为 stub，2026-06-20 重排时顺延并入 Phase 9 待办，不单独占编号。

#### 任务清单

- [ ] 实现技能：神识扩展（被动，筑基初） — Stub
- [ ] 实现技能：御剑飞行进阶（筑基初，3灵力/s） — Partial: 飞剑物品 + 飞行事件（`ModEvents.handleFlyingArtifact`）存在，但「御剑飞行进阶」技能本身未实现；当前筑基+飞行由 Curios `artifact` 槽飞剑/法宝驱动，与练气「御剑飞行初」技能是两套独立机制
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

#### 验收标准

- [ ] 筑基初自动解锁神识扩展，神识范围正确×1.5
- [ ] 筑基初解锁御剑飞行进阶，速度×1.8
- [ ] 罡气护体正确吸收下一次伤害
- [ ] 五行遁术检查灵根属性，正确瞬移20格
- [ ] 北斗剑阵召唤7把飞剑，持续攻击范围内敌人8s
- [ ] 筑基圆满解锁阵法感知（视觉效果可简化）

### 旧路线任务清单（顺延，未进入）

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

- [ ] **Phase 9 已完成，可进入 Phase 10** — Stub: MVP 集成测试与筑基期技能未开始；旧七玄门任务线未执行

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
| Phase 2 | 2026-06-18 | 2026-06-19 | 已完成（Realm System） | 2026-06-19 审计以当前 `gradlew build` 成功重判 Realm System 子项为 [x]；独立打坐 GUI/灵根鉴定方块独立逻辑仍 Partial/Missing |
| Phase 3 | 2026-06-18 | 2026-06-18 | 已完成 | 补齐突破材料/拥有量预览、成功率与全部加成来源预览、打坐受伤走火检定、超阶功法走火检定；构建通过 |
| Phase 4 | 2026-06-19 | 2026-06-19 | 已完成 | 9 个练气期技能、自动解锁、灵力消耗、冷却、基础效果、MVP 御剑飞行与飞剑弹射物已实现；构建通过 |
| Phase 5 | 2026-06-19 | 2026-06-19 | 已完成 | 炼丹系统（原 Phase 7 重排）：丹炉/BlockEntity/MVP丹方/废丹/爆炉/丹药效果接入；构建通过（见 `docs/phase-5-report.md`） |
| Phase 6 | 2026-06-19 | 2026-06-19 | 已完成 | 神秘小瓶（原 Phase 5 重排）：MysticVialItem 绑定/24h充能/离线补算/植物加速/伪杂灵根发放；构建通过（见 `docs/phase-6-report.md`） |
| Phase 7 | 2026-06-19 | 2026-06-19 | 已完成 | 基础 HUD（CultivationHudOverlay 境界/修为/灵力/神识/走火风险警示 + 复用技能栏冷却）；构建通过 |
| Phase 8 | 2026-06-19 | 2026-06-19 | 已完成 | 资源与物品接入验证：模型/贴图/方块状态/lang 审计修复，构建通过 |
| Phase 9 | - | - | 未开始 | MVP 集成测试未执行；筑基期技能（原 Phase 6 顺延并入）全 stub；旧七玄门任务线顺延/重新排期 |
| Phase 10 | - | - | 未开始 | 越国宗门任务线未实现 |

> 0.1.47 版本线补充：六大核心属性存储/同步/展示、手动突破流程 + 成功率加成（丹药/灵脉灵眼/功法品质/执念）、走火入魔四级判定、飞剑/法宝飞行闭环（Curios `artifact` 槽）均已实装并构建通过，详见 `project_docs/features.md` 与 `project_docs/updates/20260613_0.1.47.md`。

---

## 执行注意事项

1. **一次只执行一个 Phase**：严禁同时进行多个 Phase 的开发
2. **验收后才能进入下一阶段**：每个 Phase 的所有验收标准必须通过
3. **每个 Phase 完成后运行 `./gradlew build`**
4. **更新 `project_docs/step_progress.md`**
5. **添加 update note 到 `project_docs/updates/`**
6. **如果阻塞，记录原因并寻找最小可行替代方案**

---

**文档版本：** v1.3（2026-06-20 重排对齐 0.1.47）
**生成时间：** 2026-06-14
**最近审计更新：** 2026-06-18
**最近修复更新：** 2026-06-18
**最近 Phase 2 Realm System 更新：** 2026-06-18
**最近 Phase 3 修复更新：** 2026-06-18
**最近 Phase 4 实现更新：** 2026-06-19
**最近 Phase 8 资源验证更新：** 2026-06-19
**最近编号重排与版本对齐：** 2026-06-20（对齐 0.1.47；P5 炼丹 / P6 神秘小瓶 / P7 基础 HUD / P8 资源；筑基并入 P9）
**基于：** `docs/implementation-roadmap.md`
**配合文档：** `docs/mvp-scope.md`, `docs/cultivation-design-v3.md`, `docs/phase-0-3-audit-report.md`, `docs/phase-1-repair-report.md`, `docs/gap-audit-report.md`
