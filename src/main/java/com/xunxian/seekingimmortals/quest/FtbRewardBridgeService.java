package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

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
    /** Shared legacy bridge tag; the authority ledger always wins. */
    public static final String ROOT_TAG = "seeking_immortals_ftb_reward_bridge";

    private FtbRewardBridgeService() {}

    public static void onTextQuestFinished(ServerPlayer player, String chainId) {
        if (player == null || chainId == null || chainId.isBlank()) {
            return;
        }
        String id = chainId.trim().toLowerCase(Locale.ROOT);
        if (!TextQuestChainService.hasAuthorityReward(player, id)) {
            // Never invent or burn the authority ledger from this bridge: the real grant
            // path (TextQuestChainService.advance) must pay first. Log so a future legacy
            // call site is visible instead of silently swallowing the finale reward.
            SeekingImmortalsMod.LOGGER.warn("FTB reward bridge called without an authority grant for {}", id);
            return;
        }
        var legacy = player.getPersistentData().getCompound(ROOT_TAG).copy();
        legacy.putBoolean(id, true);
        player.getPersistentData().put(ROOT_TAG, legacy);
    }
}
