# 各模块扩充 v32（术法分卷 + 灵界天渊 + 千竹塔）

## 本轮

| 模块 | 变更 |
|------|------|
| `techniques/fashi.json` | +风沙/圣兽唤灵/御风/雷掌灵术（慕兰·天澜） |
| `techniques/puppet.json` | +控傀诀、机关阵术 |
| `techniques/index.json` | 重计 total_techniques |
| `secret_realms.json` | +**千竹机关塔**（5 层） |
| `secret_realm_template.json` | 机关塔四要素 |
| `puppet_craft_recipes.json` | +石卫、火矛组装 |
| `spirit_realm_interface.json` | 天渊功勋店、分身下界说明（汇编 9.7） |
| `region_cards/` | +天渊城、风元大陆、极西千竹 |
| `npc_dialogue_templates.json` | 慕兰长老、天渊功勋吏、千竹机关师 |
| `quest_hooks.json` | 机关塔试炼、天渊斩魔 |
| `trade_routes.json` | 慕兰走私线、天渊功勋商队 |
| `economy_contribution_exchange.json` | `tianyuan` 功勋兑换 |
| `tribulation_items.json` | 天渊护身符 |
| `boss_loot_tables.json` | 机关塔掉落池 |
| `patchouli_item_book.json` | 导言条目：三层技能、天渊概览 |

## 汇编

- **§18**：功法/法术/神通 → 已挂 `cultivation_methods` + `techniques/*`
- **§9.7**：天渊城修炼倍率、人妖混居 → `spirit_realm_interface.tianyuan_city`
- **§13**：千竹塔 = 开放条件 + 机关环境 + 五层 + 傀儡掉落

## 累计

v30–v32 见各 `MODULES_EXPANSION_v*.md`；法宝 169 不变。

## 下一步

- `techniques/sword.json` 挂 `qingyuan_sword_art` 前置
- `chaotic_sea_factions` 声望任务链
- ~~维度 `dimension_id` 表（路线图 P0）~~ → 已完成见 `dimensions_catalog.json`