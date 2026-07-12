package com.xunxian.seekingimmortals.compat.jei;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipe;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipeManager;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@JeiPlugin
public final class SeekingImmortalsJeiPlugin implements IModPlugin {
    public static final RecipeType<AlchemyRecipe> ALCHEMY =
            RecipeType.create(SeekingImmortalsMod.MODID, "alchemy", AlchemyRecipe.class);
    public static final RecipeType<ArtifactDataService.RefinementRecipe> REFINEMENT =
            RecipeType.create(SeekingImmortalsMod.MODID, "refinement", ArtifactDataService.RefinementRecipe.class);
    private static final ResourceLocation UID = new ResourceLocation(SeekingImmortalsMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlchemyCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RefinementCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ALCHEMY, List.copyOf(AlchemyRecipeManager.recipes()));
        registration.addRecipes(REFINEMENT, List.copyOf(ArtifactDataService.builtin().refinementRecipes().values()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.REFINEMENT_FORGE.get()), REFINEMENT);
        registration.addRecipeCatalysts(ALCHEMY,
                ModBlocks.ALCHEMY_FURNACE.get(),
                ModBlocks.ALCHEMY_FURNACE_TIER_2.get(),
                ModBlocks.ALCHEMY_FURNACE_TIER_3.get(),
                ModBlocks.ALCHEMY_FURNACE_TIER_4.get(),
                ModBlocks.ALCHEMY_FURNACE_TIER_5.get());
    }

    private static final class AlchemyCategory implements IRecipeCategory<AlchemyRecipe> {
        private static final int WIDTH = 164;
        private static final int HEIGHT = 86;
        private final IDrawable background;
        private final IDrawable icon;

        private AlchemyCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
            this.icon = guiHelper.createDrawableItemLike(ModBlocks.ALCHEMY_FURNACE.get());
        }

        @Override
        public RecipeType<AlchemyRecipe> getRecipeType() {
            return ALCHEMY;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.seeking_immortals.alchemy");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, AlchemyRecipe recipe, IFocusGroup focuses) {
            int x = 2;
            for (AlchemyRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
                builder.addSlot(RecipeIngredientRole.INPUT, x, 20)
                        .addItemStack(new ItemStack(ingredient.item(), ingredient.count()));
                x += 18;
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 20)
                    .addItemStacks(recipe.outputsByQuality().stream()
                            .map(item -> new ItemStack(item, recipe.outputCount()))
                            .toList());
        }

        @Override
        public void draw(AlchemyRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                         GuiGraphics graphics, double mouseX, double mouseY) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            graphics.drawString(font, recipe.displayName(), 2, 2, 0xFFE6D59A, false);
            graphics.drawString(font, Component.translatable("jei.seeking_immortals.alchemy.tier",
                    recipe.requiredFurnaceTier(), recipe.idealFireTier()), 2, 44, 0xFFBFAF8A, false);
            graphics.drawString(font, Component.translatable("jei.seeking_immortals.alchemy.stats",
                    Math.round(recipe.successRate() * 100.0D),
                    Math.round(recipe.explosionChance() * 100.0D)), 2, 56, 0xFFBFAF8A, false);
        }

        @Override
        public List<Component> getTooltipStrings(AlchemyRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                                                 double mouseX, double mouseY) {
            return List.of(
                    Component.translatable("jei.seeking_immortals.alchemy.mana", recipe.manaCost()).withStyle(ChatFormatting.AQUA),
                    Component.translatable("jei.seeking_immortals.alchemy.control", recipe.minControlRealm().getDisplayName()).withStyle(ChatFormatting.GRAY),
                    Component.translatable(recipe.requiresEarthFireRoom()
                            ? "jei.seeking_immortals.alchemy.earth_room.yes"
                            : "jei.seeking_immortals.alchemy.earth_room.no").withStyle(ChatFormatting.GRAY));
        }
    }

    private static final class RefinementCategory implements IRecipeCategory<ArtifactDataService.RefinementRecipe> {
        private static final int WIDTH = 164;
        private static final int HEIGHT = 72;
        private final IDrawable background;
        private final IDrawable icon;

        private RefinementCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
            this.icon = guiHelper.createDrawableItemLike(ModBlocks.REFINEMENT_FORGE.get());
        }

        @Override
        public RecipeType<ArtifactDataService.RefinementRecipe> getRecipeType() {
            return REFINEMENT;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.seeking_immortals.refinement");
        }

        @Override
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ArtifactDataService.RefinementRecipe recipe, IFocusGroup focuses) {
            // Display-only placeholders; materials are source ids mapped at runtime.
            builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 20)
                    .addItemStack(new ItemStack(ModItems.ARTIFACT_REPAIR_KIT.get()));
        }

        @Override
        public void draw(ArtifactDataService.RefinementRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                         GuiGraphics graphics, double mouseX, double mouseY) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            graphics.drawString(font, recipe.display(), 2, 2, 0xFFE6D59A, false);
            graphics.drawString(font, recipe.id(), 2, 16, 0xFFBFAF8A, false);
            graphics.drawString(font, recipe.realmMin() + " | " + Math.round(recipe.baseSuccessRate() * 100) + "%", 2, 40, 0xFFBFAF8A, false);
        }
    }
}
