package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LingGenTestStoneItem extends Item {
    private static final String USES_KEY = "RemainingUses";
    private static final int MAX_USES = 5;

    public LingGenTestStoneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            testPlayer((ServerLevel) level, player, player, stack, true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide && target instanceof Player targetPlayer && player.level() instanceof ServerLevel serverLevel) {
            testPlayer(serverLevel, player, targetPlayer, stack, true);
            return InteractionResult.SUCCESS;
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    public static void testPlayer(ServerLevel level, Player user, Player target, ItemStack stack, boolean consumeUse) {
        testPlayer(level, user, target, stack, consumeUse, true);
    }

    public static void testPlayer(ServerLevel level, Player user, Player target, ItemStack stack, boolean consumeUse, boolean showTargetNotice) {
        if (stack != null && getRemainingUses(stack) <= 0 && !user.getAbilities().instabuild) {
            user.displayClientMessage(Component.translatable("message.seeking_immortals.ling_gen_test_stone.depleted"), true);
            return;
        }
        CultivationHelper.get(target).ifPresentOrElse(cultivation -> {
            boolean created = cultivation.createLingGenIfAbsent(level.getRandom());
            showResult(user, target, cultivation, created);
            if (showTargetNotice && user != target) {
                target.displayClientMessage(Component.translatable("message.seeking_immortals.ling_gen_test.target_notice", user.getDisplayName()), false);
                showResult(target, target, cultivation, created);
            }
            if (target instanceof ServerPlayer targetServerPlayer) {
                SyncCultivationDataPacket.send(targetServerPlayer, cultivation);
                grantMysticVialForLowTalent(level, targetServerPlayer, cultivation);
            }
            playEffects(level, target);
            if (consumeUse && stack != null) {
                consumeUse(level, user, stack);
            }
        }, () -> user.displayClientMessage(Component.translatable("message.seeking_immortals.ling_gen_test.no_data"), true));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.seeking_immortals.ling_gen_test_stone.usage").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.ling_gen_test_stone.uses", getRemainingUses(stack), MAX_USES).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.ling_gen_test_stone.no_recharge").withStyle(ChatFormatting.RED));
    }

    private static int getRemainingUses(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(USES_KEY)) {
            tag.putInt(USES_KEY, MAX_USES);
        }
        return Math.max(0, Math.min(MAX_USES, tag.getInt(USES_KEY)));
    }

    private static void grantMysticVialForLowTalent(ServerLevel level, ServerPlayer player, PlayerCultivation cultivation) {
        if (!cultivation.getSpiritualRoot().isLowTalent()) return;
        if (cultivation.isMysticVialGranted()) return;
        if (player.getInventory().contains(new ItemStack(com.xunxian.seekingimmortals.registry.ModItems.MYSTIC_VIAL.get()))) return;
        ItemStack vial = new ItemStack(com.xunxian.seekingimmortals.registry.ModItems.MYSTIC_VIAL.get());
        com.xunxian.seekingimmortals.item.MysticVialItem.setOwner(vial, player);
        com.xunxian.seekingimmortals.item.MysticVialItem.refillIfNeeded(vial, System.currentTimeMillis());
        if (!player.getInventory().add(vial)) {
            player.drop(vial, false);
        }
        cultivation.setMysticVialGranted(true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.mystic_vial.granted"), false);
    }

    private static void consumeUse(ServerLevel level, Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) return;
        CompoundTag tag = stack.getOrCreateTag();
        int remaining = Math.max(0, getRemainingUses(stack) - 1);
        tag.putInt(USES_KEY, remaining);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.ling_gen_test_stone.uses_left", remaining, MAX_USES), true);
        if (remaining <= 0) {
            stack.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
            level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.25D, 0.35D, 0.25D, 0.02D);
        }
    }

    private static void showResult(Player viewer, Player target, PlayerCultivation cultivation, boolean created) {
        viewer.displayClientMessage(Component.literal("§6§l【灵根检测】§r " + target.getName().getString()), false);
        viewer.displayClientMessage(Component.translatable(created
                ? "message.seeking_immortals.ling_gen_test.created"
                : "message.seeking_immortals.ling_gen_test.read_only"), false);
        viewer.displayClientMessage(Component.translatable("message.seeking_immortals.ling_gen_test.result",
                cultivation.getSpiritualRoot().getDisplayName(), stars(cultivation.getSpiritualRoot().getStarLevel()), cultivation.getSpiritualRootAttributeNames(), cultivation.getSpiritualRootPurity()), false);
        // 显示分类名、修炼倍率、灵力回复倍率
        viewer.displayClientMessage(Component.translatable("message.seeking_immortals.ling_gen_test.multiplier",
                format(cultivation.getCultivationSpeedMultiplier()),
                format(cultivation.getSpiritualRoot().getQiRecoveryMultiplier()),
                format(cultivation.getBreakthroughMultiplier()),
                format(cultivation.getSpiritualRoot().getBreakthroughBonus())), false);
        // 如果是低资质灵根，额外显示丹药吸收率和青玉小瓶
        if (cultivation.getSpiritualRoot().isLowTalent()) {
            viewer.displayClientMessage(Component.translatable("tooltip.seeking_immortals.pill_absorption",
                    format(cultivation.getPillAbsorptionMultiplier())), false);
            viewer.displayClientMessage(Component.translatable("tooltip.seeking_immortals.jade_vial_chance",
                    String.format(java.util.Locale.ROOT, "%.0f", cultivation.getJadeVialDropChance() * 100)), false);
        }
        viewer.displayClientMessage(Component.translatable("message.seeking_immortals.ling_gen_test.tip", cultivation.getSpiritualRoot().getDescription()), false);
    }

    private static void playEffects(ServerLevel level, Player target) {
        level.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.2D, target.getZ(), 42, 0.55D, 0.75D, 0.55D, 0.04D);
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0D, target.getZ(), 10, 0.35D, 0.45D, 0.35D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.75F, 1.35F);
    }

    private static String stars(int level) {
        int count = Math.max(0, Math.min(6, level));
        return "★".repeat(count) + "☆".repeat(Math.max(0, 6 - count));
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}

