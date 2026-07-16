# 2026-07-16 M07 阵法与多方块落地

## 变更类

代码 + 发布资源（text_material 多方块/阵法语料）。`mod_version` 按任务红线保持 `0.1.506` 不升；网络协议保持 `21`（无包字段/顺序变更）。

## 语料与权威

| 集合 | 处理 |
|---|---|
| v135 索引（total=86，无 entries） | 与 v134 的 82 条 entries 合并，补 4 个战争结构 id（capture_point_obelisk / field_repair_tent / siege_spirit_ram / war_banner_pole）→ 运行时 `multiblock_structure_index.json` |
| formation_catalog 20 + array catalog 36 | 发布 + 派生 `formation_field_params.json`（56 场参数） |
| formation_items_catalog 13 | 发布 + `formation_item_behaviors.json` 放置/激活表 |
| MP sequences v135 | 发布原文 + `multiblock_mp_sequence_display.json`（M16/Patchouli 展示） |
| operational states / materials / prices | 原样发布进 jar text_material |

## 实现要点

1. **`MultiblockStructureCatalog`**：加载 86 结构索引与尺寸解析；`MultiblockStationService.isStationFormed` 统一 stationId 判定，代码 validator 优先、ring/single_core 数据模板兜底；TTL 缓存 + 方块放置/破坏脏标记。
2. **`MultiblockPattern`**：补 `ringRequirements` / `fromCatalogStation` 数据驱动路径，不破坏既有炉壳代码 pattern。
3. **`FormationFieldCatalog` + `FormationFieldService`**：目录半径/灵气加成；`getActiveFieldEffects(level,pos)` 稳定查询；环完整性改为间隔检查（非每 tick 全扫）。
4. **`FormationFieldSavedData` / `FormationCoreBlockEntity`**：持久化 formationId/radius/aura/effect/free；BE 类型覆盖全部阵法核心方块。
5. **`FormationItemService`**：M03 bulk 阵旗/阵盘 use → 放置 `spirit_gathering_array` 或 free-field 激活。
6. **聚灵阵对齐**：`SpiritualAuraManager` 保留数组块扫描，叠加 active SPIRIT_GATHER 场 aura_bonus（有上限，向后兼容）。
7. **`FormationApi`**：下游门面（M04/M06/M08/M09/M13/M14）。

## M06 友好接口取舍（保守）

- 只暴露只读查询：`isStationFormed` / `getActiveFieldEffects` / catalog size；不引入区域事件写回或包。
- stationId 与语料 id 对齐，未知 id 返回 false 而非抛异常。
- 场效果查询不修改 ACTIVE 表；客户端不直读服务端 map。

## 验证

- 聚焦：`M07FormationsMultiblockTest` + `FormationFieldServiceTest` 通过。
- 全量：`./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true` **BUILD SUCCESSFUL**。
- 跳过版本门禁原因：任务红线明确“不要修改 mod_version”。

## 备份

`.bak/20260716_223400_m07_formations/`
