# Phase 5 Report - 丹药与炼丹系统
> Date: 2026-06-19
> Scope: Phase 5 only, per latest user instruction
> Build: `./gradlew --no-daemon --max-workers=1 build` BUILD SUCCESSFUL in 36s
> Phase boundary: Did not implement Phase 6 神秘小瓶 or any later system.

## Numbering Note

`docs/task-board.md` previously listed Phase 5 as 神秘小瓶 and 炼丹 as Phase 7. The latest user instruction explicitly redefined the current work as Phase 5：丹药与炼丹系统 and asked to advance to Phase 6：神秘小瓶 only after completion. This report follows that latest instruction and does not implement 神秘小瓶.

## Implemented

- Reused existing pill items for 凝气丹 (`CULTIVATION_PILL`), 筑基丹 (`FOUNDATION_BUILDING_PILL_LOW`), 稳神丹 (`CALMING_PILL_LOW`), and 回灵丹 (`QI_RECOVERY_PILL`).
- Added 废丹 (`WASTE_PILL`) and creative-tab registration.
- Added 丹炉 block and `AlchemyFurnaceBlockEntity`.
- Added a minimal static MVP alchemy recipe structure through `AlchemyRecipe`.
- Added four MVP recipes: 凝气丹、筑基丹、稳神丹、回灵丹
- Added server-side right-click 炼丹 flow: consume materials and spiritual power, wait, then output pill, 废丹, or small-chance explosion.
- Added success-rate hooks for alchemy skill, material quality, and spirit-land bonus. MVP uses base rate plus leyline bonus.
- Calibrated pill effects: 凝气丹 gives 1-hour x2 cultivation boost, 回灵丹 restores at least 50% max spiritual power, 稳神丹 keeps -20 qi deviation risk, 筑基丹 is required for Qi Refining 13 -> Foundation breakthrough.

## GUI / Menu Decision

No full Menu/Screen was added. The project has no existing menu/screen container pattern for blocks, and implementing one would expand the scope into client UI/network work. Phase 5 therefore uses a server-authoritative right-click MVP interaction with chat progress/status messages.

## Validation

Build command:

```bash
./gradlew --no-daemon --max-workers=1 build
```

Result: BUILD SUCCESSFUL (compileJava, test, build passed).

## Non-Goals

- Did not implement 神秘小瓶.
- Did not enter Phase 6.
- Did not implement 炼器, 制符, 阵法, high-tier pills, quests, NPCs, or mysterious-bottle alchemy enhancement.
- Did not implement a full GUI/Menu.
