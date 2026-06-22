# 代码审查修复方案（0.1.47 工作区）

> - **生成日期**：2026-06-20
> - **配套报告**：`docs/code-review-report.md`（v1.0）
> - **性质**：**纯方案文档，未改任何源码、未运行 `gradlew build`**。所有 `file:line` 均经实读当前源码核对。
> - **结构**：高危 H1–H13、中危 M1–M16、未实现 stub、未决设计点 D1–D14，**每条独立成方案**（位置/根因/影响/方案/代码草图/风险/验证）。
> - **落地规则**：实际编码须先按 `CLAUDE.md` 在 `.bak/<timestamp>/` 备份，改完跑 `./gradlew build` 并更新 `project_docs/step_progress.md`；涉及 packet 字段变更须 bump `ModNetwork.PROTOCOL_VERSION` 并同步 CLAUDE.md。

---

## 目录

- [🔴 高危 H1–H13](#-高危-h1h13)
- [🟡 中危 M1–M16](#-中危-m1m16)
- [⚪ 未实现 stub](#-未实现-stub)
- [❓ 未决设计点 D1–D14](#-未决设计点-d1d14)
- [📍 优先级与落地顺序](#-优先级与落地顺序)

---

## 🔴 高危 H1–H13

### H1 两套飞行各自管 mayfly 互不感知 → 永久飞行
- **位置**：`event/ModEvents.java:545-586`（grantFlying/revokeFlying）、`:512-536`（handleQiFlying）；`skill/effect/spell/FlyingSwordBeginnerSpell.java:32-37,42-57`
- **根因**：御剑术与法宝飞行各自维护独立 `previousMayfly` 快照键（`SeekingImmortalsFlyingPreviousMayfly` vs `...QiFlyingPreviousMayfly`），在 *授予* 时各自把当前 `abilities.mayfly` 当“原始基准”采样。但对方可能已把 mayfly 置 true，撤销时还原成 true → mayfly 残留。例：先开御剑（mayfly=true,qiprev=false）→ 装法宝（grantFlying 采样 mayfly=true 当 baseline）→ 收御剑还原 qiprev=false，但法宝 tick 下 tick 又 grant → 卸法宝还原 baseline=true → 永久飞行。
- **影响**：玩家可组合两套飞行获得永久创造模式飞行，绕过境界/灵力约束。
- **方案**：单一 mayfly 基线 + **引用计数**。删两套 PREVIOUS 键，改为单一 `SeekingImmortalsFlyingBaselineMayfly/BaselineSpeed`，仅在飞行源集合为空时采样；grant 时 add 源、revoke 时 remove 源，仅当集合空才真还原 mayfly。
- **代码草图**：新建 `cultivation/FlyingAuthority`（或 ModEvents 静态工具）：
  ```java
  public static void grant(ServerPlayer p, String source, float speed) {
      CompoundTag d = p.getPersistentData();
      Set<String> srcs = readSources(d);
      boolean wasEmpty = srcs.isEmpty();
      srcs.add(source); writeSources(d, srcs);
      if (wasEmpty) {                       // 首次采样基线
          d.putBoolean(BASELINE_MAYFLY, p.getAbilities().mayfly);
          d.putFloat(BASELINE_SPEED, p.getAbilities().getFlyingSpeed());
      }
      p.getAbilities().mayfly = true;
      p.getAbilities().setFlyingSpeed(speed);
      p.onUpdateAbilities();
  }
  public static void revoke(ServerPlayer p, String source, String msg) {
      CompoundTag d = p.getPersistentData();
      Set<String> srcs = readSources(d);
      if (!srcs.remove(source)) return;     // 非本源授权不动
      if (!srcs.isEmpty()) { writeSources(d, srcs); return; } // 仍有其他源
      p.getAbilities().mayfly = d.getBoolean(BASELINE_MAYFLY);
      if (!p.getAbilities().mayfly) p.getAbilities().flying = false;
      p.getAbilities().setFlyingSpeed(d.contains(BASELINE_SPEED) ? d.getFloat(BASELINE_SPEED) : 0.05F);
      p.onUpdateAbilities();
      d.remove(BASELINE_MAYFLY); d.remove(BASELINE_SPEED);
      if (msg != null) p.displayClientMessage(Component.translatable(msg), true);
  }
  ```
- **风险**：需迁移两处调用点；respawn/dimension 钩子须清基线（见 H11）。
- **验证**：`./gradlew build`；先开御剑再装法宝→卸法宝→收御剑 mayfly 关闭，反序同样。

### H2 飞行高度上限方向反了
- **位置**：`event/ModEvents.java:484`
- **根因**：`if (player.getY() > player.level().getMinBuildHeight() + profile.maxHeight())` —— 主世界 `getMinBuildHeight()=-64`，阈值=-64+96=32。筑基玩家飞到 Y=33 即被打落，与“最大飞行高度 96”设计相反。
- **影响**：筑基及以上起飞即坠落，飞行系统基本不可用。
- **方案**：改绝对高度比较 `player.getY() > profile.maxHeight()`，删 `getMinBuildHeight()` 参与。
- **代码草图**：
  ```java
  if (player.getY() > profile.maxHeight()) {
      revokeFlying(serverPlayer, "message.seeking_immortals.flight.stop.height");
      return;
  }
  ```
- **风险**：下界基岩顶 127，96 偏低可接受；真仙 512 超主世界 320 build height 无副作用。
- **验证**：build；筑基 Y=95 不掉、Y=97 掉；金丹 128 同理。

### H3 MysticVial isValidBonemealTarget 传 null → NPE/静默失败
- **位置**：`item/MysticVialItem.java:99`
- **根因**：`growable.isValidBonemealTarget(null, null, state, false)` —— 多数 `BonemealableBlock` 实现会解引用 level（CropBlock/SaplingBlock 查 `level.getRawBrightness`/`getBlockState`），传 null 必 NPE，右键植物崩服或静默 PASS。
- **影响**：服务器崩溃；至少非作物可催熟方块失效。
- **方案**：`isGrowTarget` 改签名传入 level+pos；`useOn` 已有 level 与 pos，直接传实参。保留 CropBlock 快速路径。
- **代码草图**：
  ```java
  // useOn 内（clientSide return 之后）
  BlockState state = level.getBlockState(pos);
  if (!isGrowTarget(level, pos, state)) return InteractionResult.PASS;

  private static boolean isGrowTarget(Level level, BlockPos pos, BlockState state) {
      if (state.getBlock() instanceof CropBlock crop) return !crop.isMaxAge(state);
      return state.getBlock() instanceof BonemealableBlock g
              && g.isValidBonemealTarget(level, pos, state, false);
  }
  ```
- **风险**：无回归（isGrowTarget 已在 clientSide return 之后调用，传 serverLevel 安全）。
- **验证**：build；右键树苗/蘑菇/草丛催熟不崩，作物仍催熟。

### H4 未注册/执行失败的 effect 仍扣灵力进冷却 + 提示成功
- **位置**：`network/ReleaseTechniquePacket.java:84-148`（`:121-128` 为扣费兜底）
- **根因**：当 `SkillEffectRegistry.byDisplayName(technique.name())` 返回 null（VINE_BIND/METAL_BLADE/SOUL_SEARCH/FIVE_ELEMENT_ROTATION/SWORD_FORMATION 未注册）或 skill 未解锁/effect 为 null，整个 `if (skill!=null && skill.isUnlocked() && effect!=null)` 块被跳过，`effectExecuted` 保持 false。随后 line 121 `if (!effectExecuted && !consumeSpiritualPower(cost))` 无条件扣费，line 127 设冷却，line 137-148 仍发“释放成功”。
- **影响**：对 stub 技能按释放键 → 白扣灵力、进冷却、误导“成功”。
- **方案**：区分“无 effect 注册”（拒绝释放，不扣费不冷却）与“执行失败”（已在 line 109-114 处理）。在 techniqueOpt 分支内 effect/skill 缺失即 early return + 提示；删除 line 121 无 effect 也扣费兜底；仅 effectExecuted=true 才 setCooldown + 成功提示。
- **代码草图**：
  ```java
  if (techniqueOpt.isPresent()) {
      var technique = techniqueOpt.get();
      SkillType skillType = SkillEffectRegistry.byDisplayName(technique.name());
      SkillEffect effect = skillType == null ? null : SkillEffectRegistry.get(skillType);
      CultivationSkill skill = skillType == null ? null : cultivation.getSkill(skillType);
      if (effect == null || skill == null || !skill.isUnlocked()) {
          player.displayClientMessage(Component.translatable("message.seeking_immortals.technique_release.effect_unavailable"), true);
          SyncLearnedTechniquesPacket.send(player, cultivation);
          return;                           // 不扣费不冷却
      }
      // ... canExecute/cost/execute 流程 ...
  }
  if (effectExecuted) {
      cultivation.setTechniqueCooldown(techniqueId, nowTick + cooldownTicks);
      SyncCultivationDataPacket.send(player, cultivation);
      SyncLearnedTechniquesPacket.send(player, cultivation);
      player.displayClientMessage(/* success */);
  }
  ```
  新增 lang key `...technique_release.effect_unavailable`。建议在 `effect==null` 路径加 `LOGGER.warn` 便于排查命名不一致。
- **风险**：需确认 `technique.name()` 与 `SkillType.getDisplayName()` 一致；包结构未变，**无需 bump 协议**。
- **验证**：build；对藤蔓/金刃/五行轮转按释放键收到“无法施展”且灵力/冷却不变；火球/土遁正常。

### H5 execute 返回 false 时已扣灵力且不退费
- **位置**：`network/ReleaseTechniquePacket.java:97-114`
- **根因**：line 99 `consumeSpiritualPower(cost)` 在 line 109 `effect.execute(...)` **之前**。execute 可能返回 false（土遁无落脚点、飞剑灵力不足），line 109-114 检测 false 后只发“effect_failed”消息 + return，**无退费**。玩家可反复尝试反复扣费。
- **影响**：法术失败却扣费（且不设冷却，比 H4 更隐蔽）。
- **方案**（推荐 execute-first）：先 `canExecute` + `execute`，成功后再 `consumeSpiritualPower`。注意部分 spell 的 execute 内部也查灵力（重复但无害）；需 audit execute 失败路径无副作用（已检查 Fireball/IceCone/SwordProjectile/EarthEscape/EarthWall/Invisibility/LightBody/FlyingSword/Detection 失败前均不改世界状态）。
- **代码草图**：
  ```java
  if (!effect.canExecute(player, cultivation)) { /* effect_failed; return; */ }
  int cost = effect.getSpiritualPowerCost(skill.getLevel());
  if (cultivation.getSpiritualPower() < cost) { /* not_enough_qi; return; }   // 只检查不扣
  SkillContext ctx = SkillContext.builder()...build();
  if (!effect.execute(player, cultivation, skill, ctx)) {
      player.displayClientMessage(Component.translatable("...effect_failed"), true);
      SyncCultivationDataPacket.send(player, cultivation);
      return;
  }
  if (!cultivation.consumeSpiritualPower(cost)) { /* 防御性 return */ }
  cultivation.addSkillProficiency(skillType, 10);
  ```
- **风险**：需逐个 audit execute 副作用（已确认安全）；长期可移除 spell 内部灵力检查统一由 packet 层把关（见 D10）。
- **验证**：build；对墙释放土遁步应失败不扣灵力；灵力刚好不够时释放火球应 not_enough_qi。

### H6 凡人灵力上限 0 → 灵力失效 + 误清重伤
- **位置**：`cultivation/PlayerCultivation.java:670-672`（getMaxSpiritualPower）、`:304-308`（clearSevereInjuryIfRecovered）、`:742-747`（addSpiritualPower）；`cultivation/Realm.java:5`（MORTAL baseMaxSpiritualPower=0）
- **根因**：MORTAL 基础灵力 0，`getMaxSpiritualPower()` 返回 `Math.round(0*mult)=0`；`addSpiritualPower` 第 745 行 `Math.min(0, ...)` 钳为 0，凡人灵力恒 0；`clearSevereInjuryIfRecovered` 第 305 行判 `spiritualPower>=getMaxSpiritualPower()` 即 `0>=0=true`，凡人一旦受伤即秒清重伤，-80% HP 修饰器消失。
- **影响**：凡人灵力系统失效；重伤惩罚对凡人形同虚设。
- **方案**：给凡人最小灵力下限（如 `MORTAL_MIN_SPIRITUAL_POWER=50`），同时把重伤恢复门独立于“灵力达上限”。
- **代码草图**：
  ```java
  private static final int MORTAL_MIN_SPIRITUAL_POWER = 50;
  public int getMaxSpiritualPower() {
      int raw = Math.round(realm.getBaseMaxSpiritualPower() * stage.getMaxSpiritualPowerMultiplier());
      return realm == Realm.MORTAL ? Math.max(MORTAL_MIN_SPIRITUAL_POWER, raw) : raw;
  }
  public void clearSevereInjuryIfRecovered() {
      int max = getMaxSpiritualPower();
      if (severeInjury && max > 0 && spiritualPower >= max) severeInjury = false;
  }
  ```
- **风险**：凡人突破到炼气 max 跳 100，`Math.min(max, current+adjusted)` 不会回退灵力，安全；旧凡人存档灵力 0 经 `loadNBTData` 末尾 clamp 无影响。
- **验证**：build；新建凡人打坐能回复到 50；`/seeking_immortals affliction severe_injury` 后凡人 HP 出现 -80% 且不在灵力=0 时秒清。

### H7 走火风险=100 仅 0.50 触发，与“100% 当场死亡”矛盾
- **位置**：`cultivation/PlayerCultivation.java:991-995`（checkQiDeviation）
- **根因**：`chance = Math.min(0.50, Math.max(0.20, (risk-50)/100))`，risk=100 时 (100-50)/100=0.50 被 min 钳为 0.50，仅 50% 触发；而 `determineQiDeviationTier` 在 risk>=100 返回 EXTREME（当场死亡），设计意图是必触发。
- **影响**：极端走火无法保证触发，破坏威慑与文档承诺。
- **方案**：入口对 `qiDeviationRisk>=100` 直接返回 true；其余区间保留线性公式。
- **代码草图**：
  ```java
  private boolean checkQiDeviation(RandomSource random) {
      if (qiDeviationRisk >= MAX_QI_DEVIATION_RISK) return true;   // 100% 必触发极端
      if (qiDeviationRisk < 70) return false;
      double chance = Math.min(0.50D, Math.max(0.20D, (qiDeviationRisk - 50) / 100.0D));
      return random.nextDouble() < chance;
  }
  ```
- **风险**：`MAX_QI_DEVIATION_RISK=100` 已是 clamp 上限，`>=100` 等价 `==100`；调用链 risk=100→checkQiDeviation true→EXTREME→死亡分支，符合预期。
- **验证**：build；设 risk=100 触发走火应 100% 走 EXTREME 死亡。

### H8 EXTREME 走火掉落 100% 而非 50%
- **位置**：`cultivation/BreakthroughService.java:135-140`（EXTREME 分支）、`:164-180`（dropHalfInventory）
- **根因**：EXTREME 顺序：`dropHalfInventory` 主动掉 50% 并清空对应槽 → `hurt(magic, Float.MAX_VALUE)` 死亡。`keepInventory=false` 服 vanilla `Player.die`→`inventory.dropAll()` 再掉**剩余 50%** → 实际 100%。
- **影响**：极端走火惩罚翻倍，物品全损。
- **方案**（方案 A）：死亡前把剩余 50% 物品移到临时缓存（PersistentData 序列化为 ListTag）→ 清空背包 → `hurt` 死亡（vanilla 无物可掉）→ 在 `PlayerRespawnEvent` 归还剩余 50%。keepInventory=true 服行为一致（清空+归还=保留）。
- **代码草图**：
  ```java
  case EXTREME -> {
      cultivation.setQiDeviationRisk(0);
      dropHalfInventory(player, random);
      List<ItemStack> preserved = new ArrayList<>();
      Inventory inv = player.getInventory();
      for (int i = 0; i < inv.getContainerSize(); i++) {
          ItemStack s = inv.getItem(i);
          if (!s.isEmpty()) { preserved.add(s.copy()); inv.setItem(i, ItemStack.EMPTY); }
      }
      player.getPersistentData().put("SeekingImmortalsExtremePreserved", persistStackList(preserved));
      player.hurt(player.damageSources().magic(), Float.MAX_VALUE);
      player.displayClientMessage(Component.translatable("message.seeking_immortals.qi_deviation.extreme"), false);
  }
  ```
  `PlayerEvent.PlayerRespawnEvent` 读取并 `inventory.add` 归还，用完即删 key（需新增 `persistStackList`/`restoreStackList` helper）。
- **风险**：归还只在 EXTREME 死亡后触发一次（用完即删 key）；`PlayerRespawnEvent` 时机需早于玩家可操作；keepInventory=true 下 `dropHalfInventory` 已掉 50%，剩余归还=保留 50%，符合设计。
- **验证**：build；keepInventory=false 服堆 risk=100 触发 EXTREME，死后地上 50%、重生背包 50%；keepInventory=true 服重生保留 50%（掉 50% 在原地）。

### H9 飞剑/冰锥弹射物 hurt 触发 LivingHurtEvent 被 PvP 分支二次重算
- **位置**：`event/ModEvents.java:175-219`；`entity/SwordProjectileEntity.java:63-73`
- **根因**：`SwordProjectileEntity.onHitEntity` 调 `target.hurt(indirectMagic(this, owner), damage)` 触发 `onLivingHurt`：line 194-203 `sourceEntity=owner`(玩家) 把 `outgoingDamageMultiplier` 再乘一次到飞剑伤害（飞剑构造时 `calculateDamage` 已含 cultivation 加成，重复加成）；line 205-218 若 target 是玩家走 `CombatCalculator` 可能 `setCanceled(true)` 吞掉本应命中的飞剑。
- **影响**：飞剑 PvP 伤害双算 multiplier；命中可能被 miss/dodge 无理由 cancel；反馈被覆盖。
- **方案**（重入守卫 + 来源标记）：`SwordProjectileEntity` 构造时 `getPersistentData().putBoolean("SeekingImmortalsProjectileDamage", true)`；`onLivingHurt` 顶部若 `directEntity` 含该 key，跳过 line 194-218 的 multiplier/PvP 分支，仅保留打坐中断逻辑。
- **代码草图**：
  ```java
  // ModEvents.onLivingHurt（打坐逻辑之后，line 187 后）：
  Entity directEntity = event.getSource().getDirectEntity();
  boolean isModProjectile = directEntity != null
          && directEntity.getPersistentData().getBoolean("SeekingImmortalsProjectileDamage");
  if (isModProjectile) return;              // 弹射物伤害已在生成时算，不参与 combat 重算
  // 原 line 188-218 保持
  ```
- **风险**：火球当前靠 `SeekingImmortalsCustomDamage` NBT 在 line 188-192 改写伤害——守卫若对火球也生效会跳过改写。建议守卫只针对 `SwordProjectileEntity` instanceof，不动火球；或火球改用直接 hurt 传 damage。需确认飞剑 PvE 是否已含 multiplier（读 `calculateDamage` 确认）。
- **验证**：build；玩家 A 飞剑刺玩家 B，伤害等于飞剑生成时 `calculateDamage`，不被 dodge/miss 吞；打怪伤害与改动前“含 multiplier”一致或确认设计意图。

### H10 ClientTechniqueData.normalizeSlots 覆盖服务端 slots → 清空手动绑定
- **位置**：`client/ClientTechniqueData.java:37-53, 96-104`
- **根因**：`setLearnedTechniques(techniques)` 调 `setTechniqueData(techniques, defaultSlots(techniques), Map.of())`，用“按字母序前 N 填充”的默认槽无条件覆盖当前 `techniqueSlots`；`normalizeSlots` 在 slots 为空时也返回 `defaultSlots(learned)`。任一只更新 learned 列表的同步都会清空玩家在 `TechniqueEditScreen` 拖拽绑定的 7 槽。
- **影响**：玩家手动绑定被一次 learned-only 同步清空，释放键映射错乱。
- **方案**：`setLearnedTechniques` 仅更新 learned 并**保留**现有绑定（仅剔除已遗忘技法）；`normalizeSlots` 在 slots 为空时返回基于现有 `techniqueSlots` 的清理结果而非 defaultSlots。
- **代码草图**：
  ```java
  public static void setLearnedTechniques(List<String> techniques) {
      List<String> learned = techniques.stream().sorted().toList();
      setTechniqueData(learned, retainValidSlots(techniqueSlots, learned), Map.of());
  }
  private static List<String> retainValidSlots(List<String> current, List<String> learned) {
      List<String> out = emptySlots();
      for (int i = 0; i < SLOT_COUNT; i++) {
          String id = i < current.size() ? current.get(i) : "";
          out.set(i, !id.isBlank() && learned.contains(id) ? id : "");
      }
      return List.copyOf(out);
  }
  private static List<String> normalizeSlots(List<String> slots, List<String> learned) {
      if (slots == null || slots.isEmpty()) return retainValidSlots(techniqueSlots, learned); // 不再 defaultSlots
      // ... 现有 copy/validate ...
  }
  ```
- **风险**：`reset()` 后首次同步若服务端 slots 为空玩家会看到全空槽——预期行为（服务端登录应发真实 slots，已确认 `SyncLearnedTechniquesPacket.from` line 29 携带 `getTechniqueSlots()`）。仅客户端逻辑，**无需 bump 协议**。
- **验证**：build；学习/遗忘技法、重登后 7 槽绑定保留；绑定后触发只含 learnedCount 的 `SyncCultivationDataPacket` 槽位不清空。

### H11 飞行无生命周期清理 + grant 不校验灵力
- **位置**：`event/ModEvents.java:545-568`（grantFlying）、`:512-536`（handleQiFlying）；`skill/effect/spell/FlyingSwordBeginnerSpell.java:28`
- **根因**：(a) 无 `PlayerRespawnEvent`/`PlayerChangedDimensionEvent`/`LivingDeathEvent` 钩子 revoke → 死亡/换维后 `FLYING_GRANTED_KEY`/`ACTIVE_KEY` 残留，复活后 abilities 由 vanilla 重置但 PersistentData 未清，下 tick grantFlying 看到 GRANTED=true 跳过基线采样保留 mayfly；(b) `grantFlying` 不查灵力，灵力 0 时每 tick revoke(no_power) 下 tick 又 grant，半空抖动。
- **影响**：死亡复活/跨维度后 mayfly 残留；灵力耗尽后飞行抖动。
- **方案**：1) 新增 respawn/dimension/death 钩子调统一 `resetFlyingState` 清空所有飞行键并强制 mayfly=false/flying=false；2) grantFlying 入口加灵力门。
- **代码草图**：
  ```java
  @SubscribeEvent public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { resetFlyingState(e.getEntity()); }
  @SubscribeEvent public static void onDimChange(PlayerEvent.PlayerChangedDimensionEvent e) { resetFlyingState(e.getEntity()); }
  @SubscribeEvent public static void onDeath(LivingDeathEvent e) {
      if (e.getEntity() instanceof ServerPlayer p && !p.level().isClientSide) resetFlyingState(p);
  }
  private static void resetFlyingState(Player p) {
      if (!(p instanceof ServerPlayer sp)) return;
      CompoundTag d = sp.getPersistentData();
      boolean wasActive = d.getBoolean(FLYING_GRANTED_KEY) || d.getBoolean(FlyingSwordBeginnerSpell.ACTIVE_KEY);
      d.remove(FLYING_GRANTED_KEY); d.remove(FLYING_PREVIOUS_MAYFLY_KEY);
      d.remove(FLYING_PREVIOUS_SPEED_KEY); d.remove(FLYING_SPEED_KEY);
      d.remove(FlyingSwordBeginnerSpell.ACTIVE_KEY);
      d.remove(FlyingSwordBeginnerSpell.PREVIOUS_MAYFLY_KEY);
      d.remove(FlyingSwordBeginnerSpell.PREVIOUS_SPEED_KEY);
      if (wasActive && !sp.isCreative() && !sp.isSpectator()) {
          sp.getAbilities().mayfly = false; sp.getAbilities().flying = false;
          sp.onUpdateAbilities();
      }
  }
  // grantFlying 入口
  if (cultivation.getSpiritualPower() < profile.costPerSecond()) {
      revokeFlying(serverPlayer, "message.seeking_immortals.flight.stop.no_power"); return;
  }
  ```
- **风险**：`LivingDeathEvent` 需 isClientSide 守卫；创意模式早退；与 H1 引用计数方案配合时 resetFlyingState 应清 source 集合。
- **验证**：build；飞行中 `/kill`、跨维度门、灵力耗尽——复活后无法飞，灵力 0 不再 grant 抖动。

### H12 AlchemyFurnace progressTicks 清零不执行 finishCraft
- **位置**：`block/entity/AlchemyFurnaceBlockEntity.java:32-39`
- **根因**：`serverTick` 中 `progressTicks--` 与 `setChanged()` 在 `level instanceof ServerLevel` 判断**之前**执行。若 level 非 ServerLevel，进度已清零写盘但 `finishCraft` 不触发。**实测**：`AlchemyFurnaceBlock.getTicker`（:49-50）客户端返回 null，`serverTick` 当前只在 ServerLevel 调用，instanceof 守卫恒为真——属防御缺失型潜在 bug，运行时不触发，但脆弱写法（重构即激活且难调试）。
- **影响**：当下无实际后果；健壮性/回归隐患。
- **方案**：守卫上提到函数顶端，非 ServerLevel 直接 return；仅 ServerLevel 分支内递减 + finishCraft。
- **代码草图**：
  ```java
  public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemyFurnaceBlockEntity furnace) {
      if (furnace.progressTicks <= 0) return;
      if (!(level instanceof ServerLevel serverLevel)) return;   // 防御：非服务端不动状态
      furnace.progressTicks--;
      furnace.setChanged();
      if (furnace.progressTicks <= 0) furnace.finishCraft(serverLevel);
  }
  ```
- **风险**：几乎无回归（getTicker 已保证仅服务端调用）；注意递减须在 finishCraft 之前（finishCraft 内部会把 progressTicks 重置 0）。
- **验证**：build；投料→炼制完成→产出/爆炉/废丹分支正常；断点确认客户端不进 serverTick。

### H13 AlchemyRecipeService.getAlchemySkillBonus 硬编码 0 → 炼丹技能无效
- **位置**：`alchemy/AlchemyRecipeService.java:89-91`
- **根因**：`private static double getAlchemySkillBonus(ServerPlayer player) { return 0.0D; }`。该值同时加到 `successRate`（:31）和按 `*0.5` 抵减 `explosionChance`（:38）。`SkillType.ALCHEMY`（max level 10）已存在且 `PlayerCultivation` 已暴露 `getSkill(SkillType)`/`hasSkill`（:176-181），但服务层根本不读。
- **影响**：炼丹术解锁/升级对成功率/爆炉率无影响，技能系统在此处“死接”。
- **方案**：通过 `CultivationHelper.get(player)` 取 cultivation，读 `SkillType.ALCHEMY` 的 `CultivationSkill`，未解锁视为 0，每级 +0.02（满级 +0.20）。
- **代码草图**：
  ```java
  private static double getAlchemySkillBonus(ServerPlayer player) {
      return CultivationHelper.get(player)
          .map(c -> {
              CultivationSkill skill = c.getSkill(SkillType.ALCHEMY);
              if (skill == null || !skill.isUnlocked()) return 0.0D;
              return Math.max(0, skill.getLevel()) * 0.02D;        // 满级 +0.20
          }).orElse(0.0D);
  }
  ```
  新增 import `skill.SkillType`、`skill.CultivationSkill`。
- **风险**：`successRate` 已有 `[0.05,0.95]` 夹取，bonus 不越界；`explosionChance` 有 `Math.max(0,...)` 安全。建议改 `public` 便于 UI/调试展示。
- **验证**：build；给玩家炼丹术 LV1/LV10，对比 `alchemy_furnace.started`（:74 输出 `(int)round(successRate*100)`）成功率；爆炉率统计下降。

---

## 🟡 中危 M1–M16

### M1 凝气丹 boost 取 max 不续期
- **位置**：`item/CultivationPillItem.java:29`；`cultivation/PlayerCultivation.java:792-795`（addCultivationBoost）
- **根因**：`addCultivationBoost` 用 `Math.max` 比 ticks 又比 multiplier。重复服丹时若已生效（multiplier 2.0）新丹同 2.0，`max(2.0,2.0)=2.0` 不变，ticks 也 `max(剩余,72000)`，剩余>72000 则新丹续期被吞。玩家无提示，丹被消耗。
- **方案**：改“续期 + 取较大 multiplier”：multiplier 取 max，ticks 改 `current+ticks` 并设上限（如 `min(current+ticks, 2*BOOST_TICKS)`）；`CultivationPillItem.use` 判断已生效给续期提示。
- **代码草图**：
  ```java
  public void addCultivationBoost(int ticks, double multiplier) {
      cultivationBoostTicks = Math.min(cultivationBoostTicks + ticks, BOOST_TICKS * 2);
      cultivationBoostMultiplier = Math.max(cultivationBoostMultiplier, multiplier);
  }
  // use:
  boolean already = cultivation.getCultivationBoostTicks() > 0;
  cultivation.addCultivationBoost(BOOST_TICKS, BOOST_MULTIPLIER);
  player.displayClientMessage(Component.translatable(already
      ? "message.seeking_immortals.cultivation_pill.boost_extend"
      : "message.seeking_immortals.cultivation_pill.boost", adjustedExp), true);
  ```
- **风险**：NBT 字段已有无需改协议；上限防滥用；需加 lang key。
- **验证**：build；连服 3 颗时长累加且有续期提示；到期 multiplier 回落 1.0。（关联 D5）

### M2 回灵丹 qiValue 形同虚设
- **位置**：`item/QiRecoveryPillItem.java:25-26`
- **根因**：`Math.max(ceil(maxSP*0.5), round(qiValue*absorption))`，大丹小丹都至少 `maxSP*0.5`，只要 `qiValue*absorption <= maxSP*0.5` 丹品质不影响回复量。
- **方案**：让 qiValue 主导，百分比仅作下限兜底：`Math.max(round(qiValue*absorption), ceil(maxSP*0.1))`。
- **代码草图**：
  ```java
  int fromQi = (int) Math.round(qiValue * cultivation.getPillAbsorptionMultiplier());
  int floor = (int) Math.ceil(cultivation.getMaxSpiritualPower() * 0.1D);
  int adjustedAmount = Math.max(fromQi, floor);
  ```
- **风险**：低境界 maxSP 小时 floor 可能 > qiValue 抬升下限，可接受；若担心低阶丹过强可改纯 qiValue 无 floor。
- **验证**：build；同境界服低/中/高回灵丹回复量随 qiValue 递增。

### M3 走火风险衰减几乎不触发
- **位置**：`event/ModEvents.java:139-149`
- **根因**：靠 `tickCount % (INTERVAL*20) == 0`，且该块在 `if (tickCount%20!=0) return`（:118）之后。两个取模同时成立要求 tickCount 是 20 与 14400 的公倍数（LCM=14400），窗口仅 1 tick 宽，玩家加入时机任意，基本错过 → 风险只增不减。灵脉衰减 `%7200` 同理。
- **方案**：改累计计数器（非 tickCount 取模）。`PlayerCultivation` 新增 `qiDevDecayAccumulatorTicks`/`leylineQiDevDecayAccumulatorTicks`，每平稳打坐 tick 累加，达阈值 -1 并清零；risk=0 时重置累加器；累加器需持久化到 NBT（否则重启洗风险）。
- **代码草图**：
  ```java
  // PlayerCultivation 新增字段 + 方法
  public void tickQiDeviationDecay(boolean leyline) {
      if (qiDeviationRisk <= 0) { qiDevDecayAccumulatorTicks = 0; leylineQiDevDecayAccumulatorTicks = 0; return; }
      if (++qiDevDecayAccumulatorTicks >= QI_DEV_RISK_DECAY_INTERVAL_SECONDS * 20) {
          addQiDeviationRisk(-1); qiDevDecayAccumulatorTicks = 0;
      }
      if (leyline && ++leylineQiDevDecayAccumulatorTicks >= LEYLINE_RISK_DECAY_INTERVAL_SECONDS * 20) {
          addQiDeviationRisk(-1); leylineQiDevDecayAccumulatorTicks = 0;
      }
  }
  ```
  ModEvents:139-149 替换为 `cultivation.tickQiDeviationDecay(auraInfo.leyline());`（置于 `isMeditating()` 内每秒调用）。
- **风险**：累加器须持久化；须保证只在“平稳打坐”（无怪物/受伤打断）累加——当前块已在 `isMeditating()` 且 `shouldInterruptMeditation` 返回 false 的 else 分支内，符合语义。
- **验证**：build；堆 risk=50 平稳打坐 12 分钟稳定 -1（不再依赖加入时机）；灵脉地 6 分钟额外 -1。（关联 D3）

### M4 中性灵石无被动加成（与 CLAUDE.md 冲突）
- **位置**：`event/ModEvents.java:395-400`（getMatchingPassiveBonus）
- **根因**：`getMatchingPassiveBonus` 在 `!isFiveElement(requiredAttribute)` 时直接返回 0。中性灵石（attribute==null）即便灵根匹配也无 passive bonus；当前 ModItems 实际**未注册**任何中性灵石，CLAUDE.md “neutral ... passive bonus”描述与代码不符。
- **方案**（定调见 D4）：注册中性灵石系列（attribute=null），`getMatchingPassiveBonus` 对 attribute==null 返回通用 `passiveBonus` 不受限；或对变异灵根放宽匹配母系五行/半额加成。推荐先做逻辑修复 + 注册中性石。
- **代码草图**（方案 A 逻辑部分）：
  ```java
  private static int getMatchingPassiveBonus(ItemStack stack, PlayerCultivation cultivation) {
      if (stack.getCount() != 1 || !(stack.getItem() instanceof SpiritStoneItem stone)
              || SpiritStoneItem.getStoredPower(stack) <= 0) return 0;
      if (stone.getAttribute() == null) return stone.getPassiveBonus();          // 通用中性
      SpiritualRootAttribute req = cultivation.getSpiritualRootAttribute();
      return stone.matchesAttribute(req) ? stone.getPassiveBonus() : 0;
  }
  ```
- **风险**：需新增中性灵石注册 + 资源/lang/创造标签；若暂不注册至少为变异灵根放宽（方案 B）。
- **验证**：build；变异灵根玩家持中性石/母系石打坐 spiritualPower 加成生效。（关联 D4）

### M5 村民兑换吞掉正常交易
- **位置**：`event/ModEvents.java:222-266`
- **根因**：方法在 shift+右键村民时无条件 `event.setCanceled(true)`（:264-265），无论是否真兑换成功。无灵石/达上限分支也取消 → vanilla 交易 GUI 无法打开。
- **影响**：玩家 shift+右键村民永远打不开原版交易界面。
- **方案**：仅在实际完成兑换时取消；达上限/无灵石分支不取消，放行 vanilla。
- **代码草图**：
  ```java
  if (usedToday >= DAILY_EXCHANGE_LIMIT) {
      player.displayClientMessage(Component.translatable("message.seeking_immortals.exchange.limit", DAILY_EXCHANGE_LIMIT), true);
      return;                       // 不取消，放行 vanilla
  }
  boolean exchanged = tryExchange(...) || tryExchange(...);
  if (exchanged) {
      data.putInt(EXCHANGE_COUNT_KEY, usedToday + 1);
      player.displayClientMessage(/* success */, true);
      event.setCancellationResult(InteractionResult.SUCCESS);
      event.setCanceled(true);
  } // else 不取消
  ```
- **风险**：若设计要 shift 专用于兑换，修复后 shift 无灵石会开 vanilla GUI（更友好，可接受）。
- **验证**：build；shift+右键村民有灵石→兑换不开 GUI；无灵石→开 vanilla GUI；达上限→开 GUI。

### M6 灵石未 stacksTo(1)
- **位置**：`item/SpiritStoneItem.java:33`（`super(properties.stacksTo(64))`）；`registry/ModItems.java:154`
- **根因**：默认堆叠 64。`use`/`getMatchingPassiveBonus`/`absorbFromHeldStone` 靠运行时 `getCount()!=1` 拒绝堆叠吸收/被动，但 `StoredSpiritualPower` NBT 写在整叠上，`shrink(1)` 只减一个但 `consumeStoredPower` 改整叠 tag → 空灵石叠在一起吸收逻辑混乱。
- **方案**：`stacksTo(1)`（与 FlyingArtifact/MysticVial 一致），移除冗余的运行时 getCount 判断。代价：背包占用增大；村民兑换需 100 颗时占 100 格但仍可用。折中备选：保留 64 但 `consumeStoredPower` 当 `getCount()>1` 时先 `split(1)`。
- **代码草图**（推荐 stacksTo(1)）：
  ```java
  public SpiritStoneItem(Properties properties, int maxStoredPower, int absorbPerSecond, int passiveBonus, @Nullable SpiritualRootAttribute attribute) {
      super(properties.stacksTo(1));   // 原 stacksTo(64)
      ...
  }
  ```
- **风险**：村民兑换阈值过大时背包压力；与策划权衡（见下）。
- **验证**：build；多颗灵石不再叠成一叠；吸收/depleted 正常；村民兑换仍可达 100。

### M7 土遁步可穿 4 格墙
- **位置**：`skill/effect/spell/EarthEscapeStepSpell.java:28`
- **根因**：`for (distance=4.0; distance>=1.5; distance-=0.5)` 从 4 格递减找可站立点，`canStandAt` 只检查目标落脚点不检查 origin→target 路径中间阻隔 → 可直接 teleport 4 格穿透最多 3 格墙。设计（qi_refining_techniques.json:36）“穿越一格阻隔”。另 `canStandAt` 未校验 chunk loaded。
- **方案**：最大距离 2 格（穿越 1 格阻隔=起步+1 阻隔+落脚=2 位移）；路径逐格步进检查中间固体格数≤1；加 `level.isLoaded(pos)`/`hasChunkAt` 守卫。
- **代码草图**：
  ```java
  ServerLevel level = player.serverLevel();
  Vec3 origin = player.position();
  for (double distance = 2.0D; distance >= 1.0D; distance -= 0.5D) {
      Vec3 target = origin.add(flat.scale(distance));
      BlockPos targetPos = BlockPos.containing(target);
      if (!level.isLoaded(targetPos) || !canStandAt(level, targetPos)) continue;
      // 路径穿透性：origin→target 之间固体格数 ≤ 1
      if (countSolidBetween(level, origin, target) > 1) continue;
      player.teleportTo(target.x, target.y, target.z);
      level.playSound(...);
      return true;
  }
  player.displayClientMessage(Component.literal("前方地脉紊乱，土遁步失败。"), true);
  return false;
  ```
- **风险**：缩距影响体验，需确认设计未要求更远；`teleportTo` 跨区块加 isLoaded 守卫后安全失败。
- **验证**：build；1 格厚墙穿过；2 格厚墙失败；边界区块安全失败不崩。

### M8 DetectionSpell 同步扫描性能
- **位置**：`skill/effect/spell/DetectionSpell.java:36-54`
- **根因**：`blockRange=Math.min(16,ceil(range))`，三重循环 `[-16,16]³` 经球面过滤仍约 (4/3)π·16³≈17157 次 `getBlockState`（非 35937 但量级仍大），每匹配发 3 粒子，多玩家同 tick 释放卡顿；`getBlockState` 对未加载 chunk 触发加载；全程主线程同步。
- **方案**：1) 封顶匹配数 `maxBlockMatches=32` 达上限 break；2) 缩范围 `blockRange=Math.min(8, ceil(range/2))`，y 限 ±4；3) 每匹配发 1 粒子（前 N 个发粒子其余仅计数）；4) `isLoaded(pos)` 跳过未加载。长期可分 tick 切片扫描。
- **代码草图**：
  ```java
  int highlightedBlocks = 0, maxBlockMatches = 32;
  int blockRange = Math.min(8, (int)Math.ceil(range / 2.0D));
  BlockPos origin = player.blockPosition();
  outer:
  for (int x = -blockRange; x <= blockRange; x++)
    for (int y = -4; y <= 4; y++)
      for (int z = -blockRange; z <= blockRange; z++) {
          if (x*x + y*y + z*z > blockRange*blockRange) continue;
          BlockPos pos = origin.offset(x, y, z);
          if (!serverLevel.isLoaded(pos)) continue;
          BlockState state = serverLevel.getBlockState(pos);
          if (state.is(Blocks.AIR)) continue;
          String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
          if (path.contains("spirit") || path.contains("ore") || path.contains("grass") || path.contains("mushroom")) {
              if (highlightedBlocks < maxBlockMatches)
                  serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX()+0.5, pos.getY()+0.8, pos.getZ()+0.5, 1, 0.25, 0.25, 0.25, 0.01);
              if (++highlightedBlocks >= maxBlockMatches) break outer;
          }
      }
  ```
