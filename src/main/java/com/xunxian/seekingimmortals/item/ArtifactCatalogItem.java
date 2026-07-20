package com.xunxian.seekingimmortals.item;

import com.xunxian.seekingimmortals.artifact.ArtifactActivationService;
import com.xunxian.seekingimmortals.artifact.ArtifactDataService;
import com.xunxian.seekingimmortals.artifact.ArtifactStorageService;
import com.xunxian.seekingimmortals.artifact.NatalBindingService;
import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
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
        // M15: sneak-use 认主（UUID 绑定 + 本命）。
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                boolean claimed = com.xunxian.seekingimmortals.artifact.ArtifactOwnershipService
                        .claim(serverPlayer, stack, artifactId);
                // 器灵觉醒：已认主且再次潜行+主手空冷却时尝试（同路径，claim 幂等后走 awaken）。
                if (!claimed && com.xunxian.seekingimmortals.artifact.ArtifactOwnershipService
                        .ownerUuid(stack).map(id -> id.equals(serverPlayer.getUUID())).orElse(false)) {
                    claimed = com.xunxian.seekingimmortals.artifact.ArtifactOwnershipService
                            .tryAwakenSpirit(serverPlayer, stack, artifactId);
                }
                return claimed ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // 境界门禁检查
            ItemUsageGateService.GateResult gateCheck = CultivationHelper.get(serverPlayer)
                    .map(cultivation -> {
                        return ArtifactDataService.builtin().findArtifact(artifactId)
                                .map(artifact -> {
                                    Realm minRealm = parseRealmMin(artifact.realmMin());
                                    if (minRealm == null) {
                                        return ItemUsageGateService.GateResult.ok();
                                    }
                                    return ItemUsageGateService.checkRealm(cultivation, minRealm);
                                })
                                .orElse(ItemUsageGateService.GateResult.ok());
                    })
                    .orElse(ItemUsageGateService.GateResult.deny("message.seeking_immortals.item_gate.no_cultivation"));

            if (!gateCheck.allowed()) {
                serverPlayer.displayClientMessage(gateCheck.message(), true);
                return InteractionResultHolder.fail(stack);
            }

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

                // 使用 ItemUsageGateService 显示境界要求
                Realm minRealm = parseRealmMin(artifact.realmMin());
                if (minRealm != null) {
                    ItemUsageGateService.ItemRequirement requirement = ItemUsageGateService.ItemRequirement.realm(minRealm);
                    ItemUsageGateService.appendRequirementTooltip(stack, tooltip, requirement);
                }

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
            com.xunxian.seekingimmortals.artifact.ArtifactOwnershipService.appendOwnershipTooltip(stack, tooltip);
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
            com.xunxian.seekingimmortals.artifact.ArtifactActiveSkillService.resolve(artifactId).ifPresent(skill ->
                    tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.active_skill",
                                    skill.techniqueId())
                            .withStyle(ChatFormatting.BLUE)));
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

    /**
     * 解析境界最低要求字符串。
     */
    private Realm parseRealmMin(String realmMin) {
        if (realmMin == null || realmMin.isBlank()) {
            return null;
        }
        String normalized = realmMin.trim().toLowerCase();
        return switch (normalized) {
            case "mortal", "凡人" -> Realm.MORTAL;
            case "qi_refining", "炼气", "炼气期" -> Realm.QI_REFINING;
            case "foundation_establishment", "foundation", "筑基", "筑基期" -> Realm.FOUNDATION_ESTABLISHMENT;
            case "core_formation", "结丹", "结丹期" -> Realm.CORE_FORMATION;
            case "nascent_soul", "元婴", "元婴期" -> Realm.NASCENT_SOUL;
            case "soul_transformation", "deity_transformation", "化神", "化神期" -> Realm.SOUL_TRANSFORMATION;
            case "void_refinement", "炼虚", "炼虚期" -> Realm.VOID_REFINEMENT;
            case "body_integration", "unity", "合体", "合体期" -> Realm.UNITY;
            case "great_vehicle", "mahayana", "大乘", "大乘期" -> Realm.MAHAYANA;
            case "tribulation_land", "tribulation", "渡劫", "渡劫期" -> Realm.TRIBULATION;
            case "true_immortal", "真仙" -> Realm.TRUE_IMMORTAL;
            default -> null;
        };
    }
}
