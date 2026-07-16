package com.xunxian.seekingimmortals.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M12 dialogue archetype templates (greeting/quest_offer/... + binds + branch_tree_id).
 */
public final class DialogueTemplateService {
    public static final List<String> STANDARD_TAGS = List.of(
            "greeting", "quest_offer", "quest_turnin", "shop", "travel",
            "rep_gate", "threat", "lore", "farewell");

    private static final Snapshot BUILTIN = loadBuiltin();

    private DialogueTemplateService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<Archetype> find(String archetypeId) {
        return Optional.ofNullable(BUILTIN.archetypes().get(normalize(archetypeId)));
    }

    public static Optional<String> archetypeForNpc(String npcId) {
        return Optional.ofNullable(BUILTIN.npcBindings().get(normalize(npcId)));
    }

    public static List<String> lines(String archetypeId, String tag) {
        return find(archetypeId)
                .map(archetype -> archetype.lines().getOrDefault(normalize(tag), List.of()))
                .orElse(List.of());
    }

    public static int archetypeCount() {
        return BUILTIN.archetypes().size();
    }

    private static Snapshot loadBuiltin() {
        Map<String, Archetype> archetypes = new LinkedHashMap<>();
        Map<String, String> bindings = new LinkedHashMap<>();
        for (String path : List.of(
                "data/" + SeekingImmortalsMod.MODID + "/text_material/npc_dialogue_templates_v138.json",
                "data/" + SeekingImmortalsMod.MODID + "/text_material/npc_dialogue_templates.json")) {
            JsonObject root = readJson(path);
            if (root == null) {
                continue;
            }
            if (root.has("archetypes") && root.get("archetypes").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("archetypes")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    Archetype archetype = parseArchetype(element.getAsJsonObject());
                    if (!archetype.id().isBlank()) {
                        archetypes.put(archetype.id(), archetype);
                    }
                }
            }
            if (root.has("named_npc_bindings") && root.get("named_npc_bindings").isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray("named_npc_bindings")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject o = element.getAsJsonObject();
                    String npcId = normalize(str(o, "npc_id", ""));
                    String archetype = normalize(str(o, "archetype", ""));
                    if (!npcId.isBlank() && !archetype.isBlank()) {
                        bindings.put(npcId, archetype);
                    }
                }
            }
            if (!archetypes.isEmpty()) {
                break;
            }
        }
        return new Snapshot(Collections.unmodifiableMap(archetypes), Map.copyOf(bindings));
    }

    private static Archetype parseArchetype(JsonObject object) {
        String id = normalize(str(object, "id", ""));
        String display = str(object, "display", id);
        String branchTreeId = normalize(str(object, "branch_tree_id", ""));
        if (branchTreeId.isBlank() && !id.isBlank()) {
            branchTreeId = "tree_" + id;
        }
        Map<String, List<String>> lines = new LinkedHashMap<>();
        if (object.has("lines") && object.get("lines").isJsonObject()) {
            JsonObject lineObj = object.getAsJsonObject("lines");
            for (Map.Entry<String, JsonElement> entry : lineObj.entrySet()) {
                lines.put(normalize(entry.getKey()), stringList(entry.getValue()));
            }
        }
        List<String> binds = stringList(object.get("binds"));
        return new Archetype(id, display, branchTreeId, Map.copyOf(lines), binds);
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child != null && child.isJsonPrimitive()) {
                    String value = child.getAsString();
                    if (value != null && !value.isBlank()) {
                        list.add(value.trim());
                    }
                }
            }
        } else if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            if (value != null && !value.isBlank()) {
                list.add(value.trim());
            }
        }
        return List.copyOf(list);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = DialogueTemplateService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load dialogue templates {}", path, exception);
            return null;
        }
    }

    private static String str(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ex) {
            return String.valueOf(object.get(key));
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Snapshot(Map<String, Archetype> archetypes, Map<String, String> npcBindings) {}

    public record Archetype(
            String id,
            String display,
            String branchTreeId,
            Map<String, List<String>> lines,
            List<String> binds) {}
}
