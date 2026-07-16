# 2026-07-17 M13 维度与飞升落地

## 范围
- 分支：`task/m13-dimensions-ascension`
- 红线：不升 `mod_version`（保持 0.1.508）；协议保持 22（无新同步包字段）

## 完成点
1. **维度目录对账**
   - 发布作者语料：`dimensions_catalog` / `dimension_registry` / `ascension_flow` / `spirit_realm_interface` / `spatial_nodes_catalog` / `yin_underworld_cluster` / `flight_vehicles` / `ascension_loadout_v95` / `dimension_travel_methods_v136` / `dimension_travel_costs_v137` / `immortal_realms_v130`
   - 派生 `mortal_to_spirit_bridge.json`
   - 再生 catalog 索引与 `dimensions_reconcile.json`
   - 数据包 11 dimension + 10 dimension_type 对账；`yin_underworld` / `secret_realm_instance` 显式 deferred

2. **DimensionRegistryService**
   - id / cosmology / realm band / entry conditions
   - 覆盖 SpiritualAuraManager 已知 tianyuan/spirit_fengyuan/yin_ming_pocket/nether_river_pocket/demon_rift
   - `toMinecraftDimensionId`：mortal_world → overworld

3. **灵界接口 + 旅行权威**
   - `SpiritRealmInterfaceService`（窗口/单向/损耗）
   - `DimensionTravelService`：服务端校验路线/矩阵/冷却，禁止客户端自由指定目标维

4. **飞升流程**
   - `AscensionService`：境界(M01)+任务 flag(M11 软)+渡劫成功 → 确认流程 → 备份箱 → loadout 重置（唯一物品强制保留）→ 天渊传送

5. **空间节点 / 阴司 / 飞行**
   - spatial nodes 33 + `SpatialNodeNetworkSavedData`
   - `YinUnderworldClusterService` 鬼道通行
   - `FlyingAuthorityPolicy` 境界/维度飞行许可
   - `FlyingBoatDockStructure` + `ImmortalTeleportGrandArrayStructure`（消费 M07 formed 语义）
   - `FlightVehicleService` 接入 dock/维度限制
   - `AscensionGateBlock` 接入完整飞升流

6. **命令**
   - `/seeking_immortals catalog dimensions …`
   - `/seeking_immortals catalog ascension …`

## 验证
- 聚焦：`M13DimensionsAscensionTest` / `SpatialNodeCatalogServiceTest` / `DimensionResourceTest` 绿
- 全量：`bash ./gradlew.unix --no-daemon build -PaiSkipVersionBumpCheck=true` **BUILD SUCCESSFUL**
- 测试：472 → 通过（`ExtendedCatalogServiceTest` spatial 25→33 对齐）

## 备份
`.bak/20260717_044005_m13_dimensions_ascension/`

## 协议
无新网络包字段；`ModNetwork.PROTOCOL_VERSION` 保持 `22`。
