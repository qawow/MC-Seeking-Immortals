# Phase 0-3 Audit Report

> Audit date: 2026-06-18
> Scope: Phase 0 to Phase 3 only
> Constraints: no Java changes, no feature implementation, no Phase 4 work

## Audit Method

- Reviewed `docs/task-board.md`, `docs/mvp-scope.md`, `docs/implementation-roadmap.md`, `project_docs/ai_handoff.md`, and `project_docs/step_progress.md`.
- Checked current implementation evidence under `src/main/java` and `src/main/resources`.
- Did not count generated, backup, or historical folders as implementation truth.
- Ran `./gradlew.bat build` after the audit.

## Build Result

- Command: `./gradlew.bat build`
- Result: BUILD SUCCESSFUL in 27s
- Notes: `compileTestJava NO-SOURCE` and `test NO-SOURCE`; there are no test sources, so any task requiring unit-test evidence remains incomplete.

## Overall Conclusion

- Phase 0: Complete.
- Phase 1: Partial. Core implementation and build are present, but unit-test evidence is missing.
- Phase 2: Partial. Root system and meditation runtime are mostly present, but the independent `MeditationScreen` and complete meditation speed-detail GUI are missing; root testing is item-based, not a stone-slab block.
- Phase 3: Partial. Breakthrough and qi deviation are mostly implemented, including calming pill and risk decay, but pre-breakthrough material ownership preview is incomplete and over-tier technique usage only adds risk instead of directly triggering a qi-deviation check.
- Current recommended execution phase: Phase 1, because it is the earliest incomplete Phase.

## Phase 0: Pre-Implementation Architecture Review

Status: Done

Evidence:

- Architecture survey exists: `docs/phase-0-project-survey.md`.
- Task-board Phase 0 items are documentation/audit tasks and are supported by the generated survey.
- The survey covers existing cultivation state, realm/stage structure, spiritual root system, capability access, skill/technique systems, pill system, gaps, and required modules.

Conclusion:

- Phase 0 remains `[x]`.
- No Phase 0 blocker found during this audit.

## Phase 1: Core Attributes and Realm System

Status: Partial

Done evidence:

- `PlayerCultivation` stores the core cultivation state: spiritual power, divine consciousness, body refinement, qi deviation risk, tribulation resistance, realm/stage, and cultivation experience in `src/main/java/com/xunxian/seekingimmortals/cultivation/PlayerCultivation.java:28`.
- Long/float compatibility getters exist for cultivation, cultivation max, mana max, qi deviation risk, and tribulation resistance in `src/main/java/com/xunxian/seekingimmortals/cultivation/PlayerCultivation.java:76`.
- `RealmStage` covers mortal, Qi Refining layers 1-13, Foundation Establishment early/middle/late/peak, and higher placeholders in `src/main/java/com/xunxian/seekingimmortals/cultivation/RealmStage.java`.
- `RealmStageConfig` provides `manaBase`, `divSenseBase`, `hpBase`, attack, defense, recovery, and flight-speed baseline values in `src/main/java/com/xunxian/seekingimmortals/cultivation/RealmStageConfig.java`.
- Runtime derived cultivation attributes exist on `PlayerCultivation`; combat attack/defense are represented through `CombatStats` and `CombatCalculator`.
- `/seeking_immortals realm` is registered through `SeekingImmortalsCommand` and displays realm/cultivation details.

Partial / Missing / Unknown:

- Missing: no test sources were found; Gradle reported `compileTestJava NO-SOURCE` and `test NO-SOURCE`. Therefore “境界属性基准表单元测试” and “衍生属性计算方法实现并通过单元测试” cannot be marked complete.
- Partial: `PlayerCultivation` storage uses legacy internal names/types (`int`-backed `cultivationExp`, `spiritualPower`) with compatibility getters, not literal `long cultivation` / `long cultivationMax` fields.
- Partial: `RealmStage` uses `LAYER_1` to `LAYER_13` and `EARLY/MIDDLE/LATE/PEAK`, not literal `QI_1` / `FOUNDATION_EARLY` enum constants. Functionally it covers the required stages.

Conclusion:

- Phase 1 cannot be marked complete because unit-test evidence is explicitly absent.
- Earliest incomplete Phase is Phase 1.

## Phase 2: Spiritual Roots and Meditation Cultivation

Status: Partial

Done evidence:

