package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.craft.PuppetCraftService;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.PuppetAssemblyBenchStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Text-material puppet_assembly_bench. Multiblock station:
 * consumes mapped recipe materials and spawns a real SummonedServitorEntity
 * via PuppetCraftService / SummonHonestMvpService (Wave44).
 */
public class PuppetAssemblyBenchBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public PuppetAssemblyBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.puppet_assembly_bench.info"), false);
            return InteractionResult.CONSUME;
        }
        PuppetAssemblyBenchStructure.CheckResult check =
                PuppetAssemblyBenchStructure.validate(level, pos, ModBlocks.PUPPET_ASSEMBLY_BENCH.get(), ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.puppet_assembly_bench.incomplete",
                    check.missingBaseBlocks(),
                    check.missingFrameBlocks()), false);
            return InteractionResult.CONSUME;
        }

        PuppetCraftService.CraftResult result = PuppetCraftService.craft(serverPlayer);
        ServerLevel serverLevel = serverPlayer.serverLevel();
        if (result.success()) {
            serverLevel.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                    36, 0.55D, 0.4D, 0.55D, 0.03D);
            serverLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.65F, 0.95F);
            String display = result.recipe() == null ? "" : result.recipe().display();
            player.displayClientMessage(Component.translatable(result.messageKey(), display), true);
        } else {
            player.displayClientMessage(Component.translatable(result.messageKey()), false);
        }
        return InteractionResult.CONSUME;
    }
}
