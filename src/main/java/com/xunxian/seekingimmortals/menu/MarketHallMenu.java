package com.xunxian.seekingimmortals.menu;

import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Wave490: productized market/trade hall MenuType (server-opened via NetworkHooks).
 * No inventory slots — authority still flows through ShopActionPacket.
 */
public class MarketHallMenu extends AbstractContainerMenu {
    private final String shopId;

    public MarketHallMenu(int id, Inventory inv, String shopId) {
        super(ModMenus.MARKET_HALL.get(), id);
        this.shopId = shopId == null || shopId.isBlank() ? "herbal_stall" : shopId;
    }

    public static MarketHallMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        return new MarketHallMenu(id, inv, buf.readUtf(128));
    }

    public String shopId() {
        return shopId;
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
