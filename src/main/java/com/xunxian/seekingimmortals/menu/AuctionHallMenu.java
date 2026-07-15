package com.xunxian.seekingimmortals.menu;

import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Wave490: productized auction hall MenuType. */
public class AuctionHallMenu extends AbstractContainerMenu {
    public AuctionHallMenu(int id, Inventory inv) {
        super(ModMenus.AUCTION_HALL.get(), id);
    }

    public static AuctionHallMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        return new AuctionHallMenu(id, inv);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player != null && player.isAlive();
    }
}
