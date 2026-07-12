package com.xunxian.seekingimmortals.item.alchemy;

import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class AlchemyFormulaItem extends Item {
    private final String recipeId;
    private final AlchemyFormulaSource source;

    public AlchemyFormulaItem(Properties properties, String recipeId, AlchemyFormulaSource source) {
        super(properties);
        this.recipeId = recipeId;
        this.source = source;
    }

    public String recipeId() {
        return recipeId;
    }

    public AlchemyFormulaSource source() {
        return source;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.alchemy_formula.recipe", Component.translatable("alchemy_recipe.seeking_immortals." + recipeId)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.alchemy_formula.source." + source.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.alchemy_formula.use").withStyle(ChatFormatting.GREEN));
    }
}
