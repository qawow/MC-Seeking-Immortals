package com.xunxian.seekingimmortals.alchemy;

import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.item.pill.PillQuality;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record AlchemyRecipe(String id, Component displayName, List<Item> outputsByQuality, int outputCount, int manaCost,
                            int cookTicks, double successRate, double explosionChance,
                            int requiredFurnaceTier, int idealFireTier, Realm minControlRealm, boolean controlled,
                            boolean requiresEarthFireRoom,
                            List<IngredientRequirement> ingredients) {

    public boolean isQualityVariable() {
        if (outputsByQuality.isEmpty()) return false;
        Item first = outputsByQuality.get(0);
        return outputsByQuality.stream().anyMatch(item -> item != first);
    }

    public Item output() {
        return outputsByQuality.isEmpty() ? null : outputsByQuality.get(PillQuality.LOW.ordinal());
    }

    public Item outputForQuality(PillQuality quality) {
        if (outputsByQuality.isEmpty()) return output();
        int idx = quality.ordinal();
        if (idx < 0 || idx >= outputsByQuality.size()) return outputsByQuality.get(PillQuality.LOW.ordinal());
        Item item = outputsByQuality.get(idx);
        return item != null ? item : outputsByQuality.get(PillQuality.LOW.ordinal());
    }

    public static final List<AlchemyRecipe> MVP_RECIPES = List.of(
            new AlchemyRecipe("cultivation_pill", Component.translatable("item.seeking_immortals.cultivation_pill"),
                    uniform(ModItems.CULTIVATION_PILL.get()), 1, 20, 20 * 20, 0.80D, 0.03D,
                    1, 1, Realm.MORTAL, false, false, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_GRASS.get(), 2),
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 1))),
            new AlchemyRecipe("foundation_building_pill_low", Component.translatable("item.seeking_immortals.foundation_building_pill_low"),
                    List.of(
                            ModItems.FOUNDATION_BUILDING_PILL_LOW.get(),
                            ModItems.FOUNDATION_BUILDING_PILL_MID.get(),
                            ModItems.FOUNDATION_BUILDING_PILL_HIGH.get(),
                            ModItems.FOUNDATION_BUILDING_PILL_SUPREME.get()),
                    1, 40, 40 * 20, 0.65D, 0.05D,
                    2, 2, Realm.QI_REFINING, true, false, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_GRASS.get(), 4),
                    new IngredientRequirement(ModItems.DRAGON_BLOOD_GRASS.get(), 1),
                    new IngredientRequirement(ModItems.BEAST_CORE.get(), 1))),
            new AlchemyRecipe("calming_pill_low", Component.translatable("item.seeking_immortals.calming_pill_low"),
                    List.of(
                            ModItems.CALMING_PILL_LOW.get(),
                            ModItems.CALMING_PILL_MID.get(),
                            ModItems.CALMING_PILL_HIGH.get(),
                            ModItems.CALMING_PILL_SUPREME.get()),
                    1, 25, 25 * 20, 0.75D, 0.03D,
                    1, 1, Realm.QI_REFINING, false, false, List.of(
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 2),
                    new IngredientRequirement(ModItems.PHOENIX_FEATHER_FLOWER.get(), 1))),
            new AlchemyRecipe("qingxin_pill", Component.translatable("item.seeking_immortals.qingxin_pill"),
                    uniform(ModItems.QINGXIN_PILL.get()), 1, 20, 22 * 20, 0.65D, 0.04D,
                    1, 2, Realm.QI_REFINING, false, false, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_GRASS.get(), 3))),
            new AlchemyRecipe("qi_recovery_pill", Component.translatable("item.seeking_immortals.qi_recovery_pill"),
                    uniform(ModItems.QI_RECOVERY_PILL.get()), 1, 20, 20 * 20, 0.82D, 0.03D,
                    1, 1, Realm.MORTAL, false, false, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_GRASS.get(), 1),
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 2))),
            new AlchemyRecipe("spirit_gathering_pill", Component.translatable("item.seeking_immortals.spirit_gathering_pill"),
                    quality(ModItems.SPIRIT_GATHERING_PILL.get(), ModItems.SPIRIT_GATHERING_PILL_MID.get(),
                            ModItems.SPIRIT_GATHERING_PILL_HIGH.get(), ModItems.SPIRIT_GATHERING_PILL_SUPREME.get()),
                    1, 18, 18 * 20, 0.84D, 0.02D,
                    1, 1, Realm.MORTAL, false, false, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_GRASS.get(), 2),
                    new IngredientRequirement(ModItems.SPIRIT_IRON.get(), 1))),
            new AlchemyRecipe("fire_origin_pill", Component.translatable("item.seeking_immortals.fire_origin_pill"),
                    quality(ModItems.FIRE_ORIGIN_PILL.get(), ModItems.FIRE_ORIGIN_PILL_MID.get(),
                            ModItems.FIRE_ORIGIN_PILL_HIGH.get(), ModItems.FIRE_ORIGIN_PILL_SUPREME.get()),
                    1, 26, 24 * 20, 0.72D, 0.04D,
                    2, 2, Realm.QI_REFINING, false, false, List.of(
                    new IngredientRequirement(ModItems.PHOENIX_FEATHER_FLOWER.get(), 1),
                    new IngredientRequirement(ModItems.FIRE_ELEMENT_SPIRIT_STONE.get(), 1))),
            new AlchemyRecipe("ice_fire_pill", Component.translatable("item.seeking_immortals.ice_fire_pill"),
                    quality(ModItems.ICE_FIRE_PILL.get(), ModItems.ICE_FIRE_PILL_MID.get(),
                            ModItems.ICE_FIRE_PILL_HIGH.get(), ModItems.ICE_FIRE_PILL_SUPREME.get()),
                    1, 34, 30 * 20, 0.62D, 0.08D,
                    3, 3, Realm.FOUNDATION_ESTABLISHMENT, false, false, List.of(
                    new IngredientRequirement(ModItems.PHOENIX_FEATHER_FLOWER.get(), 1),
                    new IngredientRequirement(ModItems.COLD_JADE.get(), 1),
                    new IngredientRequirement(ModItems.FIRE_ELEMENT_SPIRIT_STONE.get(), 1))),
            new AlchemyRecipe("marrow_cleansing_pill", Component.translatable("item.seeking_immortals.marrow_cleansing_pill"),
                    quality(ModItems.MARROW_CLEANSING_PILL.get(), ModItems.MARROW_CLEANSING_PILL_MID.get(),
                            ModItems.MARROW_CLEANSING_PILL_HIGH.get(), ModItems.MARROW_CLEANSING_PILL_SUPREME.get()),
                    1, 36, 34 * 20, 0.58D, 0.06D,
                    2, 2, Realm.QI_REFINING, false, false, List.of(
                    new IngredientRequirement(ModItems.IMMORTAL_GINSENG.get(), 1),
                    new IngredientRequirement(ModItems.SPIRIT_BEAST_BONE.get(), 1),
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 2))),
            new AlchemyRecipe("body_tempering_pill", Component.translatable("item.seeking_immortals.body_tempering_pill"),
                    quality(ModItems.BODY_TEMPERING_PILL.get(), ModItems.BODY_TEMPERING_PILL_MID.get(),
                            ModItems.BODY_TEMPERING_PILL_HIGH.get(), ModItems.BODY_TEMPERING_PILL_SUPREME.get()),
                    1, 24, 23 * 20, 0.62D, 0.04D,
                    1, 2, Realm.QI_REFINING, false, false, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_BEAST_BONE.get(), 1),
                    new IngredientRequirement(ModItems.BEAST_CORE.get(), 1))),
            new AlchemyRecipe("fasting_pill_low", Component.translatable("item.seeking_immortals.fasting_pill_low"),
                    quality(ModItems.FASTING_PILL_LOW.get(), ModItems.FASTING_PILL_MID.get(),
                            ModItems.FASTING_PILL_HIGH.get(), ModItems.FASTING_PILL_SUPREME.get()),
                    1, 12, 16 * 20, 0.88D, 0.01D,
                    1, 1, Realm.MORTAL, false, false, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_GRASS.get(), 1),
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 1))),
            new AlchemyRecipe("essence_condensing_pill", Component.translatable("item.seeking_immortals.essence_condensing_pill"),
                    quality(ModItems.ESSENCE_CONDENSING_PILL.get(), ModItems.ESSENCE_CONDENSING_PILL_MID.get(),
                            ModItems.ESSENCE_CONDENSING_PILL_HIGH.get(), ModItems.ESSENCE_CONDENSING_PILL_SUPREME.get()),
                    1, 60, 50 * 20, 0.52D, 0.08D,
                    3, 3, Realm.FOUNDATION_ESTABLISHMENT, false, false, List.of(
                    new IngredientRequirement(ModItems.PHOENIX_FEATHER_FLOWER.get(), 2),
                    new IngredientRequirement(ModItems.DRAGON_BLOOD_GRASS.get(), 1),
                    new IngredientRequirement(ModItems.STAR_METEORITE.get(), 1))),
            new AlchemyRecipe("soul_gathering_pill", Component.translatable("item.seeking_immortals.soul_gathering_pill"),
                    quality(ModItems.SOUL_GATHERING_PILL.get(), ModItems.SOUL_GATHERING_PILL_MID.get(),
                            ModItems.SOUL_GATHERING_PILL_HIGH.get(), ModItems.SOUL_GATHERING_PILL_SUPREME.get()),
                    1, 28, 24 * 20, 0.74D, 0.03D,
                    2, 2, Realm.QI_REFINING, false, false, List.of(
                    new IngredientRequirement(ModItems.SOUL_FRAGMENT.get(), 1),
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 2))),
            new AlchemyRecipe("marrow_repair_pill", Component.translatable("item.seeking_immortals.marrow_repair_pill"),
                    quality(ModItems.MARROW_REPAIR_PILL.get(), ModItems.MARROW_REPAIR_PILL_MID.get(),
                            ModItems.MARROW_REPAIR_PILL_HIGH.get(), ModItems.MARROW_REPAIR_PILL_SUPREME.get()),
                    1, 38, 34 * 20, 0.64D, 0.06D,
                    2, 2, Realm.QI_REFINING, false, false, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_BEAST_BONE.get(), 2),
                    new IngredientRequirement(ModItems.TRUE_DRAGON_BLOOD.get(), 1))),
            new AlchemyRecipe("clear_void_pill", Component.translatable("item.seeking_immortals.clear_void_pill"),
                    quality(ModItems.CLEAR_VOID_PILL.get(), ModItems.CLEAR_VOID_PILL_MID.get(),
                            ModItems.CLEAR_VOID_PILL_HIGH.get(), ModItems.CLEAR_VOID_PILL_SUPREME.get()),
                    1, 20, 18 * 20, 0.78D, 0.02D,
                    1, 1, Realm.MORTAL, false, false, List.of(
                    new IngredientRequirement(ModItems.CLOUD_MUSHROOM.get(), 1),
                    new IngredientRequirement(ModItems.VOID_CRYSTAL.get(), 1))),
            new AlchemyRecipe("forget_dust_pill", Component.translatable("item.seeking_immortals.forget_dust_pill"),
                    quality(ModItems.FORGET_DUST_PILL.get(), ModItems.FORGET_DUST_PILL_MID.get(),
                            ModItems.FORGET_DUST_PILL_HIGH.get(), ModItems.FORGET_DUST_PILL_SUPREME.get()),
                    1, 24, 22 * 20, 0.70D, 0.04D,
                    1, 1, Realm.MORTAL, false, false, List.of(
                    new IngredientRequirement(ModItems.SOUL_FRAGMENT.get(), 1),
                    new IngredientRequirement(ModItems.TIME_SAND.get(), 1))),
            new AlchemyRecipe("appearance_fixing_pill", Component.translatable("item.seeking_immortals.appearance_fixing_pill"),
                    quality(ModItems.APPEARANCE_FIXING_PILL.get(), ModItems.APPEARANCE_FIXING_PILL_MID.get(),
                            ModItems.APPEARANCE_FIXING_PILL_HIGH.get(), ModItems.APPEARANCE_FIXING_PILL_SUPREME.get()),
                    1, 42, 38 * 20, 0.58D, 0.05D,
                    3, 3, Realm.FOUNDATION_ESTABLISHMENT, false, false, List.of(
                    new IngredientRequirement(ModItems.PHOENIX_FEATHER.get(), 1),
                    new IngredientRequirement(ModItems.IMMORTAL_GINSENG.get(), 1))),
            new AlchemyRecipe("longevity_pill", Component.translatable("item.seeking_immortals.longevity_pill"),
                    quality(ModItems.LONGEVITY_PILL.get(), ModItems.LONGEVITY_PILL_MID.get(),
                            ModItems.LONGEVITY_PILL_HIGH.get(), ModItems.LONGEVITY_PILL_SUPREME.get()),
                    1, 120, 80 * 20, 0.28D, 0.16D,
                    5, 5, Realm.NASCENT_SOUL, true, false, List.of(
                    new IngredientRequirement(ModItems.IMMORTAL_GINSENG.get(), 2),
                    new IngredientRequirement(ModItems.TIME_SAND.get(), 2),
                    new IngredientRequirement(ModItems.PRIMORDIAL_ESSENCE.get(), 1))),
            new AlchemyRecipe("blood_qi_pill", Component.translatable("item.seeking_immortals.blood_qi_pill"),
                    quality(ModItems.BLOOD_QI_PILL.get(), ModItems.BLOOD_QI_PILL_MID.get(),
                            ModItems.BLOOD_QI_PILL_HIGH.get(), ModItems.BLOOD_QI_PILL_SUPREME.get()),
                    1, 80, 54 * 20, 0.48D, 0.10D,
                    4, 4, Realm.CORE_FORMATION, false, true, List.of(
                    new IngredientRequirement(ModItems.TRUE_DRAGON_BLOOD.get(), 1),
                    new IngredientRequirement(ModItems.DRAGON_SCALE.get(), 1),
                    new IngredientRequirement(ModItems.DRAGON_BLOOD_GRASS.get(), 2))),
            new AlchemyRecipe("return_yang_true_water", Component.translatable("item.seeking_immortals.return_yang_true_water"),
                    quality(ModItems.RETURN_YANG_TRUE_WATER.get(), ModItems.RETURN_YANG_TRUE_WATER_MID.get(),
                            ModItems.RETURN_YANG_TRUE_WATER_HIGH.get(), ModItems.RETURN_YANG_TRUE_WATER_SUPREME.get()),
                    1, 140, 90 * 20, 0.24D, 0.18D,
                    5, 5, Realm.NASCENT_SOUL, true, false, List.of(
                    new IngredientRequirement(ModItems.PRIMORDIAL_ESSENCE.get(), 1),
                    new IngredientRequirement(ModItems.TIME_SAND.get(), 2),
                    new IngredientRequirement(ModItems.CELESTIAL_CRYSTAL.get(), 1))),
            new AlchemyRecipe("marrow_extracting_pill", Component.translatable("item.seeking_immortals.marrow_extracting_pill"),
                    quality(ModItems.MARROW_EXTRACTING_PILL.get(), ModItems.MARROW_EXTRACTING_PILL_MID.get(),
                            ModItems.MARROW_EXTRACTING_PILL_HIGH.get(), ModItems.MARROW_EXTRACTING_PILL_SUPREME.get()),
                    1, 58, 44 * 20, 0.46D, 0.12D,
                    3, 3, Realm.FOUNDATION_ESTABLISHMENT, false, false, List.of(
                    new IngredientRequirement(ModItems.SPIRIT_BEAST_BONE.get(), 2),
                    new IngredientRequirement(ModItems.SOUL_FRAGMENT.get(), 1),
                    new IngredientRequirement(ModItems.TRUE_DRAGON_BLOOD.get(), 1))),
            new AlchemyRecipe("soul_breaking_pill", Component.translatable("item.seeking_immortals.soul_breaking_pill"),
                    quality(ModItems.SOUL_BREAKING_PILL.get(), ModItems.SOUL_BREAKING_PILL_MID.get(),
                            ModItems.SOUL_BREAKING_PILL_HIGH.get(), ModItems.SOUL_BREAKING_PILL_SUPREME.get()),
                    1, 46, 34 * 20, 0.52D, 0.10D,
                    3, 3, Realm.FOUNDATION_ESTABLISHMENT, false, false, List.of(
                    new IngredientRequirement(ModItems.SOUL_FRAGMENT.get(), 2),
                    new IngredientRequirement(ModItems.VOID_CRYSTAL.get(), 1),
                    new IngredientRequirement(ModItems.DRAGON_BLOOD_GRASS.get(), 1))),
            new AlchemyRecipe("poison_dragon_pearl", Component.translatable("item.seeking_immortals.poison_dragon_pearl"),
                    quality(ModItems.POISON_DRAGON_PEARL.get(), ModItems.POISON_DRAGON_PEARL_MID.get(),
                            ModItems.POISON_DRAGON_PEARL_HIGH.get(), ModItems.POISON_DRAGON_PEARL_SUPREME.get()),
                    1, 90, 62 * 20, 0.38D, 0.16D,
                    5, 5, Realm.NASCENT_SOUL, false, false, List.of(
                    new IngredientRequirement(ModItems.DRAGON_SCALE.get(), 2),
                    new IngredientRequirement(ModItems.TRUE_DRAGON_BLOOD.get(), 1),
                    new IngredientRequirement(ModItems.CHAOS_GOLD.get(), 1)))
    );

    private static List<Item> uniform(Item item) {
        return List.of(item, item, item, item);
    }

    private static List<Item> quality(Item low, Item medium, Item high, Item supreme) {
        return List.of(low, medium, high, supreme);
    }

    public static List<AlchemyRecipe> builtinRecipes() {
        return MVP_RECIPES;
    }

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
        return AlchemyRecipeManager.recipes().stream()
                .filter(recipe -> recipe.ingredients().stream().anyMatch(ingredient -> stack.is(ingredient.item())))
                .findFirst();
    }

    public boolean acceptsHeldIngredient(ItemStack stack) {
        return !stack.isEmpty() && ingredients.stream().anyMatch(ingredient -> stack.is(ingredient.item()));
    }

    public static Optional<AlchemyRecipe> findById(String id) {
        return AlchemyRecipeManager.findById(id);
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
