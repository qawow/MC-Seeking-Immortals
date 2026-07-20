# 全玩法实现与验证总计划（供后续 AI 执行）

> 制定于 2026-07-20，基准版本 `0.2.73`，协议 25，全量 735 项测试通过，最近提交 `729cc9dd`。
> 本文档是后续 AI 的执行手册：先读本文，再读 `ai_handoff.md` 顶部 CURRENT TRUTH，然后按第 4 节路线图选下一批次。

## 1. 固定工作流（每批次必须遵守）

1. 读 `project_docs/ai_handoff.md`（顶部若干条即可）与 `step_progress.md`（顶部一条）确认当前真相。
2. 选定单一批次目标（见第 4 节），范围要小到一次构建能收口。
3. 备份将修改的文件到 `.bak/<时间戳>_<版本>_<slug>/`，保持相对路径。
4. `gradle.properties` 的 `mod_version` 按 `0.2.X` +1（代码/资源批次；纯文档不升）。
5. 实现最小改动；网络包字段/顺序或不兼容通道契约变更必须同时升 `ModNetwork.PROTOCOL_VERSION`（当前 26）。
6. 为每个新行为加测试（见第 5 节测试约定），跑 `./gradlew build --no-daemon`。
   同一批次中途返工需重建时用 `-PaiSkipVersionBumpCheck=true` 并在更新说明里写明原因。
7. 更新 `ai_handoff.md`（顶部插一条 CURRENT TRUTH）、`step_progress.md`（顶部插一节）、
   `pending_requests.md`（顶部插一条），新建 `project_docs/updates/<日期>_<版本>_<slug>.md`。
8. `git status --short` 审查后只 stage 本批文件，中文 subject + 中文 body 提交。禁止 push/PR。

## 2. 代码架构速查（改哪类玩法去哪里）

| 玩法域 | 权威入口 | 说明 |
|---|---|---|
| 修炼/境界/技能 | `cultivation/PlayerCultivation` + `CultivationHelper` | 一切玩家状态的唯一真相；改后必须 `SyncCultivationDataPacket` |
| 功法/术法 | `skill/SkillType`、`skill/effect/SkillEffectRegistry`、`AbstractTechniqueEffectResolver` | 术法运行时按 corpus 字段生成；未知类型 fail-closed |
| 丹药 | `item/pill/PillEffectCatalog` + `BasePillItem` | 114 条目录效果；新丹药先进目录再接消费端 |
| 批量物品 | `registry/ModBulkItems` + `BulkItemClassifier` + `CatalogConsumableService` | 新可执行消耗品 = 分类器 EXECUTABLE 集 + effect switch + 双语消息 |
| 物品 id 解析 | `catalog/ItemCatalogService` | 别名链 + bulk 载体；任何文本材料 id 都应能 resolve |
| 多方块工站 | `structure/MultiblockStationService`（成型）+ `MultiblockOperationalService`（运行态） | 新工站要三件套：station_patterns 条目、validator 分发、材料表 |
| 交付 | `item/InventoryDeliveryService.giveOrEnqueue` | 任何奖励/退款路径禁止 `player.drop`，登录自动补发 |
| 阵法 | `structure/FormationFieldService` + `FormationFieldCatalog` | 自由阵场/环阵两类 |
| 世界/区域/秘境 | `worldpack/WorldpackGameplayService`（travel/enterSecretRealm） | 区域 id 与秘境 id 是两套命名空间，勿混用（0.2.73 教训） |
| 经济/商店/拍卖 | `shop/ShopService`、`catalog/AuctionSoftService` | 菜单令牌 + 服务端派生价格 |
| 任务 | `quest/TextQuestChainService` + `QuestHookRuntime` | stage>0 才可推进；奖励走一次性账本 |
| NPC/对话 | `npc/DialogueActionExecutor` 等 | 对话动作全部服务端校验 |

## 3. 已知遗留风险（按需修，勿重复审查）

