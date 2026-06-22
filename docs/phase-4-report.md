# Phase 4 Report — 练气期技能系统

> Date: 2026-06-19
> Scope: Phase 4 only
> Build: `./gradlew.bat --no-daemon --max-workers=1 build` succeeded
> Phase boundary: Did not implement Phase 5, 神秘小瓶, 炼丹, NPC, 任务线, 筑基/金丹技能, 完整 HUD, or texture/resource art work.

## Summary

Phase 4 is implemented through the existing `SkillType` / `CultivationSkill` / `SkillEffectRegistry` / technique-slot release pipeline. No second skill system was created.

The implementation completes:

- Automatic unlock for Phase 4 Qi Refining skills by realm and layer.
- Skill mana checks and cooldowns through the existing server-authoritative `ReleaseTechniquePacket`.
- All nine Qi Refining skills in the Phase 4 scope.
- MVP flying sword projectile support for sword attacks.
- MVP beginner sword flight using controlled flight ability with continuous mana drain.

## Implemented Skills

| Skill | Requirement | Cost / Cooldown | Implementation |
| --- | --- | --- | --- |
| 引气入体 | 练气1 | Passive | `QiGuidingPassive`; auto-unlocked and provides the passive cultivation entry marker |
| 火球术 | 练气3 | 10 mana / 2s | `FireballSpell` calibrated to Phase 4 cost and cooldown; uses vanilla `SmallFireball` |
| 冰锥术 | 练气3 | 10 mana / 2s | `IceConeSpell`; fires MVP projectile, damages and slows on hit |
| 雷击术 | 练气3 | 12 mana / 3s | `ThunderStrikeSpell`; raycasts target position and summons lightning |
| 土遁步 | 练气4 | 15 mana / 5s | `EarthEscapeStepSpell`; short forward teleport to a valid standing position |
| 御剑飞行初 | 练气7 | 5 mana/s | `FlyingSwordBeginnerSpell`; toggles MVP basic flight and drains mana while flying |
| 单剑刺击 | 练气7 | 20 mana / 1s | `SwordProjectileSpell`; fires one sword projectile |
| 灵气探测 | 练气10 | 5 mana / 10s | `DetectionSpell` calibrated; highlights living entities and nearby spirit/ore/herb-like blocks with particles |
| 三才剑阵 | 练气13 | 40 mana / 3s | `SwordProjectileSpell`; fires three sword projectiles |

## Code Changes

- `SkillType` now carries Phase 4 metadata: required stage, technique id, configured mana cost, and configured cooldown.
- `PlayerCultivation#unlockEligiblePhase4Skills()` unlocks eligible Qi skills and learns their corresponding technique ids.
- `ModEvents` calls Phase 4 auto-unlock on login and server tick, then syncs learned techniques.
- `ReleaseTechniquePacket` now uses registered `SkillEffect` cost/cooldown when an effect exists, preserving server-side mana and cooldown checks.
- `SkillEffectRegistry` registers all Phase 4 Qi skill effects.
- `SwordProjectileEntity` adds a minimal server-side projectile used by ice cone, single sword thrust, and three-talent sword array.
- `qi_refining_techniques.json` now includes explicit Phase 4 technique entries used by auto-unlock, slot binding, and client summaries.

## MVP Notes

- `FlyingSwordEntity` as a visible rideable entity was not added. The current user scope explicitly allowed an MVP minimum flying/movement implementation if no `FlyingSwordEntity` exists; `御剑飞行初` therefore uses controlled basic flight with 5 mana per second.
- Projectile rendering uses the existing empty renderer to avoid Phase 4 texture work. Effects remain game-verifiable through damage, slow, lightning, teleport, mana drain, cooldowns, and particles.

## Validation

- First build attempt failed before compilation because sandbox networking blocked Gradle wrapper download.
- Authorized build reached compilation and found one Phase 4-introduced Java capture error in `ReleaseTechniquePacket`.
- The capture error was repaired.
- Final validation:

```powershell
.\gradlew.bat --no-daemon --max-workers=1 build
```

Result: `BUILD SUCCESSFUL in 37s`.

## Task Board Result

Phase 4 can be marked complete because:

- All nine Qi Refining skills have unlock metadata, technique data, release effects, mana checks, and cooldowns.
- Automatic unlock by realm/stage is implemented and synced.
- Sword projectile MVP exists and is registered.
- Build succeeded.

Current execution stage should advance to Phase 5: 神秘小瓶系统.

## Explicit Non-Goals

This work did not implement:

- Phase 5 or any later phase.
- 筑基期技能, 金丹期技能, or higher-realm skills.
- 神秘小瓶.
- 炼丹.
- NPCs or quests.
- A full HUD redesign.
- New textures or art resources.
