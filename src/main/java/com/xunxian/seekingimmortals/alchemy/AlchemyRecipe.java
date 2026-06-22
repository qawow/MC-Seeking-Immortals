package com.xunxian.seekingimmortals.alchemy;

import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record AlchemyRecipe(String id, Component displayName, Item output, int outputCount, int manaCost,
                            int cookTicks, double successRate, double explosionChance,
                            List<IngredientRequirement> ingredients) {
    public static final List<AlchemyRecipe> MVP_RECIPES = List.of(
            new AlchemyRecipe("cultivation_pill", Component.translatable("item.seeking_immortals.cultivation_pill"),
                    ModItems.CULTIVATION_PILL.get(), 1, 20, 20 * 20, 0.80D, 0.03D, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_GRASS.get(), 2),
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 1))),
            new AlchemyRecipe("foundation_building_pill_low", Component.translatable("item.seeking_immortals.foundation_building_pill_low"),
                    ModItems.FOUNDATION_BUILDING_PILL_LOW.get(), 1, 40, 40 * 20, 0.65D, 0.05D, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_GRASS.get(), 4),
                    new IngredientRequirement(ModItems.DRAGON_BLOOD_GRASS.get(), 1),
                    new IngredientRequirement(ModItems.BEAST_CORE.get(), 1))),
            new AlchemyRecipe("calming_pill_low", Component.translatable("item.seeking_immortals.calming_pill_low"),
                    ModItems.CALMING_PILL_LOW.get(), 1, 25, 25 * 20, 0.75D, 0.03D, List.of(
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 2),
                    new IngredientRequirement(ModItems.PHOENIX_FEATHER_FLOWER.get(), 1))),
            new AlchemyRecipe("qi_recovery_pill", Component.translatable("item.seeking_immortals.qi_recovery_pill"),
                    ModItems.QI_RECOVERY_PILL.get(), 1, 20, 20 * 20, 0.82D, 0.03D, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_GRASS.get(), 1),
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 2)))
    );

    public static Optional<AlchemyRecipe> findByHeldIngredient(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        if (stack.is(ModItems.DRAGON_BLOOD_GRASS.get()) || stack.is(ModItems.BEAST_CORE.get())) {
            return findById("foundation_building_pill_low");
        }
        if (stack.is(ModItems.PHOENIX_FEATHER_FLOWER.get())) {
            return findById("calming_pill_low");
        }
        if (stack.is(ModItems.CLOUD_MUSHROOM.get())) {
            return findById("qi_recovery_pill");
        }
        if (stack.is(ModItems.SPIRIT_GRASS.get())) {
            return findById("cultivation_pill");
        }
        return MVP_RECIPES.stream()
                .filter(recipe -> recipe.ingredients().stream().anyMatch(ingredient -> stack.is(ingredient.item())))
                .findFirst();
    }

    public static Optional<AlchemyRecipe> findById(String id) {
        return MVP_RECIPES.stream().filter(recipe -> recipe.id().equals(id)).findFirst();
    }

    public String describeIngredients() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ingredients.size(); i++) {
            IngredientRequirement ingredient = ingredients.get(i);
            if (i > 0) builder.append(", ");
            builder.append(ingredient.count()).append("x ")
                    .append(Component.translatable(ingredient.item().getDescriptionId()).getString());
        }
        return builder.toString();
    }

    public record IngredientRequirement(Item item, int count) {}
}
