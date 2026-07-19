package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.beast.BestiaryUnlockService;
import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
import com.xunxian.seekingimmortals.item.InventoryDeliveryService;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.Locale;

/**
 * Named boss encounter spawner for secret-realm cores / high events (Wave52/460).
 * Wave460: stronger scaling + nearby boss-chest reward cache.
 * Wave471: kill-gated boss cache (no pre-placed loot chest).
 * Wave480: typed SummonedServitor combat shells instead of vanilla ravager/wither skeleton.
 */
public final class BossEncounterService {
    private static final String TAG = "seeking_immortals_boss_spawned";
    private static final String KILL_ROOT = "seeking_immortals_boss_kills";
    public static final String BOSS_TAG = "seeking_immortals_boss";
    public static final String BOSS_ID = "BossId";

    private BossEncounterService() {}

    public static boolean spawnIfNeeded(ServerPlayer player, String bossId) {
        if (player == null || bossId == null || bossId.isBlank()) {
            return false;
        }
        String id = bossId.trim().toLowerCase(Locale.ROOT);
        if (!isKnownBossId(id)) {
            return false;
        }
        SecretRealmProgressSavedData.Session session =
                SecretRealmSessionService.activeSession(player, "").orElse(null);
        if (session == null) {
            return false;
        }
        String key = TAG + "_" + session.sessionId() + "_" + id;
        if (player.getPersistentData().getBoolean(key)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        // M10: prefer catalog boss (tier-scaled + phase skills) when known.
        if (com.xunxian.seekingimmortals.beast.BeastBossService.find(id).isPresent()) {
            Mob catalogBoss = com.xunxian.seekingimmortals.beast.BeastBossService.spawnCatalogBoss(player, id);
            if (catalogBoss != null) {
                bindBoss(catalogBoss, player, session, id);
                player.getPersistentData().putBoolean(key, true);
                ReputationService.add(player, "secret_realm_explorer", 1);
                return true;
            }
        }
        SummonedServitorEntity.Archetype archetype = TrialCombatShellService.archetypeFor(id);
        double health = 80.0D;
        double damage = 10.0D;
        if (id.contains("void") || id.contains("asura") || id.contains("king")) {
            health = 120.0D;
            damage = 14.0D;
        } else if (id.contains("diyuan") || id.contains("demon")) {
            health = 100.0D;
            damage = 12.0D;
        }
        BlockPos pos = player.blockPosition().offset(2, 0, 2);
        Mob boss = TrialCombatShellService.spawnHostile(
                level, pos, player.getYRot(), "boss_" + id, health, damage, archetype);
        if (boss == null) {
            return false;
        }
        boss.setCustomName(Component.translatable("entity.seeking_immortals.boss.name", id));
        boss.setCustomNameVisible(true);
        boss.setTarget(player);
        bindBoss(boss, player, session, id);
        player.getPersistentData().putBoolean(key, true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.boss.spawned", id), true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.boss.kill_gate_hint", id), false);
        ReputationService.add(player, "secret_realm_explorer", 1);
        return true;
    }

    public static boolean isKnownBossId(String bossId) {
        if (bossId == null || bossId.isBlank()) {
            return false;
        }
        String id = bossId.trim().toLowerCase(Locale.ROOT);
        return BossLootService.find(id).isPresent()
                || com.xunxian.seekingimmortals.beast.BeastBossService.find(id).isPresent();
    }

    public static boolean isBossMob(Mob mob) {
        return mob != null && mob.getPersistentData().contains(BOSS_TAG);
    }

    public static String bossIdOf(Mob mob) {
        if (!isBossMob(mob)) {
            return "";
        }
        return mob.getPersistentData().getCompound(BOSS_TAG).getString(BOSS_ID);
    }

