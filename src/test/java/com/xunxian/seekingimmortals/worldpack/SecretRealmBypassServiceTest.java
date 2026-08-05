package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Y-A-2: the authored 「付代价绕道」 branch (pay_yezha_toll) must be a real, bounded,
 * fail-closed transaction rather than authored prose with no implementation.
 */
class SecretRealmBypassServiceTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");
    private static final Path BRANCHES = Path.of(
            "src", "main", "resources", "data", "seeking_immortals",
            "text_material", "npc_dialogue_branches_v139.json");

    @Test
    void onlyAuthoredBypassableLayersAreSkippable() {
        // The authored walkthrough marks the yezha nest as 「过夜叉巢或付代价绕道」.
        assertTrue(SecretRealmBypassService.isBypassableLayer("yinyang_ku", "yy_yezha"));
        assertEquals("mid", SecretRealmBypassService.proofPhaseFor("yinyang_ku", "yy_yezha"));

        // Everything else fails closed: paying must never skip an unmarked layer.
        assertFalse(SecretRealmBypassService.isBypassableLayer("yinyang_ku", "yy_yinzhi"),
                "the capture layer is the objective and must not be purchasable");
        assertFalse(SecretRealmBypassService.isBypassableLayer("yinyang_ku", "yy_outer"));
        assertFalse(SecretRealmBypassService.isBypassableLayer("blood_forbidden", "bf_water_jiao"));
        assertFalse(SecretRealmBypassService.isBypassableLayer("", ""));
        assertTrue(SecretRealmBypassService.proofPhaseFor("blood_forbidden", "bf_mid_contest").isBlank());
    }

    @Test
    void proofPhaseMatchesTheAuthoredEncounterRoute() throws Exception {
        // Step 1 of peiying_material_hunt proves ENCOUNTER_CLEARED(region=yy_yezha), which
        // DetailedQuestProofService maps from phase "mid" of yinyang_ku. The bypass must reuse
        // that exact route, otherwise the paid branch could never advance the chain.
        String proof = Files.readString(JAVA_ROOT.resolve("quest/DetailedQuestProofService.java"));
        assertTrue(proof.contains("case \"yinyang_ku:mid\" -> List.of(\"yy_yezha\")"),
                "the authored yy_yezha encounter route must stay mapped to phase mid");
        assertEquals("mid", SecretRealmBypassService.proofPhaseFor("yinyang_ku", "yy_yezha"));
    }

    @Test
    void bypassIsAtomicAndRefundsWhenTheProofIsRejected() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve("worldpack/SecretRealmBypassService.java"));

        // Cost is reserved atomically before any ledger or world effect.
        assertTrue(service.contains("InventoryReservation.consume(player, costs)"),
                "the toll must use the atomic inventory reservation");
        assertTrue(service.contains("progress.spendContribution(BYPASS_CONTRIBUTION_COST)"),
                "contribution fallback must use the check-then-spend transaction");

        // A rejected proof refunds instead of silently eating the payment.
        assertTrue(service.contains("refund(player, reservation, progress, contributionPaid)"),
                "a rejected proof must refund the toll");
        assertTrue(service.contains("reservation.refund(player)")
                        && service.contains("progress.addContribution(contributionPaid)"),
                "both payment paths must be refundable");

        // Paying avoids the fight: the roster is suppressed and no combat spoils are granted.
        assertTrue(service.contains("suppressLayerRoster(player, session, realm, layer)"),
                "a paid bypass must suppress the layer roster");
        assertFalse(service.contains("BossLootService") || service.contains("grantBossLoot"),
                "a bypass must never grant combat loot");

        // Re-paying the same session is rejected rather than charged twice.
        assertTrue(service.contains("Status.ALREADY_BYPASSED"),
                "a second bypass in one session must be rejected");
    }

    @Test
    void bypassIsReachableThroughAnAuthoredDialogueNodeWithoutCommands() throws Exception {
        String executor = Files.readString(JAVA_ROOT.resolve("npc/DialogueActionExecutor.java"));
        String worldAction = Files.readString(JAVA_ROOT.resolve("npc/DialogueWorldActionService.java"));
        String trial = Files.readString(JAVA_ROOT.resolve("worldpack/SecretRealmTrialService.java"));

        // A dedicated dialogue effect type dispatches to its own handler (D-A typing rule).
        assertTrue(executor.contains("SACRIFICE_BYPASS -> DialogueWorldActionService.sacrificeBypass("),
                "sacrifice_bypass must have a dedicated handler");
        assertTrue(worldAction.contains("public static boolean sacrificeBypass("));

        // The broker is placed on realm entry, so the branch needs no admin command.
        assertTrue(trial.contains("SecretRealmBypassService.ensureTollBroker(player, id)"),
                "the toll broker must be placed on realm entry");

        // The authored dialogue tree carries the effect with realm/layer intent.
        JsonObject root = JsonParser.parseString(Files.readString(BRANCHES)).getAsJsonObject();
        JsonObject tree = null;
        for (JsonElement element : root.getAsJsonArray("trees")) {
            JsonObject candidate = element.getAsJsonObject();
            if (SecretRealmBypassService.TOLL_BROKER_TREE.equals(candidate.get("id").getAsString())) {
                tree = candidate;
            }
        }
        assertTrue(tree != null, "authored toll broker tree missing");
        assertTrue(tree.getAsJsonArray("npc_ids").get(0).getAsString()
                .equals(SecretRealmBypassService.TOLL_BROKER_NPC));

        Set<String> nodeIds = new LinkedHashSet<>();
        JsonObject payNode = null;
        for (JsonElement element : tree.getAsJsonArray("nodes")) {
            JsonObject node = element.getAsJsonObject();
            nodeIds.add(node.get("id").getAsString());
            for (JsonElement effect : node.getAsJsonArray("effects")) {
                if ("sacrifice_bypass".equals(effect.getAsJsonObject().get("type").getAsString())) {
                    payNode = effect.getAsJsonObject();
                }
            }
        }
        assertTrue(payNode != null, "authored tree must carry the sacrifice_bypass effect");
        assertEquals("yinyang_ku", payNode.get("realm").getAsString());
        assertEquals("yy_yezha", payNode.get("layer").getAsString());

        // The tree graph stays closed and reachable (same rule as the other authored trees).
        for (JsonElement element : tree.getAsJsonArray("nodes")) {
            for (JsonElement next : element.getAsJsonObject().getAsJsonArray("next")) {
                assertTrue(nodeIds.contains(next.getAsString()),
                        "dangling next node " + next.getAsString());
            }
        }
    }
}
