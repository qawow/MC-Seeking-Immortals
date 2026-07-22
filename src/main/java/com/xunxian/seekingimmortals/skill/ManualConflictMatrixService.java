package com.xunxian.seekingimmortals.skill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * M02: loads {@code manual_conflict_matrix_v100.json} and gates method learning on
 * D/F conflict pairs against already-learned methods.
 */
public final class ManualConflictMatrixService {
    private static final Snapshot BUILTIN = loadBuiltin();

    private ManualConflictMatrixService() {}

    public record Pair(String aId, String bId, String aDisplay, String bDisplay, String level, String note) {}

    public record GateResult(boolean allowed, String messageKey, Object[] args) {
        public static GateResult ok() {
            return new GateResult(true, "", new Object[0]);
        }

        public static GateResult deny(String messageKey, Object... args) {
            return new GateResult(false, messageKey, args == null ? new Object[0] : args);
        }
    }

    public static int pairCount() {
        return BUILTIN.pairs().size();
    }

    public static List<Pair> pairs() {
        return BUILTIN.pairs();
    }

    public static int primaryManualCap() {
        return BUILTIN.primaryManualCap();
    }

    /**
     * Deny learning {@code methodId} when it forms a D/F pair with any already-learned method,
     * or when primary-manual stack cap would be exceeded for conflicting primaries.
     */
    public static GateResult canLearnMethod(ServerPlayer player, String methodId) {
        if (player == null || methodId == null || methodId.isBlank()) {
            return GateResult.ok();
        }
        if (player.getAbilities().instabuild) {
            return GateResult.ok();
        }
        String id = methodId.trim().toLowerCase(Locale.ROOT);
        List<String> learned = learnedMethodIds(player);
        for (Pair pair : BUILTIN.pairs()) {
            String other = otherOf(pair, id);
            if (other == null) {
                continue;
            }
            if (!learned.contains(other)) {
                continue;
            }
            String level = pair.level() == null ? "" : pair.level().trim().toUpperCase(Locale.ROOT);
            if ("F".equals(level) || "D".equals(level)) {
                String display = methodDisplay(methodId, pair);
                String otherDisplay = methodDisplay(other, pair);
                return GateResult.deny("message.seeking_immortals.method.conflict",
                        display, otherDisplay, conflictLevelDisplay(level), safeNote(pair.note()));
            }
        }
        return GateResult.ok();
    }

    private static String otherOf(Pair pair, String methodId) {
        if (methodId.equals(pair.aId())) {
            return pair.bId();
        }
        if (methodId.equals(pair.bId())) {
            return pair.aId();
        }
        return null;
    }

