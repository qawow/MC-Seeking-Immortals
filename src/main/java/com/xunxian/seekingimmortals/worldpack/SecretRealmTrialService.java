package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Light secret-realm trial shell: builds a temporary multiblock arena near the
 * travel anchor for P0 realms and grants a one-time rare-drop proxy reward.
 * Wave48: core guardian encounter.
 * Wave460: mid patrol, layer hazards, richer layered loot, more realm shells.
 * Wave471: kill-gated rewards — mid/core loot + rare proxy unlock on combat kills.
 * Wave480: typed SummonedServitor combat shells (BEAST/PUPPET/GHOST/GENERIC).
 * Not a full custom-biome instanced worldgen pass.
 */
public final class SecretRealmTrialService {
    private static final String REWARD_ROOT = "seeking_immortals_secret_realm_trial_rewards";
    private static final String ENCOUNTER_ROOT = "seeking_immortals_secret_realm_encounters";
    private static final String MID_ENCOUNTER_ROOT = "seeking_immortals_secret_realm_mid_encounters";
    private static final String MID_CLEAR_ROOT = "seeking_immortals_secret_realm_mid_clear";
    private static final String CORE_CLEAR_ROOT = "seeking_immortals_secret_realm_core_clear";
    public static final String TRIAL_TAG = "seeking_immortals_trial";
    public static final String TRIAL_KIND = "Kind";
    public static final String TRIAL_REALM = "Realm";
    public static final String KIND_GUARDIAN = "guardian";
    public static final String KIND_PATROL = "patrol";

    private SecretRealmTrialService() {}

