# 物品扩充 v27（2026-07-03）

> Boss 符匣掉落、符箓 catalog↔recipe 全量测试、Forge mods.toml、汇编向阵盘/材料。

## Boss / 高阶掉落

- `beast_bestiary.json`：`thunder_jiao`（boss）→ 中阶符匣 + 雷石；`mirage_sand_beast` → 高阶符匣 + 幻沙；**tier≥9** 通用低阶符匣 8%
- `boss_extra_loot_index.json` + `beast_loot_tiers.boss_extra_loot_ref`

## 汇编向物品

- **九龙火阵盘（仿）**（formation_items）
- **混元钵残片**、**铁木芯**（materials）

## Forge

- `forge_scaffold/.../META-INF/mods.toml`（1.20.1 / FML 47）
- `scaffold_forge_mod.py` 保留已有 mods.toml

## 测试

- `test_talisman_recipes_catalog.py`：21 符 = 21 配方
- `test_boss_extra_loot_items_exist`

## 规模

| 指标 | v27 |
|------|-----|
| talisman_recipes | **21** |
| formation_items | **8** |
| materials | **68** |
| forge_registry | **238** |
| worldpack | **354** 文件 |
| pytest（本批相关） | **5** passed |

## v28 候选

- `formation_catalog` 补 `nine_dragon_flame_barrier`
- 贡献堂售符匣
- Java `DeferredRegister` 代码生成