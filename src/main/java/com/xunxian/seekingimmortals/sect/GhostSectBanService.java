package com.xunxian.seekingimmortals.sect;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.worldpack.ReputationService;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Ghost cultivator ban rules (M08) linked with M01 ghost path.
 * Server-authoritative reject/hunt marks for righteous and listed sects.
 */
public final class GhostSectBanService {
    private static final String HUNT_FLAG = "seeking_immortals_ghost_hunt";
    private static final String DETECTED_FLAG = "seeking_immortals_ghost_detected";

    private static final Snapshot BUILTIN = loadBuiltin();

    private GhostSectBanService() {}

    public static Snapshot builtin() {
        return BUILTIN;
    }

    public static boolean isGhostPath(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player)
                .map(PlayerCultivation::isGhostPath)
                .orElse(false)
                || ProgressionGateApi.meetsPath(player, "ghost")
                || ProgressionGateApi.meetsPath(player, "ghost_cultivator");
    }

    public static boolean isJoinRejected(ServerPlayer player, String sectId) {
        return isJoinRejected((net.minecraft.world.entity.player.Player) player, sectId);
    }

    public static boolean isJoinRejected(net.minecraft.world.entity.player.Player player, String sectId) {
        if (player == null) {
            return false;
        }
        boolean ghost;
        if (player instanceof ServerPlayer serverPlayer) {
            ghost = isGhostPath(serverPlayer);
        } else {
            ghost = ProgressionGateApi.meetsPath(player, "ghost")
                    || ProgressionGateApi.meetsPath(player, "ghost_cultivator");
        }
        if (!ghost) {
            return false;
        }
        String id = SectDefinitionService.canonicalizeSectId(sectId);
        if (BUILTIN.bannedSectIds().contains(normalize(id))
                || BUILTIN.bannedSectIds().contains(normalize(sectId))) {
            return true;
        }
        // Default: righteous-aligned corpus sects reject ghost path unless explicitly ghost-friendly.
        Optional<SectMasterDataService.SectMaster> master = SectMasterDataService.find(id);
        if (master.isPresent()) {
            String alignment = normalize(master.get().alignment());
            if (alignment.contains("righteous") || alignment.contains("正") || alignment.contains("buddha")
                    || alignment.contains("佛")) {
                return !BUILTIN.friendlySectIds().contains(normalize(id));
            }
        }
        return BUILTIN.onDetected().containsKey(normalize(id))
                || BUILTIN.onDetected().containsKey(normalize(sectId));
    }

    public static boolean isShopDenied(ServerPlayer player, String shopId) {
        if (!isGhostPath(player)) {
            return false;
        }
        String shop = normalize(shopId);
        for (String denied : BUILTIN.shopDenied()) {
            if (shop.equals(normalize(denied)) || shop.contains(normalize(denied)) || normalize(denied).contains(shop)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isQuestBlocked(String questOrHookId) {
        String id = normalize(questOrHookId);
        if (id.isBlank()) {
            return false;
        }
        for (String blocked : BUILTIN.questBlocks()) {
            String b = normalize(blocked);
            if (id.equals(b) || id.contains(b) || b.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Apply detection consequences: reputation penalties + hunt mark.
     */
    public static void markDetected(ServerPlayer player, String reason) {
        if (player == null || !isGhostPath(player)) {
            return;
        }
        player.getPersistentData().putBoolean(DETECTED_FLAG, true);
        player.getPersistentData().putBoolean(HUNT_FLAG, true);
        player.getPersistentData().putString(HUNT_FLAG + "_reason", reason == null ? "" : reason);
        for (Map.Entry<String, DetectedPenalty> entry : BUILTIN.onDetected().entrySet()) {
            String factionKey = ReputationUnlockService.reputationKey(entry.getKey());
            ReputationService.add(player, factionKey, entry.getValue().repDelta());
            // Also write sect-local key for fine-grained gates.
            ReputationService.add(player, normalize(entry.getKey()), entry.getValue().repDelta());
        }
    }

    public static boolean hasHuntMark(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(HUNT_FLAG);
    }

    public static boolean isDetected(ServerPlayer player) {
        return player != null && player.getPersistentData().getBoolean(DETECTED_FLAG);
    }

    public static void clearHuntMark(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.getPersistentData().remove(HUNT_FLAG);
        player.getPersistentData().remove(HUNT_FLAG + "_reason");
        player.getPersistentData().remove(DETECTED_FLAG);
    }

    public static String tribunalEventId() {
        return BUILTIN.tribunalEvent();
    }

    private static Snapshot loadBuiltin() {
        Set<String> banned = new LinkedHashSet<>();
        Set<String> friendly = new LinkedHashSet<>();
        Map<String, DetectedPenalty> onDetected = new LinkedHashMap<>();
        List<String> questBlocks = new ArrayList<>();
        List<String> shopDenied = new ArrayList<>();
        List<String> detectionTags = new ArrayList<>();
        List<String> hooks = new ArrayList<>();
        List<String> mitigation = new ArrayList<>();
        String tribunal = "righteous_sect_ghost_hunt";

        JsonObject banRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/ghost_sect_ban_rules.json");
        if (banRoot != null) {
            detectionTags.addAll(stringList(banRoot.get("detection_tags")));
            hooks.addAll(stringList(banRoot.get("hooks")));
        }

        JsonObject pathRoot = readJson("data/" + SeekingImmortalsMod.MODID + "/text_material/ghost_cultivation_path.json");
        if (pathRoot != null) {
            if (pathRoot.has("karma") && pathRoot.get("karma").isJsonObject()) {
                JsonObject karma = pathRoot.getAsJsonObject("karma");
                for (String sect : stringList(karma.get("sect_ban"))) {
                    banned.add(SectDefinitionService.canonicalizeSectId(sect));
                    banned.add(normalize(sect));
                }
            }
            if (pathRoot.has("sect_ban_consequences") && pathRoot.get("sect_ban_consequences").isJsonObject()) {
                JsonObject cons = pathRoot.getAsJsonObject("sect_ban_consequences");
                if (cons.has("on_detected") && cons.get("on_detected").isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : cons.getAsJsonObject("on_detected").entrySet()) {
                        String sect = SectDefinitionService.canonicalizeSectId(entry.getKey());
                        banned.add(sect);
                        banned.add(normalize(entry.getKey()));
                        int rep = -50;
                        String action = "";
                        if (entry.getValue().isJsonObject()) {
                            JsonObject o = entry.getValue().getAsJsonObject();
                            if (o.has("rep")) {
                                rep = o.get("rep").getAsInt();
                            }
                            action = str(o, "action");
                        }
                        onDetected.put(sect, new DetectedPenalty(sect, rep, action));
                    }
                }
                questBlocks.addAll(stringList(cons.get("quest_blocks")));
                shopDenied.addAll(stringList(cons.get("shop_denied")));
                tribunal = firstNonBlank(str(cons, "tribunal_event"), tribunal);
                mitigation.addAll(stringList(cons.get("mitigation")));
            }
        }

        // Known ghost-friendly / cover factions from corpus.
        friendly.add("guiling_gate");
        friendly.add("ghost_spirit_gate");
        friendly.add("yin_luo_hall");
        friendly.add("xuewu_sect");
        friendly.add("tianmo_sect");
        friendly.add("moyan_gate");
        friendly.add("tiansha_sect");
        friendly.add("qingluo_sect");

        return new Snapshot(
                Collections.unmodifiableSet(banned),
                Collections.unmodifiableSet(friendly),
                Collections.unmodifiableMap(onDetected),
                List.copyOf(questBlocks),
                List.copyOf(shopDenied),
                List.copyOf(detectionTags),
                List.copyOf(hooks),
                List.copyOf(mitigation),
                tribunal);
    }

    private static List<String> stringList(JsonElement element) {
        List<String> list = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return list;
        }
        if (element.isJsonArray()) {
            for (JsonElement el : element.getAsJsonArray()) {
                if (el.isJsonPrimitive()) {
                    list.add(el.getAsString());
                }
            }
        } else if (element.isJsonPrimitive()) {
            list.add(element.getAsString());
        }
        return list;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = GhostSectBanService.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception exception) {
            SeekingImmortalsMod.LOGGER.warn("Failed to load ghost ban {}", path, exception);
            return null;
        }
    }

    private static String str(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (Exception ignored) {
            return String.valueOf(object.get(key));
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record DetectedPenalty(String sectId, int repDelta, String action) {}

    public record Snapshot(Set<String> bannedSectIds,
                           Set<String> friendlySectIds,
                           Map<String, DetectedPenalty> onDetected,
                           List<String> questBlocks,
                           List<String> shopDenied,
                           List<String> detectionTags,
                           List<String> hooks,
                           List<String> mitigation,
                           String tribunalEvent) {}
}
