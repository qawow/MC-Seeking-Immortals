0.1.89 note: 0.1.84 automated build/test state was completed by later coordinator records; manual in-game regression remains pending. Treat this checklist as historical/manual coverage guidance for later command, JEI/Patchouli, GUI, and resource checks.

# 0.1.84 Manual Regression Checklist

Status: pending until the main/coordinator build passes.

Last confirmed built truth before this checklist: `mod_version=0.1.83`, `ModNetwork.PROTOCOL_VERSION=8`.

## Build Gate

- [ ] Confirm `gradle.properties` records the final intended 0.1.84 version.
- [ ] Confirm `ModNetwork.PROTOCOL_VERSION` decision is recorded.
- [ ] Run final main build and record the exact command/result in `project_docs/step_progress.md` and the 0.1.84 update note.
- [ ] If build fails, record exact failure and do not mark 0.1.84 complete.

## Resource Validation

- [ ] Parse all JSON under `src/main/resources`.
- [ ] Validate item/block model `textures.layer*` references point to existing PNGs.
- [ ] Validate blockstates point to existing block models.
- [ ] Validate zh_cn/en_us localization for new items, blocks, entities, screens, commands, tooltips, JEI categories, and Patchouli entries.
- [ ] Search shipped resources for stale `xiuxian:` references.
- [ ] Check new survival blocks have loot tables and mining tags where appropriate.
- [ ] Check Patchouli pages load when Patchouli is present.
- [ ] Check no-Patchouli client startup still reaches the title screen.
- [ ] Check JEI alchemy display shows shipped recipes and does not promise unsynced datapack recipes.

## Qinglan Sect

- [ ] New player cannot join Qinglan before Seven Mysteries completion and Yue arrival.
- [ ] Eligible player can join Qinglan through dedicated steward flow.
- [ ] Legacy named-villager steward still works or is explicitly deprecated in docs.
- [ ] Old 0.1.81/0.1.82 Qinglan save migrates to a valid outer-disciple stage.
- [ ] Sect GUI opens at GUI Scale 1/2/3/Auto without clipping.
- [ ] Shop purchase success deducts contribution and grants the correct item.
- [ ] Shop purchase failure does not deduct contribution.
- [ ] Spirit Grass donation updates contribution once and handles inventory edge cases.
- [ ] Inventory-full rewards drop safely or are rejected with clear feedback.

## Skills And Flight

- [ ] Skill bar renders and tooltips fit at GUI Scale 1/2/3/Auto.
- [ ] Technique editor renders localized text in zh_cn and en_us without clipping.
- [ ] Big Dipper Sword Array displays cost 80 and cooldown 160 ticks / 8 seconds consistently.
- [ ] Big Dipper active swords clean up after duration, logout/death, and dimension change.
- [ ] Sword projectile renderer is visible and does not crash dedicated server startup.
- [ ] Foundation flying feedback appears when starting/stopping and mana drain still cancels safely.

## Alchemy, JEI, Patchouli

- [ ] JEI category opens and lists shipped alchemy recipes.
- [ ] Patchouli alchemy guide page opens when Patchouli is installed.
- [ ] Alchemy furnace recipes still load from shipped JSON after `/reload`.
- [ ] Existing 0.1.75 quality output behavior still works for low/mid/high/supreme pills.
- [ ] Earth-fire-room exact gate still rejects tier-5 fire for exact tier-4 Earth Fire recipes.

## High-Realm And Core Flows

- [ ] High-realm breakthrough resources still preview and consume the intended item.
- [ ] Gold core display persists after relog.
- [ ] Active tribulation HUD/screen lines still fit.
- [ ] Dragon Chant Body and Ice Marrow Body defect ticks still run server-side.
- [ ] Aggregate combat stats reflect realm, root, physique, body refinement, and gold core.

## Multiplayer/Sidedness Smoke

- [ ] Dedicated server starts without loading client-only classes.
- [ ] Client-only renderers/screens are registered only on the client side.
- [ ] Sect packets reject invalid actions server-side.
- [ ] Technique release still validates learned state, slot, cooldown, mana, and capability server-side.

## Sign-Off

- [ ] Coordinator records final build result.
- [ ] Coordinator updates 0.1.84 note from pending to complete or records blocker details.
- [ ] Coordinator reconciles `items.md`, `features.md`, `pending_requests.md`, `missing_and_placeholders.md`, and `docs/task-board.md` against final code/resources.