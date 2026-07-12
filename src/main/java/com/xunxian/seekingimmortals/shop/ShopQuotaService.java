package com.xunxian.seekingimmortals.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Per-player purchase quota (Wave52 shop depth).
 * Tracks daily/monthly counters in player persistent data.
 */
public final class ShopQuotaService {
    private static final String ROOT = "seeking_immortals_shop_quota";

    private ShopQuotaService() {}

    public static boolean canBuy(ServerPlayer player, String shopId, String entryId, int perPlayerLimit) {
        if (perPlayerLimit <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        return bought(player, shopId, entryId) < perPlayerLimit;
    }

    public static void recordBuy(ServerPlayer player, String shopId, String entryId) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        String key = key(shopId, entryId);
        root.putInt(key, root.getInt(key) + 1);
        player.getPersistentData().put(ROOT, root);
    }

    public static int bought(ServerPlayer player, String shopId, String entryId) {
        return player.getPersistentData().getCompound(ROOT).getInt(key(shopId, entryId));
    }

    private static String key(String shopId, String entryId) {
        return (shopId == null ? "" : shopId.trim().toLowerCase(Locale.ROOT))
                + "|" + (entryId == null ? "" : entryId.trim().toLowerCase(Locale.ROOT));
    }
}
