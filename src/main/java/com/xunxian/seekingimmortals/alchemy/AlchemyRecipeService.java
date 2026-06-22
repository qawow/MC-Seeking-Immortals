package com.xunxian.seekingimmortals.alchemy;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class AlchemyRecipeService {
    private AlchemyRecipeService() {}

    public static boolean canCraft(ServerPlayer player, AlchemyRecipe recipe) {
        return hasIngredients(player.getInventory(), recipe)
                && CultivationHelper.get(player).map(cultivation -> cultivation.getSpiritualPower() >= recipe.manaCost()).orElse(false);
    }

    public static boolean consumeInputs(ServerPlayer player, AlchemyRecipe recipe) {
        if (!canCraft(player, recipe)) return false;
        if (!player.getAbilities().instabuild) {
            for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
                removeItems(player.getInventory(), ingredient);
            }
            CultivationHelper.get(player).ifPresent(cultivation -> cultivation.consumeSpiritualPower(recipe.manaCost()));
        }
        return true;
    }

    public static double successRate(ServerLevel level, ServerPlayer player, AlchemyRecipe recipe) {
        double alchemyBonus = getAlchemySkillBonus(player);
        double materialBonus = 0.0D;
        double auraBonus = SpiritualAuraManager.getAuraInfo(level, player.blockPosition()).leyline() ? 0.05D : 0.0D;
        return Math.min(0.95D, Math.max(0.05D, recipe.successRate() + alchemyBonus + materialBonus + auraBonus));
    }

    public static double explosionChance(ServerPlayer player, AlchemyRecipe recipe) {
        return Math.max(0.0D, recipe.explosionChance() - getAlchemySkillBonus(player) * 0.5D);
    }

    public static String missingSummary(ServerPlayer player, AlchemyRecipe recipe) {
        StringBuilder builder = new StringBuilder();
        Inventory inventory = player.getInventory();
        for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
            int owned = countItems(inventory, ingredient);
            if (owned < ingredient.count()) {
                if (builder.length() > 0) builder.append(", ");
                builder.append(ComponentName.item(ingredient.item().getDescriptionId()))
                        .append(" ")
                        .append(owned)
                        .append("/")
                        .append(ingredient.count());
            }
        }
        int mana = CultivationHelper.get(player).map(PlayerCultivation::getSpiritualPower).orElse(0);
        if (mana < recipe.manaCost()) {
            if (builder.length() > 0) builder.append(", ");
            builder.append("mana ").append(mana).append("/").append(recipe.manaCost());
        }
        return builder.length() == 0 ? "" : builder.toString();
    }

    private static boolean hasIngredients(Inventory inventory, AlchemyRecipe recipe) {
        for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
            if (countItems(inventory, ingredient) < ingredient.count()) return false;
        }
        return true;
    }

    private static int countItems(Inventory inventory, AlchemyRecipe.IngredientRequirement ingredient) {
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (stack.is(ingredient.item())) count += stack.getCount();
        }
        return count;
    }

    private static void removeItems(Inventory inventory, AlchemyRecipe.IngredientRequirement ingredient) {
        int remaining = ingredient.count();
        for (ItemStack stack : inventory.items) {
            if (!stack.is(ingredient.item())) continue;
            int remove = Math.min(remaining, stack.getCount());
            stack.shrink(remove);
            remaining -= remove;
            if (remaining <= 0) return;
        }
    }

    private static double getAlchemySkillBonus(ServerPlayer player) {
        return 0.0D;
    }

    private static final class ComponentName {
        static String item(String descriptionId) {
            return net.minecraft.network.chat.Component.translatable(descriptionId).getString();
        }
    }
}
