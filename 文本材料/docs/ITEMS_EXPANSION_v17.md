# 物品扩充 v17（2026-07-03）

> 世界对齐（地区↔群系）+ 工作台方块 + 地区专属材料入库 + 全 id CSV。

## 新增

| 文件 | 说明 |
|------|------|
| `block_items_catalog.json` | 丹炉 1–3 品、炼器阵、制符台、聚灵/传送阵基、傀儡台、灵草圃、灵矿方块 **14** |
| `worldgen_biomes.json` | **12** 个 `seeking_immortals:*` 生物群系 ↔ `region_id` |
| `item_id_aliases.json` | 地区表与主目录 id 别名（黄精草→yellow_essence 等） |
| `scripts/export_item_ids_csv.py` | 扫描各 catalog → `docs/item_ids_export.csv` |
| `docs/item_ids_export.csv` | 策划/模组批量注册用 |

## 扩充

- **items_by_region** v2：+冥河、阴冥、坠魔谷、天渊；各条 `biome_ref`；修正 JSON 根字段顺序
- **materials** +昆吾核矿、古魔封印残片、天虎爪、圣禽翎、界河髓草、灵界灵砂、魔将魔核、铁木、妖兽鲜肉等（与地区表对齐）
- **consumables** +元婴遁符、测灵石、随机丹方礼包

## 汇编 §18

- **结构优先**：群系→地区→掉落/商人，不硬编码精确掉率
- **多方块炼丹炼器** → `block_items_catalog` + 已有 `alchemy_system` / `refinement_recipes`

## 使用

```bash
python scripts/export_item_ids_csv.py
```

## v18 候选

- 为 `items_by_region` 中旧 id 批量改 canonical（或生成迁移表）
- `talisman_paper` 低/中/高阶拆成独立物品条目
- 经济量级表 `economy_reference_magnitudes` 与商店价格自动校验脚本