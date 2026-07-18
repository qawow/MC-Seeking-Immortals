package com.xunxian.seekingimmortals.item;

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
