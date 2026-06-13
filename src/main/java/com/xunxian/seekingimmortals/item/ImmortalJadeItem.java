package com.xunxian.seekingimmortals.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class ImmortalJadeItem extends Item {
    public ImmortalJadeItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("灵界货币").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("珍贵至极，可用于高阶交易").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
