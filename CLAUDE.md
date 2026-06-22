# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**寻仙问道 (Seeking Immortals)** — Minecraft Java Edition **1.20.1** Forge **47.2.0** mod.

- Mod id: `seeking_immortals`
- Java: 17
- Build system: ForgeGradle 6.x via Gradle Wrapper
- Current version: `0.1.51` in `gradle.properties`
- Theme: original 凡人流修仙 gameplay — 灵力、境界、灵根、寿元、功法/术法、灵石、丹药、材料、符箓、聚灵阵、修仙 UI and combat attributes.

This working directory is commonly used without Git history. Before modifying existing files, create a backup under `.bak/<timestamp>/` or `backups/<timestamp>/` and report the rollback path.

## Build Commands

```bash
# Build JAR (output: build/libs/seeking_immortals-{version}.jar)
./gradlew build

# Run client for development
./gradlew runClient

# Run dedicated server
./gradlew runServer

# Generate data
./gradlew runData
```

**Required:** after every code change, run `./gradlew build` and report the result. Before release/build-oriented work, update `mod_version` in `gradle.properties` using the `0.1.X` format.

## Mandatory AI Workflow

Codex and Claude Code must follow the same fixed order on this repository:

1. Read current state docs before substantive work: `project_docs/ai_handoff.md` and `project_docs/step_progress.md`; also read `items.md`, `pending_requests.md`, `features.md`, and `missing_and_placeholders.md` when the task touches items, gameplay systems, version progress, or planned features.
2. Classify the change before editing:
   - **Docs-only / comments-only / ignore-file maintenance**: no `mod_version` bump required.
   - **Code, resources, data packs, build logic, gameplay behavior, config that ships with the mod, or generated runtime behavior**: bump `mod_version` in `gradle.properties` by one patch version (`0.1.X`) before the final build.
   - **Network packet field/order/type changes or incompatible channel behavior**: bump both `mod_version` and `ModNetwork.PROTOCOL_VERSION`.
3. Create a backup of every existing file that will be edited under `.bak/<timestamp>/` or `backups/<timestamp>/`, preserving relative paths.
4. Implement the smallest scoped change that satisfies the task.
5. Update project docs and add/update a note under `project_docs/updates/` when a step, blocker, build verification, or phase wrap-up is completed.
6. Run `./gradlew build` after code/resource/build changes. The Gradle build runs `scripts/preflight.ps1` first to catch missing `mod_version` bumps.
7. If the build fails, fix the failure and rerun the build. Do not report completion until the build succeeds or the blocker is documented with exact failure details.
8. Final report must include: change class, edited files, backup path, `mod_version`, protocol-version decision, build result, and any remaining risk.

Only use `./gradlew build -PaiSkipVersionBumpCheck=true` for an explicitly documented emergency/manual override, and explain why the version gate was skipped.

## Source of Truth

Use current source/resources as truth:

- Java source: `src/main/java/com/xunxian/seekingimmortals/`
- Assets: `src/main/resources/assets/seeking_immortals/`
- Data packs / recipes / worldgen / Patchouli: `src/main/resources/data/`
- Project docs: `project_docs/`

Do **not** treat generated or historical folders as current implementation truth unless explicitly asked:

- `build/`, `bin/`, `.gradle/`, `run/`, `run-data/`
- `.bak/`, `backups/`, `manual_backups/`
- `.jdks/`, `.codegraph/`

## Project Documentation Workflow

Before substantive development, read:

1. `project_docs/ai_handoff.md`
2. `project_docs/step_progress.md`

If the task touches items, systems, version progress, or planned features, also read:

- `project_docs/items.md`
- `project_docs/pending_requests.md`
- `project_docs/features.md`
- `project_docs/missing_and_placeholders.md`

`project_docs/step_progress.md` states a local maintenance rule: after each completed step, blocker, build verification, or phase wrap-up, update the progress/docs and add an update note under `project_docs/updates/` when appropriate.

## Core Architecture

### Entry Point and Registration

