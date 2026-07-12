package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Natal artifact binding (Wave51 Phase13 depth).
 * Stores one bound artifact id on player persistent data.
 */
public final class NatalBindingService {
    private static final String ROOT = "seeking_immortals_natal_binding";
    private static final String KEY_ID = "ArtifactId";
    private static final String KEY_GROWTH = "Growth";

    private NatalBindingService() {}

    public static boolean bind(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof ArtifactCatalogItem catalogItem)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.not_artifact"), true);
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (!root.getString(KEY_ID).isBlank() && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.already",
                    root.getString(KEY_ID)), true);
            return false;
        }
        root.putString(KEY_ID, catalogItem.artifactId());
        root.putInt(KEY_GROWTH, Math.max(0, root.getInt(KEY_GROWTH)));
        player.getPersistentData().put(ROOT, root);
        stack.getOrCreateTag().putBoolean("SeekingImmortalsNatalBound", true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.bound",
                catalogItem.artifactId()), true);
        return true;
    }

    public static boolean grow(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        String id = root.getString(KEY_ID);
        if (id.isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.none"), true);
            return false;
        }
        int growth = Math.min(100, root.getInt(KEY_GROWTH) + 1);
        root.putInt(KEY_GROWTH, growth);
        player.getPersistentData().put(ROOT, root);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.grown", id, growth), true);
        return true;
    }

    public static String boundId(ServerPlayer player) {
        return player.getPersistentData().getCompound(ROOT).getString(KEY_ID);
    }

    public static int growth(ServerPlayer player) {
        return player.getPersistentData().getCompound(ROOT).getInt(KEY_GROWTH);
    }
}
