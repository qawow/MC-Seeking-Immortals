package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.structure.ThunderTribulationAltarStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Thunder tribulation altar core. Sneak-use validates the outer ring + corner pillars multiblock
 * and grants short defensive buffs when complete. Survival consumes spirit-stone shards.
 * Activation is visual/audio only — no LightningBolt entity is spawned.
 */
public class ThunderTribulationAltarBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final int SPIRIT_STONE_COST = 12;

    public ThunderTribulationAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        if (!player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.thunder_tribulation_altar.info"), false);
            return InteractionResult.CONSUME;
        }

        ThunderTribulationAltarStructure.CheckResult check = ThunderTribulationAltarStructure.validate(
                level,
                pos,
                ModBlocks.THUNDER_TRIBULATION_ALTAR.get(),
                ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.thunder_tribulation_altar.incomplete",
                    check.missingRing(),
                    check.missingPillars(),
                    check.blockedAperture()), false);
            return InteractionResult.CONSUME;
        }

        if (!player.getAbilities().instabuild && !consumeShards(serverPlayer, SPIRIT_STONE_COST)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.thunder_tribulation_altar.missing_cost", SPIRIT_STONE_COST), true);
            return InteractionResult.CONSUME;
        }

        serverPlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        playActivationEffects(serverPlayer.serverLevel(), pos);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.thunder_tribulation_altar.activated"), true);
        return InteractionResult.CONSUME;
    }

    private static boolean consumeShards(ServerPlayer player, int count) {
        Item shard = ModItems.SPIRIT_STONE_SHARD.get();
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(shard)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining <= 0;
    }

    private void playActivationEffects(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.4D, z, 48, 0.65D, 0.45D, 0.65D, 0.02D);
        // Visual/audio only — do not spawn LightningBolt entity (would deal damage).
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.85F, 1.05F);
    }
}
