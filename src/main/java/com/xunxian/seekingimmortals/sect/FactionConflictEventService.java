package com.xunxian.seekingimmortals.sect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import com.xunxian.seekingimmortals.worldpack.DailyEventEffectCatalog;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
import java.util.concurrent.ThreadLocalRandom;

/**
 * M08 faction conflict events. Subscribes to M06 daily-event refresh path and applies
 * server-side reputation / price modifiers. Soft quest bridging remains in FactionConflictSoftService.
 */
public final class FactionConflictEventService {
    private static final String ROOT = "seeking_immortals_faction_conflict";
    private static final String ACTIVE_ID = "ActiveId";
    private static final String ACTIVE_REGION = "ActiveRegion";
    private static final String ACTIVE_PHASE = "ActivePhase";
    private static final String ACTIVE_UNTIL = "ActiveUntil";
    private static final String PRICE_MOD = "PriceModBp"; // basis points, 10000 = 1.0
    private static final String LAST_DAILY = "LastDailyEvent";
    private static final String DAILY_CLAIMS = "DailyClaims";
    private static final int MAX_DAILY_CLAIMS = 32;

    private static final Snapshot BUILTIN = loadBuiltin();

    private FactionConflictEventService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static boolean hasState(ServerPlayer player) {
        return hasActiveState(player);
    }

    public static boolean hasActiveState(ServerPlayer player) {
        return player != null && hasActiveState(player.getPersistentData().getCompound(ROOT));
    }

    /** Current authored war phase, consumed by encounter reward settlement. */
    public static Optional<String> activePhase(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        return activePhase(player.getPersistentData().getCompound(ROOT), player.level().getGameTime());
    }

    static Optional<String> activePhase(CompoundTag root, long gameTime) {
        if (root == null || root.getLong(ACTIVE_UNTIL) <= gameTime) {
            return Optional.empty();
        }
        String phase = normalize(root.getString(ACTIVE_PHASE));
        return phase.isBlank() ? Optional.empty() : Optional.of(phase);
    }

    static boolean hasActiveState(CompoundTag root) {
        return root != null && (root.contains(ACTIVE_ID)
                || root.contains(ACTIVE_REGION)
                || root.contains(ACTIVE_PHASE)
                || root.contains(ACTIVE_UNTIL)
                || root.contains(PRICE_MOD));
    }

    public static Optional<ConflictEvent> find(String eventId) {
        return Optional.ofNullable(BUILTIN.events().get(normalize(eventId)));
    }

    public static List<ConflictEvent> eventsForRegion(String regionId) {
        String region = normalize(regionId);
        List<ConflictEvent> list = new ArrayList<>();
        for (ConflictEvent event : BUILTIN.events().values()) {
            if (event.matchesRegion(region)) {
                list.add(event);
            }
        }
        return List.copyOf(list);
    }

    /**
     * M06 daily-event subscription hook. Called from WorldpackGameplayService.refreshDailyEvent.
     */
    public static void onDailyEvent(ServerPlayer player, String regionId, String dailyEventId) {
        onDailyEvent(player, regionId, dailyEventId, 0L);
    }

