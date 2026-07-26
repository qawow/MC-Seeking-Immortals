package com.xunxian.seekingimmortals.skill.effect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Typed runtime projection of every authored spell plan in the text-material corpus. */
public final class AuthoredSpellEffectCatalog {
    private static final String RESOURCE = "data/" + SeekingImmortalsMod.MODID
            + "/visual/authored_spell_effects.json";
    private static final Pattern FRAME_RANGE = Pattern.compile("^(\\d+)-(\\d+)$");
    private static final Pattern SHA_256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final Set<String> NAMESPACES = Set.of("corpus", "novel");
    private static final Set<String> TARGETS = Set.of("self", "single", "area");
    private static final Set<String> EFFECT_TYPES = Set.of(
            "projectile", "beam", "cone", "chain", "aoe", "aoe_control", "aoe_dot", "field",
            "domain", "wall", "trap", "buff_zone", "debuff", "dot", "drain", "control",
            "buff_self", "buff", "shield", "transform", "heal", "heal_spirit", "cleanse",
            "movement", "dash", "escape", "teleport_short", "melee", "strike", "ultimate",
            "secret_art", "soul_attack", "summon", "summon_field", "talisman_consume", "utility",
            "utility_combat", "scout", "scan", "inspect", "command", "craft_gate");
    private static final Set<String> STATUSES = Set.of(
            "regeneration", "absorption", "resistance", "damage_boost", "burning", "weakness",
            "slowness", "armor_break", "bleeding", "rooted", "poison", "levitation", "frozen",
            "stunned", "glowing", "blindness", "wither", "confusion");
    private static final Snapshot BUILTIN = loadBuiltin();

    private AuthoredSpellEffectCatalog() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static Map<String, Profile> profiles() {
        return BUILTIN.profiles();
    }

    public static Optional<Profile> find(String techniqueId) {
        return Optional.ofNullable(BUILTIN.profiles().get(normalize(techniqueId)));
    }

    public static Snapshot parseForTest(Reader reader) {
        return parse(reader);
    }

    public record Counts(int corpus, int novel, int total, int novelDirectVisual,
                         int novelSettingFallback, int novelVisualQuotes,
                         int uniqueVisualSignatures) {}

    public record Frame(String name, int startTick, int durationTicks, String source) {
        public Frame {
            name = trim(name);
            source = trim(source);
            if (name.isBlank() || startTick < 0 || durationTicks < 1) {
                throw new IllegalArgumentException("invalid authored spell frame");
            }
        }
    }

    public record Functional(String type, String element, String target, int cost,
                             int cooldownTicks, double damageBase, double range, double radius,
                             String primaryStatus, String secondaryStatus) {
        public Functional {
            type = normalize(type);
            element = normalize(element);
            target = normalize(target);
            primaryStatus = normalize(primaryStatus);
            secondaryStatus = normalize(secondaryStatus);
        }
    }

    public enum Operation {
        ATTACK, DEFEND, RESTORE, RESTORE_SPIRIT, CLEANSE, MOVE, SUMMON, COMMAND,
        DETECT, CONCEAL, TRANSFORM, TERRAIN, SEAL, DRAIN, CRAFT, CULTIVATE, COMMUNICATE
    }

    public enum Delivery {
        PROJECTILE, BEAM, CONE, CHAIN, AREA, FIELD, SELF, MOVEMENT, SUMMON, COMMAND,
        TARGETED, CONTACT
    }

    public enum SummonArchetype {
        GENERIC, BEAST, PUPPET, GHOST
    }

    public enum TerrainMode {
        NONE, SAND, ICE, FIRE, VINE, ROCK, WATER, WIND
    }

