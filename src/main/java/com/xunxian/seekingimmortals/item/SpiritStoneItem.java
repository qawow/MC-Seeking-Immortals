package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.SpiritualRootAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpiritStoneItem extends Item {
    private static final String STORED_POWER_TAG = "StoredSpiritualPower";
    private static final String ABSORBING_TAG = "AbsorbingSpiritualPower";

    private final int maxStoredPower;
    private final int absorbPerSecond;
    private final int passiveBonus;
    @Nullable
    private final SpiritualRootAttribute attribute;

    public SpiritStoneItem(Properties properties, int maxStoredPower, int absorbPerSecond, int passiveBonus) {
        this(properties, maxStoredPower, absorbPerSecond, passiveBonus, null);
    }

    public SpiritStoneItem(Properties properties, int maxStoredPower, int absorbPerSecond, int passiveBonus, @Nullable SpiritualRootAttribute attribute) {
        super(properties.stacksTo(64));
        this.maxStoredPower = maxStoredPower;
        this.absorbPerSecond = absorbPerSecond;
        this.passiveBonus = passiveBonus;
        this.attribute = attribute;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getCount() != 1) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.spirit_stone.single_only"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (getStoredPower(stack) <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.spirit_stone.empty"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            boolean newState = !isAbsorbing(stack);
            setAbsorbing(stack, newState);
            player.displayClientMessage(Component.translatable(newState
                    ? "message.seeking_immortals.spirit_stone.absorb_start"
                    : "message.seeking_immortals.spirit_stone.absorb_stop"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getCount() == 1 && getStoredPower(stack) < maxStoredPower;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getStoredPower(stack) / maxStoredPower);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x40E0D0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.spirit_stone.power", getStoredPower(stack), maxStoredPower).withStyle(ChatFormatting.AQUA));
        if (attribute != null) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.spirit_stone.attribute", attribute.getDisplayName()).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.seeking_immortals.spirit_stone.attribute_bonus_hint").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(isAbsorbing(stack)
                ? "tooltip.seeking_immortals.spirit_stone.absorbing"
                : "tooltip.seeking_immortals.spirit_stone.single_hint").withStyle(ChatFormatting.GRAY));
    }

    @Nullable
    public SpiritualRootAttribute getAttribute() {
        return attribute;
    }

    public boolean matchesAttribute(SpiritualRootAttribute requiredAttribute) {
        return attribute != null && attribute == requiredAttribute;
    }

    public int getPassiveBonus() {
        return passiveBonus;
    }

    public int getAbsorbPerSecond() {
        return absorbPerSecond;
    }

    public static boolean isAbsorbing(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(ABSORBING_TAG);
    }

    public static void setAbsorbing(ItemStack stack, boolean absorbing) {
        stack.getOrCreateTag().putBoolean(ABSORBING_TAG, absorbing);
    }

    public static int getStoredPower(ItemStack stack) {
        if (!(stack.getItem() instanceof SpiritStoneItem stone)) return 0;
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(STORED_POWER_TAG)) {
            tag.putInt(STORED_POWER_TAG, stone.maxStoredPower);
        }
        return Math.max(0, Math.min(stone.maxStoredPower, tag.getInt(STORED_POWER_TAG)));
    }

    public static int consumeStoredPower(ItemStack stack, int amount) {
        if (!(stack.getItem() instanceof SpiritStoneItem)) return 0;
        int current = getStoredPower(stack);
        int drained = Math.min(current, Math.max(0, amount));
        stack.getOrCreateTag().putInt(STORED_POWER_TAG, current - drained);
        if (current - drained <= 0) {
            setAbsorbing(stack, false);
        }
        return drained;
    }

    public static int getAvailablePassiveBonus(ItemStack stack) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof SpiritStoneItem stone) || getStoredPower(stack) <= 0) return 0;
        return stone.getPassiveBonus();
    }

    public static int getAbsorbPerSecond(ItemStack stack) {
        if (stack.getCount() != 1 || !(stack.getItem() instanceof SpiritStoneItem stone) || getStoredPower(stack) <= 0) return 0;
        return stone.getAbsorbPerSecond();
    }
}
