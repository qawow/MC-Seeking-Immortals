# 剩余实施规划：D-A / Y 系列 / M 系列 / QA

> 依据 `implementation_plan.md`（0.2.246+ 后续计划）与 2026-08-02 四路代码现状探索。
> 当前基线：`mod_version=0.2.253`，`ModNetwork.PROTOCOL_VERSION=31`，HEAD=`860a2348`。

## 0. 执行规则（每个补丁批统一）

1. 重读 `ai_handoff.md` 顶部、`step_progress.md` 顶部、本文件对应批，确认无并发改动。
2. `git status --short` 区分既有改动；只处理本批文件（`CLAUDE.md`、`CC-Switch*.deb`、`frontend_interaction_audit_0.2.198.md` 不纳入）。
3. 编辑前备份到 `backups/<timestamp>_<slug>/` 保留相对路径。
4. 先写能复现缺陷的定向测试 → 最小实现。
5. 代码/资源/数据包变化 → `mod_version` +1 patch；网络包字段/顺序/类型/注册/频道行为变化 → 同时 `ModNetwork.PROTOCOL_VERSION+1`。
6. 生成器按 `spell -> visual` 顺序刷新并 `--check`（Java 审计哈希变化时）。
7. 定向测试 → 普通 `./gradlew build --no-daemon --max-workers=1`（不跳 preflight）。
8. 更新 `step_progress.md`、`project_docs/updates/`、`ai_handoff.md`。
9. `git diff --check`、只暂存本批文件、中文主题/正文本地提交；不 push/PR/amend。

---

## 1. D-A 对话世界动作分型（规模 L，拆 4 个补丁批，协议不变）

**目标（计划 L410-423）**：消除 `DialogueWorldActionService` 的过度泛化；每种作者动作有专用结果、失败语义与测试；未知动作 fail-closed。

**作者资源 D-A 家族全集（已核实 9 处）**：`mark_structure`×3、`hint`×1、`call_guard`×1、`combat_flag`×1、`combat_or_arrest`×1、`add_suspicion`×1、`anomaly_log`×1。无 `clue`（需补 handler）。

### D-A-1 对账测试 + dispatch 重构（0.2.254）
- **新测试** `npc/DialogueActionCoverageTest`：
  - 解析 `npc_dialogue_branches_v139.json` 全部 effect type，断言 D-A 家族 type 集合 == 全集；
  - 源码契约断言：`DialogueActionExecutor.execute` 中 `CALL_GUARD`/`COMBAT_FLAG`/`COMBAT_OR_ARREST` 三个 case 各自调用独立方法（不再共用 `triggerCombat`）；未知 `default` 仍 `effect_unsupported` fail-closed。
- **重构** `DialogueActionExecutor.java:151-156`：三个合并 case 拆为独立 case → 调 `DialogueWorldActionService.callGuard/combatFlag/combatOrArrest`；`HINT` 增加 `clue` type 别名。
- 保留现有 `DialogueWorldActionServiceTest:48-62` 断言的符号（`triggerCombat`/`putUUID(HOSTILE_PLAYER,...)`/`MAX_BOUND_HOSTILES`/`COMBAT_COOLDOWN_TICKS`）或同步更新测试。
- 版本 0.2.254；协议 31。

### D-A-2 mark_structure 意图匹配 + hint 来源绑定（0.2.255）
- `markStructure`（`DialogueWorldActionService.java:57-92`）增加：
  - 结构类别：`MultiblockStructureCatalog.StructureEntry.type` 与作者 effect `type` 参数匹配（缺省放宽）；
  - 维度：`StructureEntry.dimensions` 非空时要求 located 维度命中；
  - 任务步骤：标记结构必须是当前任务链当前步骤 `place/needs`（查 `DetailedQuestRuntimeService` 当前步骤）；
  - 幂等：`recordStructureFormed` 仅在首次标记时调用（避免重复证明）。
- `recordHint`（`:94-104`）绑定来源：`HINTS_TAG` 值改为含 `{Npc,Node,Region,Time,HintId}` 的 CompoundTag（保持布尔键兼容）；重复读取不重复推进/发奖。
- 新 lang：mark 的类别/维度/步骤拒绝提示（双语）。
- 版本 0.2.255；协议 31。

