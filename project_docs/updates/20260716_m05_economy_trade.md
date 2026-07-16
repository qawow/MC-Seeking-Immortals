# 2026-07-16 M05 经济与交易落地

## 变更类

代码 + 发布语料 + 测试 + 文档。`mod_version` 按任务红线保持 `0.1.506` 不升；网络包字段未改，`PROTOCOL_VERSION` 保持 `21`。

## 完成范围

| 功能点 | 状态 |
|---|---|
| 灵石阶梯对账 | Done `SpiritStoneLadderService` 读 master/ladder；1:100:10000:1000000 |
| 灵石兑换入口 | Done 原创 NPC `SpiritStoneBankerEntity`（**不再**用原版村民 shift 兑换） |
| 市场价格主表/标签 | Done `MarketPriceService`；unique/no_trade 禁商店与拍卖通道 |
| 商人商店 + 路线差价 | Done `TradeRouteEconomyService` 接入 `ShopService.adjustedCost` |
| 拍卖波段 + 万宝池 | Done `AuctionSoftService` 合并 `wanbao_auction_artifacts` |
| 周目经济 | Done `NewGamePlusEconomyService.price_mod` 进入商店定价 |
| 贡献兑换框架 | Done `ContributionExchangeService`（估值/功勋目录；无限兑换恒 false） |
| AuctionHouseSavedData 兼容 | Done 旧档仅 Bids 可读测试 |

## 新增服务与 M06 对接接口

### 经济侧 region 键（非 Worldpack RegionCard）

| 类型/API | 字段 | 含义 |
|---|---|---|
| `MarketPriceService.RegionMarket` | `id`, `display`, `band`, `items[]` | 价格主表区域切片 |
| `MarketPriceService.CommodityPrice` | `nameOrId`, `min`, `max`, `mid()` | 商品价带 |
| `MarketPriceService.suggestedShopCost(itemId, regionId, fallback)` | | 区域定价 |
| `MarketPriceService.isBlockedFromOpenMarket(itemId)` | | unique/no_trade 门禁 |
| `TradeRouteEconomyService.TradeRoute` | `id`, `display`, `fromRegion`, `toRegion`, `transport`, `durationDays`, `feeLowStone`, `riskEvents`, `goodsExport`, `goodsImport` | 商路 |
| `TradeRouteEconomyService.AuctionHub` | `region`, `name`, `tier` | 拍卖枢纽 |
| `TradeRouteEconomyService.from(regionId)` / `priceModifier(regionId, goodsHint)` / `shopRegion(shopId)` | | 路线差价与 shop→region |
| `ShopService.shopRegionId(shopId)` | | 商店区域查询转发 |

`regionId` 约定：语料蛇形小写（`tiannan` / `chaotic_sea` / `dajin` / `tianyuan`）。**不**新建第二套 `RegionCard`；空间权威仍归 M06 `WorldpackDataService` / 未来 `RegionRegistry`。

### 贡献兑换（M08）

| API | 说明 |
|---|---|
| `ContributionExchangeService.isInfiniteStoneSwapAllowed()` | 恒 `false` |
| `lowStonePerContribution(factionId)` / `estimateLowStoneValue` | 仅估值 |
| `catalogFor(currencyOrFaction)` | `merit_points` / `patrol_merit` / `smuggle_credit` 货架 |

### 灵石钱庄 NPC

- 实体 id：`seeking_immortals:spirit_stone_banker`
- 命令：`/seeking_immortals market spawn_banker`（权限 2）
- 交互：右键钱庄掌柜执行阶梯兑换（日限 3）

## 发布语料（text_material）

新增进 jar：`market_price_master_v100`、`item_economy_tags_v101`、`market_shelves_v94`、`market_shelf_price_align_v144`、`newgame_plus_economy_v102`、`item_economy_index_v92`、`merchant_shops_guide_v113`、`faction_shop_stock_v94`、`dajin_economy_bands`、`auction_catalog_v93`。

## 验证

- 聚焦 M05 服务测试 + `ShopServiceTest` + `AuctionHouseSavedDataCompatibilityTest` 通过
- `bash ./gradlew --no-daemon build -PaiSkipVersionBumpCheck=true` **BUILD SUCCESSFUL**
- 跳过版本门禁原因：任务红线明确不要修改 `mod_version`

## 版本与协议

- `mod_version`：保持 `0.1.506`
- `PROTOCOL_VERSION`：保持 `21`（未改包字段）

## 备份

`.bak/20260716_223521_m05_economy/`
