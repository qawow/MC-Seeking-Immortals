package com.xunxian.seekingimmortals.item.pill;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FastingPill extends BasePillItem {
    public FastingPill(Properties properties, PillQuality quality) {
        super(properties, PillType.FASTING, quality);
    }

    @Override
    protected boolean consumePill(ServerPlayer player) {
        double multiplier = effectiveMultiplier(player);
        int foodLevel = (int)(10 * multiplier);
        float saturation = (float)(5.0 * multiplier);

        player.getFoodData().eat(foodLevel, saturation);
        player.getPersistentData().putInt(CatalogPillItem.FASTING_TICKS_KEY, 24000);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.fasting_pill.success", foodLevel), true);
        return true;
    }
}
