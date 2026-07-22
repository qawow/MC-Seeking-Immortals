package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ghost contract system for Yin-path cultivation.
 * Wave480: core contract management, stability decay, backlash.
 */
public final class GhostContractService {
    private static final String ROOT = "seeking_immortals_ghost_contracts";
    private static final String GHOST_PATH_EXP_KEY = "seeking_immortals_ghost_path_exp";
    private static final String GHOST_PATH_LEVEL_KEY = "seeking_immortals_ghost_path_level";

    private static final int BASE_CONTRACT_SLOTS = 1;
    private static final int MAX_CONTRACT_SLOTS = 4;

    private static final int BASE_CONTRACT_CHANCE = 60;
    private static final int STABILITY_INITIAL = 70;
    private static final int STABILITY_MAX = 100;
    private static final int STABILITY_DECAY_PER_DAY = 10;
    private static final int STABILITY_WARNING_THRESHOLD = 30;
    private static final int MAINTENANCE_STABILITY_GAIN = 20;
    private static final int MAINTENANCE_LOYALTY_GAIN = 5;

    private static final long GAME_DAY_TICKS = 24000L; // 20 minutes
    private static final long MAINTENANCE_COOLDOWN_TICKS = 1200L; // 60 seconds

    private GhostContractService() {}

    /**
     * Extract ghost ID from summon ID (e.g., "ghost_zombie" from "summon_ghost_zombie").
     */
    public static String extractGhostIdFromSummonId(String summonId) {
        if (summonId == null || summonId.isBlank()) {
            return "";
        }
        String id = summonId.toLowerCase(Locale.ROOT);
        if (id.startsWith("summon_")) {
            id = id.substring("summon_".length());
        }
        if (!id.startsWith("ghost_") && (id.contains("ghost") || id.contains("soul") || id.contains("corpse"))) {
            return "ghost_" + id.replaceAll("[^a-z0-9_]", "_");
        }
        return id;
    }

    public record GhostContract(
            String ghostId,
            int stability,
            int loyalty,
            long contractTime,
            long lastMaintenance,
            int tier,
            int empowermentLevel
    ) {
        public GhostContract withStability(int newStability) {
            return new GhostContract(ghostId, newStability, loyalty, contractTime, lastMaintenance, tier, empowermentLevel);
        }

        public GhostContract withLoyalty(int newLoyalty) {
            return new GhostContract(ghostId, stability, newLoyalty, contractTime, lastMaintenance, tier, empowermentLevel);
        }

        public GhostContract withMaintenance(long time, int stabilityGain, int loyaltyGain) {
            int newStability = Math.min(STABILITY_MAX, stability + stabilityGain);
            int newLoyalty = Math.min(100, loyalty + loyaltyGain);
            return new GhostContract(ghostId, newStability, newLoyalty, contractTime, time, tier, empowermentLevel);
        }
    }

