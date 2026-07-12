# 物品扩充 v18（2026-07-03）

> 符纸阶位物品化 + 经济价带校验 + 汇编 11.1 幻镜/古宝/傀儡补条。

## 新增

| 文件 | 说明 |
|------|------|
| `talisman_materials_catalog.json` | 凡/下/中/上品灵符纸、阴符纸、符墨引用、**4** 条制纸配方 |
| `economy_price_bands.json` | 坊市灵石价 **量级带** + `item_overrides` 建议价 |
| `scripts/validate_shop_prices.py` | 对 `merchant_shops` 中带 override 的条目做区间 WARN |

## 扩充

- **坊市**：凡符纸、中品符纸、妖血墨上架（天南灵草摊）
- **法宝** +邪幻镜、玄黄镜（模板）、平山冠（任务真品）、巨龟傀儡核心 → **32** 条
- **傀儡** 巨龟：补 `craft_materials`、修复材料结构化、汇编注释

## 汇编对齐

- **符箓三等级三** → `papers[].grade` 与 `talisman_catalog#craft_rules` 一致；`talisman_paper` 仍为下品默认 id
- **§16 经济** → 价带 WARN 非硬失败，便于迭代平衡
- **11.1 邪幻镜 / 玄黄镜 / 巨龟** → `artifacts` + `puppet_definitions`

## 命令

```bash
python scripts/validate_shop_prices.py
python scripts/export_item_ids_csv.py
```

## v19 候选

- `talisman_recipes` 增加 `paper_item_id` 字段与 materials 联动
- 贡献堂价格与 `economy_price_bands` 贡献换算表
- 法宝 **十一级** 细分为 game_tier 1–11 映射表（当前五档+标签）