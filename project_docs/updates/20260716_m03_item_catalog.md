# 2026-07-16 M03 物品目录与批量注册

## 变更类别

- Code + resources + docs（物品目录权威、批量载体扩容、别名解析 API）
- **不**修改 `mod_version`（任务红线明确禁止）
- **不**修改 `ModNetwork.PROTOCOL_VERSION`（无包字段变更）

## 完成功能点

1. 目录对账：语料 `item_id_index`/`item_id_aliases` 与 `catalog_bulk_items.json` 逐类对账；报告见同目录 `20260716_m03_item_catalog_reconcile.json`。
2. 批量注册扩容：缺口 321 条（结构材料/符箓 v92/展示方块等）进入 `ModBulkItems`；现 bulk=1190。
3. 符箓目录：注册 + `grade` 元数据 + tooltip 品阶；消耗策略仍归 M14 `TalismanConsumePolicy`。
4. 傀儡部件：`puppet_parts_catalog` 载体齐全；组装/实体归 M04/M10。
5. 描述文本：从 catalogs + `item_descriptions_v95–v147` 生成 lang/tooltip。
6. 别名解析 API：`com.xunxian.seekingimmortals.catalog.ItemCatalogService#resolveCatalogItem`。
7. 方块物品：展示型载体补齐；功能方块用别名映射（`alchemy_furnace_g1`→`alchemy_furnace` 等）。

## 主要文件

- `registry/ModBulkItems.java` —  bulk 管线 + 唯一物品屏蔽 + grade 缓存
- `item/CatalogCarrierItem.java` — 带品阶 tooltip 的 bulk 载体
- `catalog/ItemCatalogService.java` — `resolveCatalogItem` / alias / meta
- `assets/seeking_immortals/catalog_bulk_items.json` — 1190 载体
- `assets/seeking_immortals/lang/zh_cn.json` / `en_us.json`
- `assets/seeking_immortals/models/item/*` + `textures/item/*` 占位
- `data/seeking_immortals/text_material/item_id_aliases.json`
- `data/seeking_immortals/reference/text_material_id_map.json`
- `文本材料/data/item_id_aliases.json`
- `scripts/m03_item_catalog_fill.py`
- 测试：`ItemCatalogServiceTest`、`ModBulkItemsTest`

## 红线

- 掌天瓶/绿液等唯一物品 **未** 进入 bulk 可堆叠通道；`ItemCatalogService.isUniqueForbidden` 拦截解析。
- 功能性方块仍由各玩法模块持有；本模块只补展示载体或别名。

## 验证

- `./gradlew build --no-daemon -PaiSkipVersionBumpCheck=true` → **BUILD SUCCESSFUL**
- 原因：任务红线禁止改 `mod_version`；preflight 前半段 skip，后半段 `record-state-only` 已调整为不重跑版本门禁
- 聚焦测试：`ItemCatalogServiceTest`、`ModBulkItemsTest`、`JsonSanityTest` 全绿

## 等待下游

- 无强制等待 M02 接口。
- 符箓真实消耗/释放效果：M14 `TalismanConsumePolicy` 可改为查询 `ItemCatalogService` 精确符箓 id（当前仍是大类映射）。
- 傀儡组装配方：M04；傀儡实体：M10。
- 法宝类 novel_waves artifacts 专属行为：M15。