### D-A-3 suspicion/anomaly 分桶与结算（0.2.256）
- `addSuspicion`/`suspicion`（`:121-142`）：
  - 按 authorityId 分桶保持不变；增加时间衰减（常量 `SUSPICION_DECAY_PER_TICK`，读取/结算时递减）；
  - 阈值：`ARREST_THRESHOLD=60`、`WARN_THRESHOLD=30`，供 D-A-4 消费；
- `recordAnomaly`（`:106-119`）：按 npcId/势力分桶（`ANOMALIES_TAG` 子键 `faction:<id>`），条目含写入时间；`trimOldest`（`:260-264`）改为按 `At` 时间戳淘汰（保留字典序作为最后防线）。
- 新 lang：疑点衰减/阈值/结算提示（双语）。
- 版本 0.2.256；协议 31。

### D-A-4 call_guard / combat_flag / combat_or_arrest 分型（0.2.257）
- `callGuard(player,npcId,treeId)`：复用 `SummonedServitorEntity`——`configure(owner,...,archetype)` + `setStance(Stance.GUARD)` + 设置 `GuardX/Y/Z`；外观/强度按 `NamedNpcRegistry` 势力角色选 archetype；绑定执法目标（保护玩家、对威胁敌对，沿用 `canAttack`/`enforceDialogueTarget` 模式）。
- `combatFlag(player,npcId,treeId)`：只写 `COMBAT_TAG` 敌对后果 + `applyHostilityPenalty` + 敌对标记，**不生成实体**。
- `combatOrArrest(player,npcId,treeId)`：按 `suspicion(player,faction)` + 玩家分支/声望决策——
  - 低疑点 → 警告（消息 + favor/声望轻微）；
  - 中疑点 → 缴罚（扣灵石/贡献 + 声望）；
  - 高疑点 → 逮捕（守卫生成 + 押送到可恢复落点〔复用 WorldpackSavedData 锚点/入口落点〕+ 明确解除条件〔缴罚/挣脱〕）；
  - 敌对分支 → 现有 combat hostile shell。
- 端到端验收场景：`tree_heavenly_inspector`：`explain`(add_suspicion) → `wanted`(combat_or_arrest) 天然链路。
- 版本 0.2.257；协议 31。

**D-A 验收门**：`DialogueActionCoverageTest` + 各动作行为/失败语义测试；双语对等；生成器 `--check`；完整 build；备份/文档/提交。

---

## 2. Y 系列 阴阳窟专属实施（规模 L×3，协议不变）

**目标（计划 L425-457）**：专属场景/编队 → 活捕/运输 → 协作炼丹闭环；验收"全程不使用管理员命令"。

**前置缺口（探索确认）**：
- `yinyang_ku` 无运行时条目，`yy_*` 区域数据错位在 `secret_realm_runtime.json#yin_mountain_catacomb`（行 2473-2598）；
- `silver_wing_yaksha` 图鉴条目存在但无 spawn 引用；实际刷新 `yinchi_yecha`；runtime 层用不存在的 `silver_wing_yezha/yezha_young/yin_sha_mist`；
- `yin_zhi_horse` 图鉴缺失 → `ENTITY_CAPTURED_ALIVE(entity=yin_zhi_horse)` 永远不可达；`yin_zhi_ma`/`yinyang_yinzhima` tameable 解析为 false；
- 丹炉单玩家（`AlchemyFurnaceBlockEntity` 单一 craftPlayer）、`nascent_soul_pill` 配方要求炉 5 级与任务路由 `alchemy_furnace_g3` 矛盾、配方不含阴芝马材料。