0.2.73 两轮审查已覆盖：多方块运行态、交付 outbox、消耗品、网络包、任务链、战斗。剩余已知但**有意未修**的项：

- **T4/T5 丹炉运行态钳制到 g3**：结构目录无 g4/g5 条目，运行态共享 g3。修法同 0.2.73 的 G2 修复（pattern 条目 + tier 传递）。
- **发放标志 crash 窗口**：灵根测石玉瓶与指南书是"先入 outbox 后置标志"，崩溃窗口产生的是**可恢复的重复**而非丢失——这是有意选择，不要反向"修"成先置标志。
- **附近工站扫描开销**：`bestNearbyEfficiency`/`tryCommissionNearby` 9×4×9 扫描，有 TTL 缓存兜底；如需优化，做 best==1.0 早退即可，勿改语义。
- **飞升恢复满包走 outbox**：整叠入 outbox 而非部分塞入，是防丢语义，仅是 UX 差异，已有 queued 提示。

## 4. 剩余玩法路线图（按优先级）

每项 = 一个或几个批次。完成一项就在本文档勾掉并在 handoff 记录。

### P0 — 玩法闭环缺口
1. ~~**T4/T5 丹炉运行态条目**~~（0.2.74 完成）（小批次：station_patterns g4/g5 + validator + AlchemyFurnaceBlockEntity 去钳制）。
2. ~~**工站材料表补全**~~（0.2.75 审计收口：245 材料 id 全部可解析）：`MultiblockMaterialCatalog` 覆盖 86 结构中有材料的 86 项，但 form/overhaul 只对"玩家常用工站"验证过；逐站抽查 `unresolvedShardTax`，为高频站补别名降税。
3. ~~**炼丹爆炸链与工站耐久联动**~~（0.2.75 完成）：炼丹失败/爆炸应 `applyDamage` 到工站运行态（现在只有 inspect 会漂移损伤），形成损耗→修理循环。
4. ~~**术法 corpus 未消费字段清点**~~（0.2.76 完成：18 key 对账，2 处路由修复）：`data/seeking_immortals/cultivation/` 中 `effect_key/tags` 仍有未映射值的术法逐条落实（用第 5.3 节的 corpus 对账测试模式）。

### P1 — 深度系统
5. ~~**秘境内容充实**~~（0.2.84 完成：19 条作者深潜秘境全部具备分层 trial、至少一个可生成命名 Boss 与非空奖励表；23 个目录 Boss 全部可执行并完成源/运行时对账）。
6. ~~**宗门专属内容**~~（0.2.86 完成：30 个可玩宗门均有唯一的阶段功法授予、贡献堂折扣与任务贡献/专精熟练增量；既有成员同步时自动补授，源数据与运行时三链均有测试门）。
7. ~~**NPC 对话树扩展**~~（0.2.88 完成：天南、北境、乱星海、大晋、灵界、阴冥 6 个区域组各有商人/执事画像与分支树；179 个具名 NPC 均获得可解析对话树，宗门执事打开本宗贡献堂，区域选择完全由资源画像驱动）。
8. ~~**灵兽/傀儡成长**~~（0.2.90 完成：共享 0–20 级经验曲线；灵兽按作者阶段数在化形池突破并提升阶位/属性，旧 Growth 无损迁移；7 条傀儡配方映射持久核心成长，组装/战斗/修理供经验并在核心炉按 7/14/20 级淬炼）。

### P2 — 打磨
9. ~~**JEI 集成扩展**~~（0.2.92 完成：129 炼丹、73 炼器、24 制符配方以权威材料/产物全量展示；远程客户端从打包清单补齐炼丹语料，运行时 reload 条目按 id 覆盖；三阶炼器炉、制符台和五阶丹炉均为 catalyst，丹炉进度区可点击打开 JEI）。
10. **Patchouli 指南补页**：新系统（工站运行态、outbox、消耗品语义）写进指南。
11. **美术/GeckoLib**：占位贴图替换，非阻塞，最后做。

