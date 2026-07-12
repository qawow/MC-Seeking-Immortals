# World Expansion v74

- **冥河/地渊**：`nether_river_campaign`、`diyuan_campaign` + 秘境 ref + hooks
- **慕兰天南**：`mulan_tianlan_war.json` 四阶段（斥候/大阵/圣禽/停战），对齐纪年 K3/K9/M4
- **材料**：7 条 v73 hooks 掉落/许可 ID 写入 `materials_catalog.json`
- **主线**：Ch1 挂慕兰战役；Ch6 挂地渊链

```bash
python3 scripts/expand_world_modules_v74.py
python3 -m pytest tests/test_canon_v74_nether_mulan.py -q
python3 scripts/pack_world.py
```