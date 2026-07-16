package com.xunxian.seekingimmortals.compat.patchouli;

import com.xunxian.seekingimmortals.compat.ModCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.api.PatchouliAPI;

/**
 * Isolated Patchouli API bridge. Callers must check {@link ModCompat#PATCHOULI_LOADED}
 * before invoking methods that touch Patchouli classes when possible; safe helpers
 * also re-check to avoid hard crashes if the optional mod is absent.
 */
public final class PatchouliGuideBridge {
    private PatchouliGuideBridge() {}

    public static ItemStack getBookStack(ResourceLocation bookId) {
        return PatchouliAPI.get().getBookStack(bookId);
    }

    /** Prefer this entry from common code paths; returns empty when Patchouli is missing. */
    public static ItemStack getBookStackSafe(ResourceLocation bookId) {
        if (!ModCompat.PATCHOULI_LOADED || bookId == null) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack stack = getBookStack(bookId);
            return stack == null ? ItemStack.EMPTY : stack;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }
}
