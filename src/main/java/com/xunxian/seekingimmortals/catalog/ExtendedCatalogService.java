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
    public record QuestStartRequirements(String realmMin, String faction, String region) {}
    public record QuestChain(String id, String display, String region, String realmSpan, int stepCount,
                             String mainChapterRef, List<String> rewardsFinale,
                             QuestStartRequirements startRequirements) {}
    public record SectEntry(String id, String display, String region, String alignment, String realmFocus, String specialty) {}
    public record ConsumableEntry(String id, String display, String category, String realmMin, String effect) {}
    public record StoryChapter(String id, String display, String region, String realmSpan, String summary) {}
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
                quests.put(id, new QuestChain(id, str(o, "display"), region, str(o, "realm_span"),
                        asInt(o, "step_count"), str(o, "main_chapter_ref"), stringList(o.get("rewards_finale")),
                        new QuestStartRequirements(firstString(o.get("realm_span")), "", region)));
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
                chapters.put(id, new StoryChapter(id, str(o, "display"), str(o, "region"), str(o, "realm_span"), str(o, "summary")));
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
            String region = hasStart ? str(start, "region") : current.startRequirements().region();
            quests.put(id, new QuestChain(current.id(), current.display(), current.region(), current.realmSpan(),
                    current.stepCount(), current.mainChapterRef(), current.rewardsFinale(),
                    new QuestStartRequirements(realmMin, faction, region)));
        }
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
