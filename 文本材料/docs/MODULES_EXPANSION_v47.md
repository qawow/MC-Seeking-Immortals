# 各模块扩充 v47

> 汇编 **慕兰法士**专精 · **天澜**御兽 · 战役**阶段结构**（非胜率表）

## 慕兰天澜战役

| 文件 | 内容 |
|------|------|
| `mulan_tianlan_war.json` | 四阶段：摩擦→大阵→圣禽→停战/升级 |
| 任务链 | `mulan_war_campaign`、`mulan_fashi_path`、`tianlan_defense_line` |
| 地区 | `mulan` v2、`tianlan.json` |
| 冲突 | `mulan_tianlan_war_outbreak` |

**机制标签**：`fashi_soul_burn`、法士护阵、圣禽祝福（声望 30+）、玩家可选慕兰/天南/中立商路。

## 功法术法

- **天澜御兽诀** → 天澜兽魂召唤类术法门控
- `fashi.json` v4 天澜术挂 `race_required` + `requires_method`

## Schema 扫描

- `schema_validation_report.json`：全 `data/**/*.json` 是否含 `schema_version`
- 缺省样本列表供后续批量补版本号

## 打包

**`seeking_immortals_lore_v47.zip`**

## 下一步

- 给 `missing_schema_version` 文件批量补 v1
- 突兀族世仇支线（mulan notes）
- MC 1.20.1 `ReloadListener` 构造对齐