- `SeekingImmortalsMod` registers items, blocks, entities, creative tab, and network channel on the mod event bus.
- Registration package: `registry/`
  - `ModItems` — all item and block-item registration
  - `ModBlocks` — `spirit_ore`, `meditation_cushion`, `spirit_gathering_array`
  - `ModEntities` — `cushion_seat` helper entity for meditation cushion sitting
  - `ModCreativeTabs` — all visible items are added to the 寻仙问道 creative tab

Registration pattern:

```java
public static final RegistryObject<Item> MY_ITEM = ITEMS.register("my_item",
    () -> new MyItem(new Item.Properties()));
```

For any new visible item/block also add:

1. `ModCreativeTabs` entry
2. Chinese localization in `assets/seeking_immortals/lang/zh_cn.json`
3. English localization in `assets/seeking_immortals/lang/en_us.json` when applicable
4. Item/block model JSON under `assets/seeking_immortals/models/`
5. Texture under `assets/seeking_immortals/textures/`
6. Recipe/loot/worldgen JSON if the feature requires it

### Cultivation System

Package: `cultivation/`

- `PlayerCultivation` is the central player state class.
  - Stores spiritual power, divine consciousness, realm/stage, cultivation exp, lifespan/age, spiritual roots, special physique, afflictions, learned techniques, 7 technique slots, cooldowns, and skill map.
  - Technique slot count is `PlayerCultivation.TECHNIQUE_SLOT_COUNT == 7`.
- `CultivationProvider` exposes Forge Capability persistence.
- `CultivationHelper` is the preferred helper for capability access where available.
- `Realm` defines 10 realms: 炼气 → 真仙.
- `RealmStage` handles layer/stage display and scaling.
- `SpiritualRoot`, `SpiritualRootAttribute`, `LingGenCalculator`, `SpecialPhysique`, and `TechniqueAffinityCalculator` implement root/affinity/progression rules.
- `ImmortalAffliction` and fields in `PlayerCultivation` handle severe injury, heart demon, shattered core, and realm fall scars.

Capability access pattern:

```java
CultivationHelper.get(player).ifPresent(cultivation -> {
    cultivation.addSpiritualPower(amount);
    cultivation.addCultivationExp(exp);
});
```

When direct capability access is necessary:

```java
player.getCapability(CultivationProvider.CULTIVATION).ifPresent(cultivation -> {
    // server-authoritative logic here
});
```

### Skill and Technique Systems

There are two related systems:

1. Enum/code skill system:
   - `skill/SkillType` defines cultivation methods, spells, crafting skills, and special skills.
   - `CultivationSkill` stores level/experience/proficiency/unlocked state.
   - `skill/effect/SkillEffect`, `SkillContext`, and `SkillEffectRegistry` define executable effects.
   - Current registered spell effects include Fireball, Detection, Invisibility, Light Body, and Earth Wall.

2. Resource-driven technique manual system:
   - Technique JSON files live under `data/seeking_immortals/cultivation/`.
   - Current built-in data files cover Qi Refining, Foundation Establishment, Core Formation, Nascent Soul, Spirit Transformation+, and special/common techniques.
   - `TechniqueDataManager` loads server-side technique entries from the resource manager.
   - `ClientTechniqueData` loads built-in summaries client-side for HUD/tooltips and tracks synced learned techniques, slots, and cooldowns.
   - `TechniqueManualItem` learns techniques by source name and syncs results to the client.

Technique release flow:

1. Client key or UI slot → `ReleaseTechniquePacket(slot)`
2. Server validates slot bounds, learned technique, cooldown, and spiritual power
3. Server consumes spiritual power and sets cooldown
4. Server sends `SyncCultivationDataPacket` and `SyncLearnedTechniquesPacket`
5. Client HUD/tooltips update from synced data

Network packets must assume the client is untrusted. Always validate slot indices, learned state, costs, cooldowns, and player capability server-side.

### Item Systems

Important item classes:

- `SpiritStoneItem`
  - Stores spiritual power in NBT tag `StoredSpiritualPower`
  - Toggle absorption with NBT tag `AbsorbingSpiritualPower`
  - Single-stack absorption only (`stack.getCount() == 1`)
  - Supports neutral and five-element spirit stones with passive bonus matching spiritual root attribute