- **风险**：范围缩小影响玩法，需与设计对齐；粒子减少反馈变弱，可在消息里报总数。
- **验证**：build；多玩家同时释放用 `/forge track`/spark 观察主线程占用；未加载区块不触发加载日志。

### M9 冷却用 per-level gameTime，跨维度不同步
- **位置**：`cultivation/PlayerCultivation.java:160-171`；`network/ReleaseTechniquePacket.java:55,127`
- **根因**：`player.serverLevel().getGameTime()` 取**当前维度** gameTime；1.20.1 各 ServerLevel gameTime 独立递增。overworld 设冷却 `overworldTime+100`，进 nether 后 `netherTime` 可能远小于该值 → 冷却“永远未到”；反向则“立即过期”。
- **方案**：改全局统一时钟 `player.getServer().overworld().getGameTime()`（long，跨维度一致），或 `player.getServer().getTickCount()`（int，约 3 年安全）。推荐前者以兼容现有 long 字段。
- **代码草图**：
  ```java
  long nowTick = player.getServer().overworld().getGameTime();   // 全局，跨维度一致
  if (cultivation.getTechniqueCooldownUntilTick(techniqueId) > nowTick) { /* cooling; return; */ }
  ...
  cultivation.setTechniqueCooldown(techniqueId, nowTick + cooldownTicks);
  ```
