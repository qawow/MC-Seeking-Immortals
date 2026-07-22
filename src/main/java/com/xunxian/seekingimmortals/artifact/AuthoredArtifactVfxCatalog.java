package com.xunxian.seekingimmortals.artifact;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;
import com.xunxian.seekingimmortals.skill.effect.TechniqueVfxPalette;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Runtime projection of the v118-v122 authored artifact visual and state stack. */
public final class AuthoredArtifactVfxCatalog {
    private static final String RESOURCE = "data/" + SeekingImmortalsMod.MODID
            + "/visual/authored_artifact_vfx_profiles.json";
    private static final int EXPECTED_PROFILE_COUNT = 217;
    private static final Set<String> REQUIRED_STATES = Set.of(
            "sheathed", "idle_bound", "active", "impact", "damaged", "broken");
    private static final Map<String, Profile> PROFILES = load();

    private AuthoredArtifactVfxCatalog() {}

    public static Optional<Profile> find(String artifactId) {
        return Optional.ofNullable(PROFILES.get(normalize(artifactId)));
    }

    public static Map<String, Profile> profiles() {
        return PROFILES;
    }

    public record Profile(
            String id,
            String type,
            String runtimeKind,
            String tier,
            int gameTier,
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif,
            TechniqueVfxPacket.ParticleStyle particle,
            TechniqueVfxPacket.TrailStyle trail,
            boolean telegraphed,
            String appearance,
            String silhouette,
            String color,
            String performanceIdle,
            String performanceActive,
            Map<String, String> states,
            boolean hasOrbit,
            boolean hasLaunch,
            boolean hasOpen,
            boolean hasReflect
    ) {
        public Profile {
            id = normalize(id);
            type = normalize(type);
            runtimeKind = normalize(runtimeKind);
            tier = normalize(tier);
            gameTier = Math.max(0, Math.min(32, gameTier));
            family = family == null ? TechniqueVfxPalette.Family.NEUTRAL : family;
            motif = motif == null ? TechniqueVfxPacket.Motif.GENERIC : motif;
            particle = particle == null ? TechniqueVfxPacket.ParticleStyle.DEFAULT : particle;
            trail = trail == null ? TechniqueVfxPacket.TrailStyle.DEFAULT : trail;
            appearance = trim(appearance);
            silhouette = trim(silhouette);
            color = trim(color);
            performanceIdle = trim(performanceIdle);
            performanceActive = trim(performanceActive);
            states = Map.copyOf(states == null ? Map.of() : states);
        }

        public boolean materialOnly() {
            return "material".equals(runtimeKind) || "material_artifact".equals(type);
        }

        public boolean deferred() {
            return "utility_deferred".equals(runtimeKind);
        }

        public String state(String stateId) {
            return states.getOrDefault(normalize(stateId), "");
        }
    }

    private static Map<String, Profile> load() {
        Map<String, Profile> result = new LinkedHashMap<>();
        try (InputStream stream = AuthoredArtifactVfxCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.warn("Missing authored artifact VFX catalog {}", RESOURCE);
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (integer(root, "schema_version") != 1) {
                    throw new IllegalStateException("Unsupported authored artifact VFX profile schema");
                }
                int expectedCount = integer(root, "profile_count");
                if (expectedCount != EXPECTED_PROFILE_COUNT) {
                    throw new IllegalStateException("Authored artifact VFX profile count must be "
                            + EXPECTED_PROFILE_COUNT + ", got " + expectedCount);
                }
                JsonArray profiles = root.getAsJsonArray("profiles");
                if (profiles == null) {
                    throw new IllegalStateException("Missing authored artifact VFX profiles array");
                }
                for (JsonElement element : profiles) {
                    if (!element.isJsonObject()) {
                        throw new IllegalStateException("Authored artifact VFX profile must be an object");
                    }
                    Profile profile = parseProfile(element.getAsJsonObject());
                    if (profile.id().isBlank()) {
                        throw new IllegalStateException("Authored artifact VFX profile has a blank id");
                    }
                    if (result.putIfAbsent(profile.id(), profile) != null) {
                        throw new IllegalStateException("Duplicate authored artifact VFX profile " + profile.id());
                    }
                }
                if (result.size() != expectedCount) {
                    throw new IllegalStateException("Authored artifact VFX profile count mismatch: expected "
                            + expectedCount + ", loaded " + result.size());
                }
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load authored artifact VFX catalog {}", RESOURCE, exception);
            return Map.of();
        }
        return Map.copyOf(result);
    }

