package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecretRealmDimensionServiceTest {
    @Test
    void mapsDedicatedDimensions() {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("ancient_cultivator_ruins", "secret_realm_ancient_cultivator_ruins"),
                Map.entry("blood_forbidden", "secret_realm_blood_forbidden"),
                Map.entry("chaotic_sea_abyss_rift", "secret_realm_chaotic_sea_abyss_rift"),
                Map.entry("demon_gold_mountain", "secret_realm_demon_gold_mountain"),
                Map.entry("diyuan", "secret_realm_diyuan"),
                Map.entry("fallen_demon_depths", "secret_realm_fallen_demon"),
                Map.entry("fallen_demon_valley", "secret_realm_fallen_demon_valley"),
                Map.entry("guanghan_realm", "secret_realm_guanghan_realm"),
                Map.entry("jiuxian_seclusion", "secret_realm_jiuxian_seclusion"),
                Map.entry("kunwu_mountain", "secret_realm_kunwu_mountain"),
                Map.entry("minor_asura_realm", "secret_realm_minor_asura_realm"),
                Map.entry("mist_cave_trial", "secret_realm_mist_cave"),
                Map.entry("nether_river_land", "secret_realm_nether_river_land"),
                Map.entry("seven_meridian_cave", "secret_realm_seven_meridian_cave"),
                Map.entry("spirit_grass_valley", "secret_realm_spirit_grass_valley"),
                Map.entry("thousand_bamboo_puppet_tower", "secret_realm_thousand_bamboo_puppet_tower"),
                Map.entry("tianlan_secret_grotto", "secret_realm_tianlan_secret_grotto"),
                Map.entry("void_palace", "secret_realm_void_palace"),
                Map.entry("wild_ancient_ruins", "secret_realm_wild_ancient_ruins"),
                Map.entry("wild_ancient_tomb", "secret_realm_wild_ancient_tomb"),
                Map.entry("yinyang_ku", "secret_realm_yinyang_ku"),
                Map.entry("yin_mountain_catacomb", "secret_realm_yin_mountain_catacomb"));

        expected.forEach((realmId, dimensionPath) -> {
            assertEquals(
                    "seeking_immortals:" + dimensionPath,
                    SecretRealmDimensionService.dimensionIdFor(realmId).orElseThrow(),
                    realmId);
            assertTrue(Files.isRegularFile(Path.of(
                    "src/main/resources/data/seeking_immortals/dimension",
                    dimensionPath + ".json")), dimensionPath);
        });
        assertEquals(25, SecretRealmDimensionService.dedicatedDimensionCount());
    }
}