- **风险**：旧存档 NBT 冷却值是旧维度 gameTime 不可比 → 一次性过期或永久冷却；需在 deserialize/首次登录清空旧冷却 map 或加版本标记。客户端若显示倒计时也须同步调整时钟来源。
- **验证**：build；overworld 释放→进 nether→同技能仍显示冷却中；nether 释放→回 overworld 同理；跨维度后正常到期。

### M10 消耗/境界靠文本关键字猜测
- **位置**：`network/ReleaseTechniquePacket.java:154-184`（estimateCost/estimateTechniqueRealm）；`cultivation/TechniqueDataManager.java:162-176,206`
- **根因**：`estimateCost` 用“剑”→35、“阵”→35、“符”→12 等 contains 猜；`estimateTechniqueRealm` 用“元婴”→NASCENT_SOUL 等猜。而 `TechniqueEntry` record（:206）仅 `(id,name,source,attribute,quality)`，**JSON schema 无 cost/realm 字段**；`SkillType` 枚举却有精确 `spiritualPowerCost`/`requiredRealm`。猜测值与 `SkillType`（FIREBALL=10/THUNDER=12/EARTH_ESCAPE=15）不符，且走火入魔判定（:70-82）基于猜测境界。
- **方案**：数据驱动。扩展 `TechniqueEntry` 增 `int cost`、`Realm requiredRealm`；`loadEntries` 解析新字段；6 个 technique JSON 追加 `cost`/`required_realm`；`estimateCost` 改 `technique.cost()`（fallback 15）、`estimateTechniqueRealm` 改 `technique.requiredRealm()`（fallback QI_REFINING）。
- **代码草图**：
  ```java
  // TechniqueDataManager
  public record TechniqueEntry(String id, String name, String source, String attribute,
                               int quality, int cost, Realm requiredRealm) {}
  // loadEntries:
  result.put(id, new TechniqueEntry(id, name, source, attribute, getInt(object,"quality"),
      getInt(object,"cost"), parseRealm(getString(object,"required_realm"))));
  private static Realm parseRealm(String id) {
      if (id == null || id.isBlank()) return Realm.QI_REFINING;
      try { return Realm.valueOf(id.toUpperCase(Locale.ROOT)); }
      catch (IllegalArgumentException e) { return Realm.QI_REFINING; }
  }
  // JSON: { "id":"fireball_art", ..., "cost":10, "required_realm":"QI_REFINING" }
  // ReleaseTechniquePacket:
  private static int estimateCost(ServerPlayer p, String id) {
      return TechniqueDataManager.getTechnique(p.getServer(), id).map(t -> t.cost() > 0 ? t.cost() : 15).orElse(15);
  }
  private static Realm estimateTechniqueRealm(TechniqueDataManager.TechniqueEntry t) { return t.requiredRealm(); }
  ```
