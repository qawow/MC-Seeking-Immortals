package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.BreakthroughService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BreakthroughPillItem extends Item {
    public BreakthroughPillItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean applied = CultivationHelper.get(serverPlayer).map(cultivation -> {
                BreakthroughService.HandBreakthroughAidResult result =
                        BreakthroughService.tryApplyHandConsumedBreakthroughAid(serverPlayer, cultivation, stack, false);
                if (result == BreakthroughService.HandBreakthroughAidResult.APPLIED) {
                    if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
                    return true;
                }
                if (result == BreakthroughService.HandBreakthroughAidResult.NOT_APPLICABLE) {
                    serverPlayer.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.aid_not_matching"), true);
                }
                return false;
            }).orElseGet(() -> {
                serverPlayer.displayClientMessage(Component.translatable("message.seeking_immortals.breakthrough.no_data"), true);
                return false;
            });
            return applied ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }
}
