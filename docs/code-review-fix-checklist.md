# 代码审查修复落地清单（0.1.47 工作区）

> - **生成日期**：2026-06-22
> - **配套报告**：`docs/code-review-report.md`（v1.0）
> - **配套双方案**：`docs/code-review-fix-alternatives.md`（v1.0，每 bug 两方案+推荐）
> - **用途**：可勾选的精简落地清单。每 bug 一行：位置 → 推荐改法 → 协议影响 → 验证要点。推荐方案取自 `code-review-fix-alternatives.md` 的"推荐"结论（经 3 路 agent 实读源码核实）。
> - **图例**：`[ ]` 待办 / `[x]` 完成；协议列 `bump` = 需升 `ModNetwork.PROTOCOL_VERSION`，`—` = 无需。
> - **落地规则**：每批改完先 `.bak/<timestamp>/` 备份 → 改 → `./gradlew build` → 更新 `project_docs/step_progress.md` → 勾选。

---

## 第一批：防永久飞行 / 崩服 / 白扣灵力（高确定性局部修复）

- [x] **H2** 飞行高度方向反 — `ModEvents.java:484` — `player.getY() > profile.maxHeight()` 删 `+ getMinBuildHeight()` — 协议 `—` — 验证：筑基 Y=95 不掉、97 掉
- [x] **H11** 飞行无生命周期清理 + 不校验灵力 — `ModEvents.java` 新增 `PlayerRespawnEvent`/`PlayerChangedDimensionEvent`/`LivingDeathEvent` 钩子调 `resetFlyingState`；`grantFlying` 入口加灵力门 — 协议 `—` — 验证：飞行中 `/kill`/跨维度/灵力耗尽复活后无法飞
- [x] **H1** 两套飞行 mayfly 互不感知 — `ModEvents.java:545-586` + `FlyingSwordBeginnerSpell.java:32-57` — **采用方案 A（引用计数）**：新建 `cultivation/FlyingAuthority.java`（来源集合+首源采样基线），grant/revoke 委托之 — 协议 `—` — 验证：先御剑再装法宝→卸法宝→收御剑 mayfly 正确关闭
- [x] **H3** MysticVial NPE — `MysticVialItem.java:99` — `isGrowTarget` 改签名传 level+pos，`useOn` 传实参 — 协议 `—` — 验证：右键树苗/蘑菇/草丛不崩
- [x] **H4** 未注册 effect 仍扣费进冷却 — `ReleaseTechniquePacket.java:84-148` — effect/skill 缺失 early return + "无法施展"提示，删 `:121` 兜底扣费，仅 `effectExecuted=true` 才 setCooldown+成功提示；加 lang key `...technique_release.effect_unavailable` — 协议 `—` — 验证：藤蔓/金刃/五行轮转按释放键收到"无法施展"且灵力/冷却不变
- [x] **H5** execute 失败已扣灵力不退费 — `ReleaseTechniquePacket.java:97-114` — 改 execute-first：先 `canExecute`+`execute` 成功后再 `consumeSpiritualPower`，execute 前只检查不扣 — 协议 `—` — 验证：对墙释放土遁失败不扣灵力；灵力刚好不够释放火球提示 not_enough_qi

## 第二批：玩法正确性 + 经济/世界完整性

