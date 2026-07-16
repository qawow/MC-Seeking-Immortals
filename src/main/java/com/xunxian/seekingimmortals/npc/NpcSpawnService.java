package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.entity.MarketTraderEntity;
import com.xunxian.seekingimmortals.entity.SectStewardEntity;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * M12 region-based named NPC placement helpers.
 * Does not own worldgen; callers (commands / structure processors / M08 outposts) invoke spawn.
 */
public final class NpcSpawnService {
    private static final int NEARBY_RADIUS = 48;

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
        steward.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        steward.setHomePos(pos.immutable());
        npc.ifPresent(steward::applyNamedNpc);
        if (npc.isEmpty() && namedNpcId != null && !namedNpcId.isBlank()) {
            steward.setNamedNpcId(namedNpcId);
        }
        if (steward.getRegionId().isBlank()) {
            steward.setRegionId(RegionRegistry.resolveRegionId(level, pos));
        }
        level.addFreshEntity(steward);
        return Optional.of(steward);
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
        trader.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        trader.setStallPos(pos.immutable());
        npc.ifPresent(trader::applyNamedNpc);
        if (npc.isEmpty() && namedNpcId != null && !namedNpcId.isBlank()) {
            trader.setNamedNpcId(namedNpcId);
        }
        if (trader.getRegionId().isBlank()) {
            trader.setRegionId(RegionRegistry.resolveRegionId(level, pos));
        }
        level.addFreshEntity(trader);
        return Optional.of(trader);
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
            boolean merchant = isMerchantRole(npc);
            Optional<? extends Entity> entity = merchant
                    ? spawnTrader(level, at, npc.id()).map(e -> e)
                    : spawnSteward(level, at, npc.id()).map(e -> e);
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
        for (SectStewardEntity steward : level.getEntitiesOfClass(SectStewardEntity.class, box)) {
            if (id.equals(steward.getNamedNpcId())) {
                return true;
            }
        }
        for (MarketTraderEntity trader : level.getEntitiesOfClass(MarketTraderEntity.class, box)) {
            if (id.equals(trader.getNamedNpcId())) {
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
}