- **风险**：需为 6 个 JSON 全部条目补字段（可写一次性脚本基于 SkillType techniqueId 映射）；record 加字段破坏现有构造调用（仅 loadEntries 一处，grep 同步）；`Realm` 导入到 TechniqueDataManager 无循环依赖。建议同步 bump 数据包版本/清缓存。
- **验证**：build；`/reload` 后火球术扣费应为 10（非猜测 15）；高阶功法（化神）在练气期释放应触发走火风险提示。

### M11 SyncCultivationDataPacket writeUtf 默认上限风险
- **位置**：`network/SyncCultivationDataPacket.java:163-168,185`
- **根因**：`spiritualRoot`/`spiritualRootAttributes`/`specialPhysique`/`auraNature` 均用无界 `writeUtf(String)`（默认上限 32767）。`spiritualRootAttributes` 由 `getSpiritualRootAttributeNames()`（PlayerCultivation:483-487）用 `/` 拼 displayName。当前短，但无显式上界保护；超长 encode 抛 `EncoderException` 整包失败 → 客户端永久 `!synced`。
- **方案**：对短文本用带显式上限的 `writeUtf(String,int)`，encode 前截断。上限：realm/stage/spiritualRoot/specialPhysique/auraNature=64，spiritualRootAttributes=256。
- **代码草图**：
  ```java
  private static final int SHORT_STR_MAX = 64, ROOT_ATTR_MAX = 256;
  private static String cap(String s, int max) { return s == null ? "" : (s.length() > max ? s.substring(0, max) : s); }
  // encode:
  buffer.writeUtf(cap(packet.realm, SHORT_STR_MAX), SHORT_STR_MAX);
  buffer.writeUtf(cap(packet.spiritualRootAttributes, ROOT_ATTR_MAX), ROOT_ATTR_MAX);
  buffer.writeUtf(cap(packet.specialPhysique, SHORT_STR_MAX), SHORT_STR_MAX);
  buffer.writeUtf(cap(packet.auraNature, SHORT_STR_MAX), SHORT_STR_MAX);
  // decode: readUtf() 无需改
  ```
