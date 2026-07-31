package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight reputation store in player persistent NBT.
 * Used for spatial rep_* hard gates until a full faction reputation system lands.
 */
public final class ReputationService {
    private static final String ROOT = "seeking_immortals_reputation";
    public static final int NEUTRAL_THRESHOLD = 0;
    public static final int FRIENDLY_THRESHOLD = 10;
    public static final int HONORED_THRESHOLD = 25;

    private ReputationService() {}

    public static int get(ServerPlayer player, String factionKey) {
        if (player == null) {
            return 0;
        }
        String key = normalize(factionKey);
        if (key.isBlank()) {
            return 0;
        }
        return player.getPersistentData().getCompound(ROOT).getInt(key);
    }

    public static void set(ServerPlayer player, String factionKey, int value) {
        if (player == null) {
            return;
        }
        String key = normalize(factionKey);
        if (key.isBlank()) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        root.putInt(key, value);
        player.getPersistentData().put(ROOT, root);
    }

    public static int add(ServerPlayer player, String factionKey, int delta) {
        int next = get(player, factionKey) + delta;
        set(player, factionKey, next);
        // Q-B-5: reaching a positive reputation value for the ledger key is the server fact.
        if (next >= 1) {
            com.xunxian.seekingimmortals.quest.DetailedQuestProofService.recordReputationReached(
                    player, factionKey);
        }
        return next;
    }

    /**
     * Wave45 reputation economy loop helpers.
     * Travel / shop / quest systems call these to create a real gain path.
     */
    public static void onPortalTravel(ServerPlayer player, String regionId) {
        if (player == null) {
            return;
        }
        String region = regionId == null ? "" : regionId.trim().toLowerCase(Locale.ROOT);
        add(player, "mortal_realm", 1);
        if (region.contains("mulan") || region.contains("tianlan")) {
            add(player, "mulan", 2);
        } else if (region.contains("chaotic") || region.contains("star") || region.contains("void")) {
            add(player, "chaotic_sea", 2);
        } else if (region.contains("dajin") || region.contains("kunwu") || region.contains("jin")) {
            add(player, "dajin", 2);
        } else if (region.contains("tianyuan") || region.contains("spirit") || region.contains("fengyuan")) {
            add(player, "tianyuan", 2);
        } else if (region.contains("demon") || region.contains("yin") || region.contains("nether")) {
            add(player, "demonic_path", 2);
        } else if (region.contains("barbarian") || region.contains("wasteland")) {
            add(player, "barbarian_wasteland", 2);
        }
    }

    public static void onShopPurchase(ServerPlayer player, String shopId) {
        if (player == null) {
            return;
        }
        String shop = shopId == null ? "" : shopId.trim().toLowerCase(Locale.ROOT);
        add(player, "merchant_guild", 1);
        if (shop.contains("sect") || shop.contains("qinglan") || shop.contains("yue")) {
            add(player, "dajin", 1);
        } else if (shop.contains("star") || shop.contains("chaotic") || shop.contains("wanbao")) {
            add(player, "chaotic_sea", 1);
        } else if (shop.contains("ghost") || shop.contains("yin")) {
            add(player, "demonic_path", 1);
        }
    }

    public static void onQuestComplete(ServerPlayer player, String chainId) {
        if (player == null) {
            return;
        }
        String id = chainId == null ? "" : chainId.trim().toLowerCase(Locale.ROOT);
        add(player, "mortal_realm", 2);
        if (id.contains("mulan") || id.contains("war")) {
            add(player, "mulan", 4);
        } else if (id.contains("ghost") || id.contains("demon") || id.contains("yin")) {
            add(player, "demonic_path", 4);
        } else if (id.contains("chaotic") || id.contains("star") || id.contains("void")) {
            add(player, "chaotic_sea", 4);
        } else if (id.contains("dajin") || id.contains("kunwu") || id.contains("sect")) {
            add(player, "dajin", 4);
        } else if (id.contains("spirit") || id.contains("tianyuan") || id.contains("ascension")) {
            add(player, "tianyuan", 4);
        }
    }

