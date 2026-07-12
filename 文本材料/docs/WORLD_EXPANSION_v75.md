# World Expansion v75

- **妖兽十三级**：`beast_thirteen_tier_map.json` 各阶 lore（结构分类，非精确战力）
- **spawn_tables**：区域 lore + 天渊/地渊/风元新表；尸妖阶位与图鉴对齐
- **天渊**：`tianyuan_merit_economy.json`、`tianyuan_daily_events.json`
- **Forge**：重跑 `generate_forge_item_registry.py`，v74 材料入册；`forge_scaffold` 补 model stub

```bash
python3 scripts/expand_world_modules_v75.py
python3 -m pytest tests/test_canon_v75_spawn_merit_registry.py -q
python3 scripts/pack_world.py
```