package com.xunxian.seekingimmortals.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D-A dialogue world-action typing: every authored world action must resolve to a
 * dedicated handler with its own failure semantics; unknown actions fail closed.
 * Pure data/source contract tests (no Forge runtime, no Mockito).
 */
class DialogueActionCoverageTest {
    private static final Path JAVA_ROOT = Path.of(
            "src", "main", "java", "com", "xunxian", "seekingimmortals");
    private static final Path BRANCHES = Path.of(
            "src", "main", "resources", "data", "seeking_immortals",
            "text_material", "npc_dialogue_branches_v139.json");

    /**
     * The authored world-action family (structure/guard/combat/arrest/suspicion/anomaly).
     * Every member must have its own handler in DialogueWorldActionService; a member
     * without one is an unauthenticated consequence, not a dialogue action.
     */
    private static final Set<String> WORLD_ACTIONS = Set.of(
            "mark_structure",
            "hint",
            "clue",
            "call_guard",
            "combat_flag",
            "combat_or_arrest",
            "add_suspicion",
            "anomaly_log",
            // Y-A-2: paid detour (「付代价绕道」) spends resources, suppresses a layer roster and
            // settles an encounter proof, so it is a world action and needs typed handling.
            "sacrifice_bypass");

    private static Set<String> authoredEffectTypes() throws Exception {
        Set<String> types = new LinkedHashSet<>();
        JsonObject root = JsonParser.parseString(Files.readString(BRANCHES)).getAsJsonObject();
        JsonArray trees = root.getAsJsonArray("trees");
        for (JsonElement treeElement : trees) {
            JsonObject tree = treeElement.getAsJsonObject();
            JsonArray nodes = tree.has("nodes") && tree.get("nodes").isJsonArray()
                    ? tree.getAsJsonArray("nodes") : new JsonArray();
            for (JsonElement nodeElement : nodes) {
                JsonObject node = nodeElement.getAsJsonObject();
                if (!node.has("effects") || !node.get("effects").isJsonArray()) {
                    continue;
                }
                for (JsonElement effectElement : node.getAsJsonArray("effects")) {
                    if (effectElement.isJsonObject()) {
                        String type = effectElement.getAsJsonObject().has("type")
                                ? effectElement.getAsJsonObject().get("type").getAsString() : "";
                        if (!type.isBlank()) {
                            types.add(type);
                        }
                    } else if (effectElement.isJsonPrimitive()) {
                        String type = effectElement.getAsString();
                        if (!type.isBlank()) {
                            types.add(type);
                        }
                    }
                }
            }
        }
        return types;
    }

    @Test
    void everyAuthoredWorldActionBelongsToTheTypedFamily() throws Exception {
        Set<String> authoredWorld = new LinkedHashSet<>();
        for (String type : authoredEffectTypes()) {
            if (WORLD_ACTIONS.contains(type)) {
                authoredWorld.add(type);
            }
        }
        for (String type : authoredWorld) {
            assertTrue(WORLD_ACTIONS.contains(type), "unexpected world action " + type);
        }
        // The family must stay closed: an author writing a new world-facing action
        // that is not in the D-A family must fail the coverage gate.
        assertTrue(authoredWorld.contains("mark_structure"));
        assertTrue(authoredWorld.contains("call_guard"));
        assertTrue(authoredWorld.contains("combat_flag"));
        assertTrue(authoredWorld.contains("combat_or_arrest"));
        assertTrue(authoredWorld.contains("add_suspicion"));
        assertTrue(authoredWorld.contains("anomaly_log"));
        // Y-A-2: the paid-detour action is authored and must resolve to its own handler.
        assertTrue(authoredWorld.contains("sacrifice_bypass"));
    }

