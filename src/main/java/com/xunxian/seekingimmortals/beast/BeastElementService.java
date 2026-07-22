package com.xunxian.seekingimmortals.beast;

import java.util.Locale;
import java.util.Set;

/** Bootstrap-free normalization shared by bestiary data, entities, renderers, and resource tests. */
public final class BeastElementService {
    public static final Set<String> SUPPORTED_ELEMENTS = Set.of(
            "neutral", "fire", "ice", "water", "thunder", "poison", "wood", "earth",
            "metal", "wind", "soul", "blood", "illusion", "mixed", "void");

    private BeastElementService() {}

    public static String normalize(String rawElement, String beastId) {
        String raw = normalizeId(rawElement);
        if (SUPPORTED_ELEMENTS.contains(raw)) {
            return raw;
        }
        if ("yin".equals(raw)) {
            return "soul";
        }
        if ("earth_wind".equals(raw)) {
            return "earth";
        }
        String source = raw + " " + normalizeId(beastId);
        if (containsAny(source, "fire", "flame", "huo", "yang", "火")) return "fire";
        if (containsAny(source, "ice", "frost", "cold", "bing", "han", "冰")) return "ice";
        if (containsAny(source, "water", "ocean", "shui", "hai", "水")) return "water";
        if (containsAny(source, "thunder", "lightning", "lei", "雷")) return "thunder";
        if (containsAny(source, "poison", "toxic", "du_", "_du", "毒")) return "poison";
        if (containsAny(source, "wood", "plant", "mu_", "_mu", "木")) return "wood";
        if (containsAny(source, "earth", "stone", "rock", "tu_", "shi_", "土")) return "earth";
        if (containsAny(source, "metal", "gold", "jin_", "金")) return "metal";
        if (containsAny(source, "wind", "feng", "风")) return "wind";
        if (containsAny(source, "soul", "ghost", "yin", "hun", "gui", "魂", "阴")) return "soul";
        if (containsAny(source, "blood", "xue", "血")) return "blood";
        if (containsAny(source, "illusion", "mirage", "phantom", "dream", "幻", "蜃")) return "illusion";
        if (containsAny(source, "mixed", "fusion", "hybrid", "five_element", "五行")) return "mixed";
        if (containsAny(source, "void", "abyss", "nihility", "虚空")) return "void";
        return "neutral";
    }

    private static boolean containsAny(String source, String... tokens) {
        for (String token : tokens) {
            if (source.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