### Y-A 专属场景与银翅夜叉编队（0.2.258）
- 修正 `yy_*` 数据归属：把 `yy_outer/yy_split/yy_yezha/yy_yinzhi/yy_alchemy_coop` 从 `yin_mountain_catacomb` 条目移到 `yinyang_ku` 运行时条目（新增 `yinyang_ku` 运行时 realm 定义）；确认 `SecretRealmTrialService.isShellTrialRealm` 命中。
- `silver_wing_yaksha` 用 `CultivationBeastEntity` 数据驱动承载：补图鉴条目（`silver_wing_yaksha`/`silver_wing_yezha`/`yezha_young`），修复 spawn 引用（runtime 层/`region_spawn_tables_v98.json`）；补驯服/行为字段。
- 编队：`TrialCombatShellService`/`SecretRealmTrialService` 扩展——外围幼体/巢区成体/领队分层生成，数量受会话预算（复用现有预算）；风土遁/幻术/阴煞取现有作者术法。
- 战斗与绕行显式分支：绕行=资源/献祭原子扣除（新 handler，复用贡献/物品扣除事务）；战斗沿用遭遇清场证明 `ENCOUNTER_CLEARED(yy_yezha)`。
- 版本 0.2.258；协议 31。

### Y-B 阴芝马活捕与运输（0.2.259）
- 补 `yin_zhi_horse` / `yinyang_yinzhima` 图鉴条目（`tameable:true` 或 `capture`），使 `ArtifactCaptureService.releaseOrCapture` 的 `isCapturableTarget` 放行；栖息层会话生成接线（yy_yinzhi 层）。
- 专用活体载体：新物品/载体（或在 `CaptureJarItem` 上扩展）带 `capture_uuid/life_state/source_session`；不可堆叠、不可跨会话重复提交。
- 击杀只产劣材（`yin_zhi_horse_live` 绝不产出）；运输死亡/超时转劣材 + 失败分支；丢弃/死亡/满背包/断线用 `InventoryDeliveryService.giveOrEnqueue` outbox 恢复。
- 任务只接受 `ENTITY_CAPTURED_ALIVE` 或验证后交付（现有路由已定义，补生产者可达）。
- 版本 0.2.259；协议 31。

### Y-C 窟外协作炼丹（0.2.260）
- 出窟接应点 NPC 协作/独炼分支（复用 `AlchemyFurnaceBlockEntity` + 对话动作）；要求成型丹炉 + 合法活体/劣材输入。
- 事务顺序固定：校验会话步骤 → 预留材料/贡献 → 锁定活体 → 算成功率 → 产物/失败 → 账本；重复包不重复投骰。
- 成功率约两成基线 + 活捕/熟练/协作/工站叠加；上下限纯逻辑测试。
- 爆炉对工站耐久/玩家状态后果（已有磨损/爆炸，补阴芝马锁定回滚）。
- 产物/分成/退款统一 `giveOrEnqueue`；多人按参与快照发放。
- 配方修正：新 `peiying` 配方以阴芝马活体/劣材为主料、炉级匹配任务路由（g3），或调整现 `nascent_soul_pill` 配方。
- 版本 0.2.260；协议 31（新增多人菜单才评估协议）。

**Y 系列验收门**：闭环从接取情报→进窟→绕行/战斗→活捕/击杀→出窟→协作/独炼→领奖励不用管理员命令；专项测试（无 `YinYang*Test` 先例，全新增）。

---

## 3. M 系列（规模 M/L，协议不变；可与 Y 交替）

### M-A 维度状态分类（规模 M，0.2.261）
**目标（计划 L461-471）**：`playable/preview_locked/abstract_template/logical_cluster` 四类；抽象/逻辑不计入"待实现"；空壳诚实标注。
- `DimensionRegistryService`：新增分类枚举字段；`secret_realm_instance`→abstract_template、`yin_underworld`→logical_cluster 移出 `deferredIds()`；`deferredIds()` 去重（现 4 项/2 唯一，`ingestRegistry`+`ingestIndex` 重复 `deferred.add`）。
- 空壳审计：`immortal_realm`（status 空、`playable=false` 现被 `get` 误标"可进入"）、`asura_realm`（`playable=true` 过度乐观）→ 标 `preview_locked` 并关闭普通旅行（`DimensionTravelService`/入口门禁）。
- 命令 `catalogDimensions`/`catalogDimensionGet`（`SeekingImmortalsCommand:1734-1778`）改四类展示，修 `def.isDeferred()?:"可进入"` 误报。
- 数据源收敛：分类从 `dimensions_reconcile.json` 单一资源驱动。
- 测试：`M13DimensionsAscensionTest:38-41` 方向反转（抽象/逻辑不在 deferred）；新增"每个 playable 有 datapack 维度+可达入口"、"preview 不能旅行"断言；更新 `dimensions_reconcile.json`。
- 版本 0.2.261；协议 31。

