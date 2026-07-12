# 各模块扩充 v30（原著结构向 · 2026-07-04）

> 对齐汇编：**§9 宗门专精**、**§13 秘境四要素**、**§12 炼丹**、**§11 符箓/法宝**、**§18 保留结构而非死磕数值**。

## 本轮变更摘要

| 模块 | 文件 | 扩充 |
|------|------|------|
| 丹药 | `pills_catalog.json` | +固元丹、驱魔丹、风行丹、海毒解丹等（schema v2） |
| 炼丹 | `alchemy_recipes.json` | +对应丹方 |
| 符箓 | `talisman_catalog.json` / `talisman_recipes.json` | +匿形/定身/金刚/聚灵/封魔符胚 |
| 阵法 | `formation_catalog.json` | +颠倒五行阵、金刚困仙阵、聚灵小阵、封魔柱阵、御风阵 |
| 灵草 | `spirit_herbs_catalog.json` | +驱魔草、风行草、珊瑚灵藻、冥雾兰、昆吾火苔 |
| 秘境 | `secret_realms.json` + `secret_realm_template.json` | +古修洞府、荒冢秘穴（四要素） |
| 宗门 | `sects.json` | specialty 补全：制符/炼丹/驭兽/星宫秩序/千竹傀儡等 |
| 日常 | `daily_random_events.json` | +天南涨价、乱星海海盗、慕兰罡风、拍卖周、冥河雾灾 |
| 任务钩 | `quest_hooks.json` | +炼丹考核、制符考核、驭兽试炼、虚天殿异象、慕兰巡防 |
| 傀儡 | `puppet_definitions.json` | +石卫傀儡、火矛傀儡（`definitions`） |
| 地区卡 | `region_cards/` | +慕兰草原、大晋中心 |
| 妖兽 | `beast_bestiary.json` | +幼血蛟、虚天冰蟒、风狼 |

## 未改（已较满）

- 法宝 `artifacts_catalog` 169、炼器 73 — 见 `REFINEMENT_ARTIFACTS_MASTER_LIST.md`
- 术法 `techniques/` 分卷 — 单独扩卷时再接任务链

## 合规

- 通用名词：筑基丹、灵石、符箓、本命法宝、颠倒五行阵（机制描述原创化）
- 强绑定专名仍不入公开核心 id

## 下一步建议

1. `region_cards/` 为慕兰/大晋各补 1 张设定卡  
2. `cultivation_methods.json` 与 `techniques` 挂接宗门 `technique_sources`  
3. Patchouli 增「阵法」「秘境四要素」静态页