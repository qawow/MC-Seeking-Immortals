package com.xunxian.seekingimmortals.quest;

import net.minecraft.server.level.ServerPlayer;

/**
 * Soft FTB/in-mod reward bridge.
 * Wave454: shares the authority reward ledger with TextQuestChainService so
 * finale rewards cannot be double-granted by soft table + FTB bridge.
 * Wave457: no independent item pay table — only syncs legacy FTB tag after
 * authority grant. Does not depend on FTB API classes.
 * Wave482: packaged FTB item tasks now consume items (consume_items=true);
 * this bridge remains ledger-only and does not invent a second pay table.
 */
public final class FtbRewardBridgeService {
    private static final String ROOT = "seeking_immortals_ftb_reward_bridge";

    private FtbRewardBridgeService() {}

    public static void onTextQuestFinished(ServerPlayer player, String chainId) {
        if (player == null || chainId == null || chainId.isBlank()) {
            return;
        }
        String id = chainId.trim().toLowerCase();
        // Authority path always grants first; bridge only mirrors the ledger tag.
        if (!TextQuestChainService.hasAuthorityReward(player, id)) {
            // Extremely rare legacy call site without prior grant: mark only, do not invent rewards.
            TextQuestChainService.markAuthorityReward(player, id);
        }
        var legacy = player.getPersistentData().getCompound(ROOT).copy();
        legacy.putBoolean(id, true);
        player.getPersistentData().put(ROOT, legacy);
    }
}
