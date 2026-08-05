package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.catalog.ExtendedCatalogService;
import com.xunxian.seekingimmortals.quest.TextQuestChainService;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * M-C: one-time adoption of legacy name-tagged villagers into persistent npc ids.
 *
 * <p>Before this service a vanilla villager was recognised purely by {@code getCustomName()}: no
 * persistent id, no region check, no latch. A name tag reading 「墨老先生」 on any villager anywhere
 * started {@code SevenMysteriesQuest} and handed out a free spiritual-root test, repeatedly. That is
 * a forgery vector, not a compatibility path.</p>
 *
 * <p>Recognition now happens inside one narrow window — <em>no persistent id + known legacy entity
 * type + legal authored name + matching region + not yet migrated</em> — and writes the id into
 * entity persistent data. Every later interaction reads only that id. A candidate that fails the
 * window is latched as rejected so the same tag is not re-evaluated forever.</p>
 *
 * <p>The dedicated {@link com.xunxian.seekingimmortals.entity.CultivatorNpcEntity} hierarchy keeps
 * its own authoritative id and never passes through here.</p>
 */
public final class LegacyNpcMigrationService {
    public static final String TAG_NPC_ID = "SeekingImmortalsNpcId";
    public static final String TAG_MIGRATION_VERSION = "SeekingImmortalsNpcMigrationVersion";
    public static final String TAG_SOURCE_REGION = "SeekingImmortalsNpcSourceRegion";
    public static final String TAG_MIGRATED_AT = "SeekingImmortalsNpcMigratedAt";
    public static final String TAG_REJECTED = "SeekingImmortalsNpcMigrationRejected";
    /** Bumped when the persisted shape changes; schema 1 is the first instance-safe adoption. */
    public static final int MIGRATION_VERSION = 1;

    /**
     * The authored legacy display names. These six npc ids exist nowhere in the data tree — their
     * only authority is {@link TextQuestChainService#npcFor(String)} — so the accepted names are
     * pinned here rather than inferred from a catalog that does not contain them.
     */
    private static final Map<String, String> LEGAL_NAMES = Map.ofEntries(
            Map.entry("墨老先生", "npc_mo_lao"),
            Map.entry("七玄门执事", "npc_qixuan_steward"),
            Map.entry("qixuan steward", "npc_qixuan_steward"),
            Map.entry("文本任务向导", "npc_text_quest_guide"),
            Map.entry("木兰使者", "npc_mulan_envoy"),
            Map.entry("阴罗执事", "npc_yinluo_steward"),
            Map.entry("星宫掮客", "npc_star_palace_broker"),
            Map.entry("昆吾执事", "npc_kunwu_steward"),
            Map.entry("mo lao", "npc_mo_lao"),
            Map.entry("text quest guide", "npc_text_quest_guide"),
            Map.entry("mulan envoy", "npc_mulan_envoy"),
            Map.entry("yinluo steward", "npc_yinluo_steward"),
            Map.entry("star palace broker", "npc_star_palace_broker"),
            Map.entry("kunwu steward", "npc_kunwu_steward"));

    /**
     * Regions for legacy ids that no quest chain binds. {@code npcFor} routes every 七玄/黄枫 chain
     * to {@code npc_mo_lao}, so the seven-mysteries steward has no chain of its own; the authored
     * {@code qixuan_men} / {@code huangfeng_valley} sect cards both sit in 天南.
     */
    private static final Map<String, Set<String>> PINNED_REGIONS = Map.of(
            "npc_qixuan_steward", Set.of("tiannan"));

    /** Audit buckets for a world upgrade. */
    public record AuditReport(int migrated, int ambiguous, int rejected, int pending) {}

    private LegacyNpcMigrationService() {}

    /**
     * The migration window. Every condition must hold at once; dropping any one closes it.
     * Kept as pure booleans so the rule is testable without a live server.
     */
    public static boolean isMigrationCandidate(boolean hasPersistentId, boolean knownLegacyType,
                                               boolean legalName, boolean regionMatches,
                                               boolean alreadyHandled) {
        return !hasPersistentId && knownLegacyType && legalName && regionMatches && !alreadyHandled;
    }

