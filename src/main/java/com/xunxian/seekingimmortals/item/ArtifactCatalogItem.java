package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.artifact.ArtifactActivationService;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.artifact.ArtifactStorageService;
import com.xunxian.seekingimmortals.artifact.NatalBindingService;
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
        // Wave489: appraisal lens / identify scroll appraises the opposite hand.
        if (com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.isAppraisalTool(artifactId)) {
            InteractionHand other = hand == InteractionHand.MAIN_HAND
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack target = player.getItemInHand(other);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                boolean ok = com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService
                        .appraise(serverPlayer, stack, target);
                return ok ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.consume(stack);
        }
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
        // Wave456: sneak-use binds natal artifact (one per player).
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                boolean bound = NatalBindingService.bind(serverPlayer, stack);
                return bound ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
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
        boolean appraised = com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.isAppraised(stack);
        boolean creativeFull = flag != null && flag.isCreative();
        data.findArtifact(artifactId).ifPresentOrElse(artifact -> {
            // Wave490: hide detailed identity until appraised (creative tooltips still full).
            if (appraised || creativeFull || com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.isAppraisalTool(artifactId)) {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.header",
                                artifact.display(), data.tierDisplay(artifact.tier()), artifact.gameTier())
                        .withStyle(ChatFormatting.DARK_AQUA));
                tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.realm_type",
                                com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.realm(artifact.realmMin()),
                                com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.type(artifact.type()))
                        .withStyle(ChatFormatting.DARK_GRAY));
                if (!artifact.tags().isEmpty()) {
                    tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.tags",
                                    com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.tagsJoined(artifact.tags()))
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
                data.findRecipeByArtifact(artifact.id()).ifPresent(recipe ->
                        tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.refine",
                                        recipe.forgeGrade(), Math.round(recipe.baseSuccessRate() * 100.0D))
                                .withStyle(ChatFormatting.DARK_GRAY)));
            } else {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.unappraised")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            if (ArtifactStorageService.supports(artifact.id())) {
                ArtifactStorageService.appendStorageTooltip(stack, artifact, tooltip);
            } else if (appraised || creativeFull) {
                ArtifactActivationService.appendActivationTooltip(stack, artifact, tooltip);
            }
            if (stack.hasTag() && stack.getTag().getBoolean(NatalBindingService.STACK_BOUND)) {
                int growth = NatalBindingService.growthFromStack(stack);
                tooltip.add(Component.translatable("tooltip.seeking_immortals.natal.bound_mark")
                        .withStyle(ChatFormatting.GOLD));
                if (growth > 0) {
                    tooltip.add(Component.translatable("tooltip.seeking_immortals.natal.growth", growth)
                            .withStyle(ChatFormatting.YELLOW));
                }
            } else {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.natal.bind_hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }, () -> tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.missing", artifactId)
                .withStyle(ChatFormatting.RED)));
        if (com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.isAppraisalTool(artifactId)) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.appraisal_tool")
                    .withStyle(ChatFormatting.AQUA));
        }
        if (appraised && stack.getTag() != null) {
            var tag = stack.getTag();
            tooltip.add(Component.translatable("tooltip.seeking_immortals.appraised",
                    tag.getInt(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_TIER),
                    com.xunxian.seekingimmortals.artifact.ArtifactDisplayTexts.type(
                            tag.getString(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_TYPE)),
                    tag.getInt(com.xunxian.seekingimmortals.artifact.ArtifactAppraisalService.TAG_APPRAISED_VALUE))
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
