package com.xunxian.seekingimmortals.menu;

import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Wave490: productized sect hall MenuType. */
public class SectHallMenu extends AbstractContainerMenu {
    private final String focusSectId;
    private final MenuAccessContext accessContext;

    public SectHallMenu(int id, Inventory inv, String focusSectId) {
        this(id, inv, focusSectId, MenuAccessContext.client(0L));
    }

    public SectHallMenu(int id, Inventory inv, String focusSectId, MenuAccessContext accessContext) {
        super(ModMenus.SECT_HALL.get(), id);
        this.focusSectId = focusSectId == null ? "" : focusSectId.trim().toLowerCase(java.util.Locale.ROOT);
        this.accessContext = accessContext;
    }

    public static SectHallMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        return new SectHallMenu(id, inv, buf.readUtf(128), MenuAccessContext.client(buf.readLong()));
    }

    public String focusSectId() {
        return focusSectId;
    }

    public long accessToken() {
        return accessContext.token();
    }

    public boolean authorizes(ServerPlayer player, long presentedToken) {
        return accessContext.authorizes(player, presentedToken);
    }

    public boolean authorizesSect(String requestedSectId) {
        String requested = requestedSectId == null ? "" : requestedSectId.trim().toLowerCase(java.util.Locale.ROOT);
        return focusSectId.isBlank() || focusSectId.equals(requested);
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
