# World Expansion v71

## 目标（对齐汇编）

- **§18 结构优先**：慕兰大战用「战役阶段」而非胜率刷怪；日循环用模板（采集/护送/灭妖/破阵）。
- **§9 势力**：乱星海星宫 vs 逆星盟日常与术法标签。
- **§13 秘境三段式**：破阵节点任务挂钩 `mulan_tianlan_war.json` 阶段。

## 新增/更新

| 文件 | 内容 |
|---|---|
| `data/daily_quest_templates.json` | 6 类日循环任务模板（含慕兰破阵、逆星走私、星宫稽征） |
| `data/daily_random_events.json` | 4 条慕兰战役日常 + 多条 lore 深化 |
| `data/mulan_tianlan_war.json` | 阶段 daily 挂钩、停战阶段 lore |
| `data/techniques/fashi.json` | 沙暴/圣羽/阵眼定灵 |
| `data/techniques/misc.json` | 逆星匿踪、星宫封灵爆 |
| `scripts/expand_world_modules_v71.py` | 一键合并与打包 |

## 运行

```bash
python3 scripts/expand_world_modules_v71.py
python3 -m pytest tests/test_canon_v71_mulan_daily.py -q
python3 scripts/pack_world.py
```

## 打包

- `seeking_immortals_world_v71.zip`（增量）
- `seeking_immortals_worldpack.zip`（全量，需 `pack_world.py`）