# 代码审查报告（0.1.47 工作区）

> - **审查日期**：2026-06-20
> - **审查对象**：工作区未提交改动（28 已跟踪改动 + 36 未跟踪新增，Phase 3-8 产物）+ 整个代码库
> - **审查方式**：分 cultivation / skill+combat / alchemy / network+client / event+飞行+物品 五块并行深审，仅读未改
> - **构建状态**：本次未运行 `gradlew build`（纯审查）；各 phase 报告声明曾构建通过，但当前工作区未现场验证
> - **配套文档**：与 `docs/gap-audit-report.md`（功能/进度盘点）互补，本报告聚焦代码层面的 bug、未实现与未决定点

---

`gh` 不可用、无 PR，所以这次 review 基于工作区未提交改动（28 改 + 36 新增）和整个代码库，分 cultivation / skill+combat / alchemy / network+client / event+飞行+物品 五块并行深审。下面是综合报告。

---

## 🔴 高危问题（建议优先修）

| # | 位置 | 问题 | 后果 |
|---|---|---|---|
| 1 | `ModEvents.handleFlyingArtifact` + `FlyingSwordBeginnerSpell` + `handleQiFlying` | **两套飞行各自管 mayfly 互不感知**：`previousMayfly` 采样时机错，先开御剑再装法宝（或反之）会把"已被对方置 true"的 mayfly 当原始值保存，撤销时还原成 true | 玩家获得**永久飞行** |
| 2 | `ModEvents:484` | `player.getY() > maxHeight` 直接撤销飞行——方向反了 | 筑基 maxHeight=96，**飞到 97 格就被打下来**，与"最大飞行高度"设计相反 |
| 3 | `MysticVialItem:99` | `isValidBonemealTarget(null, null, state, false)` 传 null level/pos | 原版大多数可催熟方块解引用 level/pos → **NPE 崩服/右键静默失败** |
| 4 | `ReleaseTechniquePacket:121-128` | effect 未执行/未注册（VINE_BIND、EARTH_WALL 数据版等）时仍 `consumeSpiritualPower(cost)` + 设冷却 + 提示成功 | **白扣灵力进冷却**，玩家以为成功 |
| 5 | `ReleaseTechniquePacket:97-103` | 灵力在 `execute` 前扣除，但 `execute` 可能返回 false（土遁无落脚点等），不退费 | **法术失败却扣费+冷却** |
| 6 | `PlayerCultivation` + `Realm.MORTAL`(baseMaxSpiritualPower=0) | 凡人灵力上限 0，`addSpiritualPower` 钳为 0；`clearSevereInjuryIfRecovered` 判 `spiritualPower>=maxSpiritualPower` 即 `0>=0=true` | 凡人**灵力系统失效 + 误清重伤** |
| 7 | `PlayerCultivation.checkQiDeviation` | risk=100 时触发概率仅 `min(0.50,...)=0.50`，非必触发 | 与"100% 极端当场死亡"**文档矛盾** |
| 8 | `BreakthroughService:138` | EXTREME 走火先 `dropHalfInventory`(50%) 再 `hurt(Float.MAX_VALUE)`；`keepInventory=false` 服务器 vanilla 死亡再掉剩余 50% | 实际掉落 **100% 而非 50%** |
| 9 | `ModEvents.onLivingHurt` + 飞剑/冰锥弹射物 | 弹射物 `hurt()` 再次触发本 `LivingHurtEvent`，PvP 分支二次重算可能 cancel 掉本应命中的飞剑 | **飞剑伤害被 combat 改写/吞掉**（CLAUDE.md 已知风险未修） |
| 10 | `ClientTechniqueData.normalizeSlots` | `setLearnedTechniques` 用 `defaultSlots` 覆盖服务端 slots；learned 与 slots 同步顺序错位时 | 客户端**清空玩家手动绑定的技能槽** |
| 11 | 飞行系统 | 无 `PlayerRespawnEvent`/`PlayerChangedDimensionEvent`/`LivingDeathEvent` 强制 revoke；`grantFlying` 不校验灵力 | 死亡/换维度 mayfly 残留；**灵力 0 仍能飞**（revoke 后下 tick 重授） |
| 12 | `AlchemyFurnaceBlockEntity.serverTick` | `progressTicks` 减到 0 时若 level 非 ServerLevel（防御缺失），`finishCraft` 不执行但进度已清零 | **配方静默丢失** |
| 13 | `AlchemyRecipeService.getAlchemySkillBonus` | 硬编码 `return 0.0` | 炼丹技能等级对成功率/爆炉率**完全无影响**（等同未实现） |

