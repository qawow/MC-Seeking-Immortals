# 物品扩充 v26（2026-07-03）

> 符箓三阶结构、符匣 loot、汇编向幻术/散魂符、商店价软校验、Forge 脚手架。

## 符箓体系（§18）

- `talisman_grade_map.json`：低/中/高/古符 ↔ 符纸档位
- `economy_value_bands` 增加 `talisman_low/mid/high` 量级
- 新符：**幻心符**（邪幻镜向）、**散魂符**（玄黄镜向，弱于古宝）
- 符箋配方 +2（`mirage_sand` 材料）

## Loot

- `talisman_crate_low/mid/high` 表
- 消耗品：**低/中/高阶符箓匣**；丹方礼包链 `recipe_scroll_random_low`

## 材料 / 丹药

- 幻沙、石傀板；**凝气丹**

## 工具

| 脚本 | 作用 |
|------|------|
| `validate_merchant_prices.py` | 坊市价 vs bands（当前 **0** warnings） |
| `scaffold_forge_mod.py` | 生成 `forge_scaffold/` 236 个 item model + zh_cn |

## 规模

| 指标 | v26 |
|------|-----|
| talismans | **21** |
| consumables | **21** |
| forge_registry | **236** |
| worldpack | **352** 文件 |
| pytest | **8** passed |

## v27 候选

- `talisman_recipes` 与 catalog 全量对齐测试
- boss 掉落挂 `talisman_crate_mid`
- NeoForge `mods.toml` 模板