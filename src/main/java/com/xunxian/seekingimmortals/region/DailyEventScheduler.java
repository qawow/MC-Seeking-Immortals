package com.xunxian.seekingimmortals.region;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.worldpack.DailyEventEncounterService;
import com.xunxian.seekingimmortals.worldpack.WorldpackDataService;
import com.xunxian.seekingimmortals.worldpack.WorldpackSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side daily event scheduler.
 * Expands worldpack daily_events with multi-region text_material coverage and
 * exposes {@link #onDailyEvent} subscription for M08/M11.
 */
public final class DailyEventScheduler {
    private static final List<DailyEventHook> HOOKS = new CopyOnWriteArrayList<>();
    private static final List<ExpandedEvent> EXPANDED = loadExpanded();
    private static long lastServerDay = Long.MIN_VALUE;

    private DailyEventScheduler() {}

    public static void registerHook(DailyEventHook hook) {
        if (hook != null) {
            HOOKS.add(hook);
        }
    }

    public static void clearHooks() {
        HOOKS.clear();
    }

    public static int hookCount() {
        return HOOKS.size();
    }

    public static int expandedEventCount() {
        return EXPANDED.size();
    }

    /**
     * Public subscription entry used by downstream modules.
     */
    public static void onDailyEvent(String regionId, String eventId) {
        if (regionId == null || eventId == null || regionId.isBlank() || eventId.isBlank()) {
            return;
        }
        for (DailyEventHook hook : HOOKS) {
            try {
                hook.onDailyEvent(regionId, eventId);
            } catch (Exception exception) {
                SeekingImmortalsMod.LOGGER.warn("Daily event hook failed for {}/{}", regionId, eventId, exception);
            }
        }
    }

    public static void serverTick(MinecraftServer server) {
        if (server == null || !RegionEventConfig.isDailyEventsEnabled()) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        long day = overworld.getDayTime() / 24000L;
        if (day == lastServerDay) {
            return;
        }
        lastServerDay = day;
        rollAllRegions(overworld, true);
    }

    public static WorldpackSavedData.EventRoll ensurePlayerEvent(ServerPlayer player, String regionId) {
        if (player == null || player.server == null) {
            return new WorldpackSavedData.EventRoll(regionId == null ? "" : regionId, "", 0L);
        }
        if (!RegionEventConfig.isDailyEventsEnabled()) {
            return new WorldpackSavedData.EventRoll(regionId == null ? "" : regionId, "", 0L);
        }
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackSavedData savedData = WorldpackSavedData.get(player.server.overworld());
        String resolvedRegion = regionId == null || regionId.isBlank()
                ? RegionRegistry.DEFAULT_REGION_ID
                : regionId;
        long gameTime = player.server.overworld().getGameTime();
        WorldpackSavedData.EventRoll previous = savedData.peekDailyEvent(resolvedRegion).orElse(null);
        WorldpackSavedData.EventRoll roll = savedData.getOrRollDailyEvent(
                resolvedRegion, snapshot, gameTime, player.getRandom(), expandedCandidates(resolvedRegion, snapshot));
        if (previous == null || !previous.eventId().equals(roll.eventId()) || !previous.isActive(gameTime)) {
            if (!roll.eventId().isBlank()) {
                onDailyEvent(resolvedRegion, roll.eventId());
                DailyEventEncounterService.maybeSpawn(player, roll.eventId());
            }
        }
        return roll;
    }

    public static void rollAllRegions(ServerLevel overworld, boolean notifyPlayers) {
        if (overworld == null || overworld.getServer() == null || !RegionEventConfig.isDailyEventsEnabled()) {
            return;
        }
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackSavedData savedData = WorldpackSavedData.get(overworld);
        long gameTime = overworld.getGameTime();
        RandomSource random = overworld.getRandom();
        for (WorldpackDataService.RegionCard region : snapshot.regions()) {
            WorldpackSavedData.EventRoll previous = savedData.peekDailyEvent(region.id()).orElse(null);
            WorldpackSavedData.EventRoll roll = savedData.getOrRollDailyEvent(
                    region.id(), snapshot, gameTime, random, expandedCandidates(region.id(), snapshot));
            boolean changed = previous == null
                    || !previous.eventId().equals(roll.eventId())
                    || !previous.isActive(gameTime);
            if (changed && !roll.eventId().isBlank()) {
                onDailyEvent(region.id(), roll.eventId());
                if (notifyPlayers) {
                    for (ServerPlayer player : overworld.getServer().getPlayerList().getPlayers()) {
                        com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player).ifPresent(cultivation -> {
                            if (region.id().equals(cultivation.getWorldpackCurrentRegionId())) {
                                DailyEventEncounterService.maybeSpawn(player, roll.eventId());
                            }
                        });
                    }
                }
            }
        }
    }

    public static List<WorldpackDataService.DailyEvent> expandedCandidates(String regionId,
                                                                           WorldpackDataService.Snapshot snapshot) {
        List<WorldpackDataService.DailyEvent> base = new ArrayList<>(snapshot.eventsForRegion(regionId));
        for (ExpandedEvent expanded : EXPANDED) {
            if (!expanded.matches(regionId)) {
                continue;
            }
            boolean exists = base.stream().anyMatch(event -> event.id().equals(expanded.id()));
            if (exists) {
                continue;
            }
            // Only inject when the event is not already present under another single-region binding
            // with the same id for this region.
            Optional<WorldpackDataService.DailyEvent> existing = snapshot.findDailyEvent(expanded.id());
            if (existing.isPresent() && existing.get().regionId().equals(regionId)) {
                continue;
            }
            base.add(new WorldpackDataService.DailyEvent(
                    expanded.id(),
                    regionId,
                    expanded.displayZh(),
                    expanded.displayEn(),
                    expanded.weight(),
                    expanded.durationTicks(),
                    expanded.effects()));
        }
        return List.copyOf(base);
    }

    private static List<ExpandedEvent> loadExpanded() {
        List<ExpandedEvent> events = new ArrayList<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/daily_random_events.json");
        if (root == null) {
            return List.of();
        }
        for (JsonElement element : array(root, "events")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = str(object, "id");
            if (id.isBlank()) {
                continue;
            }
            List<String> regions = strings(object, "regions");
            List<String> effects = new ArrayList<>();
            if (object.has("buff") && object.get("buff").isJsonPrimitive()) {
                effects.add(object.get("buff").getAsString());
            }
            effects.addAll(strings(object, "effects"));
            effects.addAll(strings(object, "hooks"));
            if (effects.isEmpty()) {
                if (object.has("combat_tier")) {
                    effects.add("trade_risk_up");
                } else if (object.has("rewards")) {
                    effects.add("herb_shop_bonus");
                } else {
                    effects.add("aura_plus_5");
                }
            }
            String display = str(object, "display");
            events.add(new ExpandedEvent(
                    id,
                    regions,
                    display,
                    display,
                    Math.max(1, intValue(object, "weight", 1)),
                    24000,
                    List.copyOf(effects)));
        }
        // tianyuan dedicated pool
        JsonObject tianyuan = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/tianyuan_daily_events.json");
        if (tianyuan != null) {
            String region = firstNonBlank(str(tianyuan, "region"), "tianyuan");
            for (JsonElement element : array(tianyuan, "events")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String id = str(object, "id");
                if (id.isBlank()) {
                    continue;
                }
                List<String> effects = new ArrayList<>(strings(object, "effects"));
                if (effects.isEmpty()) {
                    String effect = str(object, "effect");
                    if (!effect.isBlank()) {
                        effects.add(effect);
                    } else {
                        effects.add("aura_plus_5");
                    }
                }
                String display = str(object, "display");
                events.add(new ExpandedEvent(
                        id,
                        List.of(region),
                        display,
                        display,
                        Math.max(1, intValue(object, "weight", 1)),
                        24000,
                        List.copyOf(effects)));
            }
        }
        return List.copyOf(events);
    }

    private record ExpandedEvent(String id, List<String> regions, String displayZh, String displayEn,
                                 int weight, int durationTicks, List<String> effects) {
        private boolean matches(String regionId) {
            if (regions == null || regions.isEmpty()) {
                return false;
            }
            for (String region : regions) {
                if (region == null) {
                    continue;
                }
                if ("*".equals(region) || "any".equalsIgnoreCase(region) || region.equalsIgnoreCase(regionId)) {
                    return true;
                }
                // soft alias: tiannan_border ~ tiannan / wutu_border
                if ("tiannan_border".equalsIgnoreCase(region)
                        && ("tiannan".equalsIgnoreCase(regionId) || "wutu_border".equalsIgnoreCase(regionId))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : (fallback == null ? "" : fallback);
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = DailyEventScheduler.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load daily event resource {}", path, exception);
            return null;
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonArray()
                ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static List<String> strings(JsonObject object, String key) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array(object, key)) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                String value = element.getAsString();
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private static String str(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : "";
    }

    private static int intValue(JsonObject object, String key, int fallback) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.get(key).getAsJsonPrimitive().isNumber()
                ? object.get(key).getAsInt() : fallback;
    }
}
