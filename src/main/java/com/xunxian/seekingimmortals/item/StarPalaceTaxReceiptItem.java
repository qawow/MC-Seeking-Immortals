package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StarPalaceTaxReceiptItem extends Item {
    public static final String TAX_PAID_FLAG = "star_palace_island_trade_tax_paid";

    public StarPalaceTaxReceiptItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        boolean applied = CultivationHelper.get(serverPlayer)
                .map(cultivation -> applyReceipt(serverPlayer, cultivation))
                .orElseGet(() -> {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.star_palace_tax_receipt.no_data"), true);
                    return false;
                });
        if (!applied) {
            return InteractionResultHolder.fail(stack);
        }
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.star_palace_tax_receipt.effect")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.star_palace_tax_receipt.use")
                .withStyle(ChatFormatting.GRAY));
    }

    public static boolean hasPaidIslandTradeTax(PlayerCultivation cultivation) {
        return cultivation.getSevenMysteriesQuest().hasFlag(TAX_PAID_FLAG);
    }

    private static boolean applyReceipt(ServerPlayer player, PlayerCultivation cultivation) {
        if (hasPaidIslandTradeTax(cultivation)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.star_palace_tax_receipt.already_paid"), true);
            return false;
        }
        cultivation.getSevenMysteriesQuest().addFlag(TAX_PAID_FLAG);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.star_palace_tax_receipt.used"), false);
        return true;
    }
}
