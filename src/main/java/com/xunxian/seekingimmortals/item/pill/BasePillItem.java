package com.xunxian.seekingimmortals.item.pill;

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

import java.util.List;

public class BasePillItem extends Item {
    private final PillType pillType;
    private final PillQuality quality;

    public BasePillItem(Properties properties, PillType pillType, PillQuality quality) {
        super(properties);
        this.pillType = pillType;
        this.quality = quality;
    }

    public PillType getPillType() { return pillType; }
    public PillQuality getQuality() { return quality; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (consumePill(serverPlayer)) {
                stack.shrink(1);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    protected boolean consumePill(ServerPlayer player) {
        return true;
    }

    /** 获取玩家的丹药吸收率倍数（低资质灵根更高） */
    protected double getPillAbsorptionMultiplier(ServerPlayer player) {
        return CultivationHelper.get(player)
                .map(PlayerCultivation::getPillAbsorptionMultiplier)
                .orElse(1.0D);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(quality.getDisplayName()).withStyle(style -> style.withColor(quality.getColor())));
        tooltip.add(Component.literal(pillType.getCategory().getDisplayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(pillType.getDescription()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("最低境界: " + pillType.getMinRealm().getDisplayName()).withStyle(ChatFormatting.BLUE));
    }
}
