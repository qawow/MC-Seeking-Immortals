# 2026-07-21 客户端启动崩溃修复（puppet_repair_kit 注册失败）

## 崩溃现象

用户报告：`报错/错误报告-2026-7-21_3.55.50.zip`

- 版本：`seeking_immortals-0.2.124.jar`（Forge 47.4.20，MC 1.20.1）
- 阶段：游戏初始化（物品注册事件）即崩溃，无法进入主界面
- 关键堆栈：

```
Caused by: java.util.NoSuchElementException: No value present
    at java.util.Optional.orElseThrow(Optional.java:377)
    at com.xunxian.seekingimmortals.registry.ModItems.lambda$static$172(ModItems.java:729)
```

## 根因分析

`ModItems.java:729` 是 0.2.118 新增的注册：

```java
BulkItemClassifier.consumable("puppet_repair_kit").orElseThrow()
```

`BulkItemClassifier.consumable(id)` 的第一道闸门是 `EXECUTABLE_CONSUMABLE_IDS` 白名单——
`puppet_repair_kit` **不在该集合中**，方法直接返回 `Optional.empty()`，注册 lambda 的
`orElseThrow()` 抛出 `NoSuchElementException`，导致整个 mod 注册阶段失败。

为何构建/测试没拦住：注册 lambda 只在真实游戏启动（RegisterEvent 分发）时执行，
`./gradlew build` 的单元测试不会触发 Forge 注册事件，因此 0.2.118~0.2.124 均带病通过构建。

同批注册的 `spirit_beast_feed` / `beast_feed_spirit` 恰好在白名单里（早期批量物品遗留），
所以只有 `puppet_repair_kit` 爆雷。

## 修复内容

`BulkItemClassifier.java`：
1. `EXECUTABLE_CONSUMABLE_IDS` 加入 `"puppet_repair_kit"`；
2. 目录缺行合成分支（catalog row 缺失时）与 `executableEffect` 专用分支均映射
   `puppet_repair_kit -> "puppet_repair"`，保证无论 `consumables_index.json` 是否含该行都可解析。

`CatalogConsumableService.java`：
3. `use()` 新增 `case "puppet_repair" -> SummonHonestMvpService.repairOwnedPuppets(player) > 0;`
   —— 复用既有傀儡修理逻辑（修理全部自有傀儡 + 短暂减伤 + 成长记录）；
4. `shouldConsumeOnSuccess` 排除 `puppet_repair`：`repairOwnedPuppets` 内部的
   `consumeRepairMaterial` 已优先消耗修缮包本体，外层再 shrink 会双重扣减。

本地化（zh_cn / en_us）：
5. 新增 `tooltip.seeking_immortals.catalog_consumable.effect.puppet_repair`。

回归测试 `ModBulkItemsTest.java`：
6. 断言三个启动注册物品（`puppet_repair_kit` / `spirit_beast_feed` / `beast_feed_spirit`）的
   `consumable(...).orElseThrow()` 均可解析且 effect 正确——今后任何人从白名单移除这些 ID，
   测试即失败，不必等到游戏启动才发现。

## 验证结果

- `./gradlew build`：BUILD SUCCESSFUL（含全部单元测试）
- 独立复现脚本：对三个 ID 逐一执行与 ModItems 注册 lambda 相同的
  `BulkItemClassifier.consumable(id).orElseThrow()` 调用链，全部通过：
  - `puppet_repair_kit -> puppet_repair`
  - `spirit_beast_feed -> pet_loyalty_plus`
  - `beast_feed_spirit -> pet_loyalty_plus`

## 版本与协议

- `mod_version`: 0.2.124 → **0.2.125**
- `ModNetwork.PROTOCOL_VERSION`: 7（无网络包变更，不需 bump）

## 备份

`.bak/20260721_crash_fix/`（BulkItemClassifier.java、CatalogConsumableService.java、
SummonHonestMvpService.java、zh_cn.json、en_us.json、gradle.properties）

## 遗留风险

- `beast_feed_spirit` 与 `spirit_beast_feed` 是同义双 ID，效果一致；后续可考虑合并为单一物品并保留别名解析。
- 请用户使用 0.2.125 构建产物重新启动验证；旧 0.2.124 jar 必定复现该崩溃。