### 持续项
- **实机烟测**（见第 6 节）——每完成一个 P0/P1 项后至少跑一次 runClient 快检。

## 5. 验证方法论

### 5.1 测试三层
1. **纯逻辑单测**：无注册表依赖的公式/钳制/解析（如 `LifeSkillServiceTest`、`MultiblockOperationalServiceTest` 的 efficiency 曲线）。新公式必须有。
2. **源码契约测试**：读源码字符串断言关键调用顺序/存在性（如 "reserve 必须在 commit 前"、"必须走 giveOrEnqueue"）。适合权威顺序类保证，脆弱但有效；断言尽量用语义片段而非整行。
3. **资源对账测试**：遍历发布 JSON 与运行时目录互相对账（如 `ArtifactRefinementRecipeAuthorityResourceTest`）。新数据文件必须有 count/字段校验。

### 5.2 每批次验证清单
- [ ] 定向测试先跑（`./gradlew test --tests '...'`），再全量 build。
- [ ] 全量测试数只增不减，0 failures/errors/skipped；数字变化要能解释（如结构目录 86→87）。
- [ ] 新增双语 lang key 后 JSON parse 校验（python json.load 即可）。
- [ ] `git diff --check` 无空白问题；diff 里没有无关文件。

### 5.3 Bug 审查方法（复用 0.2.73 模式）
- 按域并行开 Explore 子代理，每个代理给定**具体怀疑点清单**（不是"找 bug"而是"验证 X 处是否 Y"）。
- 代理结论必须逐条人工复核源码后才修：0.2.73 中代理误报率约 1/3（如"阵盘不该消耗"与设计冲突、"margin 除零"实际有 clamp）。
- 修一条 = 加一条回归测试。
- 经典缺陷模式（本项目高发）：
  a. **id 命名空间混用**（区域 vs 秘境、别名 vs 原始 id、大小写归一不对称）；
  b. **fail-open 的失败分支**（失败仍返回 true → 物品被吞）；
  c. **标志/状态在副作用前置位**（崩溃窗口、重试丢失）；
  d. **绕过成本的替代路径**（repair 绕 form、工作台绕炼器）；
  e. **新默认值破坏旧存档**（DISABLED 默认 + 自动 form 补救——改默认值时必须想旧世界）。

## 6. 实机烟测清单（runClient，每个 P0/P1 项后抽查）

1. 新建世界 → 首登收到指南书与同步数据，无报错日志。
2. `/seeking_immortals lingli|realm|root` 三命令输出正常。
3. 蒲团打坐 → HUD 出现、修为增长；V 键中断。
4. 搭一品丹炉（炉+盖）→ `station form alchemy_furnace_g1` 或直接开炉（自动启封）→ 炼一炉丹成功/失败路径各一次。
5. 制符台 3×3 → 制符一次；背包塞满再制一次 → 产物入 outbox，重登补发。
6. 商店买一件、拍卖出一价、任务链 start→advance 一步。
7. 消耗品抽查：船票（乱星海）、聚灵阵盘、残图（见闻 first_discover 消息）。
8. `logs/latest.log` 搜 `ERROR`/`Exception`，除已知第三方噪音外应为零。
9. 专服 + 双客户端：PvP 一次、同一工站两人并发制作、断线重连后 outbox/冷却状态正确。

烟测发现的问题按第 5.3 节流程修复，并把复现步骤写进对应 update 文档。

## 7. 完成定义

以下全部为真时，标记总目标完成：
- 第 4 节 P0 全部完成，P1 至少完成秘境与宗门两项；
- 目录中不存在"右键无任何服务端效果"的非材料类物品（材料/组件类以 tooltip 说明用途为准）；
- 全量测试通过且覆盖每个新系统的三层测试；
- 至少一轮完整实机烟测（第 6 节 1–8 项）记录在 updates 文档；
- `missing_and_placeholders.md` 顶部只剩美术类与"需真人多人复签"类条目。