- Six spiritual-root categories and their cultivation speed, qi recovery, breakthrough, and pill-absorption modifiers exist in `src/main/java/com/xunxian/seekingimmortals/cultivation/SpiritualRoot.java`.
- `SpiritRootType` also covers the six simplified categories in `src/main/java/com/xunxian/seekingimmortals/cultivation/SpiritRootType.java`.
- `LingGenCalculator` implements randomized root generation with very rare heavenly roots and higher low-talent probability.
- `PlayerCultivation#createLingGenIfAbsent` permanently writes the generated root and the tested flag.
- `LingGenTestStoneItem` is an interactive item entry point and calls root creation/sync in `src/main/java/com/xunxian/seekingimmortals/item/LingGenTestStoneItem.java:36`.
- `/seeking_immortals root` displays spiritual-root data and modifiers.
- Meditation runtime logic exists in `ModEvents.onPlayerTick`, including spiritual power gain, cushion-based cultivation gain, aura/technique/stone bonuses, hunger/monster/movement interruption, sync, and risk decay.
- `MeditationCushionBlock` supports interaction and meditation seating.
- `BreathingHudOverlay` displays meditation HUD data and progress.

Partial / Missing / Unknown:

- Partial: “灵根鉴定石板” is implemented as an item (`LingGenTestStoneItem`), not as a dedicated stone-slab block; `ModBlocks` only registers spirit ore, meditation cushion, and spirit gathering array.
- Partial: first interaction root testing is available through the item, but the roadmap’s NPC/block stone-slab flow is not present.
- Missing: no independent `MeditationScreen` class was found.
- Partial: cultivation and meditation information is visible via `BreathingHudOverlay` and `CultivationStatsScreen`, but no dedicated meditation GUI shows the full speed formula/details as specified.
- Partial: meditation cultivation gain exists, but the implementation uses 5-second settlement with aura/technique/stone adjustments rather than the exact per-tick formula listed in the task board.
- Partial: injury while meditating interrupts meditation through `LivingHurtEvent`; risk +2% is applied while health is below max during meditation, not directly in the same hurt-interrupt path.

Conclusion:

- Phase 2 remains incomplete.
- Keep incomplete GUI, block/entry-flow, and formula-detail items unchecked with Partial or Missing notes.

## Phase 3: Breakthrough and Qi Deviation

Status: Partial

Done evidence:

- `BreakthroughService` checks final stage, breakthrough cap, breakthrough resources, consumes the resource, previews chance, and applies result in `src/main/java/com/xunxian/seekingimmortals/cultivation/BreakthroughService.java:24`.
- Breakthrough can be triggered through a client key, `AttemptBreakthroughPacket`, command, and the cultivation screen button.
- Breakthrough chance includes base chance, spiritual root modifier, pill bonus, leyline/spirit-eye bonus, technique-quality bonus, and obsession bonus.
- Consecutive-failure obsession bonus is +5% per failure, capped at +30%, and clears on success.
- Success advances the stage and clears current progress; failure rolls progress back and adds +10% qi-deviation risk.
- Four qi-deviation tiers and their effects are implemented in `PlayerCultivation` and `BreakthroughService`: minor cultivation loss, moderate cultivation loss plus debuffs, severe realm fall plus equipment damage, and extreme death plus 50% inventory drop.
- `CalmingPill` reduces qi-deviation risk by 20 in `src/main/java/com/xunxian/seekingimmortals/item/pill/CalmingPill.java:7` and is registered through `ModItems`.
- Peaceful meditation risk decay exists in `ModEvents`: every 720 seconds reduces risk by 1, equivalent to -5% per hour; leyline meditation adds extra decay.

Partial / Missing / Unknown:

- Partial: pre-breakthrough UI shows chance and bonus details, but no complete “required materials and current owned amount” preview list was found. Missing resources are only reported when the attempt fails the resource check.
- Partial: over-tier technique usage adds +5% qi-deviation risk in `ReleaseTechniquePacket`, but it does not directly run the four-tier qi-deviation trigger/check.
- Partial: meditation injury increases risk while hurt and interrupts meditation, but direct qi-deviation tier triggering from injury was not found.
- Unknown: no automated tests prove breakthrough and qi-deviation edge cases.

Conclusion:

- Phase 3 remains incomplete.
- Several task-board items previously marked incomplete can now be upgraded where evidence is clear and build passed: calming pill and peaceful meditation risk decay are implemented.
- Phase 3 completion marker remains unchecked due to incomplete material preview and direct trigger chains.

## Required Task Board Updates

- Set current Phase to Phase 1, the earliest incomplete Phase.
- Keep Phase 0 complete.
- Change Phase 1 completion marker to unchecked because unit-test evidence is missing.
- Keep Phase 2 incomplete and clarify item-based root testing, missing independent `MeditationScreen`, and Partial meditation formula/detail evidence.
- Keep Phase 3 incomplete but mark confirmed implemented items as `[x]` where evidence is clear and build passed, especially calming pill and peaceful meditation decay.
- Do not mark Phase 4 as current, because Phase 1 to Phase 3 are not all complete.
