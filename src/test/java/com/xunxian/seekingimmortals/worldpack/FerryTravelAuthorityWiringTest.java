package com.xunxian.seekingimmortals.worldpack;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FerryTravelAuthorityWiringTest {
    @Test
    void regionTravelChecksClosureBeforeInventoryReservation() throws IOException {
        String source = source("worldpack", "WorldpackGameplayService.java");
        String method = method(source,
                "private static TravelCostReservation reserveTravelCosts",
                "private static boolean planNonPortalTravelAccess");

        assertBefore(method, "FerryTravelPolicy.denyIfDelayed(player)",
                "InventoryReservation.consume(player, costs)",
                "region ferry closure must run before tickets or Yin stones are reserved");

        String pocketGate = method(source,
                "public static boolean useNetherFerryGate(ServerPlayer player, boolean enforceRequires)",
                "public static boolean useAncientRiftGate(ServerPlayer player)");
        assertBefore(pocketGate, "FerryTravelPolicy.denyIfDelayed(player)",
                "reserveSpatialNode(player, \"pocket_gate\", enforceRequires)",
                "the Nether ferry multiblock must check closure before reserving node requirements");
    }

    @Test
    void dimensionRouteChecksClosureBeforeContributionSpend() throws IOException {
        String method = method(source("worldpack", "DimensionTravelService.java"),
                "public static boolean travelByRoute",
                "public static boolean travelToDimension");

        assertBefore(method, "FerryTravelPolicy.denyIfDelayed(player)",
                "progress.spendContribution(contributionCost)",
                "dimension ferry closure must run before contribution is spent");
    }

    @Test
    void spatialNodeChecksClosureBeforeRequirementReservation() throws IOException {
        String method = method(source("worldpack", "SpatialNodeCatalogService.java"),
                "public static boolean travel(ServerPlayer player, String id)",
                "private static boolean executeTravel");

        assertBefore(method, "FerryTravelPolicy.denyIfDelayed(player)",
                "SpatialNodeRequiresService.reserve(player, node)",
                "spatial ferry closure must run before node requirements are reserved");
    }

    @Test
    void ferryVehicleChecksClosureBeforeSpawnAndFuelConsumption() throws IOException {
        String method = method(source("catalog", "FlightVehicleService.java"),
                "public static boolean board(ServerPlayer player, String vehicleId)",
                "public static boolean isBoardingDelayed");

        assertBefore(method, "FerryTravelPolicy.denyIfDelayed(player)",
                "level.addFreshEntity(boat)",
                "ferry closure must run before a boat entity is spawned");
        assertBefore(method, "FerryTravelPolicy.denyIfDelayed(player)",
                "consume(player, fuel, fuelCount)",
                "ferry closure must run before fuel is consumed");
    }

    @Test
    void artifactVehicleClosureCannotFallBackToFreeMobility() throws IOException {
        String method = method(source("artifact", "ArtifactActivationService.java"),
                "private static void applyVehicle",
                "private static void applyBeastControl");

        assertBefore(method, "FlightVehicleService.isBoardingDelayed(player, vehicleId)",
                "FlightVehicleService.board(player, vehicleId)",
                "an artifact must classify a delayed ferry before its first boarding attempt");
        assertBefore(method, "FlightVehicleService.isBoardingDelayed(player, vehicleId)",
                "message.seeking_immortals.artifact.binds_vehicle_fallback",
                "a closed ferry artifact must be handled before the mobility fallback");
        assertTrue(method.contains("FlightVehicleService.board(player, vehicleId);\n                return;"),
                "the delayed path must delegate to board for the authoritative denial message and stop");
    }

    private static String source(String packageName, String fileName) throws IOException {
        return Files.readString(Path.of("src", "main", "java", "com", "xunxian", "seekingimmortals",
                packageName, fileName));
    }

    private static String method(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue(start >= 0, "missing method start token: " + startToken);
        assertTrue(end > start, "missing method end token: " + endToken);
        return source.substring(start, end);
    }

    private static void assertBefore(String source, String gate, String mutation, String message) {
        int gateIndex = source.indexOf(gate);
        int mutationIndex = source.indexOf(mutation);
        assertTrue(gateIndex >= 0, "missing gate token: " + gate);
        assertTrue(mutationIndex >= 0, "missing mutation token: " + mutation);
        assertTrue(gateIndex < mutationIndex, message);
    }
}
