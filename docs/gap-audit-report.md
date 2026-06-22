# 未实现功能与不确定点盘点报告

> 本报告盘点「寻仙问道」模组当前尚未实现的功能，以及文档/进度中存在的不确定点。
>
> - **盘点日期**：2026-06-20
> - **当前版本**：`0.1.47`（`gradle.properties` 实测）
> - **核实方法**：交叉阅读 `docs/task-board.md`、`docs/mvp-scope.md`、`project_docs/features.md`、`project_docs/pending_requests.md`、`project_docs/missing_and_placeholders.md`、`project_docs/step_progress.md`、`project_docs/ai_handoff.md`，并对关键事实核对源码（`Realm.java`、`SkillType.java`、`SkillEffectRegistry.java`、`ModEvents.java`、`ModBlocks.java`、`ModEntities.java`、`AlchemyRecipeService.java`、`LingGenIdentificationSlabBlock.java` 等）。
> - **配套文档**：本报告与 `docs/task-board.md`、`project_docs/pending_requests.md`、`project_docs/missing_and_placeholders.md` 互补；后两者按版本号维护"待实现/待细化"与"占位"清单，本报告按"是否影响 MVP 验收"与"不确定来源"重新归类，并纠正了文档间互相矛盾之处。

---

## 一、还没实现的功能

### A. MVP 范围内、但完全没做的（影响 MVP 验收的核心缺口）

| 系统 | 状态 | 证据 |
| --- | --- | --- |
| **任务线**（七玄门 5 阶段 + 越国宗门前 3 阶段） | 未实现 | 无 `Quest` / `QuestProgress` 类，`task-board.md` Phase 9/10 全 `[ ]` |
| **NPC 系统**（墨老先生、七长老、李真人、厉飞羽） | 未实现 | `ModEntities` 只注册 `cushion_seat`、`sword_projectile`；NPC 兑换仍复用原版村民 |
| **对话系统**（JSON 对话树） | 未实现 | 全仓无 `Dialogue` / `Conversation`，`data/` 下无对话 JSON |
| **制符系统**（制符台、符纸/灵砂/朱砂、绘制流程） | 未实现 | 只有 `FireTalismanItem` / `ArmorTalismanItem` / `SpeedTalismanItem` 成品 + 合成配方；`SkillType.TALISMAN_CRAFTING` 是纯枚举占位 |
| **炼器系统**（炼器台、器形图纸、灵矿分级、凡器→灵器→宝器） | 未实现 | 无炼器台方块；`SkillType.ARTIFACT_REFINING` 是纯枚举占位 |
| **筑基期 6 技能**（神识扩展 / 御剑飞行进阶 / 罡气护体 / 五行遁术 / 北斗剑阵 / 阵法感知） | 未实现 | `SkillEffectRegistry` 注册的 12 个效果全是练气期；`task-board.md` 标 Stub |
| **炼丹等级系统 LV1-10** | 未实现 | `AlchemyRecipeService.getAlchemySkillBonus()` 硬编码 `return 0.0`，无炼丹经验字段 |
| **炼丹完整 GUI** | 未实现 | 用「右键 + 聊天进度」替代，无配方/材料槽/进度条 GUI |
| **炼丹 JSON 配方目录** | 未实现 | 用静态 MVP 配方，未建 `data/seeking_immortals/recipes/alchemy/` |

### B. MVP 验收项里只做了一半的

- **灵根鉴定石板方块**：注册了真方块 `ling_gen_identification_slab`（有独立碰撞箱/blockstate/model/材质），但 `use()` 直接复用 `LingGenTestStoneItem.testPlayer(...)`，传 `consumeUse=false` → 不消耗、无限次、无独立冷却。
- **独立打坐修炼 GUI**：只有 `BreathingHudOverlay` HUD 覆盖层，无独立 `MeditationScreen`，无修炼速度详情面板。
- **神秘小瓶**：伪/杂灵根必得已接入；「其他灵根极低概率获得」未接入。
- **境界系统**：`Realm` 枚举完整定义 11 个境界（`MORTAL`/`QI_REFINING`/`FOUNDATION_ESTABLISHMENT`/`CORE_FORMATION`/`NASCENT_SOUL`/`SOUL_TRANSFORMATION`/`VOID_REFINING`/`UNITY`/`MAHAYANA`/`TRIBULATION`/`TRUE_IMMORTAL`），但**可玩内容停在化神**——炼虚及以上无突破路径/技能/天劫；练气 9 技能完整，筑基 6 技能全空，结丹/元婴/化神技能全空。
- **可骑乘飞剑实体**：无 `FlyingSwordEntity`，只有 `SwordProjectileEntity` 弹射物；飞行靠修改玩家 `Abilities.mayfly` / `flyingSpeed`，非骑乘。

### C. 设计已规划、超出 MVP、明确待做（`pending_requests.md` 主要项）

补天丹 · 特殊体质致命缺陷事件化（龙吟/冰髓之体）· 多灵根对炼虚突破优势 · 延寿丹 / 独立寿元 UI · 完整内心世界 / 黑化玩家 / 心魔战斗 · 真实「地灵之眼」方块/结构 · 金丹品质 · 天劫 / 五衰 / 斩三尸 · 闭关天数与环境灵气加成 · 飞剑/法宝美术/粒子/HUD/品阶/认主/强化/禁飞规则 · 神识接入探测与飞剑操控、肉身强度接生命值、天劫承受接渡劫伤害 · 验灵阵 / 血脉觉醒 / 洗灵根道具 / 独立灵根 GUI · 五行灵石正式贴图与来源 · 寻脉罗盘动态指针 · 秘境/仙府维度 · 下界炼体 / 末地法则感悟。

