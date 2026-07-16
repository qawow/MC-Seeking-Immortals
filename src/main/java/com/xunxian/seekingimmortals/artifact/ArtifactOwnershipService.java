package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.ProgressionGateApi;
import com.xunxian.seekingimmortals.cultivation.PlayerCultivation;
import com.xunxian.seekingimmortals.cultivation.Realm;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * M15 认主绑定：物品 NBT 存主人 UUID + 玩家侧本命记录。
 * 红线：偷取/拾取他人本命法宝不可用；越阶使用由 {@link ArtifactPowerService} 压制。
 */
public final class ArtifactOwnershipService {
    public static final String OWNER_UUID_TAG = "SeekingImmortalsArtifactOwner";
    public static final String OWNER_NAME_TAG = "SeekingImmortalsArtifactOwnerName";
    public static final String REFINEMENT_LAYER_TAG = "SeekingImmortalsArtifactRefineLayer";
    public static final String SPIRIT_AWAKENED_TAG = "SeekingImmortalsArtifactSpiritAwakened";
    public static final String CLAIMED_AT_TAG = "SeekingImmortalsArtifactClaimedAt";

    /** 器灵觉醒：祭炼层数门槛。 */
    public static final int SPIRIT_AWAKEN_LAYER = 5;
    /** 器灵觉醒：最低境界（结丹）。 */
    public static final Realm SPIRIT_AWAKEN_REALM = Realm.CORE_FORMATION;
    public static final int MAX_REFINEMENT_LAYER = 9;

    private ArtifactOwnershipService() {}

