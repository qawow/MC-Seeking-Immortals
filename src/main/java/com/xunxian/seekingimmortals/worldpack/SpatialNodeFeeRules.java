package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Text-material spatial_nodes ticket matrix (P0 item-backed routes).
 * Creative/instabuild never consumes. Soft lore requires are not hard-gated here.
 */
public final class SpatialNodeFeeRules {
    public static final String ALLIANCE_MERIT_TOKEN = "seeking_immortals:alliance_merit_token";
    public static final String WAR_CONTRIBUTION_TOKEN = "seeking_immortals:war_contribution_token";
    public static final String STAR_PALACE_TAX_RECEIPT = "seeking_immortals:star_palace_tax_receipt";
    public static final String DIYUAN_PERMIT = "seeking_immortals:diyuan_permit";
    public static final String IMMORTAL_JADE = "seeking_immortals:immortal_jade";
    public static final String VOID_CRYSTAL = "seeking_immortals:void_crystal";
    public static final String SPIRIT_STONE_SHARD = "seeking_immortals:spirit_stone_shard";

    private SpatialNodeFeeRules() {}

    public record Fee(String itemId, int count, String messageKey) {
        public static final Fee NONE = new Fee("", 0, "");

        public boolean present() {
            return itemId != null && !itemId.isBlank() && count > 0;
        }
    }

    /**
     * Extra portal-array fee by destination region after existing hardcoded fees.
     * Returns NONE when no additional fee applies.
     */
    public static Fee portalDestinationFee(String currentRegionId, String targetRegionId) {
        String current = normalize(currentRegionId);
        String target = normalize(targetRegionId);
        if (target.isEmpty() || target.equals(current)) {
            return Fee.NONE;
        }
        return switch (target) {
            case "spirit_fengyuan", "tianyuan" ->
                    // alliance merit already charged on tianyuan->fengyuan by existing fee;
                    // charge generic spirit-stone shard for other long-range portal hops into spirit realm.
                    "tianyuan".equals(current) && "spirit_fengyuan".equals(target)
                            ? Fee.NONE
                            : new Fee(SPIRIT_STONE_SHARD, 8, "message.seeking_immortals.worldpack.missing_spatial_fee");
            case "chaotic_sea", "star_palace", "inverse_star" ->
                    new Fee(STAR_PALACE_TAX_RECEIPT, 1, "message.seeking_immortals.worldpack.missing_spatial_fee");
            case "dajin", "great_jin_central", "wanbao" ->
                    new Fee(IMMORTAL_JADE, 1, "message.seeking_immortals.worldpack.missing_spatial_fee");
            case "mulan", "tianlan" ->
                    new Fee(WAR_CONTRIBUTION_TOKEN, 1, "message.seeking_immortals.worldpack.missing_spatial_fee");
            case "diyuan" ->
                    new Fee(DIYUAN_PERMIT, 1, "message.seeking_immortals.worldpack.missing_spatial_fee");
            case "void_palace", "blood_forbidden_land" ->
                    new Fee(VOID_CRYSTAL, 1, "message.seeking_immortals.worldpack.missing_spatial_fee");
            case "fallen_demon_valley", "demon_rift" ->
                    new Fee(ALLIANCE_MERIT_TOKEN, 1, "message.seeking_immortals.worldpack.missing_spatial_fee");
            default -> Fee.NONE;
        };
    }

    public static boolean isCreativeBypass(ServerPlayer player) {
        return player != null && player.getAbilities().instabuild;
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase();
    }
}
