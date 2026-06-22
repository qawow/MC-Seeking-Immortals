package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.VialGrade;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CropBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Phase 6 神秘小瓶 MVP。
 * <p>绑定玩家、每现实 24 小时积累 1 份灵液（含离线补算）、右键植物消耗 1 份灵液催熟。</p>
 */
public class MysticVialItem extends Item {
    public static final String CHARGES_KEY = "vialCharges";
    public static final String LAST_REFILL_KEY = "vialLastRefill";
    public static final String MAX_CHARGES_KEY = "vialMaxCharges";
    public static final String OWNER_KEY = "vialOwner";
    public static final String GRADE_KEY = "vialGrade";

    private static final long MILLIS_PER_CHARGE = 24L * 60L * 60L * 1000L;

    public MysticVialItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 现实时间充能（含离线补算）
        refillIfNeeded(stack, System.currentTimeMillis());
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (getCharges(stack) <= 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.mystic_vial.no_charges"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.mystic_vial.charges", getCharges(stack), getMaxCharges(stack)), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        refillIfNeeded(stack, System.currentTimeMillis());
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (getCharges(stack) <= 0) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.mystic_vial.no_charges"), true);
            return InteractionResult.CONSUME;
        }
        BlockState state = level.getBlockState(pos);
        if (!isGrowTarget(level, pos, state)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        // 消耗 1 份灵液，对作物执行多次成长步进，等效加速
        setCharges(stack, getCharges(stack) - 1);
        growTarget(serverLevel, pos, state);
        serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.3F);
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 12, 0.3D, 0.3D, 0.3D, 0.02D);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.mystic_vial.used", getCharges(stack), getMaxCharges(stack)), true);
        return InteractionResult.CONSUME;
    }

    private static boolean isGrowTarget(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return !crop.isMaxAge(state);
        }
        return state.getBlock() instanceof BonemealableBlock growable && growable.isValidBonemealTarget(level, pos, state, false);
    }

    private static void growTarget(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            int age = crop.getAge(state);
            int max = crop.getMaxAge();
            int steps = Math.min(VialGrade.BASIC.getGrowthSpeedMultiplier() > 0
                    ? Math.max(1, (int) VialGrade.BASIC.getGrowthSpeedMultiplier()) : 1, max - age);
            for (int i = 0; i < steps; i++) {
                int next = Math.min(crop.getAge(state) + 1, max);
                if (next == crop.getAge(state)) break;
                level.setBlock(pos, crop.getStateForAge(next), 3);
                state = level.getBlockState(pos);
            }
            return;
        }
        if (state.getBlock() instanceof BonemealableBlock growable) {
            // 对其他可催熟方块执行若干次成长判定，模拟加速
            for (int i = 0; i < 3; i++) {
                if (!growable.isBonemealSuccess(level, level.random, pos, state)) break;
                growable.performBonemeal(level, level.random, pos, state);
                state = level.getBlockState(pos);
            }
        }
    }

    public static int getCharges(ItemStack stack) {
        return stack.getOrCreateTag().getInt(CHARGES_KEY);
    }

    public static int getMaxCharges(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(MAX_CHARGES_KEY)) {
            tag.putInt(MAX_CHARGES_KEY, VialGrade.BASIC.getMaxCharges());
        }
        return tag.getInt(MAX_CHARGES_KEY);
    }

    public static void setCharges(ItemStack stack, int charges) {
        stack.getOrCreateTag().putInt(CHARGES_KEY, Math.max(0, Math.min(getMaxCharges(stack), charges)));
    }

    public static void setOwner(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(OWNER_KEY, player.getUUID());
    }

    public static boolean isOwner(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.hasUUID(OWNER_KEY)) return true;
        return tag.getUUID(OWNER_KEY).equals(player.getUUID());
    }

    /** 按现实时间补算离线/在线充能，不破坏已有 NBT。 */
    public static void refillIfNeeded(ItemStack stack, long nowMillis) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(LAST_REFILL_KEY)) {
            tag.putLong(LAST_REFILL_KEY, nowMillis);
        }
        if (!tag.contains(CHARGES_KEY)) {
            tag.putInt(CHARGES_KEY, 0);
        }
        if (!tag.contains(MAX_CHARGES_KEY)) {
            tag.putInt(MAX_CHARGES_KEY, VialGrade.BASIC.getMaxCharges());
        }
        int charges = tag.getInt(CHARGES_KEY);
        int max = tag.getInt(MAX_CHARGES_KEY);
        if (charges >= max) {
            tag.putLong(LAST_REFILL_KEY, nowMillis);
            return;
        }
        long last = tag.getLong(LAST_REFILL_KEY);
        long elapsed = Math.max(0L, nowMillis - last);
        int gained = (int) (elapsed / MILLIS_PER_CHARGE);
        if (gained <= 0) {
            return;
        }
        int newCharges = Math.min(max, charges + gained);
        tag.putInt(CHARGES_KEY, newCharges);
        // 仅消耗已用于充能的时间，保留未满一份的余量
        tag.putLong(LAST_REFILL_KEY, last + (long) gained * MILLIS_PER_CHARGE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        refillIfNeeded(stack, System.currentTimeMillis());
        int charges = getCharges(stack);
        int max = getMaxCharges(stack);
        tooltip.add(Component.translatable("tooltip.seeking_immortals.mystic_vial.charges", charges, max).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.mystic_vial.usage").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        // 不可丢弃：放回玩家背包，避免丢失
        player.displayClientMessage(Component.translatable("message.seeking_immortals.mystic_vial.cannot_drop"), true);
        return false;
    }
}
