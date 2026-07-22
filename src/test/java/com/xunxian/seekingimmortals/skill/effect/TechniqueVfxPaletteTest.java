package com.xunxian.seekingimmortals.skill.effect;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure logic / source-contract tests for technique VFX depth.
 * Avoids constructing Profile (SoundEvents/ParticleTypes need full bootstrap).
 */
class TechniqueVfxPaletteTest {
    @Test
    void familyMappingCoversCorpusElements() {
        assertEquals(TechniqueVfxPalette.Family.FIRE, TechniqueVfxPalette.familyOf("fire"));
        assertEquals(TechniqueVfxPalette.Family.WATER, TechniqueVfxPalette.familyOf("water"));
        assertEquals(TechniqueVfxPalette.Family.METAL, TechniqueVfxPalette.familyOf("metal"));
        assertEquals(TechniqueVfxPalette.Family.WOOD, TechniqueVfxPalette.familyOf("wood"));
        assertEquals(TechniqueVfxPalette.Family.EARTH, TechniqueVfxPalette.familyOf("earth"));
        assertEquals(TechniqueVfxPalette.Family.WIND, TechniqueVfxPalette.familyOf("wind"));
        assertEquals(TechniqueVfxPalette.Family.ICE, TechniqueVfxPalette.familyOf("ice"));
        assertEquals(TechniqueVfxPalette.Family.THUNDER, TechniqueVfxPalette.familyOf("thunder"));
        assertEquals(TechniqueVfxPalette.Family.LIGHT, TechniqueVfxPalette.familyOf("light"));
        assertEquals(TechniqueVfxPalette.Family.DARK, TechniqueVfxPalette.familyOf("yin"));
        assertEquals(TechniqueVfxPalette.Family.SOUL, TechniqueVfxPalette.familyOf("soul"));
        assertEquals(TechniqueVfxPalette.Family.BLOOD, TechniqueVfxPalette.familyOf("blood"));
        assertEquals(TechniqueVfxPalette.Family.VOID, TechniqueVfxPalette.familyOf("void"));
        assertEquals(TechniqueVfxPalette.Family.ILLUSION, TechniqueVfxPalette.familyOf("illusion"));
        assertEquals(TechniqueVfxPalette.Family.NEUTRAL, TechniqueVfxPalette.familyOf("neutral"));
        assertEquals(TechniqueVfxPalette.Family.FIRE, TechniqueVfxPalette.familyOf("yang"));
        assertEquals(TechniqueVfxPalette.Family.DARK, TechniqueVfxPalette.familyOf("dark"));
        assertEquals(TechniqueVfxPalette.Family.BLOOD, TechniqueVfxPalette.familyOf("demonic"));
        assertEquals(TechniqueVfxPalette.Family.VOID, TechniqueVfxPalette.familyOf("space"));
        assertEquals(TechniqueVfxPalette.Family.FIRE, TechniqueVfxPalette.familyOf("烈焰真火"));
        assertEquals(TechniqueVfxPalette.Family.ICE, TechniqueVfxPalette.familyOf("水/玄冰"));
        assertEquals(TechniqueVfxPalette.Family.METAL, TechniqueVfxPalette.familyOf("青元剑诀"));
        assertEquals(TechniqueVfxPalette.Family.WOOD, TechniqueVfxPalette.familyOf("木灵藤蔓"));
        assertEquals(TechniqueVfxPalette.Family.EARTH, TechniqueVfxPalette.familyOf("山岳土元"));
        assertEquals(TechniqueVfxPalette.Family.THUNDER, TechniqueVfxPalette.familyOf("雷劫"));
        assertEquals(TechniqueVfxPalette.Family.SOUL, TechniqueVfxPalette.familyOf("神识魂魄"));
        assertEquals(TechniqueVfxPalette.Family.VOID, TechniqueVfxPalette.familyOf("空间裂隙"));
        assertEquals(TechniqueVfxPalette.Family.ILLUSION, TechniqueVfxPalette.familyOf("幻梦"));
    }

