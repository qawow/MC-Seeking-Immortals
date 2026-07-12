# World Expansion v72

## 对齐汇编

- **扩充方向 Q**：`main_story_chapters.json` Chapter 0～6（凡人→灵界）。
- **§13 秘境三段式**：血色禁地 / 虚天殿任务链 + `secret_realms` 挂钩。
- **§16 经济量级**：`economy_reference_magnitudes.json` 各境界**月收入**锚点（非官方定价）。

## 新增/更新

| 文件 | 内容 |
|---|---|
| `data/main_story_chapters.json` | 7 章主线与 quest/秘境/飞升引用 |
| `data/quest_chains.json` | `blood_forbidden_campaign`、`void_palace_campaign` |
| `data/secret_realms.json` | 两秘境 lore、队伍上限、任务链 ref |
| `data/daily_random_events.json` | 剩余 48 条短 lore 深化 |
| `data/ascension_flow.json` | 飞升阶段 lore + 章节 ref |
| `scripts/expand_world_modules_v72.py` | 一键执行 |

## 运行

```bash
python3 scripts/expand_world_modules_v72.py
python3 -m pytest tests/test_canon_v72_chapters_realms.py -q
python3 scripts/pack_world.py
```