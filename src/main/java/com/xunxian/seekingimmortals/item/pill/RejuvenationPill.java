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
            double absorption = getPillAbsorptionMultiplier(player);
            int spiritualPowerGain = (int) Math.round(50 * multiplier * absorption);
            int cultivationExpGain = (int) Math.round(10 * multiplier * absorption);

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
