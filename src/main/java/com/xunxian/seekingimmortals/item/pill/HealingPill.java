package com.xunxian.seekingimmortals.item.pill;

import net.minecraft.server.level.ServerPlayer;

public class HealingPill extends BasePillItem {
    public HealingPill(Properties properties, PillQuality quality) {
        super(properties, PillType.HEALING, quality);
    }

    @Override
    protected boolean consumePill(ServerPlayer player) {
        double multiplier = effectiveMultiplier(player);
        float healAmount = (float)(4.0 * multiplier);

        player.heal(healAmount);
        player.displayClientMessage(
            net.minecraft.network.chat.Component.translatable(
                "message.seeking_immortals.pill.healing",
                getQuality().getDisplayName(),
                String.format("%.1f", healAmount)
            ), true
        );
        return true;
    }
}
