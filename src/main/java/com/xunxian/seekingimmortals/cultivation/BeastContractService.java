package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastCompanionService;
import com.xunxian.seekingimmortals.beast.BeastTierService;
import com.xunxian.seekingimmortals.beast.BestiaryUnlockService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Persistent beast contract slots (Wave49 Phase21 depth).
 * M10: companion growth stages, tier/realm gates, bestiary unlock on contract.
 */
public final class BeastContractService {
    private static final String ROOT = "seeking_immortals_beast_contracts";
    private static final int MAX_SLOTS = 3;
    /** Cross-band suppress gap: player realm-equiv tier may lag beast by at most this. */
    private static final int CONTRACT_TIER_GAP = 2;

    public enum CreditKind {
        HIT,
        KILL,
        SURVIVE
    }

    private BeastContractService() {}

    public record Contract(String id, int affinity, int growth) {}

    public static List<Contract> list(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        List<Contract> list = new ArrayList<>();
        for (String key : root.getAllKeys()) {
            CompoundTag entry = root.getCompound(key);
            list.add(new Contract(key, entry.getInt("Affinity"), entry.getInt("Growth")));
        }
        return list;
    }

    public static boolean contract(ServerPlayer player, String beastId) {
        return contract(player, beastId, 1, 0);
    }

    public static boolean contract(ServerPlayer player, String beastId, int startAffinity, int startGrowth) {
        String id = normalize(beastId);
        if (id.isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.unknown"), false);
            return false;
        }
        // Canonicalize via bestiary when possible.
        id = BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);

