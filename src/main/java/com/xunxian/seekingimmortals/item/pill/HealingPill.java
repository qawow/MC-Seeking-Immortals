package com.xunxian.seekingimmortals.item.pill;

import net.minecraft.server.level.ServerPlayer;

public class HealingPill extends BasePillItem {
    public HealingPill(Properties properties, PillQuality quality) {
        super(properties, PillType.HEALING, quality);
    }

    @Override
    protected boolean consumePill(ServerPlayer player) {
        double multiplier = getQuality().getEffectMultiplier();
        float healAmount = (float)(4.0 * multiplier);

        player.heal(healAmount);
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(
                "服用" + getQuality().getDisplayName() + "疗伤丹，恢复" +
                String.format("%.1f", healAmount) + "生命值"
            ), true
        );
        return true;
    }
}
