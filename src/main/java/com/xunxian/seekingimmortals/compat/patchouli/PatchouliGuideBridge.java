package com.xunxian.seekingimmortals.compat.patchouli;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.api.PatchouliAPI;

public final class PatchouliGuideBridge {
    private PatchouliGuideBridge() {}

    public static ItemStack getBookStack(ResourceLocation bookId) {
        return PatchouliAPI.get().getBookStack(bookId);
    }
}
