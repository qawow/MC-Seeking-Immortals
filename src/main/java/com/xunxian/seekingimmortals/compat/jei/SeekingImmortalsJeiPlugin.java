package com.xunxian.seekingimmortals.compat.jei;

import com.xunxian.seekingimmortals.SeekingImmortalsMod;
import com.xunxian.seekingimmortals.alchemy.AlchemyRecipe;
import com.xunxian.seekingimmortals.artifact.ArtifactRefinementService;
import com.xunxian.seekingimmortals.client.AlchemyFurnaceScreen;
import com.xunxian.seekingimmortals.craft.TalismanCraftService;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
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
    public static final RecipeType<JeiRecipeCatalog.RefinementDisplayRecipe> REFINEMENT =
            RecipeType.create(SeekingImmortalsMod.MODID, "refinement", JeiRecipeCatalog.RefinementDisplayRecipe.class);
    public static final RecipeType<JeiRecipeCatalog.TalismanDisplayRecipe> TALISMAN =
            RecipeType.create(SeekingImmortalsMod.MODID, "talisman", JeiRecipeCatalog.TalismanDisplayRecipe.class);
    private static final ResourceLocation UID = new ResourceLocation(SeekingImmortalsMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new AlchemyCategory(guiHelper));
        registration.addRecipeCategories(new RefinementCategory(guiHelper));
        registration.addRecipeCategories(new TalismanCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        JeiRecipeCatalog.Snapshot catalog = JeiRecipeCatalog.snapshot();
        registration.addRecipes(ALCHEMY, catalog.alchemyRecipes());
        registration.addRecipes(REFINEMENT, catalog.refinementRecipes());
        registration.addRecipes(TALISMAN, catalog.talismanRecipes());
        SeekingImmortalsMod.LOGGER.info(
                "JEI recipe catalog: {} alchemy, {} refinement, {} talisman recipes.",
                catalog.alchemyRecipes().size(),
                catalog.refinementRecipes().size(),
                catalog.talismanRecipes().size());
        if (!catalog.omittedRefinementIds().isEmpty() || !catalog.omittedTalismanIds().isEmpty()) {
            SeekingImmortalsMod.LOGGER.warn(
                    "JEI omitted unresolved recipes. refinement={}, talisman={}",
                    catalog.omittedRefinementIds(), catalog.omittedTalismanIds());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(REFINEMENT,
                ModBlocks.REFINEMENT_FORGE.get(),
                ModBlocks.REFINEMENT_FORGE_G2.get(),
                ModBlocks.REFINEMENT_FORGE_G3.get());
        registration.addRecipeCatalyst(ModBlocks.TALISMAN_TABLE.get(), TALISMAN);
        registration.addRecipeCatalysts(ALCHEMY,
                ModBlocks.ALCHEMY_FURNACE.get(),
                ModBlocks.ALCHEMY_FURNACE_TIER_2.get(),
                ModBlocks.ALCHEMY_FURNACE_TIER_3.get(),
                ModBlocks.ALCHEMY_FURNACE_TIER_4.get(),
                ModBlocks.ALCHEMY_FURNACE_TIER_5.get());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(AlchemyFurnaceScreen.class, 86, 33, 31, 10, ALCHEMY);
    }

    private static void addInputs(IRecipeLayoutBuilder builder, List<ItemStack> inputs) {
        for (int index = 0; index < inputs.size(); index++) {
            int x = 2 + (index % 6) * 18;
            int y = 20 + (index / 6) * 18;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addItemStack(inputs.get(index));
        }
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
            builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 20)
                    .addItemStacks(recipe.outputsByQuality().stream()
                            .map(item -> new ItemStack(item, recipe.outputCount()))
                            .toList());
        }

        @Override
        public void draw(AlchemyRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                         GuiGraphics graphics, double mouseX, double mouseY) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            graphics.drawString(font, recipe.displayName(), 2, 2,
                    com.xunxian.seekingimmortals.client.ImmortalUiSkin.JOURNAL_PAPER, false);
            graphics.drawString(font, Component.translatable("jei.seeking_immortals.alchemy.tier",
                    recipe.requiredFurnaceTier(), recipe.idealFireTier()), 2, 44,
                    com.xunxian.seekingimmortals.client.ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            graphics.drawString(font, Component.translatable("jei.seeking_immortals.alchemy.stats",
                    Math.round(recipe.successRate() * 100.0D),
                    Math.round(recipe.explosionChance() * 100.0D)), 2, 56,
                    com.xunxian.seekingimmortals.client.ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }

        @Override
        public List<Component> getTooltipStrings(AlchemyRecipe recipe,
                                                 mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
                                                 double mouseX, double mouseY) {
            return List.of(
                    Component.translatable("jei.seeking_immortals.alchemy.mana", recipe.manaCost()).withStyle(ChatFormatting.AQUA),
                    Component.translatable("jei.seeking_immortals.alchemy.control", recipe.minControlRealm().getDisplayName()).withStyle(ChatFormatting.GRAY),
                    Component.translatable(recipe.requiresEarthFireRoom()
                            ? "jei.seeking_immortals.alchemy.earth_room.yes"
                            : "jei.seeking_immortals.alchemy.earth_room.no").withStyle(ChatFormatting.GRAY));
        }
    }

    private static final class RefinementCategory
            implements IRecipeCategory<JeiRecipeCatalog.RefinementDisplayRecipe> {
        private static final int WIDTH = 164;
        private static final int HEIGHT = 76;
        private final IDrawable background;
        private final IDrawable icon;

        private RefinementCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
            this.icon = guiHelper.createDrawableItemLike(ModBlocks.REFINEMENT_FORGE.get());
        }

        @Override
        public RecipeType<JeiRecipeCatalog.RefinementDisplayRecipe> getRecipeType() {
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
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipeCatalog.RefinementDisplayRecipe recipe,
                              IFocusGroup focuses) {
            addInputs(builder, recipe.inputs());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 20).addItemStack(recipe.output());
        }

        @Override
        public void draw(JeiRecipeCatalog.RefinementDisplayRecipe recipe,
                         mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                         GuiGraphics graphics, double mouseX, double mouseY) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            graphics.drawString(font, recipe.output().getHoverName(), 2, 2,
                    com.xunxian.seekingimmortals.client.ImmortalUiSkin.JOURNAL_PAPER, false);
            graphics.drawString(font, Component.translatable("jei.seeking_immortals.refinement.stats",
                    recipe.source().forgeGrade(), Math.round(recipe.source().baseSuccessRate() * 100.0D)), 2, 46,
                    com.xunxian.seekingimmortals.client.ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
            graphics.drawString(font, Component.translatable("jei.seeking_immortals.refinement.realm",
                    ArtifactRefinementService.realmFromDesignId(recipe.source().realmMin()).getDisplayName()), 2, 58,
                    com.xunxian.seekingimmortals.client.ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }
    }

    private static final class TalismanCategory
            implements IRecipeCategory<JeiRecipeCatalog.TalismanDisplayRecipe> {
        private static final int WIDTH = 164;
        private static final int HEIGHT = 66;
        private final IDrawable background;
        private final IDrawable icon;

        private TalismanCategory(IGuiHelper guiHelper) {
            this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
            this.icon = guiHelper.createDrawableItemLike(ModBlocks.TALISMAN_TABLE.get());
        }

        @Override
        public RecipeType<JeiRecipeCatalog.TalismanDisplayRecipe> getRecipeType() {
            return TALISMAN;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei.seeking_immortals.talisman");
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
        public void setRecipe(IRecipeLayoutBuilder builder, JeiRecipeCatalog.TalismanDisplayRecipe recipe,
                              IFocusGroup focuses) {
            addInputs(builder, recipe.inputs());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 142, 20).addItemStack(recipe.output());
        }

        @Override
        public void draw(JeiRecipeCatalog.TalismanDisplayRecipe recipe,
                         mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
                         GuiGraphics graphics, double mouseX, double mouseY) {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            graphics.drawString(font, recipe.output().getHoverName(), 2, 2,
                    com.xunxian.seekingimmortals.client.ImmortalUiSkin.JOURNAL_PAPER, false);
            graphics.drawString(font, Component.translatable("jei.seeking_immortals.talisman.stats",
                    Math.round(recipe.source().successRate() * 100.0D),
                    TalismanCraftService.requiredInkCount()), 2, 46,
                    com.xunxian.seekingimmortals.client.ImmortalUiSkin.JOURNAL_PAPER_MUTED, false);
        }
    }
}
