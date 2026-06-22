package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CultivationPillItem extends Item {
    private static final int BOOST_TICKS = 60 * 60 * 20;
    private static final double BOOST_MULTIPLIER = 2.0D;
    private final int expValue;

    public CultivationPillItem(Properties properties, int expValue) {
        super(properties);
        this.expValue = expValue;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CultivationHelper.get(player).ifPresent(cultivation -> {
                int adjustedExp = (int) Math.round(expValue * cultivation.getPillAbsorptionMultiplier());
                boolean alreadyBoosting = cultivation.getCultivationBoostTicks() > 0;
                cultivation.addCultivationExp(adjustedExp);
                cultivation.addCultivationBoost(BOOST_TICKS, BOOST_MULTIPLIER);
                player.displayClientMessage(Component.translatable(alreadyBoosting
                        ? "message.seeking_immortals.cultivation_pill.boost_extend" : "message.seeking_immortals.cultivation_pill.boost", adjustedExp), true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
