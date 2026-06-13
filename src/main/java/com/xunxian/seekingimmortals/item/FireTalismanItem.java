package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.TechniqueDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class FireTalismanItem extends Item {
    public FireTalismanItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            CultivationHelper.get(player).ifPresent(cultivation -> {
                if (!cultivation.consumeQi(10)) {
                    player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
                    return;
                }
                double affinity = TechniqueDataManager.getAffinityMultiplier(cultivation, "火/雷/隐雷");
                SmallFireball fireball = new SmallFireball(level, player,
                        player.getLookAngle().x * affinity,
                        player.getLookAngle().y * affinity,
                        player.getLookAngle().z * affinity);
                fireball.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
                fireball.getPersistentData().putDouble("SeekingImmortalsDamageMultiplier", affinity);
                level.addFreshEntity(fireball);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
