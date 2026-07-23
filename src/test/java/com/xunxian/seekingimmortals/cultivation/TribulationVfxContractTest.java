package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.visual.AuthoredVisualCatalog;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TribulationVfxContractTest {
    @Test
    void tribulationCoversGatheringStrikeSuccessFailureAndCollapse() throws Exception {
        String source = Files.readString(Path.of(
                "src", "main", "java", "com", "xunxian", "seekingimmortals",
                "cultivation", "TribulationService.java"));

        assertTrue(source.contains("emitTribulationStartVfx(player, targetRealm, strikeCount)"));
        assertTrue(source.contains("emitStrikeWarning(player, targetRealm, strikeNumber, totalStrikes)"));
        assertTrue(source.contains("emitStrikeImpact(player, targetRealm, strikeVfx, strikeNumber)"));
        assertTrue(source.contains("emitTribulationEndVfx(player, targetRealm, true)"));
        assertTrue(source.contains("emitTribulationEndVfx(player, targetRealm, false)"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.FORMATION"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.BEAM"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.IMPACT"));
        assertTrue(source.contains("TechniqueVfxPacket.Kind.DISSIPATE"));
        assertTrue(source.contains("TechniqueVfxPacket.Motif.RAIN"));
        assertTrue(source.contains("TechniqueVfxPacket.Motif.DOMAIN"));
        assertTrue(source.contains("STRIKE_WARNING_TICKS = 8"));
        assertTrue(source.indexOf("TechniqueVfxPacket.Kind.BEAM")
                < source.indexOf("TechniqueVfxPacket.Kind.IMPACT"));
        assertTrue(source.contains("TechniqueVfxPacket.Motif.RAIN, impact, impact,"));
        assertTrue(source.indexOf("emitStrikeImpact(player, targetRealm, strikeVfx, strikeNumber)")
                > source.indexOf("if (!damageAccepted)"));
        assertTrue(source.contains("AuthoredVisualCatalog.resolve(\"tribulation:\" + profileId)"));
        assertTrue(source.contains("VisualEventDispatcher.event(level, \"tribulation\", profileId"));
        assertTrue(source.contains("VisualEventPacket.Lifecycle.STOP"));
        assertTrue(source.contains("VisualEventDispatcher.entityKey(\"tribulation\", player, profileId)"));
        assertTrue(source.contains("TribulationRulesCatalog.Rule::id"));
        assertTrue(source.indexOf("emitStrikeWarning(player, targetRealm, strikeNumber, totalStrikes)")
                < source.indexOf("if (!cultivation.tickTribulationCountdown())"));
        assertTrue(source.indexOf("if (!cultivation.tickTribulationCountdown())")
                < source.indexOf("damageAccepted = player.hurt"));
    }

    @Test
    void everyPlayerTribulationRealmResolvesAnAuthoredProfile() {
        Map<Realm, String> expectedProfiles = Map.of(
                Realm.CORE_FORMATION, "minor_soul_trial",
                Realm.NASCENT_SOUL, "minor_soul_trial",
                Realm.SOUL_TRANSFORMATION, "heart_demon_or_thunder",
                Realm.VOID_REFINEMENT, "void_thunder",
                Realm.UNITY, "body_soul_dual",
                Realm.MAHAYANA, "great_ascension_thunder",
                Realm.TRIBULATION, "final_ascension",
                Realm.TRUE_IMMORTAL, "final_ascension");

        expectedProfiles.forEach((realm, expectedProfile) -> {
            String actualProfile = TribulationRulesCatalog.builtin().forRealm(realm)
                    .orElseThrow(() -> new AssertionError("Missing tribulation rule for " + realm))
                    .id();
            assertEquals(expectedProfile, actualProfile, realm.name());
            assertTrue(AuthoredVisualCatalog.resolve("tribulation:" + actualProfile).isPresent(),
                    () -> "Missing authored tribulation profile for " + realm + ": " + actualProfile);
        });
    }
}
