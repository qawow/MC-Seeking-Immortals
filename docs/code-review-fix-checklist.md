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

- [ ] **H8** EXTREME 掉落 100%→50% — `BreakthroughService.java:135-140` — 死亡前剩余 50% 序列化 PersistentData ListTag→清背包→`hurt` 死亡→`PlayerRespawnEvent` 归还（用完即删 key）；加 `persistStackList`/`restoreStackList` helper — 协议 `—` — 验证：keepInventory=false 服堆 risk=100 死后地上 50%、重生 50%；keepInventory=true 重生保留 50%
- [x] **H6** 凡人灵力上限 0 — `PlayerCultivation.java:670-672,304-308` — `getMaxSpiritualPower()` MORTAL `Math.max(50, raw)`；`clearSevereInjuryIfRecovered` 改独立恢复阈值 — 协议 `—` — 验证：新凡人打坐回复到 50；`/affliction severe_injury` 后凡人 HP 有 -80% 且不秒清
- [x] **H7** risk=100 仅 0.50 触发 — `PlayerCultivation.java:991-995` — 入口 `if (qiDeviationRisk >= MAX_QI_DEVIATION_RISK) return true;` — 协议 `—` — 验证：risk=100 100% 走 EXTREME 死亡
- [ ] **H13** 炼丹技能无效 — `AlchemyRecipeService.java:89-91` + 配套解锁入口 — `getAlchemySkillBonus` 读 `SkillType.ALCHEMY` level（+0.02/级）**且**补 ALCHEMY 解锁/XP 路径（炉子成功 `addSkillExperience(ALCHEMY,xp)` + 功法书/命令解锁）；否则暂留 0+TODO — 协议 `—` — 验证：LV1/LV10 炼丹各 20 炉统计成功率/爆炉率差异
- [x] **H9** 飞剑伤害二次重算 — `ModEvents.java:175-219` + `SwordProjectileEntity.java:63-73` — 弹射物构造打 `SeekingImmortalsProjectileDamage` NBT，`onLivingHurt` 顶部对 `SwordProjectileEntity instanceof` 跳过 `:194-218`，保留 `:188-192` 火球改写 — 协议 `—` — 验证：玩家 A 飞剑刺 B 伤害=生成时 calculateDamage，不被 dodge/miss 吞
- [ ] **M7** 土遁步穿 4 格墙 — `EarthEscapeStepSpell.java:28` — 缩距 2.0 + 路径逐格检查固体格≤1 + `level.isLoaded(pos)` — 协议 `—` — 验证：1 格厚墙穿过、2 格厚失败、边界区块不崩
- [ ] **S-EarthWall** 永久地形刷石 — `EarthWallSpell.java:21-29` — 新建 `EarthWallBlock extends Block` 重写 `tick` 设 air，`setBlock` + `scheduleBlockTick(pos, this, 200)`；注册 block+blockstate/model/lang — 协议 `—` — 验证：放土墙 10 秒后消失，不可刷石
- [x] **M3** 走火衰减几乎不触发 — `ModEvents.java:139-149` + `PlayerCultivation` — 新增 `qiDevDecayAccumulatorTicks`/`leylineQiDevDecayAccumulatorTicks`（持久化 NBT），平稳打坐 tick 累加达阈值 -1；`ModEvents:139-149` 替换为 `cultivation.tickQiDeviationDecay(leyline)` — 协议 `—` — 验证：堆 risk=50 平稳打坐 12 分钟稳定 -1
- [x] **M13** 寿元死亡误触走火 — `ModEvents.java:645-648,175-186` — 寿元死亡前设 `SeekingImmortalsLifespanDeath` flag + 改 `outOfWorld()`，`onLivingHurt` 顶部 flag early-return（用完即删+tick 末兜底删） — 协议 `—` — 验证：设寿元到上限并打坐，耗尽致死直接死无走火消息
- [ ] **M14** lifespanYears 回退 100 应 80 — `PlayerCultivation.java:1212` — 回退值改 `realm.getLifespanYears()` — 协议 `—` — 验证：无 LifespanYears 且 MORTAL 旧 NBT 加载后=80
- [x] **M1** 凝气丹 boost 不续期 — `PlayerCultivation.java:792-795` + `CultivationPillItem.java:29` — ticks 改 `min(2*BOOST_TICKS, current+ticks)`，multiplier 仍 `max`；`use` 已生效给续期提示 lang key — 协议 `—` — 验证：连服 3 颗时长累加且有续期提示
- [x] **M2** 回灵丹 qiValue 失效 — `QiRecoveryPillItem.java:25-26` — `Math.max(round(qiValue*absorption), ceil(maxSP*0.1))` — 协议 `—` — 验证：同境界低/中/高回灵丹回复量随 qiValue 递增
- [ ] **M5** 村民兑换吞正常交易 — `ModEvents.java:222-266` — `setCanceled` 移入实际兑换成功 if 内，达上限/无灵石分支放行 vanilla — 协议 `—` — 验证：有灵石兑换不开 GUI；无灵石/达上限开 vanilla GUI
- [ ] **M15** PillQuality 不参与产物 — `AlchemyRecipe.java:14-32` + `AlchemyFurnaceBlockEntity.finishCraft:94` — `output`→`outputsByQuality`(List<Item>，index=PillQuality.ordinal())，成功分支按 successMargin+技能等级掷骰；旧 `_low` id 别名兜底 — 协议 `—` — 验证：LV1/LV10 各炼 20 炉筑基丹品质分布差异；旧存档 recipeId 仍 findById
- [ ] **M16** 三丹未用 absorption — `HealingPill.java`/`FastingPill.java`/`ClearSpiritPowder.java` + `BasePillItem` — 抽 `protected effectiveMultiplier(player)`=品质×吸收率，三子类+RejuvenationPill 统一用之；ClearSpiritPowder 吸收率≥阈值额外清 1 负面 — 协议 `—` — 验证：高/低资质灵根服同品疗伤丹回血差异

