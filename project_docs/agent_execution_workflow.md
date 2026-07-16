# Agent Execution Workflow

This document is the current operational workflow for future AI agents working in this repository.
Read and follow it before implementing code, resource, build, gameplay, or regression tasks.

## Current Baseline

- Workspace: `/root/mc-mod`
- Mod: `seeking_immortals`
- Minecraft / Forge: `1.20.1` / `47.2.0`
- Current `mod_version`: read `gradle.properties` first; after the 0.1.80 review-fix work it is `0.1.80`.
- Current `ModNetwork.PROTOCOL_VERSION`: `7`.
- Normal build command: `./gradlew --no-daemon --max-workers=1 build`
- No-Patchouli dev client command: `./gradlew --no-daemon --max-workers=1 runClientNoPatchouli`

## Required Reading Order

Before substantive work, read:

1. `AGENTS.md`
2. `project_docs/ai_handoff.md`
3. `project_docs/step_progress.md`
4. This file: `project_docs/agent_execution_workflow.md`

If the task touches gameplay systems, items, version progress, planned features, UI, or regression, also read:

- `project_docs/items.md`
- `project_docs/pending_requests.md`
- `project_docs/features.md`
- `project_docs/missing_and_placeholders.md`

If the task touches frontend/client UI, HUD, screens, overlays, keybinds, client sync caches, tooltips, or UI resources, also read:

- `project_docs/frontend_refactor_handoff.md`

If the task touches broader fanren-style setting expansion, sects, secret realms, alchemy, artifact systems, spirit beasts, economy, or higher-realm roadmap, also read:

- `project_docs/fanren_setting_implementation_outline.md`

## Change Classification

Classify before editing:

- Docs-only / comments-only / ignore-file maintenance:
  - No `mod_version` bump required.
  - Build is optional unless the docs directly change build/release instructions that need validation.
- Code, resources, data packs, build logic, gameplay behavior, config that ships with the mod, or generated runtime behavior:
  - Bump `mod_version` in `gradle.properties` by one patch version before final build.
  - Run the normal Gradle build.
- Network packet field/order/type changes, encode/decode changes, or incompatible network channel behavior:
  - Bump `mod_version`.
  - Bump `ModNetwork.PROTOCOL_VERSION`.
  - Update encode/decode/handle together.

Do not bump `ModNetwork.PROTOCOL_VERSION` for unrelated edits in the `network/` package unless packet compatibility actually changed. The preflight script may warn about network paths; explain the protocol decision in the final report.

## Backup Rule

Before editing any existing file, create a backup under:

```text
.bak/<timestamp_or_version_label>/
```

Preserve relative paths. Example:

```text
.bak/20260630_0.1.68_no_patchouli_client/build.gradle
.bak/20260630_0.1.68_no_patchouli_client/gradle.properties
```

New files do not need a backup.

Never revert unrelated dirty work. The workspace often contains many historical changes.

## Editing Rules

- Keep changes scoped to the current request.
- Prefer existing project patterns and Forge 1.20.1 APIs.
- Keep gameplay authority server-side.
- Keep client-only code in client-side packages and events.
- Do not change packet schemas unless required.
- Use `apply_patch` for manual edits when possible.
- Be careful with old Markdown docs that may not be UTF-8. If editing them is risky, prefer appending ASCII-only notes or creating a new UTF-8 document.

## Build And Verification

After code/resource/build/gameplay/config changes, run:

```bash
./gradlew --no-daemon --max-workers=1 build
```

If build fails:

1. Fix the failure.
2. Re-run the build.
3. Do not report completion until the build succeeds or the exact blocker is documented.

The build runs `scripts/preflight.sh`. If preflight says shippable files changed but `mod_version` did not change, bump `mod_version` instead of bypassing the gate.

Only use `-PaiSkipVersionBumpCheck=true` for an explicitly documented emergency/manual override.

## Dev Client Regression

Normal client:

```bash
./gradlew --no-daemon --max-workers=1 runClient
```

No-Patchouli client:

```bash
./gradlew --no-daemon --max-workers=1 runClientNoPatchouli
```

Use `runClientNoPatchouli` for local single-player regression when Patchouli dev runtime blocks startup.

Current dev runtime facts:

- Patchouli optional support remains preserved for normal builds and releases.
- `runClientNoPatchouli` excludes Patchouli only from the nested dev runtime launch.
- Curios and JEI should remain available in `runClientNoPatchouli`.
- Forge dev runs set:
  - `mixin.env.remapRefMap=true`
  - `mixin.env.refMapRemappingFile=${buildDir}/createSrgToMcp/output.srg`
- This remap is needed so third-party runtime mods such as Curios can remap SRG refmaps in the official-mapped userdev environment.

Expected startup confirmation for the no-Patchouli path:

- Patchouli is absent from the loaded mod set.
- Curios and JEI are present.
- No Patchouli `AccessorScreen` crash.
- No Curios `AccessorEntity` crash.
- Client reaches render/resource loading, e.g. sound engine and texture atlases load.

Reaching startup is not the same as completing in-world regression.

## Single-Player Regression Checklist

After the client starts, perform or document the status of:

- Debug commands:
  - `/seeking_immortals debug set_cultivation <amount>`
  - `/seeking_immortals debug fill_mana`
  - `/seeking_immortals debug unlock_skills`
- Realm/HUD checks:
  - Mortal, Qi Refining 13, Foundation Establishment early, Core Formation, Nascent Soul, Spirit Transformation.
  - `/seeking_immortals realm`
  - HUD sync.
  - Real max health, attack, armor, and movement speed effects.
- Technique editor:
  - Learned list scrolling.
  - Drag binding.
  - Right-click clear.
  - Close/reopen persistence.
- Technique release:
  - 7 bound slots.
  - Mana insufficient rejection.
  - Cooldown sync.
- Foundation Establishment skills:
  - Advanced sword flight drains mana over time.
  - Aura body shield cancels damage.
  - Five elements escape lands safely.
  - Big Dipper sword array fires seven swords.
  - Formation sense particles render.
- Combat:
  - High-realm attacks against zombies/skeletons.
  - Being attacked by mobs.
  - Flying sword/fireball/sword array damage.
- Flight:
  - Artifact flight.
  - Advanced sword flight.
  - Continuous mana drain.
  - Auto stop on mana exhaustion.

If the client can start but manual in-world testing is not completed, say that explicitly.

## Documentation Updates

After a completed step, blocker, build verification, or phase wrap-up, update relevant docs:

- `project_docs/step_progress.md`
- `project_docs/features.md`
- `project_docs/items.md`
- `project_docs/pending_requests.md`
- `project_docs/missing_and_placeholders.md`
- `project_docs/updates/YYYYMMDD_0.1.X.md`

For docs-only workflow changes, creating or updating a dedicated docs file may be enough if touching old encoded Markdown files would risk corruption. State this choice in the final report.

## Final Report Requirements

Always include:

- Change class.
- Edited files.
- Backup path.
- `mod_version`.
- Protocol-version decision.
- Build result, or why build was not run.
- Remaining risk / unfinished regression.

For 0.1.80, the known remaining risk is:

- Full in-world single-player regression still needs to be completed for tribulation failure, Seven Mysteries marker interactions, packet-boundary behavior in multiplayer, and HUD/UI visuals.