    public static void onDailyEvent(ServerPlayer player, String regionId, String dailyEventId, long rollUntilTick) {
        if (player == null) {
            return;
        }
        String daily = normalize(dailyEventId);
        String region = normalize(regionId);
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (daily.isBlank()) {
            clearActive(root);
            root.remove(LAST_DAILY);
            player.getPersistentData().put(ROOT, root);
            return;
        }
        long gameTime = player.level().getGameTime();
        maybeExpire(player, root);
        if (rollUntilTick > 0L && rollUntilTick <= gameTime) {
            player.getPersistentData().put(ROOT, root);
            return;
        }
        long effectiveUntil = rollUntilTick;
        if (effectiveUntil <= 0L) {
            effectiveUntil = CultivationHelper.get(player)
                    .filter(cultivation -> daily.equals(normalize(cultivation.getWorldpackActiveDailyEventId())))
                    .map(cultivation -> cultivation.getWorldpackActiveDailyEventUntilTick())
                    .filter(until -> until > gameTime)
                    .orElseGet(() -> fallbackClaimUntil(gameTime));
        }
        String dailyKey = dailyClaimKey(region, daily, effectiveUntil);

        // Authored events are authoritative. A non-faction authored event must
        // not fall through to the legacy heuristic/ambient conflict resolver,
        // and a realm-gated event must not retain a conflict after the player
        // becomes ineligible during the same roll.
        DailyEventEffectCatalog.Event authored = DailyEventEffectCatalog.builtin().find(daily).orElse(null);
        if (authored != null && (!authored.hasFactionWar()
                || !com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor.isRealmAllowed(player, authored))) {
            clearActive(root);
            removeDailyClaim(root, dailyKey, gameTime);
            root.remove(LAST_DAILY);
            player.getPersistentData().put(ROOT, root);
            return;
        }

        ConflictEvent matched = resolveConflict(region, daily).orElse(null);
        boolean claimed = hasDailyClaim(root, dailyKey, gameTime);
        if (matched == null) {
            if (!claimed) {
                recordDailyClaim(root, dailyKey, gameTime);
            }
            clearActive(root);
            player.getPersistentData().put(ROOT, root);
            return;
        }
        if (!isRealmAllowed(player, matched)) {
            clearActive(root);
            removeDailyClaim(root, dailyKey, gameTime);
            root.remove(LAST_DAILY);
            player.getPersistentData().put(ROOT, root);
            return;
        }
        if (claimed && isSameActiveConflict(root, matched, region, gameTime)) {
            root.putString(LAST_DAILY, dailyKey);
            player.getPersistentData().put(ROOT, root);
            return;
        }
        if (!claimed) {
            recordDailyClaim(root, dailyKey, gameTime);
        } else {
            root.putString(LAST_DAILY, dailyKey);
        }

        applyEffects(player, matched, root, gameTime, effectiveUntil, region,
                authored == null ? "" : authored.warPhase(), !claimed);
        player.getPersistentData().put(ROOT, root);
        if (!claimed) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.faction_conflict.daily_trigger",
                    PlayerDisplayText.safeLiteral(
                            matched.display(), "text.seeking_immortals.unknown_event"),
                    eventTypeDisplay(matched.type())), true);
        }
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
        return activeConflict(player).map(ConflictEvent::id);
    }

    /**
     * Shop/market price multiplier while a conflict with tax/blockade effects is active.
     * 1.0 = no change. Greater than 1 means more expensive.
     */
    public static double activePriceMultiplier(ServerPlayer player) {
        if (activeConflict(player).isEmpty()) {
            return 1.0D;
        }
        int bp = player.getPersistentData().getCompound(ROOT).getInt(PRICE_MOD);
        if (bp <= 0) {
            return 1.0D;
        }
        return Math.max(0.5D, Math.min(2.0D, bp / 10000.0D));
    }

    /** Returns true when the current authored conflict explicitly closes ferry routes. */
    public static boolean activeFerryDelayed(ServerPlayer player) {
        return activeConflict(player)
                .map(event -> rawBoolean(event.rawEffects().get("ferry_delay")))
                .orElse(false);
    }

    /** Resolves an authored daily event to a deterministic conflict id, or an empty string. */
    public static String authoredConflictId(String dailyEventId, String regionId) {
        String daily = normalize(dailyEventId);
        String region = normalize(regionId);
        DailyEventEffectCatalog.Event authored = DailyEventEffectCatalog.builtin().find(daily).orElse(null);
        if (authored == null || !authored.hasFactionWar() || !authored.matchesRegion(region)
                && !regionAliasMatches(authored, region)) {
            return "";
        }
        String declared = normalize(authored.factionWar());
        if (declared.isBlank() || "true".equals(declared)) {
            if ((daily.contains("mulan") || daily.contains("tianlan"))
                    && (region.contains("mulan") || region.contains("tianlan")
                    || region.contains("border"))) {
                return "mulan_tianlan_war_outbreak";
            }
            return "";
        }
        if ("false".equals(declared)) {
            return "";
        }
        if ("mulan_tianlan".equals(declared)) {
            return "mulan_tianlan_war_outbreak";
        }
        if (BUILTIN.events().containsKey(declared)) {
            return declared;
        }
        return BUILTIN.events().values().stream()
                .filter(event -> event.id().contains(declared) || declared.contains(event.id()))
                .map(ConflictEvent::id)
                .findFirst()
                .orElse("");
    }

    /** Resolves a daily id while keeping authored definitions ahead of legacy heuristics. */
    static Optional<ConflictEvent> resolveConflict(String regionId, String dailyEventId) {
        String region = normalize(regionId);
        String daily = normalize(dailyEventId);
        if (daily.isBlank()) {
            return Optional.empty();
        }
        if ("mulan_tianlan_campaign".equals(daily)) {
            ConflictEvent canonical = BUILTIN.events().get("mulan_tianlan_war_outbreak");
            return canonical != null && canonical.matchesRegion(region)
                    ? Optional.of(canonical)
                    : Optional.empty();
        }
        DailyEventEffectCatalog.Event authored = DailyEventEffectCatalog.builtin().find(daily).orElse(null);
        if (authored != null) {
            if (!authored.hasFactionWar()) {
                return Optional.empty();
            }
            return Optional.ofNullable(BUILTIN.events().get(authoredConflictId(daily, region)));
        }
        return Optional.ofNullable(matchConflict(region, daily));
    }

    private static ConflictEvent matchConflict(String region, String dailyEventId) {
        if (BUILTIN.events().isEmpty() || dailyEventId.isBlank()) {
            return null;
        }
        // 1) direct id hit
        ConflictEvent direct = BUILTIN.events().get(dailyEventId);
        if (direct != null) {
            return direct.matchesRegion(region) ? direct : null;
        }
        // 2) trigger token overlap with daily event id
        List<ConflictEvent> candidates = new ArrayList<>();
        for (ConflictEvent event : BUILTIN.events().values()) {
            if (!event.matchesRegion(region)) {
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

    private static void applyEffects(ServerPlayer player, ConflictEvent event, CompoundTag root, long gameTime,
                                     long rollUntilTick, String region, String phase, boolean grantReputation) {
        root.putString(ACTIVE_ID, event.id());
        root.putString(ACTIVE_REGION, normalize(region));
        if (phase == null || phase.isBlank()) {
            root.remove(ACTIVE_PHASE);
        } else {
            root.putString(ACTIVE_PHASE, normalize(phase));
        }
        root.putLong(ACTIVE_UNTIL, rollUntilTick > gameTime ? rollUntilTick : gameTime + 24000L);

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
        if (grantReputation) {
            if (factions.size() >= 2) {
                // Player leaning: small gain with first faction, loss with second (conflict pressure).
                ReputationService.add(player, ReputationUnlockService.reputationKey(factions.get(0)), repShift);
                ReputationService.add(player, ReputationUnlockService.reputationKey(factions.get(1)), -Math.max(1, repShift / 2));
                ReputationService.add(player, normalize(factions.get(0)), repShift);
                ReputationService.add(player, normalize(factions.get(1)), -Math.max(1, repShift / 2));
            } else if (factions.size() == 1) {
                ReputationService.add(player, ReputationUnlockService.reputationKey(factions.get(0)), repShift);
            }
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
                long warUntil = rollUntilTick > gameTime ? rollUntilTick : gameTime + 10L * 60L * 20L;
                SectWarService.ensureStarted(player.server, factions.get(0), factions.get(1), warUntil,
                        region, player.level().dimension().location().toString());
            } catch (Exception ignored) {
                // war service optional; conflict rep/price still apply
            }
        }
    }

    private static void maybeExpire(ServerPlayer player, CompoundTag root) {
        long until = root.getLong(ACTIVE_UNTIL);
        if (until > 0L && player.level().getGameTime() >= until) {
            clearActive(root);
        }
    }

    private static boolean isSameActiveConflict(CompoundTag root, ConflictEvent event,
                                                String region, long gameTime) {
        return event.id().equals(normalize(root.getString(ACTIVE_ID)))
                && normalize(region).equals(normalize(root.getString(ACTIVE_REGION)))
                && root.getLong(ACTIVE_UNTIL) > gameTime;
    }

    static String dailyClaimKey(String region, String event, long until) {
        return normalize(region) + "|" + normalize(event) + "|" + Math.max(0L, until);
    }

    static boolean hasDailyClaim(CompoundTag root, String key, long gameTime) {
        migrateLegacyDailyClaim(root, key, gameTime);
        return readDailyClaims(root, gameTime).contains(key == null ? "" : key);
    }

    static void recordDailyClaim(CompoundTag root, String key, long gameTime) {
        String normalizedKey = key == null ? "" : key.trim();
        if (!isCanonicalClaim(normalizedKey) || isExpiredClaim(normalizedKey, gameTime)) {
            return;
        }
        migrateLegacyDailyClaim(root, normalizedKey, gameTime);
        List<String> claims = new ArrayList<>(readDailyClaims(root, gameTime));
        claims.remove(normalizedKey);
        claims.add(normalizedKey);
        writeDailyClaims(root, claims);
        root.putString(LAST_DAILY, normalizedKey);
    }

    static void removeDailyClaim(CompoundTag root, String key, long gameTime) {
        String normalizedKey = key == null ? "" : key.trim();
        if (normalizedKey.isBlank()) {
            return;
        }
        migrateLegacyDailyClaim(root, normalizedKey, gameTime);
        List<String> claims = new ArrayList<>(readDailyClaims(root, gameTime));
        claims.remove(normalizedKey);
        writeDailyClaims(root, claims);
        if (normalizedKey.equals(root.getString(LAST_DAILY))) {
            root.remove(LAST_DAILY);
        }
    }

    static int dailyClaimCount(CompoundTag root, long gameTime) {
        return readDailyClaims(root, gameTime).size();
    }

    private static List<String> readDailyClaims(CompoundTag root, long gameTime) {
        LinkedHashSet<String> claims = new LinkedHashSet<>();
        ListTag stored = root.getList(DAILY_CLAIMS, Tag.TAG_STRING);
        for (int i = 0; i < stored.size(); i++) {
            String claim = stored.getString(i);
            if (isCanonicalClaim(claim) && !isExpiredClaim(claim, gameTime)) {
                claims.add(claim);
            }
        }
        String legacy = root.getString(LAST_DAILY);
        if (isCanonicalClaim(legacy) && !isExpiredClaim(legacy, gameTime)) {
            claims.add(legacy);
        } else if (!legacy.isBlank()) {
            root.remove(LAST_DAILY);
        }
        List<String> bounded = newestClaims(claims);
        writeDailyClaims(root, bounded);
        return bounded;
    }

    private static List<String> newestClaims(LinkedHashSet<String> claims) {
        List<String> ordered = new ArrayList<>(claims);
        int from = Math.max(0, ordered.size() - MAX_DAILY_CLAIMS);
        return List.copyOf(ordered.subList(from, ordered.size()));
    }

    private static void writeDailyClaims(CompoundTag root, List<String> claims) {
        if (claims == null || claims.isEmpty()) {
            root.remove(DAILY_CLAIMS);
            return;
        }
        ListTag stored = new ListTag();
        int from = Math.max(0, claims.size() - MAX_DAILY_CLAIMS);
        for (int i = from; i < claims.size(); i++) {
            String claim = claims.get(i);
            if (claim != null && !claim.isBlank()) {
                stored.add(StringTag.valueOf(claim));
            }
        }
        root.put(DAILY_CLAIMS, stored);
    }

    private static boolean isExpiredClaim(String claim, long gameTime) {
        int separator = claim == null ? -1 : claim.lastIndexOf('|');
        if (separator < 0 || separator >= claim.length() - 1) {
            return true;
        }
        try {
            long until = Long.parseLong(claim.substring(separator + 1));
            return until <= 0L || gameTime >= until;
        } catch (NumberFormatException ignored) {
            return true;
        }
    }

    private static void migrateLegacyDailyClaim(CompoundTag root, String requestedKey, long gameTime) {
        if (root == null || requestedKey == null || !isCanonicalClaim(requestedKey)) {
            return;
        }
        String legacy = root.getString(LAST_DAILY).trim();
        if (legacy.isBlank() || isCanonicalClaim(legacy)) {
            return;
        }
        root.remove(LAST_DAILY);
        if (!normalize(legacy).equals(claimEvent(requestedKey)) || isExpiredClaim(requestedKey, gameTime)) {
            return;
        }
        List<String> claims = new ArrayList<>(readDailyClaims(root, gameTime));
        claims.remove(requestedKey);
        claims.add(requestedKey);
        writeDailyClaims(root, claims);
        root.putString(LAST_DAILY, requestedKey);
    }

    private static boolean isCanonicalClaim(String claim) {
        if (claim == null) {
            return false;
        }
        int first = claim.indexOf('|');
        int last = claim.lastIndexOf('|');
        if (first < 0 || last <= first || last >= claim.length() - 1) {
            return false;
        }
        try {
            return Long.parseLong(claim.substring(last + 1)) > 0L;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String claimEvent(String claim) {
        int first = claim == null ? -1 : claim.indexOf('|');
        int last = claim == null ? -1 : claim.lastIndexOf('|');
        return first >= 0 && last > first ? normalize(claim.substring(first + 1, last)) : "";
    }

    static long fallbackClaimUntil(long gameTime) {
        long normalizedTime = Math.max(0L, gameTime);
        long dayStart = normalizedTime - normalizedTime % 24000L;
        return dayStart > Long.MAX_VALUE - 24000L ? Long.MAX_VALUE : dayStart + 24000L;
    }

    private static Optional<ConflictEvent> activeConflict(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        long until = root.getLong(ACTIVE_UNTIL);
        if (until <= 0L || player.level().getGameTime() >= until) {
            return Optional.empty();
        }
        String activeRegion = normalize(root.getString(ACTIVE_REGION));
        String currentRegion = CultivationHelper.get(player)
                .map(cultivation -> normalize(cultivation.getWorldpackCurrentRegionId()))
                .orElse("");
        if (!activeRegion.isBlank() && !currentRegion.isBlank()
                && !activeRegion.equals(currentRegion)) {
            return Optional.empty();
        }
        return Optional.ofNullable(BUILTIN.events().get(normalize(root.getString(ACTIVE_ID))))
                .filter(event -> isRealmAllowed(player, event));
    }

    private static boolean isRealmAllowed(ServerPlayer player, ConflictEvent event) {
        if (event == null || event.realmMin().isBlank()) {
            return true;
        }
        return CultivationHelper.get(player)
                .map(cultivation -> com.xunxian.seekingimmortals.worldpack.DailyEventEffectExecutor
                        .meetsRealmMinimum(cultivation.getRealm(), event.realmMin()))
                .orElse(false);
    }

    private static boolean regionAliasMatches(DailyEventEffectCatalog.Event event, String region) {
        if (event == null || region == null || region.isBlank()) {
            return false;
        }
        for (String authored : event.regions()) {
            if (("tiannan_border".equals(authored)
                    && ("tiannan".equals(region) || "wutu_border".equals(region)))
                    || (("mulan".equals(authored) || "mulan_grassland".equals(authored))
                    && ("mulan".equals(region) || "mulan_grassland".equals(region)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean rawBoolean(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim();
        return "true".equalsIgnoreCase(normalized) || "\"true\"".equalsIgnoreCase(normalized);
    }

    private static void clearActive(CompoundTag root) {
        root.remove(ACTIVE_ID);
        root.remove(ACTIVE_REGION);
        root.remove(ACTIVE_PHASE);
        root.remove(ACTIVE_UNTIL);
        root.remove(PRICE_MOD);
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
            if (factions.isEmpty()) {
                factions = loadWarFactions(str(o, "war_ref"));
            }
            List<String> triggers = stringList(o.get("triggers"));
            String region = normalize(str(o, "region"));
            List<String> regions = stringList(o.get("regions"));
            String realmMin = normalize(str(o, "realm_min"));
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
                    region,
                    regions,
                    realmMin,
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

    private static List<String> loadWarFactions(String warRef) {
        String relative = warRef == null ? "" : warRef.trim();
        int fragment = relative.indexOf('#');
        if (fragment >= 0) {
            relative = relative.substring(0, fragment);
        }
        if (relative.isBlank() || !relative.endsWith(".json") || relative.startsWith("/")
                || relative.contains("..") || relative.contains("\\") || relative.contains(":")) {
            return List.of();
        }
        JsonObject war = readJson(
                "data/" + SeekingImmortalsMod.MODID + "/text_material/" + relative);
        if (war == null || !war.has("faction_sides") || !war.get("faction_sides").isJsonArray()) {
            return List.of();
        }
        LinkedHashSet<String> factions = new LinkedHashSet<>();
        for (JsonElement element : war.getAsJsonArray("faction_sides")) {
            if (!element.isJsonObject()) {
                continue;
            }
            String id = normalize(str(element.getAsJsonObject(), "id"));
            if (!id.isBlank()) {
                factions.add(id);
            }
        }
        return factions.size() >= 2 ? List.copyOf(factions) : List.of();
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
                                List<String> regions, String realmMin, List<String> triggers, int reputationShift,
                                double taxIncrease, boolean blockade, double duelChance,
                                Map<String, String> rawEffects) {
        public ConflictEvent {
            id = normalize(id);
            display = display == null ? "" : display;
            factions = factions == null ? List.of() : List.copyOf(factions);
            type = normalize(type);
            String singular = normalize(region);
            List<String> normalizedRegions = new ArrayList<>();
            if (regions != null) {
                for (String candidate : regions) {
                    String normalized = normalize(candidate);
                    if (!normalized.isBlank() && !normalizedRegions.contains(normalized)) {
                        normalizedRegions.add(normalized);
                    }
                }
            }
            if (!singular.isBlank() && !normalizedRegions.contains(singular)) {
                normalizedRegions.add(0, singular);
            }
            if (singular.isBlank() && !normalizedRegions.isEmpty()) {
                singular = normalizedRegions.get(0);
            }
            region = singular;
            regions = List.copyOf(normalizedRegions);
            realmMin = normalize(realmMin);
            triggers = triggers == null ? List.of() : List.copyOf(triggers);
            rawEffects = rawEffects == null ? Map.of() : Map.copyOf(rawEffects);
        }

        public ConflictEvent(String id, String display, List<String> factions, String type, String region,
                             List<String> triggers, int reputationShift, double taxIncrease, boolean blockade,
                             double duelChance, Map<String, String> rawEffects) {
            this(id, display, factions, type, region, List.of(), "", triggers, reputationShift,
                    taxIncrease, blockade, duelChance, rawEffects);
        }

        public boolean matchesRegion(String regionId) {
            String wanted = normalize(regionId);
            if (wanted.isBlank() || regions.isEmpty()) {
                return true;
            }
            return regions.stream().anyMatch(candidate -> wanted.equals(candidate)
                    || wanted.contains(candidate) || candidate.contains(wanted)
                    || "*".equals(candidate) || "any".equals(candidate));
        }
    }

    public record Snapshot(Map<String, ConflictEvent> events) {
        public int count() {
            return events.size();
        }
    }
}