## 第三批：数据驱动重构 + 同步链路（工作量较大，建议单独 PR）

- [x] **M10** 消耗/境界文本猜测 — `TechniqueDataManager.java:162-176,206` + 6 个 technique JSON — `TechniqueEntry` 增 `int cost`/`Realm requiredRealm`，JSON 加 `cost`/`required_realm`，`estimateCost`→`technique.cost()`、`estimateTechniqueRealm`→`technique.requiredRealm()` — 协议 `—`（改 JSON+record）— 验证：`/reload` 后火球扣费=10；化神功法练气期释放触发走火提示
- [x] **M9** 冷却跨维度不同步 — `PlayerCultivation.java:160-171` + `ReleaseTechniquePacket.java:55,127` — 改 `player.getServer().overworld().getGameTime()`；旧存档冷却 map 首次登录清空或加版本标记 — 协议 `—`（NBT 迁移）— 验证：overworld 释放→进 nether 同技能仍显示冷却中
- [x] **M12** HUD 进度条硬编码 0.35 — `SyncCultivationDataPacket.java` + `ClientCultivationData.Snapshot` + `CultivationHudOverlay.java:44-49` — record 增 `long cultivationMax`，`from`/encode/decode 配对 `writeLong/readLong`，`Snapshot` 增字段（`empty()` 默认 1L），HUD 改真实比值 — 协议 **`bump 4→5`** — 验证：修为条随 cultivation 增长真实填充；突破后阶段 cap 重置归零
- [ ] **M11** writeUtf 默认上限风险 — `SyncCultivationDataPacket.java:163-168,185` — 短文本 `writeUtf(s,64)`，`spiritualRootAttributes` `writeUtf(s,256)`，encode 前 `cap()` 截断 — 协议 `—`（与 M12 同批不额外 bump）— 验证：5 属性灵根同步 HUD 正常
- [x] **H10** 客户端清空手动绑定 — `ClientTechniqueData.java:37-53,96-104` — `setLearnedTechniques` 只存 learned 不碰 `techniqueSlots`；`normalizeSlots` 永远返回当前 slots 清理结果（不 defaultSlots） — 协议 `—` — 验证：学习/遗忘技法、重登后 7 槽绑定保留
- [x] **M8** DetectionSpell 性能 — `DetectionSpell.java:36-54` — `maxBlockMatches=32` 达上限 labeled break，`isLoaded(pos)` 跳过，每匹配 1 粒子（保范围） — 协议 `—` — 验证：多玩家同时释放用 spark 观察主线程占用
- [ ] **M6** 灵石未 stacksTo(1) — `SpiritStoneItem.java:33,127` — `consumeStoredPower` 入口 `if(count>1){ shrink(1); 对 split(1) 操作 NBT }`，保 stacksTo(64) — 协议 `—` — 验证：多颗灵石吸收/耗尽正常，NBT 不腐坏
- [x] **M4** 中性灵石无被动加成 — `ModEvents.java:395-400` — `:398` 放宽非五行灵根匹配（返回 `stone.getPassiveBonus()` 或半额）；修订 CLAUDE.md "中性灵石"措辞 — 协议 `—` — 验证：变异灵根玩家持五行石打坐 spiritualPower 加成生效

## 第四批：stub / 设计整改（按 Phase 排期）

- [ ] **S-未注册技能止血** 随 H4 落地自动止血（无 effect=拒绝释放）
- [ ] **S-筑基 6 技能** 逐个实现 SkillEffect（须先修 H4）— 按排期
- [ ] **S-Invisibility/LightBody 打破隐身** `onLivingHurt`/`AttackEntityEvent` 玩家造成伤害 `removeEffect(INVISIBILITY)`
- [ ] **S-神识消耗** `ReleaseTechniquePacket` 扣灵力后追加 `dcCost` 检查 + `consumeDivSense`（DETECTION/SOUL_SEARCH）
- [ ] **S-PvE 不经 cultivation** `onLivingHurt` 扩展 `instanceof LivingEntity` + 怪物 vanilla fallback（须先修 H9）
- [ ] **S-CombatStats 接灵根/体质/功法** 追加灵根纯度→攻击%/暴击、SpecialPhysique→防御/血、攻击型 technique→倍率
- [ ] **S-MysticVial 绑定** 首次 `use` 绑 owner，入口校验 `isOwner`，死亡 `inventory.add` 归还，`FurnaceFuelBurnTimeEvent` 阻熔炼
- [ ] **S-CultivationStatsScreen 滚动** 加 `scrollOffset`+`mouseScrolled`+clip
- [ ] **S-7 释放键 KeyConflictContext** 五参构造设 `IN_GAME`
- [ ] **S-飞行 respawn/dim/死亡清理** 随 H11 落地
- [ ] **S-死代码** 删 `ModEvents.java:446-464` showMeditationStatus/describeSpiritLand + 未用 import
- [ ] **S-炼丹闭环** H13 + 炼丹技能 XP/客户端 ticker/粒子/进度同步/废丹用途
- [ ] **S-技能图标** 每 SkillType/techniqueId 制 16×16 PNG 放 `assets/.../textures/gui/skill/<id>.png`
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
