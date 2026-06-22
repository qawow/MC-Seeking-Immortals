# 代码审查双方案备选（0.1.47 工作区）

> - **生成日期**：2026-06-22
> - **配套报告**：`docs/code-review-report.md`（v1.0）
> - **配套原方案**：`docs/code-review-fix-plan.md`（v1.0，每 bug 单方案）
> - **本文档性质**：为每个 bug 提供 **方案 A + 方案 B 两份独立方案**，并标注推荐项。方案 A 多为原方案的实读核实版，方案 B 为基于当前源码重新核实的备选路径。所有方案均经三个并行 agent 实读当前工作树源码核实可行性。
> - **当前核实基线**：`gradle.properties` 为 `mod_version=0.1.47`，`ModNetwork.PROTOCOL_VERSION` 为 `"4"`；若落地 M12，目标协议为 `4→5`。
> - **行号保鲜期**：本文行号基于 2026-06-22 的 0.1.47 工作区快照；后续编码前须先用搜索/打开源码复核行号是否漂移。
> - **落地规则**：实际编码须先按仓库代理规则文档（`AGENTS.md`/`CLAUDE.md`，以当前工作区实际存在者为准）在 `.bak/<timestamp>/` 备份，改完跑 `./gradlew build` 并更新 `project_docs/step_progress.md`；涉及 packet 字段变更须 bump `ModNetwork.PROTOCOL_VERSION` 并同步代理规则文档。
> - **单一真相**：每条 bug 自己的 **推荐** 行是最终决策源；优先级节只排落地顺序，差异速查表只作索引，不得与正文推荐行手写分叉。
> - **图例**：✅ = 推荐且低风险；✅⚠️ = 推荐但有取舍；⚠️ = 可行备选或条件推荐；❌ = 经核实不可行（仅作记录，不落地）。

## 执行摘要

- **第一批止血**：H2/H11/H1、H3、H4/H5、H12。H12 是高危项，须进入落地顺序，避免按批次执行时漏掉。
- **协议影响**：仅 M12 新增 packet 字段，需 bump `PROTOCOL_VERSION` `4→5`；M11 可与 M12 同批，不额外 bump。
- **H13 默认策略**：本期若不排炼丹闭环，默认走方案 B（显式 TODO，不假装修复）；若同批排炼丹闭环，则走方案 A 并同时补 ALCHEMY 解锁/XP 入口。
- **维护方式**：修改某条推荐时，先改正文推荐行，再机械同步优先级节与差异速查表；落地顺序节不重复方案符号，降低三处维护漂移。
- **验收补充**：每个编码批次除 `./gradlew build` 外，应补对应手测点；涉及飞行、网络同步、旧存档迁移、炼丹炉服务端 ticker 的条目，需分别做场景验证。

## 目录

