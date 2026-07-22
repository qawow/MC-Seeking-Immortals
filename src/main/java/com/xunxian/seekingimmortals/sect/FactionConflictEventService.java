package com.xunxian.seekingimmortals.sect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
import java.util.concurrent.ThreadLocalRandom;

/**
 * M08 faction conflict events. Subscribes to M06 daily-event refresh path and applies
 * server-side reputation / price modifiers. Soft quest bridging remains in FactionConflictSoftService.
 */
public final class FactionConflictEventService {
    private static final String ROOT = "seeking_immortals_faction_conflict";
    private static final String ACTIVE_ID = "ActiveId";
    private static final String ACTIVE_UNTIL = "ActiveUntil";
    private static final String PRICE_MOD = "PriceModBp"; // basis points, 10000 = 1.0
    private static final String LAST_DAILY = "LastDailyEvent";

    private static final Snapshot BUILTIN = loadBuiltin();

    private FactionConflictEventService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Optional<ConflictEvent> find(String eventId) {
        return Optional.ofNullable(BUILTIN.events().get(normalize(eventId)));
    }

    public static List<ConflictEvent> eventsForRegion(String regionId) {
        String region = normalize(regionId);
        List<ConflictEvent> list = new ArrayList<>();
        for (ConflictEvent event : BUILTIN.events().values()) {
            if (region.isBlank() || event.region().isBlank() || region.equals(event.region())
                    || region.contains(event.region()) || event.region().contains(region)) {
                list.add(event);
            }
        }
        return List.copyOf(list);
    }

