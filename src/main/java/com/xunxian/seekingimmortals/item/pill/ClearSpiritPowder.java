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

        // M16：吸收率足够时额外清除一种即时负面（缓慢/虚弱/挖掘疲劳）
        if (effectiveMultiplier(player) >= EXTRA_CLEAR_THRESHOLD) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
        }

        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal(
                "服用" + getQuality().getDisplayName() + "清灵散，解除毒素"
            ), true
        );
        return true;
    }

    private static final double EXTRA_CLEAR_THRESHOLD = 1.2D;
}
