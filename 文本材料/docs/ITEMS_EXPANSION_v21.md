# 物品扩充 v21（2026-07-03）

> 妖兽掉落带标注、Forge 注册表草稿、索引与 CSV 刷新。

## 脚本与产出

| 脚本 | 产出 |
|------|------|
| `apply_beast_thirteen_fields.py` | `beast_bestiary` v2：30 种妖兽均含 `beast_tier`、`loot_band`、`loot_table_ref` |
| `generate_forge_item_registry.py` | `forge_registry/items_registry_draft.json`（**216** 条 `seeking_immortals:id`） |
| `rebuild_item_id_index.py` | 刷新 `item_id_index.json` counts（含 beasts:30） |
| `export_item_ids_csv.py` | **230** 行 `docs/item_ids_export.csv` |

## 数据变更

- **beast_bestiary** `schema_version` **2**，挂 `thirteen_tier_ref`
- **pack_world** 含 `forge_registry/`
- **tests/test_forge_registry.py** 校验注册表无重复 id

## 汇编 §11.7 傀儡（数据侧）

巨猿/巨龟/石灵已在 `puppet_parts`、`puppet_craft_recipes`；本版未改配方，仅打通 **beast → loot_band → materials** 引用链。

## 命令

```bash
python3 scripts/apply_beast_thirteen_fields.py
python3 scripts/generate_forge_item_registry.py
python3 scripts/rebuild_item_id_index.py
python3 scripts/export_item_ids_csv.py
python3 scripts/pack_world.py
pytest tests/test_forge_registry.py tests/test_pack_world.py -q
```

## v22 候选

- 从 `items_registry_draft` 生成 `en_us.json` / `zh_cn.json` lang 片段
- `beast_bestiary` 与 `spawn_tables` id 交叉校验脚本
- 汇编古宝/灵宝各 +2 条模板（仅数据）