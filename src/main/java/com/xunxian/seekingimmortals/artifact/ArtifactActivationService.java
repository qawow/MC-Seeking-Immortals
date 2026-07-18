package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.entity.CultivationFireballEntity;
import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import com.xunxian.seekingimmortals.artifact.NatalBindingService;
import com.xunxian.seekingimmortals.network.SyncCultivationDataPacket;
import com.xunxian.seekingimmortals.structure.FormationFieldService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ArtifactActivationService {
    public static final String USES_LEFT_TAG = "SeekingImmortalsArtifactUsesLeft";
    public static final String INTEGRITY_TAG = "SeekingImmortalsArtifactIntegrity";

    private ArtifactActivationService() {}

    public static Optional<ActivationInfo> activationInfo(String artifactId) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        return snapshot.findArtifact(artifactId)
                .map(artifact -> activationInfo(snapshot, artifact))
                .filter(ActivationInfo::supported);
    }

    public static boolean hasActivation(String artifactId) {
        return activationInfo(artifactId).isPresent();
    }

    public static int getUsesLeft(ItemStack stack, int maxUses) {
        if (maxUses <= 0) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(USES_LEFT_TAG, Tag.TAG_INT)) {
            return maxUses;
        }
        return Math.max(0, Math.min(maxUses, tag.getInt(USES_LEFT_TAG)));
    }

    public static void appendActivationTooltip(ItemStack stack, ArtifactDataService.ArtifactDefinition artifact,
                                               List<Component> tooltip) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        ActivationInfo info = activationInfo(snapshot, artifact);
        if (info.supported()) {
            if (ActivationKind.REPAIR.id.equals(info.kind())) {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.repair",
                        info.spiritualPowerCost(), info.repairAmount()).withStyle(ChatFormatting.BLUE));
            } else {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.activation",
                        info.spiritualPowerCost(), Math.max(1, info.cooldownTicks() / 20))
                        .withStyle(ChatFormatting.BLUE));
            }
            if (info.maxUses() > 0) {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.uses",
                        getUsesLeft(stack, info.maxUses()), info.maxUses()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (info.integrityCost() > 0) {
                tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.integrity",
                        getIntegrity(stack, artifact), maxIntegrity(artifact), info.integrityCost())
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
            return;
        }
        if (isDeferredType(artifact.type())) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.deferred")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static boolean activate(ServerPlayer player, ItemStack stack, String artifactId,
                                   PlayerCultivation cultivation) {
        return activate(player, stack, InteractionHand.MAIN_HAND, artifactId, cultivation);
    }

    public static boolean activate(ServerPlayer player, ItemStack stack, InteractionHand hand, String artifactId,
                                   PlayerCultivation cultivation) {
        ArtifactDataService.Snapshot snapshot = ArtifactDataService.builtin();
        ArtifactDataService.ArtifactDefinition artifact = snapshot.findArtifact(artifactId).orElse(null);
        if (artifact == null) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.artifact.unsupported"), true);
            return false;
        }

        // M15: 认主红线 — 他人本命/已认主法宝不可用。
        if (!ArtifactOwnershipService.canActivate(player, stack, artifactId)) {
            return false;
        }

        double powerScale = ArtifactPowerService.effectiveScale(player, stack, artifact);
        if (powerScale <= 0.0D) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.suppressed"), true);
            return false;
        }

        ActivationInfo info = activationInfo(snapshot, artifact);
        if (!info.supported()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.artifact.unsupported"), true);
            return false;
        }
        if (cultivation.getRealm().ordinal() < info.minRealm().ordinal()
                && !com.xunxian.seekingimmortals.cultivation.ProgressionGateApi.meetsRealm(
                        cultivation, info.minRealm())) {
            // 越阶仍可释放，但 powerScale 已压制；仅当完全凡人且无门槛通过时拒绝低阶。
            if (powerScale < ArtifactDataService.builtin().realmPowerScale().belowRealmMin() + 1.0e-6D
                    && cultivation.getRealm().ordinal() + 2 < info.minRealm().ordinal()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.artifact.realm_too_low", info.minRealm().getDisplayName()), true);
                return false;
            }
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.artifact.cooldown"), true);
            return false;
        }
        boolean repairActivation = ActivationKind.REPAIR.id.equals(info.kind());
        if (!repairActivation && info.integrityCost() > 0 && !player.getAbilities().instabuild) {
            int integrity = getIntegrity(stack, artifact);
            int effectiveCost = effectiveIntegrityCost(player, artifact, info);
            if (!hasUsableIntegrity(integrity, effectiveCost, false)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.artifact.integrity_broken", stack.getHoverName()), true);
                return false;
            }
            if (integrity < effectiveCost) {
                // Wave459: last-light emergency activation zeros integrity.
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.artifact.integrity_last_light", stack.getHoverName()), true);
            }
        }
        if (info.maxUses() > 0 && getUsesLeft(stack, info.maxUses()) <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.talisman_depleted", stack.getHoverName()), true);
            return false;
        }
        // M15: 优先走 M02 SkillEffectRegistry 主动技映射（冷却/灵力服务端校验）。
        if (!repairActivation) {
            ArtifactActiveSkillService.CastResult castResult = ArtifactActiveSkillService.tryCast(
                    player, cultivation, stack, artifact, powerScale);
            if (castResult == ArtifactActiveSkillService.CastResult.SUCCESS) {
                if (info.integrityCost() > 0 && !player.getAbilities().instabuild) {
                    int effectiveCost = effectiveIntegrityCost(player, artifact, info);
                    int integrity = getIntegrity(stack, artifact);
                    if (integrity < effectiveCost) {
                        damageIntegrity(stack, artifact, integrity);
                    } else {
                        damageIntegrity(stack, artifact, effectiveCost);
                    }
                }
                consumeTalismanUse(player, stack, info);
                playActivationFeedback(player);
                SyncCultivationDataPacket.send(player, cultivation);
                if (ArtifactPowerService.isSuppressed(player, artifact)) {
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.artifact.suppressed_partial",
                            String.format(Locale.ROOT, "%.0f%%", powerScale * 100.0D)), true);
                }
                player.displayClientMessage(Component.translatable("message.seeking_immortals.artifact.activated",
                        stack.getHoverName(),
                        ArtifactPowerService.scaledSpiritualCost(info.spiritualPowerCost(), powerScale)), true);
                return true;
            }
            if (!ArtifactActiveSkillService.shouldFallbackToGeneric(castResult)) {
                return false;
            }
        }
        RepairTarget repairTarget = null;
        if (repairActivation) {
            repairTarget = findRepairTarget(player, hand, snapshot);
            if (repairTarget == null) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.artifact.repair_no_target"), true);
                return false;
            }
            if (repairTarget.currentIntegrity() >= repairTarget.maxIntegrity()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.artifact.repair_full", repairTarget.stack().getHoverName()), true);
                return false;
            }
        }
        int spCost = ArtifactPowerService.scaledSpiritualCost(
                effectiveSpiritualCost(player, artifact, info), powerScale);
        if (!cultivation.consumeSpiritualPower(spCost)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.not_enough_qi"), true);
            return false;
        }

        Item activatedItem = stack.getItem();
        if (repairTarget != null) {
            applyRepair(player, stack, repairTarget, info);
        } else {
            applyActivation(player, cultivation, artifact, info);
            // Wave456: natal-bound artifact gains growth on successful activation.
            if (artifact.id().equals(NatalBindingService.boundId(player))) {
                NatalBindingService.grow(player);
            }
            if (info.integrityCost() > 0 && !player.getAbilities().instabuild) {
                int effectiveCost = effectiveIntegrityCost(player, artifact, info);
                int integrity = getIntegrity(stack, artifact);
                if (integrity < effectiveCost) {
                    damageIntegrity(stack, artifact, integrity); // last light
                } else {
                    damageIntegrity(stack, artifact, effectiveCost);
                }
            }
        }
        int cooldown = ArtifactPowerService.scaledCooldown(
                effectiveCooldown(player, artifact, info), powerScale);
        player.getCooldowns().addCooldown(activatedItem, cooldown);
        consumeTalismanUse(player, stack, info);
        playActivationFeedback(player);
        SyncCultivationDataPacket.send(player, cultivation);
        if (repairTarget == null) {
            if (ArtifactPowerService.isSuppressed(player, artifact)) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.artifact.suppressed_partial",
                        String.format(Locale.ROOT, "%.0f%%", powerScale * 100.0D)), true);
            }
            player.displayClientMessage(Component.translatable("message.seeking_immortals.artifact.activated",
                    stack.getHoverName(), spCost), true);
        }
        return true;
    }

    private static int effectiveIntegrityCost(ServerPlayer player, ArtifactDataService.ArtifactDefinition artifact,
                                              ActivationInfo info) {
        int cost = info.integrityCost();
        if (cost <= 0) {
            return 0;
        }
        if (artifact.id().equals(NatalBindingService.boundId(player))) {
            int growth = NatalBindingService.growth(player);
            cost = Math.max(0, cost - growth / 25);
        }
        return cost;
    }

    static boolean hasUsableIntegrity(int integrity, int integrityCost, boolean instabuild) {
        return instabuild || integrityCost <= 0 || integrity > 0;
    }

    private static int effectiveSpiritualCost(ServerPlayer player, ArtifactDataService.ArtifactDefinition artifact,
                                              ActivationInfo info) {
        int cost = info.spiritualPowerCost();
        if (artifact.id().equals(NatalBindingService.boundId(player))) {
            int growth = NatalBindingService.growth(player);
            cost = Math.max(1, cost - growth / 50);
        }
        return cost;
    }

    private static int effectiveCooldown(ServerPlayer player, ArtifactDataService.ArtifactDefinition artifact,
                                         ActivationInfo info) {
        int cooldown = info.cooldownTicks();
        if (artifact.id().equals(NatalBindingService.boundId(player))) {
            int growth = NatalBindingService.growth(player);
            // Percent cut saturates softer than flat -60 ticks.
            double factor = 1.0D - Math.min(0.35D, growth / 200.0D);
            cooldown = Math.max(20, (int) Math.round(cooldown * factor));
        }
        return cooldown;
    }

    public static int maxIntegrity(ArtifactDataService.ArtifactDefinition artifact) {
        return 80 + Math.max(1, artifact.gameTier()) * 10;
    }

    public static int getIntegrity(ItemStack stack, ArtifactDataService.ArtifactDefinition artifact) {
        int max = maxIntegrity(artifact);
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(INTEGRITY_TAG, Tag.TAG_INT)) {
            return max;
        }
        return Math.max(0, Math.min(max, tag.getInt(INTEGRITY_TAG)));
    }

    public static int damageIntegrity(ItemStack stack, ArtifactDataService.ArtifactDefinition artifact, int amount) {
        if (amount <= 0) {
            return getIntegrity(stack, artifact);
        }
        int max = maxIntegrity(artifact);
        int next = Math.max(0, getIntegrity(stack, artifact) - amount);
        setIntegrity(stack, next, max);
        return next;
    }

    public static int repairIntegrity(ItemStack stack, ArtifactDataService.ArtifactDefinition artifact, int amount) {
        if (amount <= 0) {
            return getIntegrity(stack, artifact);
        }
        int max = maxIntegrity(artifact);
        int next = Math.min(max, getIntegrity(stack, artifact) + amount);
        setIntegrity(stack, next, max);
        return next;
    }

    private static ActivationInfo activationInfo(ArtifactDataService.Snapshot snapshot,
                                                 ArtifactDataService.ArtifactDefinition artifact) {
        ActivationKind kind = kindFor(artifact);
        if (kind == ActivationKind.NONE) {
            return new ActivationInfo(false, "none", 0, 0, 0, realmFromDesignId(artifact.realmMin()), 0, 0);
        }

        int gameTier = Math.max(1, artifact.gameTier());
        int cost = switch (kind) {
            case MOVEMENT -> 10 + gameTier * 4;
            case DEFENSE -> 12 + gameTier * 5;
            case FOCUS -> 8 + gameTier * 4;
            case OFFENSE -> 14 + gameTier * 6;
            case RULER -> 18 + gameTier * 6;
            case MIRROR -> 16 + gameTier * 5;
            case SOUND -> 18 + gameTier * 6;
            case SWARM -> 18 + gameTier * 7;
            case TELEPORT_PROTECTION -> 18 + gameTier * 5;
            case MAGNET -> 22 + gameTier * 7;
            case WORLD_DOMAIN -> 24 + gameTier * 8;
            case FORMATION -> 18 + gameTier * 6;
            case SOUL_DESTROY -> 18 + gameTier * 8;
            case SPACE_CONTROL -> 20 + gameTier * 7;
            case UTILITY -> 8 + gameTier * 3;
            case CAPTURE -> 20 + gameTier * 7;
            case REFINEMENT -> 16 + gameTier * 6;
            case SPIRIT_LIQUID -> 20 + gameTier * 6;
            case VEHICLE -> 12 + gameTier * 5;
            case ILLUSION -> 16 + gameTier * 5;
            case BEAST_CONTROL -> 14 + gameTier * 5;
            case TALISMAN -> 18 + gameTier * 7;
            case REPAIR -> 5 + gameTier * 2;
            case NONE -> 0;
        };
        int cooldown = switch (kind) {
            case MOVEMENT, FOCUS -> 140 + gameTier * 12;
            case DEFENSE, BEAST_CONTROL -> 180 + gameTier * 16;
            case OFFENSE, ILLUSION -> 160 + gameTier * 18;
            case RULER, MIRROR -> 190 + gameTier * 18;
            case SOUND -> 180 + gameTier * 18;
            case SWARM -> 220 + gameTier * 18;
            case TELEPORT_PROTECTION -> 200 + gameTier * 18;
            case MAGNET, WORLD_DOMAIN -> 240 + gameTier * 22;
            case FORMATION -> 210 + gameTier * 18;
            case SOUL_DESTROY -> 220 + gameTier * 20;
            case SPACE_CONTROL -> 240 + gameTier * 22;
            case UTILITY -> 150 + gameTier * 14;
            case CAPTURE -> 240 + gameTier * 20;
            case REFINEMENT -> 220 + gameTier * 18;
            case SPIRIT_LIQUID -> 300 + gameTier * 22;
            case VEHICLE -> 180 + gameTier * 16;
            case TALISMAN -> 260 + gameTier * 20;
            case REPAIR -> 60;
            case NONE -> 0;
        };
        int uses = kind == ActivationKind.TALISMAN ? talismanUses(snapshot, artifact.id()) : 0;
        int integrityCost = switch (kind) {
            case MOVEMENT, DEFENSE, FOCUS, OFFENSE, ILLUSION, BEAST_CONTROL -> Math.max(1, gameTier / 2);
            case RULER, MIRROR, SOUND, SWARM, SOUL_DESTROY, SPACE_CONTROL -> Math.max(2, gameTier / 2);
            case TELEPORT_PROTECTION, FORMATION -> Math.max(2, gameTier / 2);
            case UTILITY, REFINEMENT, SPIRIT_LIQUID, VEHICLE -> Math.max(1, gameTier / 2);
            case CAPTURE -> Math.max(2, gameTier / 2);
            case MAGNET, WORLD_DOMAIN -> Math.max(3, gameTier / 2);
            default -> 0;
        };
        int repairAmount = kind == ActivationKind.REPAIR ? 50 + gameTier * 15 : 0;
        return new ActivationInfo(true, kind.id, cost, cooldown, uses, minRealmFor(artifact),
                integrityCost, repairAmount);
    }

    private static ActivationKind kindFor(ArtifactDataService.ArtifactDefinition artifact) {
        if ("artifact_repair_kit".equals(artifact.id())) {
            return ActivationKind.REPAIR;
        }
        String type = artifact.type().toLowerCase(Locale.ROOT);
        String id = artifact.id() == null ? "" : artifact.id().toLowerCase(Locale.ROOT);
        String effect = artifact.effect() == null ? "" : artifact.effect().toLowerCase(Locale.ROOT);
        // Wave462: material shards route by id/effect keywords.
        if ("material_artifact".equals(type)) {
            if (id.contains("mirror") || effect.contains("mirror") || effect.contains("soul")) {
                return id.contains("soul") || effect.contains("soul") ? ActivationKind.SOUL_DESTROY : ActivationKind.MIRROR;
            }
            if (id.contains("ruler") || effect.contains("ruler") || effect.contains("space")) {
                return ActivationKind.SPACE_CONTROL;
            }
            if (id.contains("flag") || id.contains("disk") || effect.contains("formation")) {
                return ActivationKind.FORMATION;
            }
            if (id.contains("inlay") || id.contains("socket") || effect.contains("craft") || effect.contains("refine")) {
                return ActivationKind.REFINEMENT;
            }
            if (effect.contains("self_repair") || id.contains("repair") || effect.contains("repair")) {
                return ActivationKind.REPAIR;
            }
            if (id.contains("natal") || effect.contains("natal")) {
                return ActivationKind.FOCUS;
            }
            return ActivationKind.UTILITY;
        }
        if ("generic".equals(type) || id.startsWith("generic_treasure")) {
            int tier = Math.max(1, artifact.gameTier());
            if (tier <= 3) {
                return ActivationKind.DEFENSE;
            }
            if (tier <= 7) {
                return ActivationKind.OFFENSE;
            }
            if (tier <= 9) {
                return ActivationKind.SOUL_DESTROY;
            }
            return ActivationKind.WORLD_DOMAIN;
        }
        return switch (type) {
            case "movement" -> ActivationKind.MOVEMENT;
            case "defense" -> ActivationKind.DEFENSE;
            case "anti_illusion", "soul_stabilize" -> ActivationKind.FOCUS;
            case "ruler" -> ActivationKind.RULER;
            case "mirror" -> ActivationKind.MIRROR;
            case "sound" -> ActivationKind.SOUND;
            case "swarm" -> ActivationKind.SWARM;
            case "teleport_protection" -> ActivationKind.TELEPORT_PROTECTION;
            case "magnet" -> ActivationKind.MAGNET;
            case "world" -> ActivationKind.WORLD_DOMAIN;
            case "formation", "formation_token", "formation_deploy" -> ActivationKind.FORMATION;
            case "illusion" -> ActivationKind.ILLUSION;
            case "beast", "beast_control", "beast_refine", "beast_spirit",
                    "puppet", "puppet_control", "puppet_summon", "puppet_core",
                    "hybrid_puppet_core" -> ActivationKind.BEAST_CONTROL;
            case "soul_destroy" -> ActivationKind.SOUL_DESTROY;
            case "space_control" -> ActivationKind.SPACE_CONTROL;
            case "soul" -> ActivationKind.SOUL_DESTROY;
            case "utility", "storage", "quest_key" -> ActivationKind.UTILITY;
            case "capture" -> ActivationKind.CAPTURE;
            case "refinement" -> ActivationKind.REFINEMENT;
            case "spirit_liquid" -> ActivationKind.SPIRIT_LIQUID;
            case "vehicle", "vehicle_key" -> ActivationKind.VEHICLE;
            case "flying_sword", "offense", "attack", "poison", "soul_attack", "control", "yin", "thunder" -> ActivationKind.OFFENSE;
            case "talisman_treasure" -> ActivationKind.TALISMAN;
            default -> ActivationKind.NONE;
        };
    }

    private static boolean isDeferredType(String type) {
        // Wave462: only natal_slot stays deferred (bind path is sneak-use, not combat activate).
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "natal_slot" -> true;
            default -> false;
        };
    }

    private static Realm realmFromDesignId(String id) {
        if (id == null || id.isBlank()) {
            return Realm.MORTAL;
        }
        return switch (id.toUpperCase(Locale.ROOT)) {
            case "QI_REFINING" -> Realm.QI_REFINING;
            case "FOUNDATION", "FOUNDATION_ESTABLISHMENT" -> Realm.FOUNDATION_ESTABLISHMENT;
            case "CORE_FORMATION" -> Realm.CORE_FORMATION;
            case "NASCENT_SOUL" -> Realm.NASCENT_SOUL;
            case "SOUL_TRANSFORMATION" -> Realm.SOUL_TRANSFORMATION;
            case "VOID_REFINEMENT" -> Realm.VOID_REFINEMENT;
            case "UNITY" -> Realm.UNITY;
            case "GREAT_VEHICLE", "MAHAYANA" -> Realm.MAHAYANA;
            case "TRIBULATION" -> Realm.TRIBULATION;
            case "TRUE_IMMORTAL" -> Realm.TRUE_IMMORTAL;
            default -> Realm.MORTAL;
        };
    }

    private static Realm minRealmFor(ArtifactDataService.ArtifactDefinition artifact) {
        Realm explicit = realmFromDesignId(artifact.realmMin());
        if (explicit != Realm.MORTAL || (artifact.realmMin() != null && !artifact.realmMin().isBlank())) {
            return explicit;
        }
        String tier = artifact.tier().toLowerCase(Locale.ROOT);
        if ("spirit_treasure".equals(tier) || artifact.gameTier() >= 11) {
            return Realm.VOID_REFINEMENT;
        }
        if ("ancient_treasure".equals(tier) || artifact.gameTier() >= 10) {
            return Realm.NASCENT_SOUL;
        }
        if (artifact.gameTier() >= 7) {
            return Realm.CORE_FORMATION;
        }
        if (artifact.gameTier() >= 4) {
            return Realm.FOUNDATION_ESTABLISHMENT;
        }
        if (artifact.gameTier() >= 1) {
            return Realm.QI_REFINING;
        }
        return Realm.MORTAL;
    }

    private static int talismanUses(ArtifactDataService.Snapshot snapshot, String artifactId) {
        return snapshot.talismanTreasureTemplates().getOrDefault(artifactId,
                new ArtifactDataService.TalismanTreasureTemplate(artifactId, artifactId, "", 3)).defaultUses();
    }

    private static void applyActivation(ServerPlayer player, PlayerCultivation cultivation,
                                        ArtifactDataService.ArtifactDefinition artifact, ActivationInfo info) {
        switch (ActivationKind.byId(info.kind())) {
            case MOVEMENT -> applyMovement(player, artifact.gameTier());
            case DEFENSE -> applyDefense(player, artifact.gameTier());
            case FOCUS -> applyFocus(player, cultivation, artifact.gameTier());
            case OFFENSE -> launchProjectile(player, artifact, artifact.gameTier());
            case RULER -> applyRuler(player, artifact.gameTier());
            case MIRROR -> applyMirror(player, artifact.gameTier());
            case SOUND -> applySound(player, artifact.gameTier());
            case SWARM -> applySwarm(player, artifact.gameTier());
            case TELEPORT_PROTECTION -> applyTeleportProtection(player, artifact.gameTier());
            case MAGNET -> applyMagnet(player, artifact.gameTier());
            case WORLD_DOMAIN -> applyWorldDomain(player, artifact.gameTier());
            case FORMATION -> applyFormation(player, artifact.gameTier());
            case SOUL_DESTROY -> applySoulDestroy(player, artifact.gameTier());
            case SPACE_CONTROL -> applySpaceControl(player, artifact.gameTier());
            case UTILITY -> applyUtility(player, cultivation, artifact);
            case CAPTURE -> applyCapture(player, artifact.gameTier());
            case REFINEMENT -> applyRefinement(player, cultivation, artifact.gameTier());
            case SPIRIT_LIQUID -> applySpiritLiquid(player, cultivation, artifact.gameTier());
            case VEHICLE -> applyVehicle(player, artifact.gameTier());
            case ILLUSION -> applyIllusion(player, artifact.gameTier());
            case BEAST_CONTROL -> applyBeastControl(player, artifact.gameTier());
            case TALISMAN -> applyTalisman(player, cultivation, artifact);
            case REPAIR -> {
            }
            case NONE -> {
            }
        }
    }

    private static RepairTarget findRepairTarget(ServerPlayer player, InteractionHand repairHand,
                                                 ArtifactDataService.Snapshot snapshot) {
        ItemStack target = repairHand == InteractionHand.MAIN_HAND ? player.getOffhandItem() : player.getMainHandItem();
        if (target.isEmpty() || !(target.getItem() instanceof ArtifactCatalogItem artifactItem)
                || "artifact_repair_kit".equals(artifactItem.artifactId())) {
            return null;
        }
        ArtifactDataService.ArtifactDefinition artifact = snapshot.findArtifact(artifactItem.artifactId()).orElse(null);
        if (artifact == null) {
            return null;
        }
        int max = maxIntegrity(artifact);
        int current = getIntegrity(target, artifact);
        return new RepairTarget(target, artifact, current, max);
    }

    private static void applyRepair(ServerPlayer player, ItemStack repairStack, RepairTarget target,
                                    ActivationInfo info) {
        if (target.currentIntegrity() >= target.maxIntegrity()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.repair_full", target.stack().getHoverName()), true);
            return;
        }
        int repaired = repairIntegrity(target.stack(), target.artifact(), info.repairAmount());
        if (repaired <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.repair_full", target.stack().getHoverName()), true);
            return;
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.artifact.repaired",
                target.stack().getHoverName(), repaired, target.maxIntegrity()), true);
        if (!player.getAbilities().instabuild) {
            repairStack.shrink(1);
        }
    }

    private static void applyMovement(ServerPlayer player, int gameTier) {
        int duration = 220 + gameTier * 35;
        int speedAmplifier = gameTier >= 4 ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, speedAmplifier, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, Math.min(duration, 220), 0, false, true));
    }

    private static void applyDefense(ServerPlayer player, int gameTier) {
        int duration = 240 + gameTier * 35;
        int resistanceAmplifier = gameTier >= 5 ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, resistanceAmplifier, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(0, gameTier / 3), false, true));
    }

    private static void applyFocus(ServerPlayer player, PlayerCultivation cultivation, int gameTier) {
        player.removeEffect(MobEffects.CONFUSION);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.POISON);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300 + gameTier * 40, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120 + gameTier * 10, 0, false, true));
        cultivation.addDivineConsciousness(Math.max(1, gameTier / 2));
        cultivation.addQiDeviationRisk(-Math.max(1, gameTier / 2));
    }

    private static void launchProjectile(ServerPlayer player, ArtifactDataService.ArtifactDefinition artifact,
                                         int gameTier) {
        double damage = 4.0D + gameTier * 2.5D;
        Vec3 look = player.getLookAngle();
        CultivationFireballEntity projectile = new CultivationFireballEntity(
                player.serverLevel(), player, look, damage, 1.22D, elementFor(artifact));
        player.serverLevel().addFreshEntity(projectile);
    }

    private static void applyRuler(ServerPlayer player, int gameTier) {
        Vec3 center = player.getEyePosition().add(player.getLookAngle().normalize().scale(3.5D));
        double damage = 4.0D + gameTier * 1.8D;
        affectArea(player, center, 3.6D + gameTier * 0.2D, living -> {
            living.hurt(player.damageSources().magic(), (float) damage);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 2, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0, false, true));
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                54, 0.95D, 0.75D, 0.95D, 0.04D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.7F, 0.85F);
    }

    private static void applyMirror(ServerPlayer player, int gameTier) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220 + gameTier * 25, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120 + gameTier * 12, 0, false, true));
        affectNearby(player, 5.0D + gameTier * 0.25D, living -> {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, true));
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.15D, player.getZ(),
                64, 1.2D, 0.75D, 1.2D, 0.05D);
        level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                SoundSource.PLAYERS, 0.55F, 1.1F);
    }

    private static void applySound(ServerPlayer player, int gameTier) {
        double radius = 4.8D + gameTier * 0.28D;
        double damage = 3.5D + gameTier * 1.5D;
        affectNearby(player, radius, living -> {
            living.hurt(player.damageSources().magic(), (float) damage);
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, true));
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.0D, player.getZ(),
                36, radius * 0.16D, 0.7D, radius * 0.16D, 0.0D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.8F, 0.65F);
    }

    private static void applySwarm(ServerPlayer player, int gameTier) {
        double radius = 5.0D + gameTier * 0.3D;
        double damage = 3.0D + gameTier * 1.35D;
        affectNearby(player, radius, living -> {
            living.hurt(player.damageSources().magic(), (float) damage);
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 120 + gameTier * 10, 0, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0, false, true));
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0D, player.getZ(),
                90, radius * 0.2D, 0.85D, radius * 0.2D, 0.12D);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 0.55F, 1.35F);
    }

    private static void applyTeleportProtection(ServerPlayer player, int gameTier) {
        ServerLevel level = player.serverLevel();
        Vec3 before = player.position();
        Vec3 destination = findBlinkDestination(player, Math.min(10.0D, 4.0D + gameTier * 0.45D));
        if (destination != null) {
            level.sendParticles(ParticleTypes.PORTAL, before.x, before.y + 1.0D, before.z,
                    48, 0.45D, 0.75D, 0.45D, 0.08D);
            player.teleportTo(destination.x, destination.y, destination.z);
            level.sendParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.0D, destination.z,
                    56, 0.5D, 0.8D, 0.5D, 0.1D);
        }
        int duration = 180 + gameTier * 18;
        player.clearFire();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, gameTier >= 8 ? 1 : 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(1, gameTier / 4), false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, Math.min(duration, 260), 0, false, true));
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.6F, 0.9F);
    }

    private static void applyMagnet(ServerPlayer player, int gameTier) {
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D)
                .add(player.getLookAngle().normalize().scale(2.6D));
        double radius = 5.2D + gameTier * 0.35D;
        double damage = 3.0D + gameTier * 1.2D;
        affectArea(player, center, radius, living -> {
            living.hurt(player.damageSources().magic(), (float) damage);
            Vec3 pull = center.subtract(living.position());
            if (pull.lengthSqr() > 0.04D) {
                living.setDeltaMovement(living.getDeltaMovement()
                        .add(pull.normalize().scale(0.42D))
                        .multiply(0.72D, 0.55D, 0.72D));
                living.hasImpulse = true;
            }
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, true));
        });
        bendProjectilesTo(player, center, radius + 1.5D);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
                96, radius * 0.16D, 0.8D, radius * 0.16D, 0.12D);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.55F, 0.65F);
    }

    private static void applyWorldDomain(ServerPlayer player, int gameTier) {
        double radius = 5.8D + gameTier * 0.35D;
        int duration = 180 + gameTier * 18;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, gameTier >= 10 ? 1 : 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100 + gameTier * 8, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(1, gameTier / 4), false, true));
        affectNearby(player, radius, living -> {
            living.hurt(player.damageSources().magic(), (float) (2.0D + gameTier * 0.9D));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0, false, true));
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.1D, player.getZ(),
                110, radius * 0.18D, 1.0D, radius * 0.18D, 0.07D);
        level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE,
                SoundSource.PLAYERS, 0.55F, 0.8F);
    }

    private static void applyFormation(ServerPlayer player, int gameTier) {
        // Wave49: free persistent field around player + legacy buffs.
        if (player.level() instanceof ServerLevel serverLevel) {
            FormationFieldService.activateFreeField(serverLevel, player.blockPosition(),
                    FormationFieldService.FieldKind.DEFENSE, 160 + gameTier * 16, player);
        }
        double radius = 5.0D + gameTier * 0.32D;
        int duration = 160 + gameTier * 16;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, gameTier >= 7 ? 1 : 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, true));
        deflectProjectiles(player, radius);
        affectNearby(player, radius, living -> {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, true));
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.9D, player.getZ(),
                84, radius * 0.18D, 0.45D, radius * 0.18D, 0.03D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.7F, 0.72F);
    }

    private static void applyIllusion(ServerPlayer player, int gameTier) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 180 + gameTier * 30, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220 + gameTier * 30, 0, false, true));
        affectNearby(player, 5.0D + gameTier * 0.35D, living -> {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, true));
        });
    }

    private static void applySoulDestroy(ServerPlayer player, int gameTier) {
        Vec3 center = player.getEyePosition().add(player.getLookAngle().normalize().scale(4.0D));
        double damage = 5.0D + gameTier * 2.2D;
        affectArea(player, center, 3.2D + gameTier * 0.25D, living -> {
            living.hurt(player.damageSources().magic(), (float) damage);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true));
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z,
                42, 1.1D, 0.75D, 1.1D, 0.04D);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE,
                SoundSource.PLAYERS, 0.65F, 0.78F);
    }

    private static void applySpaceControl(ServerPlayer player, int gameTier) {
        double radius = 5.5D + gameTier * 0.35D;
        double damage = 2.0D + gameTier * 0.8D;
        affectNearby(player, radius, living -> {
            living.hurt(player.damageSources().magic(), (float) damage);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 2, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, true));
            living.setDeltaMovement(living.getDeltaMovement().scale(0.25D));
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(),
                96, radius * 0.18D, 0.95D, radius * 0.18D, 0.08D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 0.45F, 0.72F);
    }

    private static void applyUtility(ServerPlayer player, PlayerCultivation cultivation,
                                     ArtifactDataService.ArtifactDefinition artifact) {
        String id = artifact.id().toLowerCase(Locale.ROOT);
        String effect = artifact.effect() == null ? "" : artifact.effect().toLowerCase(Locale.ROOT);
        int gameTier = artifact.gameTier();
        int duration = 180 + gameTier * 22;
        if (effect.contains("hide") || effect.contains("aggro") || id.contains("stealth")
                || id.contains("conceal")) {
            player.removeEffect(MobEffects.GLOWING);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, gameTier >= 5 ? 1 : 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, true));
            pulseUtilityParticles(player, ParticleTypes.SMOKE);
            return;
        }
        if (effect.contains("reveal") || effect.contains("detect") || effect.contains("identify")
                || effect.contains("appraisal") || effect.contains("spatial") || effect.contains("route")
                || effect.contains("message")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration + 120, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, Math.max(0, gameTier / 5), false, true));
            affectNearby(player, 5.0D + gameTier * 0.25D, living ->
                    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0, false, true)));
            pulseUtilityParticles(player, ParticleTypes.ENCHANT);
            return;
        }
        if (effect.contains("natal") || effect.contains("awaken") || effect.contains("growth")) {
            cultivation.addDivineConsciousness(Math.max(1, gameTier / 2));
            cultivation.addQiDeviationRisk(-Math.max(1, gameTier / 3));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120 + gameTier * 10, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.max(0, gameTier / 4), false, true));
            pulseUtilityParticles(player, ParticleTypes.HAPPY_VILLAGER);
            return;
        }
        cultivation.addQiDeviationRisk(-Math.max(1, gameTier / 4));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80 + gameTier * 8, 0, false, true));
        pulseUtilityParticles(player, ParticleTypes.END_ROD);
    }

    private static void pulseUtilityParticles(ServerPlayer player, net.minecraft.core.particles.ParticleOptions particle) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(particle, player.getX(), player.getY() + 1.0D, player.getZ(),
                42, 0.85D, 0.65D, 0.85D, 0.04D);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.5F, 1.15F);
    }

    private static void applyCapture(ServerPlayer player, int gameTier) {
        Vec3 center = player.getEyePosition().add(player.getLookAngle().normalize().scale(3.2D));
        double radius = 3.8D + gameTier * 0.28D;
        double damage = 3.0D + gameTier * 1.15D;
        affectArea(player, center, radius, living -> {
            living.hurt(player.damageSources().magic(), (float) damage);
            Vec3 pull = center.subtract(living.position());
            if (pull.lengthSqr() > 0.04D) {
                living.setDeltaMovement(living.getDeltaMovement()
                        .add(pull.normalize().scale(0.32D))
                        .multiply(0.45D, 0.35D, 0.45D));
                living.hasImpulse = true;
            }
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 220, 3, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 160, 0, false, true));
        });
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z,
                78, radius * 0.18D, 0.8D, radius * 0.18D, 0.07D);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 0.6F, 0.72F);
    }

    private static void applyRefinement(ServerPlayer player, PlayerCultivation cultivation, int gameTier) {
        int duration = 200 + gameTier * 18;
        cultivation.addQiDeviationRisk(-Math.max(1, gameTier / 2));
        cultivation.addDivineConsciousness(Math.max(1, gameTier / 3));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, gameTier >= 10 ? 1 : 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, duration, Math.max(0, gameTier / 5), false, true));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0D, player.getZ(),
                72, 0.75D, 0.7D, 0.75D, 0.03D);
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.2D, player.getZ(),
                32, 0.5D, 0.45D, 0.5D, 0.02D);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, 0.55F, 0.78F);
    }

    private static void applySpiritLiquid(ServerPlayer player, PlayerCultivation cultivation, int gameTier) {
        int spiritualPower = 18 + gameTier * 6;
        cultivation.addSpiritualPower(spiritualPower);
        cultivation.addDivineConsciousness(Math.max(1, gameTier / 3));
        cultivation.addQiDeviationRisk(-Math.max(1, gameTier / 2));
        player.heal((float) (4.0D + gameTier * 1.2D));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120 + gameTier * 12, gameTier >= 11 ? 1 : 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 220 + gameTier * 12, Math.max(1, gameTier / 4), false, true));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0D, player.getZ(),
                64, 0.7D, 0.75D, 0.7D, 0.04D);
        level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE,
                SoundSource.PLAYERS, 0.5F, 1.35F);
    }

    private static void applyVehicle(ServerPlayer player, int gameTier) {
        int duration = 240 + gameTier * 28;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, gameTier >= 7 ? 2 : 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, gameTier >= 7 ? 1 : 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration + 80, 0, false, true));
        Vec3 forward = player.getLookAngle();
        player.setDeltaMovement(player.getDeltaMovement()
                .add(new Vec3(forward.x, 0.0D, forward.z).normalize().scale(0.45D + gameTier * 0.02D))
                .add(0.0D, 0.12D, 0.0D));
        player.hasImpulse = true;
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.35D, player.getZ(),
                58, 0.8D, 0.25D, 0.8D, 0.08D);
        level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP,
                SoundSource.PLAYERS, 0.65F, 1.2F);
    }

    private static void applyBeastControl(ServerPlayer player, int gameTier) {
        affectNearby(player, 5.0D + gameTier * 0.4D, living -> {
            if (living instanceof Monster) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 1, false, true));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 1, false, true));
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, true));
            }
        });
    }

    private static void applyTalisman(ServerPlayer player, PlayerCultivation cultivation,
                                      ArtifactDataService.ArtifactDefinition artifact) {
        if ("talisman_treasure_demon_seal".equals(artifact.id())) {
            double damage = 6.0D + artifact.gameTier() * 3.0D;
            affectNearby(player, 7.0D, living -> {
                if (living instanceof Monster) {
                    living.hurt(player.damageSources().magic(), (float) damage);
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 3, false, true));
                    living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 240, 2, false, true));
                    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 180, 0, false, true));
                }
            });
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2D, player.getZ(),
                    72, 1.8D, 1.0D, 1.8D, 0.08D);
            level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                    SoundSource.PLAYERS, 0.65F, 0.95F);
            return;
        }
        if ("talisman_treasure_soul_charm".equals(artifact.id())) {
            applyFocus(player, cultivation, artifact.gameTier());
            affectNearby(player, 6.0D, living -> {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, 1, false, true));
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 140, 0, false, true));
            });
            return;
        }
        double damage = 8.0D + artifact.gameTier() * 4.0D;
        Vec3 look = player.getLookAngle();
        CultivationFireballEntity projectile = new CultivationFireballEntity(
                player.serverLevel(), player, look, damage, 1.05D, elementFor(artifact));
        player.serverLevel().addFreshEntity(projectile);
    }

    private static CultivationFireballEntity.SpellElement elementFor(ArtifactDataService.ArtifactDefinition artifact) {
        String id = artifact.id().toLowerCase(Locale.ROOT);
        String type = artifact.type().toLowerCase(Locale.ROOT);
        String effect = artifact.effect() == null ? "" : artifact.effect().toLowerCase(Locale.ROOT);
        if (id.contains("void") || type.contains("space") || effect.contains("void")
                || id.contains("soul") || type.contains("soul") || id.contains("demon")) {
            return CultivationFireballEntity.SpellElement.DARK;
        }
        if (id.contains("snake") || id.contains("moon") || id.contains("cold") || id.contains("ice")) {
            return CultivationFireballEntity.SpellElement.WATER;
        }
        if (id.contains("thunder") || type.contains("thunder")) {
            return CultivationFireballEntity.SpellElement.THUNDER;
        }
        if (id.contains("fire") || id.contains("flame") || effect.contains("flame")) {
            return CultivationFireballEntity.SpellElement.FIRE;
        }
        return CultivationFireballEntity.SpellElement.METAL;
    }

    private static void affectArea(ServerPlayer player, Vec3 center, double radius,
                                   java.util.function.Consumer<LivingEntity> effect) {
        ServerLevel level = player.serverLevel();
        AABB area = new AABB(center, center).inflate(radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != player && !(entity instanceof Player))) {
            effect.accept(living);
        }
    }

    private static void affectNearby(ServerPlayer player, double radius, java.util.function.Consumer<LivingEntity> effect) {
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && entity != player && !(entity instanceof Player))) {
            effect.accept(living);
        }
    }

    private static int bendProjectilesTo(ServerPlayer player, Vec3 focus, double radius) {
        ServerLevel level = player.serverLevel();
        AABB area = new AABB(focus, focus).inflate(radius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int bent = 0;
        for (Projectile projectile : projectiles) {
            Vec3 pull = focus.subtract(projectile.position());
            if (pull.lengthSqr() < 0.01D) {
                continue;
            }
            double speed = Math.max(0.34D, projectile.getDeltaMovement().length());
            projectile.setDeltaMovement(pull.normalize().scale(speed * 0.75D));
            projectile.hasImpulse = true;
            bent++;
        }
        return bent;
    }

    private static int deflectProjectiles(ServerPlayer player, double radius) {
        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(radius);
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, area,
                projectile -> projectile.isAlive() && projectile.getOwner() != player);
        int deflected = 0;
        for (Projectile projectile : projectiles) {
            Vec3 away = projectile.position().subtract(player.position());
            if (away.lengthSqr() < 0.01D) {
                away = player.getLookAngle();
            }
            projectile.setOwner(player);
            projectile.setDeltaMovement(away.normalize()
                    .scale(Math.max(0.46D, projectile.getDeltaMovement().length() + 0.14D))
                    .add(0.0D, 0.05D, 0.0D));
            projectile.hasImpulse = true;
            deflected++;
        }
        return deflected;
    }

    private static Vec3 findBlinkDestination(ServerPlayer player, double distance) {
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        if (forward.lengthSqr() < 0.0001D) {
            forward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vec3 left = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3[] directions = new Vec3[]{forward, forward.scale(-1.0D), left, left.scale(-1.0D)};
        for (Vec3 direction : directions) {
            for (double step = distance; step >= 2.0D; step -= 1.0D) {
                BlockPos base = BlockPos.containing(
                        player.getX() + direction.x * step,
                        player.getY(),
                        player.getZ() + direction.z * step);
                Vec3 safe = firstSafePositionNear(player.serverLevel(), base);
                if (safe != null) {
                    return safe;
                }
            }
        }
        return null;
    }

    private static Vec3 firstSafePositionNear(ServerLevel level, BlockPos base) {
        for (int yOffset = 2; yOffset >= -3; yOffset--) {
            BlockPos feet = base.offset(0, yOffset, 0);
            if (isSafeTeleportTarget(level, feet)) {
                return new Vec3(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
            }
        }
        return null;
    }

    private static boolean isSafeTeleportTarget(ServerLevel level, BlockPos feet) {
        if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(feet.above())) {
            return false;
        }
        BlockPos belowPos = feet.below();
        BlockState below = level.getBlockState(belowPos);
        return below.isSolidRender(level, belowPos)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static void consumeTalismanUse(ServerPlayer player, ItemStack stack, ActivationInfo info) {
        if (info.maxUses() <= 0 || player.getAbilities().instabuild) {
            return;
        }
        int next = getUsesLeft(stack, info.maxUses()) - 1;
        if (next > 0) {
            stack.getOrCreateTag().putInt(USES_LEFT_TAG, next);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.talisman_uses_left", stack.getHoverName(), next), true);
            return;
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.artifact.talisman_depleted", stack.getHoverName()), true);
        stack.shrink(1);
    }

    private static void playActivationFeedback(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                18, 0.45D, 0.55D, 0.45D, 0.03D);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.55F, 1.45F);
    }

    private static void setIntegrity(ItemStack stack, int value, int max) {
        if (value >= max) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(INTEGRITY_TAG);
            }
            return;
        }
        stack.getOrCreateTag().putInt(INTEGRITY_TAG, value);
    }

    private enum ActivationKind {
        NONE("none"),
        MOVEMENT("movement"),
        DEFENSE("defense"),
        FOCUS("focus"),
        OFFENSE("offense"),
        RULER("ruler"),
        MIRROR("mirror"),
        SOUND("sound"),
        SWARM("swarm"),
        TELEPORT_PROTECTION("teleport_protection"),
        MAGNET("magnet"),
        WORLD_DOMAIN("world"),
        FORMATION("formation"),
        SOUL_DESTROY("soul_destroy"),
        SPACE_CONTROL("space_control"),
        UTILITY("utility"),
        CAPTURE("capture"),
        REFINEMENT("refinement"),
        SPIRIT_LIQUID("spirit_liquid"),
        VEHICLE("vehicle"),
        ILLUSION("illusion"),
        BEAST_CONTROL("beast_control"),
        TALISMAN("talisman"),
        REPAIR("repair");

        private final String id;

        ActivationKind(String id) {
            this.id = id;
        }

        private static ActivationKind byId(String id) {
            for (ActivationKind kind : values()) {
                if (kind.id.equals(id)) {
                    return kind;
                }
            }
            return NONE;
        }
    }

    public record ActivationInfo(
            boolean supported,
            String kind,
            int spiritualPowerCost,
            int cooldownTicks,
            int maxUses,
            Realm minRealm,
            int integrityCost,
            int repairAmount
    ) {}

    private record RepairTarget(
            ItemStack stack,
            ArtifactDataService.ArtifactDefinition artifact,
            int currentIntegrity,
            int maxIntegrity
    ) {}
}