## 🟡 中危问题（精选）

- **凝气丹 boost 取 max 不续期**（`CultivationPillItem` + `addCultivationBoost`）：重复服用若已生效则新丹被忽略，玩家浪费丹药无提示。
- **回灵丹 `qiValue` 形同虚设**（`QiRecoveryPillItem:24`）：恒取 `max(50%, qiValue*absorption)`，大丹小丹效果相同。
- **走火风险衰减几乎不触发**（`ModEvents:140,147`）：靠 `tickCount % 14400`/`%7200`，取决于玩家加入时机，基本只增不减。
- **中性灵石无被动加成**（`ModEvents:152`）：`consumeStoneBonus` 仅五行匹配才返回非 0，与 CLAUDE.md "neutral ... passive bonus" 描述冲突。
- **村民兑换吞掉正常交易**（`ModEvents:222-266`）：无灵石/达上限分支也 `setCanceled(true)`，shift+右键村民无法打开原版交易 GUI。
- **灵石未 `stacksTo(1)`**（`ModEvents:646`）：默认堆叠 64，多个空灵石叠在一起吸收会异常（靠运行时 `getCount()!=1` 判断）。
- **土遁步可穿 4 格墙**（`EarthEscapeStepSpell:28`）：设计是"穿越一格阻隔"，实际最大 4 格；未检查目标 chunk loaded。
- **DetectionSpell 性能**（`DetectionSpell:36`）：最坏 33³≈35937 次方块查询 + 每匹配发 3 粒子，冷却 10s 但同步执行，多玩家卡顿。
- **冷却用 per-level gameTime**（`PlayerCultivation:160`）：跨维度 `getGameTime()` 不同步，冷却判定错乱。
- **消耗/境界靠文本启发式猜**（`ReleaseTechniquePacket:154-184`）：`estimateCost`/`estimateTechniqueRealm` 用"剑"→35、"长春功"→练气等关键字猜，与真实值不符且不可靠。
- **SyncCultivationDataPacket `writeUtf` 默认上限**：`spiritualRootAttributes` 多属性拼接超长会抛异常，整个同步包失败 → 客户端永久 `!synced`。
- **CultivationHudOverlay 修为进度条硬编码 0.35**（`:44`）：缺 `cultivationMax` 同步字段，HUD 显示假进度。
- **寿元耗尽 `hurt(Float.MAX_VALUE)`**（`ModEvents:632`）：会触发 `onLivingHurt` 的打坐中断 + 走火检定，寿元死亡不应触发走火。
- **lifespanYears 旧存档回退到 QI_REFINING(100)**（`PlayerCultivation:1212`）：凡人应 80，硬编码回退值不对。
- **PillQuality 枚举存在但不参与炼丹产物**（`AlchemyRecipe:14-32`）：品质写死在配方 id（`_low`）里，运行时不按品质分配，同方不出多品质。
- **HealingPill/FastingPill/ClearSpiritPowder 未用 absorption**：与 RejuvenationPill 行为不一致。

## ⚪ 未实现（stub/占位/空逻辑）

**战斗/技能**
- 筑基 6 技能全 stub（已知）
- `VINE_BIND`/`METAL_BLADE`/`SOUL_SEARCH`/`FIVE_ELEMENT_ROTATION` 等无 SkillEffect 注册（释放走静默扣费）
- `EarthWallSpell` 放 `STONE` **永不移除**（永久改地形，可刷石/封路）
- `InvisibilitySpell`/`LightBodySpell` 无"攻击/移动打破隐身"逻辑
- 神识消耗未实现（接口有，`ReleaseTechniquePacket` 只扣灵力）
- PvE（玩家 vs 怪物）不经 cultivation 计算，仅 PvP 走 `CombatCalculator`
- `CombatStats` 未接入灵根/体质/功法加成
- 技能图标为 hash 着色占位色块，无真实纹理
- `getJadeVialDropChance` 仅透传，青玉小瓶掉落未实现

