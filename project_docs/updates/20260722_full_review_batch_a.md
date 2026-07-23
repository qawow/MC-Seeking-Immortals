# 全量代码审查修复 — Batch A：HIGH 安全与经济修复

**完成时间**: 2026-07-22
**版本**: 0.2.162 → 0.2.163
**变更类别**: 代码（Java + 本地化资源）

---

## 背景

对全代码库（596 个 Java 文件 / 约 13.4 万行）进行了分 7 个子系统的并行审查，发现 8 个 HIGH、约 15 个 MEDIUM、约 12 个 LOW 级问题。本次为第一批：4 个 HIGH 级安全与经济修复。

---

## 修复内容

### #3 技能 NBT 加载无边界检查（安全）
**文件**: `skill/CultivationSkill.java`
**问题**: `loadNBT` 读取 `Level`/`Experience`/`Proficiency` 时不做边界检查。被篡改或损坏的 capability 文件可把 level 设为 `Integer.MAX_VALUE`，使 `getEffectivenessMultiplier()` 产生约 3.2e8 的倍率，一击秒杀任何目标。
**修复**: 加载时夹取 `level ∈ [0, maxLevel]`、`experience ≥ 0`、`proficiency ∈ [0, 10000]`。

### #4 术法释放免费施法（经济/安全）
**文件**: `network/ReleaseTechniquePacket.java`
**问题**: 灵力在 `effect.execute()` 成功**之后**才扣除；若 execute 内部消耗了灵力导致随后 `consumeSpiritualPower` 失败，方法直接返回且不设冷却，造成可免费重复施法。
**修复**: 改为原子性扣费——在 execute 之前扣除灵力，execute 失败或符箓预留失败时退还（`addSpiritualPower(cost)`）。主施法与双施法（tryDualCast）两条路径同步修复。扣费位置放在过阶走火入魔风险检查之后，避免走火入魔触发时白扣费。

### #5 创造模式消耗丹药（经济）
**文件**: `item/pill/BasePillItem.java`
**问题**: `use()` 无条件 `stack.shrink(1)`，没有 `instabuild` 判断，创造模式玩家也会消耗丹药（符箓/CatalogPillItem 均有判断）。
**修复**: 将 `shrink` 包在 `if (!player.getAbilities().instabuild)` 中。

### #6 神秘小瓶归属绑定未生效（安全）
**文件**: `item/MysticVialItem.java`
**问题**: `isOwner()` 已实现但在 `use()`/`useOn()` 中从未调用，文档所述的玩家绑定不生效，任何玩家都能使用/吸取他人的灵液瓶。
**修复**: 在 `use()` 与 `useOn()` 开头拒绝非绑定玩家。新增本地化键 `message.seeking_immortals.mystic_vial.not_owner`（中英）。

---

## 修改文件清单

1. `gradle.properties` — mod_version 0.2.162 → 0.2.163
2. `src/main/java/com/xunxian/seekingimmortals/skill/CultivationSkill.java`
3. `src/main/java/com/xunxian/seekingimmortals/network/ReleaseTechniquePacket.java`
4. `src/main/java/com/xunxian/seekingimmortals/item/pill/BasePillItem.java`
5. `src/main/java/com/xunxian/seekingimmortals/item/MysticVialItem.java`
6. `src/main/resources/assets/seeking_immortals/lang/zh_cn.json`
7. `src/main/resources/assets/seeking_immortals/lang/en_us.json`

## 备份路径

`.bak/20260722_batch_a/`（含上述 6 个源/资源文件的原始副本）

## 验证结果

`./gradlew build` — BUILD SUCCESSFUL（2m 21s），preflight 记录 mod_version=0.2.163。

## 版本与协议

mod_version 0.2.162 → 0.2.163。未改动任何网络包字段/顺序/编码，`ModNetwork.PROTOCOL_VERSION` 不变。
