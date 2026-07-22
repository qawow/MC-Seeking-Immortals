package com.xunxian.seekingimmortals.client;

import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.skill.SkillType;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Keeps stable catalog codes out of player-facing cultivation screens. */
public final class CultivationDisplayTexts {
    static final String UNKNOWN_KEY = "screen.seeking_immortals.display.unknown";
    static final String LEVEL_KEY = "screen.seeking_immortals.display.level";
    static final String METHOD_LEVEL_KEY = "screen.seeking_immortals.display.method_level";

    private static final Set<String> ATTRIBUTES = Set.of(
            "common", "neutral", "mixed", "metal", "wood", "water", "fire", "earth",
            "wind", "thunder", "ice", "light", "dark", "yin", "yang", "blood", "soul",
            "void", "space", "spatial", "time", "spirit", "earth_wind", "none",
            "water_metal_mixed");
    private static final Set<String> SCHOOLS = Set.of(
            "dao", "sword", "elemental", "demonic", "demon", "demon_path", "buddhist",
            "confucian", "ghost", "illusion", "formation", "fashi", "talisman", "puppet",
            "movement", "recovery", "secret_arts", "divine_sense", "body", "beast",
            "xuan_yin", "misc", "mixed", "neutral", "craft_alchemy", "craft_appraise",
            "craft_artifact", "defense", "elemental_fire", "elemental_ice",
            "fashi_spirit_art", "generic", "mulan_fashi", "tianlan_fashi", "text_material");
    private static final Set<String> INTERNAL_SOURCE_MARKERS = Set.of(
            "\u5360\u4f4d", "\u63a5\u7ebf", "\u6587\u672c\u6750\u6599", "\u6279\u91cf",
            "\u6536\u53e3", "\u8865\u5168", "\u539f\u8457\u6269\u5c55",
            "placeholder", "wiring", "text_material", "text material", "backfill");
    private static final Map<String, String> SCHOOL_ALIASES = Map.of(
            "alchemy", "craft_alchemy");
    private static final Map<String, String> REALM_ALIASES = Map.ofEntries(
            Map.entry("foundation", "foundation_establishment"),
            Map.entry("deity_transformation", "soul_transformation"),
            Map.entry("spirit_transformation", "soul_transformation"),
            Map.entry("void_refining", "void_refinement"),
            Map.entry("spirit_severing", "soul_transformation"),
            Map.entry("great_vehicle", "mahayana"));
    private static final Set<String> REALMS = Set.of(
            "mortal", "qi_refining", "foundation_establishment", "core_formation",
            "nascent_soul", "soul_transformation", "void_refinement", "unity", "mahayana",
            "tribulation", "true_immortal", "body_integration", "golden_immortal",
            "taiyi_golden_immortal", "dao_ancestor_band");

    private CultivationDisplayTexts() {}

    public static Component unknown() {
        return Component.translatable(UNKNOWN_KEY);
    }

    public static Component attribute(String raw) {
        String token = normalize(raw);
        if (ATTRIBUTES.contains(token)) {
            return translatedOrUnknown("screen.seeking_immortals.display.attribute." + token);
        }
        return SCHOOLS.contains(token) ? school(token) : readableOrUnknown(raw);
    }

    public static Component school(String raw) {
        String token = SCHOOL_ALIASES.getOrDefault(normalize(raw), normalize(raw));
        return SCHOOLS.contains(token)
                ? translatedOrUnknown("screen.seeking_immortals.display.school." + token)
                : readableOrUnknown(raw);
    }

    public static Component source(String raw) {
        String token = normalize(raw);
        if (SCHOOLS.contains(token)) {
            return school(token);
        }
        if (ATTRIBUTES.contains(token)) {
            return attribute(token);
        }
        String key = "screen.seeking_immortals.display.source." + token;
        if (!token.isBlank() && Language.getInstance().has(key)) {
            return Component.translatable(key);
        }
        return Component.literal(safeMetadataText(raw));
    }

    public static Component realm(String raw) {
        String token = canonicalRealmId(raw);
        return REALMS.contains(token)
                ? translatedOrUnknown("realm.seeking_immortals." + token)
                : readableOrUnknown(raw);
    }

    static String canonicalRealmId(String raw) {
        String token = normalize(raw);
        return REALM_ALIASES.getOrDefault(token, token);
    }

    public static Component level(int level) {
        return Component.translatable(LEVEL_KEY, Math.max(0, level));
    }

    public static String techniqueName(ClientTechniqueData.TechniqueSummary summary) {
        if (summary == null) {
            return unknown().getString();
        }
        return safeDisplayName(summary.name(), summary.id());
    }

    public static String methodName(TextMaterialCatalogService.MethodEntry method) {
        if (method == null) {
            return unknown().getString();
        }
        return safeDisplayName(method.display(), method.id());
    }

    /** Safe player-facing name for the enum-backed life/special skill tree. */
    public static String skillName(SkillType type) {
        if (type == null) {
            return unknown().getString();
        }
        String key = "skill.seeking_immortals." + type.name().toLowerCase(Locale.ROOT);
        if (Language.getInstance().has(key)) {
            return Component.translatable(key).getString();
        }
        String cleaned = PlayerDisplayText.sanitizeCatalogText(type.getDisplayName());
        return cleaned.isBlank() ? unknown().getString() : cleaned;
    }

