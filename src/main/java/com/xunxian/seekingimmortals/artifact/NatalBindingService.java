package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;

/**
 * Natal artifact binding (Wave51 / Wave456 / Wave459).
 * Stores one bound artifact id + growth on player persistent data.
 */
public final class NatalBindingService {
    private static final String ROOT = "seeking_immortals_natal_binding";
    private static final String KEY_ID = "ArtifactId";
    private static final String KEY_GROWTH = "Growth";
    public static final String STACK_BOUND = "SeekingImmortalsNatalBound";
    public static final String STACK_GROWTH = "SeekingImmortalsNatalGrowth";

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
                    boundDisplay(root.getString(KEY_ID))), true);
            return false;
        }
        root.putString(KEY_ID, catalogItem.artifactId());
        int growth = Math.max(0, root.getInt(KEY_GROWTH));
        root.putInt(KEY_GROWTH, growth);
        player.getPersistentData().put(ROOT, root);
        stack.getOrCreateTag().putBoolean(STACK_BOUND, true);
        stack.getOrCreateTag().putInt(STACK_GROWTH, growth);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.bound",
                boundDisplay(catalogItem.artifactId())), true);
        return true;
    }

    public static boolean grow(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        String id = root.getString(KEY_ID);
        if (id.isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.none"), true);
            return false;
        }
        int prev = root.getInt(KEY_GROWTH);
        int growth = Math.min(100, prev + 1);
        root.putInt(KEY_GROWTH, growth);
        player.getPersistentData().put(ROOT, root);
        mirrorGrowthToHeld(player, growth);
        if (growth == 25 || growth == 50 || growth == 75 || growth == 100) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.natal.milestone", boundDisplay(id), growth), true);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.grown",
                    boundDisplay(id), growth), true);
        }
        return true;
    }

    public static String boundId(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        return player.getPersistentData().getCompound(ROOT).getString(KEY_ID);
    }

    public static int growth(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return player.getPersistentData().getCompound(ROOT).getInt(KEY_GROWTH);
    }

    public static int growthFromStack(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return 0;
        }
        return Math.max(0, stack.getTag().getInt(STACK_GROWTH));
    }

    private static Component boundDisplay(String id) {
        return ArtifactDataService.builtin().findArtifact(id)
                .filter(definition -> PlayerDisplayText.isSafe(definition.display()))
                .<Component>map(definition -> Component.literal(definition.display().trim()))
                .orElseGet(() -> PlayerDisplayText.itemName(id));
    }

    private static void mirrorGrowthToHeld(ServerPlayer player, int growth) {
        for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.getItem() instanceof ArtifactCatalogItem catalog
                    && catalog.artifactId().equals(boundId(player))) {
                stack.getOrCreateTag().putBoolean(STACK_BOUND, true);
                stack.getOrCreateTag().putInt(STACK_GROWTH, growth);
            }
        }
    }
}
