package com.xunxian.seekingimmortals.registry;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.recipe.RefinementCraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, SeekingImmortalsMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SeekingImmortalsMod.MODID);

    public static final RegistryObject<RecipeType<RefinementCraftingRecipe>> REFINEMENT_TYPE =
            RECIPE_TYPES.register("refinement", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return SeekingImmortalsMod.MODID + ":refinement";
                }
            });

    public static final RegistryObject<RecipeSerializer<RefinementCraftingRecipe>> REFINEMENT_SERIALIZER =
            RECIPE_SERIALIZERS.register("refinement", RefinementCraftingRecipe.Serializer::new);

    private ModRecipes() {}

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
