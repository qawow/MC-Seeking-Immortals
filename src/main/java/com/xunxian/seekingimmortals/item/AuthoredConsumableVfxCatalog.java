package com.xunxian.seekingimmortals.item;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Runtime projection of the v118-v122 authored pill and consumable visual stack. */
public final class AuthoredConsumableVfxCatalog {
    private static final String RESOURCE = "data/" + SeekingImmortalsMod.MODID
            + "/visual/authored_consumable_vfx_profiles.json";
    private static final int EXPECTED_PILL_COUNT = 114;
    private static final int EXPECTED_CONSUMABLE_COUNT = 57;
    private static final Map<String, Profile> PILLS = load("pills", EXPECTED_PILL_COUNT, Profile.Type.PILL);
    private static final Map<String, Profile> CONSUMABLES = load(
            "consumables", EXPECTED_CONSUMABLE_COUNT, Profile.Type.CONSUMABLE);
    private static final Map<String, String> PILL_ALIASES = Map.of(
            "appearance_lock_pill", "dingyan_pill",
            "beast_taming_pill_low", "beast_taming_pill",
            "jiangying_pill", "jiangchen_pill",
            "marrow_drain_pill", "marrow_extract_pill",
            "qingxu_pill", "calm_spirit_pill");

    private AuthoredConsumableVfxCatalog() {}

    public static Optional<Profile> findPill(String pillId) {
        String key = normalize(pillId);
        if (key.isBlank()) {
            return Optional.empty();
        }
        // Runtime PillEffectCatalog canonicalizes jiangying before resolving a profile.
        key = PILL_ALIASES.getOrDefault(key, key);
        Profile direct = PILLS.get(key);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (String suffix : new String[]{"_mid", "_middle", "_high", "_supreme", "_perfect", "_low"}) {
            if (!key.endsWith(suffix)) {
                continue;
            }
            String base = key.substring(0, key.length() - suffix.length());
            base = PILL_ALIASES.getOrDefault(base, base);
            Profile resolved = PILLS.get(base);
            if (resolved != null) {
                return Optional.of(resolved);
            }
        }
        return Optional.empty();
    }

    public static Optional<Profile> findConsumable(String catalogId) {
        return Optional.ofNullable(CONSUMABLES.get(normalize(catalogId)));
    }

    public static Map<String, Profile> pills() {
        return PILLS;
    }

    public static Map<String, Profile> consumables() {
        return CONSUMABLES;
    }

    /** All profiles, keyed with a type prefix so duplicate catalog ids remain visible. */
    public static Map<String, Profile> profiles() {
        Map<String, Profile> result = new LinkedHashMap<>();
        PILLS.forEach((id, profile) -> result.put("pill:" + id, profile));
        CONSUMABLES.forEach((id, profile) -> result.put("consumable:" + id, profile));
        return Collections.unmodifiableMap(result);
    }

    public record Profile(
            String id,
            Type type,
            String category,
            String effect,
            TechniqueVfxPalette.Family family,
            TechniqueVfxPacket.Motif motif,
            TechniqueVfxPacket.Kind vfxKind,
            TechniqueVfxPacket.ParticleStyle particle,
            TechniqueVfxPacket.TrailStyle trail,
            boolean telegraphed,
            double radius,
            int intensity,
            String appearance,
            String activeVfx,
            String bodyVfx,
            String uiVfx,
            List<String> frames,
            Map<String, String> sources
    ) {
        public Profile {
            id = normalize(id);
            category = normalize(category);
            effect = normalize(effect);
            type = type == null ? Type.CONSUMABLE : type;
            family = family == null ? TechniqueVfxPalette.Family.NEUTRAL : family;
            motif = motif == null ? TechniqueVfxPacket.Motif.GENERIC : motif;
            vfxKind = vfxKind == null ? TechniqueVfxPacket.Kind.STATUS : vfxKind;
            particle = particle == null ? TechniqueVfxPacket.ParticleStyle.DEFAULT : particle;
            trail = trail == null ? TechniqueVfxPacket.TrailStyle.DEFAULT : trail;
            radius = clamp(radius, 0.35D, 4.0D, 0.9D);
            intensity = Math.max(8, Math.min(48, intensity));
            appearance = trim(appearance);
            activeVfx = trim(activeVfx);
            bodyVfx = trim(bodyVfx);
            uiVfx = trim(uiVfx);
            frames = List.copyOf(frames == null ? List.of() : frames);
            sources = Map.copyOf(sources == null ? Map.of() : sources);
        }

        public enum Type {
            PILL,
            CONSUMABLE
        }

        public boolean storageLike() {
            return "storage".equals(category)
                    || effect.startsWith("portable_storage_")
                    || effect.startsWith("extra_inventory_slots_");
        }

        public boolean hasAuthoredText() {
            return !appearance.isBlank() || !activeVfx.isBlank() || !bodyVfx.isBlank()
                    || !uiVfx.isBlank() || !frames.isEmpty();
        }
    }