- [高危 H1-H13](#-高危-h1h13)
- [中危 M1-M16](#-中危-m1m16)
- [未实现 stub](#-未实现-stub双方案精选)
- [未决设计点](#-未决设计点双方案--推荐)
- [优先级与落地顺序](#-优先级与落地顺序按正文推荐行执行)
- [推荐方案与原方案差异速查](#-推荐方案与原方案差异速查)

---

## 🔴 高危 H1–H13

### H1 两套飞行各自管 mayfly 互不感知 → 永久飞行
- **位置**：`event/ModEvents.java:545-586`（grantFlying/revokeFlying）、`:471-536`（handleFlyingArtifact/handleQiFlying）；`skill/effect/spell/FlyingSwordBeginnerSpell.java:32-57`
- **根因**：御剑术与法宝飞行各自维护独立 `previousMayfly` 快照，grant 时把"可能已被对方置 true"的 mayfly 当原始基准采样，revoke 时还原成 true → 残留。**关键核实**：agent 确认 revoke 阶段也存在覆盖竞态（御剑 stop 把 mayfly=false 覆盖了仍生效的法宝 grant），所以仅修 grant 采样不够。

- ✅ **方案 A（引用计数，原方案）**：新建 `FlyingAuthority`，单一 mayfly 基线 + 来源集合。grant 时 add 源（首源才采样基线），revoke 时 remove 源（仅当集合空才真还原 mayfly）。两处调用点迁移。
  - 风险：需同时改 grant 与 revoke 两路径；与 H11 respawn/dim 钩子配合时清 source 集合。
  - 优点：标准模式，能正确区分"曾创造模式"基准。

- ⚠️ **方案 B（轮询重算，无基线）**：删两套 PREVIOUS 快照，每 tick 由 `mayfly = artifactActive || swordActive` 直接重算。两个 handler 本就 tick 驱动（`onPlayerTick:165-166`），天然适配。创造/旁观靠既有 early-return（`:473,:515`）保护。
  - 风险：丢失"还原原创造模式 mayfly"能力——但创造/旁观已早退，不影响；非创造玩家飞行源全关时 mayfly=false 符合预期。
  - 优点：**彻底消除 grant+revoke 双向覆盖竞态**，代码量最小，无需新类。
  - 核实结论：agent 判定 FEASIBLE 且更干净地避免竞态。

- **推荐**：✅⚠️ **方案 B**。当前两套系统都是 tick 驱动，轮询重算比引用计数更难写错，且直接根除竞态。若后续要支持"飞行结束后恢复玩家原 mayfly 状态"（如别的 mod 授予的临时飞行），再回退到方案 A。

### H2 飞行高度上限方向反了
- **位置**：`event/ModEvents.java:484`
- **核实**：当前代码 `if (player.getY() > player.level().getMinBuildHeight() + profile.maxHeight())`，`maxHeight()` 是**相对 minBuildHeight 的偏移**（筑基 96 → 主世界阈值 Y=32）。agent 指出：若设计意图是"绝对 Y 天花板"则 `+ getMinBuildHeight()` 就是 bug；若是"相对偏移"则当前写法语义自洽但数值偏低（筑基在主世界 Y=33 即被打落）。

- ✅ **方案 A（绝对高度，原方案）**：改 `player.getY() > profile.maxHeight()`，删 `getMinBuildHeight()`。筑基 Y=95 不掉、97 掉。
  - 风险：下界基岩顶 127，96 偏低可接受；真仙 512 超主世界 320 无副作用。

- ⚠️ **方案 B（保留相对语义但修正方向 + 提高数值）**：若设计就是"相对世界底部上升 N 格"，保留 `+ getMinBuildHeight()` 但把 `maxHeight()` 数值表调大（如筑基 160 → 主世界 Y=96），并在下界用 `level.getHeight()` 自适应。
  - 风险：需改 `FlightProfile.forRealm` 数值表 + 文档定调（见 D2）。
  - 核实结论：agent 提示需先确认设计语义再选。

- **推荐**：✅ **方案 A**。报告与原方案均按"最大飞行高度 = 绝对 Y"理解，且改动最局部。D2 未决设计点可后续叠加软限制。

### H3 MysticVial isValidBonemealTarget 传 null → NPE/静默失败
- **位置**：`item/MysticVialItem.java:99`
- **核实**：`growable.isValidBonemealTarget(null, null, state, false)` 传 null level/pos，SaplingBlock/MushroomBlock 等会解引用 NPE。`useOn` 内确有 level+pos 可用。

- ✅ **方案 A（传实参，原方案）**：`isGrowTarget` 改签名传入 level+pos，`useOn` 直接传。保留 CropBlock 快速路径 + 通用 `BonemealableBlock` 全覆盖。
  - 风险：无回归（clientSide return 之后调用，serverLevel 安全）。

- ⚠️ **方案 B（instanceof 白名单）**：不调 `isValidBonemealTarget` API，改 `instanceof CropBlock/SaplingBlock/MushroomBlock` 显式分支，从根上避开 null-NPE 面。
  - 风险：覆盖面窄于原 API（不支持杜鹃花、粉瓣花、modded 作物），需随版本维护白名单。
  - 核实结论：agent 判定对 NPE 更安全但功能更窄。

- **推荐**：✅ **方案 A**。一行传实参即根治且不丢功能；白名单是为"不信任 API"而设计，此处无必要。

### H4 未注册/执行失败的 effect 仍扣灵力进冷却 + 提示成功
- **位置**：`network/ReleaseTechniquePacket.java:84-148`（`:121-128` 扣费兜底）
- **核实**：`SkillEffectRegistry.byDisplayName(technique.name())` 对 VINE_BIND/METAL_BLADE/SOUL_SEARCH/FIVE_ELEMENT_ROTATION/SWORD_FORMATION 返回 null → 整块跳过 → `effectExecuted=false` → line 121 无条件扣费、line 127 设冷却、line 137-148 仍发"成功"。

- ✅ **方案 A（拒绝释放，原方案）**：effect/skill 缺失即 early return + "无法施展"提示，不扣费不冷却；删除 line 121 无 effect 也扣费的兜底；仅 `effectExecuted=true` 才 setCooldown + 成功提示。新增 lang key `...technique_release.effect_unavailable`，`effect==null` 路径加 `LOGGER.warn`。
  - 风险：确认 `technique.name()` 与 `SkillType.getDisplayName()` 一致；包结构未变，**无需 bump**。

- ⚠️ **方案 B（注册占位 SkillEffect 拒绝释放）**：为 5 个未实现技能各注册一个 `UnavailableSkillEffect`，其 `canExecute` 恒 false、`execute` 恒 false。packet 既有 canExecute=false 分支（`:91-96`）会自然走"effect_failed"提示且不扣费。
  - 风险：仍需清理 line 121 兜底（否则 effectExecuted=false 仍扣费）；多注册 5 个空类；"effect_failed" 语义不如"未实现"清晰。
  - 核实结论：可行但比方案 A 多写类、且不解决 line 121 兜底。

- **推荐**：✅ **方案 A**。直接在 packet 层把"无 effect = 拒绝"作为契约，零占位类，顺手修 line 121 兜底。

### H5 execute 返回 false 时已扣灵力且不退费
- **位置**：`network/ReleaseTechniquePacket.java:97-114`
- **核实**：`consumeSpiritualPower(cost)` 在 `execute()` 之前；execute 返回 false（土遁无落脚点）只发消息 + return，无退费。

- ✅ **方案 A（execute-first，原方案）**：先 `canExecute` + `execute`，成功后再 `consumeSpiritualPower`；execute 前只检查不扣。需 audit 各 spell execute 失败路径无副作用（原方案已确认 Fireball/IceCone/Sword/EarthEscape/EarthWall/Invisibility/LightBody/FlyingSword/Detection 失败前不改世界）。
  - 风险：spell 内部仍有重复灵力检查（无害）；长期统一见 D10。

- ⚠️ **方案 B（扣费 + 失败退费）**：保持现序（先扣后执行），execute 返回 false 时 `addSpiritualPower(cost)` 退费 + 不设冷却。
  - 风险：退费路径需处理"扣费时因灵力不足返回 false"与"execute 失败"两种情况，分支更绕；addSpiritualPower 受 max 钳制，边界 max 附近退费可能少退。
  - 核实结论：可行但不如方案 A 干净。

- **推荐**：✅ **方案 A**。execute-first 是更正确的契约（失败不产生任何副作用），且与 D10（canExecute 覆盖所有失败条件）一致。

### H6 凡人灵力上限 0 → 灵力失效 + 误清重伤
- **位置**：`cultivation/PlayerCultivation.java:670-672`、`:304-308`、`:742-747`；`cultivation/Realm.java:5`（MORTAL baseMaxSpiritualPower=0）
- **核实**：MORTAL=0 → `getMaxSpiritualPower()=0` → `addSpiritualPower` 钳 0；`clearSevereInjuryIfRecovered` 判 `0>=0=true` 误清。agent 补充：`RealmStageConfig` 的 MORTAL manaBase/hpBase/recovery 也都是 0（独立表），且 Realm 注释明确"凡人灵力上限 0"是设计意图。

- ✅ **方案 A（最小下限 + 解耦重伤门，原方案）**：`getMaxSpiritualPower()` 对 MORTAL `Math.max(50, raw)`；`clearSevereInjuryIfRecovered` 改为依赖独立恢复阈值而非"灵力满"。
  - 风险：凡人突破到炼气 max 跳 100，`Math.min` 不回退灵力，安全。

- ❌ **方案 B（改数据：Realm.MORTAL=50）**：直接把枚举 `baseMaxSpiritualPower` 改 50。
  - 核实结论：agent 判定 **NOT FEASIBLE**——(1) `RealmStageConfig` 的 MORTAL mana/hp/recovery 仍为 0，单一改动覆盖不全；(2) 违背"凡人灵力 0"的设计注释。仅作记录。

- **推荐**：✅ **方案 A**。方案 B 经核实不可行（覆盖不全 + 违设计）。凡人灵力系统失效用最小下限修复，重伤门解耦更稳健。

### H7 走火风险=100 仅 0.50 触发，与"100% 当场死亡"矛盾
- **位置**：`cultivation/PlayerCultivation.java:991-995`（checkQiDeviation）；`MAX_QI_DEVIATION_RISK=100`（:25）；`determineQiDeviationTier` risk>=100 → EXTREME（:980）
- **核实**：`chance = min(0.50, max(0.20, (risk-50)/100))`，risk=100 → 0.50。

- ✅ **方案 A（入口特判，原方案）**：`if (qiDeviationRisk >= MAX_QI_DEVIATION_RISK) return true;`，其余区间保留线性公式。
  - 风险：几乎无；`>=100` 等价 `==100`（clamp 上限）。

- ⚠️ **方案 B（重写公式）**：改 `min(1.0, max(0.20, (risk-50)/50))`，risk=100 自然 → 1.0，无特判。
  - 风险：agent 核实——会改变 **所有** risk 区间概率曲线（70→0.40 vs 现 0.20；90→0.80 vs 现 0.40），是玩法平衡变更，超出 bug 修复范围。
  - 核实结论：FEASIBLE 但属平衡改动。

- **推荐**：✅ **方案 A**。最小行为变更，只修"100% 必触发"这一 bug，不动 70–99 曲线。方案 B 若策划想要更陡曲线可单独评估。

### H8 EXTREME 走火掉落 100% 而非 50%
- **位置**：`cultivation/BreakthroughService.java:135-140`（EXTREME）、`:164-180`（dropHalfInventory）
- **核实**：EXTREME 先 `dropHalfInventory`(50%) + 清对应槽 → `hurt(magic, Float.MAX_VALUE)` 死亡 → keepInventory=false 服 vanilla `dropAll` 再掉剩余 50% → 100%。agent 确认类注释（`:89-95`）明确 EXTREME = 当场死亡 + 掉 50%，**死亡是设计意图**。

- ✅ **方案 A（保存剩余 50% + respawn 归还，原方案）**：死亡前把剩余 50% 序列化到 PersistentData ListTag → 清空背包 → `hurt` 死亡（vanilla 无物可掉）→ `PlayerRespawnEvent` 归还。keepInventory=true 服行为一致。
  - 风险：归还只在 EXTREME 死亡后触发一次（用完即删 key）；需 `persistStackList`/`restoreStackList` helper。

- ❌ **方案 B（非致命极端惩罚）**：不杀玩家，改掉 50% + 大幅走火风险 + 临时 debuff，避开 keepInventory 交互。
  - 核实结论：agent 判定 **NOT FEASIBLE**——违背设计文档（EXTREME 明确 = 死亡，是 MINOR/MODERATE/SEVERE 非致命阶梯之上的顶点）。仅作记录。

- **推荐**：✅ **方案 A**。方案 B 违设计。保留死亡、修正掉落比例是正解。

### H9 飞剑/冰锥弹射物 hurt 触发 LivingHurtEvent 被 PvP 分支二次重算
- **位置**：`event/ModEvents.java:175-219`；`entity/SwordProjectileEntity.java:63-73`
- **核实**：`onLivingHurt` 三块：`:188-192` SeekingImmortalsCustomDamage 改写（对任意 source）、`:194-203` outgoing multiplier（sourceEntity 是玩家即乘，飞剑 owner 是玩家 → 重复加成）、`:205-218` PvP CombatCalculator（可能 cancel 吞飞剑）。agent 确认 `source.isIndirect()` 对飞剑为 true（getEntity=owner, getDirectEntity=弹射物）。

- ✅ **方案 A（弹射物 NBT 标记守卫，原方案）**：SwordProjectileEntity 构造时打 `SeekingImmortalsProjectileDamage` NBT，`onLivingHurt` 顶部若 directEntity 含该 key 跳过 `:194-218`，仅留打坐中断。守卫针对 `SwordProjectileEntity instanceof`，不动火球（火球靠 `:188-192` NBT 改写需保留）。
  - 风险：需确认飞剑 PvE 是否已含 multiplier（读 calculateDamage）。

- ⚠️ **方案 B（跳过所有间接来源）**：`onLivingHurt` 中 `:194-218` 两块整体加 `!source.isIndirect()` 守卫，所有弹射物绕过 cultivation multiplier + PvP 计算。
  - 风险：agent 核实——会连带跳过火球等**所有**远程的 `:199-201` SeekingImmortalsDamageMultiplier NBT 路径与 PvP miss/dodge，可能过宽（其他远程武器也不再 scale）。
  - 核实结论：FEASIBLE 但可能过宽。

- **推荐**：✅ **方案 A**。精准针对飞剑，保留火球的 `:188-192` 改写与其他远程的既有行为。方案 B 过宽风险高。

### H10 ClientTechniqueData.normalizeSlots 覆盖服务端 slots → 清空手动绑定
- **位置**：`client/ClientTechniqueData.java:37-53, 96-104`
- **核实**：agent 确认 `SyncLearnedTechniquesPacket` **端到端携带 7 槽**（`from():29` 取 `getTechniqueSlots()`，`encode/decode/handle` 全程传递），唯一"伪造默认"的是客户端自己：`setLearnedTechniques` 调 `setTechniqueData(techniques, defaultSlots(techniques), Map.of())` 覆盖，`normalizeSlots` 空时返回 `defaultSlots(learned)`。

- ✅ **方案 A（保留现有绑定，原方案）**：`setLearnedTechniques` 仅更新 learned 并 `retainValidSlots`（剔除已遗忘）；`normalizeSlots` 空时返回基于现有 `techniqueSlots` 的清理结果而非 defaultSlots。
  - 风险：`reset()` 后首次同步若服务端 slots 为空玩家看到全空——预期（登录应发真实 slots）。

- ⚠️ **方案 B（客户端永不伪造默认）**：更激进——`setLearnedTechniques` 只存 learned 列表、**完全不碰** `techniqueSlots`；`normalizeSlots` 永远返回当前 slots 的清理结果；默认填充完全依赖 packet 携带的 slots。
  - 风险：首次同步前（packet 未到）slots 为空而非自动填——可接受，更正确。
  - 核实结论：agent 判定 FEASIBLE，因 packet 必带 slots，"永不伪造"更干净。

- **推荐**：✅⚠️ **方案 B**。既然 packet 端到端带 slots，客户端就不该有"伪造默认"路径，方案 B 从根上消除覆盖源，比方案 A 的"保留+清理"更彻底。两者均无需 bump 协议。

### H11 飞行无生命周期清理 + grant 不校验灵力
- **位置**：`event/ModEvents.java:545-568`、`:512-536`；`skill/effect/spell/FlyingSwordBeginnerSpell.java:28`
- **核实**：agent 确认 `onPlayerTick:165-166` 每 tick 调两个飞行 handler，**轮询清理可行**——存 `dimension()` 每 tick 比对可检测换维；`grantFlying`/`handleQiFlying` 已在 tick 内调 `consumeSpiritualPower`（`:491,:529`），灵力门是一行 pre-check。

- ✅ **方案 A（事件钩子，原方案）**：新增 `PlayerRespawnEvent`/`PlayerChangedDimensionEvent`/`LivingDeathEvent` 调统一 `resetFlyingState` 清键 + 强制 mayfly=false；`grantFlying` 入口加灵力门。
  - 风险：`LivingDeathEvent` 需 isClientSide 守卫；创意模式早退；与 H1 引用计数配合时清 source 集合。

- ⚠️ **方案 B（轮询清理，无新事件）**：在既有 `onPlayerTick` 飞行处理内检测换维（比对上次 dimension）+ 死亡（`isDeadOrDying`）→ revoke；灵力门加在 grant 入口。
  - 风险：agent 提示死亡后 tick 可能不触发直到 respawn，死亡态清理更宜在 respawn-tick 通过 dimension/health 重置检测；逻辑比事件钩子绕。
  - 核实结论：FEASIBLE，但死亡检测时机不如事件可靠。

- **推荐**：✅ **方案 A**。事件钩子语义清晰、时机可靠（respawn/dim 钩子是 Forge 为此设计）。方案 B 的死亡检测时机有坑。若与 H1 方案 B（轮询重算 mayfly）合并，可简化为"换维/死亡时清 active 标志 + 下一 tick 自然 mayfly=false"。

### H12 AlchemyFurnace progressTicks 清零不执行 finishCraft
- **位置**：`block/entity/AlchemyFurnaceBlockEntity.java:32-39`
- **核实**：agent 确认 `AlchemyFurnaceBlock.getTicker:49-50` 客户端返回 null，`serverTick` 仅服务端调用 → `level instanceof ServerLevel` 守卫**恒为真**，属"防御缺失型潜在 bug"，运行时不触发但脆弱。

- ✅ **方案 A（守卫上提，原方案）**：非 ServerLevel 函数顶 return；仅 ServerLevel 分支内递减 + finishCraft。
  - 风险：几乎无（getTicker 已保证仅服务端）。

- ⚠️ **方案 B（删除死守卫）**：因守卫恒真，直接删 `instanceof` 判断，`finishCraft(level)` 直接调（或改 `finishCraft` 签名接 `ServerLevel` 由调用方 cast）。
  - 风险：agent 核实 getTicker 客户端返 null 确保不可达，删守卫安全；但 `finishCraft` 取 `ServerLevel`，需调用方 cast 或改签名。
  - 核实结论：FEASIBLE，更简单。

- **推荐**：✅ **方案 A**。虽然当前 `getTicker` 已让客户端不可达，但方块实体 ticker 后续容易被调整；守卫上提更稳，且仍保持代码清晰。若选择方案 B，必须在未来修改 `getTicker` 时同步复查该假设。

### H13 AlchemyRecipeService.getAlchemySkillBonus 硬编码 0 → 炼丹技能无效
- **位置**：`alchemy/AlchemyRecipeService.java:89-91`
- **核实**：agent 确认 **ALCHEMY 是死技能**——`SkillType.ALCHEMY` 用短构造器（techniqueId=""）→ `isPhase4QiSkill()` false → 不被 `unlockEligiblePhase4Skills` 自动解锁；全 src 无 `addSkillExperience(ALCHEMY)` / `unlockSkill(ALCHEMY)` 调用，无功法书/命令/炉子 XP 授予。即原方案"+0.02/level"落地后运行时仍是 0（技能永未解锁）。

- ⚠️ **方案 A（接通等级读取，原方案）**：`getAlchemySkillBonus` 经 `CultivationHelper.get` 读 `SkillType.ALCHEMY` 的 level，未解锁视 0，每级 +0.02（满级 +0.20）。
  - 风险：agent 核实——**当前运行时无 unlock/XP 路径，落地后 bonus 恒 0**，属"未来就绪"代码；需配套补解锁入口（功法书/命令/炉子成功后 `addSkillExperience`）才真正生效。

- ⚠️ **方案 B（暂留 0，标注待 wiring）**：保持 `return 0.0` 但加 `// TODO: 接通 ALCHEMY 解锁/经验后启用`，避免落地"什么都不做"的读取。
  - 风险：炼丹技能仍无效，但显式标注而非假装实现。

- **推荐**：✅⚠️ **条件推荐**。本期若不排炼丹闭环，默认落地 **方案 B**（显式 TODO，不假装修复）；若同批排炼丹闭环，则落地 **方案 A + 配套解锁入口**，同时补 ALCHEMY 解锁/XP 路径（炉子成功 `addSkillExperience(ALCHEMY, xp)` + 功法书或命令解锁）。禁止只落地单纯等级读取。

---

## 🟡 中危 M1–M16

### M1 凝气丹 boost 取 max 不续期
- **位置**：`item/CultivationPillItem.java:29`；`cultivation/PlayerCultivation.java:792-795`
- **核实**：agent 确认 `addCultivationBoost` 两字段均 `Math.max`；NBT 是单一 `CultivationBoostTicks`(int)+`CultivationBoostMultiplier`(double) 对，无 schema 变更。`CultivationPillItem` 用固定 `BOOST_TICKS=72000`/`BOOST_MULTIPLIER=2.0`。

- ✅ **方案 A（续期 + 取较大 multiplier，原方案）**：ticks 改 `min(CAP, current+ticks)`（CAP=`2*BOOST_TICKS`），multiplier 仍 `max`；`use` 判断已生效给续期提示。
  - 风险：加 lang key；上限防滥用。

- ⚠️ **方案 B（替换不续期但给提示）**：保持 `max` 语义不变，但 `use` 时若已生效则**不消耗丹药**（返回 InteractionResult 不 shrink），并提示"已有更强 boost 生效"。
  - 风险：玩家无法主动续期，体验差；但彻底避免"浪费丹药"。
  - 核实结论：可行，但非玩家友好。

- **推荐**：✅ **方案 A**。续期是玩家正反馈，方案 B 仅止损。关联 D5。

### M2 回灵丹 qiValue 形同虚设
- **位置**：`item/QiRecoveryPillItem.java:25-26`
- **核实**：agent 确认 `getPillAbsorptionMultiplier()` 存在（`PlayerCultivation:494-496`）；`Math.max(ceil(maxSP*0.5), round(qiValue*absorption))` 中大境界 maxSP 大时 `ceil(maxSP*0.5)` 主导，qiValue 无关。

- ✅ **方案 A（qiValue 主导 + 10% 下限，原方案）**：`Math.max(round(qiValue*absorption), ceil(maxSP*0.1))`。
  - 风险：低境界 maxSP 小时 floor 可能 > qiValue 抬升下限，可接受。

- ⚠️ **方案 B（纯 qiValue*absorption 无下限）**：完全去掉百分比项，回复量 = `round(qiValue*absorption)`。
  - 风险：低阶丹 + 低资质可能回 0 或极小；无保底。

- **推荐**：✅ **方案 A**。保底防归零，qiValue 仍主导大境界。

### M3 走火风险衰减几乎不触发
- **位置**：`event/ModEvents.java:139-149`
- **核实**：靠 `tickCount % (INTERVAL*20) == 0`，且在 `if (tickCount%20!=0) return`（:118）之后，需 tickCount 是 20 与 14400 的公倍数（LCM=14400），窗口 1 tick，玩家加入时机任意基本错过。

- ✅ **方案 A（累计计数器，原方案）**：`PlayerCultivation` 新增 `qiDevDecayAccumulatorTicks`/`leylineQiDevDecayAccumulatorTicks`，每平稳打坐 tick 累加，达阈值 -1 清零；持久化 NBT；`ModEvents:139-149` 替换为 `cultivation.tickQiDeviationDecay(auraInfo.leyline())`。
  - 风险：累加器须持久化；只在"平稳打坐"累加（当前块已在 `isMeditating()` 且无打断的 else 分支）。

- ⚠️ **方案 B（放宽取模窗口）**：把 `%14400` 改为更短且与 `%20` 对齐的窗口（如 `%7200` 已是 20 倍数），并对齐到 `%20` 分支内而非之后。
  - 风险：仍依赖 tickCount 取模，玩家加入时机仍影响首触发；治标不治本。

- **推荐**：✅ **方案 A**。累计计数器语义正确、抗错过、可持久化，与 `tickCultivationBoost`/`heartDemonTriggerTicks` 模式一致。关联 D3。

### M4 中性灵石无被动加成（与 CLAUDE.md 冲突）
- **位置**：`event/ModEvents.java:395-400`（getMatchingPassiveBonus）
- **核实**：agent 关键澄清——**两层含义**：(a) 中性灵石物品（attribute==null）在 ModItems 中**从未注册**（所有灵石都带五行 attribute），故 `stone.getAttribute()==null` 分支是死代码；(b) 真实受影响的是**非五行灵根玩家**（风/雷/冰/暗等）持任何灵石，`:398` `!isFiveElement(requiredAttribute)` 直接返回 0。`SpiritStoneItem` 有 `getPassiveBonus()`/`getAttribute()`/`matchesAttribute()`。

- ✅ **方案 A（注册中性灵石 + 通用加成，原方案）**：注册中性灵石系列（attribute=null），`getMatchingPassiveBonus` 对 attribute==null 返回通用 `passiveBonus`；同时对非五行灵根放宽匹配。
  - 风险：需新增中性灵石注册 + 资源/lang/创造标签。

- ⚠️ **方案 B（仅放宽非五行灵根匹配，不注册中性石）**：不新增物品，改 `:398` 让非五行灵根玩家也能获得匹配灵石（或所有灵石）的 `passiveBonus`（可能半额）。修订 CLAUDE.md "中性灵石"措辞为"非五行灵根通用加成"。
  - 风险：与 CLAUDE.md 字面描述仍有出入，需文档同步。
  - 核实结论：agent 确认 `attribute==null` 物品不存在，方案 A 的该分支是死代码；真实 bug 在 `:398` 灵根门。

- **推荐**：✅⚠️ **方案 B**（若不打算新增中性灵石物品）。agent 核实中性灵石物品从未注册，方案 A 的 attribute==null 分支是死代码；真正要修的是 `:398` 对非五行灵根返回 0。若策划确实要中性灵石物品则选方案 A 并先定调 D4。

### M5 村民兑换吞掉正常交易
- **位置**：`event/ModEvents.java:222-266`（`:265` 无条件 setCanceled）
- **核实**：agent 确认 `setCanceled(true)` 在 `:265` 无条件执行——成功分支（258-259）、无灵石 else（261）、达上限分支（239）后都取消。

- ✅ **方案 A（仅成功时取消，原方案）**：`setCanceled` 移入实际兑换成功的 if 内；达上限/无灵石分支不取消放行 vanilla。
  - 风险：若设计要 shift 专用于兑换，修复后 shift 无灵石会开 vanilla GUI（更友好）。

- ⚠️ **方案 B（达上限也取消，仅无灵石放行）**：成功 + 达上限都取消，仅"无灵石"分支放行 vanilla GUI。
  - 风险：达上限玩家想用 vanilla 交易仍被挡；语义不如方案 A 一致。

- **推荐**：✅ **方案 A**。所有"未成功兑换"情况都应放行 vanilla，最一致。

### M6 灵石未 stacksTo(1)
- **位置**：`item/SpiritStoneItem.java:33`（`stacksTo(64)`）；`registry/ModItems.java:154`
- **核实**：agent 确认 `stacksTo(64)`，NBT 写整叠（`getStoredPower`/`consumeStoredPower` 用 `getOrCreateTag`）；`consumeStoredPower:127` **无 count 守卫**，而 `use:43`/`getAvailablePassiveBonus:139`/`getAbsorbPerSecond:144` 有 `getCount()==1` 守卫——任何新调用方传多叠都易腐坏。

- ✅ **方案 A（stacksTo(1)，原方案）**：改 `stacksTo(1)`，移除冗余运行时 count 判断。代价：背包占用增大。
  - 风险：村民兑换需 100 颗时占 100 格但仍可用。

- ⚠️ **方案 B（保持 64 + split(1) 前置）**：`consumeStoredPower` 入口 `if (count>1) { shrink(1); 对 split 出的单个操作 NBT }`，保留堆叠。
  - 风险：agent 核实可行且与既有 `getCount()==1` 守卫风格一致；但需 audit 所有 NBT 写入点。
  - 核实结论：FEASIBLE。

- **推荐**：✅⚠️ **方案 B**（若重视背包紧凑）。灵石是常用消耗品，stacksTo(1) 严重影响背包；方案 B 保堆叠且根治 NBT 腐坏。若团队倾向简单一致则选方案 A。关联村民兑换权衡。

### M7 土遁步可穿 4 格墙
- **位置**：`skill/effect/spell/EarthEscapeStepSpell.java:28`
- **核实**：agent 确认 `canStandAt:41-48` **只查目标落脚点**（脚/头空、下方实），**不查 origin→target 中间格**；循环 4.0→1.5。

- ✅ **方案 A（缩距 2.0 + 路径逐格检查 + isLoaded，原方案）**：最大 2 格（穿 1 格阻隔），中间固体格数 ≤1，加 `level.isLoaded(pos)`/`hasChunkAt`。
  - 风险：缩距影响体验，需确认设计（qi_refining_techniques.json:36 "穿越一格阻隔"）。

- ⚠️ **方案 B（仅缩距 2.0，无路径检查）**：只把循环上限改 2.0，不动 canStandAt。
  - 风险：agent 核实 **PARTIALLY**——2 格瞬移仍可跳过中间 1 格墙（canStandAt 只验终点），不能严格满足"1 格阻隔"。
  - 核实结论：cap-at-2.0 是部分缓解，非完整修复。

- **推荐**：✅ **方案 A**。方案 B 经核实不完整（canStandAt 不查中间格）。要真正防穿墙必须加路径检查。

### M8 DetectionSpell 同步扫描性能
- **位置**：`skill/effect/spell/DetectionSpell.java:36-54`
- **核实**：agent 确认扫描 `[-blockRange,blockRange]³`（blockRange≤16）经球面过滤约 18k 次 `getBlockState`+`getKey().getPath()` 字符串匹配，每匹配发 3 粒子**无上限**。主导成本：(a) 无上限粒子；(b) 每非空方块的字符串匹配。循环是普通 for（非 labeled），提前 break 需 labeled 或标志位。

- ✅ **方案 A（封顶匹配 + 缩范围 + 1 粒子 + isLoaded，原方案）**：`maxBlockMatches=32` 达上限 break；`blockRange=min(8, ceil(range/2))`，y 限 ±4；每匹配 1 粒子；`isLoaded(pos)` 跳过。
  - 风险：范围缩小影响玩法，需与设计对齐；粒子减少反馈变弱可在消息报总数。

- ⚠️ **方案 B（保留范围 + 封顶匹配 + isLoaded + 1 粒子）**：不缩范围，仅加匹配上限 32、isLoaded 跳过、每匹配 1 粒子。
  - 风险：agent 核实——**匹配上限是最大收益**（达上限可 labeled break 全部循环），isLoaded 是次要收益，范围缩小非必需。
  - 核实结论：FEASIBLE，保玩法。

- **推荐**：✅⚠️ **方案 B**。agent 核实匹配上限是主导收益，范围缩小非必需且伤玩法。方案 B 保探测范围同时根治卡顿。若仍卡再加方案 A 的范围收缩。

### M9 冷却用 per-level gameTime，跨维度不同步
- **位置**：`cultivation/PlayerCultivation.java:160-171`；`network/ReleaseTechniquePacket.java:55,127`
- **核实**：agent 确认冷却存 `long`（`Map<String,Long>`，NBT `putLong/getLong`）；`player.serverLevel().getGameTime()` 是 per-ServerLevel long。`server.overworld().getGameTime()` 是无界 long、跨维度一致、**零类型变更**。`MinecraftServer#getTickCount()` 返回 **int**（约 1.24 年溢出，非"3 年"），换它需 long→int 全链路改 + NBT 迁移。

- ✅ **方案 A（overworld().getGameTime()，原方案）**：改 `player.getServer().overworld().getGameTime()`，long 字段不变。
  - 风险：旧存档 NBT 冷却值是旧维度 gameTime 不可比 → 一次性过期或永久冷却；需 deserialize/首次登录清旧冷却 map 或加版本标记。客户端倒计时显示须同步时钟源。

- ❌ **方案 B（getTickCount()）**：用 `player.getServer().getTickCount()`（int）。
  - 核实结论：agent 判定 **PARTIALLY 且严格更差**——需 long→int 类型 churn + NBT 迁移 + 约 1.24 年溢出风险，收益与方案 A 相同。仅作记录。

- **推荐**：✅ **方案 A**。方案 B 经核实严格更差。overworld gameTime 是 drop-in 修复，重点在旧存档迁移。

### M10 消耗/境界靠文本关键字猜测
- **位置**：`network/ReleaseTechniquePacket.java:154-184`；`cultivation/TechniqueDataManager.java:162-176,206`
- **核实**：agent 关键澄清——`TechniqueEntry` record 仅 `(id,name,source,attribute,quality)`，无 cost/realm。`SkillType` **确有** `techniqueId`/`spiritualPowerCost`/`cooldownTicks`/`requiredRealm`，但**仅 9 个**技能用 8 参构造器设了这些（QI_GUIDING/FIREBALL/ICE_CONE/THUNDER/EARTH_ESCAPE/DETECTION/FLYING_SWORD/SINGLE_SWORD/THREE_TALENT）；JSON 功法文件覆盖远多于此。且**无 `byTechniqueId` 查找**，只有 `byDisplayName(name)`（按中文名匹配，脆弱）。即 SkillType 作真源只能覆盖 9 个，其余仍回落启发式。

- ✅ **方案 A（JSON 显式字段，原方案）**：`TechniqueEntry` 增 `int cost`/`Realm requiredRealm`；6 个 JSON 追加 `cost`/`required_realm`；`estimateCost`→`technique.cost()`、`estimateTechniqueRealm`→`technique.requiredRealm()`。
  - 风险：record 加字段破坏构造（仅 loadEntries 一处）；为 6 文件全条目补字段；`Realm` 导入 TechniqueDataManager 无循环依赖。

- ⚠️ **方案 B（SkillType 作 9 技能真源 + 其余回落）**：新增 `SkillEffectRegistry.byTechniqueId(id)` 查 SkillType，命中的 9 个读 `getConfiguredSpiritualPowerCost()`/`getRequiredRealm()`，未命中仍用启发式。
  - 风险：agent 核实——覆盖不全（仅 9/全部），其余仍启发式；`byDisplayName` 脆弱性依旧。
  - 核实结论：PARTIALLY，非完整替换。

- **推荐**：✅ **方案 A**。agent 核实 SkillType 仅覆盖 9 个技能，无法作全量真源；JSON 显式字段是唯一能统一覆盖所有功法的方案。关联 D9。

### M11 SyncCultivationDataPacket writeUtf 默认上限风险
- **位置**：`network/SyncCultivationDataPacket.java:163-168,185`
- **核实**：`spiritualRootAttributes` 等 4 字段用无界 `writeUtf(String)`（默认 32767），超长 encode 抛 `EncoderException` 整包失败 → 客户端永久 `!synced`。

- ✅ **方案 A（显式上限 + 截断，原方案）**：短文本 `writeUtf(s, 64)`，`spiritualRootAttributes` `writeUtf(s, 256)`，encode 前 `cap()` 截断。
  - 风险：截断丢尾部但 64/256 远超当前 displayName，实际不触发；字段顺序/类型未变，**无需 bump**。

- ⚠️ **方案 B（改用 writeCollection 传属性列表）**：`spiritualRootAttributes` 改为 `List<String>` 用 `writeCollection` 逐项 `writeUtf(s,64)`，避免单字符串拼接超长。
  - 风险：改字段结构 → **需 bump 协议**；改动大于方案 A。

- **推荐**：✅ **方案 A**。最小改动、不 bump、足够防御。方案 B 过度设计。

### M12 CultivationHudOverlay 修为进度条硬编码 0.35
- **位置**：`client/CultivationHudOverlay.java:44-49`
- **核实**：agent 确认 `getCultivationMax()`（`PlayerCultivation:81`→`getCurrentStageCapExp`）**纯粹是 realm+stage 的函数**，无服务端独占状态。但 `ClientCultivationData.Snapshot.realm/.stage` 是**中文显示名字符串**（"凡人"/"1层"），非枚举；客户端推导 max 需显示名→枚举反向映射，脆弱且本地化耦合。

- ✅ **方案 A（新增 cultivationMax 同步字段 + bump 协议，原方案）**：packet record 增 `long cultivationMax`，`from` 写 `getCultivationMax()`，encode/decode 配对 `writeLong/readLong`；`Snapshot` 增字段（`empty()` 默认 1L 防除零）；HUD 改真实比值；**bump PROTOCOL_VERSION 4→5**。
  - 风险：协议必须同步 bump（旧客户端连新服务端被拒绝，期望行为）；与 M11 同批。

- ⚠️ **方案 B（客户端从 realm+stage 推导 max）**：客户端镜像 `getCultivationMax` 逻辑，避免协议 bump。
  - 风险：agent 核实——需显示名→枚举反向映射（"1层"→LAYER_1 解析），脆弱、本地化耦合；阶段名解析易错。
  - 核实结论：FEASIBLE 但脆弱，不推荐。

- **推荐**：✅ **方案 A**。agent 核实方案 B 需脆弱的字符串反向映射；协议 bump 代价小（一个 writeLong/readLong），更干净。关联 D12（CLAUDE.md 协议版本同步）。

### M13 寿元耗尽 hurt(Float.MAX_VALUE) 误触打坐中断 + 走火检定
- **位置**：`event/ModEvents.java:645-648`；`:175-186`（onLivingHurt 打坐分支）
- **核实**：agent 关键澄清——`onLivingHurt:176-186` 打坐分支**按 `instanceof Player` 触发，无 source 过滤**；`player.kill()` 内部调 `hurt(outOfWorld, MAX_VALUE)`、`outOfWorld()` 源**都仍触发 LivingHurtEvent** → 仅换伤害源**不能**避免副作用，必须加 onLivingHurt 守卫。

- ✅ **方案 A（flag + 守卫，原方案）**：寿元死亡前设 `SeekingImmortalsLifespanDeath` PersistentData flag，`onLivingHurt` 顶部对该 flag early-return（跳过打坐中断与走火检定，仍允许伤害生效）；伤害源改 `outOfWorld()`。
  - 风险：flag 须在死亡后清除（onLivingHurt remove + tick 末兜底 remove）。

- ❌ **方案 B（仅换 kill()/outOfWorld 不加守卫）**：只把 `hurt(magic, MAX)` 换成 `player.kill()` 或 `outOfWorld()`，不加 flag/守卫。
  - 核实结论：agent 判定 **NOT FEASIBLE**——kill/outOfWorld 都仍触发 LivingHurtEvent，打坐分支按 Player 触发，副作用依旧。仅作记录。

- **推荐**：✅ **方案 A**。方案 B 经核实无效。必须 onLivingHurt 守卫 + flag。

### M14 lifespanYears 旧存档回退到 QI_REFINING(100)，凡人应 80
- **位置**：`cultivation/PlayerCultivation.java:1212`
- **核实**：agent 确认 `realm` 在 `:1202` `loadRealmAndStage` 或 `:1203` `updateRealmFromCultivationExp` 已设定，`:1212` 在其后执行，`realm` 已赋值；回退值硬编码 `Realm.QI_REFINING.getLifespanYears()`=100，MORTAL 应 80。

- ✅ **方案 A（回退 realm.getLifespanYears()，原方案）**：`lifespanYears = tag.contains("LifespanYears") ? tag.getInt("LifespanYears") : realm.getLifespanYears();`
  - 风险：无；`realm` 已确定，非凡人旧存档回退仍正确。

- （此条核实结论明确，无有意义方案 B；备选为"不修，接受 100"——不可取。）

- **推荐**：✅ **方案 A**。一行修复，agent 确认安全。

### M15 PillQuality 枚举不参与炼丹产物
- **位置**：`alchemy/AlchemyRecipe.java:14-32`；`block/entity/AlchemyFurnaceBlockEntity.finishCraft:94`
- **核实**：MVP 配方 id 形如 `foundation_building_pill_low`，`output` 直接指按品质分注册的物品；`finishCraft` `new ItemStack(recipe.output(), count)` 产物品质由配方写死，运行时无品质掷骰；`PillQuality` 只服务手动注册成品丹，与炉子产出脱钩 → 同方永出下品。

- ✅ **方案 A（output 改 List<Item> + 品质掷骰，原方案）**：`AlchemyRecipe.output` → `outputsByQuality`（index=PillQuality.ordinal()）；`finishCraft` 成功分支基于 successMargin + 炼丹技能等级掷骰；旧 `_low` id 用 `findById` 别名兜底兼容存档。
  - 风险：需先盘 ModItems 是否已注册 MEDIUM/HIGH/SUPREME 物品（若仅 `_LOW` 掷骰取 null）；存档 recipeId 别名映射；与 D6 同批。

- ⚠️ **方案 B（运行时 NBT 品质，单一物品）**：单注册一个无后缀物品，`finishCraft` 产出时 `ItemStack` 写 NBT `PillQuality`，tooltip/效果按 NBT 读。
  - 风险：与"每品质独立物品"注册侧不打通（创造栏/JEI/堆叠分不开）；需重写所有成品丹注册与 tooltip。

- **推荐**：✅ **方案 A**。保留"每品质独立物品"主方案（创造栏/JEI/贴图开箱即用），仅把炉子产出接入。关联 D6。

### M16 HealingPill/FastingPill/ClearSpiritPowder 未用 absorption
- **位置**：`item/pill/RejuvenationPill.java:13-31`（已用，基线）；`HealingPill.java`/`FastingPill.java`/`ClearSpiritPowder.java`（未用）
- **核实**：仅 `RejuvenationPill` 把 `getQuality().getEffectMultiplier()` 再乘 `getPillAbsorptionMultiplier(player)`；其余三子类直接用品质倍率。`BasePillItem.getPillAbsorptionMultiplier:49-53` 封装"低资质吸收率更高"。

- ✅ **方案 A（抽 effectiveMultiplier helper，原方案）**：`BasePillItem` 抽 `protected double effectiveMultiplier(player)` = 品质×吸收率；三子类改用之，`RejuvenationPill` 也改用消除重复；`ClearSpiritPowder`（布尔解毒）改吸收率≥阈值额外清 1 负面效果。
  - 风险：`getPillAbsorptionMultiplier` 无 capability 返回 1.0 不会清零；饱和度用 `Math.round` 防低吸收归零。

- ⚠️ **方案 B（仅给三子类乘吸收率，不抽 helper）**：每个子类内联 `* getPillAbsorptionMultiplier(player)`，不抽公共方法。
  - 风险：重复代码，与 `RejuvenationPill` 仍不一致；未来加丹仍易漏。

- **推荐**：✅ **方案 A**。抽 helper 统一"疗效=基础×品质×吸收率"，消除四子类不一致，防未来遗漏。

---

## ⚪ 未实现 stub（双方案精选）

> stub 多为"做 vs 不做"的选择，非双解法；此处对高优先 stub 给"实现路径 A / 止血路径 B"。

### S-EarthWall 永久地形（刷石漏洞）
- **位置**：`skill/effect/spell/EarthWallSpell.java:21-29`（`setBlock(wallPos, STONE, 3)` 永不移除）
- **核实**：agent 确认 vanilla `STONE` 无 `tick`/`randomTick` 移除自身，`scheduleBlockTick` 对纯 STONE **不会触发**。

- ✅ **方案 A（最小 EarthWallBlock + scheduleBlockTick）**：新建 `EarthWallBlock extends Block` 重写 `tick` 设 air，`setBlock` 时 `scheduleBlockTick(pos, this, 200)`；注册 block + blockstate/model/lang。
  - 风险：需注册新 block（必然，因 vanilla STONE 忽略 scheduled tick）。

- ⚠️ **方案 B（spell 自管位置列表 + 延迟任务移除）**：spell 内记录墙位置，靠技能/玩家 tick 倒计时后 `level.removeBlock`。无新 block。
  - 风险：需在 PlayerCultivation/ModEvents 维护"待移除墙"持久化列表（否则重启洗不掉），状态管理复杂。

- **推荐**：✅ **方案 A**。agent 核实"无新 block"方案都不可行（STONE 忽略 scheduled tick）；最小 `EarthWallBlock` 是必经路径，且比 BlockEntity 轻。

### S-未注册技能拒绝释放止血
- **位置**：`SkillType` VINE_BIND/METAL_BLADE/SOUL_SEARCH/FIVE_ELEMENT_ROTATION/SWORD_FORMATION
- ✅ **方案 A**：随 H4 方案 A 落地——packet 层"无 effect=拒绝释放"自动止血，无需为每个未实现技能写代码。
- ⚠️ **方案 B**：逐个实现 SkillEffect（工作量大，按 Phase 排期）。
- **推荐**：✅ **方案 A** 立即止血，**方案 B** 按 Phase 排期逐步实现。

### S-其他 stub（单方案，按 Phase 排期）
- 筑基 6 技能全 stub：逐个实现 SkillEffect（须先修 H4）。
- InvisibilitySpell/LightBodySpell 无打破隐身逻辑：`onLivingHurt`/`AttackEntityEvent` 玩家造成伤害时 `removeEffect(INVISIBILITY)`。
- 神识消耗未实现：`ReleaseTechniquePacket` 扣灵力后追加 `dcCost` 检查与 `consumeDivSense`（DETECTION/SOUL_SEARCH 返回非 0）。
- PvE 不经 cultivation：`onLivingHurt` 扩展 `instanceof LivingEntity`，怪物 stats 用 vanilla-derived fallback（须先修 H9）。
- CombatStats 未接灵根/体质/功法：追加灵根纯度→攻击%/暴击%、SpecialPhysique→防御/血、攻击型 technique→攻击倍率。
- MysticVialItem 绑定未生效：首次 `use` 绑定 owner，入口校验 `isOwner`，死亡 `inventory.add` 归还，`FurnaceFuelBurnTimeEvent` 阻熔炼。
- CultivationStatsScreen 无分页/滚动：加 `scrollOffset`+`mouseScrolled`+clip。
- 7 释放键无 KeyConflictContext：五参构造设 `IN_GAME`。
- 飞行 respawn/dimension/死亡清理：随 H11 落地。
- showMeditationStatus/describeSpiritLand 死代码：删除两方法及未用 import。

---

## ❓ 未决设计点（双方案 + 推荐）

> 每条给"方案 A / 方案 B"及推荐，解除"未决"。详细背景见 `code-review-fix-plan.md` D1–D14。

| # | 设计点 | 方案 A | 方案 B | 推荐 |
|---|---|---|---|---|
| D1 | 两套飞行统一 | 共存+引用计数（H1 方案 A） | 轮询重算（H1 方案 B） | ✅⚠️ B（与 H1 一致） |
| D2 | 飞行高度限制 | 硬上限修正方向（H2 A） | 软限制上升速度衰减 | ✅ A（先修 bug），软限制后续 |
| D3 | 走火风险衰减 | 累计计数器（M3 A） | tickCount 取模 | ✅ A |
| D4 | 中性灵石是否被动 | 注册中性石+通用加成（M4 A） | 仅放宽非五行灵根+修文档（M4 B） | ✅⚠️ B（核实中性石未注册） |
| D5 | 丹药 boost 叠加 | 续期+取较大 multiplier（M1 A） | 替换不续期但提示不消耗（M1 B） | ✅ A |
| D6 | 品质机制 | 每品质独立物品+outputsByQuality（M15 A） | 运行时 NBT 品质单物品（M15 B） | ✅ A |
| D7 | 丹方匹配 | 独有材料优先+库存完整满足过滤 | 候选列表消息/潜行右键第二候选 | ✅ A 短期，B 中期 |
| D8 | 爆炉/废丹/概率边界 | 投入即烧+爆炉不产废丹+概率归一化 | 失败保炉产废丹两级 | ✅ A + 归一化 bug 顺带修 |
| D9 | estimateCost/realm | JSON 显式字段（M10 A） | SkillType 作 9 技能真源（M10 B） | ✅ A（核实 B 覆盖不全） |
| D10 | canExecute 语义 | 契约：canExecute 覆盖所有失败条件 | 保持现状+execute-first（H5 A） | ✅ A（长期），H5 先行 |
| D11 | 隐身/轻身/土墙解锁时机 | 数据驱动补 techniqueId+JSON entry | 保持短构造器手动解锁 | ✅ A |
| D12 | PROTOCOL_VERSION 文档 | 更新 CLAUDE.md 为实际值+bump 规则 | 仅更新文档不立规则 | ✅ A |
| D13 | pendingMeditating 乐观锁 | 保留+失败时强制回包兜底 | 改服务端立即 ACK | ✅ A（低频场景） |
| D14 | 两套境界数值表 | Realm 枚举单一真源（合并 manaBase/divSense） | 保持双表+加注释 | ✅ A（agent 核实值已重复，机械合并） |

---

## 📍 优先级与落地顺序（按正文推荐行执行）

> 本节只负责排顺序；具体方案与取舍以各条目的 **推荐** 行为准，避免三处手写维护导致漂移。

**第一批（高确定性局部修复，防永久飞行/崩服/白扣灵力）：**
1. **H2 + H11 + H1** —— 防永久飞行；验收：法宝/御剑单独启停、双源切换、换维、死亡/重生后 mayfly 与飞行状态均正确清理。
2. **H3** —— 防崩服；验收：神秘小瓶对作物、树苗、蘑菇、不可催熟方块分别右键，服务端无 NPE。
3. **H4 + H5** —— 防白扣灵力；验收：未注册技能、canExecute 失败、execute 失败、正常成功四类释放分别检查灵力/冷却/提示。
4. **H12** —— 防炼丹炉 ticker 脆弱路径；验收：服务端炼丹炉 progress 到点后执行 `finishCraft`，客户端不跑 ticker，构建通过。

**第二批（玩法正确性 + 经济/世界完整性）：**
5. **H8**；**H6**；**H7**；**H13**（默认：若本期不排炼丹闭环则显式 TODO；若同批排炼丹闭环则接通 ALCHEMY 解锁/XP）
6. **H9**；**M7**；**S-EarthWall**
7. **M3/M13/M14**（走火衰减/寿元死亡/寿元回退）；**M1/M2**（丹药分级）；**M5**（村民交易）；**M15/M16**（品质/吸收率）

**第三批（数据驱动重构 + 同步链路）：**
8. **M10**（JSON 显式 cost/realm，改 schema + 6 文件）
9. **M9**（含旧存档迁移）；**M12**（cultivationMax 同步，**bump 协议 4→5**）；**M11**（与 M12 同批）；**H10**
10. **M8**；**M6**；**M4**

**第四批（stub/设计整改，按 Phase 排期）：**
11. S-未注册技能止血（随 H4）→ 逐个实现 SkillEffect
12. 炼丹闭环（H13 + S 炼丹技能/客户端 ticker/废丹）
13. **D6/D7/D8** 炼丹品质/丹方匹配/概率归一化（顺带修 D8 归一化 bug）
14. **D14** 境界数值表单一真源；**D10/D11** canExecute 契约/解锁时机；**D1/D2/D3/D5** 飞行/走火/丹药设计定调

**协议版本影响汇总**：多数为服务端/客户端逻辑，无需 bump；**M12**（新增 cultivationMax）须 bump `PROTOCOL_VERSION` 4→5 并同步代理规则文档（D12）；M11 与 M12 同批不额外 bump；M10/M9/H10 等改 JSON/NBT/客户端逻辑不 bump 协议（M9 需旧存档冷却迁移）。

---

## 📊 推荐方案与原方案差异速查

下表列出本文档**推荐方案与 `code-review-fix-plan.md` 原方案不同**的条目（经 agent 核实后调整）。本表只作速查索引；若与正文推荐行冲突，以正文为准，并同步修表。

| Bug | 原方案推荐 | 本文档推荐 | 调整理由（agent 核实） |
|---|---|---|---|
| H1 | 引用计数 | ✅⚠️ 轮询重算 | 两系统皆 tick 驱动，轮询更难写错且根除双向覆盖竞态 |
| H10 | 保留+清理 | ✅⚠️ 客户端永不伪造默认 | packet 端到端带 slots，伪造默认是唯一覆盖源 |
| H13 | 接通等级读取 | ✅⚠️ 条件推荐：本期不排炼丹闭环则 TODO；同批排炼丹闭环则 A+解锁/XP 入口 | ALCHEMY 是死技能，单接通读取运行时仍 0 |
| M4 | 注册中性灵石+通用加成 | ✅⚠️ 仅放宽非五行灵根 | 中性灵石物品从未注册，attribute==null 分支是死代码 |
| M6 | stacksTo(1) | ✅⚠️ split(1) 保堆叠 | 灵石常用消耗品，stacksTo(1) 伤背包；split 根治 NBT 腐坏 |
| M7 | 缩距+路径检查 | （同，方案 A） | 核实方案 B（仅缩距）不完整，维持方案 A |
| M8 | 缩范围+封顶+1 粒子 | ✅⚠️ 保范围+封顶+1 粒子 | 匹配上限是主导收益，缩范围伤玩法 |
| M9 | overworld gameTime | （同，方案 A） | 核实 getTickCount() 严格更差，维持方案 A |
| M10 | JSON 显式字段 | （同，方案 A） | 核实 SkillType 仅覆盖 9 技能，维持方案 A |
| M12 | 新增 cultivationMax+bump | （同，方案 A） | 核实客户端推导需脆弱字符串映射，维持方案 A |
| H6 | 最小下限+解耦 | （同，方案 A） | 核实改数据 MORTAL=50 不可行，维持方案 A |
| H8 | 保存+respawn 归还 | （同，方案 A） | 核实非致命违设计，维持方案 A |
| H3 | 传实参 | （同，方案 A） | 核实白名单更窄，维持方案 A |
| H9 | 飞剑 NBT 守卫 | （同，方案 A） | 核实跳过所有间接源过宽，维持方案 A |
| M13 | flag+守卫 | （同，方案 A） | 核实仅换 kill/outOfWorld 无效，维持方案 A |

其余条目（H2/H4/H5/H7/H12/M1/M2/M3/M5/M11/M14/M15/M16 及多数 D 点）推荐与原方案一致。

---

**文档版本**：v1.0
**生成方式**：3 路并行 agent 实读当前工作树源码核实每个备选方案可行性（flight / combat-client-alchemy / cultivation-core），再综合成双方案。
**未执行**：未改任何源码、未 commit、未运行 `gradlew build`（纯方案）。
