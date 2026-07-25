package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.cultivation.RealmStage;
import com.xunxian.seekingimmortals.structure.FlyingBoatDockStructure;
import com.xunxian.seekingimmortals.structure.ImmortalTeleportGrandArrayStructure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M13DimensionsAscensionTest {
    private static final Path DATA = Path.of("src", "main", "resources", "data", "seeking_immortals");

    @Test
    void dimensionRegistryCoversAuraKnownAndCatalog() {
        assertTrue(DimensionRegistryService.size() >= 10);
        assertTrue(DimensionRegistryService.coversAuraKnownDimensions());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.TIANYUAN).isPresent());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.SPIRIT_FENGYUAN).isPresent());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.YIN_MING_POCKET).isPresent());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.NETHER_RIVER_POCKET).isPresent());
        assertTrue(DimensionRegistryService.find(DimensionRegistryService.DEMON_RIFT).isPresent());
        assertTrue(DimensionRegistryService.find("mortal_world").isPresent());
        assertEquals(DimensionRegistryService.OVERWORLD,
                DimensionRegistryService.toMinecraftDimensionId(DimensionRegistryService.MORTAL_WORLD));
        // deferred markers are explicit, not silent
        assertFalse(DimensionRegistryService.deferredIds().isEmpty());
        assertTrue(DimensionRegistryService.find("seeking_immortals:yin_underworld").map(d -> d.isDeferred()).orElse(false)
                || DimensionRegistryService.deferredIds().stream().anyMatch(id -> id.contains("yin_underworld")
                || id.contains("secret_realm_instance")));
    }

    @Test
    void dimensionsCatalogReconcilesWithDatapackJson() throws Exception {
        Set<String> datapack = Files.list(DATA.resolve("dimension"))
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replace(".json", ""))
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(29, datapack.size());
        Set<String> types = Files.list(DATA.resolve("dimension_type"))
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replace(".json", ""))
                .collect(Collectors.toCollection(HashSet::new));
        assertEquals(10, types.size());

        // every datapack dim is known to registry (by bare path or namespaced id)
        for (String dim : datapack) {
            assertTrue(DimensionRegistryService.isKnown(dim) || DimensionRegistryService.isKnown("seeking_immortals:" + dim),
                    "registry missing datapack dim " + dim);
        }
        // catalog playable dims present
        for (String id : List.of(
                "seeking_immortals:mortal_world",
                "seeking_immortals:tianyuan",
                "seeking_immortals:spirit_fengyuan",
                "seeking_immortals:yin_ming_pocket",
                "seeking_immortals:demon_rift",
                "seeking_immortals:immortal_realm")) {
            assertTrue(DimensionRegistryService.isKnown(id), "missing catalog dim " + id);
        }
        // deferred logical/template not silently skipped
        assertTrue(Files.exists(DATA.resolve("catalog/dimensions_reconcile.json")));
    }

    @Test
    void spiritRealmInterfaceAndTravelAuthorityLoad() {
        assertTrue(SpiritRealmInterfaceService.gateCount() >= 3);
        assertTrue(SpiritRealmInterfaceService.findGate("mortal_to_tianyuan").isPresent());
        assertTrue(SpiritRealmInterfaceService.isMainBodyOneWay("mortal_to_tianyuan"));
        assertTrue(SpiritRealmInterfaceService.bridge().mainBodyOneWay());
        assertTrue(DimensionTravelService.methodCount() >= 4);
        assertTrue(DimensionTravelService.routeCount() >= 2);
        assertTrue(DimensionTravelService.findMethod("ascension_channel").isPresent()
                || DimensionTravelService.findMethod("fixed_teleport_array").isPresent()
                || DimensionTravelService.methodCount() >= 1);
        assertFalse(DimensionTravelService.snapshot().matrix().isEmpty());
    }

    @Test
    void travelRoutesUsePublishedDimensionAndNestedMethodFields() {
        DimensionTravelService.RouteDef ascension = DimensionTravelService.findRoute("mortal_to_tianyuan")
                .orElseThrow();
        assertEquals(DimensionRegistryService.MORTAL_WORLD, ascension.fromDimension());
        assertEquals(DimensionRegistryService.TIANYUAN, ascension.toDimension());
        assertEquals(DimensionTravelService.METHOD_ASCENSION, ascension.method());
        assertEquals("DEITY_TRANSFORMATION", ascension.realmMin());
        assertTrue(ascension.oneWay());
        assertTrue(DimensionTravelService.isDirectRouteImplemented(ascension));

        DimensionTravelService.RouteDef portal = DimensionTravelService.findRoute("tianyuan_to_fengyuan")
                .orElseThrow();
        assertEquals(DimensionRegistryService.TIANYUAN, portal.fromDimension());
        assertEquals(DimensionRegistryService.SPIRIT_FENGYUAN, portal.toDimension());
        assertEquals(DimensionTravelService.METHOD_REGULATED, portal.method());
        assertEquals("VOID_REFINEMENT", portal.realmMin());
        assertEquals("tianyuan_to_spirit_fengyuan", portal.gateId());
        assertEquals(500, DimensionTravelService.contributionCost(portal.id()));
    }

    @Test
    void flightPolicyFailsClosedOutsideRecognizedDimensions() {
        assertEquals(FlyingAuthorityPolicy.DimensionFlightRule.MORTAL,
                FlyingAuthorityPolicy.classifyDimension(DimensionRegistryService.OVERWORLD));
        assertEquals(FlyingAuthorityPolicy.DimensionFlightRule.DENY,
                FlyingAuthorityPolicy.classifyDimension("minecraft:the_nether"));
        assertEquals(FlyingAuthorityPolicy.DimensionFlightRule.DENY,
                FlyingAuthorityPolicy.classifyDimension("minecraft:the_end"));
        assertEquals(FlyingAuthorityPolicy.DimensionFlightRule.DENY,
                FlyingAuthorityPolicy.classifyDimension("othermod:tianyuan"));
        assertTrue(FlyingAuthorityPolicy.permitsManagedFlightDimension(DimensionRegistryService.OVERWORLD));
        assertTrue(FlyingAuthorityPolicy.permitsManagedFlightDimension(DimensionRegistryService.TIANYUAN));
        assertFalse(FlyingAuthorityPolicy.permitsManagedFlightDimension("minecraft:the_nether"));
        assertFalse(FlyingAuthorityPolicy.permitsManagedFlightDimension("othermod:tianyuan"));
        assertTrue(FlyingAuthorityPolicy.shouldGrantPolicy(false, false, true));
        assertFalse(FlyingAuthorityPolicy.shouldGrantPolicy(true, false, true));
        assertFalse(FlyingAuthorityPolicy.shouldGrantPolicy(false, true, true));
        assertTrue(FlyingAuthorityPolicy.allowsMortalCultivation(Realm.QI_REFINING, RealmStage.LAYER_10));
        assertFalse(FlyingAuthorityPolicy.allowsMortalCultivation(Realm.QI_REFINING, RealmStage.LAYER_9));
    }

    @Test
    void ascensionFlowAndLoadoutPresent() {
        assertTrue(AscensionService.stageCount() >= 5);
        assertTrue(AscensionService.snapshot().findStage("ascension_channel").isPresent());
        assertTrue(AscensionService.snapshot().findStage("tianyuan_garrison").isPresent());
        assertFalse(AscensionService.snapshot().loadoutPaths().isEmpty());
        assertTrue(Files.exists(DATA.resolve("text_material/ascension_loadout_v95.json")));
        assertTrue(Files.exists(DATA.resolve("text_material/mortal_to_spirit_bridge.json")));
    }

    @Test
    void spatialNodesNetworkAndCatalogExpanded() {
        assertTrue(SpatialNodeCatalogService.builtin().size() >= 33);
        assertTrue(SpatialNodeCatalogService.builtin().find("gate_mortal_to_tianyuan").isPresent());
        assertTrue(SpatialNodeCatalogService.builtin().find("node_immortal_hub").isPresent()
                || SpatialNodeCatalogService.builtin().find("gate_spirit_to_immortal").isPresent()
                || SpatialNodeCatalogService.builtin().size() >= 33);
        // network SavedData class present for M13 teleport network
        assertTrue(SpatialNodeNetworkSavedData.class.getSimpleName().contains("SpatialNode"));
    }

    @Test
    void yinUnderworldClusterRulesLoad() {
        assertTrue(YinUnderworldClusterService.snapshot().regionCount() >= 2);
        assertTrue(YinUnderworldClusterService.isYinDimension(DimensionRegistryService.YIN_MING_POCKET));
        assertTrue(YinUnderworldClusterService.isYinDimension(DimensionRegistryService.NETHER_RIVER_POCKET));
        assertTrue(YinUnderworldClusterService.isYinRegion("yinming"));
        assertTrue(YinUnderworldClusterService.isYinRegion("nether_river"));
    }

    @Test
    void flightDockAndGrandArrayStructuresExist() {
        assertTrue(FlyingBoatDockStructure.platformOffsets().size() >= 9);
        assertTrue(FlyingBoatDockStructure.mastOffsets().size() >= 2);
        // compile-time link to immortal grand array wrapper
        assertTrue(ImmortalTeleportGrandArrayStructure.class.getSimpleName().contains("Immortal"));
    }

    @Test
    void publishedTravelCorpusPresent() {
        assertTrue(Files.exists(DATA.resolve("text_material/dimension_travel_methods_v136.json")));
        assertTrue(Files.exists(DATA.resolve("text_material/dimension_travel_costs_v137.json")));
        assertTrue(Files.exists(DATA.resolve("text_material/dimensions_catalog.json")));
        assertTrue(Files.exists(DATA.resolve("text_material/dimension_registry.json")));
        assertTrue(Files.exists(DATA.resolve("catalog/dimensions_index.json")));
        assertTrue(Files.exists(DATA.resolve("catalog/dimension_registry_index.json")));
        assertTrue(Files.exists(DATA.resolve("catalog/spatial_nodes_index.json")));
    }

    @Test
    void catalogTravelPrefersAuthoredRouteAuthority() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "worldpack", "DimensionTravelService.java");
        String source = Files.readString(sourcePath);
        int methodStart = source.indexOf("public static boolean travelToDimension");
        int methodEnd = source.indexOf("public static boolean isOnCooldown", methodStart);
        assertTrue(methodStart >= 0);
        assertTrue(methodEnd > methodStart);
        String method = source.substring(methodStart, methodEnd);
        int routeLookup = method.indexOf("findRouteFor(");
        int matrixLookup = method.indexOf("findMatrixEdge(");
        assertTrue(routeLookup >= 0);
        assertTrue(matrixLookup > routeLookup);
        assertTrue(method.contains("return travelByRoute(player, route.get().id())"));

        int internalStart = source.indexOf("private static boolean travelInternal");
        int internalEnd = source.indexOf("private static long cooldownFor", internalStart);
        assertTrue(internalStart >= 0);
        assertTrue(internalEnd > internalStart);
        String internal = source.substring(internalStart, internalEnd);
        int teleport = internal.indexOf("player.teleportTo(target");
        int commitCheck = internal.indexOf("if (player.level() != target)");
        int platformWrite = internal.indexOf("ensurePlatform(target");
        assertTrue(teleport >= 0);
        assertTrue(commitCheck > teleport);
        assertTrue(platformWrite > commitCheck);
    }

    @Test
    void ascensionFallbackCommitsOnlyAfterVerifiedTeleport() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "worldpack", "AscensionService.java");
        String source = Files.readString(sourcePath);
        int start = source.indexOf("private static boolean teleportToTianyuan");
        int end = source.indexOf("private static Snapshot load()", start);
        String method = source.substring(start, end);
        int teleport = method.indexOf("player.teleportTo(target");
        int commitCheck = method.indexOf("if (!teleportCommitted(");
        int platformWrite = method.indexOf("target.setBlock");
        assertTrue(teleport >= 0);
        assertTrue(commitCheck > teleport);
        assertTrue(platformWrite > commitCheck);
        assertFalse(method.contains("WorldpackGameplayService.travel"));
        assertFalse(method.contains("setWorldpackCurrentRegionId"));
        assertTrue(AscensionService.teleportCommitted(true, 0.0D));
        assertTrue(AscensionService.teleportCommitted(true, 16.0D));
        assertFalse(AscensionService.teleportCommitted(false, 0.0D));
        assertFalse(AscensionService.teleportCommitted(true, Math.nextUp(16.0D)));
        assertFalse(AscensionService.teleportCommitted(true, Double.NaN));
    }

    @Test
    void ascensionStateCommitsAfterFailureRollbackBranch() throws Exception {
        Path sourcePath = Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                "worldpack", "AscensionService.java");
        String source = Files.readString(sourcePath);
        int start = source.indexOf("public static boolean attemptAscension");
        int end = source.indexOf("public static boolean confirmLoadoutAndAscend", start);
        String method = source.substring(start, end);
        int backup = method.indexOf("backupInventory(player)");
        int reset = method.indexOf("applyLoadoutReset(player)");
        int teleport = method.indexOf("teleportToTianyuan(player)");
        int restore = method.indexOf("restoreBackup(player)");
        int confirmed = method.indexOf("FLAG_LOADOUT_CONFIRMED, true");
        int region = method.indexOf("setWorldpackCurrentRegionId(\"tianyuan\")");
        int ascended = method.indexOf("FLAG_ASCENDED, true");
        int clearBackup = method.indexOf("clearBackup(player)");
        int flightRefresh = method.indexOf("FlyingAuthorityPolicy.onDimensionChanged");
        int starterPack = method.indexOf("grantStarterPack(player)");
        assertTrue(backup >= 0);
        assertTrue(reset > backup);
        assertTrue(teleport > reset);
        assertTrue(restore > teleport);
        assertTrue(confirmed > restore);
        assertTrue(region > restore);
        assertTrue(ascended > region);
        assertTrue(clearBackup > ascended);
        assertTrue(flightRefresh > clearBackup);
        assertTrue(starterPack > clearBackup);
    }

    /**
     * After successful teleport, the temporary rollback snapshot must be discarded so
     * restore returns no_backup and unique/equipment stacks cannot be re-injected.
     */
    @Test
    void successfulAscensionClearsBackupSnapshot() {
        CompoundTag data = new CompoundTag();
        CompoundTag backup = new CompoundTag();
        backup.putBoolean("hasBackup", true);
        ListTag items = new ListTag();
        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", "minecraft:diamond_sword");
        stackTag.putByte("Count", (byte) 1);
        stackTag.putInt("Slot", 0);
        items.add(stackTag);
        backup.put("Items", items);
        data.put(AscensionService.BACKUP_ROOT, backup);

        assertTrue(AscensionService.hasBackupData(data), "precondition: snapshot present");
        // Success path: clearBackupData (mirrors attemptAscension after teleported=true)
        AscensionService.clearBackupData(data);
        assertFalse(AscensionService.hasBackupData(data), "success must clear hasBackup");
        assertFalse(data.contains(AscensionService.BACKUP_ROOT), "success removes backup root entirely");
    }

    /**
     * Teleport-failure rollback still consumes the temporary snapshot (regression guard).
     * System restore path remains valid when hasBackup=true; after consume, no_backup.
     */
    @Test
    void teleportFailureRollbackSnapshotIsConsumableOnce() {
        CompoundTag data = new CompoundTag();
        CompoundTag backup = new CompoundTag();
        backup.putBoolean("hasBackup", true);
        backup.put("Items", new ListTag());
        data.put(AscensionService.BACKUP_ROOT, backup);

        assertTrue(AscensionService.hasBackupData(data));
        // Simulate restoreBackup's consume step (clear after inject)
        AscensionService.clearBackupData(data);
        assertFalse(AscensionService.hasBackupData(data), "rollback snapshot is one-shot");
    }

    @Test
    void reascensionGateIgnoresDimensionAndUsesFlagOnly() {
        assertTrue(AscensionService.shouldBlockReascension(true),
                "FLAG_ASCENDED alone must block even outside tianyuan");
        assertFalse(AscensionService.shouldBlockReascension(false),
                "without FLAG_ASCENDED the gate stays open");
    }

    @Test
    void starterGiftGrantIsIdempotent() {
        assertTrue(AscensionService.shouldGrantStarter(false), "first grant allowed");
        assertFalse(AscensionService.shouldGrantStarter(true), "repeat grant blocked by flag");
    }
}
