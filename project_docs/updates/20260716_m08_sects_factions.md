# 2026-07-16 M08 宗门与势力落地

## 变更类
代码 + 资源（text_material 发布补齐）+ 文档。任务红线：**不修改** `mod_version`；`SyncSectData` 字段未改，协议保持 `21`。

## 完成点
1. **宗门主数据** `SectMasterDataService`：读 `sects.json` + `sect_specialty_map`；入门走 `ProgressionGateApi` + 鬼修禁令。
2. **势力关系图** `FactionGraphService`：`faction_graph` / `faction_species` + 分势力深包节点属性；敌友/声望轴查询与双向一致性测试。
3. **声望解锁** `ReputationUnlockService`：`reputation_unlocks_v102` 阈值 → 解锁/锁定/区域通行/商店档位查询。
4. **贡献商店** `SectContributionShopService`：`sect_contribution_shop` + shelves v106/107；红线禁止贡献无限兑灵石；never_list 拦截掌天瓶/绿液。
5. **阵营冲突** `FactionConflictEventService`：订阅 `WorldpackGameplayService.refreshDailyEvent`；服务端改声望与市场物价倍率。
6. **鬼修禁令** `GhostSectBanService`：与 M01 ghost path 联动；入门拒收、商店拒绝、追杀标记。
7. **missions/dialogues**：30/30 已存在并对齐 playable 宗门。

## 接线
- `SectContributionService.applySect` 入门前校验 master/ghost。
- `ShopService.buyWithSectContribution` 鬼修店禁 + never_list。
- `WorldpackGameplayService.marketCostModifier` 乘冲突物价。
- 语言键：`faction_conflict.daily_trigger` / `sect.apply_ghost_ban` / `sect.apply_entry_denied`。

## 验证
- 聚焦：`./gradlew test --tests 'com.xunxian.seekingimmortals.sect.*' -PaiSkipVersionBumpCheck=true`
- 全量：`./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true`

## 版本与协议
- `mod_version`：保持 `0.1.506`（任务红线）
- `ModNetwork.PROTOCOL_VERSION`：保持 `21`（无包字段变更）

## 备份
`.bak/20260716_233440_m08_sects_factions/`