**炼丹**
- 炼丹技能等级 LV1-10（`getAlchemySkillBonus=0`）
- 炼丹 GUI/Container/Menu（用右键+聊天替代）
- 炼丹炉客户端 ticker/动画/粒子/进度同步
- 大量 `PillType`（黄龙丹/金髓丸/结金丹/元婴丹等）无对应物品与配方
- 废丹产出后无回收/用途

**物品/方块**
- `MysticVialItem` 绑定未生效：`use`/`useOn` 从不校验 `isOwner`，他人可用；死亡掉落/放箱子/熔炼未阻止
- `FlyingArtifactItem` 无装备回调，纯 tick 轮询；无 Curios 槽位限制
- 两个实体（`cushion_seat`/`sword_projectile`）无 spawn egg 注册
- 飞行 respawn/dimension/死亡状态清理
- `showMeditationStatus`/`describeSpiritLand` 死代码（定义未调用）

**UI/同步**
- `CultivationHudOverlay` 修为进度条真实百分比（缺 `cultivationMax`）
- `CultivationStatsScreen` 无分页/滚动，低 GUI scale 下负面状态被截断
- 7 个释放键无 `KeyConflictContext` 分组，易与其他模组冲突

## ❓ 未决定的设计点

1. **两套飞行如何统一**：`FlightProfile` 对 `QI_REFINING` 返回 null（禁飞），但御剑飞行术是练气期技能——练气到底能不能飞？两套系统互斥/优先级未定。
2. **飞行高度**：硬上限撤销（当前，但有方向 bug）vs 软限制上升速度。
3. **走火风险衰减**：用 `tickCount` 取模（当前几乎不触发）vs 累计秒计数器。
4. **中性灵石是否被动**：与 CLAUDE.md 描述冲突，需定调。
5. **丹药 boost 叠加规则**：取 max（当前，不续期）/ 相加续期 / 替换。
6. **品质机制**：每配方注册独立物品（当前 `_low` 后缀）vs 同物品运行时 NBT 品质——两套并存未打通。
7. **丹方匹配**：手持材料歧义时无玩家选择 UI，优先级靠 if-else 顺序。
8. **爆炉/废丹/成功概率边界**：爆炉是否消耗已投材料、是否合并失败，文档未明确。
9. **estimateCost/estimateTechniqueRealm**：文本启发式 vs 功法 JSON 显式 `required_realm`/`cost` 字段（当前散落魔法字符串）。
10. **`SkillEffect.canExecute` 语义**：是否应保证 `execute` 必成功（当前不一致导致扣费 bug）。
11. **INVISIBILITY/LIGHTNESS_SKILL/EARTH_WALL 解锁时机**：techniqueId 为空，不参与 phase4 自动解锁。
12. **`PROTOCOL_VERSION="4"`**：CLAUDE.md 记载为 `"2"`（文档过时），需确认实际 bump 历史与 changelog。
13. **`pendingMeditating` 乐观锁**：保留（有竞态）vs 服务端立即 ACK。
14. **两套境界数值表**：`Realm` 与 `RealmStageConfig` 并行定义 hpBase/manaBase，数值重复维护易不一致。

## 建议下一步

最该先修的 3 项（影响存档/玩法正确性，且改动局部）：
1. **飞行高度方向 bug**（`ModEvents:484` 一行判断反了）+ **mayfly 残留**（加 respawn/dimension revoke）—— 防永久飞行
2. **`MysticVialItem:99` NPE** —— 防崩服
3. **`ReleaseTechniquePacket` 静默扣费**（effect 未执行时不扣费/不进冷却）—— 防白扣灵力

这三项都是高确定性的局部修复。

---

**报告版本**：v1.0
**生成时间**：2026-06-20
**未执行**：未修改任何源码、未 commit、未运行 `gradlew build`
