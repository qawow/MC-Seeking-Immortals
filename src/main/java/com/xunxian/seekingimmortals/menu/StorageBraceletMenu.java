package com.xunxian.seekingimmortals.menu;

import com.xunxian.seekingimmortals.artifact.ArtifactStorageService;
import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Real slot GUI for storage bracelet (Wave54).
 * Backed by bracelet NBT list via ArtifactStorageService item handler bridge.
 */
public class StorageBraceletMenu extends AbstractContainerMenu {
    private final ItemStackHandler handler;
    private final InteractionHand hand;
    private final ItemStack boundBracelet;
    private final int boundHotbarSlot;
    private final int storageSlots;

    public StorageBraceletMenu(int id, Inventory inv, InteractionHand hand, ItemStack bracelet, int storageSlots) {
        super(ModMenus.STORAGE_BRACELET.get(), id);
        this.hand = hand;
        this.boundBracelet = bracelet;
        this.boundHotbarSlot = hand == InteractionHand.MAIN_HAND ? inv.selected : -1;
        this.storageSlots = Math.max(1, Math.min(27, storageSlots));
        this.handler = ArtifactStorageService.createHandler(bracelet, this.storageSlots);

        int rows = (this.storageSlots + 8) / 9;
        for (int i = 0; i < this.storageSlots; i++) {
            int row = i / 9;
            int col = i % 9;
            addSlot(new SlotItemHandler(handler, i, 8 + col * 18, 18 + row * 18));
        }

        int playerInvY = 18 + rows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            // Prevent moving the open bracelet itself into storage while menu open.
            final int hotbarIndex = col;
            addSlot(new Slot(inv, hotbarIndex, 8 + col * 18, playerInvY + 58) {
                @Override
                public boolean mayPickup(Player player) {
                    ItemStack stack = getItem();
                    if (hand == InteractionHand.MAIN_HAND && hotbarIndex == boundHotbarSlot) {
                        return false;
                    }
                    return super.mayPickup(player);
                }
            });
        }
    }

    public static StorageBraceletMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        InteractionHand hand = buf.readEnum(InteractionHand.class);
        int slots = buf.readVarInt();
        ItemStack bracelet = hand == InteractionHand.MAIN_HAND ? inv.player.getMainHandItem() : inv.player.getOffhandItem();
        return new StorageBraceletMenu(id, inv, hand, bracelet, slots);
    }

    public int storageSlots() {
        return storageSlots;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && stillValid(player)) {
            ArtifactStorageService.writeHandler(boundBracelet, handler);
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!stillValid(player)) {
            return;
        }
        boolean swapsBoundHand = clickType == ClickType.SWAP
                && ((hand == InteractionHand.MAIN_HAND && button == boundHotbarSlot)
                || (hand == InteractionHand.OFF_HAND && button == 40));
        if (swapsBoundHand) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
        if (!player.level().isClientSide && stillValid(player)) {
            ArtifactStorageService.writeHandler(boundBracelet, handler);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < storageSlots) {
                if (!moveItemStackTo(stack, storageSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, storageSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (!player.level().isClientSide) {
                ArtifactStorageService.writeHandler(boundBracelet, handler);
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack bracelet = hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
        if (bracelet.isEmpty() || !ArtifactStorageService.supportsStack(bracelet)) {
            return false;
        }
        if (player.level().isClientSide) {
            // Client inventory synchronization may replace an NBT-bearing stack instance.
            // Keep prediction open for the same supported item; the server path below remains
            // anchored to the exact opening instance and rechecks ownership/integrity.
            return boundBracelet != null && !boundBracelet.isEmpty()
                    && bracelet.getItem() == boundBracelet.getItem()
                    && ArtifactStorageService.storageSlots(bracelet) == storageSlots;
        }
        if (bracelet != boundBracelet) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return ArtifactStorageService.isContinuouslyAuthorized(serverPlayer, boundBracelet);
        }
        return true;
    }
}
