# 全量代码审查修复 — Batch C：HIGH 内容与战利品表

**完成时间**: 2026-07-22
**版本**: 0.2.164 → 0.2.165
**变更类别**: 数据（战利品表 JSON）+ 代码（世界生成）

---

## 修复内容

### #7 约 34 个方块破坏后无掉落（内容缺口）
**问题**: 46 个已注册方块中只有 26 个有战利品表。其余 35 个设置了 `.requiresCorrectToolForDrops()` 却无战利品表、也无代码掉落，破坏后什么都不掉，但它们位于创造物品栏且被玩法引用。
**修复**: 为其中 34 个可放置方块（各类 `*_formation_core`、`*_gate`、`refinement_forge*`、`talisman_table`、`puppet_assembly_bench`、`teleport_array_pedestal`、`blood_sacrifice_altar`、`ascension_gate`、`thunder_tribulation_altar`、`leyline_surface_marker` 等）新增“掉落自身”的战利品表（`survives_explosion` 条件）。
**例外**: `earth_wall` 是土系法术召唤的临时方块（200 tick 后自动移除），属法术效果而非可采集方块，故不添加战利品表。

### #8 灵脉（leyline vein）可能完全不生成（世界生成）
**文件**: `worldgen/LeylineVeinPieces.java`
**问题**: `postProcess` 通过 `getHeight(WORLD_SURFACE_WG, ...)` 把放置中心 Y 重新锚定到实时地表，但所有灵脉结构都使用 `terrain_adaptation: beard_thin`，会把地表抬升到包围盒 `box.maxY` 之上，导致 `box.isInside(...)` 剔除所有方块，灵脉静默地什么都不生成。
**修复**: 将重新锚定的 Y 夹取到 `[box.minY + 8, box.maxY - 2]` 区间内，确保中心始终落在包围盒中。

---

## 修改文件清单

1. `gradle.properties` — mod_version 0.2.164 → 0.2.165
2. `src/main/java/com/xunxian/seekingimmortals/worldgen/LeylineVeinPieces.java`
3. 新增 34 个战利品表：`src/main/resources/data/seeking_immortals/loot_tables/blocks/{ancient_rift_gate, ascension_gate, barrier_sect_protection_formation_core, blood_forbidden_gate, blood_sacrifice_altar, cycle_gate, defense_formation_core, demon_seal_pillar_formation_core, five_elements_mountain_formation_core, hidden_rift_gate, illusion_maze_formation_core, inverted_five_elements_formation_core, kill_sword_formation_core, king_territory_gate, leyline_surface_marker, ling_gen_identification_slab, long_range_teleport_array, mulan_wind_ride_formation_core, nether_ferry_gate, nine_dragon_flame_barrier_formation_core, puppet_assembly_bench, refinement_forge, refinement_forge_g2, refinement_forge_g3, seal_demon_formation_core, sect_gate_array, spirit_gathering_formation_core, spirit_gathering_minor_formation_core, sword_array_bagua_formation_core, talisman_table, teleport_array_pedestal, thunder_tribulation_altar, thunder_tribulation_array_formation_core, vajra_prison_formation_core}.json`

## 备份路径

`.bak/20260722_batch_c/`（LeylineVeinPieces.java 原始副本；战利品表为新增文件）

## 验证结果

`./gradlew build` — BUILD SUCCESSFUL（1m 17s），preflight 记录 mod_version=0.2.165。

## 版本与协议

mod_version 0.2.164 → 0.2.165。未改动网络包字段/顺序/编码，`ModNetwork.PROTOCOL_VERSION` 不变。
