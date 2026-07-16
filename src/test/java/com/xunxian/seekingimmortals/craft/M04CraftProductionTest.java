package com.xunxian.seekingimmortals.craft;

import com.xunxian.seekingimmortals.item.pill.PillQuality;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M04 craft-production structural coverage (no Forge registry required).
 */
class M04CraftProductionTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path ALCHEMY_RECIPES =
            ROOT.resolve("src/main/resources/data/seeking_immortals/alchemy/recipes");
    private static final Path PILL_MAP =
            ROOT.resolve("src/main/resources/data/seeking_immortals/alchemy/pill_material_name_map.json");
    private static final Path PILL_EFFECTS =
            ROOT.resolve("src/main/resources/data/seeking_immortals/alchemy/pill_effect_catalog.json");
    private static final Path GARDEN =
            ROOT.resolve("src/main/resources/data/seeking_immortals/text_material/garden_liquid_calendar_v108.json");
    private static final Path PILL_QUALITY =
            ROOT.resolve("src/main/resources/data/seeking_immortals/text_material/pill_quality.json");

    @Test
    void alchemyRecipeFilesCoverAtLeastOneHundredFourteenPills() throws Exception {
        assertTrue(Files.isDirectory(ALCHEMY_RECIPES), "alchemy recipes dir missing");
        long count;
        try (Stream<Path> stream = Files.list(ALCHEMY_RECIPES)) {
            count = stream.filter(p -> p.getFileName().toString().endsWith(".json")).count();
        }
        // 114 catalog recipes + curated aliases may exceed 114
        assertTrue(count >= 114, "expected >=114 alchemy recipe files, got " + count);
    }

    @Test
    void pillNameMapHasOneHundredFourteenEntries() throws Exception {
        String json = Files.readString(PILL_MAP);
        int entries = countOccurrences(json, "\"pills_catalog_id\"");
        assertEquals(114, entries, "pill_material_name_map should cover 114 catalog pills");
    }

    @Test
    void pillEffectCatalogHasOneHundredFourteenEntries() throws Exception {
        String json = Files.readString(PILL_EFFECTS);
        int entries = countOccurrences(json, "\"pill_id\"");
        assertEquals(114, entries, "pill_effect_catalog should cover 114 catalog pills");
    }

    @Test
    void pillQualityEnumMatchesDesignIds() {
        assertEquals(4, PillQuality.values().length);
        assertEquals(PillQuality.LOW, PillQuality.fromId("inferior"));
        assertEquals(PillQuality.MIDDLE, PillQuality.fromId("medium"));
        assertEquals(PillQuality.MIDDLE, PillQuality.fromId("middle"));
        assertEquals(PillQuality.HIGH, PillQuality.fromId("superior"));
        assertEquals(PillQuality.PERFECT, PillQuality.fromId("supreme"));
        assertEquals(PillQuality.PERFECT, PillQuality.fromId("perfect"));
        assertEquals(PillQuality.MIDDLE, PillQuality.MEDIUM);
        assertEquals(PillQuality.PERFECT, PillQuality.SUPREME);
        assertEquals(0.7D, PillQuality.LOW.getEffectMultiplier(), 1e-9);
        assertEquals(1.0D, PillQuality.MIDDLE.getEffectMultiplier(), 1e-9);
        assertEquals(1.25D, PillQuality.HIGH.getEffectMultiplier(), 1e-9);
        assertEquals(1.5D, PillQuality.PERFECT.getEffectMultiplier(), 1e-9);
        assertEquals(PillQuality.PERFECT, PillQuality.fromQualityScore(0.99D));
        assertEquals(PillQuality.LOW, PillQuality.fromQualityScore(0.1D));
    }

    @Test
    void gardenLiquidCalendarAndPillQualityAreShipped() {
        assertTrue(Files.isRegularFile(GARDEN), "garden_liquid_calendar_v108 must be shipped");
        assertTrue(Files.isRegularFile(PILL_QUALITY), "pill_quality.json must be shipped");
    }

    @Test
    void talismanAndPuppetBlueprintCountsMatchCorpus() {
        assertEquals(24, TalismanCraftService.recipeBlueprintCount());
        assertEquals(7, PuppetCraftService.recipeBlueprintCount());
    }

    @Test
    void refinementSerializerRecipesCoverG1ToG3() throws Exception {
        Path recipes = ROOT.resolve("src/main/resources/data/seeking_immortals/recipes");
        assertTrue(Files.isDirectory(recipes));
        long count;
        try (Stream<Path> stream = Files.list(recipes)) {
            count = stream
                    .filter(p -> p.getFileName().toString().endsWith("_serializer.json"))
                    .filter(p -> {
                        try {
                            String text = Files.readString(p);
                            return text.contains("seeking_immortals:refinement")
                                    && text.contains("forge_grade");
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
        }
        assertTrue(count >= 40, "expected many refinement serializer recipes for G1-G3, got " + count);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
