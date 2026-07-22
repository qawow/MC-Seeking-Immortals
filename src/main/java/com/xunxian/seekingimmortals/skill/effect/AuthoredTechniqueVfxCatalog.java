package com.xunxian.seekingimmortals.skill.effect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.network.TechniqueVfxPacket;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Compact runtime projection of the v118-v122 authored technique visual stack. */
public final class AuthoredTechniqueVfxCatalog {
    private static final String RESOURCE = "data/" + SeekingImmortalsMod.MODID
            + "/visual/authored_technique_vfx_profiles.json";
    private static final Map<String, Profile> PROFILES = load();

    private AuthoredTechniqueVfxCatalog() {}

    public static Optional<Profile> find(String techniqueId) {
        return Optional.ofNullable(PROFILES.get(normalize(techniqueId)));
    }

    public static Map<String, Profile> profiles() {
        return PROFILES;
    }

    public record Profile(
            String id,
            String school,
            String effectType,
            String element,
            TechniqueVfxPacket.ParticleStyle particle,
            TechniqueVfxPacket.TrailStyle trail,
            String shape,
            String color,
            int frameCount,
            boolean telegraphed
    ) {
        public Profile {
            id = normalize(id);
            school = normalize(school);
            effectType = normalize(effectType);
            element = normalize(element);
            particle = particle == null ? TechniqueVfxPacket.ParticleStyle.DEFAULT : particle;
            trail = trail == null ? TechniqueVfxPacket.TrailStyle.DEFAULT : trail;
            shape = shape == null ? "" : shape.trim();
            color = color == null ? "" : color.trim();
            frameCount = Math.max(0, Math.min(8, frameCount));
        }
    }

    private static Map<String, Profile> load() {
        Map<String, Profile> result = new LinkedHashMap<>();
        try (InputStream stream = AuthoredTechniqueVfxCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.warn("Missing authored technique VFX catalog {}", RESOURCE);
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (integer(root, "schema_version") != 1) {
                    throw new IllegalStateException("Unsupported authored VFX profile schema");
                }
                int expectedCount = integer(root, "profile_count");
                JsonArray profiles = root.getAsJsonArray("profiles");
                if (profiles == null) {
                    return Map.of();
                }
                for (JsonElement element : profiles) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject object = element.getAsJsonObject();
                    String particleRef = string(object, "particle");
                    String trailRef = string(object, "trail");
                    TechniqueVfxPacket.ParticleStyle particle =
                            TechniqueVfxPacket.ParticleStyle.fromAuthorRef(particleRef);
                    TechniqueVfxPacket.TrailStyle trail = TechniqueVfxPacket.TrailStyle.fromAuthorRef(trailRef);
                    if (!particleRef.isBlank() && particle == TechniqueVfxPacket.ParticleStyle.DEFAULT) {
                        throw new IllegalStateException("Unknown authored particle reference " + particleRef);
                    }
                    if (!trailRef.isBlank() && trail == TechniqueVfxPacket.TrailStyle.DEFAULT) {
                        throw new IllegalStateException("Unknown authored trail reference " + trailRef);
                    }
                    Profile profile = new Profile(
                            string(object, "id"),
                            string(object, "school"),
                            string(object, "effect_type"),
                            string(object, "element"),
                            particle,
                            trail,
                            string(object, "shape"),
                            string(object, "color"),
                            integer(object, "frame_count"),
                            bool(object, "has_telegraph"));
                    if (!profile.id().isBlank()) {
                        if (result.putIfAbsent(profile.id(), profile) != null) {
                            throw new IllegalStateException("Duplicate authored VFX profile " + profile.id());
                        }
                    }
                }
                if (expectedCount <= 0 || result.size() != expectedCount) {
                    throw new IllegalStateException("Authored VFX profile count mismatch: expected "
                            + expectedCount + ", loaded " + result.size());
                }
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load authored technique VFX catalog {}", RESOURCE, exception);
            return Map.of();
        }
        return Map.copyOf(result);
    }

    private static String string(JsonObject object, String key) {
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
}
