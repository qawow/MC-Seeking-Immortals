# 灵石四级阶梯与经济（v84）

> **统一主表**：`economy_spirit_stone_master.json` · 阶梯：`spirit_stone_ladder.json` · 拍卖：`economy_auction_bands.json`

## 四级兑换（结构默认 1:100）

| 阶 | 物品 ID | 流通境界 |
|----|---------|----------|
| 下品 | `low_spirit_stone` | 炼气—筑基 |
| 中品 | `mid_spirit_stone` | 筑基—结丹 |
| 上品 | `high_spirit_stone` | 结丹—元婴 |
| 极品 | `top_spirit_stone`（canonical `peak`） | 元婴—化神+ / 拍卖 |

折合下品：1 / 100 / 10 000 / 1 000 000。高阶少换低阶；逆星黑市下品约 **0.9** 折价。

## 三层经济

1. **坊市**：下品/中品标价（`merchant_shops` + `economy_price_bands`）
2. **宗门贡献**：筑基丹等管控资源（`economy_contribution_exchange`）
3. **拍卖行**：乱星海 / 大晋万宝 / 天渊；压轴拍品用下品等价数十万—数千万量级（见 `economy_reference_magnitudes` 范例）

## 关联

- 宗门月例锚点：`economy_spirit_stone_master.json#sect_stipend_low_stone_per_month`
- 物价带：`economy_value_bands.json`
- 月例量级：`economy_reference_magnitudes.json#monthly_income_by_realm`
- 校验：`scripts/validate_merchant_prices.py`（软 WARN）