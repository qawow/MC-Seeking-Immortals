# World Expansion v73

## 对齐汇编

- **§9 宗门专精**：`sect_specialty_map.json`（丹/阵/符/星宫/逆星/慕兰）。
- **§6–8 副职**：`craft_daily_loops.json` 炼丹/炼器/制符/阵法日课。
- **§13 秘境**：坠魔谷、昆吾山任务链 + hooks。

## 运行

```bash
python3 scripts/expand_world_modules_v73.py
python3 -m pytest tests/test_canon_v73_craft_realms.py -q
python3 scripts/pack_world.py
```