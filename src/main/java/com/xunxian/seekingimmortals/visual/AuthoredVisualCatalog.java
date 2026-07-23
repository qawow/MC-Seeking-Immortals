package com.xunxian.seekingimmortals.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Typed runtime view of the generated authored visual catalog.
 *
 * <p>The three older VFX catalogs remain independent projections. This loader
 * is deliberately read-only and only consumes the unified resource, so adding
 * a field or a profile cannot alter an existing packet or renderer facade.</p>
 */
public final class AuthoredVisualCatalog {
    private static final String RESOURCE =
            "data/seeking_immortals/visual/authored_visual_catalog.json";
    private static final Set<String> PARTICLES = Set.of(
            "qi_soft", "fire_ember", "water_mist", "wood_pollen", "metal_spark", "earth_dust",
            "thunder_arc", "yin_smoke", "soul_wisps", "blood_mist", "heal_motes", "space_glitch");
    private static final Set<String> TRAILS = Set.of(
            "none", "sword_thin", "heavy_weapon", "flying_sword_orbit", "talisman_ash",
            "blood_ribbon", "thunder_jagged", "soul_afterimage", "movement_wind");
    private static final Set<String> ANCHORS = Set.of("CASTER", "ITEM", "PATH", "SCREEN", "TARGET");
    private static final Set<String> TARGETS = Set.of("NONE", "TARGET");
    private static final Set<String> CONDITIONS = Set.of("ALWAYS");
    private static final Comparator<VisualTimelineEvent> TIMELINE_ORDER =
            Comparator.comparingInt(VisualTimelineEvent::startTick)
                    .thenComparingInt(VisualTimelineEvent::ordinal);
    private static final Snapshot BUILTIN = loadBuiltin();

    private AuthoredVisualCatalog() {}

    /** Returns the immutable built-in snapshot loaded from the shipped resource. */
    public static Snapshot builtin() {
        return BUILTIN;
    }

    /** Convenience projection for callers that do not need diagnostics. */
    public static Map<String, VisualProfile> profiles() {
        return BUILTIN.profiles();
    }

    public static Optional<VisualProfile> find(String key) {
        return BUILTIN.find(key);
    }

    public static Optional<VisualProfile> find(VisualDomain domain, String id) {
        return BUILTIN.find(domain, id);
    }

    public static Optional<VisualProfile> resolve(String key) {
        return BUILTIN.resolve(key);
    }

    public static Optional<VisualProfile> resolve(VisualDomain domain, String id) {
        return BUILTIN.resolve(domain, id);
    }

    public static Map<String, String> aliases() {
        return BUILTIN.aliases();
    }

    public static Map<String, String> sourceHashes() {
        return BUILTIN.sourceHashes();
    }

    public static List<String> invalidRows() {
        return BUILTIN.invalidRows();
    }

    public static Map<String, Map<String, String>> collisionResolutions() {
        return BUILTIN.collisionResolutions();
    }

    /** Public test/tooling entry point; production code uses {@link #builtin()}. */
    public static Snapshot parseForTest(Reader reader) {
        return load(reader);
    }

    /* Package-private by design: focused tests can inject malformed rows without
       making arbitrary runtime resources part of the public API. */
    static Snapshot load(Reader reader) {
        List<String> invalid = new ArrayList<>();
        if (reader == null) {
            invalid.add("root: null reader");
            return Snapshot.empty(invalid);
        }
        JsonElement parsed;
        try {
            parsed = JsonParser.parseReader(reader);
        } catch (RuntimeException exception) {
            invalid.add("root: " + message(exception));
            return Snapshot.empty(invalid);
        }
        if (parsed == null || !parsed.isJsonObject()) {
            invalid.add("root: expected object");
            return Snapshot.empty(invalid);
        }
        return parseRoot(parsed.getAsJsonObject(), invalid);
    }

