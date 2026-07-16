# 2026-07-16 M01 境界与修炼基础落地

## 变更类

代码 + 资源 + 测试 + 文档（需升 `mod_version` 与 `PROTOCOL_VERSION`）

## 完成功能点

1. **境界/寿元/别名**：`Realm.designId` 对齐语料（`DEITY_TRANSFORMATION`/`BODY_INTEGRATION`/`GREAT_VEHICLE`/`TRIBULATION_LAND`）；寿元表对齐 `cultivation_progression.json`；新增 `Realm.fromDesignId`；`WorldpackGameplayService.parseRealm` / `TechniqueDataManager.parseRealm` 统一走该解析。
2. **灵根**：`SpiritualRootAttribute` 补 `YIN`/`YANG` 与 corpus id；`LingGenCalculator` 权重对齐 `spirit_roots_catalog` multi_root_weights。
3. **体质数据驱动**：`ConstitutionCatalogService` 读 `text_material/constitution_catalog.json`；`PlayerCultivation.constitutionId` + `SpecialPhysique` 兼容映射；叠乘 cap API `clampStackedCultivation`（x2.5，供目录品阶场景）。
4. **渡劫参数化**：`TribulationRulesCatalog` 驱动波数/基伤；`TribulationService.getStrikeCount` 接目录；祭坛激活附加 `tribRes`。
5. **突破成功率**：`BreakthroughCatalog` 对齐 `realm_breakthrough_v98` base_success；`getBaseBreakthroughChance` 改查目录。红线：仍只消耗普通突破丹药链，不触碰剧情唯一道具。
6. **路线/种族**：`PathRaceCatalog` + `PlayerCultivation` 字段 `cultivationPathId`/`playableRaceId`/`ghostPathStageId` NBT 兼容默认值。
7. **门槛 API**：`ProgressionGateApi` + `CultivationHelper` 转发 `meetsRealm/Root/Path/Race`。

## 版本与协议

- `mod_version`: `0.1.505` → `0.1.506`
- `ModNetwork.PROTOCOL_VERSION`: `19` → `20`（`SyncCultivationDataPacket` 末尾新增 constitution/path/race/ghostStage 四字段）

## 验证

- 聚焦测试：`M01ProgressionFoundationTest` + `Phase1CultivationSystemTest` + `BreakthroughAidLogicTest` 全部通过
- 全量 `./gradlew build --no-daemon`：编译/打包成功（`seeking_immortals-0.1.506.jar`）；`:test` 仍有 2 个 **M00 既有**失败：
  - `SettingCatalogSummaryServiceTest`：techniques 747 vs 346
  - `JsonSanityTest#textMaterialIndexesAreCoherent`：`body.json.json` 缺失
- 备份：`.bak/20260716_154521_m01_realm_progression/`

## 取舍

- **不改 enum 常量名**（`SOUL_TRANSFORMATION`/`UNITY`/`MAHAYANA` 等）以保 NBT 兼容，只改 `designId` + 统一解析。
- **运行时灵根分类倍率**（天灵根 ×5 等）保留既有身份，不套 stack_cap；cap 作为目录品阶×体质 API 暴露。
- 渡劫触发点仍从结丹起（现有玩法），波数/基伤接语料。
- 仙界扩展境界（金仙/太乙等）仅语料 `immortal_realms_v130` 预留，未扩 enum（下游 M13 再接）。
