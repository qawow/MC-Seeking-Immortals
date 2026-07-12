package com.xunxian.seekingimmortals.worldpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WorldpackDataService {
    private static final String BASE_PATH = "data/" + SeekingImmortalsMod.MODID + "/worldpack/";
    private static final Snapshot BUILTIN = loadBuiltin();

    private WorldpackDataService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Snapshot parseForTest(Reader regions, Reader secretRealms, Reader dailyEvents) {
        return new Snapshot(
                parseRegions(JsonParser.parseReader(regions).getAsJsonObject()),
                parseSecretRealms(JsonParser.parseReader(secretRealms).getAsJsonObject()),
                parseDailyEvents(JsonParser.parseReader(dailyEvents).getAsJsonObject()));
    }

    private static Snapshot loadBuiltin() {
        return new Snapshot(
                parseRegions(readBuiltin("regions.json")),
                parseSecretRealms(readBuiltin("secret_realms.json")),
                parseDailyEvents(readBuiltin("daily_events.json")));
    }

    private static JsonObject readBuiltin(String fileName) {
        String path = BASE_PATH + fileName;
        try (InputStream stream = WorldpackDataService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.warn("Missing built-in worldpack resource {}", path);
                return new JsonObject();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load built-in worldpack resource {}", path, exception);
            return new JsonObject();
        }
    }

    private static List<RegionCard> parseRegions(JsonObject root) {
        JsonArray array = array(root, "regions");
        List<RegionCard> regions = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String id = string(object, "id");
            if (id.isBlank()) {
                continue;
            }
            regions.add(new RegionCard(
                    id,
                    string(object, "display_zh"),
                    string(object, "display_en"),
                    doubleValue(object, "aura_multiplier", 1.0D),
                    string(object, "min_realm"),
                    string(object, "travel_anchor"),
                    strings(object, "tags")));
        }
        return List.copyOf(regions);
    }

    private static List<SecretRealm> parseSecretRealms(JsonObject root) {
        JsonArray array = array(root, "secret_realms");
        List<SecretRealm> realms = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String id = string(object, "id");
            if (id.isBlank()) {
                continue;
            }
            realms.add(new SecretRealm(
                    id,
                    string(object, "region_id"),
                    string(object, "display_zh"),
                    string(object, "display_en"),
                    string(object, "min_realm"),
                    string(object, "ticket_item"),
                    intValue(object, "cooldown_ticks", 0),
                    string(object, "return_policy"),
                    strings(object, "tags")));
        }
        return List.copyOf(realms);
    }

    private static List<DailyEvent> parseDailyEvents(JsonObject root) {
        JsonArray array = array(root, "daily_events");
        List<DailyEvent> events = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String id = string(object, "id");
            if (id.isBlank()) {
                continue;
            }
            events.add(new DailyEvent(
                    id,
                    string(object, "region_id"),
                    string(object, "display_zh"),
                    string(object, "display_en"),
                    Math.max(1, intValue(object, "weight", 1)),
                    Math.max(0, intValue(object, "duration_ticks", 0)),
                    strings(object, "effects")));
        }
        return List.copyOf(events);
    }

    private static JsonArray array(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static List<String> strings(JsonObject object, String key) {
        JsonArray array = array(object, key);
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String value = element.getAsString();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return List.copyOf(values);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsInt()
                : fallback;
    }

    private static double doubleValue(JsonObject object, String key, double fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsDouble()
                : fallback;
    }

    public record Snapshot(List<RegionCard> regions, List<SecretRealm> secretRealms, List<DailyEvent> dailyEvents) {
        public Optional<RegionCard> findRegion(String id) {
            return regions.stream().filter(region -> region.id().equals(id)).findFirst();
        }

        public Optional<SecretRealm> findSecretRealm(String id) {
            return secretRealms.stream().filter(realm -> realm.id().equals(id)).findFirst();
        }

        public Optional<DailyEvent> findDailyEvent(String id) {
            return dailyEvents.stream().filter(event -> event.id().equals(id)).findFirst();
        }

        public List<DailyEvent> eventsForRegion(String regionId) {
            return dailyEvents.stream().filter(event -> event.regionId().equals(regionId)).toList();
        }
    }

    public record RegionCard(String id, String displayZh, String displayEn, double auraMultiplier,
                             String minRealm, String travelAnchor, List<String> tags) {}

    public record SecretRealm(String id, String regionId, String displayZh, String displayEn,
                              String minRealm, String ticketItem, int cooldownTicks,
                              String returnPolicy, List<String> tags) {}

    public record DailyEvent(String id, String regionId, String displayZh, String displayEn,
                             int weight, int durationTicks, List<String> effects) {}
}