### D. 占位资源（`missing_and_placeholders.md`）

- 60 个 `technique_manual_*` 占位贴图
- 五行灵石程序占位贴图
- 飞剑/法宝占位贴图
- 中阶/高阶/极品灵石缺 PNG
- 测灵盘 / 寻脉罗盘 / 聚灵阵占位贴图
- 技能栏占位色块图标（无正式技能图标 / 冷却遮罩 / 释放动效）
- `ling_gen_identification_slab` / `alchemy_furnace` 复用 `spirit_gathering_array` 贴图
- Patchouli 图文页不完整

---

## 二、不确定的东西

### 1. 文档「双轨制」严重脱节（最核心的不确定）

项目有两条并行的进度追踪线，互相矛盾：

| 来源 | 说法 |
| --- | --- |
| `gradle.properties` / `features.md` / `pending_requests.md` | 当前 **0.1.47**，已实现飞行、走火入魔、六大核心属性等 |
| `task-board.md` | 当前 **Phase 9**（MVP 集成测试未开始），Phase 6 筑基技能是 Stub，Phase 8 刚完成资源验证 |
| `ai_handoff.md` / `step_progress.md` | 当前版本写的是 **0.1.34**（**严重过时**，连工作目录都还指向 `/AstrBot/...`） |
| `CLAUDE.md` | 写 **0.1.45**（过时） |

→ **「当前真相」以哪份为准不明确**。`task-board.md` 的 Phase 线像是事后给已写好的代码补审计报告（`phase-3`~`phase-8` 报告都是 2026-06-18~19 两天内生成的），但它仍把 Phase 6 筑基技能标成未做——而 0.1.47 线已经做了飞行等更新内容。按 `outline-driven.md` 规则 1「先读 task-board」+ 规则 2「只做当前 phase」，当前应做 **Phase 9 MVP 集成测试**，但 Phase 6 还没做，执行顺序冲突。

> **2026-06-20 更新：本不确定点已处理。** `task-board.md` 已重排对齐 0.1.47（P5 炼丹 / P6 神秘小瓶 / P7 基础 HUD / P8 资源；筑基技能并入 P9 待办；完成数 9/11，当前 Phase 9）；`ai_handoff.md`、`step_progress.md`、`CLAUDE.md` 过时版本号均已修正为 0.1.47。当前真相以 `docs/task-board.md` + `gradle.properties` 为准。下文表格保留对齐前的历史描述。

### 2. MVP 边界本身不确定

`mvp-scope.md` 把**制符、炼器、七玄门 + 越国任务线、NPC、对话**都列为 MVP 必需验收项，但实际一个都没做，而 `task-board.md` 把它们推到了 Phase 9/10。`outline-driven.md` 规则 10 说「设计文档与 MVP scope 冲突时以 mvp-scope 为准」——照此当前 MVP 还差一大半；但实际开发走了「先做机制后做内容」的路线。**到底 MVP 是否还包含这些内容系统，需要确认**，否则没法判断「还差多少」。

### 3. 飞行机制重复

代码里有**两套独立飞行**：

- ① `ModEvents.handleFlyingArtifact`（筑基及以上，装备 Curios `artifact` 槽的 `flying_sword` / `flying_artifact`，授予 `mayfly` + 按境界分档速度/限高/灵力消耗）。
- ② `FlyingSwordBeginnerSpell`（练气 7 层「御剑飞行初」技能，每秒 5 灵力，由 `ModEvents.handleQiFlying` 每 tick 维护）。

而 `task-board.md` 里的「御剑飞行进阶」技能从未实现。这两套未来是否合并、进阶版是否要补，不确定。

### 4. 工作区未提交改动

`git status` 有 **61 处改动**（26 改 + 35 新增），是 2026-06-18~19 一组连贯的 Phase 3-8 产物（`alchemy/` 包、新 `spell/` 类、`phase-3`~`phase-8` 报告等），**尚未 commit**。这些改动据各 phase 报告均声明 `gradlew build` 通过过，但当前工作区是否真的能干净构建、是否与已提交的 `b0d59ef` 一致，未现场验证。

### 5. 版本号跳号

`0.1.34` → `0.1.47`，中间 `0.1.35~0.1.46` 无对应 `project_docs/updates/` 文件，跳号原因未记录。

---

## 三、建议下一步（需决策方向）

最该先解决的是**文档双轨制**——否则每次接手都会被「当前到底是 Phase 9 还是 0.1.47、MVP 含不含任务线」卡住。可选方向：

1. **对齐文档**：把 `task-board.md` / `ai_handoff.md` / `step_progress.md` 的版本号与 Phase 状态刷新到与代码一致，明确「当前真相」。
2. **按 `task-board.md` 推进 Phase 9**：MVP 集成测试，前提是接受筑基技能 / 任务线 / NPC 等「留到后续 phase」。
3. **按 `mvp-scope.md` 补内容缺口**：从「任务系统 + NPC + 对话」（内容骨架）或「筑基 6 技能」（战斗补全）开始。

---

**报告版本**：v1.0
**生成时间**：2026-06-20
**核实范围**：`docs/`、`project_docs/` 关键文档 + 源码事实核对
**未执行**：未修改任何源码、未 commit、未运行 `gradlew build`
