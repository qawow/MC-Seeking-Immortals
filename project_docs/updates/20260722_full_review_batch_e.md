# 全量代码审查修复 — Batch E：MEDIUM 战斗/性能/商店

**完成时间**: 2026-07-22
**版本**: 0.2.166 → 0.2.167
**变更类别**: 代码（Java）

---

## 已修复

### 商店限量库存重启后丢失（持久化）
**文件**: `shop/ShopService.java`、新增 `shop/ShopStockSavedData.java`
**问题**: 全局限量库存与刷新计时仅存于内存 static `STOCK_CACHE`，从不写入 `SavedData`，服务器每次重启/重载后所有商店库存都静默补满。
**修复**: 新增 `ShopStockSavedData`（overworld `SavedData`，仿照 `AuctionHouseSavedData` 模式）持久化每格库存的 `remaining` 与 `nextRefreshGameTime`。`reserveStock`/`stockPreview` 首次访问时从持久化存储加载状态，库存变化（购买扣减、定时刷新）后写回。内存缓存保留作为热路径，重启后从磁盘恢复。

---

## 审查后保留（记录决策，未改动）

### 伤害管线钩子（DamagePipelineHooks）
审查报告称 `PRE_HOOKS`/`POST_HOOKS` 从未注册、每次 PvP 命中都遍历空列表并分配 `DamageContext`。经核查：该钩子是**有测试覆盖的扩展点**（`M14CombatStatusTest` 注册钩子并验证行为），为 M15 法宝 / M07 阵法场预留。每次命中的开销仅为一次小对象分配，可忽略。删除会破坏测试与扩展契约，故保留。

### 每秒同步（SyncCultivationDataPacket）
审查报告称每秒为每个玩家重建并发送完整快照（含 33×13×33 灵气扫描）。经核查：这是为 HUD 响应性（打坐进度、冷却、灵气）的刻意设计；灵气扫描每玩家每秒仅一次，在常规服务器规模下可接受。降低频率会破坏打坐进度/冷却显示，故保留现状。如未来出现大型服务器性能问题，可考虑“仅变化时发送 + 每玩家每 tick 缓存灵气扫描”。

### 伤害倍率叠加顺序
审查报告指出输出/承伤倍率在管线前应用到 `event.amount`，导致暴击叠在已放大数值上。经核查：暴击放大“含增益后的总输出伤害”是常见且通常符合预期的设计（增益与暴击乘法叠加）。语义不明确、非明确 bug，故保留现状，留待与策划确认期望的叠加顺序。

---

## 修改文件清单

1. `gradle.properties` — mod_version 0.2.166 → 0.2.167
2. `src/main/java/com/xunxian/seekingimmortals/shop/ShopService.java`
3. `src/main/java/com/xunxian/seekingimmortals/shop/ShopStockSavedData.java`（新增）

## 备份路径

`.bak/20260722_batch_e/`（ShopService.java 原始副本；ShopStockSavedData 为新增文件）

## 验证结果

`./gradlew build` — BUILD SUCCESSFUL（1m 21s），preflight 记录 mod_version=0.2.167。

## 版本与协议

mod_version 0.2.166 → 0.2.167。未改动网络包字段/顺序/编码，`ModNetwork.PROTOCOL_VERSION` 不变。
