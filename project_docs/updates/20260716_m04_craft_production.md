# 2026-07-16 M04 炼制与生产落地

## 范围

按 `project_docs/task_briefs/M04_炼制生产.md` 完成炼丹/炼器/制符/傀儡/灵植生产线落地。

## 变更摘要

1. **炼丹配方扩量**：生成 `data/seeking_immortals/alchemy/recipes/` 共 129 份（覆盖 `pills_catalog` 114 + curated 别名），`AlchemyRecipeManager` 重载；同步 `pill_material_name_map.json` 与 `pill_effect_catalog.json`。
2. **品质管线**：`PillQuality` 改为 LOW/MIDDLE/HIGH/PERFECT（对齐 `pill_quality.json` 倍数 0.7/1.0/1.25/1.5）；兼容 medium/supreme 别名；炉内 `fromQualityScore`。
3. **丹药效果**：新增 `PillEffectCatalog` + `BulkPillItem`，bulk 丹药可服用；`CatalogPillType.futureSystemDisabled()` 全 false。
4. **炼器 G1–G3**：`RefinementForgeCraftHelper` 统一 datapack serializer（按 `forge_grade` 过滤）+ catalog 回退；生成 45 份 `*_serializer.json`。
5. **灵植/绿液红线**：ship `garden_liquid_calendar_v108.json`；`GardenLiquidService` 年配额/冷却/掌天瓶唯一；`SpiritHerbPlanter` 催熟先扣配额。
6. **测试**：`M04CraftProductionTest`；更新 `BreakthroughAidLogicTest`。

## 验证

- `bash ./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true` → **BUILD SUCCESSFUL**
- 任务红线：不修改 `mod_version`（0.1.506）；协议保持 21

## 备份

`.bak/20260716_220623_m04_craft/`
