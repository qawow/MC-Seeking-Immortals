# 2026-07-17 M10 妖兽与生态落地

## 变更摘要

落地妖兽主数据（~1850 图鉴）、十三阶映射、区域刷怪表扩量、掉落解析、灵宠契约成长、傀儡定义、秘境 Boss 阶段技能与图鉴解锁/捕捉阶位上限。

## 实现要点

1. **妖兽主数据**
   - 新包 `beast/`：`BeastBestiaryService` / `BeastTierService` / `BeastCompanionService` / `BeastLootService` / `BestiaryUnlockService` / `PuppetDefinitionService` / `BeastBossService`
   - 发布语料：`beast_bestiary_runtime.json`（1890）、`bestiary_summary_v101`、`region_spawn_tables_v98`、companion/growth/loot/boss/puppet 等
   - 属性按十三阶→M01 `Realm` 映射缩放

2. **刷怪表**
   - `BeastSpawnTableService` 合并 `spawn_tables.json` + `region_spawn_tables_v98`（region_id 键）
   - 红线：真灵/同伴兽禁刷；请求 cap / 附近去重

3. **掉落与图鉴**
   - `BeastLootService`：loot bands + materials 表，材料 id 走 M03 `ItemCatalogService`
   - 击杀钩子：`ModEvents.onLivingDeath` → loot + `BestiaryUnlockService`
   - 契约解锁图鉴

4. **灵宠 / 傀儡 / Boss / 捕捉**
   - `BeastContractService`：境界/阶位门槛 + companion 成长倍率
   - `PuppetDefinitionService` → `SummonHonestMvpService` 变体数值
   - `BeastBossService`：16 Boss 目录 + 阶段技能（M02 effect type + M14 status stub）
   - `ArtifactCaptureService`：十三阶捕捉上限

## 验证

- 聚焦：`BeastEcologyServiceTest` + `BeastSpawnTableServiceTest` 通过
- 全量：`bash ./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true`

## 版本与协议

- `mod_version`：任务红线不升（保持 0.1.506）
- `ModNetwork.PROTOCOL_VERSION`：保持 21（无新包/旧包字段变更）

## 备份

`.bak/20260717_030716_m10_beasts_ecology/`
