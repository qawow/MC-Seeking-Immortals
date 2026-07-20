package com.xunxian.seekingimmortals.skill.effect;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
