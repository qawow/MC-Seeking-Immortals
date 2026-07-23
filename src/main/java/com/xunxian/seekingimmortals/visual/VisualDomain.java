package com.xunxian.seekingimmortals.visual;

import java.util.Locale;

/** The authored visual namespaces that share the unified catalog. */
public enum VisualDomain {
    TECHNIQUE,
    ARTIFACT,
    PILL,
    CONSUMABLE,
    METHOD,
    HERB,
    MATERIAL,
    BEAST,
    NPC,
    REALM,
    ZONE,
    BOSS,
    STATUS,
    STRUCTURE,
    VEHICLE,
    FORMATION,
    TRIBULATION;

    public String wireName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String qualify(String id) {
        return wireName() + ":" + normalizeId(id);
    }

    public static VisualDomain parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("blank visual domain");
        }
        String normalized = value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        return valueOf(normalized);
    }

    public static String normalizeId(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    public static String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        int separator = normalized.indexOf(':');
        if (separator < 0) {
            return normalized;
        }
        return normalized.substring(0, separator) + ":" + normalizeId(normalized.substring(separator + 1));
    }
}
