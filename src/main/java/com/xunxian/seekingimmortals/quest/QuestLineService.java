package com.xunxian.seekingimmortals.quest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * M11: QUEST_LINES_FULL_v147 (35 lines) chapterized service.
 * Validates line ↔ chain / chapter cross-refs against ExtendedCatalog + playable index.
 */
public final class QuestLineService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private QuestLineService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record QuestLine(String id, String display, String region, List<String> realmSpan,
                            List<String> chapters, List<String> leadsTo, int stepCount,
                            String playtimeHint, String synopsis) {}

    public record Snapshot(Map<String, QuestLine> lines,
                           Map<String, List<String>> byChapter,
                           List<String> unresolvedChainRefs,
                           List<String> unresolvedChapterRefs) {
        public int lineCount() {
            return lines.size();
        }
    }

    public static int lineCount() {
        return BUILTIN.lineCount();
    }

    public static Optional<QuestLine> find(String lineId) {
        return Optional.ofNullable(BUILTIN.lines().get(normalize(lineId)));
    }

    public static List<String> linesForChapter(String chapterId) {
        return BUILTIN.byChapter().getOrDefault(normalize(chapterId), List.of());
    }

    public static List<String> sample(int limit) {
        List<String> out = new ArrayList<>();
        int i = 0;
        for (QuestLine line : BUILTIN.lines().values()) {
            out.add(line.id() + " | " + line.display() + " ch=" + String.join(",", line.chapters())
                    + " steps=" + line.stepCount());
            if (++i >= Math.max(1, limit)) {
                break;
            }
        }
        return out;
    }

    /**
     * Cross-ref validation: every line's chapter ids and lead_to targets that look like chains
     * must resolve against catalog chapters / chains / playable ids.
     */
    public static boolean crossRefsResolvable() {
        return BUILTIN.unresolvedChainRefs().isEmpty() && BUILTIN.unresolvedChapterRefs().isEmpty();
    }

    private static Snapshot loadBuiltin() {
        Map<String, QuestLine> lines = new LinkedHashMap<>();
        Map<String, List<String>> byChapter = new LinkedHashMap<>();
        Set<String> knownChains = new LinkedHashSet<>(ExtendedCatalogService.builtin().questChains().keySet());
        knownChains.addAll(loadPlayableIds());
        Set<String> knownChapters = new LinkedHashSet<>(ExtendedCatalogService.builtin().chapters().keySet());

        JsonObject root = readJson(path("catalog/quest_lines_full_index.json"));
        if (root == null) {
            root = readJson(path("text_material/quest_lines_full_descriptions_v147.json"));
        }
        JsonArray array = array(root, "lines");
        List<String> unresolvedChains = new ArrayList<>();
        List<String> unresolvedChapters = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) {
                continue;
            }
            List<String> chapters = stringList(o.get("chapters"));
            List<String> leads = stringList(o.get("leads_to"));
            int steps = asInt(o, "step_count");
            if (steps <= 0 && o.has("steps") && o.get("steps").isJsonArray()) {
                steps = o.getAsJsonArray("steps").size();
            }
            QuestLine line = new QuestLine(
                    id,
                    firstNonBlank(str(o, "display"), str(o, "title"), id),
                    str(o, "region"),
                    stringList(o.get("realm_span")),
                    chapters,
                    leads,
                    steps,
                    str(o, "playtime_hint"),
                    firstNonBlank(str(o, "synopsis"), str(o, "tagline"), "")
            );
            lines.put(id, line);
            for (String chapter : chapters) {
                String ch = normalize(chapter);
                if (!ch.isBlank() && !knownChapters.contains(ch) && !knownChapters.contains(chapter)) {
                    unresolvedChapters.add(id + "->" + chapter);
                }
                byChapter.computeIfAbsent(ch.isBlank() ? chapter : ch, k -> new ArrayList<>()).add(id);
            }
            for (String lead : leads) {
                String lid = normalize(lead);
                if (lid.isBlank()) {
                    continue;
                }
                // lead_to may point to another line or a chain/playable id.
                if (!lines.containsKey(lid) && !knownChains.contains(lid) && !knownChains.contains(lead)) {
                    // defer: line may appear later in file; recheck after loop
                    unresolvedChains.add(lid);
                }
            }
        }
        // Second pass: drop leads that resolve as other lines.
        List<String> still = new ArrayList<>();
        for (String ref : unresolvedChains) {
            if (lines.containsKey(ref) || knownChains.contains(ref)) {
                continue;
            }
            still.add(ref);
        }
        // Freeze chapter lists.
        Map<String, List<String>> frozenChapters = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : byChapter.entrySet()) {
            frozenChapters.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return new Snapshot(
                Collections.unmodifiableMap(lines),
                Collections.unmodifiableMap(frozenChapters),
                List.copyOf(still),
                List.copyOf(unresolvedChapters)
        );
    }

    private static Set<String> loadPlayableIds() {
        Set<String> ids = new LinkedHashSet<>();
        JsonObject root = readJson(path("catalog/quest_chains_playable_index.json"));
        if (root == null) {
            root = readJson(path("text_material/quest_chains_playable_v141.json"));
        }
        for (JsonElement element : array(root, "chains")) {
            if (!element.isJsonObject()) {
                continue;
            }
            String id = str(element.getAsJsonObject(), "id");
            if (!id.isBlank()) {
                ids.add(id);
            }
        }
        return ids;
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = QuestLineService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonArray array(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return root.getAsJsonArray(key);
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
    }

    private static int asInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return 0;
        }
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static List<String> stringList(JsonElement element) {
        if (element == null) {
            return List.of();
        }
        if (element.isJsonPrimitive()) {
            return List.of(element.getAsString());
        }
        if (!element.isJsonArray()) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            try {
                list.add(child.getAsString());
            } catch (Exception ignored) {
                list.add(String.valueOf(child));
            }
        }
        return List.copyOf(list);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
