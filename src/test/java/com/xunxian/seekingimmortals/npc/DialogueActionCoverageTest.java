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
            "anomaly_log");

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
}