- **风险**：截断丢尾部字符但 64/256 远超当前任何 displayName，实际不触发；**字段顺序/类型未变，无需 bump 协议**。
- **验证**：build；5 属性灵根玩家同步 HUD 正常；可选：临时拼超长字符串单测确认不抛异常。

### M12 CultivationHudOverlay 修为进度条硬编码 0.35
- **位置**：`client/CultivationHudOverlay.java:44-49`
- **根因**：HUD 缺 `cultivationMax` 同步字段。`PlayerCultivation.getCultivationMax()`（:81，返回 `getCurrentStageCapExp()`）已存在，但未进 `SyncCultivationDataPacket` 与 `ClientCultivationData.Snapshot`，HUD 只能 `cultivationCurrent>0?0.35:0.0` 假进度。
- **方案**：`SyncCultivationDataPacket` record 增 `long cultivationMax`；`from` 写 `getCultivationMax()`；encode/decode 配对加 `writeLong/readLong`；`Snapshot` 增 `long cultivationMax`（`empty()` 默认 1L 防除零）；HUD 改真实比值；**bump PROTOCOL_VERSION 4→5**。
- **代码草图**：
  ```java
  // record 新增 long cultivationMax（紧跟 cultivation）
  // from(): cultivation.getCultivation(), cultivation.getCultivationMax(), cultivation.getMana(), ...
  // encode: buffer.writeLong(packet.cultivationMax);
  // decode: buffer.readLong(),
  // Snapshot: long cultivationMax; empty() 默认 1L
  // HUD:
  double cultivationFraction = clamp01(fraction(cultivationCurrent, (int)Math.max(1, data.cultivationMax())));
  ```
