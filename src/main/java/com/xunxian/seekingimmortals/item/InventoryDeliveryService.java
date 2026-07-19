package com.xunxian.seekingimmortals.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

public final class InventoryDeliveryService {
    private InventoryDeliveryService() {}

    public static void giveOrDrop(ServerPlayer player, ItemStack source) {
        if (player == null || source == null || source.isEmpty()) {
            return;
        }
        for (int count : splitCounts(source.getCount(), source.getMaxStackSize())) {
            ItemStack batch = source.copy();
            batch.setCount(count);
            ItemHandlerHelper.giveItemToPlayer(player, batch);
        }
    }

    /**
     * Prefer immediate delivery. When the player inventory cannot fully accept the stack,
     * enqueue the whole gift into the persistent outbox instead of world-dropping it.
     */
    public static void giveOrEnqueue(ServerPlayer player, ItemStack source, String reason) {
        if (player == null || source == null || source.isEmpty()) {
            return;
        }
        if (player.getServer() == null || canFullyAccept(player, source)) {
            giveOrDrop(player, source);
            return;
        }
        DeliveryOutboxSavedData.get(player.getServer()).enqueue(player.getUUID(), source, reason);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.delivery.queued",
                source.getHoverName(), Math.max(1, source.getCount())), true);
    }

    public static int claimQueued(ServerPlayer player) {
        return DeliveryOutboxSavedData.claimFor(player);
    }

    public static boolean canFullyAccept(ServerPlayer player, ItemStack source) {
        if (player == null || source == null || source.isEmpty()) {
            return true;
        }
        int remaining = source.getCount();
        int maxStack = Math.max(1, source.getMaxStackSize());
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack slot = player.getInventory().items.get(i);
            if (slot.isEmpty()) {
                remaining -= Math.min(remaining, maxStack);
                continue;
            }
            if (ItemStack.isSameItemSameTags(slot, source)) {
                remaining -= Math.min(remaining, Math.max(0, maxStack - slot.getCount()));
            }
        }
        return remaining <= 0;
    }

    static List<Integer> splitCounts(int count, int maxStackSize) {
        int remaining = Math.max(0, count);
        int max = Math.max(1, maxStackSize);
        List<Integer> batches = new ArrayList<>();
        while (remaining > 0) {
            int batch = Math.min(max, remaining);
            batches.add(batch);
            remaining -= batch;
        }
        return List.copyOf(batches);
    }
}
