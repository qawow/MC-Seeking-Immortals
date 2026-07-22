package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.catalog.TextMaterialCatalogService;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.registry.ModBlocks;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
    private static final String ENCOUNTER_ROOT = "seeking_immortals_secret_realm_encounters";
    private static final String MID_ENCOUNTER_ROOT = "seeking_immortals_secret_realm_mid_encounters";
    public static final String TRIAL_TAG = "seeking_immortals_trial";
    public static final String TRIAL_KIND = "Kind";
    public static final String TRIAL_REALM = "Realm";
    public static final String KIND_GUARDIAN = "guardian";
    public static final String KIND_PATROL = "patrol";
    private static final String ENCOUNTER_OUTER = "trial:outer";
    private static final String ENCOUNTER_MID = "trial:mid";
    private static final String ENCOUNTER_CORE = "trial:core";

    private SecretRealmTrialService() {}

    public static void onEnter(ServerPlayer player, String realmId) {
        if (player == null || realmId == null || realmId.isBlank()) {
            return;
        }
        String id = realmId.trim().toLowerCase(Locale.ROOT);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Optional<SecretRealmProgressSavedData.Session> sessionOpt =
                SecretRealmSessionService.activeSession(player, id);
        if (sessionOpt.isEmpty()) {
            return;
        }
        SecretRealmProgressSavedData.Session session = sessionOpt.get();
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
            placeLootChest(level, center.offset(0, 0, 2), player, session, id, Layer.OUTER);
            placeSealedChest(level, midCenter, player, session, id, Layer.MID);
            placeSealedChest(level, coreCenter, player, session, id, Layer.CORE);
            // Wave460: mid patrol before core boss pressure.
            spawnMidPatrol(level, player, session, midCenter, id);
            // Wave48: one-time guardian encounter per realm for this player.
            spawnCoreEncounter(level, player, session, coreCenter, id);
            SecretRealmCatalogService.find(id).stream()
                    .flatMap(realm -> realm.bosses().stream())
                    .filter(BossEncounterService::isKnownBossId)
                    .findFirst()
                    .ifPresent(bossId -> BossEncounterService.spawnIfNeeded(player, bossId));
            applyLayerHazards(player, id);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 30, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 45, 0, false, true, true));
            if (id.contains("void") || id.contains("diyuan") || id.contains("asura")) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 20, 0, false, true, true));
            }
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_shell_ready", realmDisplay(id)));
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_multi_room", realmDisplay(id)));
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_layers", realmDisplay(id),
                    Component.literal("外层 / 中层 / 核心")));
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_kill_gates"));
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
        if (id == null || id.isBlank()) {
            return false;
        }
        // M09: any catalog deep-dive realm gets a layered trial shell.
        if (SecretRealmCatalogService.find(id).isPresent()) {
            return true;
        }
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
                || id.contains("trial")
                || id.contains("tomb")
                || id.contains("grotto")
                || id.contains("ruins")
                || id.contains("puppet")
                || id.contains("abyss");
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
                "message.seeking_immortals.worldpack.trial_hazard", realmDisplay(realmId)));
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

    private static void placeLootChest(ServerLevel level, BlockPos pos, ServerPlayer player,
                                       SecretRealmProgressSavedData.Session session,
                                       String realmId, Layer layer) {
        BlockPos chestPos = pos.above();
        if (!level.getBlockState(chestPos).isAir() && !level.getBlockState(chestPos).canBeReplaced()) {
            return;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            SecretRealmRewardService.initializeChest(
                    chest, player, session, realmId, encounterId(layer), false,
                    rewardStacks(realmId, layer, level.random.nextInt(4)));
        }
    }

    private static void placeSealedChest(ServerLevel level, BlockPos pos, ServerPlayer player,
                                         SecretRealmProgressSavedData.Session session,
                                         String realmId, Layer layer) {
        BlockPos chestPos = pos.above();
        if (!level.getBlockState(chestPos).isAir() && !level.getBlockState(chestPos).canBeReplaced()) {
            return;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            SecretRealmRewardService.initializeChest(
                    chest, player, session, realmId, encounterId(layer), true, List.of());
        }
    }

    private static List<ItemStack> rewardStacks(String realmId, Layer layer, int shardJitter) {
        int shardBase = switch (layer) {
            case OUTER -> 2;
            case MID -> 4;
            case CORE -> 8;
        };
        java.util.ArrayList<ItemStack> rewards = new java.util.ArrayList<>();
        rewards.add(new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), shardBase + Math.max(0, shardJitter)));
        if (layer != Layer.OUTER) {
            rewards.add(new ItemStack(proxyRewardItem(realmId), layer == Layer.CORE ? 2 : 1));
        }
        if (layer == Layer.CORE) {
            if (realmId.contains("void") || realmId.contains("palace") || realmId.contains("star")) {
                rewards.add(new ItemStack(ModItems.VOID_CRYSTAL.get(), 1));
            } else if (realmId.contains("demon") || realmId.contains("blood")) {
                rewards.add(new ItemStack(ModItems.DEMONIC_BLOOD_CORAL.get(), 1));
            } else if (realmId.contains("yin") || realmId.contains("nether") || realmId.contains("ghost")) {
                rewards.add(new ItemStack(ModItems.SOUL_FRAGMENT.get(), 2));
            } else {
                rewards.add(new ItemStack(ModItems.IMMORTAL_JADE.get(), 1));
            }
            rewards.add(new ItemStack(ModItems.ALLIANCE_MERIT_TOKEN.get(), 1));
        } else if (layer == Layer.MID) {
            rewards.add(new ItemStack(ModItems.JADE_SLIP_BLANK.get(), 1));
        }
        return List.copyOf(rewards);
    }

    private static void spawnMidPatrol(ServerLevel level, ServerPlayer player,
                                       SecretRealmProgressSavedData.Session session,
                                       BlockPos midCenter, String realmId) {
        CompoundTag root = player.getPersistentData().getCompound(MID_ENCOUNTER_ROOT).copy();
        String sessionKey = sessionKey(session, realmId, ENCOUNTER_MID);
        if (root.getBoolean(sessionKey)) {
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
            patrol.setCustomName(Component.translatable("entity.seeking_immortals.trial_patrol.name",
                    realmDisplay(realmId)));
            patrol.setCustomNameVisible(true);
            patrol.setTarget(player);
            tagTrial(patrol, player, session, KIND_PATROL, realmId, ENCOUNTER_MID);
        }
        root.putBoolean(sessionKey, true);
        player.getPersistentData().put(MID_ENCOUNTER_ROOT, root);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.trial_mid_patrol", realmDisplay(realmId), count));
    }

    /**
     * Wave48 encounter depth: spawn 1 guardian + 1-2 adds at the core sanctum once per realm.
     * Wave480: typed SummonedServitor combat shells (BEAST/PUPPET/GHOST/GENERIC) with GeckoLib textures.
     */
    private static void spawnCoreEncounter(ServerLevel level, ServerPlayer player,
                                           SecretRealmProgressSavedData.Session session,
                                           BlockPos coreCenter, String realmId) {
        CompoundTag root = player.getPersistentData().getCompound(ENCOUNTER_ROOT).copy();
        String sessionKey = sessionKey(session, realmId, ENCOUNTER_CORE);
        if (root.getBoolean(sessionKey)) {
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
        guardian.setCustomName(Component.translatable("entity.seeking_immortals.trial_guardian.name",
                realmDisplay(realmId)));
        guardian.setCustomNameVisible(true);
        guardian.setTarget(player);
        tagTrial(guardian, player, session, KIND_GUARDIAN, realmId, ENCOUNTER_CORE);
        int adds = realmId.contains("king") || realmId.contains("void") || realmId.contains("asura") ? 2 : 1;
        for (int i = 0; i < adds; i++) {
            BlockPos addPos = coreCenter.offset((i == 0 ? 2 : -2), 1, (i == 0 ? 1 : -1));
            Mob add = TrialCombatShellService.spawnHostile(
                    level, addPos, player.getYRot(), "add_" + realmId, 22.0D, 3.5D, archetype);
            if (add == null) {
                continue;
            }
            add.setCustomName(Component.translatable("entity.seeking_immortals.trial_patrol.name",
                    realmDisplay(realmId)));
            add.setCustomNameVisible(true);
            add.setTarget(player);
            tagTrial(add, player, session, KIND_PATROL, realmId, ENCOUNTER_MID);
        }
        root.putBoolean(sessionKey, true);
        player.getPersistentData().put(ENCOUNTER_ROOT, root);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.trial_encounter", realmDisplay(realmId),
                guardian.getName(), adds));
        ReputationService.add(player, "secret_realm_explorer", 1);
    }

    private static Component realmDisplay(String realmId) {
        return WorldpackDataService.builtin().findSecretRealm(realmId)
                .map(realm -> !realm.displayZh().isBlank() ? realm.displayZh() : realm.displayEn())
                .filter(PlayerDisplayText::isSafe)
                .map(value -> Component.literal(value.trim()))
                .orElseGet(() -> Component.literal("秘境试炼"));
    }

    public static void tagTrial(Mob mob, ServerPlayer owner,
                                SecretRealmProgressSavedData.Session session,
                                String kind, String realmId, String encounterId) {
        if (mob == null || owner == null || session == null) {
            return;
        }
        CompoundTag tag = mob.getPersistentData().getCompound(TRIAL_TAG).copy();
        tag.putString(TRIAL_KIND, kind == null ? "" : kind);
        tag.putString(TRIAL_REALM, realmId == null ? "" : realmId.trim().toLowerCase(Locale.ROOT));
        SecretRealmSessionService.bindEncounter(tag, owner, session, realmId, encounterId);
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
     * Security: validates that the killer is the session owner via
     * {@link SecretRealmSessionService#claimEncounter}, which internally calls
     * matchesEncounter to verify killer UUID matches the mob's bound OWNER_UUID.
     * All reward delivery (unlockMid, unlockCore, direct bonuses) occurs
     * only after this validation succeeds.
     *
     * @return false if killer is not the session owner or mob is not properly bound
     */
    public static boolean onTrialMobKilled(ServerPlayer killer, Mob mob) {
        if (killer == null || mob == null || !isTrialMob(mob)) {
            return false;
        }
        CompoundTag trial = mob.getPersistentData().getCompound(TRIAL_TAG);
        // Defense: claimEncounter validates owner UUID before allowing any rewards
        if (!SecretRealmSessionService.claimEncounter(killer, trial)) {
            return false;
        }
        String kind = trialKind(mob);
        String realmId = trialRealm(mob);
        if (realmId.isBlank()) {
            return false;
        }
        if (KIND_PATROL.equals(kind)) {
            unlockMid(killer, realmId, mob.blockPosition());
        } else if (KIND_GUARDIAN.equals(kind)) {
            unlockCore(killer, realmId, mob.blockPosition());
        } else {
            return false;
        }
        return true;
    }

    private static void unlockMid(ServerPlayer player, String realmId, BlockPos near) {
        fillNearbySealedChest(player, near, realmId, Layer.MID);
        ItemStack bonus = new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), 4);
        InventoryDeliveryService.giveOrEnqueue(player, bonus, "secret_realm_trial");
        ReputationService.add(player, "secret_realm_explorer", 1);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.trial_mid_clear", realmDisplay(realmId)));
    }

    private static void unlockCore(ServerPlayer player, String realmId, BlockPos near) {
        fillNearbySealedChest(player, near, realmId, Layer.CORE);
        ReputationService.add(player, "secret_realm_explorer", 2);
        player.sendSystemMessage(Component.translatable(
                "message.seeking_immortals.worldpack.trial_core_clear", realmDisplay(realmId)));
        // M09: publish clear hook when core guardian falls (no catalog boss path).
        SecretRealmSessionService.onRealmCleared(realmId, player);
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
                    List<ItemStack> rewards = rewardStacks(realmId, layer, level.random.nextInt(4));
                    if (SecretRealmRewardService.unlock(
                            chest, player, realmId, encounterId(layer), rewards)) {
                        return;
                    }
                }
            }
        }
        rewardStacks(realmId, layer, level.random.nextInt(4))
                .forEach(stack -> InventoryDeliveryService.giveOrEnqueue(player, stack, "secret_realm_trial"));
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
        Optional<SecretRealmProgressSavedData.Session> session =
                SecretRealmSessionService.activeSession(player, realmId);
        if (session.isEmpty()) {
            return false;
        }
        return player.getPersistentData().getCompound(ENCOUNTER_ROOT)
                .getBoolean(sessionKey(session.get(), realmId, ENCOUNTER_CORE));
    }

    private static void grantOneTimeRareDropProxy(ServerPlayer player, String realmId) {
        if (!SecretRealmProgressSavedData.get(player).claimUniqueDrop(
                player.getUUID(), "trial_rare_proxy:" + realmId)) {
            return;
        }
        Item reward = proxyRewardItem(realmId);
        ItemStack stack = new ItemStack(reward, 1);
        InventoryDeliveryService.giveOrEnqueue(player, stack, "secret_realm_trial");

        Optional<TextMaterialCatalogService.SecretRealmFlavor> flavor =
                TextMaterialCatalogService.builtin().findFlavor(realmId);
        List<String> rare = flavor.map(TextMaterialCatalogService.SecretRealmFlavor::rareDrops).orElse(List.of());
        if (!rare.isEmpty()) {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_rare_proxy",
                    rareDropsDisplay(rare), stack.getHoverName()));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.seeking_immortals.worldpack.trial_rare_proxy_generic",
                    stack.getHoverName()));
        }
    }

    private static Component rareDropsDisplay(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Component.translatable("text.seeking_immortals.unknown_item");
        }
        MutableComponent joined = Component.empty();
        boolean first = true;
        for (String itemId : itemIds) {
            if (!first) {
                joined.append(Component.literal("、"));
            }
            joined.append(PlayerDisplayText.itemName(itemId));
            first = false;
        }
        return joined;
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

    private static String encounterId(Layer layer) {
        return switch (layer) {
            case OUTER -> ENCOUNTER_OUTER;
            case MID -> ENCOUNTER_MID;
            case CORE -> ENCOUNTER_CORE;
        };
    }

    private static String sessionKey(SecretRealmProgressSavedData.Session session,
                                     String realmId, String encounterId) {
        return session.sessionId() + "|" + realmId.trim().toLowerCase(Locale.ROOT) + "|" + encounterId;
    }
}
