# Phase 3 Repair Report — Breakthrough and Qi Deviation

> Date: 2026-06-18
> Scope: Phase 3 only
> Build: `./gradlew.bat --no-daemon --max-workers=1 build` succeeded
> Phase boundary: Did not implement Phase 4, skill-system expansion, full HUD work, alchemy, or quest content.

## Summary

This repair closes the Phase 3 Partial/Missing items from the audit and task board:

- Breakthrough attempts now show required material and current owned amount before consuming resources.
- Breakthrough attempts now show final success chance and all bonus sources before the roll.
- Injured meditation now adds qi-deviation risk and immediately performs a qi-deviation check when the risk threshold can apply.
- Over-tier technique use now adds qi-deviation risk and immediately performs a qi-deviation check when the risk threshold can apply.
- Existing four-tier qi-deviation effects are preserved and reused.
- Existing calming pill reduction and peaceful meditation decay logic are preserved.

## Code Changes

### Breakthrough preview

`BreakthroughService` now displays:

- Final breakthrough chance.
- Required material: `Breakthrough Pill`, owned count, required count, and whether pill aid/creative bypass is active.
- Bonus sources: base chance, spiritual root, pill, spirit eye/leyline, technique quality, obsession, and final chance.

The preview is sent before resource consumption, so the player can see the requirement and odds before the result is rolled.

### Shared qi-deviation trigger path

`PlayerCultivation` now exposes `rollQiDeviation(RandomSource)` so non-breakthrough trigger paths can reuse the same threshold and random-check logic as breakthrough failure.

`BreakthroughService.tryTriggerQiDeviation(...)` applies the existing four-tier effects from the shared implementation:

- Minor: cultivation loss and risk clear.
- Moderate: cultivation loss plus debuffs and risk clear.
- Severe: realm fall, debuffs, and equipment damage.
- Extreme: death and half-inventory drop.

### Meditation injury trigger

`ModEvents.onLivingHurt(...)` now:

- Adds the Phase 3 injured-meditation risk increase immediately when damage interrupts meditation.
- Runs the qi-deviation check after the risk increase.
- Reuses existing meditation stop behavior.

### Over-tier technique trigger

`ReleaseTechniquePacket` now:

- Keeps the existing over-tier risk increase.
- Runs the qi-deviation check after the risk increase.
- Stops technique execution if a qi-deviation event triggers.

### Build blockers repaired

The previous build blockers were corrected as part of validation:

- `LingGenTestStoneItem` static helper calls were made consistent.
- `SyncCultivationDataPacket` now has local helper methods for spirit-stone bonus and cushion detection.
- `SyncCultivationDataPacket` decode and client snapshot construction were realigned with the record fields.

## Validation

- Initial sandbox build failed because Gradle distribution download was blocked by sandbox networking.
- Authorized build reached compilation.
- Two long Gradle runs timed out and their Java processes were stopped.
- Final command succeeded:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 build
```

Result: `BUILD SUCCESSFUL in 43s`.

## Task Board Result

Phase 3 can now be marked complete because:

- Required material preview is implemented.
- Success chance and all bonus-source preview is implemented.
- Meditation injury directly triggers qi-deviation checks after risk increase.
- Over-tier technique use directly triggers qi-deviation checks after risk increase.
- Four-tier qi-deviation, calming pill, and peaceful meditation decay remain intact.
- Full build succeeded.

Current execution stage may advance to Phase 4: 练气期技能系统.

## Explicit Non-Goals

This repair did not implement:

- Phase 4 skills.
- HUD full redesign.
- New alchemy systems.
- Quest lines.
- Any Phase 4 gameplay content.
