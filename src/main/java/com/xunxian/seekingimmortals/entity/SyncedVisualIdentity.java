package com.xunxian.seekingimmortals.entity;

import java.util.Locale;

/** Small, side-neutral validation helpers for entity-owned visual identity data. */
public final class SyncedVisualIdentity {
    public static final int MAX_KEY_LENGTH = 96;

    private SyncedVisualIdentity() {}

    /**
     * Returns a bounded, lower-case identifier suitable for SynchedEntityData or NBT.
     * Invalid, blank, or overlong values fall back instead of being truncated into a
     * different authored profile.
     */
    public static String boundedKey(String value, String fallback) {
        String safeFallback = fallback == null ? "" : fallback.trim().toLowerCase(Locale.ROOT);
        String candidate = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (candidate.length() > MAX_KEY_LENGTH || candidate.isBlank() || !isSafe(candidate)) {
            return safeFallback;
        }
        return candidate;
    }

    /** Adds a domain to a raw profile id while preserving an already-qualified key. */
    public static String qualified(String domain, String value, String fallback) {
        String normalized = boundedKey(value, fallback);
        if (normalized.indexOf(':') < 0 && domain != null && !domain.isBlank()) {
            normalized = domain.trim().toLowerCase(Locale.ROOT) + ":" + normalized;
        }
        return boundedKey(normalized, fallback);
    }

    /** Returns the id portion of a qualified key for legacy catalog lookup. */
    public static String rawId(String qualifiedKey) {
        String normalized = boundedKey(qualifiedKey, "");
        int separator = normalized.indexOf(':');
        return separator < 0 ? normalized : normalized.substring(separator + 1);
    }

    public static boolean isSafe(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int domainSeparators = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (!(character >= 'a' && character <= 'z')
                    && !(character >= '0' && character <= '9')
                    && character != '_' && character != '-' && character != '.'
                    && character != '/' && character != ':') {
                return false;
            }
            if (character == ':' && ++domainSeparators > 1) {
                return false;
            }
        }
        return value.charAt(0) != ':' && value.charAt(value.length() - 1) != ':'
                && !value.contains("::");
    }

    public static <E extends Enum<E>> E byOrdinal(E[] values, int ordinal, E fallback) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    public static <E extends Enum<E>> E byName(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