- **风险**：协议必须同步 bump（旧客户端连新服务端被拒绝，期望行为）；`empty()` 默认不能为 0 否则 HUD 显示 0%。与 M11 可同批提交。
- **验证**：build；修为条随 cultivation 增长真实填充；突破后阶段 cap 重置时归零。

### M13 寿元耗尽 hurt(Float.MAX_VALUE) 误触打坐中断 + 走火检定
- **位置**：`event/ModEvents.java:645-648`（handleAgeAndLifespan）；`:175-186`（onLivingHurt 打坐分支）
- **根因**：`handleAgeAndLifespan` 用 `player.hurt(magic, Float.MAX_VALUE)` 处死寿元耗尽玩家，触发 `LivingHurtEvent` 进 `onLivingHurt`：若玩家正在打坐会 `addQiDeviationRisk(INJURED_MEDITATION_RISK)` + `stopMeditation` + `BreakthroughService.tryTriggerQiDeviation` —— 寿元天年之死不应触发走火。`Float.MAX_VALUE` 滥用 magic 源语义不清。
- **方案**：1) 寿元死亡设 PersistentData flag `SeekingImmortalsLifespanDeath`，`onLivingHurt` 顶部对该 flag early-return（跳过打坐中断与走火检定，仍允许伤害生效）；2) 伤害源改 `outOfWorld()`（绕护甲）或直接 `player.kill()`。
- **代码草图**：
  ```java
  // handleAgeAndLifespan:
  if (cultivation.isLifespanExhausted() && !player.isCreative() && !player.isSpectator()) {
      if (player instanceof ServerPlayer sp) sp.getPersistentData().putBoolean("SeekingImmortalsLifespanDeath", true);
      player.hurt(player.damageSources().outOfWorld(), Float.MAX_VALUE);
      player.displayClientMessage(Component.translatable("message.seeking_immortals.lifespan.exhausted"), false);
  }
  // onLivingHurt 顶部:
  if (hurtPlayer.getPersistentData().getBoolean("SeekingImmortalsLifespanDeath")) {
      hurtPlayer.getPersistentData().remove("SeekingImmortalsLifespanDeath");
      return;   // 寿元死亡无攻击者，combat hook 本就 return，无副作用
  }
  ```
- **风险**：`return` 跳过后续本 mod 订阅，寿元死亡无 sourceEntity 不影响 combat hook；flag 须在死亡后清除（已在 onLivingHurt remove，未死时 tick 末兜底 remove）。
- **验证**：build；设寿元到上限并打坐，耗尽致死时直接死亡，无走火入魔消息、无额外风险。

### M14 lifespanYears 旧存档回退到 QI_REFINING(100)，凡人应 80
- **位置**：`cultivation/PlayerCultivation.java:1212`
- **根因**：`lifespanYears = tag.contains("LifespanYears") ? tag.getInt(...) : Realm.QI_REFINING.getLifespanYears();`（=100）。缺失字段的旧存档若境界仍 MORTAL，寿元被错设为 100（凡人应 80）。此时 `realm` 已在 :1202 `loadRealmAndStage` 设定。
- **方案**：回退值改 `realm.getLifespanYears()`。
- **代码草图**：
  ```java
  lifespanYears = tag.contains("LifespanYears") ? tag.getInt("LifespanYears") : realm.getLifespanYears();
  ```
- **风险**：`realm` 在 :1202 已确定或 :1203 `updateRealmFromCultivationExp` 已重算；:1212 在其后执行，无冲突；非凡人旧存档回退仍正确。
- **验证**：build；构造无 LifespanYears 且 MORTAL 的旧 NBT，加载后 lifespanYears=80。

### M15 PillQuality 枚举不参与炼丹产物
- **位置**：`alchemy/AlchemyRecipe.java:14-32`；`block/entity/AlchemyFurnaceBlockEntity.finishCraft:94`
- **根因**：MVP 配方 id 形如 `foundation_building_pill_low`，`output` 直接指 `FOUNDATION_BUILDING_PILL_LOW` 等按品质分注册的物品；`finishCraft` `new ItemStack(recipe.output(), count)` 产物品质完全由配方决定，运行时无品质掷骰。`PillQuality`（LOW/MEDIUM/HIGH/SUPREME）只服务手动注册成品丹，与炼丹炉产出脱钩 → 同方永远只出下品。
- **方案**（关联 D6）：1) 配方去后缀化，`AlchemyRecipe.output` 改 `List<Item> outputsByQuality`（index=PillQuality.ordinal()）；2) `finishCraft` 成功分支基于 successMargin + 炼丹技能等级（H13 bonus）做品质掷骰；3) 旧 `_low` id 用 `findById` 别名兜底兼容存档。
- **代码草图**：
  ```java
  // AlchemyRecipe: output → outputsByQuality
  public record AlchemyRecipe(String id, Component displayName, List<Item> outputsByQuality,
      int outputCount, int manaCost, int cookTicks, double successRate, double explosionChance,
      List<IngredientRequirement> ingredients) {}
  // finishCraft 成功分支:
  if (roll < explosionChance + successRate) {
      Item out = pickQualityItem(recipe, serverLevel, successMargin, alchemySkillLevel);
      storedOutput = new ItemStack(out, recipe.outputCount());
  }
  ```
- **风险**：需先盘 `ModItems` 是否已注册 MEDIUM/HIGH/SUPREME 物品（若仅 `_LOW` 则掷骰取 null）；存档 recipeId 别名映射；与 D6 同批设计避免返工。
- **验证**：build；LV1 与 LV10 炼丹术各炼 20 炉筑基丹统计品质分布；旧存档炼丹炉加载后 recipeId 仍能 findById。

