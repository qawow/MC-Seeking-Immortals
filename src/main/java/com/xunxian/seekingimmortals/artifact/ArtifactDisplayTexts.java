package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Localizes raw artifact catalog codes (realm / type / tag / effect) for UI tooltips.
 */
public final class ArtifactDisplayTexts {
    private ArtifactDisplayTexts() {}

    public static Component realm(String realmCode) {
        String original = safe(realmCode);
        String key = "realm.seeking_immortals." + original.toLowerCase(Locale.ROOT);
        if (hasKey(key)) {
            return Component.translatable(key);
        }
        return Component.literal(resolveRealmFallback(original));
    }

    public static Component type(String typeCode) {
        String code = safe(typeCode).toLowerCase(Locale.ROOT);
        String key = "artifact.type.seeking_immortals." + code;
        if (hasKey(key)) {
            return Component.translatable(key);
        }
        return Component.literal(humanize(code));
    }

    public static Component tag(String tagCode) {
        String code = safe(tagCode).toLowerCase(Locale.ROOT);
        String key = "artifact.tag.seeking_immortals." + code;
        if (hasKey(key)) {
            return Component.translatable(key);
        }
        return Component.literal(humanize(code));
    }

    public static Component effect(String effectCode) {
        String code = safe(effectCode).toLowerCase(Locale.ROOT);
        if (code.isEmpty()) {
            return Component.literal("");
        }
        String key = "artifact.effect.seeking_immortals." + code;
        if (hasKey(key)) {
            return Component.translatable(key);
        }
        return Component.literal(humanize(code));
    }

    public static Component tagsJoined(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Component.literal("");
        }
        MutableComponent joined = Component.empty();
        boolean first = true;
        for (String value : tags) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!first) {
                joined.append(Component.literal("、"));
            }
            joined.append(tag(value));
            first = false;
        }
        return joined;
    }

    public static String realmText(String realmCode) {
        return realm(realmCode).getString();
    }

    public static String typeText(String typeCode) {
        return type(typeCode).getString();
    }

    public static String effectText(String effectCode) {
        return effect(effectCode).getString();
    }

    public static String tagsText(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String value : tags) {
            if (value != null && !value.isBlank()) {
                parts.add(tag(value).getString());
            }
        }
        return String.join("、", parts);
    }

    private static boolean hasKey(String key) {
        Language language = Language.getInstance();
        return language != null && language.has(key);
    }

    private static String resolveRealmFallback(String realmCode) {
        String code = safe(realmCode).toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            return "未知";
        }
        try {
            return Realm.valueOf(code).getDisplayName();
        } catch (IllegalArgumentException ignored) {
            // fall through to design-id / alias mapping
        }
        for (Realm realm : Realm.values()) {
            if (code.equalsIgnoreCase(realm.getDesignId()) || code.equalsIgnoreCase(realm.name())) {
                return realm.getDisplayName();
            }
        }
        return switch (code) {
            case "FOUNDATION" -> Realm.FOUNDATION_ESTABLISHMENT.getDisplayName();
            case "DEITY_TRANSFORMATION", "SPIRIT_TRANSFORMATION", "SOUL_TRANSFORMATION" ->
                    Realm.SOUL_TRANSFORMATION.getDisplayName();
            case "GREAT_VEHICLE", "MAHAYANA" -> Realm.MAHAYANA.getDisplayName();
            case "SPIRIT_SEVERING" -> Realm.UNITY.getDisplayName();
            case "VOID_REFINING", "VOID_REFINEMENT" -> Realm.VOID_REFINEMENT.getDisplayName();
            default -> humanize(code.toLowerCase(Locale.ROOT));
        };
    }

    private static String safe(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static String humanize(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        String[] parts = code.toLowerCase(Locale.ROOT).split("[_\\-]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part);
        }
        return sb.toString();
    }
}
