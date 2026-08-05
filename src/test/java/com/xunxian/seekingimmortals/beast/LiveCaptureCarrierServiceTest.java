package com.xunxian.seekingimmortals.beast;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y-B: 阴芝马 live capture/transport. Authored rules under test —
 * 「活的才是丹」(live carrier is a distinct instance), 「击杀降材料品质」(kill pays inferior
 * material only), 「运出途中死，只剩劣材」(death/timeout degrades rather than deletes).
 */
class LiveCaptureCarrierServiceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");

    @Test
    void capturedAliveProofRouteResolvesToTheRealSpawnedBeastId() throws Exception {
        // The authored route requires entity token "yin_zhi_horse", but the yy_yinzhi layer
        // spawns bestiary id "yinyang_yinzhima". Without the mapping the step is unreachable.
        String proof = Files.readString(JAVA_ROOT.resolve("quest/DetailedQuestProofService.java"));
        assertTrue(proof.contains("Map.entry(\"yin_zhi_horse\", Set.of(\"yinyang_yinzhima\"))"),
                "the authored 阴芝马 token must be provable by the real spawned id");
        // The mapping is consulted through the shared token resolver (identity always included).
        assertTrue(proof.contains("PROOF_ENTITY_MAPPINGS.getOrDefault(token, Set.of())")
                        && proof.contains("result.add(token)"),
                "entity token resolution must stay mapping + identity");

        // The authored route really does ask for that token at step 2.
        String routes = Files.readString(Path.of("src", "main", "resources", "data",
                "seeking_immortals", "text_material", "detailed_quest_proof_routes.json"));
        assertTrue(routes.contains("\"entity\":\"yin_zhi_horse\"")
                        || routes.contains("\"entity\": \"yin_zhi_horse\""),
                "peiying_material_hunt step 2 must still require the 阴芝马 token");
    }

    @Test
    void authoredCaptureOnlyRightIsParsedWithoutGrantingPetRights() {
        BeastBestiaryService.BeastEntry horse =
                BeastBestiaryService.find("yinyang_yinzhima").orElseThrow();
        assertTrue(horse.captureOnly());
        assertTrue(horse.capturable());
        assertFalse(horse.tameable(), "capture_only must not become a contractible companion");

        // The guard beast on the same layer is an obstacle, not a capture objective.
        assertFalse(BeastBestiaryService.isCaptureOnlyBeast("yinyang_guard_beast"));
    }

    @Test
    void carrierStateMachineIsOneWayFromLiveToDegraded() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve("beast/LiveCaptureCarrierService.java"));

        // A carrier is one captured instance: single stack + per-capture UUID + source session.
        assertTrue(service.contains("UUID.randomUUID().toString()"),
                "each capture must mint a fresh instance id so it cannot settle twice");
        assertTrue(service.contains("TAG_SESSION") && service.contains("sourceSession("),
                "carriers must record the source session");
        assertEquals(20 * 60 * 20, LiveCaptureCarrierService.LIVE_TIMEOUT_TICKS);

        // Degradation is one-way and never deletes the carrier.
        assertTrue(service.contains("if (!isCarrier(stack) || !isLive(stack))"),
                "degrade must be idempotent on an already-dead carrier");
        // Y-C: the death branch sweeps every compartment, not just the main inventory.
        assertTrue(service.contains("carriedCompartments(player)"),
                "the death branch must use the shared compartment sweep");
        assertFalse(service.contains("stack.shrink(") || service.contains("setCount(0)"),
                "a timed-out carrier must degrade, not vanish");
    }

    @Test
    void killPaysInferiorMaterialAndNeverTheLiveCarrier() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve("beast/LiveCaptureCarrierService.java"));
        String capture = Files.readString(JAVA_ROOT.resolve("artifact/ArtifactCaptureService.java"));

        // Kill path grants material only; it must not mint a carrier or record a capture proof.
        String killPath = service.substring(service.indexOf("public static void grantKillMaterial("));
        assertFalse(killPath.contains("createLive("),
                "killing must never produce a live carrier");
        assertFalse(killPath.contains("recordEntityCaptured("),
                "killing must never record the captured-alive proof");

        // Capture routes authored capture-only beasts into the live carrier instead of the jar.
        assertTrue(capture.contains("captureIntoLiveCarrier(player, best, id, tier)"),
                "capture-only beasts must use the live carrier path");
        assertTrue(capture.contains("BeastBestiaryService.isCaptureOnlyBeast(id)"));
    }

    @Test
    void carrierDeliveryAndFailureBranchesAreWiredToOutboxAndTicks() throws Exception {
        String capture = Files.readString(JAVA_ROOT.resolve("artifact/ArtifactCaptureService.java"));
        String events = Files.readString(JAVA_ROOT.resolve("event/ModEvents.java"));

        // A full/dropped inventory must not destroy the capture.
        assertTrue(capture.contains("InventoryDeliveryService.giveOrEnqueue(")
                        && capture.contains("\"live_capture_carrier\""),
                "carrier delivery must go through the recoverable outbox");

        // Transit timeout ticks, and dying degrades what is carried.
        // Y-C moved the per-stack loop into tickCarriedTransit so every compartment is swept.
        assertTrue(events.contains("tickCarriedTransit(serverPlayer"),
                "transit timeout must be ticked server-side across all compartments");
        assertTrue(events.contains("LiveCaptureCarrierService.degradeAllCarried(dead)"),
                "player death must degrade live carriers");
        assertTrue(events.contains("LiveCaptureCarrierService.onCaptureOnlyKilled(killer, mob)"),
                "killing a capture-only beast must pay inferior material");
    }
}
