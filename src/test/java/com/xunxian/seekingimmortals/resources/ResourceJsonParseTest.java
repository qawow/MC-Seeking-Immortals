package com.xunxian.seekingimmortals.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceJsonParseTest {
    private static final Path PATCHOULI_GUIDE_ROOT = Path.of(
            "src/main/resources/data/seeking_immortals/patchouli_books/seeking_immortals_guide");

    @Test
    void shippedJsonResourcesParse() {
        List<Path> roots = List.of(
                Path.of("src/main/resources/assets/seeking_immortals/lang"),
                Path.of("src/main/resources/data/seeking_immortals/shops"),
                Path.of("src/main/resources/data/seeking_immortals/recipes"),
                Path.of("src/main/resources/data/seeking_immortals/alchemy"),
                Path.of("src/main/resources/data/seeking_immortals/artifacts"),
                PATCHOULI_GUIDE_ROOT);

        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            assertDoesNotThrow(() -> parseTree(root), "Failed to parse JSON under " + root);
        }
    }

    @Test
    void patchouliGuideUsesCanonicalBookLayout() throws Exception {
        assertTrue(Files.exists(PATCHOULI_GUIDE_ROOT.resolve("book.json")),
                "Patchouli requires book.json directly under the book id folder");
        assertFalse(Files.exists(PATCHOULI_GUIDE_ROOT.resolve("zh_cn/book.json")),
                "Language folders must not contain book.json; it makes the starter book invalid");
        assertFalse(Files.exists(PATCHOULI_GUIDE_ROOT.resolve("en_us/book.json")),
                "Language folders must not contain book.json; it makes the starter book invalid");

        assertLanguageContentMatches("categories");
        assertLanguageContentMatches("entries");
    }

    @Test
    void lowFlyingSwordRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_flying_sword_low.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("SIS", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("ISI", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("SIS", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:spirit_iron",
                recipe.getAsJsonObject("key").getAsJsonObject("I").get("item").getAsString());
        assertEquals("seeking_immortals:spirit_stone_shard",
                recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        assertEquals("seeking_immortals:flying_sword_low",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void qingyeFanRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_qingye_fan.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("BSS", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("B  ", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("BSS", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:ironwood",
                recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("seeking_immortals:spirit_stone_shard",
                recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        assertEquals("seeking_immortals:qingye_leaf_fan",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void spiritGatheringBeadRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_spirit_gathering_bead.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("S S", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("SGS", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(" S ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:soul_gathering_stone",
                recipe.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        assertEquals("seeking_immortals:spirit_stone_shard",
                recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        assertEquals("seeking_immortals:spirit_gathering_bead",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void yellowUmbrellaRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_yellow_umbrella.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("SSS", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("SCS", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("ICI", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:beast_core",
                recipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString());
        assertEquals("seeking_immortals:spirit_iron",
                recipe.getAsJsonObject("key").getAsJsonObject("I").get("item").getAsString());
        assertEquals("seeking_immortals:spirit_silk",
                recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        assertEquals("seeking_immortals:yellow_umbrella",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void storageBraceletRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_storage_bracelet.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(" K ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(" V ", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(" K ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:kunwu_copper",
                recipe.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString());
        assertEquals("seeking_immortals:void_crystal",
                recipe.getAsJsonObject("key").getAsJsonObject("V").get("item").getAsString());
        assertEquals("seeking_immortals:storage_bracelet_low",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void flyingNeedleSetRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_flying_needle_set.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(" S ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("III", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(" S ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:spirit_iron",
                recipe.getAsJsonObject("key").getAsJsonObject("I").get("item").getAsString());
        assertEquals("seeking_immortals:spirit_silk",
                recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        assertEquals("seeking_immortals:flying_needle_set",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void qingningMirrorRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_qingning_mirror.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(" C ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(" K ", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(" C ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:cold_jade",
                recipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString());
        assertEquals("seeking_immortals:kunwu_copper",
                recipe.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString());
        assertEquals("seeking_immortals:qingning_mirror",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void snakePearlRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_snake_pearl.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(" B ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("CCC", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(" B ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:true_dragon_blood",
                recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("seeking_immortals:beast_core",
                recipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString());
        assertEquals("seeking_immortals:snake_pearl",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void bedrockShieldRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_bedrock_shield.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("K K", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(" D ", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("seeking_immortals:diyuan_pressure_moss",
                recipe.getAsJsonObject("key").getAsJsonObject("D").get("item").getAsString());
        assertEquals("seeking_immortals:kunwu_copper",
                recipe.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString());
        assertEquals("seeking_immortals:bedrock_shield",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void talismanSoulCharmRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_talisman_soul_charm.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(" P ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("PSP", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("seeking_immortals:talisman_paper_mortal",
                recipe.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString());
        assertEquals("seeking_immortals:soul_gathering_stone",
                recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        assertEquals("seeking_immortals:talisman_treasure_soul_charm",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void evilIllusionMirrorRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_evil_illusion_mirror.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("MMM", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(" C ", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(" G ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:cold_jade",
                recipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString());
        assertEquals("seeking_immortals:soul_gathering_stone",
                recipe.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        assertEquals("seeking_immortals:cloud_mushroom",
                recipe.getAsJsonObject("key").getAsJsonObject("M").get("item").getAsString());
        assertEquals("seeking_immortals:evil_illusion_mirror",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void voidColdJadePendantRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_void_cold_jade_pendant.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(" C ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("C C", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("seeking_immortals:cold_jade",
                recipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString());
        assertEquals("seeking_immortals:void_palace_cold_jade_pendant",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void moonShadowDiskRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_moon_shadow_disk.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(" I ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("YIY", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("I I", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:spirit_iron",
                recipe.getAsJsonObject("key").getAsJsonObject("I").get("item").getAsString());
        assertEquals("seeking_immortals:yin_stone",
                recipe.getAsJsonObject("key").getAsJsonObject("Y").get("item").getAsString());
        assertEquals("seeking_immortals:moon_shadow_disk",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void blackGoldShieldRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_black_gold_shield.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("III", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("K K", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("III", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:spirit_iron",
                recipe.getAsJsonObject("key").getAsJsonObject("I").get("item").getAsString());
        assertEquals("seeking_immortals:kunwu_copper",
                recipe.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString());
        assertEquals("seeking_immortals:black_gold_shield",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void goldDemonChainRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_gold_demon_chain.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("GBG", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("K K", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("GB ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:gold_seam_stone",
                recipe.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        assertEquals("seeking_immortals:beast_core",
                recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("seeking_immortals:kunwu_copper",
                recipe.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString());
        assertEquals("seeking_immortals:gold_demon_chain",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void xuanguangShardRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_xuanguang_shard.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("SBS", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(" B ", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("S D", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:xuanguang_mirror_shard",
                recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        assertEquals("seeking_immortals:beast_core",
                recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("seeking_immortals:true_dragon_blood",
                recipe.getAsJsonObject("key").getAsJsonObject("D").get("item").getAsString());
        assertEquals("seeking_immortals:xuanguang_mirror",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void xuanhuangShardRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_xuanhuang_shard.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("SBS", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("BGB", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("S G", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:xuanhuang_mirror_shard",
                recipe.getAsJsonObject("key").getAsJsonObject("S").get("item").getAsString());
        assertEquals("seeking_immortals:beast_core",
                recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("seeking_immortals:soul_gathering_stone",
                recipe.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        assertEquals("seeking_immortals:xuanhuang_mirror",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void voidRefiningBellRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_void_refining_bell.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("FMF", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("MCM", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(" C ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:void_bell_fragment",
                recipe.getAsJsonObject("key").getAsJsonObject("F").get("item").getAsString());
        assertEquals("seeking_immortals:void_marrow",
                recipe.getAsJsonObject("key").getAsJsonObject("M").get("item").getAsString());
        assertEquals("seeking_immortals:void_crystal",
                recipe.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString());
        assertEquals("seeking_immortals:void_refining_bell",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void goldLightBrickRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_gold_light_brick.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("PPP", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("GBG", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("P P", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:talisman_paper_mortal",
                recipe.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString());
        assertEquals("seeking_immortals:gold_seam_stone",
                recipe.getAsJsonObject("key").getAsJsonObject("G").get("item").getAsString());
        assertEquals("seeking_immortals:beast_core",
                recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("seeking_immortals:gold_light_brick",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void talismanDemonSealRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_talisman_demon_seal.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("PPP", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(" D ", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("PBP", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:talisman_paper_mortal",
                recipe.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString());
        assertEquals("seeking_immortals:demon_suppress_talisman_blank",
                recipe.getAsJsonObject("key").getAsJsonObject("D").get("item").getAsString());
        assertEquals("seeking_immortals:beast_core",
                recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("seeking_immortals:talisman_treasure_demon_seal",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void natalEmbryoRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_natal_embryo.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("KKK", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("KBT", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("KK ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:kunwu_copper",
                recipe.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString());
        assertEquals("seeking_immortals:beast_core",
                recipe.getAsJsonObject("key").getAsJsonObject("B").get("item").getAsString());
        assertEquals("seeking_immortals:true_dragon_blood",
                recipe.getAsJsonObject("key").getAsJsonObject("T").get("item").getAsString());
        assertEquals("seeking_immortals:natal_artifact_embryo",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void fourSymbolsRulerRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_four_symbols_ruler.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(" K ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("KRK", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(" K ", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:eight_spirit_ruler_shard",
                recipe.getAsJsonObject("key").getAsJsonObject("R").get("item").getAsString());
        assertEquals("seeking_immortals:kunwu_copper",
                recipe.getAsJsonObject("key").getAsJsonObject("K").get("item").getAsString());
        assertEquals("seeking_immortals:four_symbols_ruler_replica",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    void threeFlameFanRecipeMatchesCurrentRefinementApproximation() throws Exception {
        JsonObject recipe = readObject(Path.of(
                "src/main/resources/data/seeking_immortals/recipes/refine_three_flame_fan.json"));

        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals("FFF", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("FHF", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("FFF", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("seeking_immortals:seven_flame_fan_replica",
                recipe.getAsJsonObject("key").getAsJsonObject("H").get("item").getAsString());
        assertEquals("seeking_immortals:phoenix_feather",
                recipe.getAsJsonObject("key").getAsJsonObject("F").get("item").getAsString());
        assertEquals("seeking_immortals:three_flame_fan_replica",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    private static void parseTree(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".json")).toList()) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonParser.parseReader(reader);
                }
            }
        }
    }

    private static JsonObject readObject(Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void assertLanguageContentMatches(String folder) throws Exception {
        Path zhRoot = PATCHOULI_GUIDE_ROOT.resolve(Path.of("zh_cn", folder));
        Path enRoot = PATCHOULI_GUIDE_ROOT.resolve(Path.of("en_us", folder));

        assertTrue(Files.isDirectory(zhRoot), "Missing zh_cn Patchouli " + folder + " folder");
        assertTrue(Files.isDirectory(enRoot), "Missing en_us Patchouli " + folder + " folder");
        assertEquals(relativeJsonFiles(zhRoot), relativeJsonFiles(enRoot),
                "Patchouli " + folder + " files must match between zh_cn and en_us");
    }

    private static List<String> relativeJsonFiles(Path root) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(root::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }
}