    public static boolean onBossKilled(ServerPlayer killer, Mob boss) {
        if (killer == null || boss == null || !isBossMob(boss)) {
            return false;
        }
        CompoundTag bossTag = boss.getPersistentData().getCompound(BOSS_TAG);
        if (!SecretRealmSessionService.claimEncounter(killer, bossTag)) {
            return false;
        }
        String bossId = bossIdOf(boss);
        if (bossId.isBlank()) {
            return false;
        }
        CompoundTag kills = killer.getPersistentData().getCompound(KILL_ROOT).copy();
        boolean firstKill = !kills.getBoolean(bossId);
        if (firstKill) {
            kills.putBoolean(bossId, true);
            killer.getPersistentData().put(KILL_ROOT, kills);
        }
        // M09: first/repeat loot from boss_loot_runtime (unique never on repeat).
        boolean firstClear = firstKill;
        String realmId = SecretRealmSessionService.boundRealmId(bossTag);
        if (!realmId.isBlank()) {
            firstClear = !SecretRealmProgressSavedData.get(killer).hasFirstCleared(killer.getUUID(), realmId);
        }
        int granted = BossLootService.grantBossLoot(killer, bossId, firstClear, killer.getRandom());
        SecretRealmProgressSavedData.Session session =
                SecretRealmSessionService.activeSession(killer, realmId).orElse(null);
        if (killer.level() instanceof ServerLevel level && session != null) {
            placeBossCache(level, boss.blockPosition().above(), killer, session, realmId, bossId, firstClear);
        }
        ItemStack bonus = new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), firstClear ? 8 : 3);
        InventoryDeliveryService.giveOrEnqueue(killer, bonus, "boss_encounter");
        ReputationService.add(killer, "secret_realm_explorer", firstClear ? 3 : 1);
        BestiaryUnlockService.unlock(killer, bossId, BestiaryUnlockService.UnlockKind.KILL);
        killer.displayClientMessage(Component.translatable("message.seeking_immortals.boss.defeated", bossId), true);
        if (granted > 0) {
            killer.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.boss.loot_granted", bossId, granted, firstClear), false);
        }
        // Core boss defeat clears the secret realm for M11/M06 hooks (session-latched).
        if (!realmId.isBlank()) {
            SecretRealmSessionService.onRealmCleared(realmId, killer);
        }
        return true;
    }

    private static void bindBoss(Mob boss, ServerPlayer player,
                                 SecretRealmProgressSavedData.Session session, String bossId) {
        CompoundTag tag = boss.getPersistentData().getCompound(BOSS_TAG).copy();
        tag.putString(BOSS_ID, bossId);
        SecretRealmSessionService.bindEncounter(
                tag, player, session, session.realmId(), "boss:" + bossId);
        boss.getPersistentData().put(BOSS_TAG, tag);
    }

    private static void placeBossCache(ServerLevel level, BlockPos pos, ServerPlayer player,
                                       SecretRealmProgressSavedData.Session session,
                                       String realmId, String bossId, boolean firstClear) {
        BlockPos chestPos = pos;
        if (!level.getBlockState(chestPos).isAir() && !level.getBlockState(chestPos).canBeReplaced()) {
            chestPos = pos.above();
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            java.util.ArrayList<ItemStack> rewards = new java.util.ArrayList<>();
            rewards.add(new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), firstClear ? 12 : 4));
            if (firstClear) {
                rewards.add(new ItemStack(ModItems.IMMORTAL_JADE.get(), 1));
            }
            if (bossId.contains("void") || bossId.contains("asura")) {
                if (firstClear) {
                    rewards.add(new ItemStack(ModItems.VOID_CRYSTAL.get(), 1));
                } else {
                    rewards.add(new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), 2));
                }
            } else if (bossId.contains("demon") || bossId.contains("blood")) {
                rewards.add(new ItemStack(ModItems.DEMONIC_BLOOD_CORAL.get(), firstClear ? 2 : 1));
            } else {
                rewards.add(new ItemStack(ModItems.ALLIANCE_MERIT_TOKEN.get(), firstClear ? 2 : 1));
            }
            SecretRealmRewardService.initializeChest(
                    chest, player, session, realmId, "boss:" + bossId, false, rewards);
        }
    }
}
