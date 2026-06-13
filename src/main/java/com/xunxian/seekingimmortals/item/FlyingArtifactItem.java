package com.xunxian.seekingimmortals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FlyingArtifactItem extends Item {
    private final boolean flyingSword;

    public FlyingArtifactItem(Properties properties, boolean flyingSword) {
        super(properties.stacksTo(1));
        this.flyingSword = flyingSword;
    }

    public boolean isFlyingSword() {
        return flyingSword;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.flying_artifact.requirement").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.flying_artifact.rule").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(flyingSword
                ? "tooltip.seeking_immortals.flying_sword.flavor"
                : "tooltip.seeking_immortals.flying_artifact.flavor").withStyle(ChatFormatting.GOLD));
    }
}
