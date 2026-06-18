# Phase 1 Repair Report

> Repair date: 2026-06-18
> Scope: Phase 1 only — Core Attributes and Realm System
> Build: `./gradlew.bat build` succeeded
> Phase boundary: No Phase 2, Phase 3, or Phase 4 features were implemented

## Summary

Phase 1 Partial/Missing items from `docs/phase-0-3-audit-report.md` were repaired without renaming or deleting existing internal fields. Existing storage names such as `cultivationExp`, `spiritualPower`, `divineConsciousness`, `bodyRefinement`, `qiDeviationRisk`, and `tribulationResistance` remain intact.

## Changes

### PlayerCultivation compatibility

Updated `PlayerCultivation` with stable Phase 1 semantic accessors while preserving legacy callers and NBT compatibility:

- `cultivation`: stable read/write through `getCultivationLong()`, `setCultivation(long)`, `setCultivationLong(long)`, and existing `getCultivation()` legacy int accessor.
- `cultivationMax`: dynamic read through `getCultivationMax()` and `getCultivationMaxInt()`.
- `mana`: stable read/write through `getMana()` and `setMana(int)`.
- `manaMax`: dynamic read through `getManaMax()`, `getManaMaxLong()`, and existing `getMaxSpiritualPower()`.
- `divSense`: stable read/write through `getDivSense()`, `setDivSense(int)`, and `setDivineConsciousness(int)`.
- `bodyRef`: stable read/write through `getBodyRef()` and `setBodyRef(int)`.
- `qiDevRisk`: supports legacy percent storage and normalized float access through `getQiDevRiskFloat()` / `setQiDevRisk(float)`.
- `tribRes`: supports legacy percent storage and normalized float access through `getTribResFloat()` / `setTribRes(float)`.

### NBT compatibility

- Existing legacy fields remain readable: `SpiritualPower`, `Qi`, `DivineConsciousness`, `CultivationExp`, `qiDevRisk`, and `tribRes`.
- Save output now includes both normalized semantic fields and percent compatibility fields for risk/resistance.
- No existing fields were removed from save/load.

### Realm / RealmStage mapping

- Added `getDesignId()` / `getDesignKey()` to `Realm` and `RealmStage`.
- Existing enum constants remain unchanged: `LAYER_1` through `LAYER_13`, `EARLY`, `MIDDLE`, `LATE`, `PEAK`.
- Added design mappings for `QI_1` through `QI_13` and `FOUNDATION_EARLY`, `FOUNDATION_MID`, `FOUNDATION_LATE`, `FOUNDATION_PEAK`.
- Added `PlayerCultivation.getStagesForRealmPublic(Realm)` to prove the functional stage coverage without exposing mutable internals.

### Tests

Added `Phase1CultivationSystemTest` with JUnit 5 coverage for:

- Qi Refining 1-13 and Foundation Establishment early/mid/late/peak design mappings.
- Phase 1 baseline table values: `manaBase`, `divSenseBase`, `hpBase`, and per-stage cultivation span.
- Core attribute compatibility read/write and NBT round trip.
- Legacy NBT migration.
- Derived attributes: attack, defense, max HP, mana recovery, cultivation gain, and flying speed.

## Validation

- `./gradlew.bat test`: BUILD SUCCESSFUL
- `./gradlew.bat build`: BUILD SUCCESSFUL

## Task Board Result

Phase 1 can now be marked complete because:

- Compatibility accessors provide the required design semantics without breaking existing fields or callers.
- RealmStage functional coverage is proven by design IDs and unit tests.
- Unit tests now exist and pass through Gradle.
- Full build succeeds.

Current execution stage should advance to Phase 2: 灵根系统与打坐修炼.

## Explicit Non-Goals

This repair did not implement:

- 灵根测试流程
- 打坐 GUI
- 突破系统
- 技能系统
- HUD
- 炼丹
- 神秘小瓶
- Phase 2, Phase 3, or Phase 4 features
