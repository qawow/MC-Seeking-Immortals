# 全量代码审查修复 — Batch B：HIGH 性能与衰减修复

**完成时间**: 2026-07-22
**版本**: 0.2.163 → 0.2.164
**变更类别**: 代码（Java）

---

## 修复内容

### #1 功法数据每次调用全量重解析（性能）
**文件**: `cultivation/TechniqueDataManager.java`、`event/ModEvents.java`
**问题**: `getTechnique()` 对每个非内置 id 都调用 `loadTechniques(server)`，后者每次都会重新列举并解析所有 `data/.../cultivation/*.json`。该方法被每次施法（`ReleaseTechniquePacket.tryDualCast` 循环 7 槽位）与每秒同步（`ModEvents.getBestMeditationTechniqueMultiplier`）调用，大型数据包下每次施法/同步都变成一次完整资源重载。
**修复**: 引入按 server 实例键控的 `WeakHashMap` 缓存（`SERVER_TECHNIQUE_CACHE`），`loadTechniques` 走 `computeIfAbsent`；新增 `invalidateCache(server)` 与 `invalidateAllCaches()`。在 `ModEvents` 中：`ServerStoppedEvent` 清空缓存，`AddReloadListenerEvent` 注册一个轻量监听器在数据包重载（`/reload`）后丢弃缓存，保证数据包改动仍能被拾取。

### #2 走火入魔风险衰减比文档慢 20 倍（逻辑）
**文件**: `cultivation/PlayerCultivation.java`
**问题**: 衰减阈值 `QI_DEV_RISK_DECAY_TICKS = 720 * 20`、`LEYLINE_RISK_DECAY_TICKS = 360 * 20`（注释称“每 720/360 秒 -1”），但 `tickQiDeviationDecay` 在 `ModEvents` 中位于 `tickCount % 20 != 0` 早退之后，实际每秒才调用一次。累加器以“秒”计数却用“tick”阈值，导致每 -1 实际需 14400 秒（4 小时），风险几乎永不衰减。
**修复**: 阈值改为 `720` 与 `360`（秒），与每秒一次的调用频率匹配，符合注释语义。

---

## 修改文件清单

1. `gradle.properties` — mod_version 0.2.163 → 0.2.164
2. `src/main/java/com/xunxian/seekingimmortals/cultivation/TechniqueDataManager.java`
3. `src/main/java/com/xunxian/seekingimmortals/cultivation/PlayerCultivation.java`
4. `src/main/java/com/xunxian/seekingimmortals/event/ModEvents.java`

## 备份路径

`.bak/20260722_batch_b/`

## 验证结果

`./gradlew build` — BUILD SUCCESSFUL（1m 30s），preflight 记录 mod_version=0.2.164。

## 版本与协议

mod_version 0.2.163 → 0.2.164。未改动网络包字段/顺序/编码，`ModNetwork.PROTOCOL_VERSION` 不变。