- `ImmortalJadeItem` — high-tier currency with glint
- Legacy/simple pills: `QiRecoveryPillItem`, `CultivationPillItem`, `BreakthroughPillItem`
- Quality pill framework: `item/pill/`
  - `PillQuality`: LOW/MIDDLE/HIGH/PERFECT multipliers
  - `BasePillItem` with per-pill `consumePill()` implementations
  - Current low-tier registered examples include Rejuvenation, Foundation Building, Healing, Clear Spirit Powder, and Fasting Pill
- Materials: `item/material/`
  - `MaterialType`, `MaterialCategory`, `MaterialRarity`, `BaseMaterialItem`
  - Current material system includes spiritual herbs, beast materials, minerals, and special materials
- Talismans/tools:
  - `FireTalismanItem`, `ArmorTalismanItem`, `SpeedTalismanItem`
  - `SpiritCharmItem` for Curios
  - `LingGenTestStoneItem`, `SpiritDetectorItem`, `LeylineCompassItem`

### Blocks, Aura, and World Data

- `MeditationCushionBlock` uses `CushionSeatEntity` to seat the player for meditation.
- `SpiritGatheringArrayBlock` contributes aura formation bonus and reports local aura info on use.
- `SpiritualAuraManager` computes aura concentration from dimension, biome, deterministic leyline hash, and nearby gathering arrays.
- Spirit ore worldgen resources:
  - `data/seeking_immortals/worldgen/configured_feature/spirit_ore.json`
  - `data/seeking_immortals/worldgen/placed_feature/spirit_ore.json`
  - `data/seeking_immortals/forge/biome_modifier/add_spirit_ore.json`

### Combat System

Package: `combat/`

- `CombatStats` derives attack, defense, crit chance, crit damage, dodge chance, and accuracy from cultivation realm.
- `CombatCalculator` performs hit → dodge → raw damage → crit → defense mitigation → minimum damage pipeline.
- `DamageResult` carries final damage and combat flags for feedback.
- `CultivationStatsScreen` displays synced combat attributes in the player cultivation UI.

Known review risks to keep in mind before extending combat:

- PvP damage replacement currently hooks `LivingDamageEvent`; calling `defender.hurt(...)` from the same event path can recurse unless guarded or moved to a more appropriate event phase.
- `LivingDamageEvent` fires after vanilla armor/absorption processing, so canceling/reapplying damage can consume resources before cultivation miss/dodge logic.
- `CombatCalculator#getCombatStats` can return null if capability access fails; defensive handling is needed before broadening combat usage.

### Events and Server Tick Logic

`event/ModEvents` handles:

- Capability attachment and clone copying
- Server player tick cultivation/spiritual power gain
- Meditation movement/hunger/monster interruption
- Spirit stone passive and active absorption
- Curios spirit charm spiritual power bonus
- Immortal affliction effects
- Login sync and guide book grant
- Villager spirit stone exchange
- Commands registration
- Damage modifiers and current PvP combat hook

Keep gameplay state changes server-side. Use packets only to request actions from client to server and to sync final server state back to client.

### Network Synchronization

Package: `network/`

Current packets registered in `ModNetwork`:

- `SetMeditatingPacket`
- `SyncLearnedTechniquesPacket`
- `SyncCultivationDataPacket`
- `ReleaseTechniquePacket`
- `SetTechniqueSlotPacket`

`ModNetwork.PROTOCOL_VERSION` is currently `5`. If packet fields/order change, bump the protocol version to prevent mismatched clients/servers from decoding stale packet formats.

Client state mirrors:

- `ClientCultivationData` — realm, spiritual power, root, afflictions, aura, combat attributes, etc.
- `ClientTechniqueData` — learned techniques, 7 slots, cooldowns, built-in tooltip summaries

### Client UI and Input

Client-only code lives under `client/` and is registered through `ClientEvents` with `Dist.CLIENT`.

- `ClientEvents`
  - Registers key mappings
  - Registers GUI overlays
  - Adds the “修仙” button to the vanilla inventory screen
  - Resets client sync state on login/logout/respawn/local join
- `CultivationStatsScreen`
  - Independent screen, returns to inventory when opened from inventory
  - Renders single-page sections: 基础状态, 战斗属性, 灵根信息, 功法信息, 负面状态
- `TechniqueSkillBarOverlay`
  - Left-side 7-slot HUD