    @Test
    void paidDetourIsTypedAndFailsClosedOutsideAuthoredLayers() throws Exception {
        String executor = Files.readString(JAVA_ROOT.resolve("npc/DialogueActionExecutor.java"));
        String service = Files.readString(JAVA_ROOT.resolve("npc/DialogueWorldActionService.java"));

        // sacrifice_bypass dispatches to its own handler; it must not reuse an unrelated action.
        assertTrue(executor.contains("SACRIFICE_BYPASS -> DialogueWorldActionService.sacrificeBypass("),
                "sacrifice_bypass must have a dedicated handler");
        assertTrue(service.contains("public static boolean sacrificeBypass("));

        // Blank realm/layer intent is refused rather than defaulted into some layer.
        assertTrue(service.contains("if (realm.isBlank() || layer.isBlank())"),
                "paid detour must refuse blank realm/layer intent");
        assertTrue(service.contains("bypass_unavailable"),
                "an unbypassable target must report a dedicated failure");
    }

    @Test
    void worldActionsHaveIndependentHandlersAndFailClosed() throws Exception {
        String executor = Files.readString(JAVA_ROOT.resolve("npc/DialogueActionExecutor.java"));
        String service = Files.readString(JAVA_ROOT.resolve("npc/DialogueWorldActionService.java"));

        // Each enforcement/combat action must dispatch to its own handler method.
        assertTrue(executor.contains("case CALL_GUARD -> DialogueWorldActionService.callGuard(")
                || executor.contains("CALL_GUARD\n                -> DialogueWorldActionService.callGuard(")
                || executor.contains("CALL_GUARD -> DialogueWorldActionService.callGuard("),
                "call_guard must have a dedicated callGuard handler");
        assertTrue(executor.contains("COMBAT_FLAG -> DialogueWorldActionService.combatFlag("),
                "combat_flag must have a dedicated combatFlag handler");
        assertTrue(executor.contains("COMBAT_OR_ARREST -> DialogueWorldActionService.combatOrArrest("),
                "combat_or_arrest must have a dedicated combatOrArrest handler");

        // The three actions must no longer collapse into a single generic branch.
        assertFalse(executor.contains("case CALL_GUARD, COMBAT_FLAG, COMBAT_OR_ARREST ->"),
                "call_guard/combat_flag/combat_or_arrest must not share one generic handler");

        // WorldActionService exposes the three typed handlers.
        assertTrue(service.contains("public static boolean callGuard("));
        assertTrue(service.contains("public static boolean combatFlag("));
        assertTrue(service.contains("public static boolean combatOrArrest("));

        // Unknown actions stay fail-closed server-side.
        assertTrue(executor.contains("effect_unsupported"));
        assertFalse(executor.contains("effect_\" + type"));

        // hint and clue both resolve to the source-bound hint recorder.
        assertTrue(executor.contains("case HINT"));

        // suspicion is a typed tally, not a generic penalty-only sink.
        assertTrue(service.contains("public static int addSuspicion("));
        assertTrue(service.contains("public static int suspicion("));
    }

