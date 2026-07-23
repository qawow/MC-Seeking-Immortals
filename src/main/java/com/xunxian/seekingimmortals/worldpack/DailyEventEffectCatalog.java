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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable, lossless index of authored daily-event semantics.
 *
 * <p>The worldpack event format predates the text-material event format and only
 * exposes a flat list of effect ids. This catalog keeps the authored channels
 * separate while offering that flat list as a compatibility projection.</p>
 */
public final class DailyEventEffectCatalog {
    private static final String DAILY_RESOURCE =
            "data/" + SeekingImmortalsMod.MODID + "/text_material/daily_random_events.json";
    private static final String TIANYUAN_RESOURCE =
            "data/" + SeekingImmortalsMod.MODID + "/text_material/tianyuan_daily_events.json";

    private static final Set<String> DESCRIPTIVE_FIELDS = Set.of(
            "id", "display", "lore", "setting", "learn_requirements");

    private static final Set<String> EXECUTED_TOKENS = Set.of(
            "cultivation_speed_1.1_1day",
            "cultivation_speed_1.2_3day",
            "cultivation_buff_minor",
            "cultivation_buff_tag",
            "breakthrough_chance_small",
            "contribution_gain_1.5_1day",
            "herb_shop_price_x1.3",
            "shop_herb_discount",
            "ferry_cost_double",
            "ferry_delay",
            "movement_debuff_outdoor",
            "movement_debuff",
            "demon_qi_tick",
            "yin_damage_ambient",
            "spawn_beast_wave",
            "spawn_elite",
            "random_ambush_low",
            "sea_spawn_boost",
            "auction_active",
            "auction_preview",
            "quest_hint_blood_forbidden",
            "quest_hint",
            "clan_quest_offer",
            "faction_conflict_minor",
            "herb_growth_boost",
            "tribulation_pressure",
            "tribulation_prep_event",
            "tax_mult",
            "spawn",
            "yin_wraith",
            "spawn_multiplier",
            "combat_tier",
            "cost_yin_stone",
            "quest_hook",
            "faction_trigger",
            "realm_min",
            "duration_days",
            "bu_tian_pill",
            "high_herb",
            "huangfeng_entry",
            "treasure_fair_invite",
            "auction_notice");

    private static final Snapshot BUILTIN = loadBuiltin();

    private DailyEventEffectCatalog() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    /** Parses one daily-event root for focused tests and tooling. */
    public static Snapshot parseForTest(Reader reader) {
        return parseRoots(List.of(new RootInput("test", JsonParser.parseReader(reader).getAsJsonObject(), "")));
    }

    /** Parses the Tianyuan variant for focused tests and tooling. */
    public static Snapshot parseTianyuanForTest(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        return parseRoots(List.of(new RootInput("tianyuan", root, string(root, "region"))));
    }

    private static Snapshot loadBuiltin() {
        List<RootInput> roots = new ArrayList<>();
        JsonObject daily = readJson(DAILY_RESOURCE);
        if (daily != null) {
            roots.add(new RootInput("daily_random_events", daily, ""));
        }
        JsonObject tianyuan = readJson(TIANYUAN_RESOURCE);
        if (tianyuan != null) {
            roots.add(new RootInput("tianyuan_daily_events", tianyuan, string(tianyuan, "region")));
        }
        return parseRoots(roots);
    }

