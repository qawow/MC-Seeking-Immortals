# Phase 0-6 Audit Report

> Audit date: 2026-06-19
> Scope: Phase 0 to Phase 6 (按用户最新口径：Phase 5=炼丹，Phase 6=神秘小瓶)
> Constraints: no Java changes, no feature implementation, no Phase 7+ work

## Numbering Note

`docs/task-board.md` 历史编号为 Phase 5=神秘小瓶、Phase 7=炼丹。本审计遵循用户最新指令：炼丹视为 Phase 5（已完成），神秘小瓶视为 Phase 6（未实现）。任务板原 Phase 6“筑基期技能”顺延为后续阶段，不在本次 Phase 0~6 审计完成口径内。

## Audit Method

- 审阅 `docs/task-board.md`、`docs/mvp-scope.md`、`docs/implementation-roadmap.md`、`project_docs/ai_handoff.md`、`project_docs/step_progress.md`。
- 核对 `src/main/java` 与 `src/main/resources` 的当前实现证据。
- 不把 `build/`、`.bak/`、`generated_art/` 等生成/历史目录当作实现真相。
- 以当前 `gradlew.bat build` 结果作为 [x] 判定门槛。

## Build Result

- Command: `.\gradlew.bat --no-daemon --max-workers=1 build`
- Result: BUILD SUCCESSFUL in 28s（`compileJava`/`test`/`build` 均通过，`compileTestJava UP-TO-DATE`）。
- Notes: 历史报告显示 Phase 1/3/4/5 构建均成功；Phase 2 报告当时构建失败，但后续 Phase 3 修复已使整体构建恢复成功。

## Overall Conclusion

- Phase 0: Complete（架构审查报告存在）。
- Phase 1: Complete（核心属性/境界/基准表/单元测试，构建成功）。
- Phase 2: Partial。Realm System 代码证据齐全且当前构建成功，可标 [x]；但灵根鉴定方块、独立打坐 GUI、速度详情仍缺失（Partial）。
- Phase 3: Complete（突破预览、走火检定、四级走火效果、稳神丹、风险衰减，构建成功）。
- Phase 4: Complete（9 个练气期技能、自动解锁、灵力/冷却、MVP 飞剑，构建成功）。
- Phase 5（炼丹）: Complete（丹炉/BlockEntity/MVP丹方/废丹/爆炉/丹药效果，构建成功）；JSON 丹方目录、完整 GUI、炼丹等级系统为后续扩展（Missing）。
- Phase 6（神秘小瓶）: Not started。仅有 `VialGrade` 枚举与 `getJadeVialDropChance()` 接口占位，无物品/灵液/植物加速实现。
- Current recommended execution phase: Phase 6（神秘小瓶），为最早未完成 Phase。

## Phase 0: Pre-Implementation Architecture Review

Status: Done

Evidence:
- `docs/phase-0-project-survey.md` 存在，覆盖修炼状态、境界/阶段、灵根、Capability、技能/功法、丹药、缺口与待新增/重构模块。
- Phase 0 任务为文档/审计类，均有报告支撑。

Conclusion: Phase 0 全部 `[x]`。

## Phase 1: Core Attributes and Realm System

Status: Done

Evidence:
- `PlayerCultivation` 提供六大核心属性设计语义 getter/setter（cultivation/mana/manaMax/divSense/bodyRef/qiDevRisk/tribRes），保留旧内部字段与 NBT 兼容。
- `RealmStage` 含 `LAYER_1`~`LAYER_13` 与 `EARLY/MIDDLE/LATE/PEAK`，`getDesignId()` 映射设计名。
- `RealmStageConfig` 提供 manaBase/divSenseBase/hpBase/manaRecovery/cultivationGain/flyingSpeed 基准。
- 衍生属性方法齐全；`Phase1CultivationSystemTest` 覆盖基准表与衍生属性，`gradlew test` 通过。
- `phase-1-repair-report.md` 记录 `gradlew build` 成功。

Conclusion: Phase 1 全部 `[x]`。