    private static Snapshot loadBuiltin() {
        try (InputStream stream = AuthoredVisualCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return Snapshot.empty(List.of("root: missing resource " + RESOURCE));
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return load(reader);
            }
        } catch (Exception exception) {
            return Snapshot.empty(List.of("root: " + message(exception)));
        }
    }

    private static Snapshot parseRoot(JsonObject root, List<String> invalid) {
        Map<String, VisualProfile> profiles = new LinkedHashMap<>();
        JsonElement profileElement = root.get("profiles");
        if (profileElement == null || !profileElement.isJsonArray()) {
            invalid.add("profiles: expected array");
        } else {
            JsonArray profileArray = profileElement.getAsJsonArray();
            for (int index = 0; index < profileArray.size(); index++) {
                JsonElement element = profileArray.get(index);
                try {
                    if (element == null || !element.isJsonObject()) {
                        throw new IllegalArgumentException("expected object");
                    }
                    VisualProfile profile = parseProfile(element.getAsJsonObject());
                    if (profiles.putIfAbsent(profile.key(), profile) != null) {
                        throw new IllegalArgumentException("duplicate key " + profile.key());
                    }
                } catch (RuntimeException exception) {
                    invalid.add("profiles[" + index + "]: " + message(exception));
                }
            }
        }

        Map<String, String> aliases = parseAliases(root.get("aliases"), invalid);
        Map<String, String> sourceHashes = parseStringMap(root.get("source_hashes"), "source_hashes", invalid);
        Map<String, Map<String, String>> collisions = parseCollisions(root.get("collision_resolutions"), invalid);
        Map<String, PaletteColor> palette = parsePalette(root.get("palette"), invalid);
        Map<VisualDomain, Integer> counts = parseCounts(root.get("counts"), invalid);
        int declaredCount = optionalInteger(root, "profile_count", profiles.size(), "root", invalid);
        int schemaVersion = optionalInteger(root, "schema_version", 0, "root", invalid);
        return new Snapshot(profiles, aliases, sourceHashes, collisions, palette, counts,
                declaredCount, schemaVersion, invalid);
    }

    private static VisualProfile parseProfile(JsonObject object) {
        String rawKey = requiredString(object, "key");
        VisualDomain domain = VisualDomain.parse(requiredString(object, "domain"));
        String id = VisualDomain.normalizeId(requiredString(object, "id"));
        String key = VisualDomain.normalizeKey(rawKey);
        String expectedKey = domain.qualify(id);
        if (!expectedKey.equals(key)) {
            throw new IllegalArgumentException("key does not match domain/id: " + rawKey);
        }

        String runtimeId = requiredString(object, "runtime_id");
        String family = requiredString(object, "family").toUpperCase(Locale.ROOT);
        String motif = requiredString(object, "motif").toUpperCase(Locale.ROOT);
        requireEnum(TechniqueVfxPalette.Family.class, family, "family");
        requireEnum(TechniqueVfxPacket.Motif.class, motif, "motif");
        String particle = normalizedRequired(object, "particle");
        String trail = normalizedRequired(object, "trail");
        if (!PARTICLES.contains(particle)) {
            throw new IllegalArgumentException("unknown particle " + particle);
        }
        if (!TRAILS.contains(trail)) {
            throw new IllegalArgumentException("unknown trail " + trail);
        }
        String paletteKey = normalizedRequired(object, "palette_key");
        long argb = requiredUnsignedLong(object, "primary_argb");
        boolean authored = requiredBoolean(object, "authored");
        boolean fallback = requiredBoolean(object, "fallback");
        boolean paletteFallback = requiredBoolean(object, "palette_fallback");
        boolean telegraphed = requiredBoolean(object, "telegraphed");
        double radius = requiredDouble(object, "radius");
        int intensity = requiredInteger(object, "intensity");
        if (radius <= 0.0D || !Double.isFinite(radius) || intensity < 1) {
            throw new IllegalArgumentException("invalid profile numeric bounds");
        }

        List<VisualTimelineEvent> timeline = parseTimeline(object.get("timeline"));
        Map<String, VisualAction> states = parseStates(object.get("states"));
        Map<String, String> stateSources = parseProfileStringMap(object.get("state_sources"),
                key + ".state_sources");
        Map<String, String> sources = parseProfileStringMap(object.get("sources"), key + ".sources");
        return new VisualProfile(
                key, domain, id, runtimeId,
                optionalString(object, "display"), authored, fallback, paletteFallback,
                family, motif, optionalString(object, "shape"), particle, trail,
                optionalString(object, "color_prose"), paletteKey, argb, telegraphed,
                radius, intensity, timeline, states, stateSources, sources);
    }

    private static List<VisualTimelineEvent> parseTimeline(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("timeline must be an array");
        }
        JsonArray array = element.getAsJsonArray();
        if (array.isEmpty()) {
            throw new IllegalArgumentException("timeline must not be empty");
        }
        List<VisualTimelineEvent> events = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            JsonElement eventElement = array.get(index);
            if (eventElement == null || !eventElement.isJsonObject()) {
                throw new IllegalArgumentException("timeline[" + index + "] must be an object");
            }
            JsonObject event = eventElement.getAsJsonObject();
            int ordinal = requiredInteger(event, "ordinal");
            VisualTrigger trigger = VisualTrigger.parse(requiredString(event, "trigger"));
            int startTick = requiredInteger(event, "start_tick");
            int durationTicks = requiredInteger(event, "duration_ticks");
            VisualAction action = VisualAction.parse(requiredString(event, "action"));
            String anchor = requiredString(event, "anchor").toUpperCase(Locale.ROOT);
            String target = requiredString(event, "target").toUpperCase(Locale.ROOT);
            String state = optionalString(event, "state").toLowerCase(Locale.ROOT);
            String particle = normalizedRequired(event, "particle");
            String trail = normalizedRequired(event, "trail");
            double radius = requiredDouble(event, "radius");
            int intensity = requiredInteger(event, "intensity");
            String condition = requiredString(event, "condition").toUpperCase(Locale.ROOT);
            if (!ANCHORS.contains(anchor) || !TARGETS.contains(target) || !CONDITIONS.contains(condition)) {
                throw new IllegalArgumentException("unknown timeline reference");
            }
            if (!PARTICLES.contains(particle) || !TRAILS.contains(trail)) {
                throw new IllegalArgumentException("unknown timeline particle/trail");
            }
            events.add(new VisualTimelineEvent(ordinal, trigger, startTick, durationTicks, action,
                    anchor, target, state, particle, trail, radius, intensity, condition,
                    optionalString(event, "source")));
        }
        events.sort(TIMELINE_ORDER);
        return List.copyOf(events);
    }

    private static Map<String, VisualAction> parseStates(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("states must be an object");
        }
        Map<String, VisualAction> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String state = VisualDomain.normalizeId(entry.getKey());
            if (state.isBlank()) {
                throw new IllegalArgumentException("blank state key");
            }
            if (entry.getValue() == null || !entry.getValue().isJsonPrimitive()
                    || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("state action must be a string");
            }
            result.put(state, VisualAction.parse(entry.getValue().getAsString()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> parseAliases(JsonElement element, List<String> invalid) {
        Map<String, String> result = new LinkedHashMap<>();
        if (element == null) {
            return Map.of();
        }
        if (!element.isJsonArray()) {
            invalid.add("aliases: expected array");
            return Map.of();
        }
        JsonArray array = element.getAsJsonArray();
        for (int index = 0; index < array.size(); index++) {
            try {
                JsonElement value = array.get(index);
                if (value == null || !value.isJsonObject()) {
                    throw new IllegalArgumentException("expected object");
                }
                JsonObject alias = value.getAsJsonObject();
                VisualDomain domain = VisualDomain.parse(requiredString(alias, "domain"));
                String source = VisualDomain.normalizeId(requiredString(alias, "alias"));
                String target = VisualDomain.normalizeId(requiredString(alias, "target"));
                String key = domain.qualify(source);
                String targetKey = domain.qualify(target);
                if (result.putIfAbsent(key, targetKey) != null) {
                    throw new IllegalArgumentException("duplicate alias " + key);
                }
            } catch (RuntimeException exception) {
                invalid.add("aliases[" + index + "]: " + message(exception));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> parseStringMap(JsonElement element, String label, List<String> invalid) {
        if (element == null) {
            return Map.of();
        }
        if (!element.isJsonObject()) {
            invalid.add(label + ": expected object");
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                invalid.add(label + "." + entry.getKey() + ": expected string");
                continue;
            }
            result.put(VisualDomain.normalizeId(entry.getKey()), value.getAsString());
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, String> parseProfileStringMap(JsonElement element, String label) {
        List<String> invalid = new ArrayList<>();
        Map<String, String> result = parseStringMap(element, label, invalid);
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException(invalid.get(0));
        }
        return result;
    }

    private static Map<String, Map<String, String>> parseCollisions(JsonElement element, List<String> invalid) {
        if (element == null) {
            return Map.of();
        }
        if (!element.isJsonObject()) {
            invalid.add("collision_resolutions: expected object");
            return Map.of();
        }
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> domain : element.getAsJsonObject().entrySet()) {
            if (!domain.getValue().isJsonObject()) {
                invalid.add("collision_resolutions." + domain.getKey() + ": expected object");
                continue;
            }
            Map<String, String> entries = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> collision : domain.getValue().getAsJsonObject().entrySet()) {
                entries.put(VisualDomain.normalizeId(collision.getKey()), collision.getValue().toString());
            }
            result.put(domain.getKey().toUpperCase(Locale.ROOT), Collections.unmodifiableMap(entries));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, PaletteColor> parsePalette(JsonElement element, List<String> invalid) {
        if (element == null) {
            return Map.of();
        }
        if (!element.isJsonObject()) {
            invalid.add("palette: expected object");
            return Map.of();
        }
        Map<String, PaletteColor> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            try {
                if (!entry.getValue().isJsonObject()) {
                    throw new IllegalArgumentException("expected object");
                }
                JsonObject value = entry.getValue().getAsJsonObject();
                String rgb = requiredString(value, "rgb");
                long argb = requiredUnsignedLong(value, "argb");
                result.put(VisualDomain.normalizeId(entry.getKey()), new PaletteColor(rgb, argb));
            } catch (RuntimeException exception) {
                invalid.add("palette." + entry.getKey() + ": " + message(exception));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<VisualDomain, Integer> parseCounts(JsonElement element, List<String> invalid) {
        EnumMap<VisualDomain, Integer> result = new EnumMap<>(VisualDomain.class);
        if (element == null) {
            return Collections.unmodifiableMap(result);
        }
        if (!element.isJsonObject()) {
            invalid.add("counts: expected object");
            return Collections.unmodifiableMap(result);
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            try {
                result.put(VisualDomain.parse(entry.getKey()), requiredInteger(entry.getValue(), "count"));
            } catch (RuntimeException exception) {
                invalid.add("counts." + entry.getKey() + ": " + message(exception));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static int optionalInteger(JsonObject object, String key, int fallback,
                                       String label, List<String> invalid) {
        if (!object.has(key)) {
            return fallback;
        }
        try {
            return requiredInteger(object, key);
        } catch (RuntimeException exception) {
            invalid.add(label + "." + key + ": " + message(exception));
            return fallback;
        }
    }

    private static int requiredInteger(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalArgumentException("missing " + key);
        }
        return requiredInteger(object.get(key), key);
    }

    private static int requiredInteger(JsonElement element, String key) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        try {
            BigDecimal value = new BigDecimal(element.getAsString());
            if (value.scale() > 0 && value.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException(key + " must be integral");
            }
            return value.intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be an integer", exception);
        }
    }

    private static long requiredUnsignedLong(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalArgumentException("missing " + key);
        }
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        try {
            BigDecimal value = new BigDecimal(element.getAsString());
            long result = value.longValueExact();
            if (result < 0L || result > 0xffff_ffffL) {
                throw new IllegalArgumentException(key + " outside unsigned 32-bit range");
            }
            return result;
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException(key + " must be an unsigned integer", exception);
        }
    }

    private static double requiredDouble(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalArgumentException("missing " + key);
        }
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(key + " must be finite");
        }
        return value;
    }

    private static boolean requiredBoolean(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalArgumentException("missing " + key);
        }
        JsonElement element = object.get(key);
        JsonPrimitive primitive = element != null && element.isJsonPrimitive()
                ? element.getAsJsonPrimitive() : null;
        if (primitive == null || !primitive.isBoolean()) {
            throw new IllegalArgumentException(key + " must be boolean");
        }
        return primitive.getAsBoolean();
    }

    private static String requiredString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalArgumentException("missing " + key);
        }
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be string");
        }
        String value = element.getAsString().trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }
        return value;
    }

    private static String normalizedRequired(JsonObject object, String key) {
        return requiredString(object, key).toLowerCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
    }

    private static <E extends Enum<E>> void requireEnum(Class<E> type, String value, String field) {
        try {
            Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown " + field + " " + value);
        }
    }

    private static String optionalString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        JsonElement value = object.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be string");
        }
        return value.getAsString().trim();
    }

    private static String message(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    /** A palette entry retains both the authored RGB spelling and unsigned ARGB value. */
    public record PaletteColor(String rgb, long argb) {
        public PaletteColor {
            rgb = rgb == null ? "" : rgb.trim();
            if (argb < 0L || argb > 0xffff_ffffL) {
                throw new IllegalArgumentException("ARGB outside unsigned 32-bit range");
            }
        }
    }

    /** Alias record useful to callers that need more than the flattened map. */
    public record Alias(String key, String targetKey) {}

    /** Immutable catalog snapshot, including non-fatal row diagnostics. */
    public record Snapshot(
            Map<String, VisualProfile> profiles,
            Map<String, String> aliases,
            Map<String, String> sourceHashes,
            Map<String, Map<String, String>> collisionResolutions,
            Map<String, PaletteColor> palette,
            Map<VisualDomain, Integer> counts,
            int declaredProfileCount,
            int schemaVersion,
            List<String> invalidRows) {

        public Snapshot {
            profiles = immutableProfiles(profiles);
            aliases = immutableStrings(aliases);
            sourceHashes = immutableStrings(sourceHashes);
            collisionResolutions = immutableNestedStrings(collisionResolutions);
            palette = palette == null ? Map.of() : Map.copyOf(palette);
            counts = counts == null ? Map.of() : Map.copyOf(counts);
            invalidRows = invalidRows == null ? List.of() : List.copyOf(invalidRows);
        }

        public static Snapshot empty(List<String> invalidRows) {
            return new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                    0, 0, invalidRows);
        }

        public Optional<VisualProfile> find(String key) {
            String normalized = VisualDomain.normalizeKey(key);
            VisualProfile direct = profiles.get(normalized);
            if (direct != null) {
                return Optional.of(direct);
            }
            if (normalized.indexOf(':') >= 0 || normalized.isBlank()) {
                return Optional.empty();
            }
            VisualProfile match = null;
            for (VisualProfile profile : profiles.values()) {
                if (profile.id().equals(normalized)) {
                    if (match != null) {
                        return Optional.empty();
                    }
                    match = profile;
                }
            }
            return Optional.ofNullable(match);
        }

        public Optional<VisualProfile> find(VisualDomain domain, String id) {
            if (domain == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(profiles.get(domain.qualify(id)));
        }

        public Optional<VisualProfile> resolve(String key) {
            String normalized = VisualDomain.normalizeKey(key);
            if (normalized.indexOf(':') < 0) {
                if (normalized.isBlank()) {
                    return Optional.empty();
                }
                VisualProfile match = null;
                for (VisualDomain domain : VisualDomain.values()) {
                    Optional<VisualProfile> candidate = resolve(domain.qualify(normalized));
                    if (candidate.isPresent()) {
                        if (match != null && match != candidate.get()) {
                            return Optional.empty();
                        }
                        match = candidate.get();
                    }
                }
                return Optional.ofNullable(match);
            }
            String target = aliases.get(normalized);
            if (target == null) {
                return Optional.ofNullable(profiles.get(normalized));
            }
            Set<String> visited = new LinkedHashSet<>();
            String current = normalized;
            while (target != null && visited.add(current)) {
                current = target;
                target = aliases.get(current);
            }
            return visited.contains(current) ? Optional.empty() : Optional.ofNullable(profiles.get(current));
        }

        public Optional<VisualProfile> resolve(VisualDomain domain, String id) {
            if (domain == null) {
                return Optional.empty();
            }
            return resolve(domain.qualify(id));
        }

        public int count() {
            return profiles.size();
        }

        public int count(VisualDomain domain) {
            if (domain == null) {
                return 0;
            }
            return (int) profiles.values().stream().filter(profile -> profile.domain() == domain).count();
        }

        public Optional<PaletteColor> palette(String key) {
            return Optional.ofNullable(palette.get(VisualDomain.normalizeId(key)));
        }

        private static Map<String, VisualProfile> immutableProfiles(Map<String, VisualProfile> values) {
            if (values == null || values.isEmpty()) {
                return Map.of();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }

        private static Map<String, String> immutableStrings(Map<String, String> values) {
            if (values == null || values.isEmpty()) {
                return Map.of();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }

        private static Map<String, Map<String, String>> immutableNestedStrings(
                Map<String, Map<String, String>> values) {
            if (values == null || values.isEmpty()) {
                return Map.of();
            }
            Map<String, Map<String, String>> copy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, String>> entry : values.entrySet()) {
                copy.put(entry.getKey(), immutableStrings(entry.getValue()));
            }
            return Collections.unmodifiableMap(copy);
        }
    }
}
