package com.xunxian.seekingimmortals.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.Locale;

/** Shared safeguards for text that can reach a player-facing tooltip or screen. */
public final class PlayerDisplayText {
    private PlayerDisplayText() {}

    public static boolean hasTranslation(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            Language language = Language.getInstance();
            return language != null && language.has(key);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Component translatedOr(String key, String fallbackKey, Object... args) {
        return hasTranslation(key) ? Component.translatable(key, args)
                : Component.translatable(fallbackKey, args);
    }

    public static Component itemName(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return Component.translatable("text.seeking_immortals.unknown_item");
        }
        String value = rawId.trim().toLowerCase(Locale.ROOT);
        int separator = value.indexOf(':');
        String namespace = separator > 0 ? value.substring(0, separator) : "seeking_immortals";
        String path = separator >= 0 ? value.substring(separator + 1) : value;
        if (!path.isBlank()) {
            String key = "item." + namespace + "." + path;
            if (hasTranslation(key)) {
                return Component.translatable(key);
            }
        }
        return Component.translatable("text.seeking_immortals.unknown_item");
    }

    /** Resolves an already registered item without exposing its translation key when absent. */
    public static Component itemName(Item item) {
        if (item == null) {
            return Component.translatable("text.seeking_immortals.unknown_item");
        }
        String key = item.getDescriptionId();
        return hasTranslation(key)
                ? Component.translatable(key)
                : Component.translatable("text.seeking_immortals.unknown_item");
    }

    public static Component safeLiteral(String value, String fallbackKey) {
        return isSafe(value) ? Component.literal(value.trim()) : Component.translatable(fallbackKey);
    }

    /**
     * Sanitizes authored catalog text before it is inserted into a message or screen.
     * Catalogs occasionally contain a Chinese label with an implementation token mixed in
     * (for example a {@code hehuan_sect} prefix); the token is not player-facing text and must not
     * be allowed to leak. Pure ids fall back to the supplied human label.
     */
    public static String sanitizeCatalogText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (isSafe(trimmed)) {
            return trimmed;
        }
        if (!containsHan(trimmed)) {
            return "";
        }
        String cleaned = trimmed
                .replaceAll("[A-Za-z][A-Za-z0-9_.:/-]*", "")
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+([/·,，。；：、（）()])", "$1")
                .replaceAll("([（(])\\s+", "$1")
                .trim();
        return containsHan(cleaned) && !looksLikeCode(cleaned) ? cleaned : "";
    }

    /** Returns a literal only after catalog-code sanitization, otherwise a literal fallback. */
    public static Component safeCatalogLiteral(String value, String fallback) {
        String cleaned = sanitizeCatalogText(value);
        return Component.literal(cleaned.isBlank() ? (fallback == null ? "" : fallback) : cleaned);
    }

    /** True when a literal is suitable for direct player display rather than an internal id. */
    public static boolean isSafe(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        boolean hasHan = false;
        for (int i = 0; i < value.length(); i++) {
            if (Character.UnicodeScript.of(value.charAt(i)) == Character.UnicodeScript.HAN) {
                hasHan = true;
                break;
            }
        }
        if (!hasHan) {
            return false;
        }
        // Mixed Chinese/ASCII implementation tokens are still leaks (for example v135 or
        // structure_token); numbers, punctuation and %s placeholders remain valid.
        return !value.matches(".*[A-Za-z_][A-Za-z0-9_.:/-]*.*");
    }

    public static boolean looksLikeCode(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim();
        if (isSafe(normalized)) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        return lower.contains("_") || lower.matches("[a-z0-9\\-./: ]+") || !containsHan(normalized);
    }

    public static String normalizeId(String rawId) {
        if (rawId == null) {
            return "";
        }
        String value = rawId.trim().toLowerCase(Locale.ROOT);
        int separator = value.indexOf(':');
        return separator >= 0 ? value.substring(separator + 1) : value;
    }

    private static boolean containsHan(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.UnicodeScript.of(value.charAt(i)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }
}