    /** Resolves a realm through the same design-id aliases used by method screens. */
    public static String realmName(Realm realm) {
        if (realm == null) {
            return unknown().getString();
        }
        return realm(realm.getDesignId()).getString();
    }

    public static String sourceText(String raw) {
        return source(raw).getString();
    }

    public static String visibleSourceText(String raw) {
        if (isInternalSourceMetadata(raw)) {
            return "";
        }
        String value = sourceText(raw);
        return value.equals(unknown().getString()) ? "" : value;
    }

    static boolean isInternalSourceMetadata(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return INTERNAL_SOURCE_MARKERS.stream().anyMatch(value::contains);
    }

    public static String attributeText(String raw) {
        return attribute(raw).getString();
    }

    public static String schoolText(String raw) {
        return school(raw).getString();
    }

    public static String realmText(String raw) {
        return realm(raw).getString();
    }

    public static String safeText(String raw) {
        return readableOrUnknown(raw).getString();
    }

    static String humanizeCode(String raw) {
        String token = normalize(raw);
        if (token.isBlank()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (String part : token.split("[_\\-]+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!text.isEmpty()) {
                text.append(' ');
            }
            text.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return text.toString();
    }

    static boolean looksLikeCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String value = raw.trim();
        return value.matches("[a-z0-9]+(?:[_:\\-][a-z0-9]+)+")
                || value.matches("[a-z]+[0-9]+")
                || "unknown".equalsIgnoreCase(value)
                || "unknown_source".equalsIgnoreCase(value)
                || "true".equalsIgnoreCase(value)
                || "false".equalsIgnoreCase(value);
    }

    private static Component readableOrUnknown(String raw) {
        if (raw == null || raw.isBlank() || looksLikeCode(raw)) {
            return unknown();
        }
        return Component.literal(raw.trim());
    }

    private static String safeDisplayName(String display, String id) {
        if (display == null || display.isBlank()
                || display.equalsIgnoreCase(id == null ? "" : id)
                || looksLikeCode(display)) {
            return unknown().getString();
        }
        String trimmed = display.trim();
        String leadingToken = longestLeadingToken(trimmed);
        String localized = leadingToken.isBlank() ? trimmed : localizeLeadingCode(trimmed, leadingToken);
        boolean replacedKnownCode = !leadingToken.isBlank();
        String cloakKey = "screen.seeking_immortals.display.term.cloak";
        if (localized.toLowerCase(Locale.ROOT).contains("cloak")) {
            String cloak = Language.getInstance().has(cloakKey)
                    ? Component.translatable(cloakKey).getString() : "";
            localized = localized.replaceAll("(?i)cloak", cloak);
            replacedKnownCode = true;
        }
        if (!replacedKnownCode && containsCjk(localized) && localized.matches(".*[A-Za-z_]{2,}.*")) {
            localized = localized.replaceAll("[A-Za-z_]{2,}", "").trim();
        }
        return localized.isBlank() ? unknown().getString() : localized;
    }

    private static String safeMetadataText(String raw) {
        if (raw == null || raw.isBlank() || looksLikeCode(raw)) {
            return unknown().getString();
        }
        String value = raw.trim();
        String leadingToken = longestLeadingToken(value);
        if (!leadingToken.isBlank()) {
            value = localizeLeadingCode(value, leadingToken);
        }
        if (containsCjk(value) && value.matches(".*[A-Za-z_]{2,}.*")) {
            value = value.replaceAll("[A-Za-z][A-Za-z0-9_.:-]*", "")
                    .replaceAll("\\s+", " ")
                    .replaceAll("\\s+([/·,，。])", "$1")
                    .trim();
        }
        return value.isBlank() ? unknown().getString() : value;
    }

    private static String localizeLeadingCode(String value, String token) {
        String replacement;
        if (REALMS.contains(REALM_ALIASES.getOrDefault(token, token))) {
            replacement = realm(token).getString();
        } else if (SCHOOLS.contains(SCHOOL_ALIASES.getOrDefault(token, token))) {
            replacement = school(token).getString();
        } else {
            replacement = attribute(token).getString();
        }
        return replacement + value.substring(token.length());
    }

    private static String longestLeadingToken(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String best = "";
        for (String token : REALM_ALIASES.keySet()) {
            if (startsWithCodeToken(lower, token) && token.length() > best.length()) best = token;
        }
        for (String token : REALMS) {
            if (startsWithCodeToken(lower, token) && token.length() > best.length()) best = token;
        }
        for (String token : SCHOOL_ALIASES.keySet()) {
            if (startsWithCodeToken(lower, token) && token.length() > best.length()) best = token;
        }
        for (String token : SCHOOLS) {
            if (startsWithCodeToken(lower, token) && token.length() > best.length()) best = token;
        }
        for (String token : ATTRIBUTES) {
            if (startsWithCodeToken(lower, token) && token.length() > best.length()) best = token;
        }
        return best;
    }

    private static boolean startsWithCodeToken(String value, String token) {
        if (!value.startsWith(token)) {
            return false;
        }
        return value.length() == token.length()
                || value.charAt(token.length()) > 127
                || !Character.isLetterOrDigit(value.charAt(token.length()));
    }

    private static boolean containsCjk(String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint >= 0x4E00 && codePoint <= 0x9FFF);
    }

    private static Component translatedOrUnknown(String key) {
        return Language.getInstance().has(key) ? Component.translatable(key) : unknown();
    }

    private static String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
