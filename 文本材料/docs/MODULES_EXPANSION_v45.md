# 各模块扩充 v45

> 汇编 **§18** 结构优先 · **§9.7** 星宫秩序 · 通用名「补天丹」配额（非精确拍卖概率）

## 补天丹 ↔ 星宫功勋

| 声望 | 配额/周期 | 功勋价（量级） |
|------|-----------|----------------|
| 0–99 | 0 | — |
| 100–299 | 0 | 开放功勋店 |
| 300–599 | **1** | ~6500 功勋，元婴+ |
| 600+ | **2** | 略减价 + 传送许可折扣 |

- `economy_contribution_exchange.json` v2：`bu_tian_pill_quota` + `item_equiv`
- `alchemy_recipes.json`：`recipe_bu_tian` 挂星宫配额引用
- 商店 **`star_palace_merit_hall`**（功勋堂）

与 `chaotic_sea_factions.json#reputation_tiers` 对齐。

## 外海坊市

- 地区卡 **`outer_sea_market.json`**
- 公开摊 **`outer_sea_public_stall`**（灵石、低阶材料、采珠）
- 税：**星宫** `island_tax`；邻接黑市（准入不同）
- `chaotic_sea_factions.json` islands 挂 region_card

## NeoForge

- **`LorePackLoader.java`**：`data/seeking_immortals/lore/` 读 manifest + 门控辅助
- scaffold 资源目录 README + 复制 `data_manifest.json` 样例

## 打包

**`seeking_immortals_lore_v45.zip`**（含 lore Java 源码）

## 下一步

- Forge 主类注册 `AddReloadListener` 调用 `LorePackLoader`
- 凡人采珠 `mortal_pearl_fleet` 日常事件表
- 双圣派系内斗任务分支（星宫）