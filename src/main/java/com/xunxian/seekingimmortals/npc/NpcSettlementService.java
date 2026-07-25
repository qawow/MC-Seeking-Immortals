package com.xunxian.seekingimmortals.npc;

import com.xunxian.seekingimmortals.entity.SpiritStoneBankerEntity;
import com.xunxian.seekingimmortals.entity.CultivatorNpcEntity;
import com.xunxian.seekingimmortals.region.RegionRegistry;
import com.xunxian.seekingimmortals.registry.ModEntities;
import com.xunxian.seekingimmortals.worldpack.WorldpackDataService;
import com.xunxian.seekingimmortals.worldpack.WorldpackGameplayService;
import com.xunxian.seekingimmortals.worldpack.WorldpackSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Places the small set of service NPCs that have authored world anchors. */
public final class NpcSettlementService {
    private static final double BANKER_SEARCH_RADIUS = 16.0D;
    private static final int REGIONAL_BATCH_SIZE = 3;

    private NpcSettlementService() {}

    public static boolean ensureStarterHub(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return false;
        }
        ServerLevel overworld = player.getServer().overworld();
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackSavedData savedData = WorldpackSavedData.get(overworld);
        savedData.ensureStarterAnchor(overworld, snapshot);
        String anchorId = snapshot.findRegion(WorldpackGameplayService.DEFAULT_REGION_ID)
                .map(WorldpackDataService.RegionCard::travelAnchor)
                .orElse("");
        WorldpackSavedData.Anchor anchor = savedData.getAnchor(anchorId).orElse(null);
        if (anchor == null || !overworld.dimension().location().toString().equals(anchor.dimension())) {
            return false;
        }

        BlockPos column = BlockPos.containing(anchor.x() + 3.0D, anchor.y(), anchor.z() + 1.0D);
        BlockPos spawnPos = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        overworld.getChunkAt(spawnPos);
        AABB search = new AABB(spawnPos).inflate(BANKER_SEARCH_RADIUS, 8.0D, BANKER_SEARCH_RADIUS);
        if (!overworld.getEntitiesOfClass(SpiritStoneBankerEntity.class, search,
                SpiritStoneBankerEntity::isAlive).isEmpty()) {
            return true;
        }

        SpiritStoneBankerEntity banker = ModEntities.SPIRIT_STONE_BANKER.get().create(overworld);
        if (banker == null) {
            return false;
        }
        banker.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                anchor.yRot(), 0.0F);
        banker.setCustomName(Component.translatable("entity.seeking_immortals.spirit_stone_banker"));
        banker.setCustomNameVisible(true);
        if (!overworld.noCollision(banker)) {
            banker.moveTo(banker.getX(), banker.getY() + 1.0D, banker.getZ(), banker.getYRot(), 0.0F);
        }
        return overworld.noCollision(banker) && overworld.addFreshEntity(banker);
    }

    /** Places a bounded regional roster and records it across restarts and player visits. */
    public static List<String> ensureRegionalRoster(ServerPlayer player) {
        if (player == null || player.getServer() == null) {
            return List.of();
        }
        String regionId = RegionRegistry.resolveAndSync(player);
        List<NamedNpcRegistry.NamedNpc> candidates = regionalCandidates(regionId);
        if (candidates.isEmpty()) {
            return List.of();
        }

        ServerLevel level = player.serverLevel();
        NamedNpcPlacementSavedData placements = NamedNpcPlacementSavedData.get(player.getServer());
        BlockPos center = regionalCenter(player, regionId);
        List<String> placed = new ArrayList<>();
        for (NamedNpcRegistry.NamedNpc npc : candidates) {
            if (placed.size() >= REGIONAL_BATCH_SIZE) {
                break;
            }
            Optional<NamedNpcPlacementSavedData.Placement> recorded = placements.find(npc.id());
            if (recorded.isPresent()) {
                NamedNpcPlacementSavedData.Placement placement = recorded.get();
                if (!level.dimension().location().toString().equals(placement.dimensionId())
                        || !level.hasChunkAt(placement.pos())) {
                    continue;
                }
                var entity = level.getEntity(placement.entityId());
                if (entity instanceof CultivatorNpcEntity named
                        && named.isAlive() && npc.id().equals(named.getNamedNpcId())) {
                    continue;
                }
                Optional<CultivatorNpcEntity> recovered =
                        NpcSpawnService.findNearbyNamed(level, placement.pos(), npc.id());
                if (recovered.isPresent()) {
                    record(placements, level, npc.id(), recovered.get());
                    continue;
                }
                placements.remove(npc.id());
            }
            Optional<CultivatorNpcEntity> existing = NpcSpawnService.findNearbyNamed(level, center, npc.id());
            if (existing.isPresent()) {
                record(placements, level, npc.id(), existing.get());
                continue;
            }
            BlockPos preferred = offsetFor(center, npc.id());
            Optional<CultivatorNpcEntity> spawned = NpcSpawnService.spawnNamed(level, preferred, npc.id());
            if (spawned.isEmpty()) {
                continue;
            }
            record(placements, level, npc.id(), spawned.get());
            placed.add(npc.id());
        }
        return List.copyOf(placed);
    }

    private static List<NamedNpcRegistry.NamedNpc> regionalCandidates(String regionId) {
        List<NamedNpcRegistry.NamedNpc> exact = NamedNpcRegistry.byRegion(regionId);
        if (!exact.isEmpty()) {
            return exact;
        }
        List<NamedNpcRegistry.NamedNpc> related = new ArrayList<>();
        for (NamedNpcRegistry.NamedNpc npc : NamedNpcRegistry.all()) {
            if (!npc.regionId().isBlank()
                    && (npc.regionId().startsWith(regionId) || regionId.startsWith(npc.regionId()))) {
                related.add(npc);
            }
        }
        return List.copyOf(related);
    }

    private static BlockPos regionalCenter(ServerPlayer player, String regionId) {
        WorldpackDataService.Snapshot snapshot = WorldpackDataService.builtin();
        WorldpackSavedData savedData = WorldpackSavedData.get(player.serverLevel());
        String anchorId = snapshot.findRegion(regionId)
                .map(WorldpackDataService.RegionCard::travelAnchor)
                .orElse("");
        WorldpackSavedData.Anchor anchor = savedData.getAnchor(anchorId).orElse(null);
        if (anchor != null && player.serverLevel().dimension().location().toString().equals(anchor.dimension())) {
            return BlockPos.containing(anchor.x(), anchor.y(), anchor.z());
        }
        return player.blockPosition();
    }

    private static BlockPos offsetFor(BlockPos center, String npcId) {
        int hash = npcId.hashCode();
        int x = 4 + Math.floorMod(hash, 9);
        int z = 4 + Math.floorMod(hash / 11, 9);
        if ((hash & 1) == 0) {
            x = -x;
        }
        if ((hash & 2) == 0) {
            z = -z;
        }
        return center.offset(x, 0, z);
    }

    private static void record(
            NamedNpcPlacementSavedData placements,
            ServerLevel level,
            String npcId,
            CultivatorNpcEntity entity) {
        placements.record(npcId, entity.getUUID(), level.dimension().location().toString(), entity.blockPosition());
    }
}
