package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.cultivation.GhostContractService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Yin Coffin Nail - core ghost contract item.
 * Wave480: supports ghost contract establishment and maintenance.
 */
public class YinCoffinNailItem extends Item {
    private static final long CONTRACT_WINDOW_TICKS = 200L; // 10 seconds

    public YinCoffinNailItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        // Check if this is a recently killed entity eligible for ghost contract
        if (target instanceof Mob mob && !mob.isAlive()) {
            return attemptContractFromCorpse(serverPlayer, mob, stack, hand);
        }

        // Cannot contract living entities directly
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.ghost_contract.target_must_be_dead"), true);
        return InteractionResult.FAIL;
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

        // Default right-click behavior: empower/maintain ghost contracts
        boolean success = com.xunxian.seekingimmortals.item.CatalogConsumableService.use(
                serverPlayer, stack, hand, "yin_coffin_nail", "corpse_control", ""
        ) == com.xunxian.seekingimmortals.item.CatalogConsumableService.UseResult.SUCCESS;

        if (success && !serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }

        return success ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    private InteractionResult attemptContractFromCorpse(ServerPlayer player, Mob corpse, ItemStack stack, InteractionHand hand) {
        // Check if corpse is within contract window
        long killTime = corpse.getPersistentData().getLong("SeekingImmortalsGhostContractKillTime");
        if (killTime == 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.corpse_too_old"), true);
            return InteractionResult.FAIL;
        }

        long currentTime = player.level().getGameTime();
        if (currentTime - killTime > CONTRACT_WINDOW_TICKS) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.corpse_expired",
                    (CONTRACT_WINDOW_TICKS / 20)), true);
            return InteractionResult.FAIL;
        }

        // Check if player is the one who killed it
        java.util.UUID killerUUID = corpse.getPersistentData().getUUID("SeekingImmortalsGhostContractKiller");
        if (!player.getUUID().equals(killerUUID)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.not_your_kill"), true);
            return InteractionResult.FAIL;
        }

        // Attempt to establish contract
        boolean success = GhostContractService.attemptContract(player, corpse);

        if (success) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            // Clear contract markers to prevent double-contract
            corpse.getPersistentData().remove("SeekingImmortalsGhostContractKillTime");
            corpse.getPersistentData().remove("SeekingImmortalsGhostContractKiller");
            return InteractionResult.SUCCESS;
        }

        // Contract failed - nail is consumed by backlash
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.seeking_immortals.yin_coffin_nail.contract")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.yin_coffin_nail.empower")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.seeking_immortals.yin_coffin_nail.window",
                CONTRACT_WINDOW_TICKS / 20).withStyle(ChatFormatting.DARK_GRAY));
    }
}