    private static List<String> learnedMethodIds(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        for (TextMaterialCatalogService.MethodEntry method : TextMaterialCatalogService.builtin().methods().values()) {
            if (method != null && ManualCatalogService.hasLearnedMethod(player, method.id())) {
                out.add(method.id().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    private record Snapshot(List<Pair> pairs, int primaryManualCap) {}

    private static Snapshot loadBuiltin() {
        Map<String, String> displayToId = new LinkedHashMap<>();
        for (TextMaterialCatalogService.MethodEntry method : TextMaterialCatalogService.builtin().methods().values()) {
            if (method == null || method.id() == null || method.id().isBlank()) {
                continue;
            }
            displayToId.put(normalizeDisplay(method.display()), method.id().toLowerCase(Locale.ROOT));
            displayToId.putIfAbsent(normalizeDisplay(method.id()), method.id().toLowerCase(Locale.ROOT));
        }

        List<Pair> pairs = new ArrayList<>();
        int primaryCap = 1;
        JsonObject root = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/manual_conflict_matrix_v100.json");
        if (root != null) {
            if (root.has("stack_caps") && root.get("stack_caps").isJsonObject()) {
                JsonObject caps = root.getAsJsonObject("stack_caps");
                if (caps.has("primary_manual") && caps.get("primary_manual").isJsonPrimitive()) {
                    try {
                        primaryCap = Math.max(1, caps.get("primary_manual").getAsInt());
                    } catch (Exception ignored) {
                        primaryCap = 1;
                    }
                }
            }
            JsonArray array = root.has("pairs") && root.get("pairs").isJsonArray()
                    ? root.getAsJsonArray("pairs") : new JsonArray();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String aDisplay = str(o, "a");
                String bDisplay = str(o, "b");
                String level = str(o, "level");
                String note = str(o, "note");
                String aId = resolveId(displayToId, aDisplay);
                String bId = resolveId(displayToId, bDisplay);
                if (aId.isBlank() || bId.isBlank()) {
                    // Keep unresolved display pairs out of hard gates; they remain soft catalog data.
                    continue;
                }
                pairs.add(new Pair(aId, bId, aDisplay, bDisplay, level, note));
            }
        }
        return new Snapshot(List.copyOf(pairs), primaryCap);
    }

    private static String resolveId(Map<String, String> displayToId, String display) {
        if (display == null || display.isBlank()) {
            return "";
        }
        String needle = normalizeDisplay(display);
        String direct = displayToId.get(needle);
        if (direct != null) {
            return direct;
        }
        // Explicit aliases for matrix labels that are not exact method display names.
        String alias = switch (needle) {
            case "木系长春", "长春" -> "changchun_gong";
            case "本体青元", "青元清修", "正道剑修清名" -> "qingyuan_sword_art";
            case "玄阴经全修", "玄阴经煞丹术" -> "xuan_yin_art";
            case "第二元婴魔功", "第二元婴" -> "dayan_art";
            case "佛门正统清修", "佛门戒杀金身" -> "dajin_buddhist_vajra_art".equals(
                    displayToId.getOrDefault("大晋佛门金刚法", ""))
                    ? "dajin_buddhist_vajra_art" : firstContaining(displayToId, "佛");
            case "合欢双修正统", "颠凤培元功", "男修夺元阴" -> firstContaining(displayToId, "合欢");
            case "鬼道炼尸法" -> firstContaining(displayToId, "鬼");
            case "纯火霸道功" -> firstContaining(displayToId, "烈焰");
            case "星宫正统" -> firstContaining(displayToId, "星宫");
            case "天庭体系功法" -> firstContaining(displayToId, "天庭");
            case "百脉炼宝决" -> firstContaining(displayToId, "百脉");
            case "淬骨诀" -> firstContaining(displayToId, "淬骨");
            case "弄焰诀" -> firstContaining(displayToId, "弄焰");
            case "明王诀" -> firstContaining(displayToId, "明王");
            case "换形诀" -> firstContaining(displayToId, "换形");
            case "血影遁" -> firstContaining(displayToId, "血影");
            default -> "";
        };
        if (!alias.isBlank()) {
            return alias;
        }
        // Fuzzy: longest contains match on display to avoid short false positives.
        String bestId = "";
        int bestLen = 0;
        for (Map.Entry<String, String> entry : displayToId.entrySet()) {
            String key = entry.getKey();
            if (key.isBlank()) {
                continue;
            }
            if (key.contains(needle) || needle.contains(key)) {
                int score = Math.min(key.length(), needle.length());
                if (score > bestLen) {
                    bestLen = score;
                    bestId = entry.getValue();
                }
            }
        }
        return bestId;
    }

    private static String firstContaining(Map<String, String> displayToId, String token) {
        String needle = normalizeDisplay(token);
        for (Map.Entry<String, String> entry : displayToId.entrySet()) {
            if (entry.getKey().contains(needle)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private static String normalizeDisplay(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private static String methodDisplay(String methodId, Pair pair) {
        String catalogDisplay = TextMaterialCatalogService.builtin().findMethod(methodId)
                .map(TextMaterialCatalogService.MethodEntry::display)
                .orElse("");
        String safeCatalogDisplay = PlayerDisplayText.sanitizeCatalogText(catalogDisplay);
        if (!safeCatalogDisplay.isBlank()) {
            return safeCatalogDisplay;
        }
        if (pair != null && methodId != null) {
            if (methodId.equalsIgnoreCase(pair.aId())) {
                String display = PlayerDisplayText.sanitizeCatalogText(pair.aDisplay());
                if (!display.isBlank()) {
                    return display;
                }
            }
            if (methodId.equalsIgnoreCase(pair.bId())) {
                String display = PlayerDisplayText.sanitizeCatalogText(pair.bDisplay());
                if (!display.isBlank()) {
                    return display;
                }
            }
        }
        return "未知功法";
    }

    private static String conflictLevelDisplay(String level) {
        return switch (level == null ? "" : level.trim().toUpperCase(Locale.ROOT)) {
            case "F" -> "绝对冲突";
            case "D" -> "严重冲突";
            default -> "冲突";
        };
    }

    private static String safeNote(String note) {
        String display = PlayerDisplayText.sanitizeCatalogText(note);
        return display.isBlank() ? "无补充说明" : display;
    }

    private static String str(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }

    private static JsonObject readJson(String path) {
        ClassLoader loader = ManualConflictMatrixService.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonElement element = JsonParser.parseReader(reader);
                return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load conflict matrix from {}", path, exception);
            return null;
        }
    }
}
