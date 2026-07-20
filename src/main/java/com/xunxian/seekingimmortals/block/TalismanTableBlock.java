package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.craft.TalismanCraftService;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.structure.TalismanTableStructure;
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
 * Text-material talisman_table. Sneak-use validates 3x3 multiblock then crafts via
 * TalismanCraftService (24 mapped recipes from text materials).
 */
public class TalismanTableBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D);

    public TalismanTableBlock(Properties properties) {
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.talisman_table.info"), false);
            return InteractionResult.CONSUME;
        }
        TalismanTableStructure.CheckResult check =
                TalismanTableStructure.validate(level, pos, ModBlocks.TALISMAN_TABLE.get(), ModBlocks.SPIRIT_ORE.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.talisman_table.incomplete",
                    check.missingBaseBlocks(),
                    check.missingFrameBlocks()), false);
            return InteractionResult.CONSUME;
        }
        if (!com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .ensureCommissioned(serverPlayer, "talisman_table", pos)) {
            return InteractionResult.CONSUME;
        }

        TalismanCraftService.CraftResult result = TalismanCraftService.craft(serverPlayer);
        ServerLevel serverLevel = serverPlayer.serverLevel();
        if (result.success()) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    28, 0.45D, 0.35D, 0.45D, 0.02D);
            serverLevel.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.2F);
            player.displayClientMessage(Component.translatable(result.messageKey(), result.product().getHoverName()), true);
        } else {
            player.displayClientMessage(Component.translatable(result.messageKey()), false);
        }
        return InteractionResult.CONSUME;
    }
}
