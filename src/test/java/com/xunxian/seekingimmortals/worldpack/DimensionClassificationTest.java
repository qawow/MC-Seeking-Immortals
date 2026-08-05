package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M-A: dimension state classification. Four honest classes replace the old
 * playable/deferred pair, so an abstract template and a logical cluster id stop being
 * counted as "unimplemented work", and an empty datapack shell stops advertising itself
 * as enterable.
 */
class DimensionClassificationTest {
    private static final Path DATA = Path.of("src", "main", "resources", "data", "seeking_immortals");
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void abstractTemplateAndLogicalClusterAreNotUnimplementedWork() {
        // Direction reversal: these two are honest architecture, not a backlog.
        DimensionRegistryService.DimensionDef template =
                DimensionRegistryService.find("seeking_immortals:secret_realm_instance").orElseThrow();
        assertEquals(DimensionRegistryService.DimensionClass.ABSTRACT_TEMPLATE, template.dimensionClass());

        DimensionRegistryService.DimensionDef cluster =
                DimensionRegistryService.find("seeking_immortals:yin_underworld").orElseThrow();
        assertEquals(DimensionRegistryService.DimensionClass.LOGICAL_CLUSTER, cluster.dimensionClass());

        List<String> pending = DimensionRegistryService.deferredIds();
        assertFalse(pending.contains("seeking_immortals:secret_realm_instance"),
                "an instantiation template is not pending implementation work");
        assertFalse(pending.contains("seeking_immortals:yin_underworld"),
                "a logical cluster id is not pending implementation work");

        // Neither is enterable, so neither may appear in the playable list either.
        Set<String> playable = DimensionRegistryService.snapshot().playable().stream()
                .map(DimensionRegistryService.DimensionDef::id)
                .collect(Collectors.toSet());
        assertFalse(playable.contains("seeking_immortals:secret_realm_instance"));
        assertFalse(playable.contains("seeking_immortals:yin_underworld"));
    }

    @Test
    void pendingListIsDeduplicatedAndOnlyPreviewLocked() {
        List<String> pending = DimensionRegistryService.deferredIds();
        assertEquals(pending.size(), Set.copyOf(pending).size(),
                "ingestRegistry and ingestIndex both add markers; the list must be unique");
        assertFalse(pending.isEmpty(), "the empty shells must still be reported honestly");

        // Only preview_locked counts as real pending work now.
        for (String id : pending) {
            assertEquals(DimensionRegistryService.DimensionClass.PREVIEW_LOCKED,
                    DimensionRegistryService.find(id).orElseThrow().dimensionClass(),
                    "only preview-locked shells are pending work: " + id);
        }
        assertTrue(pending.contains("seeking_immortals:immortal_realm"));
        assertTrue(pending.contains("seeking_immortals:asura_realm"));
    }

    @Test
    void emptyShellsAreDemotedFromPlayableAndCannotBeTravelledTo() throws Exception {
        // immortal_realm is authored `playable: false` but had a blank status, so isDeferred()
        // was false and the catalog command reported it as "可进入".
        DimensionRegistryService.DimensionDef immortal =
                DimensionRegistryService.find(DimensionRegistryService.IMMORTAL_REALM).orElseThrow();
        assertEquals(DimensionRegistryService.DimensionClass.PREVIEW_LOCKED, immortal.dimensionClass());
        assertFalse(immortal.playable(), "an authored playable:false shell must not stay playable");
        assertFalse(immortal.enterable());

        // asura_realm was optimistic: a hard seed marked it playable=true although the
        // authored catalog never lists it and its datapack json is a bare shell.
        DimensionRegistryService.DimensionDef asura =
                DimensionRegistryService.find(DimensionRegistryService.ASURA_REALM).orElseThrow();
        assertEquals(DimensionRegistryService.DimensionClass.PREVIEW_LOCKED, asura.dimensionClass());
        assertFalse(asura.enterable());

        // Ordinary travel must fail closed for every non-enterable class, not just deferred.
        String travel = Files.readString(JAVA_ROOT.resolve("worldpack/DimensionTravelService.java"));
        assertTrue(travel.contains("!def.get().enterable()"),
                "the travel gate must reject on enterable(), so preview shells are covered too");
        assertFalse(travel.contains("def.get().isDeferred()"),
                "the narrower deferred-only gate must be gone");
    }

