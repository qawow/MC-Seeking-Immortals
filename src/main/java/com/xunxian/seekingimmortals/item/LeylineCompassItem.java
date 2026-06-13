package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LeylineCompassItem extends Item {
    public LeylineCompassItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            SpiritualAuraManager.findNearestLeyline(serverLevel, player.blockPosition()).ifPresentOrElse(target -> {
                stack.getOrCreateTag().putInt("TargetX", target.blockX());
                stack.getOrCreateTag().putInt("TargetZ", target.blockZ());
                stack.getOrCreateTag().putString("TargetDimension", serverLevel.dimension().location().toString());
                player.displayClientMessage(Component.translatable("message.seeking_immortals.leyline_compass.found",
                        getDirectionName(player, target), Math.max(1, (int)Math.round(target.distance())), String.format(java.util.Locale.ROOT, "%.1fx", target.multiplier())), false);
            }, () -> player.displayClientMessage(Component.translatable("message.seeking_immortals.leyline_compass.not_found"), false));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.leyline_compass").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (stack.hasTag() && stack.getOrCreateTag().contains("TargetX") && stack.getOrCreateTag().contains("TargetZ")) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.leyline_compass.target", stack.getOrCreateTag().getInt("TargetX"), stack.getOrCreateTag().getInt("TargetZ")).withStyle(ChatFormatting.GRAY));
        }
    }

    private String getDirectionName(Player player, SpiritualAuraManager.LeylineTarget target) {
        double dx = target.blockX() - player.getX();
        double dz = target.blockZ() - player.getZ();
        double angle = Math.atan2(dz, dx);
        double degrees = Math.toDegrees(angle);
        if (degrees < 0.0D) degrees += 360.0D;
        String[] names = new String[] {"东", "东南", "南", "西南", "西", "西北", "北", "东北"};
        int index = (int)Math.round(degrees / 45.0D) % names.length;
        return names[index];
    }
}
