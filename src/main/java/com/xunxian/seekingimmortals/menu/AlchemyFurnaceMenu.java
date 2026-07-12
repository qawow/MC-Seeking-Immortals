package com.xunxian.seekingimmortals.menu;

import com.xunxian.seekingimmortals.block.entity.AlchemyFurnaceBlockEntity;
import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Real MenuType GUI for alchemy furnace (Wave54).
 * Slots: 0 formula, 1 ingredient, 2 lid, 3 fire, 4 output.
 */
public class AlchemyFurnaceMenu extends AbstractContainerMenu {
    public static final int SLOT_FORMULA = 0;
    public static final int SLOT_INGREDIENT = 1;
    public static final int SLOT_LID = 2;
    public static final int SLOT_FIRE = 3;
    public static final int SLOT_OUTPUT = 4;

    private final AlchemyFurnaceBlockEntity furnace;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final ItemStackHandler items;

    public AlchemyFurnaceMenu(int id, Inventory inv, AlchemyFurnaceBlockEntity furnace, ContainerData data) {
        super(ModMenus.ALCHEMY_FURNACE.get(), id);
        this.furnace = furnace;
        this.access = ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos());
        this.data = data;
        this.items = furnace.getItemHandler();

        addSlot(new SlotItemHandler(items, SLOT_FORMULA, 26, 20));
        addSlot(new SlotItemHandler(items, SLOT_INGREDIENT, 62, 20));
        addSlot(new SlotItemHandler(items, SLOT_LID, 26, 48));
        addSlot(new SlotItemHandler(items, SLOT_FIRE, 62, 48));
        addSlot(new SlotItemHandler(items, SLOT_OUTPUT, 116, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

        public static AlchemyFurnaceMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        net.minecraft.core.BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        AlchemyFurnaceBlockEntity furnace = be instanceof AlchemyFurnaceBlockEntity a
                ? a
                : new AlchemyFurnaceBlockEntity(pos, inv.player.level().getBlockState(pos));
        return new AlchemyFurnaceMenu(id, inv, furnace, furnace.getContainerData());
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getTotal() {
        return Math.max(1, data.get(1));
    }

    public AlchemyFurnaceBlockEntity getFurnace() {
        return furnace;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 5) {
                if (!moveItemStackTo(stack, 5, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 4, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, furnace.getBlockState().getBlock());
    }
}