### M16 HealingPill/FastingPill/ClearSpiritPowder 未用 absorption
- **位置**：`item/pill/RejuvenationPill.java:13-31`（已用，基线）；`HealingPill.java:11-23`、`FastingPill.java:11-23`、`ClearSpiritPowder.java:12-23`（未用）
- **根因**：四个 `BasePillItem` 子类只有 `RejuvenationPill` 把 `getQuality().getEffectMultiplier()` 再乘 `getPillAbsorptionMultiplier(player)`，其余三个直接用品质倍率，忽略灵根吸收率。`BasePillItem.getPillAbsorptionMultiplier`（:49-53）已封装“低资质吸收率更高”规则。
- **方案**：在 `BasePillItem` 抽 protected helper `effectiveMultiplier(player)` 统一“疗效=基础×品质×吸收率”，三个子类改用之，`RejuvenationPill` 也改用消除重复。`ClearSpiritPowder`（布尔解毒）改为吸收率≥阈值额外清 1 个负面效果或附带短时抗毒。
- **代码草图**：
  ```java
  // BasePillItem
  protected double effectiveMultiplier(ServerPlayer player) {
      return getQuality().getEffectMultiplier() * getPillAbsorptionMultiplier(player);
  }
  // HealingPill.consumePill
  double mult = effectiveMultiplier(player);
  player.heal((float)(4.0 * mult));
  // FastingPill: foodLevel=round(10*mult); saturation=(float)(5.0*mult)
  ```
- **风险**：`getPillAbsorptionMultiplier` 无 capability 时返回 1.0 不会清零疗效；饱和度用 `Math.round` 防低吸收归零。
- **验证**：build；高/低资质灵根玩家服同品疗伤丹对比回血差异；辟谷丹对比饱食度增量。

---

## ⚪ 未实现 stub

> 模板：现状 / 建议方案 / 优先级。多数 stub 与上面 bug 交叉，已标注。

### S-战斗技能
- **筑基 6 技能全 stub**：`foundation_establishment_techniques.json` 含 17 条目但 `SkillEffectRegistry` 仅注册炼气期 spell。建议逐个实现 `SkillEffect`（多发飞剑复用 `SwordProjectileEntity`、防御墙用临时方块+定时移除参考 EarthWall 修复、wind_binding 用 SLOWDOWN+leash）。**须先修 H4** 否则 stub 释放=白扣费。优先级中。
- **VINE_BIND/METAL_BLADE/SOUL_SEARCH/FIVE_ELEMENT_ROTATION/SWORD_FORMATION 无 SkillEffect**：`SkillType` 含但 registry 未注册 → 释放走 H4 白扣费。短期靠 H4 修复后“未实现即拒绝释放”止血；完整实现按 Phase 排期。优先级高（配合 H4）。
- **EarthWallSpell 放 STONE 永不移除**（`skill/effect/spell/EarthWallSpell.java:27`）：无定时移除 → 永久改地形 + 刷石漏洞。建议 200 tick 后 `level.removeBlock` 配合 `level.scheduleTick`，或定义 `EarthWallBlock extends Block` 带 BE 自动消失。**属经济/世界完整性 bug，非纯 stub**，优先级高。
- **InvisibilitySpell/LightBodySpell 无打破隐身逻辑**：隐身玩家可攻击仍隐身。建议 `onLivingHurt`/`AttackEntityEvent` 玩家造成伤害时 `removeEffect(INVISIBILITY)`；轻身术位移 buff 无需打断。优先级中。
- **神识消耗未实现**：`SkillEffect` 有 `default getDivineConsciousnessCost(level){return 0;}`，`ReleaseTechniquePacket` 只扣灵力。建议扣灵力后追加 `dcCost` 检查与 `consumeDivSense`（DETECTION/SOUL_SEARCH 返回非 0）。优先级中。
- **PvE 不经 cultivation 计算**：`onLivingHurt:205-218` `CombatCalculator` 仅 PvP。建议扩展 `event.getEntity() instanceof LivingEntity`，怪物 stats 用 fallback vanilla-derived；或 PvE 简化只保留 multiplier+暴击。须先修 H9。优先级中。
- **CombatStats 未接灵根/体质/功法**（`combat/CombatStats.java:15-36`）：仅按 realmOrdinal 线性推导。建议追加灵根纯度→攻击%/暴击%、SpecialPhysique→防御/血、攻击型 technique→攻击倍率；`PlayerCultivation` 暴露聚合方法。优先级中低。
- **技能图标为 hash 着色占位**：为每 SkillType/techniqueId 制 16×16 PNG 放 `assets/seeking_immortals/textures/gui/skill/<id>.png`，overlay 查贴图回退色块。优先级低。
- **getJadeVialDropChance 仅透传**（`cultivation/SpiritualRoot.java:79`）：无 JadeVialItem/掉落事件/灵液系统。Phase 5/6 实现怪物死亡按概率掉落 + 灵液催熟。优先级低（未排期）。

### S-炼丹
- **炼丹技能 LV1-10**（H13 延伸）：H13 接通等级→概率；`finishCraft` 成功后 `addSkillExperience(SkillType.ALCHEMY, xp)`；补解锁入口（功法书/命令）。优先级高（闭环炼丹）。
- **炼丹 GUI/Container/Menu**：当前 `interact` 右键+actionbar 消息。MVP 可不引入完整 Container；若做需 `AlchemyFurnaceMenu`+`Screen`+`ModMenuTypes` 注册，可能 bump 协议。优先级中。
- **炼丹炉客户端 ticker/动画/粒子/进度同步**：`getTicker` 客户端返回 null，无 `LIT` 属性。建议加 `BooleanProperty LIT` + 客户端轻量 ticker 播粒子 + `getUpdateTag/getUpdatePacket` 同步 progressTicks。优先级中。
- **大量 PillType 无物品与配方**：MVP 仅 4 配方。建议先数据驱动改造（`MVP_RECIPES` 迁 JSON + loader），再按境界分批补。优先级中低。
- **废丹无回收/用途**：`WASTE_PILL` 仅产出无消耗处。建议加副作用（服后降修为/增风险）或回收配方（N 废丹+灵石→部分材料），或 D8 定调后爆炉不产废丹。优先级低。

### S-物品/方块
- **MysticVialItem 绑定未生效**（`item/MysticVialItem.java:147-151` 有 isOwner/setOwner 但 `use`/`useOn` 不校验，setOwner 无调用点；死亡掉落/放箱子/熔炼未阻止）：首次 use 绑定 owner；入口校验 isOwner；死亡时 `inventory.add` 归还；`FurnaceFuelBurnTimeEvent` 阻止熔炼。优先级中。
- **FlyingArtifactItem 无装备回调**：纯 tick 轮询 Curios 槽，无槽位限制。可接受轮询（已工作）；槽位限制需 `ICurio`+`getSlot`。MVP 可不改。优先级低。
- **两实体无 spawn egg**：`cushion_seat`/`sword_projectile` 均非自然生成（由方块/技能生成），spawn egg 无实际用途。**建议不修**，标记设计如此。
- **飞行 respawn/dimension/死亡清理**：见 H11，已含完整方案。优先级高（与 H11 合并）。
- **showMeditationStatus/describeSpiritLand 死代码**（`event/ModEvents.java:446-464`）：全仓无调用方。建议删除两方法及未用 import（`describeSpiritLand` 若计划用于未来 HUD 可保留）。优先级低。

### S-UI/同步
- **CultivationHudOverlay 真实百分比**：即 M12，已含方案。
- **CultivationStatsScreen 无分页/滚动**（`client/CultivationStatsScreen.java:181-194,231-234`）：低 GUI scale 下负面状态被截断。建议加垂直 `scrollOffset`+`mouseScrolled`+clip，或翻页子界面；建议单页整体加滚动。优先级中。
- **7 释放键无 KeyConflictContext**（`client/ClientEvents.java:57-63`）：默认 UNIVERSAL。建议五参构造设 `KeyConflictContext.IN_GAME`，统一 MEDITATE/BREAKTHROUGH/RELEASE/OPEN_EDIT。优先级低-中。

---

## ❓ 未决设计点 D1–D14

> 模板：现状 / 建议 / 理由。每条给出明确推荐以解除“未决”。

### D1 两套飞行如何统一
- **现状**：`FlightProfile.forRealm(QI_REFINING)` 返回 null（禁飞），但御剑飞行术是练气期技能（`handleQiFlying` 校验 `realm==QI_REFINING`）——练气靠御剑可飞，法宝飞行要筑基+。两套互斥未定义：筑基玩家同时装法宝+激活御剑，两套 grant 并存（H1 根因）。
- **建议**：方案 B 共存 + 引用计数（按 H1 的 `FlyingAuthority` 多源共存，speed 取较高，灵力双倍消耗），符合“法宝是御剑上位替代”直觉。给 QI_REFINING 一个受限 profile 供御剑复用（当前 null 致法宝路径禁飞但御剑独立 mayfly，逻辑割裂）。

### D2 飞行高度限制
- **现状**：硬上限撤销（且有 H2 方向 bug）。
- **建议**：保留硬上限并修正方向（H2），后续可加软限制——接近上限时 verticalSpeed 衰减而非瞬间撤销。MVP 先做 H2 硬修复。

### D3 走火风险衰减机制
- **现状**：tickCount 取模（几乎不触发，见 M3）。
- **建议**：累计打坐 tick 计数器（见 M3 方案）。语义正确（按平稳打坐时长）、抗错过、可持久化、与 `tickCultivationBoost`/`heartDemonTriggerTicks` 模式一致。不推荐继续取模。

