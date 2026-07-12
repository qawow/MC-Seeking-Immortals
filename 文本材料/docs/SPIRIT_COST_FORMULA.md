# 术法灵力消耗公式（草案）

> 样板数据：`data/techniques_sample.json`

## 基础公式

```text
cost = base * (1 + 0.05 * max(0, caster_realm_tier - technique_realm_min_tier))
```

- `base`：术法 JSON 中 `spirit_cost_base`
- `realm_tier`：炼气=1 … 化神=5（可扩展）

## 属性缩放伤害（示例）

| 术法 | 缩放 |
|---|---|
| 火弹术 | `12 + spirit_power * 0.15` |
| 青元剑芒 | `45 + spirit_power * 0.35 + sword_affinity * 10` |

## 法士灵术

使用 `spirit_art_charge` 条，不扣灵力池；每日恢复上限与 `mulan_fashi` 声望挂钩。

## 实现注意

- Cooldown 单位：游戏 tick（20 tick = 1 秒）
- `tags` 供逻辑分支：`requires_flying_sword`、`demonic_karma`、`corpse_synergy`