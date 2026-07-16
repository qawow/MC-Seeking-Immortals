package com.xunxian.seekingimmortals.worldpack;

import com.xunxian.seekingimmortals.entity.SummonedServitorEntity;
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
        String key = TAG + "_" + bossId;
        if (player.getPersistentData().getBoolean(key)) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        // M10: prefer catalog boss (tier-scaled + phase skills) when known.
        if (com.xunxian.seekingimmortals.beast.BeastBossService.find(bossId).isPresent()) {
            boolean catalog = com.xunxian.seekingimmortals.beast.BeastBossService.spawnCatalogBoss(player, bossId);
            if (catalog) {
                player.getPersistentData().putBoolean(key, true);
                ReputationService.add(player, "secret_realm_explorer", 1);
                return true;
            }
        }
        SummonedServitorEntity.Archetype archetype = TrialCombatShellService.archetypeFor(bossId);
        double health = 80.0D;
        double damage = 10.0D;
        if (bossId.contains("void") || bossId.contains("asura") || bossId.contains("king")) {
            health = 120.0D;
            damage = 14.0D;
        } else if (bossId.contains("diyuan") || bossId.contains("demon")) {
            health = 100.0D;
            damage = 12.0D;
        }
        BlockPos pos = player.blockPosition().offset(2, 0, 2);
        Mob boss = TrialCombatShellService.spawnHostile(
                level, pos, player.getYRot(), "boss_" + bossId, health, damage, archetype);
        if (boss == null) {
            return false;
        }
        boss.setCustomName(Component.translatable("entity.seeking_immortals.boss.name", bossId));
        boss.setCustomNameVisible(true);
        boss.setTarget(player);
        // Wave471: tag for kill-gated reward; do not pre-place cache.
        CompoundTag tag = boss.getPersistentData().getCompound(BOSS_TAG).copy();
        tag.putString(BOSS_ID, bossId);
        boss.getPersistentData().put(BOSS_TAG, tag);
        player.getPersistentData().putBoolean(key, true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.boss.spawned", bossId), true);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.boss.kill_gate_hint", bossId), false);
        ReputationService.add(player, "secret_realm_explorer", 1);
        return true;
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

    public static void onBossKilled(ServerPlayer killer, Mob boss) {
        if (killer == null || boss == null || !isBossMob(boss)) {
            return;
        }
        String bossId = bossIdOf(boss);
        if (bossId.isBlank()) {
            return;
        }
        CompoundTag kills = killer.getPersistentData().getCompound(KILL_ROOT).copy();
        boolean firstKill = !kills.getBoolean(bossId);
        if (firstKill) {
            kills.putBoolean(bossId, true);
            killer.getPersistentData().put(KILL_ROOT, kills);
        }
        // M09: first/repeat loot from boss_loot_runtime (unique never on repeat).
        boolean firstClear = firstKill;
        String realmId = resolveRealmId(killer, bossId);
        if (!realmId.isBlank()) {
            firstClear = !SecretRealmProgressSavedData.get(killer).hasFirstCleared(killer.getUUID(), realmId);
        }
        int granted = BossLootService.grantBossLoot(killer, bossId, firstClear, killer.getRandom());
        if (killer.level() instanceof ServerLevel level) {
            placeBossCache(level, boss.blockPosition().above(), bossId, firstClear);
        }
        ItemStack bonus = new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), firstClear ? 8 : 3);
        if (!killer.getInventory().add(bonus.copy())) {
            killer.drop(bonus.copy(), false);
        }
        ReputationService.add(killer, "secret_realm_explorer", firstClear ? 3 : 1);
        killer.displayClientMessage(Component.translatable("message.seeking_immortals.boss.defeated", bossId), true);
        if (granted > 0) {
            killer.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.boss.loot_granted", bossId, granted, firstClear), false);
        }
        // Core boss defeat clears the secret realm for M11/M06 hooks (session-latched).
        if (!realmId.isBlank()) {
            SecretRealmSessionService.onRealmCleared(realmId, killer);
        }
    }

    private static String resolveRealmId(ServerPlayer killer, String bossId) {
        String active = "";
        var optional = com.xunxian.seekingimmortals.cultivation.CultivationHelper.get(killer);
        if (optional.isPresent()) {
            active = optional.get().getWorldpackActiveSecretRealmId();
        }
        if (active != null && !active.isBlank()) {
            return active.trim().toLowerCase(java.util.Locale.ROOT);
        }
        return BossLootService.find(bossId).map(BossLootService.TableDef::secretRealmId).orElse("");
    }

    private static void placeBossCache(ServerLevel level, BlockPos pos, String bossId, boolean firstClear) {
        BlockPos chestPos = pos;
        if (!level.getBlockState(chestPos).isAir() && !level.getBlockState(chestPos).canBeReplaced()) {
            chestPos = pos.above();
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            // Prefer rolled catalog drops already granted to inventory; chest keeps soft residual.
            chest.setItem(0, new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), firstClear ? 12 : 4));
            if (firstClear) {
                chest.setItem(1, new ItemStack(ModItems.IMMORTAL_JADE.get(), 1));
            }
            if (bossId.contains("void") || bossId.contains("asura")) {
                if (firstClear) {
                    chest.setItem(2, new ItemStack(ModItems.VOID_CRYSTAL.get(), 1));
                } else {
                    chest.setItem(2, new ItemStack(ModItems.SPIRIT_STONE_SHARD.get(), 2));
                }
            } else if (bossId.contains("demon") || bossId.contains("blood")) {
                chest.setItem(2, new ItemStack(ModItems.DEMONIC_BLOOD_CORAL.get(), firstClear ? 2 : 1));
            } else {
                chest.setItem(2, new ItemStack(ModItems.ALLIANCE_MERIT_TOKEN.get(), firstClear ? 2 : 1));
            }
        }
    }
}
