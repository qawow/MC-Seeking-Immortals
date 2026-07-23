# 全量代码审查修复 — Batch G：LOW 加固与文档更新

**完成时间**: 2026-07-22
**版本**: 0.2.168 → 0.2.169
**变更类别**: 代码（Java）+ 文档（CLAUDE.md）

---

## 已修复（代码）

### 灵根属性空集合崩溃（加固）
**文件**: `cultivation/PlayerCultivation.java`
**问题**: `getSpiritualRootAttribute()` 直接 `iterator().next()`，若集合为空会抛 `NoSuchElementException`（当前仅因加载/设置路径保证非空而安全）。
**修复**: 集合为空时返回默认属性 `WOOD`。

### 暴击/闪避上限不一致（平衡）
**文件**: `combat/CombatStats.java`
**问题**: `CombatStats` 把暴击夹到 0.75、闪避夹到 0.50，而 `PlayerCultivation.getCriticalRate/getDodgeRate` 已分别夹到 0.80/0.75，二次夹取静默覆盖了修炼层调参。
**修复**: 上限对齐为 0.80/0.75。

### 阶段标记查询大小写失配（加固）
**文件**: `phase/SoftPhaseShellService.java`
**问题**: `isMarked()` 直接读原始 `phaseId`，而 `mark()` 存储时做了 `trim().toLowerCase()`，混合大小写/带空白的查询会失配。
**修复**: `isMarked()` 同样做 `trim().toLowerCase()` 归一化。

### 客户端收包反射崩溃（加固）
**文件**: `network/ClientPacketDispatch.java`
**问题**: 客户端处理器缺失/签名漂移时把 `ReflectiveOperationException` 重新抛为 `RuntimeException`，导致收包即崩溃客户端。
**修复**: 改为记录 WARN 日志并吞掉异常，优雅降级。

### 蒲团座位 cushionPos 未同步（同步）
**文件**: `entity/CushionSeatEntity.java`
**问题**: `cushionPos` 从未同步（`defineSynchedData` 为空），客户端经 `(type, level)` 工厂构造时 `getCushionPos()` 返回 `BlockPos.ZERO`。当前消费者均在服务端故可用，但未来任何客户端使用都会静默失效。
**修复**: 改用 `EntityDataAccessor<BlockPos>` 同步，`getCushionPos()` 双端一致。

---

## 已修复（文档 CLAUDE.md）

- `PROTOCOL_VERSION` 由 `7` 更正为 `30`（实际值）。
- `BreathingHudOverlay` 更正为 `CultivationHudOverlay`（实际类名）。
- “Meditation key: V” 更正为“无按键，右键蒲团触发、移动停止”（实际行为）。
- 战斗“已知风险”一节更新：三条历史风险均已解决（PvP 已改用 `LivingHurtEvent`；`getCombatStats` 返回 `Optional` 安全兜底）。
- “Current version: 0.1.80” 更正为“见 gradle.properties 的 mod_version”（避免再次漂移）。

---

## 审查后保留（记录决策，未改动）

- **NPC 驻留锚点**（`QuestNpcEntity` 等）：`tick()` 中的惰性锚点是自然生成 NPC 的合理兜底，且 `homePos` 已持久化；移除会使自然生成 NPC 失去归巢参考，故保留。
- **`addCultivationExp` 二次缩放**：已有 `addCultivationExpRaw` 原始变体，属命名易踩坑而非 bug，保留。
- **命中率 1% 下限**（`CombatCalculator` accuracy 夹到 0.99）：设计选择，保留。
- **`SpellEffect.ACTIVE_POWER_SCALE` ThreadLocal**：轻微每线程泄漏，且已部分由 `SkillContext.powerScale` 传参替代，保留。
- **`SeekingStatusEffect.getDefenseMul` 死 getter**：防御实际由构造器的属性修改器消费，getter 仅为潜在误导，保留。

---

## 修改文件清单

1. `gradle.properties` — mod_version 0.2.168 → 0.2.169
2. `src/main/java/com/xunxian/seekingimmortals/cultivation/PlayerCultivation.java`
3. `src/main/java/com/xunxian/seekingimmortals/combat/CombatStats.java`
4. `src/main/java/com/xunxian/seekingimmortals/phase/SoftPhaseShellService.java`
5. `src/main/java/com/xunxian/seekingimmortals/network/ClientPacketDispatch.java`
6. `src/main/java/com/xunxian/seekingimmortals/entity/CushionSeatEntity.java`
7. `CLAUDE.md`

## 备份路径

`.bak/20260722_batch_g/`

## 验证结果

`./gradlew build` — BUILD SUCCESSFUL（1m 19s），preflight 记录 mod_version=0.2.169。

## 版本与协议

mod_version 0.2.168 → 0.2.169。未改动网络包字段/顺序/编码，`ModNetwork.PROTOCOL_VERSION` 不变。
