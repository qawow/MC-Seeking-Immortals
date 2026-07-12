package com.xunxian.seekingimmortals.quest;

import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Soft FTB/in-mod reward bridge (Wave50).
 * Grants registered carriers when text quest finale completes; does not depend on FTB API classes.
 */
public final class FtbRewardBridgeService {
    private static final String ROOT = "seeking_immortals_ftb_reward_bridge";

    private FtbRewardBridgeService() {}

    public static void onTextQuestFinished(ServerPlayer player, String chainId) {
        if (player == null || chainId == null || chainId.isBlank()) {
            return;
        }
        var tag = player.getPersistentData().getCompound(ROOT).copy();
        if (tag.getBoolean(chainId)) {
            return;
        }
        ItemStack reward = switch (chainId) {
            case "huangfeng_cultivation_path" -> new ItemStack(ModItems.FOUNDATION_BUILDING_PILL_LOW.get(), 1);
            case "ghost_path" -> new ItemStack(ModItems.YIN_STONE.get(), 4);
            case "mulan_tianlan_war" -> new ItemStack(ModItems.WAR_CONTRIBUTION_TOKEN.get(), 1);
            default -> new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), 8);
        };
        if (!player.getInventory().add(reward.copy())) {
            player.drop(reward.copy(), false);
        }
        tag.putBoolean(chainId, true);
        player.getPersistentData().put(ROOT, tag);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.ftb_bridge.reward", chainId, reward.getHoverName()), true);
    }
}