- [x] **H8** EXTREME 掉落保留50% — `BreakthroughService.java` + `ModEvents.onPlayerRespawn` — EXTREME 改 `preserveHalfInventory`（序列化50%到 PersistentData ListTag + 移除背包不掉世界）→ `hurt` 死亡→`onPlayerRespawn` 调 `restorePreservedOnRespawn` 归还(用完即删 key)；删旧 `dropHalfInventory` 死代码 — 协议 `—` — 验证：keepInventory=false 服地上50%+重生50%；keepInventory=true 重生保留50%；二次EXTREME不归还旧物
- [x] **H6** 凡人灵力上限 0 — `PlayerCultivation.java:670-672,304-308` — `getMaxSpiritualPower()` MORTAL `Math.max(50, raw)`；`clearSevereInjuryIfRecovered` 改独立恢复阈值 — 协议 `—` — 验证：新凡人打坐回复到 50；`/affliction severe_injury` 后凡人 HP 有 -80% 且不秒清
- [x] **H7** risk=100 仅 0.50 触发 — `PlayerCultivation.java:991-995` — 入口 `if (qiDeviationRisk >= MAX_QI_DEVIATION_RISK) return true;` — 协议 `—` — 验证：risk=100 100% 走 EXTREME 死亡
- [x] **H13** 炼丹技能无效 — `AlchemyRecipeService.java` + `AlchemyFurnaceBlockEntity.java` + `PlayerCultivation.hasAlchemy()` — `getAlchemySkillBonus` 读 `SkillType.ALCHEMY` level（+0.02/级，上限+0.20，未学=0）；成丹时 `grantAlchemyExperience` 解锁并 `addSkillExperience(ALCHEMY,25)`，开炉玩家 UUID 持久化 — 协议 `—` — 验证：LV1/LV10 炼丹各 20 炉统计成功率/爆炉率差异；连续成丹可升 ALCHEMY 等级
- [x] **H9** 飞剑伤害二次重算 — `ModEvents.java:175-219` + `SwordProjectileEntity.java:63-73` — 弹射物构造打 `SeekingImmortalsProjectileDamage` NBT，`onLivingHurt` 顶部对 `SwordProjectileEntity instanceof` 跳过 `:194-218`，保留 `:188-192` 火球改写 — 协议 `—` — 验证：玩家 A 飞剑刺 B 伤害=生成时 calculateDamage，不被 dodge/miss 吞
- [x] **M7** 土遁步穿薄墙 — `EarthEscapeStepSpell.java` — 新增 `isPathClear` 沿起止点身体通道逐格检查实心阻挡（已有 `canStandAt` 落点检查 + `isLoaded` 经由 `getCollisionShape` 隐式安全），厚墙/薄墙皆不可穿 — 协议 `—` — 验证：1 格厚墙失败、空地可遁、边界区块不崩
- [x] **S-EarthWall** 永久地形刷石 — `EarthWallSpell.java` + 新建 `EarthWallBlock`（重写 `tick` 设 air）+ 注册 `earth_wall`（blockstate/model/复用 vanilla dirt 贴图，无 BlockItem） — `setBlock` + `level.scheduleTick(pos, this, 200)`（200 tick=10 秒后消散） — 协议 `—` — 验证：放土墙 10 秒后消失，不可刷石
- [x] **M3** 走火衰减几乎不触发 — `ModEvents.java:139-149` + `PlayerCultivation` — 新增 `qiDevDecayAccumulatorTicks`/`leylineQiDevDecayAccumulatorTicks`（持久化 NBT），平稳打坐 tick 累加达阈值 -1；`ModEvents:139-149` 替换为 `cultivation.tickQiDeviationDecay(leyline)` — 协议 `—` — 验证：堆 risk=50 平稳打坐 12 分钟稳定 -1
- [x] **M13** 寿元死亡误触走火 — `ModEvents.java:645-648,175-186` — 寿元死亡前设 `SeekingImmortalsLifespanDeath` flag + 改 `outOfWorld()`，`onLivingHurt` 顶部 flag early-return（用完即删+tick 末兜底删） — 协议 `—` — 验证：设寿元到上限并打坐，耗尽致死直接死无走火消息
- [x] **M14** lifespanYears 回退值 — `PlayerCultivation.load` — 回退值改 `realm.getLifespanYears()`（凡人玩家=MORTAL=80，练气=100） — 协议 `—` — 验证：无 LifespanYears NBT 的 MORTAL 旧存档加载后=80
- [x] **M1** 凝气丹 boost 不续期 — `PlayerCultivation.java:792-795` + `CultivationPillItem.java:29` — ticks 改 `min(2*BOOST_TICKS, current+ticks)`，multiplier 仍 `max`；`use` 已生效给续期提示 lang key — 协议 `—` — 验证：连服 3 颗时长累加且有续期提示
- [x] **M2** 回灵丹 qiValue 失效 — `QiRecoveryPillItem.java:25-26` — `Math.max(round(qiValue*absorption), ceil(maxSP*0.1))` — 协议 `—` — 验证：同境界低/中/高回灵丹回复量随 qiValue 递增
- [x] **M5** 村民兑换吞正常交易 — `ModEvents.onVillagerExchange` — `setCanceled` 移入实际兑换成功 if 内，达上限/无灵石分支放行 vanilla — 协议 `—` — 验证：有灵石兑换不开 GUI；无灵石/达上限开 vanilla GUI
- [x] **M15** PillQuality 不参与产物 — `AlchemyRecipe`(outputsByQuality) + `AlchemyFurnaceBlockEntity.rollOutputQuality` + 新增 6 丹药(筑基丹/稳神丹 各 中/上/极品) — `output`→`outputsByQuality`(按 PillQuality.ordinal)，成丹按成功余量+炼丹等级掷骰定品质；legacy cultivation/qi_recovery 配方 uniform 不参与品质掷骰；旧 `foundation_building_pill_low` id 仍 findById 兼容存档 — 协议 `—` — 验证：LV1/LV10 各炼筑基丹，品质分布随等级/余量偏移
- [x] **M16** 三丹未用 absorption — `BasePillItem.effectiveMultiplier(player)` + HealingPill/FastingPill/ClearSpiritPowder — 抽 `effectiveMultiplier(player)`=品质×吸收率，三子类统一用之；ClearSpiritPowder 吸收率≥1.2 额外清缓慢/虚弱/挖掘疲劳 — 协议 `—` — 验证：高/低资质灵根服同品疗伤丹回血差异；高吸收清灵散额外解负面

