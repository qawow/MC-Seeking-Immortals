# 物品扩充 v25（2026-07-03）

> 经济量级、炼器失败 loot 表、按手册分组的 Patchouli 炼器页。

## 新增 data

| 文件 | 作用 |
|------|------|
| `economy_value_bands.json` | 灵石四级 + 丹/器/手册/灵药 **量级区间**（§16） |
| `loot_tables.json` | `refinement_fail_*` 表 + `recipe_tier_to_table` 映射 |

## 炼器闭环

- `refinement_system.json` 增加 `failure_loot_tables_ref`
- 与 `refinement_failure_loot.json` 双轨：结构化表供 Forge/datapack 直接读

## 法宝 +2

- `nine_dragon_barrier_token` 九龙罩阵符（一次性）
- `quhun_iron_puppet` 驱魂铁傀战利品召唤

## Patchouli

- `generate_patchouli_refinement_by_manual.py` → **3** 页（入门/真解/上古傀儡术）
- `artifacts` 分类 entries 已合并手册炼器列表
- `economy_guide` 条目指向 `economy_value_bands.json`

## 规模

| 指标 | v25 |
|------|-----|
| artifacts | **33** |
| materials | **65** |
| forge_registry | **228** |
| worldpack | **348** 文件 |
| pytest | **7** passed |

## 命令

```bash
python3 scripts/generate_patchouli_refinement_by_manual.py
python3 scripts/pack_world.py
```

## v26 候选

- `economy_value_bands` → merchant_shops 自动校验脚本
- 符箓三阶 × loot_tables 开箱
- Forge 工程脚手架 `scripts/scaffold_forge_mod.py`