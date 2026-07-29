package com.xunxian.seekingimmortals.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Per-player purchase quota (Wave52 shop depth).
 * Tracks daily counters in player persistent data.
 * 计数按游戏日（{@code dayTime / 24000}）自动重置，跨日后限购额度恢复。
 */
public final class ShopQuotaService {
    private static final String ROOT = "seeking_immortals_shop_quota";
    private static final String LAST_RESET_DAY = "lastResetDay";

    private ShopQuotaService() {}

    public static boolean canBuy(ServerPlayer player, String shopId, String entryId, int perPlayerLimit) {
        if (perPlayerLimit <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        return bought(player, shopId, entryId) < perPlayerLimit;
    }

    public static void recordBuy(ServerPlayer player, String shopId, String entryId) {
        CompoundTag root = rolledOverRoot(player);
        String key = key(shopId, entryId);
        root.putInt(key, root.getInt(key) + 1);
        player.getPersistentData().put(ROOT, root);
    }

    public static int bought(ServerPlayer player, String shopId, String entryId) {
        CompoundTag root = rolledOverRoot(player);
        player.getPersistentData().put(ROOT, root);
        return root.getInt(key(shopId, entryId));
    }

    /**
     * 返回按当前游戏日滚动后的配额标签副本：跨日时清空所有计数并记录新的一天。
     */
    private static CompoundTag rolledOverRoot(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        long currentDay = currentDay(player);
        if (!root.contains(LAST_RESET_DAY) || root.getLong(LAST_RESET_DAY) != currentDay) {
            root = new CompoundTag();
            root.putLong(LAST_RESET_DAY, currentDay);
        }
        return root;
    }

    private static long currentDay(ServerPlayer player) {
        return Math.floorDiv(player.serverLevel().getDayTime(), 24000L);
    }

    private static String key(String shopId, String entryId) {
        return (shopId == null ? "" : shopId.trim().toLowerCase(Locale.ROOT))
                + "|" + (entryId == null ? "" : entryId.trim().toLowerCase(Locale.ROOT));
    }
}