    public record Mechanics(Operation operation, Delivery delivery, int durationTicks,
                            int projectileCount, int maxTargets,
                            SummonArchetype summonArchetype, TerrainMode terrainMode) {
        public Mechanics {
            if (operation == null || delivery == null || summonArchetype == null || terrainMode == null
                    || durationTicks < 20 || durationTicks > 600
                    || projectileCount < 1 || projectileCount > 12
                    || maxTargets < 1 || maxTargets > 32) {
                throw new IllegalArgumentException("invalid authored spell mechanics");
            }
        }
    }

    public record Profile(String id, String namespace, String qualifiedId, String display,
                          String source, String sourceFile,
                          TechniqueVfxPalette.Family family, TechniqueVfxPacket.Motif motif,
                          String effectType, String target,
                          TechniqueVfxPacket.ParticleStyle particle,
                          TechniqueVfxPacket.TrailStyle trail,
                          String shape, String color, boolean telegraphed,
                          double radius, int intensity, int scaleTier, String visualSignature,
                          List<Frame> frames, Mechanics mechanics, Functional functional, Set<String> tags) {
        public Profile {
            id = normalize(id);
            namespace = normalize(namespace);
            qualifiedId = normalizeQualified(qualifiedId);
            display = trim(display);
            source = trim(source);
            sourceFile = trim(sourceFile);
            effectType = normalize(effectType);
            target = normalize(target);
            shape = normalize(shape);
            color = trim(color);
            visualSignature = normalize(visualSignature);
            frames = List.copyOf(frames == null ? List.of() : frames);
            tags = immutableNormalizedSet(tags);
        }
    }

    public record Snapshot(Map<String, Profile> profiles, Counts counts, List<String> invalidRows) {
        public Snapshot {
            profiles = Collections.unmodifiableMap(new LinkedHashMap<>(profiles));
            invalidRows = List.copyOf(invalidRows);
        }

        public boolean valid() {
            return invalidRows.isEmpty() && profiles.size() == counts.total();
        }

        private static Snapshot empty(List<String> invalidRows) {
            return new Snapshot(Map.of(), new Counts(0, 0, 0, 0, 0, 0, 0), invalidRows);
        }
    }

    private static Snapshot loadBuiltin() {
        try (InputStream stream = AuthoredSpellEffectCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                Snapshot empty = Snapshot.empty(List.of("missing resource " + RESOURCE));
                SeekingImmortalsMod.LOGGER.error("Missing authored spell effect catalog {}", RESOURCE);
                return empty;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Snapshot snapshot = parse(reader);
                if (!snapshot.valid()) {
                    SeekingImmortalsMod.LOGGER.error("Invalid authored spell effect catalog {}: {}",
                            RESOURCE, snapshot.invalidRows());
                }
                return snapshot;
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.error("Failed to load authored spell effect catalog {}", RESOURCE, exception);
            return Snapshot.empty(List.of(exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage()));
        }
    }

    private static Snapshot parse(Reader reader) {
        List<String> invalid = new ArrayList<>();
        if (reader == null) {
            return Snapshot.empty(List.of("null reader"));
        }
        JsonObject root;
        try {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException exception) {
            return Snapshot.empty(List.of("root: " + message(exception)));
        }
        if (integer(root, "schema_version") != 1) {
            invalid.add("root: unsupported schema_version");
        }
        Counts counts;
        try {
            counts = parseCounts(requiredObject(root, "counts"));
        } catch (RuntimeException exception) {
            return Snapshot.empty(List.of("counts: " + message(exception)));
        }

        Map<String, Profile> profiles = new LinkedHashMap<>();
        Set<String> signatures = new LinkedHashSet<>();
        JsonArray array = root.getAsJsonArray("profiles");
        if (array == null) {
            return Snapshot.empty(List.of("profiles: expected array"));
        }
        for (int index = 0; index < array.size(); index++) {
            try {
                JsonElement element = array.get(index);
                if (element == null || !element.isJsonObject()) {
                    throw new IllegalArgumentException("expected object");
                }
                Profile profile = parseProfile(element.getAsJsonObject());
                if (profiles.putIfAbsent(profile.id(), profile) != null) {
                    throw new IllegalArgumentException("duplicate id " + profile.id());
                }
                if (!signatures.add(profile.visualSignature())) {
                    throw new IllegalArgumentException("duplicate visual_signature " + profile.visualSignature());
                }
            } catch (RuntimeException exception) {
                invalid.add("profiles[" + index + "]: " + message(exception));
            }
        }

        int corpus = (int) profiles.values().stream().filter(profile -> "corpus".equals(profile.namespace())).count();
        int novel = profiles.size() - corpus;
        if (profiles.size() != counts.total() || corpus != counts.corpus() || novel != counts.novel()
                || signatures.size() != counts.uniqueVisualSignatures()) {
            invalid.add("counts: declared/runtime mismatch");
        }
        return new Snapshot(profiles, counts, invalid);
    }

