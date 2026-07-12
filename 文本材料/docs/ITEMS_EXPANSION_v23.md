# 物品扩充 v23（2026-07-03）

> 炼器残片链、Patchouli 物品章、en_us 占位、汇编向条目。

## 炼器 `refinement_recipes`（24 条，+4）

| id | 产出 | 说明 |
|----|------|------|
| refine_xuanhuang_shard | xuanhuang_mirror | 玄黄镜残片×3，古宝重铸 |
| refine_nine_dragon_replica | nine_dragon_cauldron_replica | 九龙罩残片+火羽 |
| refine_void_refining_bell | void_refining_bell | 灵界炼虚钟碎片 |
| refine_giant_turtle_core | giant_turtle_puppet_core | 巨龟线（汇编 11.1/11.7） |

## 材料 +3

- `xuanhuang_mirror_shard`、`nine_dragon_cauldron_shard`、`void_bell_fragment`

## 丹方 +1

- `recipe_foundation_break` → 产出 `foundation_pill`（筑基丹方破境篇）

## Patchouli

- `generate_patchouli_items_chapter.py` 重写：从五类 catalog 生成 **135** 条 `entries/items/*.json`
- `categories/items.json` 物品图鉴分类
- 手工条目：邪幻镜、巨龟傀儡（汇编对齐）

## Lang

- `enrich_en_us_lang.py`：en_us 用 id 转 Title Case 占位（**221** keys）

## 规模

| 指标 | v23 |
|------|-----|
| forge_registry | **221** |
| worldpack 文件 | **331** |
| materials | **64** |

## 命令

```bash
python3 scripts/generate_patchouli_items_chapter.py
python3 scripts/enrich_en_us_lang.py
python3 scripts/pack_world.py
```

## v24 候选

- Patchouli `book.json` 注册 items 分类
- `refinement_recipes` ↔ `manuals_catalog` 交叉索引
- 炼器失败掉落表