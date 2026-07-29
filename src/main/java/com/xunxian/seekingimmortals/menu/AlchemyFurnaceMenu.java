package com.xunxian.seekingimmortals.menu;

import com.xunxian.seekingimmortals.block.entity.AlchemyFurnaceBlockEntity;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyFormulaItem;
import com.xunxian.seekingimmortals.item.alchemy.AlchemyTieredItem;
import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Real MenuType GUI for alchemy furnace.
 * Wave499: slots are authority for formula/lid/fire; typed mayPlace filters.
 * Slots: 0 formula, 1 ingredient, 2 lid, 3 fire, 4 output.
 */
public class AlchemyFurnaceMenu extends AbstractContainerMenu {
    public static final int SLOT_FORMULA = AlchemyFurnaceBlockEntity.SLOT_FORMULA;
    public static final int SLOT_INGREDIENT = AlchemyFurnaceBlockEntity.SLOT_INGREDIENT;
    public static final int SLOT_LID = AlchemyFurnaceBlockEntity.SLOT_LID;
    public static final int SLOT_FIRE = AlchemyFurnaceBlockEntity.SLOT_FIRE;
    public static final int SLOT_OUTPUT = AlchemyFurnaceBlockEntity.SLOT_OUTPUT;

    private final AlchemyFurnaceBlockEntity furnace;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public AlchemyFurnaceMenu(int id, Inventory inv, AlchemyFurnaceBlockEntity furnace, ContainerData data) {
        super(ModMenus.ALCHEMY_FURNACE.get(), id);
        this.furnace = furnace;
        this.access = ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos());
        this.data = data;
        ItemStackHandler items = furnace.getItemHandler();

        addSlot(new SlotItemHandler(items, SLOT_FORMULA, 26, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AlchemyFormulaItem;
            }
        });
        addSlot(new SlotItemHandler(items, SLOT_INGREDIENT, 62, 20));
        // Wave500: lid is a world block above the furnace; this slot is disabled/display-only.
        addSlot(new SlotItemHandler(items, SLOT_LID, 26, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player playerIn) {
                return false;
            }
        });
        addSlot(new SlotItemHandler(items, SLOT_FIRE, 62, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AlchemyTieredItem tiered
                        && tiered.componentType() == AlchemyTieredItem.ComponentType.FIRE;
            }
        });
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
        return Math.max(0, data.get(0));
    }

    public int getTotal() {
        return Math.max(0, data.get(1));
    }

    /** True only while a recipe has a positive remaining duration. */
    public boolean isCrafting() {
        return getProgress() > 0 && getTotal() > 0;
    }

    public boolean isFormed() {
        return data.get(2) != 0;
    }

    public boolean hasEarthFireRoom() {
        return data.get(3) != 0;
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
            } else {
                // Prefer typed furnace slots when shift-clicking from player inventory.
                // Lids are placeable world blocks, not GUI items.
                boolean moved;
                if (stack.getItem() instanceof AlchemyFormulaItem) {
                    moved = moveItemStackTo(stack, SLOT_FORMULA, SLOT_FORMULA + 1, false);
                } else if (stack.getItem() instanceof AlchemyTieredItem tiered
                        && tiered.componentType() == AlchemyTieredItem.ComponentType.FIRE) {
                    moved = moveItemStackTo(stack, SLOT_FIRE, SLOT_FIRE + 1, false);
                } else {
                    moved = moveItemStackTo(stack, SLOT_INGREDIENT, SLOT_INGREDIENT + 1, false);
                }
                if (!moved) {
                    return ItemStack.EMPTY;
                }
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