    public static boolean claim(ServerPlayer player, ItemStack stack, String artifactId) {
        if (player == null || stack == null || stack.isEmpty() || artifactId == null || artifactId.isBlank()) {
            return false;
        }
        ArtifactDataService.ArtifactDefinition def = ArtifactDataService.builtin()
                .findArtifact(artifactId).orElse(null);
        if (def == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.claim.unknown", artifactId), true);
            return false;
        }
        if (!meetsClaimRealm(player, def)) {
            Realm required = Realm.fromDesignIdOrMortal(def.realmMin());
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.claim.realm_too_low",
                    def.display(), required.getDisplayName()), true);
            return false;
        }
        Optional<UUID> existing = ownerUuid(stack);
        if (existing.isPresent() && !existing.get().equals(player.getUUID())
                && !player.getAbilities().instabuild) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.claim.owned_by_other",
                    ownerName(stack)), true);
            return false;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(OWNER_UUID_TAG, player.getUUID());
        tag.putString(OWNER_NAME_TAG, player.getGameProfile().getName());
        tag.putLong(CLAIMED_AT_TAG, player.level().getGameTime());
        if (!tag.contains(REFINEMENT_LAYER_TAG, Tag.TAG_INT)) {
            tag.putInt(REFINEMENT_LAYER_TAG, 0);
        }
        // 本命认主（最多一件）：与 NatalBindingService 协同，不覆盖已有本命除非创造模式。
        if (NatalBindingService.boundId(player).isBlank() || player.getAbilities().instabuild) {
            NatalBindingService.bind(player, stack);
        }
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.artifact.claim.success", def.display()), true);
        return true;
    }

    public static boolean isUsableBy(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        Optional<UUID> owner = ownerUuid(stack);
        if (owner.isEmpty()) {
            // 未认主：允许检视/认主，但不允许激活主动技（由调用方决定）。
            return true;
        }
        return owner.get().equals(player.getUUID());
    }

    public static boolean requiresOwnerForActivation(ItemStack stack) {
        return ownerUuid(stack).isPresent() || isHighTier(stack);
    }

    public static boolean canActivate(ServerPlayer player, ItemStack stack, String artifactId) {
        if (player == null || stack == null) {
            return false;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        Optional<UUID> owner = ownerUuid(stack);
        if (owner.isPresent()) {
            if (!owner.get().equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.artifact.claim.stolen_unusable"), true);
                return false;
            }
            return true;
        }
        // 高阶古宝/灵宝必须先认主。
        ArtifactDataService.ArtifactDefinition def = ArtifactDataService.builtin()
                .findArtifact(artifactId).orElse(null);
        if (def != null && (def.gameTier() >= 9
                || "ancient_treasure".equalsIgnoreCase(def.tier())
                || "spirit_treasure".equalsIgnoreCase(def.tier())
                || "legendary".equalsIgnoreCase(def.tier()))) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.claim.required",
                    def.display()), true);
            return false;
        }
        return true;
    }

    public static Optional<UUID> ownerUuid(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(OWNER_UUID_TAG)) {
            return Optional.empty();
        }
        return Optional.of(tag.getUUID(OWNER_UUID_TAG));
    }

    public static String ownerName(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return "";
        }
        return stack.getTag().getString(OWNER_NAME_TAG);
    }

    public static int refinementLayer(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return 0;
        }
        return Math.max(0, Math.min(MAX_REFINEMENT_LAYER, stack.getTag().getInt(REFINEMENT_LAYER_TAG)));
    }

    public static int addRefinementLayer(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) {
            return refinementLayer(stack);
        }
        int next = Math.min(MAX_REFINEMENT_LAYER, refinementLayer(stack) + amount);
        stack.getOrCreateTag().putInt(REFINEMENT_LAYER_TAG, next);
        return next;
    }

    public static boolean isSpiritAwakened(ItemStack stack) {
        return stack != null && stack.hasTag() && stack.getTag().getBoolean(SPIRIT_AWAKENED_TAG);
    }

    /**
     * 器灵觉醒：本命绑定 + 祭炼层数 &gt;= {@link #SPIRIT_AWAKEN_LAYER} + 结丹境 + 主人本人。
     */
    public static boolean tryAwakenSpirit(ServerPlayer player, ItemStack stack, String artifactId) {
        if (player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (!canActivate(player, stack, artifactId)) {
            return false;
        }
        if (isSpiritAwakened(stack)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.spirit.already"), true);
            return false;
        }
        if (refinementLayer(stack) < SPIRIT_AWAKEN_LAYER) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.spirit.need_layer",
                    SPIRIT_AWAKEN_LAYER, refinementLayer(stack)), true);
            return false;
        }
        if (!ProgressionGateApi.meetsRealm(player, SPIRIT_AWAKEN_REALM.getDesignId())) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.spirit.need_realm",
                    SPIRIT_AWAKEN_REALM.getDisplayName()), true);
            return false;
        }
        String bound = NatalBindingService.boundId(player);
        if (bound == null || bound.isBlank() || !bound.equals(artifactId)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.artifact.spirit.need_natal"), true);
            return false;
        }
        stack.getOrCreateTag().putBoolean(SPIRIT_AWAKENED_TAG, true);
        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.artifact.spirit.awakened", artifactId), true);
        return true;
    }

    public static boolean meetsClaimRealm(ServerPlayer player, ArtifactDataService.ArtifactDefinition def) {
        if (def == null) {
            return false;
        }
        String realmMin = def.realmMin();
        if (realmMin == null || realmMin.isBlank()) {
            // 回退：按 game_tier 推导门槛。
            int tier = Math.max(1, def.gameTier());
            if (tier >= 10) {
                realmMin = Realm.VOID_REFINEMENT.getDesignId();
            } else if (tier >= 9) {
                realmMin = Realm.NASCENT_SOUL.getDesignId();
            } else if (tier >= 7) {
                realmMin = Realm.CORE_FORMATION.getDesignId();
            } else if (tier >= 4) {
                realmMin = Realm.FOUNDATION_ESTABLISHMENT.getDesignId();
            } else {
                realmMin = Realm.QI_REFINING.getDesignId();
            }
        }
        return ProgressionGateApi.meetsRealm(player, realmMin);
    }

    public static boolean meetsClaimRealm(PlayerCultivation cultivation, ArtifactDataService.ArtifactDefinition def) {
        if (cultivation == null || def == null) {
            return false;
        }
        return ProgressionGateApi.meetsRealm(cultivation, def.realmMin());
    }

    private static boolean isHighTier(ItemStack stack) {
        // used only when stack lacks owner but may still be high tier via activation path
        return false;
    }

    public static void appendOwnershipTooltip(ItemStack stack, java.util.List<net.minecraft.network.chat.Component> tooltip) {
        Optional<UUID> owner = ownerUuid(stack);
        if (owner.isPresent()) {
            String name = ownerName(stack);
            tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.owner",
                    name == null || name.isBlank() ? owner.get().toString() : name)
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.owner.none")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        int layer = refinementLayer(stack);
        if (layer > 0) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.refine_layer", layer, MAX_REFINEMENT_LAYER)
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
        }
        if (isSpiritAwakened(stack)) {
            tooltip.add(Component.translatable("tooltip.seeking_immortals.artifact.spirit.awake")
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
    }
}
