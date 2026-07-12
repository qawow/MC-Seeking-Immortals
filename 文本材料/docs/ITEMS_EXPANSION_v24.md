# 物品扩充 v24（2026-07-03）

> 炼器系统闭环、手册交叉索引、Patchouli book.json、汇编 §11.1 补页。

## 新增 data

| 文件 | 作用 |
|------|------|
| `refinement_system.json` | 炼器阵/锻心/灵风囊、错配规则（对齐 §12） |
| `refinement_failure_loot.json` | 按 tier 失败返还碎料 |
| `refine_manual_index.json` | 24 条炼器配方 ↔ 手册（脚本生成） |

## 手册 +4

- `refinement_manual_ancient` 古宝重铸秘要
- `recipe_refine_flying_sword` / `recipe_refine_evil_mirror` / `recipe_refine_giant_turtle`

## Patchouli

- `zh_cn/book.json`：5 分类含 **seeking_immortals:items**
- 条目：炼器总览、石灵傀儡、玄黄镜
- `scripts/sync_patchouli_book.py`

## 脚本

- `build_refine_manual_index.py`
- `sync_patchouli_book.py`

## 规模

| 指标 | v24 |
|------|-----|
| forge_registry | **225** |
| manuals | **19** |
| worldpack | **341** 文件 |
| pytest | **5** passed |

## 命令

```bash
python3 scripts/build_refine_manual_index.py
python3 scripts/sync_patchouli_book.py
python3 scripts/pack_world.py
```

## v25 候选

- `scrap_spirit_iron` 炼器失败掉落接入 loot 表
- Patchouli 炼器配方分页（按 manual 分组）
- 经济量级表 `economy_value_bands.json`