## Phase 2: Realm System / Spiritual Root & Meditation

Status: Partial

Evidence (可标 [x]):
- `RealmStage` enum / 境界配置、凡人/练气1~13/筑基初、`cultivationMax`/`manaMax`/`divSense`/`hpBase`、修为增加/灵力恢复/突破成功/突破失败/修为回退20%/`qiDevRisk` 增加均有代码证据，且当前构建成功，可标 [x]。

Evidence (缺失，Partial/Missing):
- 独立 `MeditationScreen` 打坐 GUI 未实现（Missing）。
- 打坐速度详情 GUI 未完整（Partial）。
- 灵根鉴定为测试石物品交互，非独立方块流程（Partial）。

Conclusion: Realm System 子项标 [x]；打坐 GUI/灵根鉴定流程保持 [ ] Partial/Missing。

## Phase 3: Breakthrough & Qi Deviation

Status: Done

Evidence:
- `BreakthroughService` 在突破前显示所需材料与拥有量、成功率与全部加成来源。
- 打坐受伤与超阶功法均在增加风险后触发统一四级走火检定；四级效果保留。
- 稳神丹 -20% 走火风险；平稳/灵脉打坐风险衰减保留。
- `phase-3-repair-report.md` 记录 `gradlew build` 成功。

Conclusion: Phase 3 全部 `[x]`。

## Phase 4: Qi Refining Skill System

Status: Done

Evidence:
- `SkillType` 补齐 Phase 4 元数据；`PlayerCultivation.unlockEligiblePhase4Skills()` + `ModEvents` 自动解锁并同步。
- 9 个练气期技能注册到 `SkillEffectRegistry`；`ReleaseTechniquePacket` 复用灵力/冷却服务端校验。
- `SwordProjectileEntity` 提供最小飞剑弹射物；御剑飞行初用 MVP 基础飞行。
- `phase-4-report.md` 记录 `BUILD SUCCESSFUL in 37s`。

Conclusion: Phase 4 全部 `[x]`。

## Phase 5: Alchemy & Pills

Status: Done（含已知扩展缺口）

Evidence:
- `AlchemyFurnaceBlock` + `AlchemyFurnaceBlockEntity` 实现右键炼丹、材料/灵力消耗、cookTicks 计时、成功率/爆炉、产物持久化。
- `AlchemyRecipe` 静态 MVP 丹方覆盖凝气丹/筑基丹/稳神丹/回灵丹；`WASTE_PILL` 注册。
- 丹药效果接入 `PlayerCultivation`：凝气丹 1 小时 ×2 修炼增益、回灵丹 ≥50% 最大灵力、稳神丹 -20 走火风险、筑基丹用于练气13→筑基突破资源。
- `phase-5-report.md` 记录 `BUILD SUCCESSFUL in 36s`。

Gaps (Missing，不影响 Phase 5 验收):
- JSON 丹方目录 `data/seeking_immortals/recipes/alchemy/` 未建。
- 完整丹炉 GUI 未实现（用右键+聊天进度）。
- 炼丹技能等级 LV1~LV10 与炼丹经验未实现。

Conclusion: Phase 5 核心项 `[x]`；JSON 目录/完整 GUI/等级系统 `[ ] Missing`。

## Phase 6: Mysterious Vial

Status: Not started

Evidence:
- 仅有 `cultivation.VialGrade` 枚举占位与 `SpiritualRoot.getJadeVialDropChance()` / `PlayerCultivation.getJadeVialDropChance()` 接口，`LingGenTestStoneItem` 显示获取率 tooltip。
- 无 `MysteriousVialItem`、无 `vialCharges`/`vialLastRefill` NBT、无灵液积累/离线补偿、无植物加速、无 BlockGrow 监听、无物品模型/材质/本地化。

Conclusion: Phase 6 全部 `[ ]`（Stub/Missing），当前执行阶段设为 Phase 6。

## Current Recommended Phase

Phase 6：神秘小瓶系统（最早未完成 Phase）。

