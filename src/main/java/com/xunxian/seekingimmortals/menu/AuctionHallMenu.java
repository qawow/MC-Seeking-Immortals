package com.xunxian.seekingimmortals.menu;

import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Wave490: productized auction hall MenuType. */
public class AuctionHallMenu extends AbstractContainerMenu {
    private final MenuAccessContext accessContext;

    public AuctionHallMenu(int id, Inventory inv) {
        this(id, inv, MenuAccessContext.client(0L));
    }

    public AuctionHallMenu(int id, Inventory inv, MenuAccessContext accessContext) {
        super(ModMenus.AUCTION_HALL.get(), id);
        this.accessContext = accessContext;
    }

    public static AuctionHallMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        return new AuctionHallMenu(id, inv, MenuAccessContext.client(buf.readLong()));
    }

    public long accessToken() {
        return accessContext.token();
    }

    public boolean authorizes(ServerPlayer player, long presentedToken) {
        return accessContext.authorizes(player, presentedToken);
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