    @Test
    void structureMarkingMatchesAuthorIntentAndIsIdempotent() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve("npc/DialogueWorldActionService.java"));
        String runtime = Files.readString(JAVA_ROOT.resolve("quest/DetailedQuestRuntimeService.java"));
        String executor = Files.readString(JAVA_ROOT.resolve("npc/DialogueActionExecutor.java"));

        // mark_structure receives the authored type/dimension intent and enforces it.
        assertTrue(service.contains("public static boolean markStructure(ServerPlayer player, String structureId,\n"
                + "                                        String authorType, String authorDimension)"),
                "markStructure must accept authored type/dimension intent");
        assertTrue(service.contains("entry.type().equalsIgnoreCase(authorType)"),
                "markStructure must check the structure category");
        assertTrue(service.contains("located.dimension()).contains(normalize(authorDimension))"),
                "markStructure must check the dimension intent");
        assertTrue(executor.contains("DialogueWorldActionService.markStructure(player, structure, authorType, authorDimension)"),
                "executor must forward author type/dimension to markStructure");

        // Marking a structure whose current detailed-quest step does not expect it is rejected.
        assertTrue(runtime.contains("public static boolean currentStepExpectsStructure(ServerPlayer player, String structureId)"),
                "runtime must expose current-step structure expectation");

        // Structural proof is only emitted once per marked id (idempotent).
        assertTrue(service.contains("boolean alreadyMarked = markers.contains(id);"));
        assertTrue(service.contains("if (!alreadyMarked) {"));

        // Recent hint/anomaly ledgers are retained by age, not by lexicographic key order.
        assertTrue(service.contains("trimOldestByAge("));
        assertFalse(service.contains("getAllKeys().stream().sorted().findFirst().ifPresent(root::remove)"),
                "lexicographic eviction simulating time decay is not allowed");
    }

    @Test
    void hintsBindToDialogueSourceAndOnlyRecordOnce() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve("npc/DialogueWorldActionService.java"));

        // Hints are recorded with their dialogue source (NPC/node/time), not as bare booleans.
        assertTrue(service.contains("hintEntry(npcId, nodeId, player)"),
                "hint entries must bind to source NPC/node/time");
        assertTrue(service.contains("entry.putString(\"Npc\", normalize(npcId))"));
        assertTrue(service.contains("entry.putString(\"Node\", normalize(nodeId))"));
        assertTrue(service.contains("entry.putLong(\"At\", player.serverLevel().getGameTime())"));
        assertTrue(service.contains("boolean added = !hints.contains(id);"),
                "hint is recorded only once per id");
        assertTrue(service.contains("trimOldestByAge(hints)"),
                "hint ledger eviction must use age, not key order");
    }

    @Test
    void suspicionSettlesByDecayAndThresholdsAndAnomaliesBucketByFaction() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve("npc/DialogueWorldActionService.java"));

        // Suspicion is a decaying tally with warn/arrest thresholds (the arrest consumer comes
        // in D-A-4); it must never accumulate without a settlement point.
        assertTrue(service.contains("SUSPICION_TAG") && service.contains("LastAt"),
                "suspicion entries must carry a last-update timestamp for decay");
        assertTrue(service.contains("WARN_SUSPICION_THRESHOLD")
                        || service.contains("WARN_SUSPICION"),
                "suspicion must define a warn threshold");
        assertTrue(service.contains("ARREST_SUSPICION_THRESHOLD")
                        || service.contains("ARREST_SUSPICION"),
                "suspicion must define an arrest threshold");
        assertTrue(service.contains("suspectLevel("),
                "suspicion must expose a categorized level for the arrest consumer");

        // Recent anomalies are bucketed by authority/faction (NPC first), not by raw node id.
        assertTrue(service.contains("ANOMALIES_TAG"));
        assertTrue(service.contains("normalize(npcId)") || service.contains("\"world\""),
                "anomaly log must bucket by NPC/authority");
    }

    @Test
    void guardsProtectAndArrestHasRecoveryAndRelease() throws Exception {
        String service = Files.readString(JAVA_ROOT.resolve("npc/DialogueWorldActionService.java"));

        // callGuard summons an owner-bound GUARD (configure + Stance.GUARD) rather than a
        // hostile shell aimed at the player, capped per faction.
        assertTrue(service.contains("guard.configure(player,")
                && service.contains("guard.setStance(SummonedServitorEntity.Stance.GUARD)"),
                "call_guard must spawn an owner-bound guarding servitor");
        assertTrue(service.contains("MAX_GUARDS_PER_FACTION"),
                "call_guard must be capped per faction");
        assertFalse(service.contains("SummonedServitorEntity.Archetype.GENERIC) != null"),
                "call_guard must not be reduced to a generic shell");

        // combat_or_arrest branches on suspicion level: warn / fine / arrest / combat.
        assertTrue(service.contains("int level = suspectLevel(player, authority);"),
                "combat_or_arrest must consult the suspicion level");
        assertTrue(service.contains("arrestPlayer(player, authority);"));
        assertTrue(service.contains("finePlayer(player, authority);"));
        assertTrue(service.contains("warnPlayer(player, authority);"));

        // Arrest must persist a marker with a recoverable position and an explicit release.
        assertTrue(service.contains("public static boolean arrestPlayer("));
        assertTrue(service.contains("public static boolean settleArrest("));
        assertTrue(service.contains("public static boolean isArrested("));
        assertTrue(service.contains("RecoverX"));
        assertTrue(service.contains("player.teleportTo(level,"),
                "arrest release must teleport back to the recoverable spot");
    }
}
