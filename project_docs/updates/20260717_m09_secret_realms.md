# 2026-07-17 M09 秘境与副本落地

## 变更摘要

落地 19 个作者秘境主数据深潜运行时、7 入口门绑定、Boss/守卫刷点、首通/重复掉落红线、通关事件钩子、限时/人数/死亡遣返 SavedData，以及消费 M07 场效果的机关布置。

## 实现要点

1. **主数据 / 深潜**
   - 发布 `worldpack/secret_realm_runtime.json`（19 境：楼层、机关 field_kind、Boss、时限、人数、gate）
   - 发布 `worldpack/boss_loot_runtime.json` + `loot_tables/chests/boss_*.json`
   - 扩量 `secret_realm_flavor.json`（≥19）并同步 `text_material/secret_realms.json` schema12
   - `WorldpackDataService` 主表补齐 6 个缺失作者境，合计 ≥19

2. **服务**
   - `SecretRealmCatalogService` / `SecretRealmSessionService` / `SecretRealmProgressSavedData`
   - `BossLootService`（unique/first_clear_only 不进重复）
   - `SecretRealmTrapService` → `FormationFieldService.activateFreeField`（禁止自建场系统）
   - `SecretRealmClearedEvent` + `onRealmCleared(realmId, player)`

3. **入口门与规则**
   - 7 gate 全部走 `enterBoundRealmOr`；服务端校验境界带/人数/开放窗口
   - 进入：session 计时 + 机关布设；超时/死亡强制 `returnFromSecretRealm`
   - Boss 击杀：首通/重复掉落 + 通关钩子；核心守护同样可触发通关

4. **维度**
   - `SecretRealmDimensionService` 扩到口袋维/天渊/风元/修罗等软绑定

## 验证

- 聚焦：`SecretRealmM09ServiceTest` / `WorldpackDataServiceTest` / `SecretRealmDimensionServiceTest`
- 全量：`bash ./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true` BUILD SUCCESSFUL

## 版本与协议

- `mod_version`：任务红线不升（保持 0.1.507）
- `ModNetwork.PROTOCOL_VERSION`：保持 21（无新包/旧包字段变更）

## 备份

`.bak/20260717_040118_m09_secret_realms/`
