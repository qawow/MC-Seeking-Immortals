package com.xunxian.seekingimmortals.worldpack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class InventoryReservation {
    private final List<ItemStack> consumed;
    private boolean refunded;

    private InventoryReservation(List<ItemStack> consumed) {
        this.consumed = consumed.stream().map(ItemStack::copy).toList();
    }

    static InventoryReservation none() {
        return new InventoryReservation(List.of());
    }

    static InventoryReservation consume(ServerPlayer player, Map<Item, Integer> costs) {
        if (player == null || costs == null) {
            return null;
        }
        if (player.getAbilities().instabuild || costs.isEmpty()) {
            return none();
        }
        for (Map.Entry<Item, Integer> entry : costs.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0
                    || !hasItems(player, entry.getKey(), entry.getValue())) {
                return null;
            }
        }

        List<ItemStack> consumed = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : costs.entrySet()) {
            int remaining = entry.getValue();
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.is(entry.getKey()) || stack.isEmpty()) {
                    continue;
                }
                int take = Math.min(remaining, stack.getCount());
                consumed.add(copyForReservation(stack, take));
                stack.shrink(take);
                remaining -= take;
                if (remaining == 0) {
                    break;
                }
            }
            if (remaining != 0) {
                InventoryReservation partial = new InventoryReservation(consumed);
                partial.refund(player);
                return null;
            }
        }
        return new InventoryReservation(consumed);
    }

    static boolean hasItems(ServerPlayer player, Item item, int count) {
        if (count <= 0) {
            return true;
        }
        if (player == null || item == null) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && !stack.isEmpty()) {
                found += stack.getCount();
                if (found >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    static ItemStack copyForReservation(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(Math.max(0, count));
        return copy;
    }

    void refund(ServerPlayer player) {
        if (refunded || player == null) {
            return;
        }
        // Mark refunded only after every stack is handed off, so a mid-loop
        // failure can be retried instead of silently dropping the tail.
        for (ItemStack consumedStack : consumed) {
            com.xunxian.seekingimmortals.item.InventoryDeliveryService.giveOrEnqueue(
                    player, consumedStack.copy(), "inventory_reservation_refund");
        }
        refunded = true;
    }
}