        // M10: thirteen-tier contract gate vs player realm.
        int beastTier = BeastBestiaryService.find(id)
                .map(BeastBestiaryService.BeastEntry::tier)
                .orElse(BeastCompanionService.find(id).map(BeastCompanionService.CompanionDef::startTier).orElse(1));
        int playerTier = playerRealmTier(player);
        if (!BeastTierService.canSuppress(playerTier, beastTier, CONTRACT_TIER_GAP)) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.beast.tier_gate", beastTier, playerTier), false);
            return false;
        }
        // M01 realm gate for high-tier companions.
        Realm required = BeastTierService.realmForTier(Math.max(1, beastTier - CONTRACT_TIER_GAP));
        if (!ProgressionGateApi.meetsRealm(player, required.getDesignId())) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.beast.realm_gate", required.getDisplayName()), false);
            return false;
        }

        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (root.contains(id)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.already", id), false);
            return false;
        }
        if (root.getAllKeys().size() >= MAX_SLOTS) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.full", MAX_SLOTS), false);
            return false;
        }
        CompoundTag entry = new CompoundTag();
        entry.putInt("Affinity", Math.max(1, Math.min(100, startAffinity)));
        entry.putInt("Growth", Math.max(0, Math.min(20, startGrowth)));
        entry.putInt("Tier", beastTier);
        root.put(id, entry);
        player.getPersistentData().put(ROOT, root);
        // Wave489: beast taming special skill practice + denser leyline clusters aid affinity.
        int affinity = entry.getInt("Affinity");
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                && com.xunxian.seekingimmortals.spiritual.SpiritualAuraManager.isLeylineCluster(
                serverLevel, new net.minecraft.world.level.ChunkPos(player.blockPosition()))) {
            affinity = Math.min(100, affinity + 5);
            entry.putInt("Affinity", affinity);
            root.put(id, entry);
            player.getPersistentData().put(ROOT, root);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.cluster_bonus"), false);
        }
        com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                com.xunxian.seekingimmortals.skill.SkillType.BEAST_TAMING, 18, 8);
        BestiaryUnlockService.unlock(player, id, BestiaryUnlockService.UnlockKind.CONTRACT);
        player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.contracted", id), true);
        return true;
    }

    public static boolean feed(ServerPlayer player, String beastId) {
        return feedInternal(player, beastId, true);
    }

    public static boolean feedFromConsumable(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        List<Contract> contracts = list(player);
        if (contracts.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.beast.feed_no_contract"), false);
            return false;
        }
        return feedInternal(player, contracts.get(0).id(), false);
    }

    private static boolean feedInternal(ServerPlayer player, String beastId, boolean consumeInventoryFeed) {
        String id = normalize(beastId);
        id = BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (!root.contains(id)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.missing", id), false);
            return false;
        }
        if (consumeInventoryFeed && !player.getAbilities().instabuild) {
            if (!consumeOne(player, ModItems.BEAST_CORE.get().getDefaultInstance())
                    && !consumeOne(player, ModItems.SPIRIT_STONE_SHARD.get().getDefaultInstance())
                    && !consumeFeedItems(player)) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.feed_missing"), false);
                return false;
            }
        }
        CompoundTag entry = root.getCompound(id).copy();
        entry.putInt("Affinity", Math.min(100, entry.getInt("Affinity") + 5));
        int growth = Math.min(20, entry.getInt("Growth") + 1);
        entry.putInt("Growth", growth);
        root.put(id, entry);
        player.getPersistentData().put(ROOT, root);
        // Companion stage message when stage advances.
        Optional<BeastCompanionService.GrowthStage> stage = BeastCompanionService.stageForGrowth(id, growth);
        if (stage.isPresent() && !stage.get().name().isBlank()) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.stage",
                    id, stage.get().name(), entry.getInt("Affinity"), growth), true);
        } else {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.fed",
                    id, entry.getInt("Affinity"), growth), true);
        }
        return true;
    }

    public static boolean summon(ServerPlayer player, String beastId) {
        String id = normalize(beastId);
        id = BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        if (!root.contains(id)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.missing", id), false);
            return false;
        }
        CompoundTag entry = root.getCompound(id);
        int affinity = entry.getInt("Affinity");
        int growth = entry.getInt("Growth");
        int tier = entry.contains("Tier") ? entry.getInt("Tier")
                : BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::tier).orElse(1);
        // Wave489: BEAST_TAMING skill scales summoned beast stats.
        int tameLv = com.xunxian.seekingimmortals.skill.LifeSkillService.level(player,
                com.xunxian.seekingimmortals.skill.SkillType.BEAST_TAMING);
        BeastTierService.ScaledStats base = BeastTierService.scaleStats(tier);
        double mult = BeastCompanionService.growthStatMultiplier(id, growth);
        double health = (base.health() * 0.55D + affinity * 0.4D + growth * 2.0D + tameLv * 1.2D) * mult;
        double damage = (base.damage() * 0.55D + affinity * 0.05D + growth * 0.4D + tameLv * 0.2D) * mult;
        int life = base.lifeTicks() / 2 + 20 * (25 + growth * 2 + tameLv);
        boolean ok = SummonHonestMvpService.spawnConfigured(
                player, "beast_" + id, life, health, damage,
                com.xunxian.seekingimmortals.entity.SummonedServitorEntity.Archetype.BEAST);
        if (ok) {
            com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                    com.xunxian.seekingimmortals.skill.SkillType.BEAST_TAMING, 14, 6);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.summoned",
                    id, affinity, growth), true);
        }
        return ok;
    }

    /**
     * Wave458 combat feedback: hit/kill/survive bump affinity/growth.
     */
    public static void recordCombatCredit(ServerPlayer player, String beastId, CreditKind kind) {
        if (player == null || kind == null) {
            return;
        }
        String id = normalize(beastId);
        id = BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);
        if (id.isBlank()) {
            return;
        }
        CompoundTag root = player.getPersistentData().getCompound(ROOT).copy();
        if (!root.contains(id)) {
            return;
        }
        CompoundTag entry = root.getCompound(id).copy();
        int affinity = entry.getInt("Affinity");
        int growth = entry.getInt("Growth");
        switch (kind) {
            case HIT -> affinity = Math.min(100, affinity + 1);
            case KILL -> {
                affinity = Math.min(100, affinity + 3);
                growth = Math.min(20, growth + 1);
            }
            case SURVIVE -> affinity = Math.min(100, affinity + 2);
        }
        entry.putInt("Affinity", affinity);
        entry.putInt("Growth", growth);
        root.put(id, entry);
        player.getPersistentData().put(ROOT, root);
        if (kind == CreditKind.KILL || kind == CreditKind.SURVIVE) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.beast.combat_growth", id, affinity, growth), true);
        }
    }

    public static String beastIdFromSummonId(String summonId) {
        if (summonId == null) {
            return "";
        }
        String id = summonId.trim().toLowerCase(Locale.ROOT);
        if (id.startsWith("beast_")) {
            return id.substring("beast_".length());
        }
        if (id.startsWith("captured_")) {
            return id.substring("captured_".length());
        }
        if (id.startsWith("ecology_")) {
            return id.substring("ecology_".length());
        }
        return "";
    }

    public static boolean hasContract(ServerPlayer player, String beastId) {
        if (player == null) {
            return false;
        }
        String id = normalize(beastId);
        id = BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);
        return player.getPersistentData().getCompound(ROOT).contains(id);
    }

    public static Map<String, String> snapshotLines(ServerPlayer player) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Contract contract : list(player)) {
            map.put(contract.id(), "affinity=" + contract.affinity() + ",growth=" + contract.growth());
        }
        return map;
    }

    private static int playerRealmTier(ServerPlayer player) {
        return CultivationHelper.get(player).map(c -> {
            Realm realm = c.getRealm();
            // Rough inverse of thirteen-tier map: QI=1-4, FOUNDATION=5-8, CORE=9-10, NASCENT=11-12, DEITY+=13
            return switch (realm) {
                case MORTAL -> 0;
                case QI_REFINING -> 3;
                case FOUNDATION_ESTABLISHMENT -> 6;
                case CORE_FORMATION -> 9;
                case NASCENT_SOUL -> 11;
                case SOUL_TRANSFORMATION -> 13;
                case VOID_REFINEMENT, UNITY, MAHAYANA, TRIBULATION, TRUE_IMMORTAL -> 13;
            };
        }).orElse(0);
    }

    private static boolean consumeFeedItems(ServerPlayer player) {
        // Optional spirit-beast feed carriers when present.
        try {
            var feed = com.xunxian.seekingimmortals.catalog.ItemCatalogService.resolveCatalogItem("spirit_beast_feed");
            if (feed != null && consumeOne(player, feed.getDefaultInstance())) {
                return true;
            }
            var nurture = com.xunxian.seekingimmortals.catalog.ItemCatalogService.resolveCatalogItem("spirit_beast_nurture_pill");
            if (nurture != null && consumeOne(player, nurture.getDefaultInstance())) {
                return true;
            }
        } catch (Throwable ignored) {
            // ignore
        }
        return false;
    }

    private static boolean consumeOne(ServerPlayer player, ItemStack sample) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.is(sample.getItem())) {
                stack.shrink(1);
                player.containerMenu.broadcastChanges();
                return true;
            }
        }
        return false;
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
