package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.entity.CultivatorNpcEntity;
import com.xunxian.seekingimmortals.entity.MarketTraderEntity;
import com.xunxian.seekingimmortals.entity.QuestNpcEntity;
import com.xunxian.seekingimmortals.entity.SectStewardEntity;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * M12 region-based named NPC placement helpers.
 * Does not own worldgen; callers (commands / structure processors / M08 outposts) invoke spawn.
 */
public final class NpcSpawnService {
    private static final int NEARBY_RADIUS = 48;
    private static final int SAFE_SPAWN_RADIUS = 4;
    private static final Set<String> STEWARD_ROLES = Set.of(
            "sect_master",
            "great_elder",
            "alchemy_elder",
            "outer_deacon",
            "patrol_captain");

    public enum NpcKind {
        STEWARD,
        MERCHANT,
        QUEST
    }

    private NpcSpawnService() {}

    public static Optional<SectStewardEntity> spawnSteward(ServerLevel level, BlockPos pos, String namedNpcId) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        Optional<NamedNpcRegistry.NamedNpc> npc = NamedNpcRegistry.find(namedNpcId);
        SectStewardEntity steward = ModEntities.SECT_STEWARD.get().create(level);
        if (steward == null) {
            return Optional.empty();
        }
        npc.ifPresent(steward::applyNamedNpc);
        if (npc.isEmpty() && namedNpcId != null && !namedNpcId.isBlank()) {
            steward.setNamedNpcId(namedNpcId);
        }
        if (!positionForSpawn(level, steward, pos, 0.0F)) {
            return Optional.empty();
        }
        BlockPos spawnPos = steward.blockPosition().immutable();
        steward.setHomePos(spawnPos);
        if (steward.getRegionId().isBlank()) {
            steward.setRegionId(RegionRegistry.resolveRegionId(level, spawnPos));
        }
        return level.addFreshEntity(steward) ? Optional.of(steward) : Optional.empty();
    }

    public static Optional<MarketTraderEntity> spawnTrader(ServerLevel level, BlockPos pos, String namedNpcId) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        Optional<NamedNpcRegistry.NamedNpc> npc = NamedNpcRegistry.find(namedNpcId);
        MarketTraderEntity trader = ModEntities.MARKET_TRADER.get().create(level);
        if (trader == null) {
            return Optional.empty();
        }
        npc.ifPresent(trader::applyNamedNpc);
        if (npc.isEmpty() && namedNpcId != null && !namedNpcId.isBlank()) {
            trader.setNamedNpcId(namedNpcId);
        }
        if (!positionForSpawn(level, trader, pos, 0.0F)) {
            return Optional.empty();
        }
        BlockPos spawnPos = trader.blockPosition().immutable();
        trader.setStallPos(spawnPos);
        if (trader.getRegionId().isBlank()) {
            trader.setRegionId(RegionRegistry.resolveRegionId(level, spawnPos));
        }
        return level.addFreshEntity(trader) ? Optional.of(trader) : Optional.empty();
    }

    public static Optional<QuestNpcEntity> spawnQuestNpc(ServerLevel level, BlockPos pos, String namedNpcId) {
        return spawnQuestNpc(level, pos, namedNpcId, "", 0.0F);
    }

    public static Optional<QuestNpcEntity> spawnQuestNpc(
            ServerLevel level, BlockPos pos, String namedNpcId, String fallbackDisplayName, float yRot) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        Optional<NamedNpcRegistry.NamedNpc> npc = NamedNpcRegistry.find(namedNpcId);
        QuestNpcEntity questNpc = ModEntities.QUEST_NPC.get().create(level);
        if (questNpc == null) {
            return Optional.empty();
        }
        npc.ifPresent(questNpc::applyNamedNpc);
        if (npc.isEmpty() && namedNpcId != null && !namedNpcId.isBlank()) {
            questNpc.setNamedNpcId(namedNpcId);
        }
        if (npc.isEmpty() && fallbackDisplayName != null && !fallbackDisplayName.isBlank()) {
            questNpc.setStoryIdentity(fallbackDisplayName);
        }
        if (!positionForSpawn(level, questNpc, pos, yRot)) {
            return Optional.empty();
        }
        BlockPos spawnPos = questNpc.blockPosition().immutable();
        questNpc.setHomePos(spawnPos);
        if (questNpc.getRegionId().isBlank()) {
            questNpc.setRegionId(RegionRegistry.resolveRegionId(level, spawnPos));
        }
        return level.addFreshEntity(questNpc) ? Optional.of(questNpc) : Optional.empty();
    }

    /** Route every named NPC through one role/archetype authority. */
    public static Optional<CultivatorNpcEntity> spawnNamed(
            ServerLevel level, BlockPos pos, String namedNpcId) {
        NamedNpcRegistry.NamedNpc npc = NamedNpcRegistry.find(namedNpcId).orElse(null);
        return switch (kindFor(npc)) {
            case STEWARD -> spawnSteward(level, pos, namedNpcId).map(CultivatorNpcEntity.class::cast);
            case MERCHANT -> spawnTrader(level, pos, namedNpcId).map(CultivatorNpcEntity.class::cast);
            case QUEST -> spawnQuestNpc(level, pos, namedNpcId).map(CultivatorNpcEntity.class::cast);
        };
    }

    /** Classify the full named-NPC roster before selecting its dedicated entity type. */
    public static NpcKind kindFor(NamedNpcRegistry.NamedNpc npc) {
        if (isStewardRole(npc)) {
            return NpcKind.STEWARD;
        }
        if (isMerchantRole(npc)) {
            return NpcKind.MERCHANT;
        }
        return NpcKind.QUEST;
    }

    /**
     * Spawn up to {@code limit} missing named NPCs for a region near {@code origin}.
     * Skips ids already present within {@link #NEARBY_RADIUS}.
     */
    public static List<String> ensureRegionNpcs(ServerLevel level, BlockPos origin, String regionId, int limit) {
        if (level == null || origin == null) {
            return List.of();
        }
        String region = regionId == null || regionId.isBlank()
                ? RegionRegistry.resolveRegionId(level, origin)
                : regionId.trim().toLowerCase(Locale.ROOT);
        List<NamedNpcRegistry.NamedNpc> candidates = new ArrayList<>(NamedNpcRegistry.byRegion(region));
        if (candidates.isEmpty()) {
            // Soft fallback: any npc whose region shares prefix.
            for (NamedNpcRegistry.NamedNpc npc : NamedNpcRegistry.all()) {
                if (!npc.regionId().isBlank()
                        && (npc.regionId().startsWith(region) || region.startsWith(npc.regionId()))) {
                    candidates.add(npc);
                }
            }
        }
        List<String> spawned = new ArrayList<>();
        int cap = Math.max(0, limit);
        int index = 0;
        for (NamedNpcRegistry.NamedNpc npc : candidates) {
            if (spawned.size() >= cap) {
                break;
            }
            if (isNearbyNamed(level, origin, npc.id())) {
                continue;
            }
            BlockPos at = origin.offset((index % 5) - 2, 0, (index / 5) - 2);
            index++;
            Optional<CultivatorNpcEntity> entity = spawnNamed(level, at, npc.id());
            if (entity.isPresent()) {
                spawned.add(npc.id());
            }
        }
        return List.copyOf(spawned);
    }

    public static boolean isNearbyNamed(ServerLevel level, BlockPos origin, String namedNpcId) {
        if (level == null || origin == null || namedNpcId == null || namedNpcId.isBlank()) {
            return false;
        }
        String id = namedNpcId.trim().toLowerCase(Locale.ROOT);
        AABB box = new AABB(origin).inflate(NEARBY_RADIUS);
        for (CultivatorNpcEntity npc : level.getEntitiesOfClass(CultivatorNpcEntity.class, box)) {
            if (id.equals(npc.getNamedNpcId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMerchantRole(NamedNpcRegistry.NamedNpc npc) {
        if (npc == null) {
            return false;
        }
        String role = npc.role() == null ? "" : npc.role();
        String archetype = npc.archetype() == null ? "" : npc.archetype();
        return role.contains("black_market")
                || role.contains("vendor")
                || archetype.contains("market")
                || archetype.contains("inverse")
                || archetype.contains("vendor")
                || (!npc.shopId().isBlank() && !npc.shopId().contains("contribution"));
    }

    private static boolean isStewardRole(NamedNpcRegistry.NamedNpc npc) {
        if (npc == null) {
            return false;
        }
        String id = npc.id() == null ? "" : npc.id();
        String role = npc.role() == null ? "" : npc.role();
        String archetype = npc.archetype() == null ? "" : npc.archetype();
        return STEWARD_ROLES.contains(role)
                || id.endsWith("_steward")
                || role.contains("steward")
                || role.contains("deacon")
                || role.contains("contribution")
                || role.contains("recruit")
                || role.contains("patrol_captain")
                || archetype.contains("contribution");
    }

    private static boolean positionForSpawn(
            ServerLevel level, CultivatorNpcEntity npc, BlockPos preferred, float yRot) {
        for (int radius = 0; radius <= SAFE_SPAWN_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = 3; dy >= -4; dy--) {
                        BlockPos feet = preferred.offset(dx, dy, dz);
                        if (!level.isInWorldBounds(feet)
                                || !level.isInWorldBounds(feet.above())
                                || !level.isInWorldBounds(feet.below())) {
                            continue;
                        }
                        boolean clear = level.getFluidState(feet).isEmpty()
                                && level.getFluidState(feet.above()).isEmpty()
                                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
                        if (!clear || !level.getBlockState(feet.below())
                                .isFaceSturdy(level, feet.below(), Direction.UP)) {
                            continue;
                        }
                        npc.moveTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D, yRot, 0.0F);
                        if (level.noCollision(npc)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