    /** Exact-match only: no prefix or substring matching, so a near-miss name never resolves. */
    public static String npcIdForLegacyName(String rawName) {
        if (rawName == null) {
            return "";
        }
        String key = rawName.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return "";
        }
        return LEGAL_NAMES.getOrDefault(key, "");
    }

    /**
     * The regions in which a legacy id may legally be adopted, derived from the quest chains bound
     * to that npc. Unmapped ids get an empty set, which makes {@link #regionMatches} reject.
     */
    public static Set<String> legalRegionsFor(String npcId) {
        String id = normalize(npcId);
        if (id.isBlank()) {
            return Set.of();
        }
        Set<String> regions = new LinkedHashSet<>(PINNED_REGIONS.getOrDefault(id, Set.of()));
        for (ExtendedCatalogService.QuestChain chain : ExtendedCatalogService.builtin().questChains().values()) {
            if (!id.equals(normalize(TextQuestChainService.npcFor(chain.id())))) {
                continue;
            }
            for (String token : normalize(chain.region()).split("[/,]")) {
                String region = token.trim();
                if (!region.isBlank()) {
                    regions.add(region);
                }
            }
        }
        return Set.copyOf(regions);
    }

    /** Fails closed: an unmapped id or a blank region never matches. */
    public static boolean regionMatches(String npcId, String regionId) {
        String region = normalize(regionId);
        if (region.isBlank()) {
            return false;
        }
        return legalRegionsFor(npcId).contains(region);
    }

    /** The persistent id written by a completed migration, or blank. */
    public static String persistentNpcId(Villager villager) {
        if (villager == null) {
            return "";
        }
        CompoundTag data = villager.getPersistentData();
        if (data.getInt(TAG_MIGRATION_VERSION) < MIGRATION_VERSION) {
            return "";
        }
        return normalize(data.getString(TAG_NPC_ID));
    }

    /**
     * The single entry point for legacy villager interaction. Returns the authoritative npc id,
     * migrating on the first qualifying interaction and reading only the persistent id afterwards.
     * A villager that fails the window is latched as rejected and returns blank forever.
     */
    public static String resolveNpcId(ServerPlayer player, Villager villager) {
        if (player == null || villager == null) {
            return "";
        }
        String existing = persistentNpcId(villager);
        if (!existing.isBlank()) {
            return existing;
        }
        CompoundTag data = villager.getPersistentData();
        if (data.getBoolean(TAG_REJECTED)) {
            return "";
        }
        String name = villager.getCustomName() == null ? "" : villager.getCustomName().getString();
        String npcId = npcIdForLegacyName(name);
        String region = RegionRegistry.resolveRegionId(villager.level(), villager.blockPosition());
        boolean legalName = !npcId.isBlank();
        boolean matches = legalName && regionMatches(npcId, region);
        if (!isMigrationCandidate(false, true, legalName, matches, false)) {
            // Latch the refusal so a forged tag is not re-evaluated on every right-click.
            data.putBoolean(TAG_REJECTED, true);
            return "";
        }
        data.putString(TAG_NPC_ID, npcId);
        data.putInt(TAG_MIGRATION_VERSION, MIGRATION_VERSION);
        data.putString(TAG_SOURCE_REGION, region);
        data.putLong(TAG_MIGRATED_AT, villager.level().getGameTime());
        return npcId;
    }

    /**
     * Read-only world-upgrade report. Never destroys a player's entities: a forged or ambiguous
     * villager is counted and left alone for the operator to deal with.
     */
    public static AuditReport audit(ServerPlayer player, double radius) {
        if (player == null) {
            return new AuditReport(0, 0, 0, 0);
        }
        Map<String, Integer> seen = new LinkedHashMap<>();
        int migrated = 0;
        int rejected = 0;
        int pending = 0;
        String region = RegionRegistry.resolveRegionId(player.level(), player.blockPosition());
        for (Villager villager : player.serverLevel().getEntitiesOfClass(
                Villager.class, player.getBoundingBox().inflate(radius))) {
            String id = persistentNpcId(villager);
            if (!id.isBlank()) {
                migrated++;
                seen.merge(id, 1, Integer::sum);
                continue;
            }
            if (villager.getPersistentData().getBoolean(TAG_REJECTED)) {
                rejected++;
                continue;
            }
            String name = villager.getCustomName() == null ? "" : villager.getCustomName().getString();
            String candidate = npcIdForLegacyName(name);
            if (candidate.isBlank() || !regionMatches(candidate, region)) {
                rejected++;
            } else {
                pending++;
            }
        }
        int ambiguous = 0;
        for (int count : seen.values()) {
            if (count > 1) {
                ambiguous += count;
            }
        }
        return new AuditReport(migrated, ambiguous, rejected, pending);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
