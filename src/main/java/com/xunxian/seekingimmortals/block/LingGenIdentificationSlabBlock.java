package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.item.LingGenTestStoneItem;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Independent ling-gen identification slab (Wave49 Phase2 depth).
 * Own cooldown + optional spirit-stone fee; does not share test-stone consume path.
 */
public class LingGenIdentificationSlabBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 4.0D, 15.0D);
    private static final String COOLDOWN_TAG = "seeking_immortals_ling_gen_slab_cd";
    private static final long COOLDOWN_MS = 30_000L;
    private static final int FEE_SHARDS = 1;

    public LingGenIdentificationSlabBlock(Properties properties) {
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
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        long now = System.currentTimeMillis();
        CompoundTag data = serverPlayer.getPersistentData();
        long readyAt = data.getLong(COOLDOWN_TAG);
        if (now < readyAt) {
            long remainSec = Math.max(1L, (readyAt - now + 999L) / 1000L);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ling_gen_slab.cooldown", remainSec), true);
            return InteractionResult.CONSUME;
        }
        if (!serverPlayer.getAbilities().instabuild && !consumeShard(serverPlayer, FEE_SHARDS)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ling_gen_slab.need_fee", FEE_SHARDS), true);
            return InteractionResult.CONSUME;
        }

        // Independent path: first identification rolls; later rereads current root.
        CultivationHelper.get(serverPlayer).ifPresent(cultivation -> {
            if (!cultivation.isSpiritualRootTested()) {
                LingGenTestStoneItem.testPlayer(serverLevel, serverPlayer, serverPlayer, null, false);
            } else {
                serverPlayer.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.ling_gen_slab.reread",
                        cultivation.getSpiritualRoot().getDisplayName()), true);
            }
            SyncCultivationDataPacket.send(serverPlayer, cultivation);
        });

        data.putLong(COOLDOWN_TAG, now + COOLDOWN_MS);
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                24, 0.35D, 0.2D, 0.35D, 0.02D);
        serverLevel.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.1F);
        serverPlayer.displayClientMessage(Component.translatable("message.seeking_immortals.ling_gen_slab.done"), true);
        return InteractionResult.CONSUME;
    }

    private static boolean consumeShard(ServerPlayer player, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.is(ModItems.SPIRIT_STONE_SHARD.get())) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        if (remaining <= 0) {
            player.containerMenu.broadcastChanges();
            return true;
        }
        return false;
    }
}
