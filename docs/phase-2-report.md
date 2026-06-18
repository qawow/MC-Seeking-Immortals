# Phase 2 Report — Realm System

> Date: 2026-06-18
> Scope: User-specified Phase 2 only — Realm System
> Build: Failed current `./gradlew.bat build`
> Phase boundary: No Java code was changed; no spiritual-root identification, meditation GUI, HUD, alchemy, skill, texture/resource work, or Phase 3 expansion was implemented.

## Summary

The requested Phase 2 Realm System checklist has clear implementation evidence in the current source, but the checklist cannot be marked complete because the required `./gradlew.bat build` failed during `compileJava`. This pass audited the existing code, generated this report, and updated `docs/task-board.md`; it did not modify Java source or implement new gameplay behavior.

The current implementation includes:

- Realm/stage coverage for Mortal, Qi Refining layers 1-13, and Foundation Establishment early stage.
- Realm baseline configuration for `cultivationMax`, `manaMax`, `divSense`, and `hpBase`.
- Basic cultivation increase and mana recovery methods.
- Breakthrough success and failure handling.
- 20% cultivation rollback on breakthrough failure.
- `qiDevRisk` increase on breakthrough failure.

## Checklist Result

| Requirement | Status | Evidence |
| --- | --- | --- |
| RealmStage enum / realm configuration | Unknown: Build failed | `RealmStage` contains `MORTAL`, `LAYER_1` through `LAYER_13`, and `EARLY`; `RealmStageConfig` provides baseline tables. Cannot mark complete until build succeeds. |
| Mortal, Qi 1-13, Foundation Early | Unknown: Build failed | `PlayerCultivation.getStagesForRealmPublic()` returns Mortal for `MORTAL`, 13 Qi layers for `QI_REFINING`, and `EARLY` as first Foundation stage. Cannot mark complete until build succeeds. |
| `cultivationMax` | Unknown: Build failed | `PlayerCultivation#getCultivationMax()` dynamically returns `getCurrentStageCapExp()`. Cannot mark complete until build succeeds. |
| `manaMax` | Unknown: Build failed | `PlayerCultivation#getManaMax()` / `getManaMaxLong()` delegate to `getMaxSpiritualPower()`. Cannot mark complete until build succeeds. |
| `divSense` | Unknown: Build failed | `PlayerCultivation#getDivSense()` / `setDivSense(int)` read and write `divineConsciousness`. Cannot mark complete until build succeeds. |
| `hpBase` | Unknown: Build failed | `RealmStageConfig#getHpBase(Realm)` and `PlayerCultivation#getMaxHealthPoints()` are implemented. Cannot mark complete until build succeeds. |
| Basic cultivation increase method | Unknown: Build failed | `PlayerCultivation#addCultivationExp(int)`, `setCultivation(long)`, and meditation accumulation helpers exist. Cannot mark complete until build succeeds. |
| Basic mana recovery method | Unknown: Build failed | `PlayerCultivation#addSpiritualPower(int)`, `setMana(int)`, and `getManaRecoveryPerSecond()` exist. Cannot mark complete until build succeeds. |
| Breakthrough success | Unknown: Build failed | `PlayerCultivation#tryBreakthrough(...)` advances one stage, resets progress to the new stage start, and clears spiritual power. Cannot mark complete until build succeeds. |
| Breakthrough failure | Unknown: Build failed | `PlayerCultivation#tryBreakthrough(...)` returns failure status when the roll misses. Cannot mark complete until build succeeds. |
| Cultivation rollback 20% | Unknown: Build failed | Failure sets current-stage progress to 80% of the current stage span. Cannot mark complete until build succeeds. |
| `qiDevRisk` increase | Unknown: Build failed | Failure calls `addQiDeviationRisk(10)`. Cannot mark complete until build succeeds. |

## Notes

- This report intentionally treats the user-specified "Phase 2: Realm System" as the active scope for this pass. The older roadmap section named "Phase 2: 灵根系统与打坐修炼" remains outside this request and is not completed here.
- Existing tests in `Phase1CultivationSystemTest` cover stage mapping, baseline table values, core attribute compatibility, NBT round-trip, and derived attributes. No new tests were added because no Java behavior changed.
- The task board should remain on Phase 2 until the user explicitly chooses the next phase; this pass does not enter Phase 3.

## Validation

- `./gradlew.bat build`: FAILED during `:compileJava`.
- First blocking errors:
  - `LingGenTestStoneItem`: static context calls non-static `showResult`, `playEffects`, and `consumeUse`.
  - `SyncCultivationDataPacket`: missing helper methods `getMatchingPassiveBonus(...)` and `isSittingOnMeditationCushion(...)`.
  - `SyncCultivationDataPacket` / `ClientCultivationData.Snapshot`: constructor argument count mismatches.

Because build failed, no Realm System checklist item is marked complete for this pass.
