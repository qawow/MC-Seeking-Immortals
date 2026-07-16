package com.xunxian.seekingimmortals.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * M07 MP sequence export for Patchouli/M16 display consumers.
 * Runtime does not enforce multiplayer locks; this is presentation data only.
 */
public final class MultiblockSequenceDisplayCatalog {
    private static final Snapshot BUILTIN = loadBuiltin();

    private MultiblockSequenceDisplayCatalog() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record SequenceStep(int order, String actor, String action, String note) {}

    public record SequenceEntry(
            String id,
            String display,
            List<String> appliesTo,
            List<String> supportStructures,
            List<SequenceStep> steps,
            List<String> edgeCases
    ) {}

    public record Snapshot(List<SequenceEntry> sequences) {
        public Snapshot {
            sequences = sequences == null ? List.of() : List.copyOf(sequences);
        }

        public int size() {
            return sequences.size();
        }

        public Optional<SequenceEntry> find(String id) {
            if (id == null || id.isBlank()) {
                return Optional.empty();
            }
            for (SequenceEntry entry : sequences) {
                if (entry.id().equalsIgnoreCase(id)) {
                    return Optional.of(entry);
                }
            }
            return Optional.empty();
        }

        public List<SequenceEntry> forStructure(String structureId) {
            if (structureId == null || structureId.isBlank()) {
                return List.of();
            }
            String key = structureId.trim().toLowerCase(Locale.ROOT);
            List<SequenceEntry> out = new ArrayList<>();
            for (SequenceEntry entry : sequences) {
                for (String apply : entry.appliesTo()) {
                    String a = apply == null ? "" : apply.toLowerCase(Locale.ROOT);
                    if (a.equals(key) || a.contains(key) || key.contains(a)
                            || a.equals("any_structure_with_operational_states")
                            || a.equals("any_with_build_stages")) {
                        out.add(entry);
                        break;
                    }
                }
            }
            return List.copyOf(out);
        }
    }

    private static Snapshot loadBuiltin() {
        List<SequenceEntry> list = new ArrayList<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/multiblock_mp_sequence_display.json");
        if (root == null) {
            root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/multiblock_multiplayer_sequences_v135.json");
        }
        if (root != null) {
            for (JsonElement element : array(root, "sequences")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) {
                    continue;
                }
                List<SequenceStep> steps = new ArrayList<>();
                int order = 1;
                for (JsonElement stepEl : array(o, "steps")) {
                    if (stepEl.isJsonObject()) {
                        JsonObject s = stepEl.getAsJsonObject();
                        steps.add(new SequenceStep(
                                intVal(s, "order", order),
                                str(s, "actor"),
                                str(s, "action").isBlank() ? str(s, "result") : str(s, "action"),
                                str(s, "note").isBlank() ? str(s, "rules") : str(s, "note")));
                    } else if (stepEl.isJsonPrimitive()) {
                        steps.add(new SequenceStep(order, "", stepEl.getAsString(), ""));
                    }
                    order++;
                }
                list.add(new SequenceEntry(
                        id,
                        str(o, "display").isBlank() ? id : str(o, "display"),
                        stringList(o.get("applies_to")),
                        stringList(o.get("support_structures")),
                        List.copyOf(steps),
                        stringList(o.get("edge_cases"))));
            }
        }
        return new Snapshot(list);
    }

    private static JsonObject readJson(String path) {
        try (InputStream in = MultiblockSequenceDisplayCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
        } catch (Exception e) {
            SeekingImmortalsMod.LOGGER.warn("Failed loading MP sequence display {}", path, e);
            return null;
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return root.getAsJsonArray(key);
    }

    private static List<String> stringList(JsonElement el) {
        if (el == null || !el.isJsonArray()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (JsonElement e : el.getAsJsonArray()) {
            if (e != null && e.isJsonPrimitive()) {
                out.add(e.getAsString());
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static String str(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        try {
            return o.get(key).getAsString().trim();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int intVal(JsonObject o, String key, int def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        try {
            return o.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return def;
        }
    }
}
