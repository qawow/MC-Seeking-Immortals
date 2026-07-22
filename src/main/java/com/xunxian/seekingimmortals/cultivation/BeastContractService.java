package com.xunxian.seekingimmortals.cultivation;

import com.xunxian.seekingimmortals.beast.BeastBestiaryService;
import com.xunxian.seekingimmortals.beast.BeastCompanionService;
import com.xunxian.seekingimmortals.beast.BeastTierService;
import com.xunxian.seekingimmortals.beast.BestiaryUnlockService;
import com.xunxian.seekingimmortals.beast.CompanionGrowthService;
import com.xunxian.seekingimmortals.catalog.SummonHonestMvpService;
import com.xunxian.seekingimmortals.registry.ModItems;
import com.xunxian.seekingimmortals.util.PlayerDisplayText;
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

    public record Contract(String id, int affinity, int growth, int experience, int evolutionStage) {}

    private enum FeedKind {
        NURTURE_PILL(70, 8),
        PREPARED_FEED(35, 5),
        BEAST_CORE(45, 6),
        SPIRIT_SHARD(15, 3),
        CONSUMABLE(35, 5);

        private final int experience;
        private final int affinity;

        FeedKind(int experience, int affinity) {
            this.experience = experience;
            this.affinity = affinity;
        }
    }

    public static List<Contract> list(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        List<Contract> list = new ArrayList<>();
        for (String key : root.getAllKeys()) {
            CompoundTag entry = root.getCompound(key);
            CompanionGrowthService.Progress progress = readProgress(key, entry);
            list.add(new Contract(key, entry.getInt("Affinity"), progress.level(),
                    progress.experience(), progress.evolutionStage()));
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.already",
                    displayName(id)), false);
            return false;
        }
        if (root.getAllKeys().size() >= MAX_SLOTS) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.full", MAX_SLOTS), false);
            return false;
        }
        CompoundTag entry = new CompoundTag();
        entry.putInt("Affinity", Math.max(1, Math.min(100, startAffinity)));
        CompanionGrowthService.Progress initial = CompanionGrowthService.legacyProgress(
                startGrowth, BeastCompanionService.stageCount(id));
        writeProgress(entry, initial);
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
        player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.contracted",
                displayName(id)), true);
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
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.missing",
                    displayName(id)), false);
            return false;
        }
        CompoundTag entry = root.getCompound(id).copy();
        CompanionGrowthService.Progress before = readProgress(id, entry);
        int affinityBefore = entry.getInt("Affinity");
        if (before.level() >= CompanionGrowthService.MAX_LEVEL && affinityBefore >= 100) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.beast.growth_max", displayName(id)), false);
            return false;
        }
        int stageCount = BeastCompanionService.stageCount(id);
        boolean evolutionReady = false;
        boolean waitingAtThreshold = CompanionGrowthService.grant(
                before, 0, stageCount, false).evolutionBlocked();
        if (waitingAtThreshold && affinityBefore >= 100) {
            evolutionReady = evolutionPoolReady(player);
            if (!evolutionReady) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.beast.evolution_pool_required"), false);
                return false;
            }
        }
        FeedKind feedKind = FeedKind.CONSUMABLE;
        if (consumeInventoryFeed && !player.getAbilities().instabuild) {
            feedKind = consumeFeedItems(player);
            if (feedKind == null) {
                player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.feed_missing"), false);
                return false;
            }
        }
        entry.putInt("Affinity", Math.min(100, affinityBefore + feedKind.affinity));
        CompanionGrowthService.Update update = CompanionGrowthService.grant(
                before, feedKind.experience, stageCount, evolutionReady);
        if (update.evolutionBlocked() && evolutionPoolReady(player)) {
            update = CompanionGrowthService.grant(before, feedKind.experience, stageCount, true);
        }
        writeProgress(entry, update.progress());
        root.put(id, entry);
        player.getPersistentData().put(ROOT, root);
        if (update.evolutionsGained() > 0) {
            Component stageName = stageDisplayName(id, update.progress().evolutionStage());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.evolved",
                    displayName(id), stageName, update.progress().level(), entry.getInt("Affinity")), true);
        } else {
            int needed = update.progress().level() >= CompanionGrowthService.MAX_LEVEL ? 0
                    : CompanionGrowthService.experienceToNextLevel(update.progress().level());
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.feed_progress",
                    displayName(id), entry.getInt("Affinity"), update.progress().level(),
                    update.progress().experience(), needed), true);
        }
        if (update.evolutionBlocked()) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.beast.evolution_pool_required"), false);
        }
        return true;
    }

    public static boolean summon(ServerPlayer player, String beastId) {
        String id = normalize(beastId);
        id = BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::id).orElse(id);
        CompoundTag root = player.getPersistentData().getCompound(ROOT);
        if (!root.contains(id)) {
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.missing",
                    displayName(id)), false);
            return false;
        }
        CompoundTag entry = root.getCompound(id);
        int affinity = entry.getInt("Affinity");
        CompanionGrowthService.Progress progress = readProgress(id, entry);
        int tier = BeastCompanionService.find(id).isPresent()
                ? BeastCompanionService.tierForEvolution(id, progress.evolutionStage())
                : entry.contains("Tier") ? entry.getInt("Tier")
                : BeastBestiaryService.find(id).map(BeastBestiaryService.BeastEntry::tier).orElse(1);
        // Wave489: BEAST_TAMING skill scales summoned beast stats.
        int tameLv = com.xunxian.seekingimmortals.skill.LifeSkillService.level(player,
                com.xunxian.seekingimmortals.skill.SkillType.BEAST_TAMING);
        BeastTierService.ScaledStats base = BeastTierService.scaleStats(tier);
        double mult = BeastCompanionService.growthStatMultiplier(id, progress);
        double health = (base.health() * 0.55D + affinity * 0.4D + progress.level() * 2.0D + tameLv * 1.2D) * mult;
        double damage = (base.damage() * 0.55D + affinity * 0.05D + progress.level() * 0.4D + tameLv * 0.2D) * mult;
        int life = base.lifeTicks() / 2 + 20 * (25 + progress.level() * 2 + tameLv);
        boolean ok = SummonHonestMvpService.spawnBeastConfigured(
                player, id, tier, life, health, damage);
        if (ok) {
            com.xunxian.seekingimmortals.skill.LifeSkillService.grantPractice(player,
                    com.xunxian.seekingimmortals.skill.SkillType.BEAST_TAMING, 14, 6);
            player.displayClientMessage(Component.translatable("message.seeking_immortals.beast.summoned",
                    displayName(id), affinity, progress.level(), progress.evolutionStage()), true);
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
        CompanionGrowthService.Progress before = readProgress(id, entry);
        int experience = 0;
        switch (kind) {
            case HIT -> {
                affinity = Math.min(100, affinity + 1);
                experience = 2;
            }
            case KILL -> {
                affinity = Math.min(100, affinity + 3);
                experience = 12;
            }
            case SURVIVE -> {
                affinity = Math.min(100, affinity + 2);
                experience = 6;
            }
        }
        CompanionGrowthService.Update update = CompanionGrowthService.grant(
                before, experience, BeastCompanionService.stageCount(id), false);
        entry.putInt("Affinity", affinity);
        writeProgress(entry, update.progress());
        root.put(id, entry);
        player.getPersistentData().put(ROOT, root);
        if (kind == CreditKind.KILL || kind == CreditKind.SURVIVE) {
            player.displayClientMessage(Component.translatable(
                    "message.seeking_immortals.beast.combat_growth", displayName(id), affinity,
                    update.progress().level(), update.progress().experience()), true);
            if (update.evolutionBlocked()) {
                player.displayClientMessage(Component.translatable(
                        "message.seeking_immortals.beast.evolution_pool_required"), false);
            }
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
            int needed = contract.growth() >= CompanionGrowthService.MAX_LEVEL ? 0
                    : CompanionGrowthService.experienceToNextLevel(contract.growth());
            map.put(contract.id(), "affinity=" + contract.affinity() + ",level=" + contract.growth()
                    + ",experience=" + contract.experience() + "/" + needed
                    + ",evolution=" + contract.evolutionStage());
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

    private static FeedKind consumeFeedItems(ServerPlayer player) {
        try {
            var nurture = com.xunxian.seekingimmortals.catalog.ItemCatalogService.resolveCatalogItem("spirit_beast_nurture_pill");
            if (nurture != null && consumeOne(player, nurture.getDefaultInstance())) {
                return FeedKind.NURTURE_PILL;
            }
            var feed = com.xunxian.seekingimmortals.catalog.ItemCatalogService.resolveCatalogItem("spirit_beast_feed");
            if (feed != null && consumeOne(player, feed.getDefaultInstance())) {
                return FeedKind.PREPARED_FEED;
            }
        } catch (Throwable ignored) {
            // ignore
        }
        if (consumeOne(player, ModItems.BEAST_CORE.get().getDefaultInstance())) {
            return FeedKind.BEAST_CORE;
        }
        if (consumeOne(player, ModItems.SPIRIT_STONE_SHARD.get().getDefaultInstance())) {
            return FeedKind.SPIRIT_SHARD;
        }
        return null;
    }

    private static CompanionGrowthService.Progress readProgress(String beastId, CompoundTag entry) {
        int stageCount = BeastCompanionService.stageCount(beastId);
        if (!entry.contains("GrowthExperience") && !entry.contains("EvolutionStage")) {
            return CompanionGrowthService.legacyProgress(entry.getInt("Growth"), stageCount);
        }
        return new CompanionGrowthService.Progress(entry.getInt("Growth"),
                entry.getInt("GrowthExperience"), entry.getInt("EvolutionStage"));
    }

    private static void writeProgress(CompoundTag entry, CompanionGrowthService.Progress progress) {
        entry.putInt("Growth", progress.level());
        entry.putInt("GrowthExperience", progress.experience());
        entry.putInt("EvolutionStage", progress.evolutionStage());
    }

    private static boolean evolutionPoolReady(ServerPlayer player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        String station = "spirit_beast_evolution_pool";
        if (com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .bestNearbyEfficiency(player, station) > 0.0D) {
            return true;
        }
        com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .tryCommissionNearby(player, station);
        return com.xunxian.seekingimmortals.structure.MultiblockOperationalService
                .bestNearbyEfficiency(player, station) > 0.0D;
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

    /** Resolve a contract id only for display; the canonical id remains the persistence key. */
    private static Component displayName(String beastId) {
        return BeastBestiaryService.find(beastId)
                .map(BeastBestiaryService.BeastEntry::display)
                .filter(PlayerDisplayText::isSafe)
                .map(Component::literal)
                .orElseGet(() -> Component.literal("未知灵兽"));
    }

    private static Component stageDisplayName(String beastId, int evolutionStage) {
        return BeastCompanionService.stageForEvolution(beastId, evolutionStage)
                .map(BeastCompanionService.GrowthStage::name)
                .filter(PlayerDisplayText::isSafe)
                .map(Component::literal)
                .orElseGet(() -> evolutionStage <= 0
                        ? Component.literal("初始形态")
                        : Component.literal("第" + evolutionStage + "阶段"));
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
