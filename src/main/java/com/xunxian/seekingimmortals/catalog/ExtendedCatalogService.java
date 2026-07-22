package com.xunxian.seekingimmortals.catalog;

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
import java.util.Map;
import java.util.Optional;

/**
 * Extended text-material indexes: quests, sects, economy bands, consumables, chapters, daily events.
 */
public final class ExtendedCatalogService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private ExtendedCatalogService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public record PriceBand(String id, int min, int max, int suggested, String band, String note) {}
    /**
     * Start requirements that can be evaluated without loading the full narrative engine.
     * The legacy three-argument constructor remains source-compatible for thin index entries.
     */
    public record QuestStartRequirements(String realmMin, String faction, String region,
                                         String pathRequired, String raceRequired,
                                         String karmaRequired, String parentChain,
                                         String extendsChain) {
        public QuestStartRequirements(String realmMin, String faction, String region) {
            this(realmMin, faction, region, "", "", "", "", "");
        }
    }
    public record QuestChain(String id, String display, String region, String realmSpan, int stepCount,
                             String mainChapterRef, List<String> rewardsFinale,
                             QuestStartRequirements startRequirements,
                             List<String> stepHooks, String alchemyLoopRef, String skillTreeRef) {
        public QuestChain(String id, String display, String region, String realmSpan, int stepCount,
                          String mainChapterRef, List<String> rewardsFinale,
                          QuestStartRequirements startRequirements) {
            this(id, display, region, realmSpan, stepCount, mainChapterRef, rewardsFinale,
                    startRequirements, List.of(), "", "");
        }
    }
    public record SectEntry(String id, String display, String region, String alignment, String realmFocus, String specialty) {}
    public record ConsumableEntry(String id, String display, String category, String realmMin, String effect) {}
    public record StoryChapter(String id, String display, String region, String realmSpan, String summary,
                               List<String> questChainRefs) {
        public StoryChapter(String id, String display, String region, String realmSpan, String summary) {
            this(id, display, region, realmSpan, summary, List.of());
        }
    }
    public record DailyEvent(String id, String display, String region, int weight, List<String> effects) {}

    public record IdDisplay(String id, String display, String extra) {}

    public record Snapshot(Map<String, PriceBand> priceBands,
                           Map<String, QuestChain> questChains,
                           Map<String, SectEntry> sects,
                           Map<String, ConsumableEntry> consumables,
                           Map<String, StoryChapter> chapters,
                           Map<String, DailyEvent> dailyEvents,
                           JsonObject contributionExchange,
                           Map<String, IdDisplay> alchemyRecipes,
                           Map<String, IdDisplay> spatialNodes,
                           Map<String, IdDisplay> materials,
                           Map<String, IdDisplay> pills,
                           Map<String, IdDisplay> artifacts,
                           Map<String, IdDisplay> talismanMaterials) {
        public Optional<PriceBand> findBand(String id) {
            return Optional.ofNullable(priceBands.get(id == null ? "" : id));
        }

        public Optional<QuestChain> findQuest(String id) {
            return Optional.ofNullable(questChains.get(id == null ? "" : id));
        }

        public Optional<SectEntry> findSect(String id) {
            return Optional.ofNullable(sects.get(id == null ? "" : id));
        }

        public int totalIndexedEntries() {
            return priceBands.size() + questChains.size() + sects.size() + consumables.size()
                    + chapters.size() + dailyEvents.size() + alchemyRecipes.size() + spatialNodes.size()
                    + materials.size() + pills.size() + artifacts.size() + talismanMaterials.size();
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, PriceBand> bands = new LinkedHashMap<>();
        JsonObject bandRoot = readJson(path("catalog/economy_price_bands.json"));
        if (bandRoot != null) {
            for (JsonElement element : array(bandRoot, "bands")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                bands.put(id, new PriceBand(id, asInt(o, "min"), asInt(o, "max"), asInt(o, "suggested"), str(o, "band"), str(o, "note")));
            }
        }

        Map<String, QuestChain> quests = new LinkedHashMap<>();
        JsonObject questRoot = readJson(path("catalog/quest_chains_index.json"));
        if (questRoot != null) {
            for (JsonElement element : array(questRoot, "chains")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                String region = str(o, "region");
                String realmSpan = joinOrStr(o, "realm_span");
                if (region.isBlank()) {
                    region = "unknown";
                }
                if (realmSpan.isBlank()) {
                    realmSpan = "QI_REFINING";
                }
                quests.put(id, new QuestChain(id, str(o, "display"), region, realmSpan,
                        asInt(o, "step_count"), str(o, "main_chapter_ref"), stringList(o.get("rewards_finale")),
                        new QuestStartRequirements(firstString(o.get("realm_span")), "", region),
                        stringList(o.get("step_hooks")), str(o, "alchemy_loop_ref"), str(o, "skill_tree_ref")));
            }
        }
        enrichQuestStartRequirements(quests);

        Map<String, SectEntry> sects = new LinkedHashMap<>();
        JsonObject sectRoot = readJson(path("catalog/sects_index.json"));
        if (sectRoot != null) {
            for (JsonElement element : array(sectRoot, "sects")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                sects.put(id, new SectEntry(id, str(o, "display"), str(o, "region"), str(o, "alignment"),
                        str(o, "realm_focus"), str(o, "specialty")));
            }
        }

        Map<String, ConsumableEntry> consumables = new LinkedHashMap<>();
        JsonObject consRoot = readJson(path("catalog/consumables_index.json"));
        if (consRoot != null) {
            for (JsonElement element : array(consRoot, "consumables")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                consumables.put(id, new ConsumableEntry(id, str(o, "display"), str(o, "category"), str(o, "realm_min"), str(o, "effect")));
            }
        }

        Map<String, StoryChapter> chapters = new LinkedHashMap<>();
        JsonObject chapterRoot = readJson(path("catalog/main_story_chapters_index.json"));
        if (chapterRoot != null) {
            for (JsonElement element : array(chapterRoot, "chapters")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                chapters.put(id, new StoryChapter(id, str(o, "display"), str(o, "region"),
                        joinOrStr(o, "realm_span"), str(o, "summary"),
                        stringList(o.get("quest_chain_refs"))));
            }
        }

        Map<String, DailyEvent> events = new LinkedHashMap<>();
        JsonObject eventRoot = readJson(path("catalog/daily_random_events_index.json"));
        if (eventRoot != null) {
            for (JsonElement element : array(eventRoot, "events")) {
                if (!element.isJsonObject()) continue;
                JsonObject o = element.getAsJsonObject();
                String id = str(o, "id");
                if (id.isBlank()) continue;
                events.put(id, new DailyEvent(id, str(o, "display"), str(o, "region"), asInt(o, "weight"), stringList(o.get("effects"))));
            }
        }

        JsonObject contribution = readJson(path("catalog/economy_contribution_exchange.json"));
        if (contribution == null) {
            contribution = new JsonObject();
        }

        Map<String, IdDisplay> alchemy = loadIdDisplayMap("catalog/alchemy_recipes_index.json", "recipes", "output");
        Map<String, IdDisplay> spatial = loadIdDisplayMap("catalog/spatial_nodes_index.json", "nodes", "type");
        Map<String, IdDisplay> materials = loadIdDisplayMap("catalog/materials_index.json", "materials", "category");
        Map<String, IdDisplay> pills = loadIdDisplayMap("catalog/pills_index.json", "pills", "effect");
        Map<String, IdDisplay> artifacts = loadIdDisplayMap("catalog/artifacts_index.json", "artifacts", "tier");
        Map<String, IdDisplay> talismanMats = loadIdDisplayMap("catalog/talisman_materials_index.json", "materials", "grade");

        return new Snapshot(Collections.unmodifiableMap(bands),
                Collections.unmodifiableMap(quests),
                Collections.unmodifiableMap(sects),
                Collections.unmodifiableMap(consumables),
                Collections.unmodifiableMap(chapters),
                Collections.unmodifiableMap(events),
                contribution,
                Collections.unmodifiableMap(alchemy),
                Collections.unmodifiableMap(spatial),
                Collections.unmodifiableMap(materials),
                Collections.unmodifiableMap(pills),
                Collections.unmodifiableMap(artifacts),
                Collections.unmodifiableMap(talismanMats));
    }

    private static Map<String, IdDisplay> loadIdDisplayMap(String relativePath, String arrayKey, String extraKey) {
        Map<String, IdDisplay> map = new LinkedHashMap<>();
        JsonObject root = readJson(path(relativePath));
        if (root == null) {
            return map;
        }
        for (JsonElement element : array(root, arrayKey)) {
            if (!element.isJsonObject()) continue;
            JsonObject o = element.getAsJsonObject();
            String id = str(o, "id");
            if (id.isBlank()) continue;
            map.put(id, new IdDisplay(id, str(o, "display"), str(o, extraKey)));
        }
        return map;
    }

    private static void enrichQuestStartRequirements(Map<String, QuestChain> quests) {
        JsonObject root = readJson(path("text_material/quest_chains.json"));
        if (root == null) {
            return;
        }
        for (JsonElement element : array(root, "chains")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = str(object, "id");
            QuestChain current = quests.get(id);
            if (current == null) {
                continue;
            }
            JsonObject requirements = object(object, "learn_requirements");
            boolean hasStart = requirements.has("start") && requirements.get("start").isJsonObject();
            JsonObject start = object(requirements, "start");
            String realmMin = hasStart ? str(start, "realm_min") : current.startRequirements().realmMin();
            String faction = hasStart ? str(start, "faction") : current.startRequirements().faction();
            String startRegion = hasStart ? str(start, "region") : current.startRequirements().region();
            String pathRequired = firstString(object, "requires");
            String raceRequired = str(object, "race_required");
            String karmaRequired = str(object, "karma_required");
            String parentChain = str(object, "parent_chain");
            String extendsChain = str(object, "extends_chain");
            // Prefer live step hooks / refs from full schema-18 corpus when index is thin.
            List<String> hooks = new ArrayList<>(current.stepHooks());
            JsonElement stepsEl = object.get("steps");
            if (hooks.isEmpty() && stepsEl != null && stepsEl.isJsonArray()) {
                for (JsonElement stepEl : stepsEl.getAsJsonArray()) {
                    String hook = "";
                    if (stepEl.isJsonObject()) {
                        hook = str(stepEl.getAsJsonObject(), "hook");
                    } else if (stepEl.isJsonPrimitive() && stepEl.getAsJsonPrimitive().isString()) {
                        // Schema 18 also permits the compact form ["hook_id", ...].
                        hook = stepEl.getAsString().trim();
                    }
                    if (!hook.isBlank()) hooks.add(hook);
                }
            }
            String alchemy = current.alchemyLoopRef().isBlank() ? str(object, "alchemy_loop_ref") : current.alchemyLoopRef();
            String skillTree = current.skillTreeRef().isBlank() ? str(object, "skill_tree_ref") : current.skillTreeRef();
            int stepCount = current.stepCount();
            if (stepCount <= 0) {
                if (stepsEl != null && stepsEl.isJsonArray()) {
                    stepCount = stepsEl.getAsJsonArray().size();
                } else if (stepsEl != null && stepsEl.isJsonPrimitive()) {
                    try {
                        stepCount = stepsEl.getAsInt();
                    } catch (Exception ignored) {
                        stepCount = 0;
                    }
                }
            }
            String region = current.region().isBlank() ? firstNonBlank(str(object, "region"), "unknown") : current.region();
            if (startRegion == null || startRegion.isBlank()) {
                startRegion = region;
            }
            String realmSpan = current.realmSpan().isBlank()
                    ? firstNonBlank(joinOrStr(object, "realm_span"), "QI_REFINING") : current.realmSpan();
            List<String> finale = current.rewardsFinale();
            if ((finale == null || finale.isEmpty()) && object.has("rewards_finale")) {
                finale = stringList(object.get("rewards_finale"));
            }
            String mainChapter = current.mainChapterRef().isBlank() ? str(object, "main_chapter_ref") : current.mainChapterRef();
            quests.put(id, new QuestChain(current.id(), current.display(), region, realmSpan,
                    stepCount, mainChapter, finale,
                    new QuestStartRequirements(realmMin, faction, startRegion,
                            pathRequired, raceRequired, karmaRequired, parentChain, extendsChain),
                    List.copyOf(hooks), alchemy, skillTree));
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b == null ? "" : b;
    }

    private static String firstString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        JsonElement value = object.get(key);
        if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
                try {
                    String text = element.getAsString().trim();
                    if (!text.isBlank()) {
                        return text;
                    }
                } catch (Exception ignored) {
                    // Ignore malformed authored requirement rows.
                }
            }
            return "";
        }
        return str(object, key);
    }

    private static String joinOrStr(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        JsonElement element = object.get(key);
        if (element.isJsonArray()) {
            List<String> parts = stringList(element);
            return String.join(",", parts);
        }
        return str(object, key);
    }

    private static String path(String relative) {
        return "data/" + SeekingImmortalsMod.MODID + "/" + relative;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = ExtendedCatalogService.class.getClassLoader().getResourceAsStream(path)) {
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

    private static JsonObject object(JsonObject root, String key) {
        if (root == null || !root.has(key) || !root.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return root.getAsJsonObject(key);
    }

    private static String firstString(JsonElement element) {
        List<String> values = stringList(element);
        return values.isEmpty() ? "" : values.get(0);
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
}
