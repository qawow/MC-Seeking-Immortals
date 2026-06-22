# Phase 5 Report - 涓硅嵂涓庣偧涓圭郴缁?> Date: 2026-06-19
> Scope: Phase 5 only, per latest user instruction
> Build: `.\gradlew.bat --no-daemon --max-workers=1 build` BUILD SUCCESSFUL in 36s
> Phase boundary: Did not implement Phase 6 绁炵灏忕摱 or any later system.

## Numbering Note

`docs/task-board.md` previously listed Phase 5 as 绁炵灏忕摱 and 鐐间腹 as Phase 7. The latest user instruction explicitly redefined the current work as Phase 5锛氫腹鑽笌鐐间腹绯荤粺 and asked to advance to Phase 6锛氱绉樺皬鐡?only after completion. This report follows that latest instruction and does not implement 绁炵灏忕摱.

## Implemented

- Reused existing pill items for 鍑濇皵涓?(`CULTIVATION_PILL`), 绛戝熀涓?(`FOUNDATION_BUILDING_PILL_LOW`), 绋崇涓?(`CALMING_PILL_LOW`), and 鍥炵伒涓?(`QI_RECOVERY_PILL`).
- Added 搴熶腹 (`WASTE_PILL`) and creative-tab registration.
- Added 涓圭倝 block and `AlchemyFurnaceBlockEntity`.
- Added a minimal static MVP alchemy recipe structure through `AlchemyRecipe`.
- Added four MVP recipes: 鍑濇皵涓? 绛戝熀涓? 绋崇涓? 鍥炵伒涓?
- Added server-side right-click鐐间腹 flow: consume materials and spiritual power, wait, then output pill, 搴熶腹, or small-chance explosion.
- Added success-rate hooks for alchemy skill, material quality, and spirit-land bonus. MVP uses base rate plus leyline bonus.
- Calibrated pill effects: 鍑濇皵涓?gives 1-hour x2 cultivation boost, 鍥炵伒涓?restores at least 50% max spiritual power, 绋崇涓?keeps -20 qi deviation risk, 绛戝熀涓?is required for Qi Refining 13 -> Foundation breakthrough.

## GUI / Menu Decision

No full Menu/Screen was added. The project has no existing menu/screen container pattern for blocks, and implementing one would expand the scope into client UI/network work. Phase 5 therefore uses a server-authoritative right-click MVP interaction with chat progress/status messages.

## Validation

Build command:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 build
```

Result: BUILD SUCCESSFUL (compileJava, test, build passed).

## Non-Goals

- Did not implement 绁炵灏忕摱.
- Did not enter Phase 6.
- Did not implement 鐐煎櫒, 鍒剁, 闃垫硶, high-tier pills, quests, NPCs, or mysterious-bottle alchemy enhancement.
- Did not implement a full GUI/Menu.
