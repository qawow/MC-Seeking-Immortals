package com.xunxian.seekingimmortals.block;

import com.xunxian.seekingimmortals.block.entity.AlchemyFurnaceBlockEntity;
import com.xunxian.seekingimmortals.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AlchemyFurnaceBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(1.0D, 0.0D, 1.0D, 15.0D, 14.0D, 15.0D);
    private final int tier;

    public AlchemyFurnaceBlock(Properties properties) {
        this(properties, 1);
    }

    public AlchemyFurnaceBlock(Properties properties, int tier) {
        super(properties);
        this.tier = Math.max(1, tier);
    }

    public int tier() {
        return tier;
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AlchemyFurnaceBlockEntity furnace && player instanceof ServerPlayer serverPlayer) {
            ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty()) {
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
