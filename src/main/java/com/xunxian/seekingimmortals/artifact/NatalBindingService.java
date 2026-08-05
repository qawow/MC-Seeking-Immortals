package com.xunxian.seekingimmortals.artifact;

import com.xunxian.seekingimmortals.cultivation.CultivationHelper;
import com.xunxian.seekingimmortals.cultivation.Realm;
import com.xunxian.seekingimmortals.item.ArtifactCatalogItem;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Natal artifact binding (Wave51 / Wave456 / Wave459; M-B instance binding).
 *
 * <p>M-B: a natal artifact is one specific <em>instance</em>, not an artifact id. Schema 1 stored
 * only {@code ArtifactId + Growth}, so a second copy of the same id inherited every natal benefit
 * — cooldown cut, spiritual cost cut, integrity cut and growth — without ever being bound. Schema 2
 * records the bound instance UUID on both the player root and the target stack, and every benefit
 * site resolves that exact instance.</p>
 *
 * <p>Binding is a deliberate two-hand ritual (embryo + already-claimed artifact + 结丹), never a
 * side effect of claiming ownership.</p>
 */
public final class NatalBindingService {
    private static final String ROOT = "seeking_immortals_natal_binding";
    private static final String KEY_ID = "ArtifactId";
    private static final String KEY_GROWTH = "Growth";
    static final String KEY_INSTANCE = "InstanceUuid";
    static final String KEY_SCHEMA = "SchemaVersion";
    /** Schema 1 was id-only; instance binding is schema 2. */
    public static final int SCHEMA_VERSION = 2;
    public static final String STACK_BOUND = "SeekingImmortalsNatalBound";
    public static final String STACK_GROWTH = "SeekingImmortalsNatalGrowth";
    public static final String STACK_INSTANCE = "SeekingImmortalsNatalInstance";
    /** The authored ritual component: 结丹后择一飞剑为本命. */
    public static final String EMBRYO_ITEM_ID = "natal_sword_embryo";

    /** Outcome of adopting a schema-1 binding into schema 2. */
    public enum MigrationResult {
        NOT_LEGACY,
        MIGRATED,
        AMBIGUOUS,
        NO_CANDIDATE
    }

    private NatalBindingService() {}

    public static String boundId(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        return player.getPersistentData().getCompound(ROOT).getString(KEY_ID);
    }

    public static String boundInstance(ServerPlayer player) {
        if (player == null) {
            return "";
        }
        return player.getPersistentData().getCompound(ROOT).getString(KEY_INSTANCE);
    }

    public static int growth(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return player.getPersistentData().getCompound(ROOT).getInt(KEY_GROWTH);
    }

