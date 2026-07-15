package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.block.entity.AlchemyFurnaceBlockEntity;
import com.xunxian.seekingimmortals.registry.ModBlockEntities;
import com.xunxian.seekingimmortals.structure.AlchemyFurnaceShellStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AlchemyFurnaceBlock extends BaseEntityBlock {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    private static final VoxelShape SHAPE = box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    private final int tier;

    public AlchemyFurnaceBlock(Properties properties) {
        this(properties, 1);
    }

    public AlchemyFurnaceBlock(Properties properties, int tier) {
        super(properties);
        this.tier = Math.max(1, tier);
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    public int tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FORMED, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlchemyFurnaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ALCHEMY_FURNACE.get(), AlchemyFurnaceBlockEntity::serverTick);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace) {
            furnace.refreshFormedState((ServerLevel) level, false);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace) {
            furnace.refreshFormedState((ServerLevel) level, false);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AlchemyFurnaceBlockEntity furnace && player instanceof ServerPlayer serverPlayer) {
            ItemStack held = player.getItemInHand(hand);
            // Sneak + empty hand: report multiblock shell status without opening menu.
            if (held.isEmpty() && player.isShiftKeyDown()) {
                furnace.refreshFormedState(serverPlayer.serverLevel(), false);
                int furnaceTier = tier();
                int required = AlchemyFurnaceShellStructure.requiredCount(furnaceTier);
                int present = AlchemyFurnaceShellStructure.presentCount(level, pos, furnaceTier);
                int missing = Math.max(0, required - present);
                boolean ok = missing == 0 && state.getValue(FORMED);
                player.displayClientMessage(Component.translatable(
                        ok ? "message.seeking_immortals.alchemy_furnace.shell_status_ok"
                                : "message.seeking_immortals.alchemy_furnace.shell_status_bad",
                        furnaceTier, present, required, missing), false);
                if (!ok && level instanceof ServerLevel serverLevel) {
                    for (BlockPos offset : AlchemyFurnaceShellStructure.missingOffsets(level, pos, furnaceTier)) {
                        BlockPos mark = pos.offset(offset);
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                                mark.getX() + 0.5D, mark.getY() + 0.6D, mark.getZ() + 0.5D,
                                4, 0.1D, 0.1D, 0.1D, 0.0D);
                    }
                }
                return InteractionResult.CONSUME;
            }
            if (held.isEmpty()) {
                furnace.refreshFormedState(serverPlayer.serverLevel(), false);
                NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("screen.seeking_immortals.alchemy_menu.title");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inv, Player p) {
                        return new com.xunxian.seekingimmortals.menu.AlchemyFurnaceMenu(id, inv, furnace, furnace.getContainerData());
                    }
                }, buf -> buf.writeBlockPos(pos));
                return InteractionResult.CONSUME;
            }
            furnace.interact(serverPlayer, held);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!oldState.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace) {
            furnace.dropStoredContents();
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }
}