    public static void onEnter(ServerPlayer player, String realmId) {
        if (player == null || realmId == null || realmId.isBlank()) {
            return;
        }
        String id = realmId.trim().toLowerCase(Locale.ROOT);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (isShellTrialRealm(id)) {
            BlockPos center = player.blockPosition().above();
            int radius = shellRadiusFor(id);
            BlockPos midCenter = center.offset(radius + 4, 0, 0);
            BlockPos coreCenter = center.offset(0, 0, radius + 4);
            // Wave46 layered zones: outer / mid / core.
            buildTrialShell(level, center, radius);
            buildInnerChamber(level, midCenter, Math.max(2, radius - 1));
            buildCoreSanctum(level, coreCenter, Math.max(2, radius - 1));
            // Wave471: outer loot free; mid/core chests sealed until kill gates.
            placeLootChest(level, center.offset(0, 0, 2), id, Layer.OUTER);
            placeSealedChest(level, midCenter, id, Layer.MID);
            placeSealedChest(level, coreCenter, id, Layer.CORE);
            // Wave460: mid patrol before core boss pressure.
            spawnMidPatrol(level, player, midCenter, id);
            // Wave48: one-time guardian encounter per realm for this player.
            spawnCoreEncounter(level, player, coreCenter, id);
            BossEncounterService.spawnIfNeeded(player, "core_" + id);
            applyLayerHazards(player, id);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 45, 0, false, true, true));
            if (id.contains("void") || id.contains("diyuan") || id.contains("asura")) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 20, 0, false, true, true));
            }
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_shell_ready", id));
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_multi_room", id));
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_layers", id, "outer/mid/core"));
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_kill_gates", id));
            ReputationService.add(player, "secret_realm_explorer", 2);
        }
        // Wave471: rare proxy no longer granted on enter — unlock on guardian kill.
    }

    private enum Layer { OUTER, MID, CORE }

    public static void onReturn(ServerPlayer player) {
        if (player == null) {
            return;
        }
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.SLOW_FALLING);
    }

    /**
     * Wave472: periodic mild hazard reapplication while a secret-realm trial is active.
     * Called from player tick roughly every 5 seconds.
     */
    public static void tickHazard(ServerPlayer player) {
        if (player == null || player.getAbilities().instabuild) {
            return;
        }
        String realmId = "";
        var optional = com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(player);
        if (optional.isPresent()) {
            realmId = optional.get().getWorldpackActiveSecretRealmId();
        }
        if (realmId == null || realmId.isBlank()) {
            return;
        }
        String id = realmId.trim().toLowerCase(Locale.ROOT);
        if (!isShellTrialRealm(id) && !id.contains("trial") && !id.contains("secret")) {
            // Still apply light pressure in any active secret realm.
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 6, 0, false, true, true));
            return;
        }
        // Mild reapplication (shorter than enter pulse).
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 6, 0, false, true, true));
        if (id.contains("diyuan") || id.contains("void") || id.contains("asura")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 0, false, true, true));
        }
        if (id.contains("yin") || id.contains("nether") || id.contains("ghost")) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 4, 0, false, true, true));
        }
        if (id.contains("demon") || id.contains("blood")) {
            // Avoid stacking long poison — short pulse only.
            if (!player.hasEffect(MobEffects.POISON)) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 3, 0, false, true, true));
            }
        }
    }

    private static boolean isShellTrialRealm(String id) {
        return id.contains("blood_forbidden")
                || id.contains("fallen_demon")
                || id.contains("void_palace")
                || id.contains("nether_river")
                || id.contains("diyuan")
                || id.contains("kunwu")
                || id.contains("mist_cave")
                || id.contains("asura")
                || id.contains("jiuxian")
                || id.contains("king_")
                || id.contains("chaotic")
                || id.contains("yinming")
                || id.contains("spirit_realm")
                || id.contains("huangfeng")
                || id.contains("trial");
    }

    private static void applyLayerHazards(ServerPlayer player, String realmId) {
        // Outer: mild pressure. Mid/core pressure is communicated as stronger effects.
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 25, 0, false, true, true));
        if (realmId.contains("diyuan") || realmId.contains("void") || realmId.contains("asura")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 20, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20 * 20, 0, false, true, true));
        }
        if (realmId.contains("yin") || realmId.contains("nether") || realmId.contains("ghost")) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 15, 0, false, true, true));
        }
        if (realmId.contains("demon") || realmId.contains("blood")) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 8, 0, false, true, true));
        }
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.trial_hazard", realmId));
    }

    private static int shellRadiusFor(String id) {
        if (id.contains("void") || id.contains("diyuan") || id.contains("asura")) {
            return 4;
        }
        if (id.contains("king_") || id.contains("kunwu")) {
            return 3;
        }
        return 3;
    }

    private static void buildTrialShell(ServerLevel level, BlockPos center, int radius) {
        int r = Math.max(2, Math.min(5, radius));
        // Outer ring of spirit ore + floor + corner pillars.
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                if (Math.abs(x) == r || Math.abs(z) == r) {
                    setIfAir(level, center.offset(x, -1, z), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
                } else {
                    setIfAir(level, center.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                }
            }
        }
        setIfAir(level, center, ModBlocks.TELEPORT_ARRAY_PEDESTAL.get().defaultBlockState());
        int[] corners = {-r, r};
        for (int x : corners) {
            for (int z : corners) {
                setIfAir(level, center.offset(x, 0, z), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
                setIfAir(level, center.offset(x, 1, z), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
            }
        }
        // Clear standing room.
        int clear = Math.max(1, r - 1);
        for (int y = 1; y <= 3; y++) {
            for (int x = -clear; x <= clear; x++) {
                for (int z = -clear; z <= clear; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private static void setIfAir(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, state, 3);
        }
    }

    private static void buildInnerChamber(ServerLevel level, BlockPos center, int radius) {
        buildTrialShell(level, center, radius);
        // Mark inner room floor with spirit gathering arrays as chamber identity.
        setIfAir(level, center.offset(0, -1, 0), ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState());
        setIfAir(level, center.offset(1, -1, 0), ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState());
        setIfAir(level, center.offset(-1, -1, 0), ModBlocks.SPIRIT_GATHERING_ARRAY.get().defaultBlockState());
    }

    private static void buildCoreSanctum(ServerLevel level, BlockPos center, int radius) {
        buildTrialShell(level, center, radius);
        // Core sanctum uses teleport pedestal + ore pillars as boss/core marker.
        setIfAir(level, center, ModBlocks.TELEPORT_ARRAY_PEDESTAL.get().defaultBlockState());
        setIfAir(level, center.offset(0, 1, 0), Blocks.AIR.defaultBlockState());
        setIfAir(level, center.offset(2, 0, 2), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
        setIfAir(level, center.offset(-2, 0, 2), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
        setIfAir(level, center.offset(2, 0, -2), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
        setIfAir(level, center.offset(-2, 0, -2), ModBlocks.SPIRIT_ORE.get().defaultBlockState());
    }

    private static void placeLootChest(ServerLevel level, BlockPos pos, String realmId) {
        placeLootChest(level, pos, realmId, Layer.OUTER);
    }

    private static void placeLootChest(ServerLevel level, BlockPos pos, String realmId, Layer layer) {
        BlockPos chestPos = pos.above();
        if (!level.getBlockState(chestPos).isAir() && !level.getBlockState(chestPos).canBeReplaced()) {
            return;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            fillChest(chest, realmId, layer, level.random.nextInt(4));
        }
    }

    private static void placeSealedChest(ServerLevel level, BlockPos pos, String realmId, Layer layer) {
        BlockPos chestPos = pos.above();
        if (!level.getBlockState(chestPos).isAir() && !level.getBlockState(chestPos).canBeReplaced()) {
            return;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        // Wave471: leave empty until kill gate; optional sealed marker item.
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.JADE_SLIP_BLANK.get(), 1));
            chest.getPersistentData().putString(TRIAL_TAG + "_layer", layer.name());
            chest.getPersistentData().putString(TRIAL_TAG + "_realm", realmId == null ? "" : realmId);
            chest.getPersistentData().putBoolean(TRIAL_TAG + "_sealed", true);
        }
    }

    private static void fillChest(net.minecraft.world.level.block.entity.ChestBlockEntity chest,
                                  String realmId, Layer layer, int shardJitter) {
        int shardBase = switch (layer) {
            case OUTER -> 2;
            case MID -> 4;
            case CORE -> 8;
        };
        chest.clearContent();
        chest.setItem(0, new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), shardBase + Math.max(0, shardJitter)));
        if (layer != Layer.OUTER) {
            chest.setItem(1, new ItemStack(proxyRewardItem(realmId), layer == Layer.CORE ? 2 : 1));
        }
        if (layer == Layer.CORE) {
            if (realmId.contains("void") || realmId.contains("palace") || realmId.contains("star")) {
                chest.setItem(2, new ItemStack(ModItems.VOID_CRYSTAL.get(), 1));
            } else if (realmId.contains("demon") || realmId.contains("blood")) {
                chest.setItem(2, new ItemStack(ModItems.DEMONIC_BLOOD_CORAL.get(), 1));
            } else if (realmId.contains("yin") || realmId.contains("nether") || realmId.contains("ghost")) {
                chest.setItem(2, new ItemStack(ModItems.SOUL_FRAGMENT.get(), 2));
            } else {
                chest.setItem(2, new ItemStack(ModItems.IMMORTAL_JADE.get(), 1));
            }
            chest.setItem(3, new ItemStack(ModItems.ALLIANCE_MERIT_TOKEN.get(), 1));
        } else if (layer == Layer.MID) {
            chest.setItem(2, new ItemStack(ModItems.JADE_SLIP_BLANK.get(), 1));
        }
        chest.getPersistentData().putBoolean(TRIAL_TAG + "_sealed", false);
    }

    private static void spawnMidPatrol(ServerLevel level, ServerPlayer player, BlockPos midCenter, String realmId) {
        CompoundTag root = player.getPersistentData().getCompound(MID_ENCOUNTER_ROOT).copy();
        if (root.getBoolean(realmId)) {
            return;
        }
        // Wave480: typed GeckoLib combat shells instead of bare vanilla zombies/skeletons.
        SummonedServitorEntity.Archetype archetype = TrialCombatShellService.archetypeFor(realmId);
        int count = realmId.contains("king") || realmId.contains("void") ? 3 : 2;
        for (int i = 0; i < count; i++) {
            BlockPos spawn = midCenter.offset((i % 2 == 0 ? 2 : -2), 1, (i < 2 ? 1 : -1));
            Mob patrol = TrialCombatShellService.spawnHostile(
                    level, spawn, player.getYRot(), "patrol_" + realmId, 24.0D, 4.0D, archetype);
            if (patrol == null) {
                continue;
            }
            patrol.setCustomName(Component.translatable("entity.seeking_immortals.trial_patrol.name", realmId));
            patrol.setCustomNameVisible(true);
            patrol.setTarget(player);
            tagTrial(patrol, KIND_PATROL, realmId);
        }
        root.putBoolean(realmId, true);
        player.getPersistentData().put(MID_ENCOUNTER_ROOT, root);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.trial_mid_patrol", realmId, count));
    }

    /**
     * Wave48 encounter depth: spawn 1 guardian + 1-2 adds at the core sanctum once per realm.
     * Wave480: typed SummonedServitor combat shells (BEAST/PUPPET/GHOST/GENERIC) with GeckoLib textures.
     */
    private static void spawnCoreEncounter(ServerLevel level, ServerPlayer player, BlockPos coreCenter, String realmId) {
        CompoundTag root = player.getPersistentData().getCompound(ENCOUNTER_ROOT).copy();
        if (root.getBoolean(realmId)) {
            return;
        }
        SummonedServitorEntity.Archetype archetype = TrialCombatShellService.archetypeFor(realmId);
        double[] stats = guardianStatsFor(realmId);
        BlockPos spawn = coreCenter.offset(0, 1, 0);
        Mob guardian = TrialCombatShellService.spawnHostile(
                level, spawn, player.getYRot(), "guardian_" + realmId, stats[0], stats[1], archetype);
        if (guardian == null) {
            return;
        }
        guardian.setCustomName(Component.translatable("entity.seeking_immortals.trial_guardian.name", realmId));
        guardian.setCustomNameVisible(true);
        guardian.setTarget(player);
        tagTrial(guardian, KIND_GUARDIAN, realmId);
        int adds = realmId.contains("king") || realmId.contains("void") || realmId.contains("asura") ? 2 : 1;
        for (int i = 0; i < adds; i++) {
            BlockPos addPos = coreCenter.offset((i == 0 ? 2 : -2), 1, (i == 0 ? 1 : -1));
            Mob add = TrialCombatShellService.spawnHostile(
                    level, addPos, player.getYRot(), "add_" + realmId, 22.0D, 3.5D, archetype);
            if (add == null) {
                continue;
            }
            add.setCustomName(Component.translatable("entity.seeking_immortals.trial_patrol.name", realmId));
            add.setCustomNameVisible(true);
            add.setTarget(player);
            tagTrial(add, KIND_PATROL, realmId);
        }
        root.putBoolean(realmId, true);
        player.getPersistentData().put(ENCOUNTER_ROOT, root);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.trial_encounter", realmId, guardian.getName().getString(), adds));
        ReputationService.add(player, "secret_realm_explorer", 1);
    }

    public static void tagTrial(Mob mob, String kind, String realmId) {
        if (mob == null) {
            return;
        }
        CompoundTag tag = mob.getPersistentData().getCompound(TRIAL_TAG).copy();
        tag.putString(TRIAL_KIND, kind == null ? "" : kind);
        tag.putString(TRIAL_REALM, realmId == null ? "" : realmId.trim().toLowerCase(Locale.ROOT));
        mob.getPersistentData().put(TRIAL_TAG, tag);
    }

    public static boolean isTrialMob(Mob mob) {
        return mob != null && mob.getPersistentData().contains(TRIAL_TAG);
    }

    public static String trialKind(Mob mob) {
        if (!isTrialMob(mob)) {
            return "";
        }
        return mob.getPersistentData().getCompound(TRIAL_TAG).getString(TRIAL_KIND);
    }

    public static String trialRealm(Mob mob) {
        if (!isTrialMob(mob)) {
            return "";
        }
        return mob.getPersistentData().getCompound(TRIAL_TAG).getString(TRIAL_REALM);
    }

    /**
     * Wave471: combat kill gates for mid/core unlocks.
     */
    public static void onTrialMobKilled(ServerPlayer killer, Mob mob) {
        if (killer == null || mob == null || !isTrialMob(mob)) {
            return;
        }
        String kind = trialKind(mob);
        String realmId = trialRealm(mob);
        if (realmId.isBlank()) {
            return;
        }
        if (KIND_PATROL.equals(kind)) {
            unlockMid(killer, realmId, mob.blockPosition());
        } else if (KIND_GUARDIAN.equals(kind)) {
            unlockCore(killer, realmId, mob.blockPosition());
        }
    }

    private static void unlockMid(ServerPlayer player, String realmId, BlockPos near) {
        CompoundTag root = player.getPersistentData().getCompound(MID_CLEAR_ROOT).copy();
        if (root.getBoolean(realmId)) {
            return;
        }
        root.putBoolean(realmId, true);
        player.getPersistentData().put(MID_CLEAR_ROOT, root);
        fillNearbySealedChest(player, near, realmId, Layer.MID);
        ItemStack bonus = new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), 4);
        if (!player.getInventory().add(bonus.copy())) {
            player.drop(bonus.copy(), false);
        }
        ReputationService.add(player, "secret_realm_explorer", 1);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.trial_mid_clear", realmId));
    }

    private static void unlockCore(ServerPlayer player, String realmId, BlockPos near) {
        CompoundTag root = player.getPersistentData().getCompound(CORE_CLEAR_ROOT).copy();
        if (!root.getBoolean(realmId)) {
            root.putBoolean(realmId, true);
            player.getPersistentData().put(CORE_CLEAR_ROOT, root);
            fillNearbySealedChest(player, near, realmId, Layer.CORE);
            ReputationService.add(player, "secret_realm_explorer", 2);
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_core_clear", realmId));
        }
        grantOneTimeRareDropProxy(player, realmId);
    }

    private static void fillNearbySealedChest(ServerPlayer player, BlockPos near, String realmId, Layer layer) {
        if (!(player.level() instanceof ServerLevel level) || near == null) {
            return;
        }
        int r = 8;
        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 3; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = near.offset(x, y, z);
                    if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) {
                        continue;
                    }
                    CompoundTag data = chest.getPersistentData();
                    if (!data.getBoolean(TRIAL_TAG + "_sealed")) {
                        continue;
                    }
                    String chestRealm = data.getString(TRIAL_TAG + "_realm");
                    String chestLayer = data.getString(TRIAL_TAG + "_layer");
                    if (!realmId.equals(chestRealm) || !layer.name().equals(chestLayer)) {
                        continue;
                    }
                    fillChest(chest, realmId, layer, level.random.nextInt(4));
                    chest.setChanged();
                    return;
                }
            }
        }
        // Fallback: grant layer loot directly if sealed chest not found.
        ItemStack stack = new ItemStack(proxyRewardItem(realmId), layer == Layer.CORE ? 2 : 1);
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    /** Wave480: health/damage pairs for typed guardian shells. */
    private static double[] guardianStatsFor(String realmId) {
        double health = 40.0D;
        double damage = 6.0D;
        String id = realmId == null ? "" : realmId.toLowerCase(Locale.ROOT);
        if (id.contains("void") || id.contains("asura") || id.contains("king")) {
            health = 70.0D;
            damage = 9.0D;
        } else if (id.contains("diyuan") || id.contains("demon") || id.contains("kunwu")) {
            health = 55.0D;
            damage = 7.5D;
        }
        return new double[]{health, damage};
    }

    public static boolean hasEncountered(ServerPlayer player, String realmId) {
        if (player == null || realmId == null) {
            return false;
        }
        return player.getPersistentData().getCompound(ENCOUNTER_ROOT)
                .getBoolean(realmId.trim().toLowerCase(Locale.ROOT));
    }

    private static void grantOneTimeRareDropProxy(ServerPlayer player, String realmId) {
        CompoundTag root = player.getPersistentData().getCompound(REWARD_ROOT).copy();
        if (root.getBoolean(realmId)) {
            return;
        }
        Item reward = proxyRewardItem(realmId);
        ItemStack stack = new ItemStack(reward, 1);
        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
        root.putBoolean(realmId, true);
        player.getPersistentData().put(REWARD_ROOT, root);

        Optional<TextMaterialCatalogService.SecretRealmFlavor> flavor =
                TextMaterialCatalogService.builtin().findFlavor(realmId);
        List<String> rare = flavor.map(TextMaterialCatalogService.SecretRealmFlavor::rareDrops).orElse(List.of());
        if (!rare.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_rare_proxy",
                    String.join(", ", rare), stack.getHoverName()));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_rare_proxy_generic",
                    stack.getHoverName()));
        }
    }

    private static Item proxyRewardItem(String realmId) {
        String id = realmId == null ? "" : realmId.toLowerCase(Locale.ROOT);
        if (id.contains("blood") || id.contains("demon")) {
            return ModItems.DEMONIC_BLOOD_CORAL.get();
        }
        if (id.contains("void") || id.contains("star")) {
            return ModItems.VOID_CRYSTAL.get();
        }
        if (id.contains("yin") || id.contains("ghost")) {
            return ModItems.YIN_STONE.get();
        }
        return ModItems.SPIRIT_STONE_SHARD.get();
    }
}