    private static Profile parseProfile(JsonObject object) {
        String id = requiredString(object, "id");
        String namespace = normalize(requiredString(object, "namespace"));
        String qualifiedId = requiredString(object, "qualified_id");
        if (!NAMESPACES.contains(namespace) || !qualifiedId.equals(namespace + ":" + id)) {
            throw new IllegalArgumentException("invalid namespace/qualified_id");
        }
        TechniqueVfxPalette.Family family = enumValue(
                TechniqueVfxPalette.Family.class, requiredString(object, "family"));
        TechniqueVfxPacket.Motif motif = enumValue(
                TechniqueVfxPacket.Motif.class, requiredString(object, "motif"));
        String effectType = normalize(requiredString(object, "effect_type"));
        String target = normalize(requiredString(object, "target"));
        if (!EFFECT_TYPES.contains(effectType) || !TARGETS.contains(target)) {
            throw new IllegalArgumentException("unknown effect_type/target");
        }
        String particleRef = requiredString(object, "particle");
        String trailRef = requiredString(object, "trail");
        TechniqueVfxPacket.ParticleStyle particle = TechniqueVfxPacket.ParticleStyle.fromAuthorRef(particleRef);
        TechniqueVfxPacket.TrailStyle trail = TechniqueVfxPacket.TrailStyle.fromAuthorRef(trailRef);
        if (particle == TechniqueVfxPacket.ParticleStyle.DEFAULT
                || trail == TechniqueVfxPacket.TrailStyle.DEFAULT) {
            throw new IllegalArgumentException("unknown particle/trail reference");
        }
        double radius = decimal(object, "radius");
        int intensity = integer(object, "intensity");
        int scaleTier = integer(object, "scale_tier");
        if (!Double.isFinite(radius) || radius < 0.1D || radius > 8.0D
                || intensity < 1 || intensity > 48 || scaleTier < 0 || scaleTier > 4) {
            throw new IllegalArgumentException("visual bounds exceeded");
        }
        String signature = normalize(requiredString(object, "visual_signature"));
        if (!SHA_256.matcher(signature).matches()) {
            throw new IllegalArgumentException("invalid visual_signature");
        }
        List<Frame> frames = parseFrames(object.getAsJsonArray("frames"));
        Functional functional = parseFunctional(requiredObject(object, "functional"));
        Mechanics mechanics = parseMechanics(requiredObject(object, "mechanics"));
        if (!effectType.equals(functional.type()) || !target.equals(functional.target())
                || Math.abs(radius - functional.radius()) > 0.0001D) {
            throw new IllegalArgumentException("visual/functional semantic mismatch");
        }
        return new Profile(id, namespace, qualifiedId, optionalString(object, "display"),
                optionalString(object, "source"), requiredString(object, "source_file"),
                family, motif, effectType, target, particle, trail,
                requiredString(object, "shape"), optionalString(object, "color"),
                bool(object, "telegraphed"), radius, intensity, scaleTier, signature,
                frames, mechanics, functional, stringSet(object.get("tags")));
    }

