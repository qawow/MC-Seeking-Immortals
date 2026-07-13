package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.catalog.ManualCatalogService;
import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
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

/**
 * Physical carrier for text-material manuals_catalog ids.
 * Right-click studies the catalog entry via ManualCatalogService.
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
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return ok ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.seeking_immortals.catalog_manual.tooltip", manualId));
        TextMaterialCatalogService.builtin().findManual(manualId).ifPresent(manual -> {
            if (manual.unlocksForgeGrade() > 0) {
                tooltip.add(Component.translatable("message.seeking_immortals.manual.forge_grade",
                        manual.unlocksForgeGrade()));
            }
            if (!manual.realmMin().isBlank()) {
                tooltip.add(Component.translatable("message.seeking_immortals.manual.realm_req", manual.realmMin()));
            }
        });
    }
}
