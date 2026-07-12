# World Expansion v76

- **乱星海**：`chaotic_sea_faction_war.json` 五阶段；星宫/逆星 factions + `chaotic_sea_civil_war` 步骤结构化
- **法宝十一级**：`artifact_tier_map.json`；`artifacts_catalog` 补 `artifact_grade` / `game_tier`
- **空间节点**：`spatial_nodes_catalog` 补 `dimension_from/to`（默认人界）

```bash
python3 scripts/expand_world_modules_v76.py
python3 -m pytest tests/test_canon_v76_chaotic_artifacts_nodes.py -q
python3 scripts/pack_world.py
```