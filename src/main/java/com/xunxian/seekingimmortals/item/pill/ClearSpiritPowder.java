package com.xunxian.seekingimmortals.item.pill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

public class ClearSpiritPowder extends BasePillItem {
    public ClearSpiritPowder(Properties properties, PillQuality quality) {
        super(properties, PillType.CLEAR_SPIRIT_POWDER, quality);
    }

    @Override
    protected boolean consumePill(ServerPlayer player) {
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.HUNGER);

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(
                "服用" + getQuality().getDisplayName() + "清灵散，解除所有毒素"
            ), true
        );
        return true;
    }
}