### M-B 本命飞剑实例绑定与迁移（规模 L，0.2.262）
**目标（计划 L473-485，含兼容 NBT 迁移测试）**：
- `ArtifactOwnershipService.claim`（`:69-72`）删除自动 `bind`（只认主）。
- `NatalBindingService` 本命根加 `artifact_id + instance_uuid + schema_version + growth`；目标物品记录相同 instance UUID；收益（`ArtifactActivationService:222/254/277/287`、`ArtifactActiveSkillService:169`、`ArtifactRefinementService:225`、`mirrorGrowthToHeld:98-106`）只作用于精确实例，同 id 第二件不继承。
- 双手绑定交互：胚 `natal_sword_embryo` + 已认主飞剑 + 结丹门槛，潜行原子消耗胚并绑定目标。
- 失败关闭：他人持有/复制 NBT/目标损坏/不在手/已有本命/背包替换/交易。
- 旧档 `ArtifactId/Growth` 标记 legacy binding → 首次持有唯一匹配物品迁移；多候选要求用胚确认；permission 2 诊断/救档命令。
- tooltip 改准确双手说明；`hasActivation` 不误当主动技（已满足，加固）。
- 版本 0.2.262；协议 31（NBT 兼容迁移不升协议）。

### M-C 旧命名 NPC 迁移（规模 M，0.2.263）
**目标（计划 L487-498）**：
- Villager 持久 `SeekingImmortalsNpcId` + 迁移版本 + 来源区域 + 迁移时间；专用 `CultivatorNpcEntity` 继续权威 id（现 `NamedNpcId` 已实现）。
- 名称识别收敛：仅"无持久 id + 已知旧实体类型 + 合法命名 NPC + 匹配区域/锚点 + 未迁移"窗口一次；首次交互写持久 id，之后只读持久 id。
- 命名牌伪造拒绝（无合法锚点/来源）。
- 世界升级审计命令：报告已迁移/歧义/拒绝/待处理（不自动删除玩家实体）。
- 扩充 `QuestNamedVillagerAuthorityTest`/`TextQuestNpcHookServiceTest`/`NamedNpcPlacementSavedDataTest`：名称伪造、合法迁移、重启、跨维、重复实体、不匹配区域。
- 版本 0.2.263；协议 31。

---

## 4. QA 发布签字（QA-01/QA-02，P0）

- **QA-01 单客户端全流程签字**：新 JAR，报告记录版本/协议/步骤/日志审计（完成任务链、对话 D-A 动作、突破、宗门、拍卖、阴阳窟闭环不用管理员命令）。
- **QA-02 专服 + 双客户端签字**：竞价、工站、outbox、冷却、PvP、断线重连。
- 必须全部代码批完成后重做签字，不复用旧版本签字；签字报告由实际执行者写时间/JAR SHA-256/mod_version/protocol/日志位置/失败重试。
- 已完成自动门（计划 10.1）每批执行。

---

## 5. 依赖顺序总览

```text
D-A（4 补丁批，0.2.254-0.2.257）
  └─> Y-A（0.2.258）→ Y-B（0.2.259）→ Y-C（0.2.260）   ← 玩法主线串行

M-A（0.2.261）┐
M-B（0.2.262）├─ 可与 D-A/Y 交替，互不依赖
M-C（0.2.263）┘

全部完成后 ──> QA-01 单客户端 / QA-02 专服+双客户端 签字 → 发布
```

版本号按真实执行时工作树计算，上述为预估序列，实际以 preflight 为准；D-A/Y/M 均不改网络协议（31），`F-E2` 已是 31，后续无包变更则保持。
