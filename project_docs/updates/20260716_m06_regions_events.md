# 2026-07-16 M06 区域与世界事件落地

## 变更摘要

为 22 张已发布 region cards（+index）建立首个运行时消费者，统一 region_id 权威，并接入每日事件调度、区域物品/生物群系/旅行路线查询与灵气地脉挂钩。

## 实现要点

1. **Region 注册表归一**
   - 新包 `region/`：`RegionRegistry` / `RegionDefinition` / `RegionItemsService` / `RegionBiomeMap` / `TravelRouteGraph` / `DailyEventScheduler` / `DailyEventHook` / `RegionEventConfig`
   - 合成 `worldpack/regions.json` + `text_material/region_cards/*` + `worldgen_biomes.json`
   - 对外唯一 region_id 权威是 `RegionRegistry`；worldpack `RegionCard` 保留为旅行 DTO

2. **区域→物品**：`RegionItemsService.itemsForRegion(regionId)` 读 `items_by_region.json`

3. **每日事件调度**
   - 服务端 overworld day-roll：`DailyEventScheduler.serverTick`
   - 扩量：`daily_random_events.json` + `tianyuan_daily_events.json` 注入多区域候选
   - 订阅：`DailyEventScheduler.registerHook` / `onDailyEvent(regionId,eventId)`（供 M08/M11）
   - 可关闭：`RegionEventConfig` + `/seeking_immortals worldpack daily_events enable|disable|roll|status`

4. **生物群系映射**：`RegionBiomeMap` 读 `worldgen_biomes.json`

5. **旅行路线图**：发布 `text_material/travel_routes.json`（v102）并与 `trade_routes.json` 合成 `TravelRouteGraph`

6. **灵气地脉**
   - `SpiritualAuraManager` 增加 region 倍率项（clamp 0.5–2.5）
   - `SpiritDetectorItem` / `LeylineCompassItem` 展示区域信息

7. **玩家 region 解析与同步**
   - `RegionRegistry.resolveRegionId(level,pos,preferred)` / `resolveAndSync(player)`
   - 登录与跨维度后解析并走既有 `SyncWorldpackDataPacket`（未新增包、未改旧包字段）

## 验证

- 聚焦：`RegionRegistryTest` + `SpiritualAuraManagerTest` + `WorldpackDataServiceTest` 通过
- 全量：`bash ./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true`

## 版本与协议

- `mod_version`：任务红线不升（保持当前）
- `ModNetwork.PROTOCOL_VERSION`：保持 21（无新包/旧包字段变更）

## 备份

`.bak/20260716_225956_m06_regions_events/`