    @Test
    void classificationComesFromTheSingleReconcileResource() throws Exception {
        String reconcile = Files.readString(DATA.resolve("catalog/dimensions_reconcile.json"));
        assertTrue(reconcile.contains("\"classification\""),
                "the four classes must be data-driven, not hard-coded per call site");
        for (String key : List.of("preview_locked", "abstract_template", "logical_cluster")) {
            assertTrue(reconcile.contains("\"" + key + "\""), "missing class bucket " + key);
        }

        String registry = Files.readString(JAVA_ROOT.resolve("worldpack/DimensionRegistryService.java"));
        assertTrue(registry.contains("catalog/dimensions_reconcile.json"),
                "the registry must actually read the reconcile resource it is reconciled against");
        // Every classified id must exist in the registry, or the data silently does nothing.
        for (String id : List.of(
                "seeking_immortals:immortal_realm",
                "seeking_immortals:asura_realm",
                "seeking_immortals:secret_realm_instance",
                "seeking_immortals:yin_underworld")) {
            assertTrue(DimensionRegistryService.find(id).isPresent(), "unknown classified id " + id);
        }
    }

    @Test
    void everyPlayableDimensionHasADatapackDimensionOrAnOverworldMapping() throws Exception {
        Set<String> datapack = Files.list(DATA.resolve("dimension"))
                .filter(path -> path.toString().endsWith(".json"))
                .map(path -> path.getFileName().toString().replace(".json", ""))
                .collect(Collectors.toSet());

        for (DimensionRegistryService.DimensionDef def : DimensionRegistryService.snapshot().playable()) {
            String minecraftId = def.effectiveMinecraftId();
            if (DimensionRegistryService.OVERWORLD.equals(minecraftId)) {
                continue;
            }
            String path = minecraftId.contains(":")
                    ? minecraftId.substring(minecraftId.indexOf(':') + 1)
                    : minecraftId;
            assertTrue(datapack.contains(path),
                    "playable dimension without a datapack dimension json: " + def.id());
        }
    }

    @Test
    void classifiedShellsKeepTheirDatapackJsonAndFlightRules() throws Exception {
        // Reclassifying is a status change, not a deletion: the shells stay shipped so an
        // existing save that already visited them still loads.
        assertTrue(Files.exists(DATA.resolve("dimension/immortal_realm.json")));
        assertTrue(Files.exists(DATA.resolve("dimension/asura_realm.json")));

        // Flight classification is independent of enterability and must not regress.
        assertEquals(FlyingAuthorityPolicy.DimensionFlightRule.SPIRIT_REALM,
                FlyingAuthorityPolicy.classifyDimension(DimensionRegistryService.IMMORTAL_REALM));
        assertEquals(FlyingAuthorityPolicy.DimensionFlightRule.SECRET_REALM,
                FlyingAuthorityPolicy.classifyDimension(DimensionRegistryService.ASURA_REALM));
    }

    @Test
    void catalogCommandReportsFourClassesInsteadOfABooleanGuess() throws Exception {
        String command = Files.readString(JAVA_ROOT.resolve("command/SeekingImmortalsCommand.java"));
        // The old line printed "可进入" for anything whose status string was blank.
        assertFalse(command.contains("def.isDeferred() ? \"待实现\" : \"可进入\""),
                "the boolean status guess must be replaced by the real class");
        assertTrue(command.contains("dimensionClassLabel("),
                "the command must render the classified state");
        assertTrue(command.contains("PREVIEW_LOCKED")
                        && command.contains("ABSTRACT_TEMPLATE")
                        && command.contains("LOGICAL_CLUSTER"),
                "all four classes need a player-facing label");
    }
}