    /**
     * Wave46 reputation discount table for shops.
     * Returns multiplier in (0,1], lower is cheaper.
     */
    public static double shopDiscountMultiplier(ServerPlayer player, String shopId) {
        if (player == null) {
            return 1.0D;
        }
        String shop = shopId == null ? "" : shopId.trim().toLowerCase(Locale.ROOT);
        int score = get(player, "merchant_guild");
        if (shop.contains("sect") || shop.contains("qinglan") || shop.contains("yue") || shop.contains("huangfeng")) {
            score = Math.max(score, get(player, "dajin"));
        } else if (shop.contains("star") || shop.contains("chaotic") || shop.contains("wanbao") || shop.contains("inverse")) {
            score = Math.max(score, get(player, "chaotic_sea"));
        } else if (shop.contains("ghost") || shop.contains("yin") || shop.contains("nether")) {
            score = Math.max(score, get(player, "demonic_path"));
        } else if (shop.contains("mulan") || shop.contains("tianlan")) {
            score = Math.max(score, get(player, "mulan"));
        } else if (shop.contains("tianyuan") || shop.contains("merit")) {
            score = Math.max(score, get(player, "tianyuan"));
        }
        if (score >= HONORED_THRESHOLD) {
            return 0.80D;
        }
        if (score >= FRIENDLY_THRESHOLD) {
            return 0.95D;
        }
        return 1.0D;
    }

    public static String discountLabel(ServerPlayer player, String shopId) {
        double m = shopDiscountMultiplier(player, shopId);
        if (m <= 0.80D) {
            return "honored-20%";
        }
        if (m < 1.0D) {
            return "friendly-5%";
        }
        return "none";
    }

    public static boolean meets(ServerPlayer player, String requireToken) {
        ParsedRep parsed = parse(requireToken);
        if (parsed == null) {
            return true;
        }
        return get(player, parsed.faction()) >= parsed.minValue();
    }

    public static Map<String, Integer> snapshot(ServerPlayer player) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (player == null) {
            return map;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        for (String key : root.getAllKeys()) {
            map.put(key, root.getInt(key));
        }
        return map;
    }

    public static List<String> sample(ServerPlayer player, int limit) {
        List<String> list = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> entry : snapshot(player).entrySet()) {
            list.add(entry.getKey() + "=" + entry.getValue());
            if (++i >= Math.max(1, limit)) {
                break;
            }
        }
        return list;
    }

    public static void notifyStatus(ServerPlayer player, String factionKey) {
        if (player == null) {
            return;
        }
        String key = normalize(factionKey);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.reputation.status", key, get(player, key)), false);
    }

    public record ParsedRep(String faction, int minValue) {}

    public static ParsedRep parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String lower = token.trim().toLowerCase(Locale.ROOT);
        if (!lower.startsWith("rep_") && !lower.contains("reputation") && !lower.contains("rep.")) {
            // also accept star_palace_neutral style without prefix if explicitly reputation-like
            if (!(lower.contains("neutral") || lower.contains("friendly") || lower.contains("honored"))) {
                return null;
            }
        }
        String body = lower;
        if (body.startsWith("rep_")) {
            body = body.substring(4);
        } else if (body.startsWith("rep.")) {
            body = body.substring(4);
        }
        int min = NEUTRAL_THRESHOLD;
        if (body.endsWith("_honored") || body.contains("honored")) {
            min = HONORED_THRESHOLD;
            body = body.replace("_honored", "").replace("honored", "");
        } else if (body.endsWith("_friendly") || body.contains("friendly")) {
            min = FRIENDLY_THRESHOLD;
            body = body.replace("_friendly", "").replace("friendly", "");
        } else if (body.endsWith("_neutral") || body.contains("neutral")) {
            min = NEUTRAL_THRESHOLD;
            body = body.replace("_neutral", "").replace("neutral", "");
        }
        body = body.replaceAll("^_+|_+$", "");
        if (body.isBlank()) {
            body = "generic";
        }
        return new ParsedRep(body, min);
    }

    private static String normalize(String key) {
        if (key == null) {
            return "";
        }
        return key.trim().toLowerCase(Locale.ROOT)
                .replace("seeking_immortals:", "")
                .replace(' ', '_')
                .replaceAll("^rep[_\\.]", "");
    }
}
