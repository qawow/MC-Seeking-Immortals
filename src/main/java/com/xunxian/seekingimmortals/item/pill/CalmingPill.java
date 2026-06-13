package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CalmingPill extends BasePillItem {
    private static final int RISK_REDUCTION = 20;

    public CalmingPill(Properties properties, PillQuality quality) {
        super(properties, PillType.CALMING, quality);
    }

    @Override
    protected boolean consumePill(ServerPlayer player) {
        return CultivationHelper.get(player).map(cultivation -> {
            int before = cultivation.getQiDeviationRisk();
            cultivation.addQiDeviationRisk(-RISK_REDUCTION);
            int after = cultivation.getQiDeviationRisk();
            if (before == after && before == 0) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.calming_pill.no_risk"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.calming_pill.success", before, after), false);
            }
            return true;
        }).orElse(false);
    }
}
