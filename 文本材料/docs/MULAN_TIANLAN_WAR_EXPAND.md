# 慕兰 / 天澜 / 天南边境线（v31）

> 汇编 §9 异族势力 + §13 战争作为区域环境规则，非精确战力数值。

## 阵营

| id | 显示 | 主修 |
|----|------|------|
| `mulan_council` | 慕兰神师议会 | 法士、风/沙术、体修辅助 |
| `tianlan_temple` | 天澜圣殿 | 法士、圣兽唤灵 |
| `tiannan_seven_sects` | 天南七派联防 | 剑/符/丹/兽 |

## 功法

- `spirit_art_mulan` / `mulan_wind_spirit_art` — 慕兰法士入门
- `tianlan_holy_beast_art` — 天澜圣兽诀
- `tiannan_border_guard_art` — 天南守边（抗法士）

## 法术（`techniques_sample`）

风刃术、风沙术、唤兽术、剑雨术、缚灵术

## 事件

- `faction_conflict_events`: `tiannan_mulan_border_war`、`tianlan_holy_beast_ritual`、`seven_sects_joint_defense`
- `daily_random_events`: `mulan_wind_storm`
- `quest_hooks`: `hook_mulan_border_patrol`
- `quest_chains`: `chain_mulan_border`

## 特产（`items_by_region`）

慕兰：风行草、风行丹、风刃符、兽皮  
乱星海：珊瑚灵藻、海毒解丹  
冥河：冥雾兰、阴灵护符  

## 纪年

`E_mulan_invasion_wave`、`E_tianlan_saint_beast` — 见 `chronicle_events.json`

## 合规

素女轮回功等用 **（仿）**；圣兽机制为「唤兽术」+ 剧情祭，不绑定单一原著角色名。