# 物品扩充 v20（2026-07-03）

> 妖兽十三级、法宝 `game_tier`、去重、打包与 Patchouli 生成。

## 新增

| 文件 | 说明 |
|------|------|
| `beast_thirteen_tier_map.json` | 汇编 **妖兽 1–13 阶** ↔ 境界等价 ↔ `beast_loot_tiers` 三段掉落带 |
| `scripts/apply_artifact_game_tiers.py` | 写入 `artifacts_catalog` 的 `game_tier`（1–11），**去重**重复 id |
| `scripts/generate_patchouli_items_chapter.py` | 从 `item_id_index` 生成 Patchouli 条目草稿（上限 80） |

## 扩充

- **artifacts_catalog** → `schema_version` **2**，**29** 条唯一法宝，均含 `game_tier`
- **materials** +上古妖核、玄光镜残片、凡铁剑（tier1 示例）
- **beast_loot_tiers** v2，挂 `thirteen_tier_ref`
- **pack_world.py** 打包目录含 `scripts/`

## 汇编 §18

- **十三级 / 十一级** 以结构表落地，数值可重平衡
- 邪幻镜等已在 v18–v19；本版侧重阶位与导出

## 命令

```bash
python3 scripts/apply_artifact_game_tiers.py
python3 scripts/generate_patchouli_items_chapter.py
python3 scripts/pack_world.py
python scripts/export_item_ids_csv.py
```

## v21 候选

- `beast_bestiary` 批量写入 `beast_tier` 1–13 字段
- Forge `items` 注册表 JSON 生成器
- 合并 `evil_illusion_mirror` 等于 artifacts 的重复历史条目（已完成 dedup）