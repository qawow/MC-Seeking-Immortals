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
        Realm resolved = Realm.fromDesignId(original);
        String token = resolved == null ? original.toLowerCase(Locale.ROOT)
                : resolved.name().toLowerCase(Locale.ROOT);
        String key = "realm.seeking_immortals." + token;
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
        return unknownLabel();
    }

    public static Component tag(String tagCode) {
        String code = safe(tagCode).toLowerCase(Locale.ROOT);
        String key = "artifact.tag.seeking_immortals." + code;
        if (hasKey(key)) {
            return Component.translatable(key);
        }
        return unknownLabel();
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
        return Component.literal("未知效果");
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
        Realm realm = Realm.fromDesignId(code);
        return realm == null ? "未知境界" : realm.getDisplayName();
    }

    /** Never expose an authored snake_case id when a translation key is missing. */
    private static Component unknownLabel() {
        String key = "artifact.type.seeking_immortals.unknown";
        return hasKey(key) ? Component.translatable(key) : Component.literal("未知");
    }

    private static String safe(String raw) {
        return raw == null ? "" : raw.trim();
    }

}