### D4 中性灵石是否被动
- **现状**：代码无中性灵石注册，`getMatchingPassiveBonus` 对非五行灵根返回 0；CLAUDE.md 称中性灵石有 passive bonus（见 M4）。
- **建议**：定调中性灵石=通用加成（任何灵根可用，bonus 较低），五行灵石=匹配灵根加成（bonus 较高）。注册中性灵石系列并修 `getMatchingPassiveBonus`；若策划不想要中性石则修订 CLAUDE.md。

### D5 丹药 boost 叠加规则
- **现状**：取 max 不续期（见 M1）。
- **建议**：续期 + 取较大 multiplier，设上限防滥用（见 M1）。玩家重复服丹有正反馈（时长累加），multiplier 不无限叠（取 max 防数值膨胀）。

### D6 品质机制
- **现状**：注册侧“每品质独立物品”（`_LOW/MEDIUM/HIGH/SUPREME` 分开），运行时产出却只走 `_LOW`，两套并存未打通（见 M15）。
- **建议**：保留“每品质独立物品”为主方案——创造栏/JEI/堆叠/贴图/tooltip 开箱即用，`BasePillItem` 构造已携 `PillQuality`，逻辑最干净。`AlchemyRecipe.output` 改 `List<Item> outputsByQuality`，`finishCraft` 按 `PillQuality.ordinal()` 取。NBT 方案仅当品质档位>4 才考虑。

### D7 丹方匹配
- **现状**：`AlchemyRecipe.findByHeldIngredient`（:34-51）if-else 硬编码优先级，灵草同时是炼气丹(2)和筑基丹(4)输入时持灵草必匹配炼气丹；`startRecipe` 后直接消耗无二次确认。
- **建议**：短期维持 if-else 但“独有材料优先”（龙血草只出现在筑基丹→持龙血草必筑基丹），通用材料回落时按“库存能完整满足的配方”过滤而非固定顺序；中期命中多候选时不直接扣料，发候选列表消息或“潜行右键=第二候选”手势。不引入完整配方 UI 作为 MVP 阻断项。

### D8 爆炉/废丹/成功概率边界
- **现状**：`finishCraft`（:85-99）三分支材料已在 `startRecipe`→`consumeInputs`（:63）提前扣；爆炉 `reset`+`destroyBlock`+`explode`；废丹产 `WASTE_PILL`。文档未明确材料消耗时机。
- **建议**：1) 材料扣减时机不变（投入即烧，防白嫖成功率）；2) 爆炉销毁方块+不产废丹+小范围伤害；3) 普通失败保炉产废丹（软/硬失败两级）；4) **概率归一化**：`if (exp+suc>1.0) suc=1.0-exp;` 防废丹分支永不触发（真实潜在 bug，顺带修）；5) 文档化到 `project_docs/features.md`。

### D9 estimateCost/estimateTechniqueRealm
- **现状**：文本启发式（见 M10）。
- **建议**：功法 JSON 显式 `required_realm`/`cost` 字段（见 M10 方案），消除魔法字符串。

### D10 SkillEffect.canExecute 语义
- **现状不一致**：`canExecute` 默认 true（SkillEffect.java:16），但部分 spell 的 execute 仍可能 false（EarthEscape 地形、FlyingSword 灵力），失败条件未对齐 → canExecute=true 但 execute=false 时扣费不退（H5）。
- **建议**：明确契约 `canExecute` 必须覆盖 execute 所有可能失败条件（灵力/地形/目标存在性），保证 `canExecute==true→execute==true`；spell 内部灵力检查移到 `canExecute`；packet 层据此采用 execute-first。`canExecute` 为“前置只读检查”（无副作用），`execute` 假定前置已过则必成功（仅防御性 false）。需 audit 每个 spell execute 失败路径迁移到 canExecute，一次性收益大。

### D11 INVISIBILITY/LIGHTNESS_SKILL/EARTH_WALL 解锁时机
- **现状**：`SkillType` 中三者用短构造器（techniqueId=""），`isPhase4QiSkill()` 判 `techniqueId!=null&&!isBlank` 返回 false，不参与 `unlockEligiblePhase4Skills` 自动解锁，也无 technique manual 关联。
- **建议**：方案 A 数据驱动——补 techniqueId（`invisibility_art`/`lightness_art`/`earth_wall_art`），对应 JSON 加 entry，改长构造器带 realm/stage/cost/cooldown，随 phase4 自动解锁且可功法书学习（与 FIREBALL/ICE_CONE/THUNDER 一致）。需确认 `getConfiguredSpiritualPowerCost`/`getConfiguredCooldownTicks` 调用方不受影响。

### D12 PROTOCOL_VERSION="4" vs CLAUDE.md "2"
- **现状**：`ModNetwork.java:9` 实际 `"4"`；CLAUDE.md 记载 "2"。文档 drift，非代码 bug。
- **建议**：CLAUDE.md 更新为 4，加维护规则“每次 packet 字段顺序/类型变化必须 bump 并同步 CLAUDE.md”。若实施 M12（新增 cultivationMax）bump 至 5 并一并更新。

### D13 pendingMeditating 乐观锁
- **现状**：`ClientCultivationData` 维护 `pendingMeditating`，`setSnapshot` 收服务端同步后清空。竞态：连按两次 V 快速切换时中间状态可能与服务端不一致，HUD 短暂闪烁；`CultivationHelper.get` 失败时无回包 pending 永悬。
- **建议**：保留乐观锁（打坐低频非战斗，UI 即时反馈 > 网络延迟；`SetMeditatingPacket.handle:36` 已是立即 ACK 语义）。改进点：`SetMeditatingPacket.handle` 在 `CultivationHelper.get` 失败或状态不变时也强制回 `SyncCultivationDataPacket` 兜底（加 `else { send; }`）。优先级低。

### D14 两套境界数值表
- **现状**：`Realm.java:5-15` 枚举含 baseMaxSpiritualPower/stageExpSpan/lifespanYears；`RealmStageConfig.java:16-30` `getManaBase` 重复完全相同的灵力基准表，`getDivSenseBase`（:37-51）与 `PlayerCultivation.getMaxDivineConsciousness`（:677-692）内联 switch 也完全重复。三处定义同一组数值，改一处易漏。
- **建议**：单一真源——以 `Realm` 枚举为权威，把神识/HP 基准也并入构造参数（`baseDivSense`/`baseHp`），删 `RealmStageConfig` 重复表改委托 `realm.getBaseDivSense()`/`getBaseHp()`，`PlayerCultivation.getMaxDivineConsciousness` 内联 switch 改用之。`RealmStageConfig` 独有的派生率表（manaRecovery/cultivationGain/flyingSpeed）可保留。建议与 H6 修复同批完成避免数值漂移。迁移成本中等。

---

## 📍 优先级与落地顺序

**最该先修（高确定性局部修复，影响存档/玩法正确性）：**
1. **H2** 飞行高度方向反（一行判断）+ **H11** mayfly 残留（加 respawn/dimension/death revoke）+ **H1** 两套飞行引用计数 —— 防永久飞行
2. **H3** MysticVial NPE —— 防崩服
3. **H4/H5** ReleaseTechniquePacket 静默扣费 —— 防白扣灵力（H5 顺带定 D10 契约）

**第二批（玩法正确性 + 经济/世界完整性）：**
4. **H8** EXTREME 掉落 100%→50%；**H6** 凡人灵力/重伤；**H7** risk=100 必触发；**H13** 炼丹技能接通
5. **H9** 飞剑伤害二次重算；**M7** 土遁穿墙；**EarthWall 永久地形**（刷石漏洞，stub 中的高优先）
6. **M3/M13/M14** 走火衰减/寿元死亡/寿元回退；**M1/M2** 丹药分级；**M5** 村民交易；**M15/M16** 品质/吸收率

**第三批（数据驱动重构 + 同步链路，工作量较大，单独 PR）：**
7. **M10** technique JSON 显式 cost/realm（改 schema + 6 文件）
8. **M9** 冷却全局时钟（含旧存档迁移）；**M12** cultivationMax 同步（bump 协议 4→5，含 D12 文档）；**M11** writeUtf 上限（与 M12 同批）；**H10** 客户端槽位保留
9. **M8** DetectionSpell 性能；**M6** 灵石堆叠；**M4** 中性灵石（含 D4 定调）

**第四批（stub/设计整改，按 Phase 排期）：**
10. VINE_BIND 等拒绝释放止血（随 H4）→ 逐个实现 SkillEffect
11. 炼丹闭环（H13 + S 炼丹技能/客户端 ticker/废丹）
12. **D6/D7/D8** 炼丹品质/丹方匹配/概率归一化（顺带修 D8 归一化 bug）
13. **D14** 境界数值表单一真源；**D10/D11** canExecute 契约/解锁时机；**D1/D2/D3/D5** 飞行/走火/丹药设计定调

**协议版本影响汇总**：H4/H5/H6/H7/H8/H9/H13 等多数为服务端逻辑或客户端逻辑，**无需 bump**；**M12**（新增 cultivationMax 字段）须 bump `PROTOCOL_VERSION` 4→5 并同步 CLAUDE.md；M11 可与 M12 同批不额外 bump。

---

**文档版本**：v1.0
**生成方式**：5 路并行实读源码核对（cultivation / skill+combat / alchemy / network+client / event+flight+items）
**未执行**：未改任何源码、未 commit、未运行 `gradlew build`（纯方案）