    private static Map<String, Profile> load(String arrayName, int expectedCount, Profile.Type expectedType) {
        Map<String, Profile> result = new LinkedHashMap<>();
        try (InputStream stream = AuthoredConsumableVfxCatalog.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                SeekingImmortalsMod.LOGGER.warn("Missing authored consumable VFX catalog {}", RESOURCE);
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (integer(root, "schema_version") != 1) {
                    throw new IllegalStateException("Unsupported authored consumable VFX profile schema");
                }
                int declared = integer(root, expectedType == Profile.Type.PILL
                        ? "pill_profile_count" : "consumable_profile_count");
                if (declared != expectedCount) {
                    throw new IllegalStateException("Authored " + arrayName + " VFX profile count must be "
                            + expectedCount + ", got " + declared);
                }
                JsonArray profiles = root.getAsJsonArray(arrayName);
                if (profiles == null) {
                    throw new IllegalStateException("Missing authored consumable VFX array " + arrayName);
                }
                for (JsonElement element : profiles) {
                    if (!element.isJsonObject()) {
                        throw new IllegalStateException("Authored consumable VFX profile must be an object");
                    }
                    Profile profile = parseProfile(element.getAsJsonObject(), expectedType);
                    if (profile.id().isBlank()) {
                        throw new IllegalStateException("Authored consumable VFX profile has a blank id");
                    }
                    if (result.putIfAbsent(profile.id(), profile) != null) {
                        throw new IllegalStateException("Duplicate authored " + arrayName + " VFX profile "
                                + profile.id());
                    }
                }
                if (result.size() != expectedCount) {
                    throw new IllegalStateException("Authored " + arrayName + " VFX profile count mismatch: expected "
                            + expectedCount + ", loaded " + result.size());
                }
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load authored consumable VFX catalog {}", RESOURCE, exception);
            return Map.of();
        }
        return Map.copyOf(result);
    }

    private static Profile parseProfile(JsonObject object, Profile.Type expectedType) {
        String particleRef = string(object, "particle");
        String trailRef = string(object, "trail");
        TechniqueVfxPacket.ParticleStyle particle = TechniqueVfxPacket.ParticleStyle.fromAuthorRef(particleRef);
        TechniqueVfxPacket.TrailStyle trail = TechniqueVfxPacket.TrailStyle.fromAuthorRef(trailRef);
        if (particleRef.isBlank() || particle == TechniqueVfxPacket.ParticleStyle.DEFAULT) {
            throw new IllegalStateException("Unknown authored consumable particle reference " + particleRef);
        }
        if (trailRef.isBlank() || trail == TechniqueVfxPacket.TrailStyle.DEFAULT) {
            throw new IllegalStateException("Unknown authored consumable trail reference " + trailRef);
        }
        TechniqueVfxPalette.Family family = enumValue(
                TechniqueVfxPalette.Family.class, string(object, "family"), "family");
        TechniqueVfxPacket.Motif motif = enumValue(
                TechniqueVfxPacket.Motif.class, string(object, "motif"), "motif");
        TechniqueVfxPacket.Kind vfxKind = enumValue(
                TechniqueVfxPacket.Kind.class, string(object, "vfx_kind"), "vfx_kind");
        List<String> frames = stringList(object.getAsJsonArray("frames"));
        Map<String, String> sources = stringMap(object.getAsJsonObject("sources"));
        Profile profile = new Profile(
                string(object, "id"), expectedType,
                string(object, "category"), string(object, "effect"), family, motif, vfxKind,
                particle, trail, bool(object, "telegraphed"), decimal(object, "radius"),
                integer(object, "intensity"), string(object, "appearance"), string(object, "active_vfx"),
                string(object, "body_vfx"), string(object, "ui_vfx"), frames, sources);
        if (integer(object, "frame_count") != profile.frames().size()) {
            throw new IllegalStateException("Authored consumable frame count mismatch for " + profile.id());
        }
        if (!profile.hasAuthoredText()) {
            throw new IllegalStateException("Authored consumable profile has no visual text " + profile.id());
        }
        return profile;
    }

    private static List<String> stringList(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element != null && element.isJsonPrimitive()) {
                values.add(trim(element.getAsString()));
            }
        }
        return values;
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
            throw new IllegalStateException("Unknown authored consumable " + label + " " + value, exception);
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

    private static double decimal(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : 0.9D;
        } catch (RuntimeException ignored) {
            return 0.9D;
        }
    }

    private static boolean bool(JsonObject object, String key) {
        try {
            return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static double clamp(double value, double min, double max, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