- `BreathingHudOverlay`
  - Meditation/breathing HUD and progress bar
- `TechniqueEditScreen`
  - Technique slot editor with drag/drop binding and right-click clear
- `ImmortalUiSkin`
  - Shared native Minecraft/Forge UI drawing helpers

Default key behavior:

- Meditation key: `V`
- Technique edit key: unbound by default
- 7 technique release keys: unbound by default

Never load client-only classes from common/server initialization paths.

## Dependencies and Compatibility

Configured dependencies:

- Minecraft Forge: `minecraft_version=1.20.1`, `forge_version=47.2.0`
- Curios: mandatory in `mods.toml`, compileOnly API + runtimeOnly dev dependency
- Patchouli: optional in `mods.toml`, compileOnly API + runtimeOnly dev dependency
- JEI: optional in `mods.toml`, compileOnly API + runtimeOnly dev dependency

Compatibility flags:

- `compat/ModCompat.java`
  - `CURIOS_LOADED`
  - `JEI_LOADED`
  - `PATCHOULI_LOADED`
  - `JADE_LOADED` (detection only; no dependency currently declared)
  - `GECKOLIB_LOADED` (detection only; no dependency currently declared)

Important: because Patchouli is optional in `mods.toml`, any direct Patchouli API usage should be guarded or isolated so the mod does not crash when Patchouli is absent.

## Resources and Data

Important resource locations:

- Localization:
  - `assets/seeking_immortals/lang/zh_cn.json`
  - `assets/seeking_immortals/lang/en_us.json`
- Item models: `assets/seeking_immortals/models/item/`
- Block models/blockstates: `assets/seeking_immortals/models/block/`, `assets/seeking_immortals/blockstates/`
- Textures:
  - blocks: `assets/seeking_immortals/textures/block/`
  - items: `assets/seeking_immortals/textures/item/`
  - GUI: `assets/seeking_immortals/textures/gui/`
- Recipes: `data/seeking_immortals/recipes/`
- Loot tables: `data/seeking_immortals/loot_tables/`
- Patchouli guide book: `data/seeking_immortals/patchouli_books/seeking_immortals_guide/zh_cn/`
- Curios slot/entity data: `data/curios/`
- Vanilla tags: `data/minecraft/`

Technique manual placeholder textures are intentionally present for many manuals. Do not delete placeholder resources unless replacing them with real textures and updating models consistently.

## Commands

Main command root: `/seeking_immortals`

Current subcommands include:

- `lingli` / `qi` — show spiritual power and cultivation exp
- `realm` — show realm, age/lifespan, breakthrough chance
- `root` — show spiritual root, attributes, purity, physique, multipliers
- `breakthrough` — attempt breakthrough
- `affliction severe_injury|heart_demon|realm_fall|shattered_core` — operator-only test/debug afflictions

## Development Rules and Conventions

- Match existing Java style: concise classes, minimal comments except for gameplay formulas or non-obvious rules.
- Use official Mojang mappings names for Minecraft 1.20.1 Forge APIs.
- Keep gameplay authority on the server.
- For client actions, send small intent packets; never trust client-provided costs, technique IDs, cooldown state, or cultivation values.
- Use `Component.translatable(...)` for player-facing text when adding durable UI/messages.
- Add localization keys to both Chinese and English lang files when practical.
- Prefer enum/resource-driven additions over hard-coded scattered special cases.
- If adding a new packet or changing packet fields, update encode/decode/handle together and bump `ModNetwork.PROTOCOL_VERSION`.
- If adding a new item/block/entity, register it through `registry/`, add it to the creative tab, add resources/localization, and verify with `./gradlew build`.
- If adding optional-mod integration, guard with `ModList`/`ModCompat` and avoid unconditional references from code paths that run without that mod.

## Known Pending Areas

Project docs list broader pending work. Current major future systems include:

- Life skills system
- Special skills system
- Artifact / magic treasure system
- Multi-layer GUI systems such as storage/sect/trading
- More complete world generation, secret realms, spirit veins, and spirit beasts
- NPC AI, sect NPCs, merchants, dialogue, and quests

Before implementing any of these, read the relevant `project_docs/` files and update progress/update logs when finished.