    @Test
    void familyCountIsFifteen() {
        assertEquals(15, TechniqueVfxPalette.Family.values().length);
    }

    @Test
    void areaSpellSourceDefinesExpandedElementVisuals() throws Exception {
        String area = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "skill", "effect", "spell", "ElementalAreaSpell.java"));
        for (String name : new String[]{
                "METAL_SHARD", "WOOD_BLOOM", "LIGHT_BURST", "SOUL_REND",
                "BLOOD_MIST", "VOID_RIFT", "ILLUSION_HAZE"
        }) {
            assertTrue(area.contains(name), name);
            assertTrue(area.contains("spawn" + toPascal(name)), "spawn method for " + name);
        }
    }

    @Test
    void resolverAndHighImpactConsumePalette() throws Exception {
        String resolver = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "skill", "effect", "AbstractTechniqueEffectResolver.java"));
        assertTrue(resolver.contains("TechniqueVfxPalette.profile"));
        assertTrue(resolver.contains("TechniqueVfxPalette.familyOf"));
        assertTrue(resolver.contains("AreaElement.METAL_SHARD"));
        assertTrue(resolver.contains("AreaElement.BLOOD_MIST"));
        assertTrue(resolver.contains("AreaElement.VOID_RIFT"));
        assertTrue(resolver.contains("AreaElement.ILLUSION_HAZE"));

        String highImpact = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "skill", "effect", "spell", "HighImpactTechniqueSpell.java"));
        assertTrue(highImpact.contains("TechniqueVfxPalette.profile"));
        assertTrue(highImpact.contains("vfx.burst"));
        assertTrue(highImpact.contains("vfx.family()"));

        String projectile = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "skill", "effect", "spell", "ElementalProjectileSpell.java"));
        assertTrue(projectile.contains("TechniqueVfxPalette.profile"));
        assertTrue(projectile.contains("castAt"));

        String palette = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "skill", "effect", "TechniqueVfxPalette.java"));
        assertTrue(palette.contains("case BLOOD"));
        assertTrue(palette.contains("case VOID"));
        assertTrue(palette.contains("case ILLUSION"));
        assertTrue(palette.contains("case SOUL"));
    }

    @Test
    void sharedPaletteDefinesCastPathScanTrailAndImpactFeedback() throws Exception {
        String palette = source("skill", "effect", "TechniqueVfxPalette.java");
        for (String method : List.of(
                "castAt", "path", "trailAt", "auraAt", "scanAt", "beamAt", "coneAt", "impactAt")) {
            assertTrue(palette.contains("void " + method + "("), method);
        }
        assertTrue(palette.contains("sendParticles"), "shared feedback must emit server particles");
        assertTrue(palette.contains("playSound"), "cast and impact paths must emit server sounds");
    }

    @Test
    void everyGenericRuntimeFamilyHasParticleAndSoundFeedbackPath() throws Exception {
        for (String className : List.of(
                "ElementalProjectileSpell", "ElementalAreaSpell", "TargetedDebuffSpell", "AreaDebuffSpell",
                "SelfBuffSpell", "RecoverySpell", "SwordTechniqueSpell", "HonestSummonSpell",
                "WallTechniqueSpell", "BuffZoneTechniqueSpell", "HighImpactTechniqueSpell",
                "TalismanConsumeSpell", "CommandTechniqueSpell", "CraftGateTechniqueSpell",
                "ElementalBeamSpell", "ElementalConeSpell")) {
            String source = source("skill", "effect", "spell", className + ".java");
            boolean particlePath = source.contains("TechniqueVfxPalette")
                    || source.contains("sendParticles")
                    || source.contains("CultivationFireballEntity")
                    || source.contains("SwordProjectileEntity");
            boolean soundPath = source.contains("TechniqueVfxPalette")
                    || source.contains("playSound")
                    || source.contains("CultivationFireballEntity")
                    || source.contains("SwordProjectileEntity");
            assertTrue(particlePath, className + " particle path");
            assertTrue(soundPath, className + " sound path");
        }
    }

    @Test
    void everyExecutableSpellClassHasParticleAndSoundFeedbackPath() throws Exception {
        Path spellRoot = Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals", "skill", "effect", "spell");
        Set<String> nonCastingClasses = Set.of(
                "SpellEffect", "QiGuidingPassive");
        List<Path> spellSources;
        try (var files = Files.list(spellRoot)) {
            spellSources = files
                    .filter(path -> path.getFileName().toString().endsWith("Spell.java")
                            || path.getFileName().toString().endsWith("Passive.java"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        assertTrue(spellSources.size() >= 45, "spell corpus unexpectedly shrank: " + spellSources.size());
        for (Path path : spellSources) {
            String className = path.getFileName().toString().replace(".java", "");
            if (nonCastingClasses.contains(className)) {
                continue;
            }
            String spell = Files.readString(path);
            assertTrue(hasParticleFeedback(spell), className + " particle feedback path");
            assertTrue(hasSoundFeedback(spell), className + " sound feedback path");
        }
    }

    @Test
    void weakFeedbackSpellsAndSwordProjectileUseSharedFeedback() throws Exception {
        Map<String, List<String>> contracts = Map.of(
                "InvisibilitySpell", List.of("castAt", "auraAt"),
                "LightBodySpell", List.of("castAt", "auraAt"),
                "EarthWallSpell", List.of("castAt", "path", "impactAt"),
                "EarthEscapeStepSpell", List.of("castAt", "path", "impactAt"),
                "FlyingSwordBeginnerSpell", List.of("castAt", "auraAt", "impactAt"),
                "FlyingSwordAdvancedSpell", List.of("castAt", "auraAt", "trailAt", "impactAt"),
                "DetectionSpell", List.of("castAt", "scanAt"),
                "DivineSenseExpansionPassive", List.of("castAt", "auraAt"),
                "FormationSenseSpell", List.of("castAt", "scanAt"));
        for (Map.Entry<String, List<String>> entry : contracts.entrySet()) {
            String source = source("skill", "effect", "spell", entry.getKey() + ".java");
            assertTrue(source.contains("TechniqueVfxPalette.profile"), entry.getKey());
            for (String method : entry.getValue()) {
                assertTrue(source.contains("." + method + "("), entry.getKey() + " -> " + method);
            }
        }

        String projectile = source("entity", "SwordProjectileEntity.java");
        assertTrue(projectile.contains("TechniqueVfxPalette.profile"));
        assertFalse(projectile.contains(".castAt("), "entity construction must not replay cast feedback");
        assertTrue(projectile.contains(".trailAt("), "sword projectile trail feedback");
        assertTrue(projectile.contains(".impactAt("), "sword projectile impact feedback");
    }

    @Test
    void swordCastFeedbackIsOwnedByPlayerEntryPoints() throws Exception {
        String projectile = source("entity", "SwordProjectileEntity.java");
        assertFalse(projectile.contains(".castAt("));
        assertTrue(projectile.contains("life % 4 == 0"), "projectile trail must be throttled");

        for (String className : List.of("SwordProjectileSpell", "MultiSwordArraySpell")) {
            String spell = source("skill", "effect", "spell", className + ".java");
            assertEquals(1, occurrences(spell, ".castAt("), className + " must cast-feedback once per execute");
        }
    }

    @Test
    void persistentEffectsUseBoundedServerParticleCalls() throws Exception {
        String palette = source("skill", "effect", "TechniqueVfxPalette.java");
        String trail = between(palette, "public void trailAt(", "public void auraAt(");
        String scan = between(palette, "public void scanAt(", "public void beamAt(");
        assertTrue(occurrences(trail, "sendParticles(") <= 3, "trail should use at most two moving batches");
        assertTrue(occurrences(scan, "sendParticles(") <= 5, "scan should use bounded batches plus anchors");

        String advanced = source("skill", "effect", "spell", "FlyingSwordAdvancedSpell.java");
        assertTrue(advanced.contains("tickCount % 10 != 0"), "advanced flying trail must run at 2Hz");
        assertFalse(advanced.contains("sendParticles("), "advanced flying should delegate batched trails");

        String detection = source("skill", "effect", "spell", "DetectionSpell.java");
        assertTrue(detection.contains("MAX_ENTITY_PARTICLE_MARKERS = 12"));
        assertTrue(detection.contains("MAX_BLOCK_PARTICLE_MARKERS = 12"));
        assertTrue(detection.contains("Math.min(16.0D, range), 36"));
    }

    @Test
    void genericBeamAndConeUseServerGeometryAndDedicatedVisuals() throws Exception {
        String resolver = source("skill", "effect", "AbstractTechniqueEffectResolver.java");
        assertTrue(resolver.contains("case \"beam\" -> new ElementalBeamSpell("));
        assertTrue(resolver.contains("case \"cone\" -> new ElementalConeSpell("));

        String beam = source("skill", "effect", "spell", "ElementalBeamSpell.java");
        assertTrue(beam.contains("ClipContext.Block.COLLIDER"));
        assertTrue(beam.contains("distanceToSegment("));
        assertTrue(beam.contains("canAffect(player, entity)"));
        assertTrue(beam.contains(".beamAt("));

        String cone = source("skill", "effect", "spell", "ElementalConeSpell.java");
        assertTrue(cone.contains("insideCone("));
        assertTrue(cone.contains("hasLineOfSight("));
        assertTrue(cone.contains("canAffect(player, entity)"));
        assertTrue(cone.contains(".coneAt("));
    }

    @Test
    void earthWallOnlySucceedsAfterPlacingBlocks() throws Exception {
        String wall = source("skill", "effect", "spell", "EarthWallSpell.java");
        int failureGuard = wall.indexOf("if (placed <= 0)");
        int castFeedback = wall.indexOf("vfx.castAt(");
        assertTrue(failureGuard >= 0 && castFeedback > failureGuard,
                "zero-placement guard must precede all success feedback");
        assertTrue(wall.substring(failureGuard, castFeedback).contains("return false;"));
    }

    @Test
    void summonArchetypeIsSyncedForClientRenderer() throws Exception {
        String servitor = source("entity", "SummonedServitorEntity.java");
        assertTrue(servitor.contains("EntityDataAccessor<Integer> DATA_ARCHETYPE"));
        assertTrue(servitor.contains("entityData.define(DATA_ARCHETYPE"));
        assertTrue(servitor.contains("entityData.set(DATA_ARCHETYPE"));
        assertTrue(servitor.contains("entityData.get(DATA_ARCHETYPE)"));
        assertTrue(servitor.contains("onSyncedDataUpdated"));
    }

    private static String source(String... relative) throws Exception {
        Path path = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals");
        for (String part : relative) {
            path = path.resolve(part);
        }
        return Files.readString(path);
    }

    private static boolean hasParticleFeedback(String source) {
        return source.contains("TechniqueVfxPalette.profile")
                || source.contains(".sendParticles(")
                || source.contains(".addParticle(")
                || source.contains("new CultivationFireballEntity(")
                || source.contains("new SwordProjectileEntity(")
                || source.contains("extends ElementalProjectileSpell");
    }

    private static boolean hasSoundFeedback(String source) {
        return source.contains("TechniqueVfxPalette.profile")
                || source.contains(".playSound(")
                || source.contains("new CultivationFireballEntity(")
                || source.contains("new SwordProjectileEntity(")
                || source.contains("extends ElementalProjectileSpell");
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(startIndex >= 0 && endIndex > startIndex, start + " -> " + end);
        return source.substring(startIndex, endIndex);
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String toPascal(String enumName) {
        StringBuilder out = new StringBuilder();
        for (String part : enumName.split("_")) {
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1).toLowerCase());
            }
        }
        return out.toString();
    }
}
