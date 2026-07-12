# 乱星海扩充 v33

> 汇编 **§9.7** 星宫 / 逆星盟；**§13** 虚天殿秘境挂 `void_palace`；**§18** 声望分档非精确数值。

## 数据

| 文件 | 内容 |
|------|------|
| `chaotic_sea_factions.json` v2 | 声望档位、岛屿、航线、灾难响应 |
| `region_cards/chaotic_sea.json` | 地区四要素 + 父维度人界 |
| `quest_hooks.json` | 巡防/暗礁/剿盗/劫税/虚天钥/护航 |
| `quest_chains.json` | 已有 `chaotic_sea_politics` 分支互斥 |
| `daily_random_events.json` | 兽潮、鬼雾休战、查税、岛拍传闻 |
| `faction_conflict_events.json` | 封锁暗礁、劫税船 |
| `merchant_shops.json` | 外海杂货、逆星黑市、星宫巡防补给 |
| `economy_contribution_exchange.json` | `star_palace` 巡防功勋 / `inverse_star` 走私信用 |
| `cultivation_methods.json` | 镇海诀、逆星潜行诀 |

## 玩法结构

1. **筑基+** 选边：星宫忠诚线 vs 逆星 rebel 线（`branch_mutual_exclusive`）
2. **外海→内海** 需声望或贿赂；走私链需逆星声望
3. **三星灾**（兽潮/鬼雾/天风）改变 PvP 与物价 — 见 `disasters_faction_response`
4. **终章** `void_palace_key` → `secret_realms#void_palace` 四要素

## 合规

通用：灵舟、岛税、黑市、玉简碎片；不绑主角专属法宝名。