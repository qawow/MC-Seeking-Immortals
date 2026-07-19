package com.xunxian.seekingimmortals.sect;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.quest.QuestProgress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SectContributionTokenService {
    public static final int CONTRIBUTION_PER_TOKEN = 1;

    public enum RedemptionResult {
        SUCCESS,
        NO_PROGRESS,
        NO_SECT,
        CAPPED
    }

    private SectContributionTokenService() {}

    public static boolean redeem(ServerPlayer player) {
        var cultivation = CultivationHelper.get(player).orElse(null);
        if (cultivation == null) {
            return false;
        }
        QuestProgress progress = cultivation.getSevenMysteriesQuest();
        RedemptionResult result = redeem(progress);
        if (result == RedemptionResult.SUCCESS) {
            SyncCultivationDataPacket.send(player, cultivation);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.sect_contribution_token.redeemed",
                    CONTRIBUTION_PER_TOKEN,
                    progress.getContribution()), true);
            return true;
        }
        String messageKey = result == RedemptionResult.CAPPED
                ? "message.seeking_immortals.sect_contribution_token.capped"
                : "message.seeking_immortals.sect_contribution_token.no_sect";
        player.displayClientMessage(Component.translatable(messageKey), true);
        return false;
    }

    static RedemptionResult redeem(QuestProgress progress) {
        if (progress == null) {
            return RedemptionResult.NO_PROGRESS;
        }
        if (SectDefinitionService.find(progress.getSectId()).isEmpty()) {
            return RedemptionResult.NO_SECT;
        }
        if (progress.getContribution() >= Integer.MAX_VALUE) {
            return RedemptionResult.CAPPED;
        }
        progress.addContribution(CONTRIBUTION_PER_TOKEN);
        return RedemptionResult.SUCCESS;
    }
}
