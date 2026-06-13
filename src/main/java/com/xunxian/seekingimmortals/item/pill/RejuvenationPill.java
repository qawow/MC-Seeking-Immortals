package com.xunxian.seekingimmortals.item.pill;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.CultivationProvider;
import net.minecraft.server.level.ServerPlayer;

public class RejuvenationPill extends BasePillItem {
    public RejuvenationPill(Properties properties, PillQuality quality) {
        super(properties, PillType.REJUVENATION, quality);
    }

    @Override
    protected boolean consumePill(ServerPlayer player) {
        return player.getCapability(CultivationProvider.CULTIVATION).map(cultivation -> {
            double multiplier = getQuality().getEffectMultiplier();
            int spiritualPowerGain = (int)(50 * multiplier);
            int cultivationExpGain = (int)(10 * multiplier);

            cultivation.addSpiritualPower(spiritualPowerGain);
            cultivation.addCultivationExp(cultivationExpGain);

            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    "服用" + getQuality().getDisplayName() + "回春丹，恢复" +
                    spiritualPowerGain + "灵力，增加" + cultivationExpGain + "修为"
                ), true
            );
            return true;
        }).orElse(false);
    }
}
