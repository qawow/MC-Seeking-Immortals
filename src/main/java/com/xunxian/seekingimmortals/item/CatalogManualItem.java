package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts;
import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.structure.FormationItemService;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Physical carrier for text-material manuals_catalog ids.
 * Right-click studies the catalog entry via ManualCatalogService.
 * When a formation_item_behaviors row exists for the same id (e.g. array_blueprint_scroll),
 * already-studied stacks fall through to FormationItemService activation.
 */
public class CatalogManualItem extends Item {
    private final String manualId;

    public CatalogManualItem(Properties properties, String manualId) {
        super(properties);
        this.manualId = manualId == null ? "" : manualId;
    }

    public String getManualId() {
        return manualId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }
        // Prefer first-time study when the manuals catalog still has an unread entry.
        boolean alreadyStudied = ManualCatalogService.hasStudied(serverPlayer, manualId);
        if (!alreadyStudied) {
            boolean ok = ManualCatalogService.study(serverPlayer, manualId);
            if (ok) {
                // Wave476: also map manual id/type text into learned methods when possible.
                ManualCatalogService.grantMethodsFromTechniqueSource(serverPlayer, manualId);
                TextMaterialCatalogService.builtin().findManual(manualId).ifPresent(manual -> {
                    if (manual.type() != null && !manual.type().isBlank()) {
                        ManualCatalogService.grantMethodsFromTechniqueSource(serverPlayer, manual.type());
                    }
                    if (manual.display() != null && !manual.display().isBlank()) {
                        ManualCatalogService.grantMethodsFromTechniqueSource(serverPlayer, manual.display());
                    }
                });
                // Dual-path manuals (blueprint scrolls) keep the stack after study so remaining
                // formation uses can be spent later; pure manuals still consume on study.
                if (!player.getAbilities().instabuild && FormationItemService.builtin().find(manualId).isEmpty()) {
                    stack.shrink(1);
                }
                return InteractionResultHolder.consume(stack);
            }
            // Study failed for a non-formation manual; surface the fail.
            if (FormationItemService.builtin().find(manualId).isEmpty()) {
                return InteractionResultHolder.fail(stack);
            }
        }
        Optional<InteractionResultHolder<ItemStack>> formationUse =
                FormationItemService.tryUse(serverPlayer, stack);
        if (formationUse.isPresent()) {
            return formationUse.get();
        }
        // Already studied pure manual with no formation behavior.
        if (alreadyStudied) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.manual.already_studied",
                    resolveManualTitle(stack,
                            TextMaterialCatalogService.builtin().findManual(manualId).orElse(null))), false);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        TextMaterialCatalogService.ManualEntry manual =
                TextMaterialCatalogService.builtin().findManual(manualId).orElse(null);
        Component title = resolveManualTitle(stack, manual);
        tooltip.add(Component.translatable("item.seeking_immortals.catalog_manual.tooltip", title)
                .withStyle(ChatFormatting.GOLD));
        if (manual != null) {
            if (manual.type() != null && !manual.type().isBlank()) {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_manual.type",
                                ManualCatalogService.typeDisplay(manual.type()))
                        .withStyle(ChatFormatting.GRAY));
            }
            if (manual.unlocksForgeGrade() > 0) {
                tooltip.add(Component.translatable("message.seeking_immortals.manual.forge_grade",
                        manual.unlocksForgeGrade()).withStyle(ChatFormatting.DARK_AQUA));
            }
            if (!manual.realmMin().isBlank()) {
                tooltip.add(Component.translatable("message.seeking_immortals.manual.realm_req",
                        ArtifactDisplayTexts.realm(manual.realmMin())).withStyle(ChatFormatting.GRAY));
            }
            if (manual.recipeId() != null && !manual.recipeId().isBlank()) {
                tooltip.add(Component.translatable("message.seeking_immortals.manual.recipe",
                                ManualCatalogService.recipeDisplay(manual.recipeId()))
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
            if (manual.note() != null && !manual.note().isBlank()
                    && !manual.note().equals(manual.display())
                    && PlayerDisplayText.isSafe(manual.note())) {
                tooltip.add(Component.literal(manual.note()).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else if (!manualId.isBlank()) {
            tooltip.add(Component.translatable("message.seeking_immortals.manual.unindexed")
                    .withStyle(ChatFormatting.DARK_GRAY));
            if ("artifact_identify_scroll".equals(manualId)) {
                tooltip.add(Component.translatable(
                        "tooltip.seeking_immortals.catalog_manual.artifact_identify")
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        if (FormationItemService.builtin().find(manualId).isPresent()) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_manual.formation_after_study")
                    .withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("tooltip.seeking_immortals.catalog_item.detail.array_blueprint_scroll")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private Component resolveManualTitle(ItemStack stack, @Nullable TextMaterialCatalogService.ManualEntry manual) {
        if (manual != null && manual.display() != null && !manual.display().isBlank()
                && PlayerDisplayText.isSafe(manual.display())) {
            return Component.literal(manual.display());
        }
        // Prefer item lang name (already Chinese for registered manuals).
        Component hover = stack.getHoverName();
        if (hover != null && !PlayerDisplayText.looksLikeCode(hover.getString())) {
            return hover;
        }
        return PlayerDisplayText.itemName(manualId);
    }
}