    public static int growthFromStack(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return 0;
        }
        return Math.max(0, stack.getTag().getInt(STACK_GROWTH));
    }

    public static String instanceOf(ItemStack stack) {
        if (stack == null || !stack.hasTag()) {
            return "";
        }
        return stack.getTag().getString(STACK_INSTANCE);
    }

    /**
     * A schema-1 binding: an artifact id with no instance recorded. Such a save cannot tell two
     * copies apart, so it must be migrated (or diagnosed) before instance benefits apply.
     */
    public static boolean isLegacyBinding(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        return !root.getString(KEY_ID).isBlank()
                && (root.getString(KEY_INSTANCE).isBlank() || root.getInt(KEY_SCHEMA) < SCHEMA_VERSION);
    }

    /**
     * The single authority for "is this exact stack the player's natal artifact".
     * Fails closed: a blank instance id never matches, the artifact id must still agree, and a
     * duplicated NBT copy is rejected because ownership must also resolve to this player.
     */
    public static boolean isBoundInstance(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()
                || !(stack.getItem() instanceof ArtifactCatalogItem catalogItem)) {
            return false;
        }
        String boundInstance = boundInstance(player);
        String stackInstance = instanceOf(stack);
        if (boundInstance.isBlank() || stackInstance.isBlank() || !boundInstance.equals(stackInstance)) {
            return false;
        }
        if (!catalogItem.artifactId().equals(boundId(player))) {
            return false;
        }
        // A copied instance tag must not work in someone else's hands.
        return ArtifactOwnershipService.ownerUuid(stack)
                .map(owner -> owner.equals(player.getUUID()))
                .orElse(false);
    }

    /** Does the player actually carry the bound instance of {@code artifactId} right now. */
    public static boolean holdsBoundInstanceOf(ServerPlayer player, String artifactId) {
        if (player == null || artifactId == null || !artifactId.equals(boundId(player))) {
            return false;
        }
        return findBoundInstance(player).isPresent();
    }

    private static java.util.Optional<ItemStack> findBoundInstance(ServerPlayer player) {
        for (ItemStack stack : carried(player)) {
            if (isBoundInstance(player, stack)) {
                return java.util.Optional.of(stack);
            }
        }
        return java.util.Optional.empty();
    }

    private static List<ItemStack> carried(ServerPlayer player) {
        List<ItemStack> out = new ArrayList<>();
        out.addAll(player.getInventory().items);
        out.addAll(player.getInventory().offhand);
        out.addAll(player.getInventory().armor);
        return out;
    }

    /**
     * The authored two-hand ritual: hold the embryo in one hand and an already-claimed artifact in
     * the other, at 结丹 or above, then sneak-use. The embryo is consumed only after every
     * rejection path has returned, so a failed attempt never costs the component.
     */
    public static boolean bindWithEmbryo(ServerPlayer player, ItemStack embryo, ItemStack target) {
        if (player == null || embryo == null || target == null || embryo.isEmpty() || target.isEmpty()) {
            return false;
        }
        if (!(embryo.getItem() instanceof ArtifactCatalogItem embryoItem)
                || !EMBRYO_ITEM_ID.equals(embryoItem.artifactId())) {
            return false;
        }
        if (!(target.getItem() instanceof ArtifactCatalogItem targetItem)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.not_artifact"), true);
            return false;
        }
        boolean creative = player.getAbilities().instabuild;
        // 结丹门槛（natal_sword_embryo realm_min CORE_FORMATION）。
        boolean meetsRealm = creative || CultivationHelper.get(player)
                .map(cultivation -> cultivation.getRealm().ordinal() >= Realm.CORE_FORMATION.ordinal())
                .orElse(false);
        if (!meetsRealm) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.natal.realm_too_low",
                    Realm.CORE_FORMATION.getDisplayName()), true);
            return false;
        }
        // Only an artifact the player has already claimed may become their natal artifact.
        boolean owned = ArtifactOwnershipService.ownerUuid(target)
                .map(owner -> owner.equals(player.getUUID()))
                .orElse(false);
        if (!owned) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.natal.needs_claim", displayOf(targetItem.artifactId())), true);
            return false;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        boolean alreadyBound = !root.getString(KEY_INSTANCE).isBlank() || !root.getString(KEY_ID).isBlank();
        if (alreadyBound && !creative) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.already",
                    displayOf(root.getString(KEY_ID))), true);
            return false;
        }

        // Every gate has passed: mint the instance, stamp both sides, then spend the embryo.
        String instance = UUID.randomUUID().toString();
        int growth = Math.max(0, root.getInt(KEY_GROWTH));
        root.putString(KEY_ID, targetItem.artifactId());
        root.putString(KEY_INSTANCE, instance);
        root.putInt(KEY_SCHEMA, SCHEMA_VERSION);
        root.putInt(KEY_GROWTH, growth);
        player.getPersistentData().put(ROOT, root);
        stamp(target, instance, growth);
        if (!creative) {
            embryo.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.bound",
                displayOf(targetItem.artifactId())), true);
        return true;
    }

    /**
     * Adopts a schema-1 binding into schema 2 by locating the single carried artifact that matches
     * the recorded id. Several candidates stay {@link MigrationResult#AMBIGUOUS} rather than
     * guessing, because picking one for the player could silently pick the wrong sword.
     */
    public static MigrationResult migrateLegacyBinding(ServerPlayer player) {
        if (player == null || !isLegacyBinding(player)) {
            return MigrationResult.NOT_LEGACY;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        String boundId = root.getString(KEY_ID);
        List<ItemStack> candidates = new ArrayList<>();
        for (ItemStack stack : carried(player)) {
            if (stack.getItem() instanceof ArtifactCatalogItem catalogItem
                    && boundId.equals(catalogItem.artifactId())
                    && ArtifactOwnershipService.ownerUuid(stack)
                            .map(owner -> owner.equals(player.getUUID()))
                            .orElse(false)) {
                candidates.add(stack);
            }
        }
        if (candidates.isEmpty()) {
            return MigrationResult.NO_CANDIDATE;
        }
        if (candidates.size() != 1) {
            return MigrationResult.AMBIGUOUS;
        }
        String instance = UUID.randomUUID().toString();
        int growth = Math.max(0, root.getInt(KEY_GROWTH));
        root.putString(KEY_INSTANCE, instance);
        root.putInt(KEY_SCHEMA, SCHEMA_VERSION);
        root.putInt(KEY_GROWTH, growth);
        player.getPersistentData().put(ROOT, root);
        stamp(candidates.get(0), instance, growth);
        return MigrationResult.MIGRATED;
    }

    public static boolean grow(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        String id = root.getString(KEY_ID);
        if (id.isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.none"), true);
            return false;
        }
        int prev = root.getInt(KEY_GROWTH);
        int growth = Math.min(100, prev + 1);
        root.putInt(KEY_GROWTH, growth);
        player.getPersistentData().put(ROOT, root);
        mirrorGrowthToBoundInstance(player, growth);
        if (growth == 25 || growth == 50 || growth == 75 || growth == 100) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.natal.milestone", displayOf(id), growth), true);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.natal.grown",
                    displayOf(id), growth), true);
        }
        return true;
    }

    private static void stamp(ItemStack stack, String instance, int growth) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(STACK_BOUND, true);
        tag.putString(STACK_INSTANCE, instance);
        tag.putInt(STACK_GROWTH, growth);
    }

    /** Mirrors growth onto the bound instance only, wherever it is carried. */
    private static void mirrorGrowthToBoundInstance(ServerPlayer player, int growth) {
        findBoundInstance(player).ifPresent(stack -> stack.getOrCreateTag().putInt(STACK_GROWTH, growth));
    }

    private static Component displayOf(String id) {
        return ArtifactDataService.builtin().findArtifact(id)
                .filter(definition -> PlayerDisplayText.isSafe(definition.display()))
                .<Component>map(definition -> Component.literal(definition.display().trim()))
                .orElseGet(() -> PlayerDisplayText.itemName(id));
    }
}
