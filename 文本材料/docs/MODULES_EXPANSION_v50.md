# 各模块扩充 v50

> 汇编 **§9.7 天渊城** · **§13 秘境四要素** · **§18 结构倍率**

## 天渊城 / 灵界边境

| 文件 | 内容 |
|------|------|
| `tianyuan_city.json` | 人妖聚集、功勋货币、双倍修炼**结构倍率**（2.0） |
| `region_cards/tianyuan.json` | 兽潮、地渊许可、功勋链 |
| `spirit_realm_border.json` | 荒原兽群、裂隙、断界+门槛 |

- 任务链：**`tianyuan_merit_path`**、**`spirit_realm_border`**
- `dimensions_catalog` v5 挂 `hub_city`

## 鬼修 sect_ban 后果

- `ghost_cultivation_path` v4：`sect_ban_consequences`（声望、悬赏、商店拒绝、追魂事件）
- `ghost_sect_ban_rules.json`：暴露标签与缓解（敛魂符、阴罗掩护）
- 任务链 **`ghost_sect_ban_arc`**

## 秘境 template 补全

- `secret_realm_template.json` v2：**全部** `secret_realms` id 入 `realms_enriched`
- 缺四要素的自动从 `loot_tiers`/`layers` 生成占位，写入 `completeness_report`
- `secret_realms.json` v4 回写四要素字段

## 打包

**`seeking_immortals_lore_v50.zip`**

## 下一步

- 风元大陆 / 蛮荒区域卡
- 天渊功勋与 `economy_contribution_exchange#tianyuan` 对齐
- 自动占位秘境人工润色清单