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
                cultivation.addCultivationExp(adjustedExp);
                player.displayClientMessage(Component.translatable("message.seeking_immortals.cultivation_exp", adjustedExp), true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
