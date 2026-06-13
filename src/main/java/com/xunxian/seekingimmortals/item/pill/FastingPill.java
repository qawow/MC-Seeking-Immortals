package com.xunxian.seekingimmortals.item.pill;

import net.minecraft.server.level.ServerPlayer;

public class FastingPill extends BasePillItem {
    public FastingPill(Properties properties, PillQuality quality) {
        super(properties, PillType.FASTING, quality);
    }

    @Override
    protected boolean consumePill(ServerPlayer player) {
        double multiplier = getQuality().getEffectMultiplier();
        int foodLevel = (int)(10 * multiplier);
        float saturation = (float)(5.0 * multiplier);

        player.getFoodData().eat(foodLevel, saturation);
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(
                "服用" + getQuality().getDisplayName() + "辟谷丹，恢复" + foodLevel + "饱食度"
            ), true
        );
        return true;
    }
}
