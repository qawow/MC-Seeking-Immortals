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
import java.util.Locale;

public class SpiritDetectorItem extends Item {
    public SpiritDetectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            SpiritualAuraManager.AuraInfo aura = SpiritualAuraManager.getAuraInfo(serverLevel, player.blockPosition());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.aura.detector.header"), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.aura.detector.concentration", aura.concentration()), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.aura.detector.detail",
                    format(aura.dimensionMultiplier()), format(aura.biomeMultiplier()), format(aura.leylineMultiplier()), aura.formationBonus()), false);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.aura.detector.nature", aura.nature().getDisplayName()), false);
            if (aura.leyline()) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.aura.detector.leyline"), false);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.spirit_detector").withStyle(ChatFormatting.AQUA));
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2fx", value);
    }
}
