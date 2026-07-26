# 2026-07-26 0.2.112 时代缺口文档对账（docs-only）

## 变更分类

docs-only（仅文档，无代码/资源/数据包改动）：`mod_version=0.2.194` 不变，`ModNetwork.PROTOCOL_VERSION=30` 不变，按 CLAUDE.md 规则无需构建。

## 背景

仓库根目录存在两份 2026-07-21 生成的审查文档（`technique_gap_summary.md`、`功法系统缺口清单.md`），基于 0.2.112 / 协议 26 时代的代码与数据。0.2.113–0.2.193 期间 authored 视觉目录体系与功法数据大幅演进，两份文档的核心论断已过时，会误导后续开发。本批在两份文档文首各插入「2026-07-26 复核对账」节，逐项标注已解决/仍待办，正文保留为历史快照。

## 复核结论摘要

### technique_gap_summary.md（术法接线）

已作废的旧论断：

- "97.3%（727/747）术法无法被运行时消费"——现 `skill/effect/AbstractTechniqueEffectResolver.resolve()` 以 `AuthoredSpellEffectCatalog` 优先解析；`authored_spell_effects.json` 含 2,292 profile（747 语料全覆盖 + 1,545 原著扩展），语料 747/747 = 100% 可运行时执行。
- 别名通道实况：`SkillEffectRegistry` 614 处 `registerTechniqueAlias`、611 唯一 ID、覆盖语料 591/747（79.1%），远非旧文的 20 条；未别名 156 条（111 `_v129_*`、7 `_auto_*`、38 无类型）均经 authored 通道解析。
- 元素覆盖：`TechniqueVfxPalette.Family` 共 15 族（FIRE/WATER/METAL/WOOD/EARTH/WIND/ICE/THUNDER/LIGHT/DARK/SOUL/BLOOD/VOID/ILLUSION/NEUTRAL），旧文点名缺失的 EARTH/SOUL/BLOOD/VOID/NEUTRAL 均已覆盖；遗留 `SpellElement` 枚举也已扩到 13 项（含 EARTH）。

仍待办：YIN/YANG 无字面枚举成员（映射到近似族，表现精度问题）；CAST_* 符箓 effectKey 真实深度未逐一运行时验证；旧文 18 个召唤实体 ID 与 `ModEntities`（现 13 个注册实体）的映射未逐一核对。

### 功法系统缺口清单.md（功法数据）

已作废的旧论断（数据源 `text_material/cultivation_methods.json`，schema_version 19，136 功法）：

- "层数配置 17/136"——现 `setting.layers_max` 136/136 配置（82 个 >1 层、54 个 =1 层）；曾点名的 P0 功法现状：玄阴经 4 层、大衍诀 5 层、天符灵经 4 层、掩月周天功 3 层、烈焰功 1 层、长春功/青元剑诀 13 层。
- "14 个空功法矩阵"——现 matrices 136/136 非空。
- 旧文引用的 `TechniqueGateService` 类不存在，实际门槛类是 `cultivation/ProgressionGateApi.java`。

仍待办：`cultivation_costs` 0/136（数据与消费端均无）；`purity_min` 与 `same_sect` 在数据与 Java 中均不存在（`grep -rlE` 全库零命中）；转修网络无结构化边（仅长春功 `must_convert_after: "FOUNDATION"`，长春功→青元剑诀靠 prerequisite 字段表达）；多数功法层数深度偏浅（3–5 层），realm band 分段与每层成本/解锁细化留待 P1。

## 复核方法备注

- 复核脚本置于 `.bak/`（不入库）：`tmp_gap_recheck.py`、`tmp_gap_breakdown.py`、`tmp_authored_coverage.py`、`tmp_methods_deep_check.py`、`tmp_matrices_check.py`、`tmp_named_methods.py`。
- 两个踩坑记录：①功法主数据源是 `text_material/cultivation_methods.json` 而非 `data/seeking_immortals/cultivation/*.json`（后者是 477 条术法条目数据集），且 `layers_max` 嵌套在 `setting` 下需递归查找；②本机 grep（ugrep）对 `"a\|b"` 转义交替产生误报，须改用 `grep -rlE "a|b"`。
- 数据基线：本会话 0.2.193 构建绿（1m30s，1,162 测试 0 失败/错误/跳过）。

## 编辑文件与备份

- 编辑：`technique_gap_summary.md`、`功法系统缺口清单.md`（文首对账节）、`project_docs/step_progress.md`（第 588 步）、`project_docs/ai_handoff.md`（对账 handoff 段）、本备注。
- 备份：`.bak/20260726_gap_docs_reconcile/`（含四个被编辑存量文件的改前副本，保留相对路径）。

## 后续

- P1：核心战斗功法层数/境界带深化（玄阴经、大衍诀、烈焰功、天符灵经、掩月周天功优先）；`merit_mult_2`/`inverse_star_smuggle_chance`/`star_palace_patrol_bonus` 生产结算调用方；阴阳窟专属遭遇。
- P2：按功法配置 `cultivation_costs`；门槛深化（purity_min、same_sect + 叛宗惩罚、特殊体质专属、结构化转修网络）。
- 外部遗留（不变）：工作室级美术/GeckoLib 模型、实机多人烟测签字、既往泄露 Minecraft token 的账号侧吊销或刷新。