    private static Mechanics parseMechanics(JsonObject object) {
        return new Mechanics(
                enumValue(Operation.class, requiredString(object, "operation")),
                enumValue(Delivery.class, requiredString(object, "delivery")),
                integer(object, "duration_ticks"),
                integer(object, "projectile_count"),
                integer(object, "max_targets"),
                enumValue(SummonArchetype.class, requiredString(object, "summon_archetype")),
                enumValue(TerrainMode.class, requiredString(object, "terrain_mode")));
    }

    private static Functional parseFunctional(JsonObject object) {
        String type = normalize(requiredString(object, "type"));
        String target = normalize(requiredString(object, "target"));
        String primaryStatus = normalize(requiredString(object, "primary_status"));
        String secondaryStatus = normalize(requiredString(object, "secondary_status"));
        int cost = integer(object, "cost");
        int cooldown = integer(object, "cooldown_ticks");
        double damage = decimal(object, "damage_base");
        double range = decimal(object, "range");
        double radius = decimal(object, "radius");
        if (!EFFECT_TYPES.contains(type) || !TARGETS.contains(target)
                || !STATUSES.contains(primaryStatus) || !STATUSES.contains(secondaryStatus)
                || cost < 1 || cooldown < 20 || damage < 0.0D
                || !Double.isFinite(range) || range < 0.0D || range > 48.0D
                || !Double.isFinite(radius) || radius < 0.1D || radius > 8.0D) {
            throw new IllegalArgumentException("invalid functional plan");
        }
        return new Functional(type, requiredString(object, "element"), target, cost, cooldown,
                damage, range, radius, primaryStatus, secondaryStatus);
    }

    private static List<Frame> parseFrames(JsonArray array) {
        if (array == null || array.isEmpty() || array.size() > 9) {
            throw new IllegalArgumentException("frames must contain 1-9 rows");
        }
        List<Frame> frames = new ArrayList<>();
        int previousEnd = -1;
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) {
                throw new IllegalArgumentException("frame must be an object");
            }
            JsonObject object = element.getAsJsonObject();
            Matcher matcher = FRAME_RANGE.matcher(requiredString(object, "frame"));
            if (!matcher.matches()) {
                throw new IllegalArgumentException("invalid frame range");
            }
            int start = Integer.parseInt(matcher.group(1));
            int end = Integer.parseInt(matcher.group(2));
            if (end < start || start <= previousEnd) {
                throw new IllegalArgumentException("overlapping frame range");
            }
            frames.add(new Frame(requiredString(object, "name"), start, end - start + 1,
                    requiredString(object, "vis")));
            previousEnd = end;
        }
        return List.copyOf(frames);
    }

    private static Counts parseCounts(JsonObject object) {
        return new Counts(integer(object, "corpus"), integer(object, "novel"), integer(object, "total"),
                integer(object, "novel_direct_visual"), integer(object, "novel_setting_fallback"),
                integer(object, "novel_visual_quotes"), integer(object, "unique_visual_signatures"));
    }

    private static JsonObject requiredObject(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalArgumentException(key + " must be an object");
        }
        return value.getAsJsonObject();
    }

    private static String requiredString(JsonObject object, String key) {
        String value = optionalString(object, key);
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }
        return value;
    }

    private static String optionalString(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? trim(object.get(key).getAsString()) : "";
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private static int integer(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : 0;
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static double decimal(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : 0.0D;
        } catch (RuntimeException exception) {
            return 0.0D;
        }
    }

    private static boolean bool(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static Set<String> stringSet(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child != null && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                String value = normalize(child.getAsString());
                if (!value.isBlank()) {
                    result.add(value);
                }
            }
        }
        return Set.copyOf(result);
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown " + type.getSimpleName() + " " + value);
        }
    }

    private static Set<String> immutableNormalizedSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalize(value);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return Set.copyOf(result);
    }

    private static String normalizeQualified(String value) {
        return trim(value).toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String normalize(String value) {
        return trim(value).toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String message(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
