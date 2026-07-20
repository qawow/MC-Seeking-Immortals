package com.xunxian.seekingimmortals.sect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectSpecialtyGameplayServiceTest {
    @Test
    void everyPlayableSectHasExclusiveMethodDiscountAndMissionIncrement() {
        Set<String> methods = new HashSet<>();
        assertEquals(30, SectDefinitionService.playableDefinitions().size());

        for (SectDefinitionService.SectDefinition definition : SectDefinitionService.playableDefinitions()) {
            SectMasterDataService.Specialty specialty = SectMasterDataService.specialty(definition.id())
                    .orElseThrow(() -> new AssertionError("specialty missing for " + definition.id()));
            assertFalse(specialty.methodGrants().isEmpty(), "method grant missing for " + definition.id());
            assertTrue(specialty.shopDiscountPercent() > 0, "shop discount missing for " + definition.id());
            assertTrue(specialty.missionContributionBonus() > 0, "mission increment missing for " + definition.id());
            assertTrue(SectSpecialtyGameplayService.missionSkill(definition.id()).isPresent(),
                    "mission skill missing/invalid for " + definition.id());

            for (SectMasterDataService.MethodGrant grant : specialty.methodGrants()) {
                assertTrue(TextMaterialCatalogService.builtin().findMethod(grant.methodId()).isPresent(),
                        "unknown specialty method " + definition.id() + " -> " + grant.methodId());
                assertTrue(methods.add(grant.methodId()),
                        "specialty method must remain exclusive: " + grant.methodId());
            }

            int discounted = SectSpecialtyGameplayService.contributionCost(
                    definition.id(), definition.shopId(), SectContributionService.STAGE_OUTER_DISCIPLE, 100);
            assertTrue(discounted < 100 && discounted >= 75,
                    "invalid own-hall discount for " + definition.id() + ": " + discounted);
            assertTrue(SectSpecialtyGameplayService.missionContributionReward(
                    definition.id(), SectContributionService.STAGE_OUTER_DISCIPLE, 20) > 20,
                    "mission reward did not increase for " + definition.id());
        }
        assertEquals(30, methods.size());
    }

    @Test
    void rankScalingAndForeignShopGuardAreDeterministic() {
        assertEquals(92, SectSpecialtyGameplayService.contributionCost(
                "qinglan_sect", "qinglan_contribution_hall",
                SectContributionService.STAGE_OUTER_DISCIPLE, 100));
        assertEquals(87, SectSpecialtyGameplayService.contributionCost(
                "qinglan_sect", "qinglan_contribution_hall",
                SectContributionService.STAGE_INNER_DISCIPLE, 100));
        assertEquals(100, SectSpecialtyGameplayService.contributionCost(
                "qinglan_sect", "danxia_valley_contribution_hall",
                SectContributionService.STAGE_INNER_DISCIPLE, 100));
        assertEquals(25, SectSpecialtyGameplayService.missionContributionReward(
                "qinglan_sect", SectContributionService.STAGE_OUTER_DISCIPLE, 20));
        assertEquals(30, SectSpecialtyGameplayService.missionContributionReward(
                "qinglan_sect", SectContributionService.STAGE_INNER_DISCIPLE, 20));
        assertEquals(20, SectSpecialtyGameplayService.missionContributionReward(
                "missing", SectContributionService.STAGE_OUTER_DISCIPLE, 20));
    }

    @Test
    void authoredSpecialtyMapCoversAllPlayableCanonicalIds() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(Path.of(
                "src", "main", "resources", "data", "seeking_immortals",
                "text_material", "sect_specialty_map.json"))).getAsJsonObject();
        Set<String> ids = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray("sects")) {
            ids.add(SectDefinitionService.canonicalizeSectId(
                    element.getAsJsonObject().get("id").getAsString()));
        }
        for (SectDefinitionService.SectDefinition definition : SectDefinitionService.playableDefinitions()) {
            assertTrue(ids.contains(definition.id()), "source specialty missing for " + definition.id());
        }
    }

    @Test
    void runtimeWiringUsesSpecialtyAuthorityForAllThreeLoops() throws Exception {
        String manual = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "catalog", "ManualCatalogService.java"));
        String sect = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "sect", "SectContributionService.java"));
        String shop = Files.readString(Path.of("src", "main", "java", "com", "xunxian",
                "seekingimmortals", "shop", "ShopService.java"));

        assertTrue(manual.contains("SectMasterDataService.specialty(sectId)"));
        assertTrue(manual.contains("specialty.methodGrants()"));
        assertFalse(manual.contains("starterMethodForSect("));
        assertTrue(shop.contains("SectSpecialtyGameplayService.contributionCost("));
        assertTrue(shop.indexOf("int adjustedCost = SectSpecialtyGameplayService.contributionCost(")
                < shop.indexOf("progress.spendContribution(adjustedCost)"));
        assertTrue(sect.contains("SectSpecialtyGameplayService.missionContributionReward("));
        assertTrue(sect.contains("SectSpecialtyGameplayService.grantMissionPractice("));
        assertTrue(sect.contains("grantSectSpecialtyMethods("));
    }
}
