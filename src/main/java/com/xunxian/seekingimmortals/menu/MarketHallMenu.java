package com.xunxian.seekingimmortals.menu;

import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
    private final MenuAccessContext accessContext;

    public MarketHallMenu(int id, Inventory inv, String shopId) {
        this(id, inv, shopId, MenuAccessContext.client(0L));
    }

    public MarketHallMenu(int id, Inventory inv, String shopId, MenuAccessContext accessContext) {
        super(ModMenus.MARKET_HALL.get(), id);
        this.shopId = shopId == null || shopId.isBlank() ? "market_herbal_stall" : shopId;
        this.accessContext = accessContext;
    }

    public static MarketHallMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        return new MarketHallMenu(id, inv, buf.readUtf(128), MenuAccessContext.client(buf.readLong()));
    }

    public String shopId() {
        return shopId;
    }

    public long accessToken() {
        return accessContext.token();
    }

    public boolean authorizes(ServerPlayer player, String requestedShopId, long presentedToken) {
        String requested = requestedShopId == null ? "" : requestedShopId.trim().toLowerCase(java.util.Locale.ROOT);
        return shopId.equals(requested) && accessContext.authorizes(player, presentedToken);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return accessContext.isValid(player);
    }
}
