# NeoForge 设定数据加载（草案）

> 汇编 §18：优先加载**结构**（境界、技能树门控），数值表可后接平衡。

## 入口

1. 将 `data/` 复制或软链到 `src/main/resources/data/seeking_immortals/lore/`
2. 启动时读取 `data_manifest.json` → 按 `categories` 分批注册
3. 术法：`techniques/index.json` → 懒加载各 `techniques/*.json`

## 建议 Java 接口

```java
// LorePackLoader.load(Path root)
// - validate schema_version on each file
// - register CultivationMethod, Technique, QuestHook, SpatialNode
```

## 门控规则（与 JSON 一致）

- `requires_method`：未习得功法则法术不可用
- `race_required` / `faction`：慕兰法士、星宫声望等
- 传送：`spatial_nodes_catalog.json`，**不读**成功率字段

## 当前包

- 最新 zip：`seeking_immortals_lore_v46.zip`
- 重载：`LorePackReloadListener` 实现 `PreparableReloadListener`（**1.20.1 Forge 47**）
- 最新 zip：`seeking_immortals_lore_v49.zip`
- Java：`forge_scaffold/src/main/java/com/seekingimmortals/lore/LorePackLoader.java`
- Patchouli：`patchouli_item_book.json` → 游戏内书

## P0 未实现

实体生成、战斗演算、天劫 RNG 需模组代码；本目录仅为**设定真源**。
