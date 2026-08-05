package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y-A-2: authored per-layer spawn rosters must be live data, resolved fail-closed per entry.
 * Pure data/source contract tests (no Forge runtime).
 */
class SecretRealmLayerSpawnServiceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    private static SecretRealmCatalogService.LayerDef layer(String realmId, String layerId) {
        return SecretRealmCatalogService.find(realmId).orElseThrow()
                .layers().stream()
                .filter(def -> layerId.equals(def.id()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing layer " + realmId + "/" + layerId));
    }

    @Test
    void yinyangCombatLayersResolveToRealBestiaryEntries() {
        // Before the Y-A-2 id correction every yy_* roster resolved to nothing, so the
        // authored formation could never spawn. Each combat layer must now be spawnable.
        for (String layerId : List.of("yy_outer", "yy_split", "yy_yezha", "yy_yinzhi")) {
            List<SecretRealmLayerSpawnService.Resolved> roster =
                    SecretRealmLayerSpawnService.resolveRoster(layer("yinyang_ku", layerId));
            assertFalse(roster.isEmpty(), "authored roster missing for " + layerId);
            assertTrue(roster.stream().anyMatch(SecretRealmLayerSpawnService.Resolved::resolvable),
                    "no spawnable entry in " + layerId + ": " + roster);
        }
    }

    @Test
    void correctedSpawnIdsPointAtTheAuthoredYinyangFamily() {
        // The authored bestiary already carries a purpose-built yinyang_* family; the runtime
        // layers must reference those ids, not the ad-hoc names that never resolved.
        assertEquals(List.of("yinyang_mist_corpse", "yinyang_twin_beast"),
                resolvableIds("yinyang_ku", "yy_outer"));
        assertEquals(List.of("yinyang_lion_structure"),
                resolvableIds("yinyang_ku", "yy_split"));
        assertEquals(List.of("yinyang_yezha_adult", "yinyang_yezha_young"),
                resolvableIds("yinyang_ku", "yy_yezha"));
        assertEquals(List.of("yinyang_yinzhima", "yinyang_guard_beast", "yinyang_poacher_xiu"),
                resolvableIds("yinyang_ku", "yy_yinzhi"));
    }

    @Test
    void unresolvableAuthoredIdsFailClosedInsteadOfSubstituting() {
        // These three authored entries are environmental hazards with no bestiary entry.
        // They must be reported as unresolved, never silently replaced by another beast.
        assertEquals(List.of("poison_insect"),
                SecretRealmLayerSpawnService.unresolvedIds(layer("yinyang_ku", "yy_outer")));
        assertEquals(List.of("array_spirit"),
                SecretRealmLayerSpawnService.unresolvedIds(layer("yinyang_ku", "yy_split")));
        assertEquals(List.of("yin_sha_mist"),
                SecretRealmLayerSpawnService.unresolvedIds(layer("yinyang_ku", "yy_yezha")));
        assertTrue(SecretRealmLayerSpawnService.unresolvedIds(layer("yinyang_ku", "yy_yinzhi")).isEmpty(),
                "the habitat layer must be fully wired");

        // An unresolved entry never yields a beast id.
        SecretRealmLayerSpawnService.resolveRoster(layer("yinyang_ku", "yy_outer")).stream()
                .filter(resolved -> !resolved.resolvable())
                .forEach(resolved -> assertTrue(resolved.beastId().isBlank(),
                        "unresolved entry must not carry a substituted beast id: " + resolved));
    }

    @Test
    void otherRealmPrefixCorrectionsResolve() {
        // Pure prefix renames verified against the shipped bestiary.
        assertTrue(resolvableIds("tianlan_secret_grotto", "tl_foothill")
                .containsAll(List.of("tianlan_wind_blade_wolf", "tianlan_mulan_scout")));
        assertTrue(resolvableIds("thousand_bamboo_puppet_tower", "qz_l2")
                .contains("qianzhu_crossbow_puppet"));
        assertTrue(resolvableIds("void_palace", "xt_outer_court").contains("xutian_seal_spirit"));
        assertTrue(resolvableIds("guanghan_realm", "gh_approach").contains("guanghan_void_mite"));
    }

    @Test
    void consumerIsWiredAndBoundedAndSessionLatched() throws Exception {
        String trial = Files.readString(JAVA_ROOT.resolve("worldpack/SecretRealmTrialService.java"));
        String service = Files.readString(JAVA_ROOT.resolve("worldpack/SecretRealmLayerSpawnService.java"));

        // The roster consumer runs on realm entry, so LayerDef.spawns is no longer dead data.
        assertTrue(trial.contains("SecretRealmLayerSpawnService.spawnRealmLayers(player, id)"),
                "layer rosters must be consumed on secret realm entry");
        assertTrue(service.contains("layer.spawns()"), "the consumer must read the authored spawn list");

        // No session budget exists, so the service must cap requests itself.
        assertTrue(service.contains("MAX_LAYER_SPAWNS_PER_REQUEST"),
                "layer spawns must be capped per request");
        assertTrue(service.contains("MAX_LAYER_SPAWNS_PER_ENTRY"),
                "layer spawns must be capped per realm entry");

        // Re-entering the same session must not stack rosters.
        assertTrue(service.contains("root.getBoolean(sessionKey)")
                        && service.contains("root.putBoolean(sessionKey, true)"),
                "layer rosters must latch per session+layer");

        // Kills stay owner-bound like the existing patrol path.
        assertTrue(service.contains("SecretRealmTrialService.tagTrial("),
                "layer mobs must be bound to the session owner");
    }

    private static List<String> resolvableIds(String realmId, String layerId) {
        return SecretRealmLayerSpawnService.resolveRoster(layer(realmId, layerId)).stream()
                .filter(SecretRealmLayerSpawnService.Resolved::resolvable)
                .map(SecretRealmLayerSpawnService.Resolved::beastId)
                .toList();
    }
}
