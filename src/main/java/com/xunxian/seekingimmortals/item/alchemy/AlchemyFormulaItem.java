package com.xunxian.seekingimmortals.item.alchemy;

import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaKnowledge;
import com.xunxian.seekingimmortals.alchemy.AlchemyFormulaSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        // Studying does not consume the carrier — the same stack can still be installed on a furnace.
        boolean ok = AlchemyFormulaKnowledge.study(serverPlayer, recipeId);
        return ok ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.alchemy_formula.recipe", Component.translatable("alchemy_recipe.seeking_immortals." + recipeId)).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.alchemy_formula.source." + source.id()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.alchemy_formula.use").withStyle(ChatFormatting.GREEN));
    }
}
