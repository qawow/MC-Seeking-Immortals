package com.xunxian.seekingimmortals.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xunxian.seekingimmortals.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * Data-driven refinement recipe (Wave54 real RecipeSerializer).
 * JSON under data/<ns>/recipes/*.json with type seeking_immortals:refinement
 */
public class RefinementCraftingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final String group;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final float successRate;
    private final String realmMin;
    private final int forgeGrade;

    public RefinementCraftingRecipe(ResourceLocation id, String group, NonNullList<Ingredient> ingredients,
                                    ItemStack result, float successRate, String realmMin, int forgeGrade) {
        this.id = id;
        this.group = group == null ? "" : group;
        this.ingredients = ingredients;
        this.result = result;
        this.successRate = Math.max(0.0F, Math.min(1.0F, successRate));
        this.realmMin = realmMin == null ? "" : realmMin;
        this.forgeGrade = Math.max(1, forgeGrade);
    }

    public float successRate() {
        return successRate;
    }

    public String realmMin() {
        return realmMin;
    }

    public int forgeGrade() {
        return forgeGrade;
    }

    @Override
    public boolean matches(Container container, Level level) {
        // Order-insensitive multiset match against container slots 0..size-2 (last reserved output).
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            remaining.set(i, container.getItem(i).copy());
        }
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < remaining.size(); i++) {
                ItemStack stack = remaining.get(i);
                if (!stack.isEmpty() && ingredient.test(stack)) {
                    stack.shrink(1);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.REFINEMENT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.REFINEMENT_TYPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public String getGroup() {
        return group;
    }

    public static class Serializer implements RecipeSerializer<RefinementCraftingRecipe> {
        @Override
        public RefinementCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (int i = 0; i < arr.size(); i++) {
                ingredients.add(Ingredient.fromJson(arr.get(i)));
            }
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            float success = GsonHelper.getAsFloat(json, "success_rate", 0.7F);
            String realmMin = GsonHelper.getAsString(json, "realm_min", "");
            int forgeGrade = GsonHelper.getAsInt(json, "forge_grade", 1);
            return new RefinementCraftingRecipe(id, group, ingredients, result, success, realmMin, forgeGrade);
        }

        @Override
        public RefinementCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            int size = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < size; i++) {
                ingredients.set(i, Ingredient.fromNetwork(buffer));
            }
            ItemStack result = buffer.readItem();
            float success = buffer.readFloat();
            String realmMin = buffer.readUtf();
            int forgeGrade = buffer.readVarInt();
            return new RefinementCraftingRecipe(id, group, ingredients, result, success, realmMin, forgeGrade);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, RefinementCraftingRecipe recipe) {
            buffer.writeUtf(recipe.group);
            buffer.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                ingredient.toNetwork(buffer);
            }
            buffer.writeItem(recipe.result);
            buffer.writeFloat(recipe.successRate);
            buffer.writeUtf(recipe.realmMin);
            buffer.writeVarInt(recipe.forgeGrade);
        }
    }
}
