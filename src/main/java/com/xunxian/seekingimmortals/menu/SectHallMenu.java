package com.xunxian.seekingimmortals.menu;

import com.xunxian.seekingimmortals.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Wave490: productized sect hall MenuType. */
public class SectHallMenu extends AbstractContainerMenu {
    private final String focusSectId;

    public SectHallMenu(int id, Inventory inv, String focusSectId) {
        super(ModMenus.SECT_HALL.get(), id);
        this.focusSectId = focusSectId == null ? "" : focusSectId;
    }

    public static SectHallMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        return new SectHallMenu(id, inv, buf.readUtf(128));
    }

    public String focusSectId() {
        return focusSectId;
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
