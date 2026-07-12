package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.artifact.ArtifactActivationService;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.artifact.ArtifactStorageService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArtifactCatalogItem extends Item {
    private final String artifactId;
    private final boolean foil;

    public ArtifactCatalogItem(Properties properties, String artifactId) {
        this(properties, artifactId, true);
    }

    public ArtifactCatalogItem(Properties properties, String artifactId, boolean foil) {
        super(properties);
        this.artifactId = artifactId;
        this.foil = foil;
    }

    public String artifactId() {
        return artifactId;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil || super.isFoil(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player,
                                                  InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ArtifactStorageService.supports(artifactId)) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                boolean handled = CultivationHelper.get(serverPlayer)
                        .map(cultivation -> ArtifactDataService.builtin()
                                .findArtifact(artifactId)
                                .map(artifact -> ArtifactStorageService.use(serverPlayer, stack, hand,
                                        artifact, cultivation))
                                .orElse(false))
                        .orElse(false);
                return handled ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            boolean activated = CultivationHelper.get(serverPlayer)
                    .map(cultivation -> ArtifactActivationService.activate(serverPlayer, stack, hand,
                            artifactId, cultivation))
                    .orElse(false);
            return activated ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }
        return ArtifactActivationService.hasActivation(artifactId)
                ? InteractionResultHolder.consume(stack)
                : InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ArtifactDataService.Snapshot data = ArtifactDataService.builtin();
        data.findArtifact(artifactId).ifPresentOrElse(artifact -> {
            tooltip.add(Component.literal("Artifact: " + artifact.display()
                    + " / " + data.tierDisplay(artifact.tier())
                    + " / game tier " + artifact.gameTier()).withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.literal("Realm: " + artifact.realmMin()
                    + " / type: " + artifact.type()).withStyle(ChatFormatting.DARK_GRAY));
            if (!artifact.tags().isEmpty()) {
                tooltip.add(Component.literal("Tags: " + String.join(", ", artifact.tags()))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            data.findRecipeByArtifact(artifact.id()).ifPresent(recipe ->
                    tooltip.add(Component.literal("Refine: grade " + recipe.forgeGrade()
                            + " / success " + Math.round(recipe.baseSuccessRate() * 100.0D) + "%")
                            .withStyle(ChatFormatting.DARK_GRAY)));
            if (ArtifactStorageService.supports(artifact.id())) {
                ArtifactStorageService.appendStorageTooltip(stack, artifact, tooltip);
            } else {
                ArtifactActivationService.appendActivationTooltip(stack, artifact, tooltip);
            }
        }, () -> tooltip.add(Component.literal("Artifact data missing: " + artifactId)
                .withStyle(ChatFormatting.RED)));
    }
}