    /**
     * M06 daily-event subscription hook. Called from WorldpackGameplayService.refreshDailyEvent.
     */
    public static void onDailyEvent(ServerPlayer player, String regionId, String dailyEventId) {
        if (player == null) {
            return;
        }
        String daily = normalize(dailyEventId);
        String region = normalize(regionId);
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (daily.equals(root.getString(LAST_DAILY))) {
            // already processed this daily id for this player
            maybeExpire(player, root);
            return;
        }
        root.putString(LAST_DAILY, daily);

        long gameTime = player.level().getGameTime();
        maybeExpire(player, root);

        ConflictEvent matched = matchConflict(region, daily);
        if (matched == null) {
            player.getPersistentData().put(ROOT, root);
            return;
        }

        applyEffects(player, matched, root, gameTime);
        player.getPersistentData().put(ROOT, root);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.faction_conflict.daily_trigger",
                PlayerDisplayText.safeLiteral(
                        matched.display(), "text.seeking_immortals.unknown_event"),
                eventTypeDisplay(matched.type())), true);
    }

    private static Component eventTypeDisplay(String type) {
        return switch (normalize(type)) {
            case "rivalry" -> Component.literal("势力摩擦");
            case "war", "regional_war" -> Component.literal("区域战事");
            case "blockade" -> Component.literal("封锁");
            case "alliance" -> Component.literal("结盟");
            case "trade" -> Component.literal("商贸变动");
            case "ritual" -> Component.literal("大型仪式");
            case "world_cycle" -> Component.literal("天地周期");
            case "world_event" -> Component.literal("天地异象");
            default -> Component.translatable("text.seeking_immortals.unknown_event");
        };
    }

    public static Optional<String> activeConflictId(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        long until = root.getLong(ACTIVE_UNTIL);
        if (until > 0L && player.level().getGameTime() > until) {
            return Optional.empty();
        }
        String id = root.getString(ACTIVE_ID);
        return id == null || id.isBlank() ? Optional.empty() : Optional.of(id);
    }

    /**
     * Shop/market price multiplier while a conflict with tax/blockade effects is active.
     * 1.0 = no change. Greater than 1 means more expensive.
     */
    public static double activePriceMultiplier(ServerPlayer player) {
        if (player == null) {
            return 1.0D;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        long until = root.getLong(ACTIVE_UNTIL);
        if (until > 0L && player.level().getGameTime() > until) {
            return 1.0D;
        }
        int bp = root.getInt(PRICE_MOD);
        if (bp <= 0) {
            return 1.0D;
        }
        return Math.max(0.5D, Math.min(2.0D, bp / 10000.0D));
    }

    private static ConflictEvent matchConflict(String region, String dailyEventId) {
        if (BUILTIN.events().isEmpty()) {
            return null;
        }
        // 1) direct id hit
        ConflictEvent direct = BUILTIN.events().get(dailyEventId);
        if (direct != null) {
            return direct;
        }
        // 2) trigger token overlap with daily event id
        List<ConflictEvent> candidates = new ArrayList<>();
        for (ConflictEvent event : BUILTIN.events().values()) {
            boolean regionOk = event.region().isBlank() || region.isBlank()
                    || region.equals(event.region())
                    || region.contains(event.region())
                    || event.region().contains(region);
            if (!regionOk) {
                continue;
            }
            if (dailyEventId.contains(normalize(event.id())) || normalize(event.id()).contains(dailyEventId)) {
                candidates.add(event);
                continue;
            }
            for (String trigger : event.triggers()) {
                String t = normalize(trigger);
                if (!t.isBlank() && (dailyEventId.contains(t) || t.contains(dailyEventId))) {
                    candidates.add(event);
                    break;
                }
            }
            // token keywords
            if (dailyEventId.contains("war") || dailyEventId.contains("raid") || dailyEventId.contains("conflict")
                    || dailyEventId.contains("skirmish") || dailyEventId.contains("bandit")) {
                if (event.type().contains("war") || event.type().contains("rivalry") || event.type().contains("raid")
                        || event.type().contains("blockade") || event.type().contains("regional")) {
                    candidates.add(event);
                }
            }
        }
        if (candidates.isEmpty()) {
            // Low-probability ambient rivalry in region when daily id is non-empty.
            List<ConflictEvent> regional = eventsForRegion(region);
            if (!regional.isEmpty() && !dailyEventId.isBlank()
                    && ThreadLocalRandom.current().nextInt(100) < 8) {
                return regional.get(ThreadLocalRandom.current().nextInt(regional.size()));
            }
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private static void applyEffects(ServerPlayer player, ConflictEvent event, CompoundTag root, long gameTime) {
        root.putString(ACTIVE_ID, event.id());
        root.putLong(ACTIVE_UNTIL, gameTime + 24000L); // one Minecraft day

        int repShift = event.reputationShift();
        if (repShift == 0) {
            repShift = switch (normalize(event.type())) {
                case "war", "regional_war" -> 5;
                case "rivalry" -> 3;
                case "blockade" -> 2;
                case "alliance", "trade" -> 1;
                default -> 2;
            };
        }
        List<String> factions = event.factions();
        if (factions.size() >= 2) {
            // Player leaning: small gain with first faction, loss with second (conflict pressure).
            ReputationService.add(player, ReputationUnlockService.reputationKey(factions.get(0)), repShift);
            ReputationService.add(player, ReputationUnlockService.reputationKey(factions.get(1)), -Math.max(1, repShift / 2));
            ReputationService.add(player, normalize(factions.get(0)), repShift);
            ReputationService.add(player, normalize(factions.get(1)), -Math.max(1, repShift / 2));
        } else if (factions.size() == 1) {
            ReputationService.add(player, ReputationUnlockService.reputationKey(factions.get(0)), repShift);
        }

        double priceMul = 1.0D;
        if (event.taxIncrease() > 0) {
            priceMul += Math.min(0.5D, event.taxIncrease());
        }
        if (event.blockade()) {
            priceMul += 0.15D;
        }
        if (normalize(event.type()).contains("blockade") || normalize(event.type()).contains("war")) {
            priceMul = Math.max(priceMul, 1.1D);
        }
        root.putInt(PRICE_MOD, (int) Math.round(priceMul * 10000.0D));

        // Optional short war window for war-type events.
        if (normalize(event.type()).contains("war") && factions.size() >= 2 && player.server != null) {
            try {
                SectWarService.start(player.server, factions.get(0), factions.get(1), 10);
            } catch (Exception ignored) {
                // war service optional; conflict rep/price still apply
            }
        }
    }

    private static void maybeExpire(ServerPlayer player, CompoundTag root) {
        long until = root.getLong(ACTIVE_UNTIL);
        if (until > 0L && player.level().getGameTime() > until) {
            root.remove(ACTIVE_ID);
            root.remove(ACTIVE_UNTIL);
            root.remove(PRICE_MOD);
        }
    }

    private static Snapshot loadBuiltin() {
        Map<String, ConflictEvent> events = new LinkedHashMap<>();
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/faction_conflict_events.json");
        if (root == null) {
            return new Snapshot(Map.of());
        }
        JsonArray arr = root.getAsJsonArray("events");
        if (arr == null) {
            return new Snapshot(Map.of());
        }
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            String id = normalize(str(o, "id"));
            if (id.isBlank()) continue;
            List<String> factions = stringList(o.get("factions"));
            List<String> triggers = stringList(o.get("triggers"));
            int repShift = 0;
            double tax = 0.0D;
            boolean blockade = false;
            double duel = 0.0D;
            Map<String, String> rawEffects = new LinkedHashMap<>();
            if (o.has("effects") && o.get("effects").isJsonObject()) {
                JsonObject effects = o.getAsJsonObject("effects");
                repShift = intOr(effects, "reputation_shift", 0);
                tax = doubleOr(effects, "tax_increase", 0.0D);
                duel = doubleOr(effects, "duel_chance", 0.0D);
                blockade = effects.has("blockade_islands") || effects.has("blockade")
                        || (effects.has("blockade_islands") && !effects.get("blockade_islands").isJsonNull());
                if (effects.has("blockade") && effects.get("blockade").isJsonPrimitive()) {
                    try {
                        blockade = blockade || effects.get("blockade").getAsBoolean();
                    } catch (Exception ignored) {
                        blockade = true;
                    }
                }
                for (Map.Entry<String, JsonElement> entry : effects.entrySet()) {
                    rawEffects.put(entry.getKey(), entry.getValue().isJsonNull() ? "" : entry.getValue().toString());
                }
            } else if (o.has("effects") && o.get("effects").isJsonArray()) {
                for (JsonElement e : o.getAsJsonArray("effects")) {
                    if (e.isJsonPrimitive()) {
                        String token = e.getAsString();
                        rawEffects.put(token, "true");
                        if (token.contains("tax")) tax = Math.max(tax, 0.1D);
                        if (token.contains("blockade")) blockade = true;
                        if (token.contains("war")) repShift = Math.max(repShift, 4);
                    }
                }
            }
            events.put(id, new ConflictEvent(
                    id,
                    str(o, "display"),
                    factions,
                    normalize(str(o, "type")),
                    normalize(str(o, "region")),
                    triggers,
                    repShift,
                    tax,
                    blockade,
                    duel,
                    Map.copyOf(rawEffects)));
        }
        return new Snapshot(Collections.unmodifiableMap(events));
    }

    private static List<String> stringList(JsonElement element) {
        List<String> list = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return list;
        }
        if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                if (el.isJsonPrimitive()) {
                    list.add(el.getAsString());
                }
            }
        } else if (element.isJsonPrimitive()) {
            list.add(element.getAsString());
        }
        return list;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = FactionConflictEventService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load faction conflict events {}", path, exception);
            return null;
        }
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

    private static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double doubleOr(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsDouble();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ConflictEvent(String id, String display, List<String> factions, String type, String region,
                                List<String> triggers, int reputationShift, double taxIncrease, boolean blockade,
                                double duelChance, Map<String, String> rawEffects) {}

    public record Snapshot(Map<String, ConflictEvent> events) {
        public int count() {
            return events.size();
        }
    }
}
