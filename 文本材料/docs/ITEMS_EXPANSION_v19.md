# 物品扩充 v19（2026-07-03）

> 制符 `paper_item_id`、贡献换算、法宝十一级、傀儡组装、制符辅料入库。

## 新增

| 文件 | 说明 |
|------|------|
| `artifact_eleven_tier_map.json` | 汇编 **法宝十一级** → `game_tier` 1–11 + `mod_tier` 五档映射 |
| `economy_contribution_exchange.json` | 贡献点 ↔ 下品灵石等价带 + 宗门汇率 |
| `puppet_craft_recipes.json` | 木人/巨猿/巨龟/石灵/混元改造 **5** 条组装 |
| `scripts/validate_contribution_prices.py` | 贡献堂价带 WARN |
| `scripts/migrate_talisman_paper_ids.py` | 维护用：同步符纸材料 id |

## 扩充

- **talisman_recipes** `schema_version` **2**：每条 `paper_item_id`，材料符纸与中/上品对齐
- **materials_catalog** +妖兽精魄、百年寒玉、火羽、定魂苔（制符方已引用）
- **贡献堂**（黄枫谷）：中品符纸、护体符方解锁
- **data_manifest** 登记 v18–v19 经济/符纸/傀儡/十一级表

## 汇编 §18 对齐

- **结构优先**：十一级表保留分类，数值可重平衡
- **混元钵**：`artifacts` + `refinement` + `puppet` 三线已通
- **炼丹炼器机制**：傀儡 `puppet_assembly_bench` 与炼器台并列（见 `block_items`）

## 命令

```bash
python scripts/validate_shop_prices.py
python scripts/validate_contribution_prices.py
python scripts/migrate_talisman_paper_ids.py   # 可选维护
```

## v20 候选

- `artifacts_catalog` 批量写入 `game_tier` 字段（由 eleven_tier_map 生成）
- 妖兽 **十三级** ↔ `beast_loot_tiers` 对照表
- 打包 `seeking_immortals_lore.zip` + Patchouli 章节自动生成