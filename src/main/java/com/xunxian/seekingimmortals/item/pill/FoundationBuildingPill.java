package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.CultivationProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FoundationBuildingPill extends BasePillItem {
    public FoundationBuildingPill(Properties properties, PillQuality quality) {
        super(properties, PillType.FOUNDATION_BUILDING, quality);
    }

    @Override
    protected boolean consumePill(ServerPlayer player) {
        return player.getCapability(CultivationProvider.CULTIVATION).map(cultivation -> {
            BreakthroughService.HandBreakthroughAidResult result =
                    BreakthroughService.tryApplyHandConsumedBreakthroughAid(player, cultivation, getDefaultInstance(), false);
            if (result == BreakthroughService.HandBreakthroughAidResult.APPLIED) return true;
            if (result == BreakthroughService.HandBreakthroughAidResult.NOT_APPLICABLE) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.aid_not_matching"), true);
            }
            return false;
        }).orElse(false);
    }
}