## 第三批：数据驱动重构 + 同步链路（工作量较大，建议单独 PR）

- [x] **M10** 消耗/境界文本猜测 — `TechniqueDataManager.java:162-176,206` + 6 个 technique JSON — `TechniqueEntry` 增 `int cost`/`Realm requiredRealm`，JSON 加 `cost`/`required_realm`，`estimateCost`→`technique.cost()`、`estimateTechniqueRealm`→`technique.requiredRealm()` — 协议 `—`（改 JSON+record）— 验证：`/reload` 后火球扣费=10；化神功法练气期释放触发走火提示
- [x] **M9** 冷却跨维度不同步 — `PlayerCultivation.java:160-171` + `ReleaseTechniquePacket.java:55,127` — 改 `player.getServer().overworld().getGameTime()`；旧存档冷却 map 首次登录清空或加版本标记 — 协议 `—`（NBT 迁移）— 验证：overworld 释放→进 nether 同技能仍显示冷却中
- [x] **M12** HUD 进度条硬编码 0.35 — `SyncCultivationDataPacket.java` + `ClientCultivationData.Snapshot` + `CultivationHudOverlay.java:44-49` — record 增 `long cultivationMax`，`from`/encode/decode 配对 `writeLong/readLong`，`Snapshot` 增字段（`empty()` 默认 1L），HUD 改真实比值 — 协议 **`bump 4→5`** — 验证：修为条随 cultivation 增长真实填充；突破后阶段 cap 重置归零
- [x] **M11** writeUtf 加上界 — `SyncCultivationDataPacket.encode` — 短文本(realm/stage/specialPhysique/auraNature) `writeUtf(cap(s,64),64)`；`spiritualRoot` 128；`spiritualRootAttributes` 256；新增 `cap(s,maxLen)` 截断 helper；decode `readUtf()` 不变 — 协议 `—`（加长度上界不改字段，与 M12 同批不额外 bump）— 验证：5 属性灵根同步 HUD 正常
- [x] **H10** 客户端清空手动绑定 — `ClientTechniqueData.java:37-53,96-104` — `setLearnedTechniques` 只存 learned 不碰 `techniqueSlots`；`normalizeSlots` 永远返回当前 slots 清理结果（不 defaultSlots） — 协议 `—` — 验证：学习/遗忘技法、重登后 7 槽绑定保留
- [x] **M8** DetectionSpell 性能 — `DetectionSpell.java:36-54` — `maxBlockMatches=32` 达上限 labeled break，`isLoaded(pos)` 跳过，每匹配 1 粒子（保范围） — 协议 `—` — 验证：多玩家同时释放用 spark 观察主线程占用
- [x] **M6** 灵石 NBT 腐坏防护（守卫方案） — `SpiritStoneItem.consumeStoredPower` — 实读确认 `use()`/两调用方全程 count==1 门控，`consumeStoredPower` 的 `count!=1→return 0` 守卫已达成防 NBT 腐坏核心目标；多叠吸收与单颗架构相悖且 split/merge 有 NBT 语义陷阱，本轮守卫方案视为落地，多叠吸收未启用 — 协议 `—` — 验证：单颗灵石右键吸收/被动加成/打坐耗灵力正常，NBT 不腐坏
- [x] **M4** 中性灵石无被动加成 — `ModEvents.java:395-400` — `:398` 放宽非五行灵根匹配（返回 `stone.getPassiveBonus()` 或半额）；修订 CLAUDE.md "中性灵石"措辞 — 协议 `—` — 验证：变异灵根玩家持五行石打坐 spiritualPower 加成生效

## 第四批：stub / 设计整改（按 Phase 排期）

