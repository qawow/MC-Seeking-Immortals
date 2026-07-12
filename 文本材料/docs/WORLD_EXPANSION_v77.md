# World Expansion v77

- **大晋**：`dajin_factions.json`（万宝楼/合欢/阴罗/世家/太真）、`dajin_righteous_demon_border.json`、`dajin_righteous_demon_line` 任务链
- **符箓三等级**：`talisman_grade_map.json`；`talisman_catalog` 补全 `grade` 与 lore
- **维度**：`dimensions_catalog` 五界 + MC 维度 lore；`region_cards/dajin.json` 挂 factions

```bash
python3 scripts/expand_world_modules_v77.py
python3 -m pytest tests/test_canon_v77_dajin_talisman_dimensions.py -q
python3 scripts/pack_world.py
```