package com.xunxian.seekingimmortals.alchemy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Persistent player knowledge of alchemy recipe ids, earned by studying formula items.
 * Furnaces accept either an installed formula stack or studied knowledge of that recipe.
 */
public final class AlchemyFormulaKnowledge {
    public static final String STUDIED_FORMULAS_TAG = "seeking_immortals_studied_formulas";

    private AlchemyFormulaKnowledge() {}

    public static void copyProgressionData(CompoundTag originalData, CompoundTag clonedData) {
        if (originalData == null || clonedData == null) {
            return;
        }
        if (originalData.contains(STUDIED_FORMULAS_TAG) && originalData.get(STUDIED_FORMULAS_TAG) != null) {
            clonedData.put(STUDIED_FORMULAS_TAG, originalData.get(STUDIED_FORMULAS_TAG).copy());
        }
    }

    public static boolean hasStudied(ServerPlayer player, String recipeId) {
        if (player == null) {
            return false;
        }
        String id = normalize(recipeId);
        if (id.isBlank()) {
            return false;
        }
        return player.getPersistentData().getCompound(STUDIED_FORMULAS_TAG).getBoolean(id);
    }

    /**
     * Marks a recipe as studied. Returns false when already known or invalid.
     */
    public static boolean study(ServerPlayer player, String recipeId) {
        if (player == null) {
            return false;
        }
        String id = normalize(recipeId);
        if (id.isBlank()) {
            return false;
        }
        if (hasStudied(player, id)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.alchemy_formula.already_studied",
                    Component.translatable("alchemy_recipe.seeking_immortals." + id)), true);
            return false;
        }
        CompoundTag tag = player.getPersistentData().getCompound(STUDIED_FORMULAS_TAG).copy();
        tag.putBoolean(id, true);
        player.getPersistentData().put(STUDIED_FORMULAS_TAG, tag);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.alchemy_formula.studied",
                Component.translatable("alchemy_recipe.seeking_immortals." + id)), true);
        return true;
    }

    public static int studiedCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return player.getPersistentData().getCompound(STUDIED_FORMULAS_TAG).size();
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
