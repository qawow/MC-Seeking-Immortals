package com.xunxian.seekingimmortals.item.material;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class BaseMaterialItem extends Item {
    private final MaterialCategory category;
    private final MaterialRarity rarity;
    private final String description;

    public BaseMaterialItem(Properties properties, MaterialCategory category, MaterialRarity rarity, String description) {
        super(properties);
        this.category = category;
        this.rarity = rarity;
        this.description = description;
    }

    public MaterialCategory getCategory() { return category; }
    public MaterialRarity getRarity() { return rarity; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(rarity.getDisplayName()).withStyle(style -> style.withColor(rarity.getColor())));
        tooltip.add(Component.literal(category.getDisplayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(description).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return rarity == MaterialRarity.LEGENDARY;
    }
}