- [x] **S-未注册技能止血** 随 H4 落地自动止血（无 effect=拒绝释放）
- [ ] **S-筑基 6 技能** 逐个实现 SkillEffect（须先修 H4）— 按排期
- [ ] **S-Invisibility/LightBody 打破隐身** `onLivingHurt`/`AttackEntityEvent` 玩家造成伤害 `removeEffect(INVISIBILITY)`
- [ ] **S-神识消耗** `ReleaseTechniquePacket` 扣灵力后追加 `dcCost` 检查 + `consumeDivSense`（DETECTION/SOUL_SEARCH）
- [ ] **S-PvE 不经 cultivation** `onLivingHurt` 扩展 `instanceof LivingEntity` + 怪物 vanilla fallback（须先修 H9）
- [ ] **S-CombatStats 接灵根/体质/功法** 追加灵根纯度→攻击%/暴击、SpecialPhysique→防御/血、攻击型 technique→倍率
- [ ] **S-MysticVial 绑定** 首次 `use` 绑 owner，入口校验 `isOwner`，死亡 `inventory.add` 归还，`FurnaceFuelBurnTimeEvent` 阻熔炼
- [ ] **S-CultivationStatsScreen 滚动** 加 `scrollOffset`+`mouseScrolled`+clip
- [ ] **S-7 释放键 KeyConflictContext** 五参构造设 `IN_GAME`
- [x] **S-飞行 respawn/dim/死亡清理** 随 H11 落地
- [x] **S-死代码** 删 `ModEvents.java:446-464` showMeditationStatus/describeSpiritLand + 未用 import
- [ ] **S-炼丹闭环** H13 + 炼丹技能 XP/客户端 ticker/粒子/进度同步/废丹用途
- [x] **S-技能图标** 每 SkillType/techniqueId 制 16×16 PNG 放 `assets/.../textures/gui/skill/<id>.png` — 0.1.58 落地：`scripts/generate_skill_icons.py` 生成 15 个 16×16 占位 PNG（按属性配色+符号）；`ImmortalUiSkin.drawSkillIcon`/`hasSkillIcon` + 缓存 ResourceLocation；`TechniqueSkillBarOverlay` 先 blit 图标、未知 id 回退 hashCode+首字母；程序化占位美术，正式像素美术后续同名替换
- [ ] **D1/D2/D3/D5** 飞行统一/高度/走火/丹药 boost 设计定调（随对应 bug 落地）
- [ ] **D6/D7/D8** 炼丹品质/丹方匹配/概率归一化（顺带修 D8 `if(exp+suc>1.0) suc=1.0-exp` 归一化 bug）
- [ ] **D9** estimateCost/realm 随 M10 定调（JSON 显式字段）— ✅ 已落地
- [ ] **D10** canExecute 契约：覆盖 execute 所有失败条件（audit 各 spell 迁移失败条件到 canExecute）
- [ ] **D11** 隐身/轻身/土墙补 techniqueId+JSON entry 数据驱动解锁
- [x] **D12** CLAUDE.md 协议版本更新为实际值 + "packet 字段变更须 bump 并同步 CLAUDE.md" 规则（随 M12 bump 4→5）
- [ ] **D13** pendingMeditating 乐观锁：`SetMeditatingPacket.handle` 失败/状态不变时强制回 `SyncCultivationDataPacket` 兜底
- [ ] **D14** 境界数值表单一真源：Realm 枚举合并 manaBase（复用 baseMaxSpiritualPower）/divSense（新增字段），删 RealmStageConfig 重复表 + PlayerCultivation 内联 switch

---

## 协议版本影响汇总

| 条目 | 协议 | 说明 |
|---|---|---|
| M12 | **bump 4→5** | 新增 `cultivationMax` 字段，已同步 encode/decode/Snapshot/HUD/ModNetwork |
| M11 | — | 与 M12 同批，不额外 bump |
| 其余全部 | — | 服务端逻辑 / 客户端逻辑 / JSON / NBT 改动，不改 packet 字段 |

> M9 需旧存档冷却 map 迁移（NBT，非协议）；M10 改 JSON+record（非协议）；H10 纯客户端（非协议）。

## 完成统计

- 第一批：`[x] 6/6`（全部落地）
- 第二批：`[x] 7/15`（H6/H7/H9/M1/M2/M3/M13 已落地）
- 第三批：`[x] 6/8`（M8/M9/M10/M12/H10/M4 已落地）
- **本期已落地：19 项**
- 第四批：`[ ] 0/22+`（按 Phase 排期，非本期）
- **本期可落地：30 项**（第一+二+三批）

---

**文档版本**：v1.0
**推荐方案来源**：`docs/code-review-fix-alternatives.md`（3 路 agent 实读源码核实）
**未执行**：未改任何源码、未 commit、未运行 `gradlew build`（纯清单）

## 0.1.80 Review Fixes

- [x] Core Formation tribulation failure clears gold-core grade/score through `PlayerCultivation.clearGoldCore()`.
- [x] Nascent Soul and higher tribulation failure keeps an existing gold core.
- [x] `SyncLearnedTechniquesPacket`, `SetTechniqueSlotPacket`, and `SyncCultivationDataPacket` now use explicit `readUtf` limits.
- [x] Learned-technique, slot, and cooldown list counts are bounded during decode.
- [x] Seven Mysteries marker block interaction only intercepts matching quest stages.
- [x] Legacy `src/main/resources/assets/xiuxian/` namespace resources were backed up and removed.
- [x] `ModEvents` unused `showMeditationStatus` and `describeSpiritLand` were removed.
- [x] `ModNetwork.PROTOCOL_VERSION` remains `7`; packet fields, order, and types were not changed.
- [x] Verification passed: packet/tribulation tests, `xiuxian:` resource-reference check, and final Gradle build.
