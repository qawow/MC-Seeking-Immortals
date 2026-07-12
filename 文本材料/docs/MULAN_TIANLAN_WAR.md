# 慕兰草原 ↔ 突兀族 战争线大纲

> 纪年锚点：`CHRONICLE.md` K3、K9；势力：`faction_graph.json`；地区：`region_cards/mulan.json` + 待建 `tianlan.json`

## 背景

- **慕兰**：法士信仰圣禽，灵术不依赖传统灵根，草原灵气略低于天南宗门核心区。
- **突兀**：人族修士形态，供奉**天澜兽**（圣印），与慕兰争夺草原边缘灵脉与圣物。
- **圣印之约**：双方高层曾立约「圣印不可互杀」——违反者全族声望崩盘（`holy_peacock` / 天澜兽任务标记）。

## 时间轴（游戏内周期）

| 周期 ID | 名称 | 间隔（游戏日） | 效果 |
|---|---|---:|---|
| `mulan_tianlan_war_minor` | 边境摩擦 | 1440（约 1 现实日） | 小规模营地刷怪，双方日常+声望 |
| `mulan_tianlan_war_major` | 圣战征召 | 8640 | 玩家可选阵营，大型战场副本入口 |
| `holy_bird_blessing_window` | 圣禽赐福 | 与 major 后 480 | 慕兰声望≥30 可接祝福 buff |

## 声望链

| 行为 | 慕兰 | 突兀 | 天南中立 |
|---|---|---|---|
| 完成慕兰巡逻 | +15 | -10 | 0 |
| 完成突兀圣印试炼 | -10 | +15 | 0 |
| 击杀对方低阶战士 | +5 / -20 对称 | 对称 | -5 业力（可选） |
| 误伤圣禽/天澜兽幼体 | -100 | -100 | 通缉 |

## 任务线（草案）

1. **引子**：天南边境修士被卷入「烽火传讯」→ 选择旁观 / 助慕兰 / 助突兀。
2. **灵术入门**（慕兰线）：`spirit_art_wind_blade` 传授，转 `mulan_fashi` 子职业标记。
3. **圣印试炼**（突兀线）：护送圣印碎片，解锁天澜兽坐骑（后期）。
4. **决战事件**：`mulan_tianlan_war_major` 触发 → 世界 Boss 营帐 / 双方神师 NPC 对峙（不强制击杀，可谈判结局）。

## 与妖兽图鉴

- 草原：`wind_screech_hawk`、`sky_tiger_beast`（高阶需慕兰声望）。
- 圣禽：仅剧情/祝福，**不可击杀**（`beast_bestiary.json` → `holy_peacock`）。

## 数据挂钩

```text
chronicle_events: K3, K9, M4
sects: mulan_fashi_council, tianlan_temple
spawn_tables: region mulan + event beast_tide
techniques_sample: spirit_art_wind_blade
```