    /**
     * Attempt to establish a ghost contract after killing an entity.
     */
    public static boolean attemptContract(ServerPlayer player, Entity target) {
        if (player == null || target == null || !(target instanceof LivingEntity living)) {
            return false;
        }

        // Validate target eligibility
        if (!isValidContractTarget(target)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.invalid_target"), true);
            return false;
        }

        // Check realm requirement
        if (!CultivationHelper.get(player).map(c -> c.getRealm().ordinal() >= Realm.QI_REFINING.ordinal()).orElse(false)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.realm_too_low",
                    Realm.QI_REFINING.getDisplayName()), true);
            return false;
        }

        // Check slot availability
        int maxSlots = getMaxContractSlots(player);
        List<GhostContract> current = listContracts(player);
        if (current.size() >= maxSlots) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.slots_full", current.size(), maxSlots), true);
            return false;
        }

        // Check if already contracted
        String ghostId = generateGhostId(living);
        if (hasContract(player, ghostId)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.already_contracted", displayName(ghostId)), true);
            return false;
        }

        // Calculate success chance
        int chance = calculateContractChance(player, living);
        boolean success = player.getRandom().nextInt(100) < chance;

        if (success) {
            // Establish contract
            int tier = estimateTier(living);
            GhostContract contract = new GhostContract(
                    ghostId,
                    STABILITY_INITIAL,
                    50, // initial loyalty
                    player.level().getGameTime(),
                    player.level().getGameTime(),
                    tier,
                    0 // initial empowerment
            );
            addContract(player, contract);
            addGhostPathExp(player, 100);

            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.success", displayName(ghostId), chance), true);
            return true;
        } else {
            // Contract failure - backlash
            applyContractFailureBacklash(player);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.failed", chance), true);
            return false;
        }
    }

    /**
     * Maintain a ghost contract to restore stability.
     * Can be done remotely (when ghost is not present) or on nearby ghost.
     */
    public static boolean maintainContract(ServerPlayer player, String ghostId) {
        if (player == null || ghostId == null || ghostId.isBlank()) {
            return false;
        }

        GhostContract contract = findContract(player, ghostId);
        if (contract == null) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.not_found", displayName(ghostId)), true);
            return false;
        }

        // Check cooldown
        long gameTime = player.level().getGameTime();
        long timeSinceLastMaintenance = gameTime - contract.lastMaintenance();
        if (timeSinceLastMaintenance < MAINTENANCE_COOLDOWN_TICKS) {
            long remaining = (MAINTENANCE_COOLDOWN_TICKS - timeSinceLastMaintenance) / 20;
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.maintenance_cooldown", remaining), true);
            return false;
        }

        // Consume spiritual power
        boolean consumed = CultivationHelper.get(player).map(cultivation -> {
            if (cultivation.getSpiritualPower() < 50) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.not_enough_qi"), true);
                return false;
            }
            cultivation.consumeQi(50);
            return true;
        }).orElse(false);

        if (!consumed) {
            return false;
        }

        // Update contract
        GhostContract updated = contract.withMaintenance(gameTime, MAINTENANCE_STABILITY_GAIN, MAINTENANCE_LOYALTY_GAIN);
        updateContract(player, updated);
        addGhostPathExp(player, 10);

        player.displayClientMessage(Component.translatable(
                "message.seeking_immortals.ghost_contract.maintained",
                displayName(ghostId), updated.stability(), updated.loyalty()), true);
        return true;
    }

    /**
     * List all active ghost contracts for a player.
     */
    public static List<GhostContract> listContracts(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        ListTag list = root.getList("Contracts", Tag.TAG_COMPOUND);
        List<GhostContract> contracts = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            contracts.add(readContract(tag));
        }
        return contracts;
    }

    /**
     * Check if player has a contract with the given ghost ID.
     */
    public static boolean hasContract(ServerPlayer player, String ghostId) {
        return findContract(player, ghostId) != null;
    }

    /**
     * Break a ghost contract. If forced, applies backlash.
     */
    public static void breakContract(ServerPlayer player, String ghostId, boolean forced) {
        if (player == null || ghostId == null) {
            return;
        }

        GhostContract contract = findContract(player, ghostId);
        if (contract == null) {
            return;
        }

        removeContract(player, ghostId);

        if (forced) {
            applyContractBreakBacklash(player, contract.tier());
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.broken_forced", displayName(ghostId)), true);
        } else {
            // Voluntary release - return some materials
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.broken_voluntary", displayName(ghostId)), true);
        }
    }

    /**
     * Tick stability decay for all contracts. Should be called periodically (e.g., daily).
     */
    public static void tickStabilityDecay(ServerPlayer player) {
        if (player == null) {
            return;
        }

        List<GhostContract> contracts = listContracts(player);
        if (contracts.isEmpty()) {
            return;
        }

        List<GhostContract> toBreak = new ArrayList<>();
        for (GhostContract contract : contracts) {
            int newStability = Math.max(0, contract.stability() - STABILITY_DECAY_PER_DAY);

            if (newStability == 0) {
                // Contract breaks due to zero stability
                toBreak.add(contract);
            } else {
                GhostContract updated = contract.withStability(newStability);
                updateContract(player, updated);

                if (newStability <= STABILITY_WARNING_THRESHOLD && contract.stability() > STABILITY_WARNING_THRESHOLD) {
                    // Crossed warning threshold
                    player.displayClientMessage(Component.translatable(
                            "message.seeking_immortals.ghost_contract.stability_warning",
                            displayName(contract.ghostId()), newStability), true);
                }
            }
        }

        // Break unstable contracts
        for (GhostContract contract : toBreak) {
            breakContract(player, contract.ghostId(), true);
        }
    }

    /**
     * Calculate maximum contract slots based on player realm and ghost path level.
     */
    public static int getMaxContractSlots(ServerPlayer player) {
        if (player == null) {
            return BASE_CONTRACT_SLOTS;
        }

        final int[] slots = {BASE_CONTRACT_SLOTS};

        // Add slots from realm
        CultivationHelper.get(player).ifPresent(cultivation -> {
            Realm realm = cultivation.getRealm();
            if (realm.ordinal() >= Realm.CORE_FORMATION.ordinal()) {
                slots[0]++;
            }
            if (realm.ordinal() >= Realm.NASCENT_SOUL.ordinal()) {
                slots[0]++;
            }
        });

        // Add slot from ghost cultivation manual (placeholder check)
        int ghostLevel = getGhostPathLevel(player);
        if (ghostLevel >= 5) {
            slots[0]++;
        }

        return Math.min(slots[0], MAX_CONTRACT_SLOTS);
    }

    /**
     * Calculate contract success chance.
     */
    private static int calculateContractChance(ServerPlayer player, LivingEntity target) {
        int chance = BASE_CONTRACT_CHANCE;

        // Target tier penalty
        int tier = estimateTier(target);
        chance -= (tier - 1) * 5;

        // Player realm bonus
        final int[] realmBonus = {0};
        CultivationHelper.get(player).ifPresent(cultivation -> {
            realmBonus[0] = cultivation.getRealm().ordinal() * 10;
        });
        chance += realmBonus[0];

        // Ghost path level bonus
        int ghostLevel = getGhostPathLevel(player);
        chance += ghostLevel * 5;

        // Existing contracts penalty
        int existingContracts = listContracts(player).size();
        chance -= existingContracts * 15;

        return Math.max(5, Math.min(95, chance));
    }

    /**
     * Check if entity is a valid contract target.
     */
    private static boolean isValidContractTarget(Entity target) {
        if (!(target instanceof LivingEntity)) {
            return false;
        }

        // Exclude players
        if (target instanceof Player) {
            return false;
        }

        // Exclude bosses
        if (target instanceof WitherBoss || target instanceof EnderDragon || target instanceof EnderDragonPart) {
            return false;
        }

        // Exclude entities that are already dead
        if (!target.isAlive()) {
            return false;
        }

        return true;
    }

    /**
     * Estimate tier based on entity health and type.
     */
    private static int estimateTier(LivingEntity entity) {
        float maxHealth = entity.getMaxHealth();

        if (maxHealth >= 100.0F) return 6;
        if (maxHealth >= 60.0F) return 5;
        if (maxHealth >= 40.0F) return 4;
        if (maxHealth >= 25.0F) return 3;
        if (maxHealth >= 15.0F) return 2;
        return 1;
    }

    /**
     * Generate ghost ID from entity type.
     */
    private static String generateGhostId(LivingEntity entity) {
        String typeName = entity.getType().getDescription().getString().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_");
        return "ghost_" + typeName;
    }

    private static Component displayName(String ghostId) {
        String id = ghostId == null ? "" : ghostId.trim().toLowerCase(Locale.ROOT);
        String path = id.startsWith("ghost_") ? id.substring("ghost_".length()) : id;
        ResourceLocation key = ResourceLocation.tryParse(path.contains(":") ? path : "minecraft:" + path);
        EntityType<?> type = key == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(key);
        return type == null ? Component.literal("契约鬼灵") : type.getDescription();
    }

    /**
     * Apply backlash when contract establishment fails.
     */
    private static void applyContractFailureBacklash(ServerPlayer player) {
        player.hurt(player.damageSources().magic(), 2.0F + player.getRandom().nextFloat() * 2.0F);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
    }

    /**
     * Apply backlash when contract breaks due to instability.
     */
    private static void applyContractBreakBacklash(ServerPlayer player, int tier) {
        float damage = tier * 4.0F;
        DamageSource source = player.damageSources().magic();
        player.hurt(source, damage);
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 2));
        addGhostPathExp(player, -50);
    }

    /**
     * Ghost path experience and level management.
     */
    public static int getGhostPathExp(ServerPlayer player) {
        return player == null ? 0 : Math.max(0, player.getPersistentData().getInt(GHOST_PATH_EXP_KEY));
    }

    public static int getGhostPathLevel(ServerPlayer player) {
        return player == null ? 0 : Math.max(0, Math.min(10, player.getPersistentData().getInt(GHOST_PATH_LEVEL_KEY)));
    }

    public static void addGhostPathExp(ServerPlayer player, int amount) {
        if (player == null) {
            return;
        }
        int current = getGhostPathExp(player);
        int newExp = Math.max(0, current + amount);
        player.getPersistentData().putInt(GHOST_PATH_EXP_KEY, newExp);

        // Check for level up
        int currentLevel = getGhostPathLevel(player);
        int requiredExp = (currentLevel + 1) * 200;
        if (newExp >= requiredExp && currentLevel < 10) {
            player.getPersistentData().putInt(GHOST_PATH_LEVEL_KEY, currentLevel + 1);
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.ghost_contract.level_up", currentLevel + 1), true);
        }
    }

    // === Internal contract management ===

    private static void addContract(ServerPlayer player, GhostContract contract) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        if (!player.getPersistentData().contains(ROOT)) {
            player.getPersistentData().put(ROOT, root);
        }

        ListTag list = root.getList("Contracts", Tag.TAG_COMPOUND);
        list.add(writeContract(contract));
        root.put("Contracts", list);
    }

    private static void updateContract(ServerPlayer player, GhostContract contract) {
        removeContract(player, contract.ghostId());
        addContract(player, contract);
    }

    private static void removeContract(ServerPlayer player, String ghostId) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        ListTag list = root.getList("Contracts", Tag.TAG_COMPOUND);
        ListTag newList = new ListTag();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (!tag.getString("GhostId").equals(ghostId)) {
                newList.add(tag);
            }
        }

        root.put("Contracts", newList);
    }

    private static GhostContract findContract(ServerPlayer player, String ghostId) {
        for (GhostContract contract : listContracts(player)) {
            if (contract.ghostId().equals(ghostId)) {
                return contract;
            }
        }
        return null;
    }

    private static CompoundTag writeContract(GhostContract contract) {
        CompoundTag tag = new CompoundTag();
        tag.putString("GhostId", contract.ghostId());
        tag.putInt("Stability", contract.stability());
        tag.putInt("Loyalty", contract.loyalty());
        tag.putLong("ContractTime", contract.contractTime());
        tag.putLong("LastMaintenance", contract.lastMaintenance());
        tag.putInt("Tier", contract.tier());
        tag.putInt("EmpowermentLevel", contract.empowermentLevel());
        return tag;
    }

    private static GhostContract readContract(CompoundTag tag) {
        return new GhostContract(
                tag.getString("GhostId"),
                tag.getInt("Stability"),
                tag.getInt("Loyalty"),
                tag.getLong("ContractTime"),
                tag.getLong("LastMaintenance"),
                tag.getInt("Tier"),
                tag.getInt("EmpowermentLevel")
        );
    }
}
