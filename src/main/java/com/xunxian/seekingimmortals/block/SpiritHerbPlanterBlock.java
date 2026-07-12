package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.structure.SpiritHerbPlanterStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
 * Text-material spirit_herb_planter. Sneak-use validates a soil/array ring then harvests
 * a catalog herb with a short growth cooldown stored in player persistent data.
 */
public class SpiritHerbPlanterBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final String COOLDOWN_ROOT = "seeking_immortals_herb_planter_cd";
    private static final long COOLDOWN_TICKS = 20L * 60L * 3L; // 3 minutes

    public SpiritHerbPlanterBlock(Properties properties) {
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.spirit_herb_planter.info"), false);
            return InteractionResult.CONSUME;
        }
        SpiritHerbPlanterStructure.CheckResult check =
                SpiritHerbPlanterStructure.validate(level, pos, ModBlocks.SPIRIT_GATHERING_ARRAY.get());
        if (!check.complete()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.spirit_herb_planter.incomplete",
                    check.missingSoil()), false);
            return InteractionResult.CONSUME;
        }

        long now = level.getGameTime();
        String key = pos.asLong() + "";
        CompoundTag root = serverPlayer.getPersistentData().getCompound(COOLDOWN_ROOT).copy();
        long readyAt = root.getLong(key);
        if (!serverPlayer.getAbilities().instabuild && readyAt > now) {
            long remainSec = Math.max(1L, (readyAt - now + 19L) / 20L);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.spirit_herb_planter.cooldown", remainSec), false);
            return InteractionResult.CONSUME;
        }

        Item herb = rollHerb(serverPlayer.getRandom());
        ItemStack stack = new ItemStack(herb, 1 + serverPlayer.getRandom().nextInt(2));
        if (!serverPlayer.getInventory().add(stack)) {
            serverPlayer.drop(stack, false);
        }
        if (!serverPlayer.getAbilities().instabuild) {
            root.putLong(key, now + COOLDOWN_TICKS);
            serverPlayer.getPersistentData().put(COOLDOWN_ROOT, root);
        }
        ServerLevel serverLevel = serverPlayer.serverLevel();
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                18, 0.45D, 0.25D, 0.45D, 0.02D);
        serverLevel.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8F, 1.1F);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.spirit_herb_planter.activated", stack.getHoverName()), true);
        return InteractionResult.CONSUME;
    }

    private static Item rollHerb(RandomSource random) {
        int roll = random.nextInt(3);
        if (roll == 0) {
            return ModItems.CLOUD_MUSHROOM.get();
        }
        if (roll == 1) {
            return ModItems.FENGYUAN_CLAN_GINSENG.get();
        }
        return ModItems.IMMORTAL_GINSENG.get();
    }
}