    private static Snapshot parseRoots(List<RootInput> roots) {
        Map<String, Event> events = new LinkedHashMap<>();
        Map<String, String> rootFields = new LinkedHashMap<>();
        int authoredEventCount = 0;
        for (RootInput input : roots == null ? List.<RootInput>of() : roots) {
            if (input == null || input.root() == null) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : input.root().entrySet()) {
                rootFields.putIfAbsent(input.source() + "." + entry.getKey(), canonical(entry.getValue()));
            }
            for (JsonElement element : array(input.root(), "events")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                Event event = parseEvent(input.source(), input.defaultRegion(), element.getAsJsonObject());
                if (event == null || event.id().isBlank()) {
                    continue;
                }
                authoredEventCount++;
                // The first authored definition is authoritative if a compatibility file repeats an id.
                events.putIfAbsent(event.id(), event);
            }
        }
        return new Snapshot(events, authoredEventCount, rootFields);
    }

    private static Event parseEvent(String source, String defaultRegion, JsonObject object) {
        String id = normalize(string(object, "id"));
        if (id.isBlank()) {
            return null;
        }

        List<String> regions = new ArrayList<>();
        regions.addAll(strings(object, "regions"));
        String singularRegion = string(object, "region");
        if (!singularRegion.isBlank()) {
            regions.add(singularRegion);
        }
        if (regions.isEmpty() && defaultRegion != null && !defaultRegion.isBlank()) {
            regions.add(defaultRegion);
        }
        regions = normalizeDistinct(regions);

        List<EffectToken> tokens = new ArrayList<>();
        addScalarToken(tokens, object, "effect", Source.EFFECT);
        addScalarToken(tokens, object, "buff", Source.BUFF);
        addScalarToken(tokens, object, "debuff", Source.DEBUFF);
        addArrayTokens(tokens, object, "effects", Source.EFFECTS);
        addObjectTokens(tokens, object, "effects", Source.EFFECT_OBJECT);
        addArrayTokens(tokens, object, "hooks", Source.HOOK);
        addScalarToken(tokens, object, "spawn", Source.SPAWN);

        LinkedHashSet<String> legacyEffects = new LinkedHashSet<>();
        for (EffectToken token : tokens) {
            if (!token.token().isBlank()) {
                legacyEffects.add(token.token());
            }
        }

        Map<String, String> rawFields = new LinkedHashMap<>();
        List<AuthoredField> authoredFields = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = normalize(entry.getKey());
            String value = canonical(entry.getValue());
            rawFields.put(key, value);
            authoredFields.add(new AuthoredField(key, value, coverageForField(key, entry.getValue(), tokens)));
        }

        Map<String, String> objectEffects = new LinkedHashMap<>();
        JsonElement effectElement = object.get("effects");
        if (effectElement != null && effectElement.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : effectElement.getAsJsonObject().entrySet()) {
                objectEffects.put(normalize(entry.getKey()), canonical(entry.getValue()));
            }
        }

        List<String> rewards = strings(object, "rewards");
        String rewardsTag = string(object, "rewards_tag");
        int costYinStone = nonNegativeInt(object, "cost_yin_stone", 0);
        int combatTier = nonNegativeInt(object, "combat_tier", 0);
        double spawnMultiplier = positiveDouble(object, "spawn_multiplier", 1.0D);
        double weight = nonNegativeDouble(object, "weight", 1.0D);
        int durationTicks = durationTicks(object, objectEffects);

        String realmMin = string(object, "realm_min");
        if (realmMin.isBlank()) {
            realmMin = nestedString(object, "learn_requirements", "trigger", "realm_min");
        }

        return new Event(
                source,
                id,
                regions,
                string(object, "display"),
                weight,
                durationTicks,
                List.copyOf(legacyEffects),
                tokens,
                objectEffects,
                rewards,
                rewardsTag,
                costYinStone,
                combatTier,
                spawnMultiplier,
                string(object, "spawn"),
                string(object, "buff"),
                string(object, "debuff"),
                string(object, "quest_hook"),
                string(object, "faction_trigger"),
                realmMin,
                rawFields,
                authoredFields);
    }

    private static int durationTicks(JsonObject object, Map<String, String> objectEffects) {
        double days = positiveDouble(object, "duration_days", 0.0D);
        if (days <= 0.0D && objectEffects.containsKey("duration_days")) {
            days = parsePositiveDouble(objectEffects.get("duration_days"));
        }
        if (days > 0.0D) {
            return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, Math.round(days * 24000.0D)));
        }
        int explicit = nonNegativeInt(object, "duration_ticks", 0);
        return explicit > 0 ? explicit : 24000;
    }

    private static Coverage coverageForField(String field, JsonElement value, List<EffectToken> tokens) {
        if (DESCRIPTIVE_FIELDS.contains(field)) {
            return Coverage.DESCRIPTIVE;
        }
        if ("effects".equals(field) || "effect".equals(field) || "buff".equals(field)
                || "debuff".equals(field) || "hooks".equals(field) || "spawn".equals(field)) {
            boolean allKnown = tokens.stream().filter(token -> token.source().field().equals(field))
                    .allMatch(token -> token.coverage() == Coverage.EXECUTED);
            return allKnown ? Coverage.EXECUTED : Coverage.PRESERVED;
        }
        return switch (field) {
            case "region", "regions", "weight", "duration_ticks", "duration_days",
                    "spawn_multiplier", "combat_tier", "cost_yin_stone",
                    "quest_hook", "faction_trigger", "realm_min", "faction_war" -> Coverage.EXECUTED;
            case "war_phase" -> Coverage.PRESERVED;
            default -> Coverage.PRESERVED;
        };
    }

    private static void addScalarToken(List<EffectToken> target, JsonObject object, String field, Source source) {
        JsonElement element = object.get(field);
        if (element != null && element.isJsonPrimitive()) {
            String token = normalize(element.getAsString());
            if (!token.isBlank()) {
                target.add(new EffectToken(token, canonical(element), source, coverageForToken(token)));
            }
        }
    }

    private static void addArrayTokens(List<EffectToken> target, JsonObject object, String field, Source source) {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonArray()) {
            return;
        }
        for (JsonElement child : element.getAsJsonArray()) {
            if (child.isJsonPrimitive()) {
                String token = normalize(child.getAsString());
                if (!token.isBlank()) {
                    target.add(new EffectToken(token, canonical(child), source, coverageForToken(token)));
                }
            }
        }
    }

    private static void addObjectTokens(List<EffectToken> target, JsonObject object, String field, Source source) {
        JsonElement element = object.get(field);
        if (element == null || !element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String token = normalize(entry.getKey());
            if (!token.isBlank()) {
                target.add(new EffectToken(token, canonical(entry.getValue()), source, coverageForToken(token)));
            }
        }
    }

    private static Coverage coverageForToken(String token) {
        return EXECUTED_TOKENS.contains(normalize(token)) ? Coverage.EXECUTED : Coverage.PRESERVED;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = DailyEventEffectCatalog.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonElement parsed = JsonParser.parseReader(reader);
                return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load daily event effect catalog {}", path, exception);
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
                String value = normalize(element.getAsString());
                if (!value.isBlank()) {
                    values.add(value);
                }
            }
        }
        return List.copyOf(values);
    }

    private static List<String> normalizeDistinct(List<String> values) {
        LinkedHashSet<String> distinct = new LinkedHashSet<>();
        for (String value : values == null ? List.<String>of() : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                distinct.add(normalized);
            }
        }
        return List.copyOf(distinct);
    }

    private static String string(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString().trim() : "";
    }

    private static String nestedString(JsonObject object, String... path) {
        JsonElement current = object;
        for (String key : path) {
            if (current == null || !current.isJsonObject() || !current.getAsJsonObject().has(key)) {
                return "";
            }
            current = current.getAsJsonObject().get(key);
        }
        return current != null && current.isJsonPrimitive() ? current.getAsString().trim() : "";
    }

    private static int nonNegativeInt(JsonObject object, String key, int fallback) {
        double value = nonNegativeDouble(object, key, fallback);
        return (int) Math.min(Integer.MAX_VALUE, Math.round(value));
    }

    private static double positiveDouble(JsonObject object, String key, double fallback) {
        double value = nonNegativeDouble(object, key, fallback);
        return value > 0.0D ? value : fallback;
    }

    private static double nonNegativeDouble(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            double value = object.get(key).getAsDouble();
            return Double.isFinite(value) && value >= 0.0D ? value : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static double parsePositiveDouble(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed > 0.0D ? parsed : 0.0D;
        } catch (Exception ignored) {
            return 0.0D;
        }
    }

    private static String canonical(JsonElement element) {
        return element == null || element.isJsonNull() ? "null" : element.toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record RootInput(String source, JsonObject root, String defaultRegion) {}

    public enum Coverage {
        EXECUTED,
        DESCRIPTIVE,
        PRESERVED
    }

    public enum Source {
        EFFECT("effect"),
        BUFF("buff"),
        DEBUFF("debuff"),
        EFFECTS("effects"),
        EFFECT_OBJECT("effects"),
        HOOK("hooks"),
        SPAWN("spawn");

        private final String field;

        Source(String field) {
            this.field = field;
        }

        public String field() {
            return field;
        }
    }

    public record EffectToken(String token, String value, Source source, Coverage coverage) {
        public EffectToken {
            token = normalize(token);
            value = value == null ? "" : value;
            source = source == null ? Source.EFFECTS : source;
            coverage = coverage == null ? Coverage.PRESERVED : coverage;
        }
    }

    public record AuthoredField(String field, String value, Coverage coverage) {
        public AuthoredField {
            field = normalize(field);
            value = value == null ? "" : value;
            coverage = coverage == null ? Coverage.PRESERVED : coverage;
        }
    }

    public record Event(String source, String id, List<String> regions, String display, double weight,
                        int durationTicks, List<String> legacyEffects, List<EffectToken> tokens,
                        Map<String, String> objectEffects, List<String> rewards, String rewardsTag,
                        int costYinStone, int combatTier, double spawnMultiplier, String spawn,
                        String buff, String debuff, String questHook, String factionTrigger,
                        String realmMin, Map<String, String> rawFields, List<AuthoredField> authoredFields) {
        public Event {
            source = source == null ? "" : source;
            id = normalize(id);
            regions = Collections.unmodifiableList(new ArrayList<>(regions == null ? List.of() : regions));
            display = display == null ? "" : display;
            weight = Double.isFinite(weight) && weight >= 0.0D ? weight : 1.0D;
            durationTicks = Math.max(1, durationTicks);
            legacyEffects = Collections.unmodifiableList(new ArrayList<>(legacyEffects == null ? List.of() : legacyEffects));
            tokens = Collections.unmodifiableList(new ArrayList<>(tokens == null ? List.of() : tokens));
            objectEffects = immutableMap(objectEffects);
            rewards = Collections.unmodifiableList(new ArrayList<>(rewards == null ? List.of() : rewards));
            rewardsTag = rewardsTag == null ? "" : normalize(rewardsTag);
            costYinStone = Math.max(0, costYinStone);
            combatTier = Math.max(0, combatTier);
            spawnMultiplier = Double.isFinite(spawnMultiplier) && spawnMultiplier > 0.0D ? spawnMultiplier : 1.0D;
            spawn = spawn == null ? "" : normalize(spawn);
            buff = buff == null ? "" : normalize(buff);
            debuff = debuff == null ? "" : normalize(debuff);
            questHook = questHook == null ? "" : normalize(questHook);
            factionTrigger = factionTrigger == null ? "" : normalize(factionTrigger);
            realmMin = realmMin == null ? "" : normalize(realmMin);
            rawFields = immutableMap(rawFields);
            authoredFields = Collections.unmodifiableList(new ArrayList<>(authoredFields == null ? List.of() : authoredFields));
        }

        public boolean hasToken(String token) {
            String wanted = normalize(token);
            return tokens.stream().anyMatch(effect -> wanted.equals(effect.token()));
        }

        public Optional<String> tokenValue(String token) {
            String wanted = normalize(token);
            return tokens.stream().filter(effect -> wanted.equals(effect.token()))
                    .map(EffectToken::value).findFirst();
        }

        public List<String> unknownTokens() {
            return tokens.stream().filter(effect -> effect.coverage() == Coverage.PRESERVED)
                    .map(EffectToken::token).distinct().toList();
        }

        public int legacyWeight() {
            return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.round(weight)));
        }

        /** Converts authored decimal weights to a bounded fixed-point integer. */
        public int scaledWeight(int scale) {
            int safeScale = Math.max(1, scale);
            double scaled = weight * safeScale;
            if (!Double.isFinite(scaled)) {
                return safeScale;
            }
            return Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.round(scaled)));
        }

        /** Returns a scalar authored field without its JSON string quoting. */
        public String authoredValue(String field) {
            String encoded = rawFields.get(normalize(field));
            if (encoded == null || encoded.isBlank()) {
                return "";
            }
            try {
                JsonElement parsed = JsonParser.parseString(encoded);
                return parsed.isJsonPrimitive() ? parsed.getAsString().trim() : encoded;
            } catch (Exception ignored) {
                return encoded.trim();
            }
        }

        public String factionWar() {
            return authoredValue("faction_war");
        }

        public String warPhase() {
            return authoredValue("war_phase");
        }

        public boolean hasFactionWar() {
            String value = factionWar();
            return "true".equalsIgnoreCase(value) || (!value.isBlank() && !"false".equalsIgnoreCase(value));
        }

        public boolean definesContributionReward() {
            return hasToken("contribution_gain_1.5_1day")
                    || rawFields.containsKey("contribution_multiplier");
        }

        public boolean definesMeritReward() {
            return hasToken("merit_mult_2") || rawFields.containsKey("merit_multiplier");
        }

        public double authoredNumber(String field, double fallback) {
            String value = authoredValue(field);
            if (value.isBlank()) {
                return fallback;
            }
            try {
                double parsed = Double.parseDouble(value);
                return Double.isFinite(parsed) ? parsed : fallback;
            } catch (Exception ignored) {
                return fallback;
            }
        }

        public boolean matchesRegion(String regionId) {
            String wanted = normalize(regionId);
            for (String region : regions) {
                if ("*".equals(region) || "any".equals(region) || region.equals(wanted)) {
                    return true;
                }
            }
            return false;
        }
    }

    public record Snapshot(Map<String, Event> events, int authoredEventCount, Map<String, String> rootFields) {
        public Snapshot {
            events = immutableMap(events);
            authoredEventCount = Math.max(0, authoredEventCount);
            rootFields = immutableMap(rootFields);
        }

        public Optional<Event> find(String id) {
            return Optional.ofNullable(events.get(normalize(id)));
        }

        public List<Event> list() {
            return List.copyOf(events.values());
        }

        public int count() {
            return events.size();
        }
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