    private static Profile parseProfile(JsonObject object) {
        String particleRef = string(object, "particle");
        String trailRef = string(object, "trail");
        TechniqueVfxPacket.ParticleStyle particle = TechniqueVfxPacket.ParticleStyle.fromAuthorRef(particleRef);
        TechniqueVfxPacket.TrailStyle trail = TechniqueVfxPacket.TrailStyle.fromAuthorRef(trailRef);
        if (particleRef.isBlank() || particle == TechniqueVfxPacket.ParticleStyle.DEFAULT) {
            throw new IllegalStateException("Unknown authored artifact particle reference " + particleRef);
        }
        if (trailRef.isBlank() || trail == TechniqueVfxPacket.TrailStyle.DEFAULT) {
            throw new IllegalStateException("Unknown authored artifact trail reference " + trailRef);
        }
        TechniqueVfxPalette.Family family = enumValue(
                TechniqueVfxPalette.Family.class, string(object, "family"), "family");
        TechniqueVfxPacket.Motif motif = enumValue(
                TechniqueVfxPacket.Motif.class, string(object, "motif"), "motif");
        Map<String, String> states = stringMap(object.getAsJsonObject("states"));
        if (!states.keySet().containsAll(REQUIRED_STATES)) {
            throw new IllegalStateException("Authored artifact profile " + string(object, "id")
                    + " is missing required states " + REQUIRED_STATES);
        }
        if (states.size() != integer(object, "state_count")) {
            throw new IllegalStateException("Authored artifact state count mismatch for " + string(object, "id"));
        }
        JsonObject performance = object.getAsJsonObject("performance");
        JsonObject look = object.getAsJsonObject("look");
        Profile profile = new Profile(
                string(object, "id"),
                string(object, "type"),
                string(object, "runtime_kind"),
                string(object, "tier"),
                integer(object, "game_tier"),
                family,
                motif,
                particle,
                trail,
                bool(object, "telegraphed"),
                string(object, "appearance"),
                string(look, "silhouette"),
                string(look, "color"),
                string(performance, "idle"),
                string(performance, "active"),
                states,
                bool(object, "has_orbit"),
                bool(object, "has_launch"),
                bool(object, "has_open"),
                bool(object, "has_reflect"));
        validateStateFlags(profile);
        return profile;
    }

    private static void validateStateFlags(Profile profile) {
        if (profile.hasOrbit() != profile.states().containsKey("orbit")
                || profile.hasLaunch() != profile.states().containsKey("launch")
                || profile.hasOpen() != profile.states().containsKey("open")
                || profile.hasReflect() != profile.states().containsKey("reflect")) {
            throw new IllegalStateException("Authored artifact state flags do not match states for " + profile.id());
        }
    }

    private static Map<String, String> stringMap(JsonObject object) {
        Map<String, String> values = new LinkedHashMap<>();
        if (object == null) {
            return values;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                values.put(normalize(entry.getKey()), trim(entry.getValue().getAsString()));
            }
        }
        return values;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String label) {
        try {
            return Enum.valueOf(type, normalize(value).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Unknown authored artifact " + label + " " + value, exception);
        }
    }

    private static String string(JsonObject object, String key) {
        if (object == null) {
            return "";
        }
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int integer(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean bool(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
